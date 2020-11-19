package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.FlagsHolder;
import yanry.lib.java.model.Registry;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.animate.TimeController;
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
public abstract class AnimateSegment extends TimeController {
    /**
     * 动画播放中
     */
    public static final int ANIMATE_STATE_PLAYING = 1;
    /**
     * 动画暂停中
     */
    public static final int ANIMATE_STATE_PAUSED = 2;
    /**
     * 动画结束
     */
    public static final int ANIMATE_STATE_STOPPED = 3;

    /**
     * 动画正在结束
     */
    public static final int ANIMATE_STATE_STOPPING = 4;

    /**
     * 动画结束时关闭ShowData
     */
    public static final int BINDING_FLAG_STOP_DISMISS = 1;
    /**
     * ShowData关闭时结束动画
     */
    public static final int BINDING_FLAG_DISMISS_STOP = 2;

    private int zOrder;
    private Logger logger = Logger.getDefault();
    private int animateState;
    private Registry<AnimateStateWatcher> animateStateRegistry = new Registry<>();
    AnimateLayout renderer;
    private ScheduleBinding binding;
    private long elapsedTimeOnStop;

    public int getAnimateState() {
        return animateState;
    }

    public Registry<AnimateStateWatcher> getAnimateStateRegistry() {
        return animateStateRegistry;
    }

    /**
     * 设置该动画所在的z轴坐标，z值大的动画会覆盖在z值小的动画上面。
     *
     * @param zOrder
     */
    public void setZOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    public void setLogger(Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
    }

    public int getZOrder() {
        return zOrder;
    }

    @NonNull
    public Logger getLogger() {
        return logger;
    }

    public boolean stopAnimate() {
        if (animateState != ANIMATE_STATE_STOPPED) {
            return setAnimateState(ANIMATE_STATE_STOPPING);
        }
        return false;
    }

    public long getExitElapsedTime() {
        return elapsedTimeOnStop > 0 ? getElapsedTime() - elapsedTimeOnStop : -1;
    }

    /**
     * 绑定动画和ShowData以实现二者的协同结束
     *
     * @param bindingData
     * @param animateLayout
     * @return
     */
    public ScheduleBinding bindShowData(ShowData bindingData, AnimateLayout animateLayout) {
        if (renderer != null && renderer != animateLayout) {
            logger.concat(LogLevel.Error, "failed to bind animate segment(", this, ") to show data: ", bindingData);
            return null;
        }
        if (binding != null) {
            binding.unbind();
        }
        binding = new ScheduleBinding(bindingData, animateLayout);
        return binding;
    }

    boolean setAnimateState(int animateState) {
        int oldState = this.animateState;
        if (oldState != animateState) {
            this.animateState = animateState;
            if (animateState == ANIMATE_STATE_STOPPING) {
                elapsedTimeOnStop = getElapsedTime();
            } else if (animateState == ANIMATE_STATE_STOPPED) {
                renderer = null;
                elapsedTimeOnStop = 0;
            }
            onStateChange(animateState, oldState);
            for (AnimateStateWatcher watcher : animateStateRegistry.getList()) {
                watcher.onAnimateStateChange(this, animateState, oldState);
            }
            return true;
        }
        return false;
    }

    long dispatchDraw(Canvas canvas) {
        if (animateState == ANIMATE_STATE_STOPPING && !supportExitAnimate()) {
            return -1;
        } else {
            return draw(canvas);
        }
    }

    /**
     * 准备开始绘制。
     */
    protected void prepare() {
        animateState = 0;
        seekTo(0);
    }

    /**
     * 动画状态变化回调。
     *
     * @param to
     * @param from
     */
    protected void onStateChange(int to, int from) {
        if (to == ANIMATE_STATE_PAUSED) {
            super.setPause(true);
        } else if (to == ANIMATE_STATE_PLAYING) {
            super.setPause(false);
        }
    }

    /**
     * 参考{@link View#setLayerType(int, Paint)}。子类可按需重写此方法。
     *
     * @return
     */
    protected int getLayerType() {
        return View.LAYER_TYPE_NONE;
    }

    /**
     * @return 是否支持退场动画
     */
    protected boolean supportExitAnimate() {
        return false;
    }

    /**
     * 绘制动画的当前帧。
     *
     * @param canvas 画布
     * @return 正数表示该时间（毫秒）后绘制下一帧，0表示没有下一帧，负数表示动画结束。
     */
    protected abstract long draw(Canvas canvas);

    @Override
    public void setPause(boolean pause) {
        if (animateState == ANIMATE_STATE_STOPPED) {
            logger.concat(LogLevel.Warn, "fail to ", pause ? "pause" : "resume", " a stopped animate: ", this);
        } else if (animateState == ANIMATE_STATE_STOPPING) {
            logger.concat(LogLevel.Warn, "fail to ", pause ? "pause" : "resume", " a stopping animate: ", this);
        } else {
            setAnimateState(pause ? ANIMATE_STATE_PAUSED : ANIMATE_STATE_PLAYING);
        }
    }

    public class ScheduleBinding extends FlagsHolder implements ValueWatcher<Integer>, AnimateStateWatcher, Runnable {
        private ShowData bindingData;
        private AnimateLayout animateLayout;

        public ScheduleBinding(ShowData bindingData, AnimateLayout animateLayout) {
            super(false);
            addFlag(BINDING_FLAG_STOP_DISMISS | BINDING_FLAG_DISMISS_STOP);
            this.bindingData = bindingData;
            this.animateLayout = animateLayout;
            Integer dataState = bindingData.getState().getValue();
            switch (dataState) {
                case STATE_SHOWING:
                    Singletons.get(UiScheduleRunner.class).run(this);
                    break;
                case STATE_DISMISS:
                case STATE_DEQUEUE:
                    if (hasFlag(BINDING_FLAG_DISMISS_STOP) && animateState != ANIMATE_STATE_STOPPED) {
                        setAnimateState(ANIMATE_STATE_STOPPING);
                    }
                    break;
            }
            bindingData.getState().addWatcher(this);
            getAnimateStateRegistry().register(this);
        }

        public void unbind() {
            if (bindingData != null) {
                bindingData.getState().removeWatcher(this);
            }
            getAnimateStateRegistry().unregister(this);
            bindingData = null;
            animateLayout = null;
            binding = null;
        }

        public ShowData getBindingData() {
            return bindingData;
        }

        public AnimateSegment getAnimateSegment() {
            return AnimateSegment.this;
        }

        @Override
        public void onValueChange(Integer to, Integer from) {
            switch (to.intValue()) {
                case STATE_SHOWING:
                    Singletons.get(UiScheduleRunner.class).run(this);
                    break;
                case STATE_DEQUEUE:
                case STATE_DISMISS:
                    if (hasFlag(BINDING_FLAG_DISMISS_STOP) && animateState != ANIMATE_STATE_STOPPED) {
                        setAnimateState(ANIMATE_STATE_STOPPING);
                    }
                    unbind();
                    break;
            }
        }

        @Override
        public void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState) {
            if (animateState == ANIMATE_STATE_STOPPING || animateState == ANIMATE_STATE_STOPPED) {
                if (bindingData != null) {
                    if (hasFlag(BINDING_FLAG_STOP_DISMISS)) {
                        bindingData.dismiss(0);
                    }
                }
                unbind();
            }
        }

        @Override
        public void run() {
            if (animateLayout != null) {
                animateLayout.showAnimate(AnimateSegment.this, true);
            }
        }
    }
}
