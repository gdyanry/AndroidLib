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

/**
 * 要显示的数据。一般推荐使用Builder来创建对象（简单），当需要动态配置显示策略时才直接使用构造函数并实现抽象方法创建对象（灵活）。
 */
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

    public ShowData(Context context) {
        this.context = context;
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
                Logger.getDefault().vv("manually dismiss: ", this);
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

    @Override
    public void run() {
        if (doDismiss()) {
            Logger.getDefault().vv("dismiss on timeout: ", this);
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
