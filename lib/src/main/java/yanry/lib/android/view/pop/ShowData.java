package yanry.lib.android.view.pop;

import android.content.Context;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.LinkedList;

import yanry.lib.android.interfaces.BooleanConsumer;
import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.log.Logger;

public class ShowData implements Runnable {
    public static final int STRATEGY_APPEND_TAIL = 0;
    public static final int STRATEGY_INSERT_HEAD = 1;
    public static final int STRATEGY_SHOW_IMMEDIATELY = 2;

    Object extra;
    long duration;
    Context context;
    PopScheduler scheduler;
    Object tag;
    private LinkedList<Runnable> onShowListeners;
    private LinkedList<BooleanConsumer> onDismissListeners;
    Display display;
    int priority;

    public ShowData(Context context) {
        this.context = context;
        onShowListeners = new LinkedList<>();
        onDismissListeners = new LinkedList<>();
    }

    public boolean isScheduled() {
        return display != null;
    }

    public boolean isShowing() {
        return scheduler != null && scheduler.current == this;
    }

    public void dismiss(long delay) {
        if (scheduler != null && scheduler.current == this) {
            if (delay > 0) {
                CommonUtils.scheduleTimeout(this, delay);
            } else {
                CommonUtils.cancelPendingTimeout(this);
                CommonUtils.runOnUiThread(this);
            }
        }
    }

    public ShowData setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public ShowData setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    public ShowData setExtra(Object extra) {
        this.extra = extra;
        return this;
    }

    public ShowData setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public Object getExtra() {
        return extra;
    }

    /**
     * Add callback after this task has been shown.
     */
    public ShowData addOnShowListener(Runnable listener) {
        onShowListeners.add(listener);
        return this;
    }

    /**
     * 回调的boolean参数只有当dismiss事件由外部触发后并且调用了{@link Display#notifyDismiss(Object)}的情况下才为false。
     */
    public ShowData addOnDismissListener(BooleanConsumer listener) {
        onDismissListeners.add(listener);
        return this;
    }

    final void onShow() {
        for (Runnable listener : onShowListeners) {
            listener.run();
        }
    }

    final void onDismiss(boolean isBeforeDismiss) {
        for (BooleanConsumer listener : onDismissListeners) {
            listener.accept(isBeforeDismiss);
        }
    }


    @Strategy
    protected int getStrategy() {
        return STRATEGY_SHOW_IMMEDIATELY;
    }

    protected boolean rejectExpelled() {
        return false;
    }

    protected boolean rejectDismissed() {
        return false;
    }

    protected boolean expelWaitingTask(ShowData request) {
        return false;
    }

    /**
     * 当数据从队列中取出显示时回调此方法，如果返回false则不显示直接丢弃
     *
     * @return
     */
    protected boolean isValidOnDequeue() {
        return true;
    }

    @Override
    public final void run() {
        if (scheduler != null && scheduler.current == this) {
            scheduler.current = null;
            Logger.getDefault().vv("dismiss: ", this);
            onDismiss(true);
            HashSet<Display> displaysToDismiss = new HashSet<>();
            displaysToDismiss.add(display);
            scheduler.rebalance(null, displaysToDismiss);
        }
    }

    @Override
    public String toString() {
        return extra == null ? super.toString() : extra.toString();
    }

    @IntDef({STRATEGY_APPEND_TAIL, STRATEGY_INSERT_HEAD, STRATEGY_SHOW_IMMEDIATELY})
    @Retention(RetentionPolicy.SOURCE)
    @interface Strategy {
    }
}
