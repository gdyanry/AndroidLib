package yanry.lib.android.model;

import yanry.lib.java.model.runner.Runner;

/**
 * rongyu.yan
 * 2019/5/27
 **/
public abstract class RepeatedTask implements Runnable {
    private Runner runner;
    private boolean isActive;
    private long period;

    public RepeatedTask(Runner runner, long period) {
        this.runner = runner;
        this.period = period;
    }

    public void start() {
        if (!isActive) {
            isActive = true;
            runner.cancelPendingTimeout(this);
            runner.run(this);
        }
    }

    public void stop() {
        if (isActive) {
            isActive = false;
            runner.cancelPendingTimeout(this);
        }
    }

    @Override
    public final void run() {
        if (isActive) {
            doRun();
            runner.scheduleTimeout(this, period);
        }
    }

    protected abstract void doRun();
}
