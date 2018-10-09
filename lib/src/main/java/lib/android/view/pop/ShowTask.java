package lib.android.view.pop;

import android.content.Context;

import lib.android.entity.MainHandler;
import lib.android.interfaces.BooleanConsumer;
import lib.android.interfaces.Filter;
import lib.android.view.pop.handler.ToastHandler;
import lib.common.model.Singletons;
import lib.common.model.log.Logger;

/**
 * 要显示的数据。一般推荐使用Builder来创建对象（简单），当需要动态配置显示策略时才直接使用构造函数并实现抽象方法创建对象（灵活）。
 */
public abstract class ShowTask implements Runnable {
    protected static final int STRATEGY_INSERT_HEAD = 1;
    protected static final int STRATEGY_SHOW_IMMEDIATELY = 2;

    Object typeId;
    Context context;
    Object data;
    long duration;
    DataViewHandler handler;
    PopDataManager manager;

    /**
     *
     * @param typeId
     * @param context
     * @param data
     * @param duration 该数据的显示时间，若为0则一直显示。
     */
    public ShowTask(Object typeId, Context context, Object data, long duration) {
        this.typeId = typeId;
        this.context = context;
        this.data = data;
        this.duration = duration;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public void dismiss() {
        Singletons.get(MainHandler.class).removeCallbacks(this);
        if (doDismiss()) {
            Logger.getDefault().v("manually dismiss: %s", data);
        }
    }

    private boolean doDismiss() {
        if (manager.currentTask == this) {
            manager.currentTask = null;
            handler.dismiss();
            onDismiss(true);
            manager.loop();
            return true;
        }
        return false;
    }

    public Object getTypeId() {
        return typeId;
    }

    public Context getContext() {
        return context;
    }

    public Object getData() {
        return data;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public void run() {
        Logger.getDefault().v("dismiss on timeout: %s", data);
        doDismiss();
    }

    protected abstract int getStrategy();

    protected abstract boolean rejectExpelled();

    protected abstract boolean rejectDismissed();

    protected abstract boolean expelWaitingTask(ShowTask task);

    protected abstract void onShow();

    protected abstract void onDismiss(boolean isFromInternal);

    /**
     * 默认构造的ShowTask不拒绝从队列被清除或者被替换显示，如果当前有正在显示的数据界面，则加入队列尾部等待。
     */
    public static class Builder {
        private Object typeId;
        private long duration;
        private int strategy;
        private boolean rejectExpelled;
        private boolean rejectDismissed;
        private Filter<ShowTask> ifExpel;
        private Runnable onShow;
        private BooleanConsumer onDismiss;

        private Builder() {
        }

        public Builder type(Object typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder toast() {
            this.typeId = ToastHandler.class;
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

        public Builder onShow(Runnable onShow) {
            this.onShow = onShow;
            return this;
        }

        public Builder onDismiss(BooleanConsumer onDismiss) {
            this.onDismiss = onDismiss;
            return this;
        }

        public ShowTask build(Context context, Object data) {
            return new ShowTask(typeId == null ? data : typeId, context, data, duration) {
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
                protected boolean expelWaitingTask(ShowTask task) {
                    if (ifExpel != null) {
                        return ifExpel.accept(task);
                    }
                    return false;
                }

                @Override
                protected void onShow() {
                    if (onShow != null) {
                        onShow.run();
                    }
                }

                @Override
                protected void onDismiss(boolean isFromInternal) {
                    if (onDismiss != null) {
                        onDismiss.accept(isFromInternal);
                    }
                }
            };
        }
    }
}
