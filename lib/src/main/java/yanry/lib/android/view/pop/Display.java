package yanry.lib.android.view.pop;

import android.content.Context;
import android.support.annotation.NonNull;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.log.Logger;

/**
 * 为特定数据显示特定界面。
 *
 * @param <D> data type.
 * @param <V> view type.
 */
public abstract class Display<D extends ShowData, V> {
    private PopScheduler scheduler;
    private V popInstance;

    protected Display() {
    }

    void setScheduler(PopScheduler scheduler) {
        this.scheduler = scheduler;
    }

    protected V getPopInstance() {
        return popInstance;
    }

    protected void setPopInstance(V popInstance) {
        this.popInstance = popInstance;
    }

    public ShowData getShowingData() {
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
            ShowData currentTask = scheduler.current;
            scheduler.current = null;
            setPopInstance(null);
            Logger.getDefault().vv(currentTask);
            CommonUtils.cancelPendingTimeout(currentTask);
            currentTask.onDismiss(false);
            scheduler.rebalance(null, null);
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
            dismiss(popInstance);
            setPopInstance(null);
        }
    }

    public final boolean isShowing() {
        if (popInstance != null) {
            return isShowing(popInstance);
        }
        return false;
    }

    protected abstract void show(Context context, @NonNull D data);

    protected abstract void dismiss(V popInstance);

    protected abstract boolean isShowing(@NonNull V popInstance);
}
