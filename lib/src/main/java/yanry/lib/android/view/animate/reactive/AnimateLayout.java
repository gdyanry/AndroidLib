package yanry.lib.android.view.animate.reactive;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.cache.CacheTimer;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;

/**
 * Created by yanry on 2020/4/27.
 */
public class AnimateLayout extends FrameLayout {
    private CacheTimer<AnimateView> dropTimer;
    private ArrayList<AnimateView> temp;
    private AtomicInteger animateCounter;
    private ValueHolderImpl<Integer> animateCount;

    public AnimateLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public AnimateLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dropTimer = new CacheTimer<AnimateView>() {
            @Override
            protected void onTimeout(AnimateView tag) {
                Singletons.get(UiScheduleRunner.class).post(() -> removeView(tag));
            }
        };
        temp = new ArrayList<>();
        animateCounter = new AtomicInteger();
        animateCount = new ValueHolderImpl<>();
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
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child instanceof AnimateView) {
                AnimateView animateView = (AnimateView) child;
                index = i;
                if (!animateView.isShowing()) {
                    temp.add(animateView);
                } else {
                    if (segment == animateView.animateSegment) {
                        segment.getLogger().concat(LogLevel.Debug, "segment (", segment, ") is already showing.");
                        temp.clear();
                        if (refreshIfShowing) {
                            Singletons.get(UiScheduleRunner.class).run(animateView);
                            return true;
                        } else {
                            return false;
                        }
                    }
                    int zOrder = animateView.animateSegment.getZOrder();
                    if (zOrder > segment.getZOrder()) {
                        break;
                    } else if (zOrder < segment.getZOrder()) {
                        temp.clear();
                    }
                }
            }
        }
        int size = temp.size();
        if (size > 0) {
            AnimateView selectedView = temp.get(size / 2);
            segment.getLogger().concat(LogLevel.Verbose, "select available view ", selectedView.hashCode(), " out of ", size, "(", animateCounter.get(), "/", childCount, ") to render segment: ", segment);
            selectedView.bind(segment);
            temp.clear();
        } else {
            AnimateView animateView = new AnimateView(getContext());
            addView(animateView, index, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            segment.getLogger().concat(LogLevel.Verbose, "create new view ", animateView.hashCode(), " at index ", index, "/", childCount, "(animateCount=", animateCounter.get(), ") to render segment: ", segment);
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

    private class AnimateView extends View implements Runnable, AnimateStateWatcher {
        private AnimateSegment animateSegment;

        public AnimateView(Context context) {
            super(context);
        }

        private void bind(AnimateSegment segment) {
            dropTimer.invalid(this);
            segment.renderer = AnimateLayout.this;
            this.animateSegment = segment;
            segment.prepare();
            segment.addAnimateStateWatcher(this);
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
                    if (nextFrameDelay > 0 && state == AnimateSegment.ANIMATE_STATE_PLAYING) {
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
                        Singletons.get(UiScheduleRunner.class).run(this);
                        break;
                    case AnimateSegment.ANIMATE_STATE_STOPPED:
                        animateSegment.getLogger().concat(LogLevel.Verbose, "unbind segment from view ", hashCode(), ": ", animateSegment);
                        dropTimer.refresh(this);
                        Singletons.get(UiScheduleRunner.class).run(this);
                        animateSegment.removeAnimateStateWatcher(this);
                        this.animateSegment = null;
                        animateCount.setValue(animateCounter.decrementAndGet());
                        break;
                }
            }
        }
    }
}
