package lib.android.view.pop;

import android.content.Context;

import lib.android.interfaces.Filter;
import lib.common.model.log.Logger;

/**
 * 要显示的数据。一般推荐使用Builder来创建对象（简单），当需要动态配置显示策略时才直接使用构造函数并实现抽象方法创建对象（灵活）。
 */
public abstract class ShowTask implements Runnable {
    protected static final int STRATEGY_INSERT_HEAD = 1;
    protected static final int STRATEGY_SHOW_IMMEDIATELY = 2;

    Context context;
    Object data;
    long duration;
    DataViewHandler handler;
    PopDataManager manager;

    public ShowTask(Context context, Object data, long duration) {
        this.context = context;
        this.data = data;
        this.duration = duration;
    }

    public static Builder builder() {
        return new Builder();
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

    public DataViewHandler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        // dismiss
        if (manager.currentTask == this) {
            manager.currentTask = null;
            Logger.getDefault().v("dismiss on timeout: %s", data);
            handler.dismiss();
            manager.loop();
        }
    }

    protected abstract int getStrategy();

    protected abstract boolean rejectExpelled();

    protected abstract boolean rejectDismissed();

    protected abstract boolean expelWaitingTask(ShowTask task);

    /**
     * 默认构造的ShowTask不拒绝从队列被清除或者被替换显示，如果当前有正在显示的数据界面，则加入队列尾部等待。
     */
    public static class Builder {
        private int strategy;
        private boolean rejectExpelled;
        private boolean rejectDismissed;
        private Filter<ShowTask> ifExpel;

        private Builder() {
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

        public ShowTask build(Context context, Object data, long duration) {
            return new ShowTask(context, data, duration) {
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
            };
        }
    }
}
