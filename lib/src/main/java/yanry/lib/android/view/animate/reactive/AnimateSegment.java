package yanry.lib.android.view.animate.reactive;

import static yanry.lib.java.model.schedule.ShowData.STATE_DEQUEUE;
import static yanry.lib.java.model.schedule.ShowData.STATE_DISMISS;
import static yanry.lib.java.model.schedule.ShowData.STATE_SHOWING;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.FlagsHolder;
import yanry.lib.java.model.Registry;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.animate.TimeController;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.schedule.ShowData;
import yanry.lib.java.model.watch.IntWatcher;

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
     * 动画正在结束，只有当{@link #isSupportExitAnimate()}返回true时才会出现此状态
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
    private View animateView;
    private ScheduleBinding binding;
    private long elapsedTimeOnStop;

    public int getAnimateState() {
        return animateState;
    }

    /**
     * 获取该动画片断绑定的View，在动画开始播放后以及结束播放之前不为null。
     *
     * @return
     */
    @Nullable
    public View getAnimateView() {
        return animateView;
    }

    public Registry<AnimateStateWatcher> getAnimateStateRegistry() {
        return animateStateRegistry;
    }

    /**
     * 设置该动画所在的z轴坐标，z值大的动画会覆盖在z值小的动画上面，不能为负数。
     *
     * @param zOrder
     */
    public void setZOrder(int zOrder) {
        if (zOrder < 0) {
            throw new IllegalArgumentException("invalid z-order: " + zOrder);
        }
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
        if (isSupportExitAnimate()) {
            if (animateState != ANIMATE_STATE_STOPPED) {
                return setAnimateState(ANIMATE_STATE_STOPPING);
            }
        } else {
            return setAnimateState(ANIMATE_STATE_STOPPED);
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
        if (animateView != null && animateView.getParent() != animateLayout) {
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
            switch (animateState) {
                case ANIMATE_STATE_PAUSED:
                    super.setPause(true);
                    break;
                case ANIMATE_STATE_PLAYING:
                    super.setPause(false);
                    break;
                case ANIMATE_STATE_STOPPED:
                    elapsedTimeOnStop = 0;
                    break;
                case ANIMATE_STATE_STOPPING:
                    super.setPause(false);
                    elapsedTimeOnStop = getElapsedTime();
                    break;
            }
            onStateChange(animateState, oldState);
            for (AnimateStateWatcher watcher : animateStateRegistry.getList()) {
                watcher.onAnimateStateChange(this, animateState, oldState);
            }
            if (animateState == ANIMATE_STATE_STOPPED) {
                animateView = null;
            }
            return true;
        }
        return false;
    }

    /**
     * 准备开始绘制。
     */
    protected void prepare(View animateView) {
        this.animateView = animateView;
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
    }

    /**
     * Called when the current configuration of the resources being used
     * by the application have changed.  You can use this to decide when
     * to reload resources that can changed based on orientation and other
     * configuration characteristics.  You only need to use this if you are
     * not relying on the normal {@link android.app.Activity} mechanism of
     * recreating the activity instance upon a configuration change.
     *
     * @param newConfig The new resource configuration.
     */
    protected void onConfigurationChanged(Configuration newConfig) {
    }

    /**
     * @return 是否支持退场动画
     */
    protected boolean isSupportExitAnimate() {
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

    public class ScheduleBinding extends FlagsHolder implements IntWatcher, AnimateStateWatcher, Runnable {
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
                    Singletons.get(UiRunner.class).run(this);
                    break;
                case STATE_DISMISS:
                case STATE_DEQUEUE:
                    if (hasFlag(BINDING_FLAG_DISMISS_STOP)) {
                        stopAnimate();
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
        public void onValueChange(int to, int from) {
            switch (to) {
                case STATE_SHOWING:
                    Singletons.get(UiRunner.class).run(this);
                    break;
                case STATE_DEQUEUE:
                case STATE_DISMISS:
                    if (hasFlag(BINDING_FLAG_DISMISS_STOP)) {
                        stopAnimate();
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
