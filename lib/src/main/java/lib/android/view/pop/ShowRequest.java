package lib.android.view.pop;

import android.content.Context;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import lib.android.interfaces.BooleanConsumer;
import lib.android.interfaces.Consumer;
import lib.android.interfaces.Filter;
import lib.android.util.CommonUtils;
import lib.android.view.pop.display.ToastDisplay;
import lib.common.model.log.Logger;

/**
 * 要显示的数据。一般推荐使用Builder来创建对象（简单），当需要动态配置显示策略时才直接使用构造函数并实现抽象方法创建对象（灵活）。
 */
public abstract class ShowRequest implements Runnable {
    public static final int STRATEGY_APPEND_TAIL = 0;
    public static final int STRATEGY_INSERT_HEAD = 1;
    public static final int STRATEGY_SHOW_IMMEDIATELY = 2;

    Object displayIndicator;
    Context context;
    Object data;
    /**
     * 该数据的显示时间，若为0则一直显示
     */
    long duration;
    Object tag;
    Display display;
    PopScheduler scheduler;

    /**
     * @param displayIndicator
     * @param context
     * @param data
     */
    public ShowRequest(Object displayIndicator, Context context, Object data) {
        this.displayIndicator = displayIndicator;
        this.context = context;
        this.data = data;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public void dismiss() {
        CommonUtils.cancelPendingTimeout(this);
        if (doDismiss()) {
            Logger.getDefault().vv("manually dismiss: ", data);
        }
    }

    private boolean doDismiss() {
        if (scheduler.current == this) {
            scheduler.current = null;
            onDismiss(true);
            display.internalDismiss();
            scheduler.loop();
            return true;
        }
        return false;
    }

    public ShowRequest setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public ShowRequest setTag(Object tag) {
        this.tag = tag;
        return this;
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

    protected abstract boolean expelWaitingTask(ShowRequest request);

    protected abstract void onShow();

    protected abstract void onDismiss(boolean isFromInternal);

    /**
     * 默认构造的ShowTask不拒绝从队列被清除或者被替换显示，如果当前有正在显示的数据界面，则加入队列尾部等待。
     */
    public static class Builder {
        private Object displayIndicator;
        private int strategy;
        private boolean rejectExpelled;
        private boolean rejectDismissed;
        private Filter<ShowRequest> ifExpel;
        private Consumer<ShowRequest> onShow;
        private BooleanConsumer onDismiss;

        private Builder() {
        }

        public Builder displayIndicator(Object displayIndicator) {
            this.displayIndicator = displayIndicator;
            return this;
        }

        public Builder toast() {
            this.displayIndicator = ToastDisplay.class;
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

        public Builder expelWaitingTasks(Filter<ShowRequest> ifExpel) {
            this.ifExpel = ifExpel;
            return this;
        }

        public Builder onShow(Consumer<ShowRequest> onShow) {
            this.onShow = onShow;
            return this;
        }

        public Builder onDismiss(BooleanConsumer onDismiss) {
            this.onDismiss = onDismiss;
            return this;
        }

        public ShowRequest build(Context context, Object data) {
            ShowRequest request = new ShowRequest(displayIndicator == null ? data : displayIndicator, context, data) {
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
                protected boolean expelWaitingTask(ShowRequest request) {
                    if (ifExpel != null) {
                        return ifExpel.accept(request);
                    }
                    return false;
                }

                @Override
                protected void onShow() {
                    if (onShow != null) {
                        onShow.accept(this);
                    }
                }

                @Override
                protected void onDismiss(boolean isFromInternal) {
                    if (onDismiss != null) {
                        onDismiss.accept(isFromInternal);
                    }
                }
            };
            return request;
        }
    }

    @IntDef({STRATEGY_APPEND_TAIL, STRATEGY_INSERT_HEAD, STRATEGY_SHOW_IMMEDIATELY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Strategy {
    }
}
