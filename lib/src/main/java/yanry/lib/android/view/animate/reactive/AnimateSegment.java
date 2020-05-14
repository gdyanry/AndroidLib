package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import yanry.lib.java.model.Registry;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.schedule.ShowData;
import yanry.lib.java.model.watch.ValueWatcher;

import static yanry.lib.java.model.schedule.ShowData.STATE_DEQUEUE;
import static yanry.lib.java.model.schedule.ShowData.STATE_DISMISS;
import static yanry.lib.java.model.schedule.ShowData.STATE_SHOWING;

/**
 * 动画片段。
 */
public abstract class AnimateSegment extends Registry<AnimateStateWatcher> {
    public static final int ANIMATE_STATE_PLAYING = 1;
    public static final int ANIMATE_STATE_PAUSED = 2;
    public static final int ANIMATE_STATE_STOPPED = 3;

    private int animateState;
    private Registry<AnimateStateWatcher> animateStateRegistry;

    public AnimateSegment() {
        animateStateRegistry = new Registry<>();
    }

    public int getAnimateState() {
        return animateState;
    }

    public boolean addAnimateStateWatcher(AnimateStateWatcher watcher) {
        return animateStateRegistry.register(watcher);
    }

    public boolean removeAnimateStateWatcher(AnimateStateWatcher watcher) {
        return animateStateRegistry.unregister(watcher);
    }

    public boolean pauseAnimate() {
        if (animateState != ANIMATE_STATE_STOPPED) {
            return setAnimateState(ANIMATE_STATE_PAUSED);
        }
        getLogger().concat(LogLevel.Warn, "fail to pause a stopped animate: ", this);
        return false;
    }

    public boolean resumeAnimate() {
        if (animateState != ANIMATE_STATE_STOPPED) {
            return setAnimateState(ANIMATE_STATE_PLAYING);
        }
        getLogger().concat(LogLevel.Warn, "fail to resume a stopped animate: ", this);
        return false;
    }

    public boolean stopAnimate() {
        return setAnimateState(ANIMATE_STATE_STOPPED);
    }

    public ScheduleBinding bindShowData(ShowData bindingData, AnimateLayout animateLayout) {
        return new ScheduleBinding(bindingData, animateLayout);
    }

    boolean setAnimateState(int animateState) {
        int oldState = this.animateState;
        if (oldState != animateState) {
            this.animateState = animateState;
            onStateChange(animateState, oldState);
            for (AnimateStateWatcher watcher : animateStateRegistry.getCopy()) {
                watcher.onAnimateStateChange(this, animateState, oldState);
            }
            return true;
        }
        return false;
    }

    /**
     * 准备开始绘制。
     */
    protected void prepare() {
        animateState = 0;
    }

    /**
     * 动画状态变化回调。
     *
     * @param to
     * @param from
     */
    protected void onStateChange(int to, int from) {

    }

    protected abstract Logger getLogger();

    /**
     * @return 该动画所在的z轴坐标，z值大的动画会覆盖在z值小的动画上面。
     */
    protected abstract int getZOrder();

    /**
     * 绘制动画的当前帧。
     *
     * @param canvas 画布
     * @return 正数表示该时间（毫秒）后绘制下一帧，0表示没有下一帧，负数表示动画结束。
     */
    protected abstract long draw(Canvas canvas);

    public class ScheduleBinding implements ValueWatcher<Integer>, AnimateStateWatcher {
        private ShowData bindingData;
        private AnimateLayout animateLayout;

        public ScheduleBinding(ShowData bindingData, AnimateLayout animateLayout) {
            this.bindingData = bindingData;
            this.animateLayout = animateLayout;
            bindingData.getState().addWatcher(this);
            addAnimateStateWatcher(this);
        }

        public void unbind() {
            if (bindingData != null) {
                bindingData.getState().removeWatcher(this);
            }
            removeAnimateStateWatcher(this);
            bindingData = null;
            animateLayout = null;
        }

        @Override
        public void onValueChange(Integer to, Integer from) {
            switch (to.intValue()) {
                case STATE_SHOWING:
                    if (animateLayout != null) {
                        animateLayout.showAnimate(AnimateSegment.this);
                    }
                    break;
                case STATE_DEQUEUE:
                case STATE_DISMISS:
                    setAnimateState(ANIMATE_STATE_STOPPED);
                    unbind();
                    break;
            }
        }

        @Override
        public void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState) {
            if (animateState == ANIMATE_STATE_STOPPED) {
                if (bindingData != null) {
                    bindingData.dismiss(0);
                }
                unbind();
            }
        }
    }
}
