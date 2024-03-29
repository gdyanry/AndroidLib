package yanry.lib.android.view.animate.reactive;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.cache.CacheTimer;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * 动画布局，用于绘制{@link AnimateSegment}。
 * <p>
 * Created by yanry on 2020/4/27.
 */
public class AnimateLayout extends FrameLayout {
    private CacheTimer<AnimateView> dropTimer = new CacheTimer<AnimateView>(Singletons.get(UiRunner.class)) {
        @Override
        protected void onTimeout(AnimateView tag) {
            if (getAnimateViewCount() > animateViewMinCount) {
                Logger.getDefault().dd("remove animate view: ", tag.hashCode());
                removeView(tag);
            } else {
                refresh(tag);
            }
        }
    };
    private AtomicInteger animateCounter = new AtomicInteger();
    private ValueHolderImpl<Integer> animateCount = new ValueHolderImpl<>(0);
    private LinkedList<AnimateView> availableTemp = new LinkedList<>();
    private int animateViewMinCount;
    private long animateViewCacheTimeout;

    public AnimateLayout(@NonNull Context context) {
        super(context);
    }

    public AnimateLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 此方法需要在主线程中调用。
     *
     * @param animateViewMinCount     AnimateView的最低数量，多于此数量的AnimateView会被超时清理
     * @param animateViewCacheTimeout AnimateView闲置缓存超时时间
     */
    public void init(int animateViewMinCount, long animateViewCacheTimeout) {
        this.animateViewMinCount = animateViewMinCount;
        this.animateViewCacheTimeout = animateViewCacheTimeout;
        int viewsToAdd = animateViewMinCount - getAnimateViewCount();
        while (viewsToAdd-- > 0) {
            AnimateView animateView = new AnimateView(getContext());
            addView(animateView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            Logger.getDefault().dd("create animate view: ", animateView.hashCode());
        }
        if (isAttachedToWindow()) {
            dropTimer.startTiming(animateViewCacheTimeout);
        }
    }

    private int getAnimateViewCount() {
        int count = 0;
        int childCount = getChildCount();
        while (childCount-- > 0) {
            View child = getChildAt(childCount);
            if (child != null && child instanceof AnimateView) {
                count++;
            }
        }
        return count;
    }

    /**
     * 显示动画，需要在主线程中调用。
     *
     * @param segment
     * @param refreshIfShowing 如果该动画正在显示，是否刷新动画
     * @return 如果该动画已经在显示且refreshIfShowing为false，则返回false，否则返回true。
     */
    public boolean showAnimate(@NonNull AnimateSegment segment, boolean refreshIfShowing) {
        View boundView = segment.getAnimateView();
        if (boundView != null && boundView.getParent() != this) {
            segment.getLogger().concat(LogLevel.Error, "segment (", segment, ") is showing in another layout: ", boundView.getParent());
            return false;
        }
        int fromZOrder = 0;
        int toZOrder = 0;
        int childCount = getChildCount();
        int i = 0;
        int zOrder = segment.getZOrder();
        for (; i < childCount; i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateView animateView = (AnimateView) child;
                if (!animateView.isShowing()) {
                    availableTemp.add(animateView);
                } else {
                    if (segment == animateView.animateSegment) {
                        segment.getLogger().concat(LogLevel.Debug, "segment (", segment, ") is already showing.");
                        availableTemp.clear();
                        if (refreshIfShowing) {
                            Singletons.get(UiRunner.class).run(animateView);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    int order = animateView.animateSegment.getZOrder();
                    if (order > zOrder) {
                        toZOrder = order;
                        break;
                    } else if (order < zOrder) {
                        fromZOrder = order;
                        availableTemp.clear();
                    }
                }
            }
        }
        AnimateView selectedView = null;
        int size = availableTemp.size();
        if (size == 1) {
            selectedView = availableTemp.get(0);
        } else if (size > 1) {
            if (toZOrder == 0) {
                selectedView = availableTemp.get(Math.min(size - 1, zOrder - fromZOrder));
            } else {
                selectedView = availableTemp.get((zOrder - fromZOrder) * size / (toZOrder - fromZOrder));
            }
        }
        if (selectedView != null) {
            availableTemp.clear();
            segment.getLogger().concat(LogLevel.Verbose, "select available view ", selectedView.hashCode(), " at index ", indexOfChild(selectedView), "/", childCount, "(animCount=", animateCounter.get(), ",z=", zOrder, ") to render segment: ", segment);
            selectedView.bind(segment);
            invalidate();
        } else {
            AnimateView animateView = new AnimateView(getContext());
            addView(animateView, i, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            segment.getLogger().concat(LogLevel.Verbose, "create new view ", animateView.hashCode(), " at index ", i, "/", getChildCount(), "(animCount=", animateCounter.get(), ",z=", zOrder, ") to render segment: ", segment);
            animateView.bind(segment);
        }
        return true;
    }

    public void pauseAnimate() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateSegment animateSegment = ((AnimateView) child).animateSegment;
                if (animateSegment != null) {
                    animateSegment.setPause(true);
                }
            }
        }
    }

    public void resumeAnimate() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateSegment animateSegment = ((AnimateView) child).animateSegment;
                if (animateSegment != null) {
                    animateSegment.setPause(false);
                }
            }
        }
    }

    public void stopAnimate() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateSegment animateSegment = ((AnimateView) child).animateSegment;
                if (animateSegment != null) {
                    animateSegment.stopAnimate();
                }
            }
        }
    }

    public ValueHolder<Integer> getAnimateCount() {
        return animateCount;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animateViewCacheTimeout > 0) {
            dropTimer.startTiming(animateViewCacheTimeout);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        dropTimer.invalidAll();
        dropTimer.stopTiming();
        super.onDetachedFromWindow();
    }

    class AnimateView extends View implements Runnable, AnimateStateWatcher {
        private AnimateSegment animateSegment;

        public AnimateView(Context context) {
            super(context);
            setBackgroundResource(0);
        }

        private void bind(AnimateSegment segment) {
            dropTimer.invalid(this);
            this.animateSegment = segment;
            segment.prepare(this);
            segment.getAnimateStateRegistry().register(this);
            segment.setAnimateState(AnimateSegment.ANIMATE_STATE_PLAYING);
            animateCount.setValue(animateCounter.incrementAndGet());
        }

        private boolean isShowing() {
            return animateSegment != null && animateSegment.getAnimateState() != AnimateSegment.ANIMATE_STATE_STOPPED;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (animateSegment != null) {
                int state = animateSegment.getAnimateState();
                if (state != AnimateSegment.ANIMATE_STATE_STOPPED) {
                    long nextFrameDelay = animateSegment.draw(canvas);
                    if (nextFrameDelay > 0 && (state == AnimateSegment.ANIMATE_STATE_PLAYING || state == AnimateSegment.ANIMATE_STATE_STOPPING)) {
                        Singletons.get(UiRunner.class).schedule(this, nextFrameDelay);
                    } else if (nextFrameDelay == 0) {
                        animateSegment.setAnimateState(AnimateSegment.ANIMATE_STATE_PAUSED);
                    } else if (nextFrameDelay < 0) {
                        animateSegment.setAnimateState(AnimateSegment.ANIMATE_STATE_STOPPED);
                    }
                }
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            if (animateSegment != null) {
                animateSegment.setAnimateState(AnimateSegment.ANIMATE_STATE_STOPPED);
            }
            Singletons.get(UiRunner.class).cancel(this);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            if (animateSegment != null) {
                animateSegment.onConfigurationChanged(newConfig);
            }
        }

        @Override
        public void run() {
            invalidate();
        }

        @Override
        public void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState) {
            if (animateSegment == this.animateSegment) {
                switch (toState) {
                    case AnimateSegment.ANIMATE_STATE_PLAYING:
                    case AnimateSegment.ANIMATE_STATE_STOPPING:
                        Singletons.get(UiRunner.class).run(this);
                        break;
                    case AnimateSegment.ANIMATE_STATE_STOPPED:
                        animateSegment.getLogger().concat(LogLevel.Verbose, "unbind segment from view ", hashCode(), ": ", animateSegment);
                        dropTimer.refresh(this);
                        Singletons.get(UiRunner.class).run(this);
                        animateSegment.getAnimateStateRegistry().unregister(this);
                        setLayerType(View.LAYER_TYPE_NONE, null);
                        this.animateSegment = null;
                        animateCount.setValue(animateCounter.decrementAndGet());
                        break;
                }
            }
        }
    }
}
