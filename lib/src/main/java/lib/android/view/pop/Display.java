package lib.android.view.pop;

import android.content.Context;

import lib.android.util.CommonUtils;
import lib.common.model.log.Logger;

/**
 * 为特定数据显示特定界面。
 * @param <D> data type.
 * @param <V> view type.
 */
public abstract class Display<D, V> {
    PopScheduler scheduler;
    private V popInstance;

    public ShowTask getShowingTask() {
        if (scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            return scheduler.current;
        }
        return null;
    }

    /**
     * 此数据界面被提前关闭（非超时，比如由用户按返回键触发）时需要调用此方法通知显示队列中等待的数据，否则队列中下一条数据要等到前一条数据超时时间后才会显示。
     */
    public void notifyDismiss() {
        if (scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            Logger.getDefault().vv(scheduler.current.data);
            CommonUtils.cancelPendingTimeout(scheduler.current);
            scheduler.current.onDismiss(false);
            scheduler.current = null;
            scheduler.loop();
        }
    }

    public void dismiss() {
        if (scheduler != null && scheduler.current != null && scheduler.current.display == this) {
            scheduler.current.dismiss();
        }
    }

    final void show(Context context, Object data) {
        popInstance = showData(popInstance, context, (D) data);
    }

    final void internalDismiss() {
        if (popInstance != null) {
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

    /**
     * @param currentInstance may be null.
     * @param context
     * @param data
     * @return
     */
    protected abstract V showData(V currentInstance, Context context, D data);

    protected abstract void dismiss(V popInstance);

    protected abstract boolean isShowing(V popInstance);
}
