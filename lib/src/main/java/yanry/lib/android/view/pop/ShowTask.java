package yanry.lib.android.view.pop;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.LinkedList;

import yanry.lib.android.interfaces.BooleanConsumer;
import yanry.lib.android.interfaces.Consumer;
import yanry.lib.android.interfaces.Filter;
import yanry.lib.android.util.CommonUtils;
import yanry.lib.android.view.pop.display.ToastDisplay;
import yanry.lib.java.model.log.Logger;

/**
 * 要显示的数据。一般推荐使用Builder来创建对象（简单），当需要动态配置显示策略时才直接使用构造函数并实现抽象方法创建对象（灵活）。
 */
public abstract class ShowTask implements Runnable {
    public static final int STRATEGY_APPEND_TAIL = 0;
    public static final int STRATEGY_INSERT_HEAD = 1;
    public static final int STRATEGY_SHOW_IMMEDIATELY = 2;

    public static Builder getBuilder() {
        return new Builder();
    }

    Class<? extends Display> displayIndicator;
    Context context;
    Object data;
    Object tag;
    Display display;
    PopScheduler scheduler;
    long duration;
    private LinkedList<Consumer<ShowTask>> onShowListeners;
    private LinkedList<BooleanConsumer> onDismissListeners;

    private ShowTask(Class<? extends Display> displayIndicator, Context context, Object data) {
        this.displayIndicator = displayIndicator;
        this.context = context;
        this.data = data;
        onShowListeners = new LinkedList<>();
        onDismissListeners = new LinkedList<>();
    }

    public boolean isShowing() {
        return scheduler != null && scheduler.current == this;
    }

    public void dismiss(long delay) {
        if (scheduler != null && scheduler.current == this) {
            CommonUtils.cancelPendingTimeout(this);
            if (delay > 0) {
                CommonUtils.scheduleTimeout(this, delay);
            } else if (doDismiss()) {
                Logger.getDefault().vv("manually dismiss: ", data);
            }
        }
    }

    private boolean doDismiss() {
        if (scheduler != null && scheduler.current == this) {
            scheduler.current = null;
            onDismiss(true);
            HashSet<Display> displaysToDismiss = new HashSet<>();
            displaysToDismiss.add(display);
            scheduler.rebalance(null, displaysToDismiss);
            return true;
        }
        return false;
    }

    public ShowTask setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    /**
     * Add callback after this task has been shown.
     */
    public void addOnShowListener(Consumer<ShowTask> listener) {
        onShowListeners.add(listener);
    }

    /**
     * 回调的boolean参数只有当dismiss事件由外部触发后并且调用了{@link Display#notifyDismiss(Object)}的情况下才为false。
     */
    public void addOnDismissListener(BooleanConsumer listener) {
        onDismissListeners.add(listener);
    }

    final void onShow() {
        for (Consumer<ShowTask> listener : onShowListeners) {
            listener.accept(this);
        }
    }

    final void onDismiss(boolean isBeforeDismiss) {
        for (BooleanConsumer listener : onDismissListeners) {
            listener.accept(isBeforeDismiss);
        }
    }

    @Override
    public void run() {
        Logger.getDefault().vv("dismiss on timeout: ", data);
        doDismiss();
    }

    @Strategy
    protected abstract int getStrategy();

    protected abstract boolean rejectExpelled();

    protected abstract boolean rejectDismissed();

    protected abstract boolean expelWaitingTask(ShowTask request);

    /**
     * 默认构造的ShowTask不拒绝从队列被清除或者被替换显示，如果当前有正在显示的数据界面，则加入队列尾部等待。
     */
    public static class Builder {
        private Class<? extends Display> displayIndicator;
        private int strategy;
        private boolean rejectExpelled;
        private boolean rejectDismissed;
        private Filter<ShowTask> ifExpel;
        private Consumer<ShowTask> onShow;
        private BooleanConsumer onDismiss;
        private long duration;

        private Builder() {
        }

        public Builder displayIndicator(Class<? extends Display> displayIndicator) {
            this.displayIndicator = displayIndicator;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder insertHead() {
            strategy = STRATEGY_INSERT_HEAD;
            return this;
        }

        public Builder showImmediately() {
            strategy = STRATEGY_SHOW_IMMEDIATELY;
            return this;
        }

        public Builder rejectExpelled() {
            rejectExpelled = true;
            return this;
        }

        public Builder rejectDismissed() {
            rejectDismissed = true;
            return this;
        }

        public Builder expelWaitingTasks() {
            this.ifExpel = target -> true;
            return this;
        }

        public Builder expelWaitingTasks(Filter<ShowTask> ifExpel) {
            this.ifExpel = ifExpel;
            return this;
        }

        public Builder onShow(Consumer<ShowTask> onShow) {
            this.onShow = onShow;
            return this;
        }

        public Builder onDismiss(BooleanConsumer onDismiss) {
            this.onDismiss = onDismiss;
            return this;
        }

        public ShowTask build(Context context, @NonNull Object data) {
            ShowTask showTask = new ShowTask(displayIndicator == null ? ToastDisplay.class : displayIndicator, context, data) {
                @Override
                protected int getStrategy() {
                    return strategy;
                }

                @Override
                protected boolean rejectExpelled() {
                    return rejectExpelled;
                }

                @Override
                protected boolean rejectDismissed() {
                    return rejectDismissed;
                }

                @Override
                protected boolean expelWaitingTask(ShowTask request) {
                    if (ifExpel != null) {
                        return ifExpel.accept(request);
                    }
                    return false;
                }
            };
            showTask.duration = this.duration;
            if (onShow != null) {
                showTask.addOnShowListener(onShow);
            }
            if (onDismiss != null) {
                showTask.addOnDismissListener(onDismiss);
            }
            return showTask;
        }
    }

    @IntDef({STRATEGY_APPEND_TAIL, STRATEGY_INSERT_HEAD, STRATEGY_SHOW_IMMEDIATELY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Strategy {
    }
}
