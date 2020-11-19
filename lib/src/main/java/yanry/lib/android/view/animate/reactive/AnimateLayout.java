package yanry.lib.android.view.animate.reactive;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.cache.CacheTimer;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * Created by yanry on 2020/4/27.
 */
public class AnimateLayout extends FrameLayout implements Comparator<View> {
    private CacheTimer<AnimateView> dropTimer;
    private AtomicInteger animateCounter;
    private ValueHolderImpl<Integer> animateCount;
    private SparseIntArray drawingOrder;

    public AnimateLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public AnimateLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dropTimer = new CacheTimer<AnimateView>(Singletons.get(UiScheduleRunner.class)) {
            @Override
            protected void onTimeout(AnimateView tag) {
                drawingOrder.clear();
                removeView(tag);
            }
        };
        animateCounter = new AtomicInteger();
        animateCount = new ValueHolderImpl<>(0);
        drawingOrder = new SparseIntArray();
        setChildrenDrawingOrderEnabled(true);
    }

    /**
     * 显示动画，需要在主线程中调用。
     *
     * @param segment
     * @param refreshIfShowing 如果该动画正在显示，是否刷新动画
     * @return 如果该动画已经在显示且refreshIfShowing为false，则返回false，否则返回true。
     */
    public boolean showAnimate(@NonNull AnimateSegment segment, boolean refreshIfShowing) {
        if (segment.renderer != null && segment.renderer != this) {
            segment.getLogger().concat(LogLevel.Error, "segment (", segment, ") is showing in another layout: ", segment.renderer);
            return false;
        }
        int index = 0;
        int childCount = getChildCount();
        AnimateView selectedView = null;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateView animateView = (AnimateView) child;
                if (!animateView.isShowing()) {
                    index = i;
                    selectedView = animateView;
                } else if (segment == animateView.animateSegment) {
                    segment.getLogger().concat(LogLevel.Debug, "segment (", segment, ") is already showing.");
                    if (refreshIfShowing) {
                        Singletons.get(UiScheduleRunner.class).run(animateView);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        if (selectedView != null) {
            segment.getLogger().concat(LogLevel.Verbose, "select available view ", selectedView.hashCode(), " at index ", index, "(", animateCounter.get(), "/", childCount, ",z=", segment.getZOrder(), ") to render segment: ", segment);
            selectedView.bind(segment);
            invalidate();
        } else {
            AnimateView animateView = new AnimateView(getContext());
            addView(animateView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            segment.getLogger().concat(LogLevel.Verbose, "create new view ", animateView.hashCode(), " at index ", childCount, "(animateCount=", animateCounter.get(), ",z=", segment.getZOrder(), ") to render segment: ", segment);
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
        dropTimer.startTiming(300000);
    }

    @Override
    protected void onDetachedFromWindow() {
        dropTimer.invalidAll();
        dropTimer.stopTiming();
        super.onDetachedFromWindow();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (drawingOrder.size() != childCount) {
            drawingOrder.clear();
            ArrayList<View> temp = new ArrayList<>(childCount);
            for (int j = 0; j < childCount; j++) {
                temp.add(getChildAt(j));
            }
            Collections.sort(temp, this);
            for (int j = 0; j < temp.size(); j++) {
                View view = temp.get(j);
                drawingOrder.put(indexOfChild(view), j);
            }
            Logger.getDefault().vv("drawing order: ", drawingOrder);
        }
        return drawingOrder.get(i);
    }

    @Override
    public int compare(View o1, View o2) {
        if (o1 instanceof AnimateView) {
            if (o2 instanceof AnimateView) {
                AnimateView animateView1 = (AnimateView) o1;
                AnimateView animateView2 = (AnimateView) o2;
                if (animateView1.animateSegment != null) {
                    if (animateView2.animateSegment != null) {
                        int zOrder1 = animateView1.animateSegment.getZOrder();
                        int zOrder2 = animateView2.animateSegment.getZOrder();
                        if (zOrder1 == zOrder2) {
                            return (int) (animateView1.bindTime - animateView2.bindTime);
                        } else {
                            return zOrder1 - zOrder2;
                        }
                    } else {
                        // 未绑定动画的放下面
                        return 1;
                    }
                } else {
                    if (animateView2.animateSegment != null) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            } else {
                // 非AnimateView放下面
                return 1;
            }
        } else {
            // 非AnimateView放下面
            return -1;
        }
    }

    private class AnimateView extends View implements Runnable, AnimateStateWatcher {
        private AnimateSegment animateSegment;
        private long bindTime;

        public AnimateView(Context context) {
            super(context);
            setBackgroundResource(0);
        }

        private void bind(AnimateSegment segment) {
            drawingOrder.clear();
            bindTime = System.currentTimeMillis();
            dropTimer.invalid(this);
            segment.renderer = AnimateLayout.this;
            this.animateSegment = segment;
            setLayerType(segment.getLayerType(), null);
            segment.prepare();
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
                    long nextFrameDelay = animateSegment.dispatchDraw(canvas);
                    if (nextFrameDelay > 0 && (state == AnimateSegment.ANIMATE_STATE_PLAYING || state == AnimateSegment.ANIMATE_STATE_STOPPING)) {
                        Singletons.get(UiScheduleRunner.class).schedule(this, nextFrameDelay);
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
            Singletons.get(UiScheduleRunner.class).cancel(this);
            super.onDetachedFromWindow();
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
                        Singletons.get(UiScheduleRunner.class).run(this);
                        break;
                    case AnimateSegment.ANIMATE_STATE_STOPPED:
                        animateSegment.getLogger().concat(LogLevel.Verbose, "unbind segment from view ", hashCode(), ": ", animateSegment);
                        dropTimer.refresh(this);
                        drawingOrder.clear();
                        Singletons.get(UiScheduleRunner.class).run(this);
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
