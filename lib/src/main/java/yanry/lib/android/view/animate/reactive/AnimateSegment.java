package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import yanry.lib.java.model.Registry;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.schedule.Display;
import yanry.lib.java.model.schedule.Scheduler;
import yanry.lib.java.model.schedule.ShowData;

/**
 * 动画片段。
 */
public abstract class AnimateSegment extends ShowData {
    public static final int ANIMATE_STATE_PLAYING = 1;
    public static final int ANIMATE_STATE_PAUSED = 2;
    public static final int ANIMATE_STATE_STOPPED = 3;

    public static <T extends AnimateSegment> void schedule(Scheduler scheduler, Class<? extends Display<T>> displayType, T... segments) {
        for (int i = 0; i < segments.length; i++) {
            T segment = segments[i];
            if (i == 0) {
                segment.addFlag(FLAG_EXPEL_WAITING_DATA);
                segment.setStrategy(STRATEGY_SHOW_IMMEDIATELY);
            } else {
                segment.setStrategy(STRATEGY_APPEND_TAIL);
            }
            scheduler.show(segment, displayType);
        }
    }

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

    /**
     * 准备开始绘制。
     */
    protected void prepare() {
        animateState = 0;
    }

    boolean setAnimateState(int animateState) {
        int oldState = this.animateState;
        if (oldState != animateState) {
            this.animateState = animateState;
            if (animateState == ANIMATE_STATE_STOPPED) {
                dismiss(0);
            }
            if (animateStateRegistry.size() > 0) {
                for (AnimateStateWatcher watcher : animateStateRegistry.getCopy()) {
                    watcher.onAnimateStateChange(this, animateState, oldState);
                }
            }
            return true;
        }
        return false;
    }

    protected abstract Logger getLogger();

    /**
     * @return 该动画所在的z轴坐标，z值大的动画会覆盖在z值小的动画上面。
     */
    protected abstract int getZOrder();

    /**
     * 绘制动画的当前帧。
     *
     * @param canvas
     * @return 正数表示该时间（毫秒）后绘制下一帧，0表示没有下一帧，负数表示动画结束。
     */
    protected abstract long draw(Canvas canvas);

    @Override
    protected void onStateChange(int to, int from) {
        if (to == STATE_DISMISS) {
            setAnimateState(ANIMATE_STATE_STOPPED);
        }
    }
}
