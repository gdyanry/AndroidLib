package lib.android.view.pop;

import android.content.Context;
import android.support.annotation.NonNull;

import lib.android.util.CommonUtils;
import lib.common.model.log.Logger;

/**
 * 为特定数据显示特定界面。
 *
 * @param <D> data type.
 * @param <V> view type.
 */
public abstract class Display<D, V> {
    PopScheduler scheduler;
    protected V popInstance;

    Display() {
    }

    public ShowTask getShowingTask() {
        if (scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            return scheduler.current;
        }
        return null;
    }

    /**
     * 此数据界面被提前关闭（非超时，比如由用户按返回键触发）时需要调用此方法通知显示队列中等待的数据，否则队列中下一条数据要等到前一条数据超时时间后才会显示。
     */
    public boolean notifyDismiss(V popInstance) {
        if (popInstance == this.popInstance && scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            ShowTask currentTask = scheduler.current;
            scheduler.current = null;
            this.popInstance = null;
            Logger.getDefault().vv(currentTask.data);
            CommonUtils.cancelPendingTimeout(currentTask);
            currentTask.onDismiss(false);
            scheduler.loop(null);
            return true;
        }
        return false;
    }

    public void dismiss(long delay) {
        if (scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            scheduler.current.dismiss(delay);
        }
    }

    protected void internalDismiss() {
        if (popInstance != null) {
            Logger.getDefault().vv(popInstance);
            dismiss(popInstance);
        }
        popInstance = null;
    }

    final boolean isShowing() {
        if (popInstance != null) {
            return isShowing(popInstance);
        }
        return false;
    }

    protected abstract boolean accept(Object handlerIndicator);

    protected abstract void show(Context context, D data);

    protected abstract void dismiss(V popInstance);

    protected abstract boolean isShowing(@NonNull V popInstance);
}
