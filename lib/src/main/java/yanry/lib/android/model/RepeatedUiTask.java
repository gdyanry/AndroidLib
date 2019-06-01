package yanry.lib.android.model;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.Singletons;

/**
 * rongyu.yan
 * 2019/5/27
 **/
public abstract class RepeatedUiTask implements Runnable {
    private boolean isActive;
    private long period;

    public RepeatedUiTask(long period) {
        this.period = period;
    }

    public void start() {
        if (!isActive) {
            isActive = true;
            CommonUtils.cancelPendingTimeout(this);
            Singletons.get(MainHandler.class).post(this);
        }
    }

    public void stop() {
        if (isActive) {
            isActive = false;
            CommonUtils.cancelPendingTimeout(this);
        }
    }

    @Override
    public void run() {
        if (isActive) {
            doRun();
            CommonUtils.scheduleTimeout(this, period);
        }
    }

    protected abstract void doRun();
}
