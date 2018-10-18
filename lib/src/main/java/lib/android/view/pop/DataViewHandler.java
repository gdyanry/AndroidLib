package lib.android.view.pop;

import android.content.Context;

import lib.common.model.log.Logger;

/**
 * 为特定数据显示特定界面。
 * @param <D>
 * @param <V>
 */
public abstract class DataViewHandler<D, V> {
    PopDataManager manager;
    private V popInstance;

    /**
     * 此数据界面被提前关闭（非超时，比如由用户按返回键触发）时需要调用此方法通知显示队列中等待的数据，否则队列中下一条数据要等到前一条数据超时时间后才会显示。
     */
    public void notifyDismiss() {
        if (manager != null && manager.currentTask != null && manager.currentTask.handler == this) {
            Logger.getDefault().v(manager.currentTask.data);
            manager.currentTask.onDismiss(false);
            manager.currentTask = null;
            manager.loop();
        }
    }

    public void dismiss() {
        if (manager != null && manager.currentTask != null && manager.currentTask.handler == this) {
            manager.currentTask.dismiss();
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

    protected abstract boolean accept(Object typeId);

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
