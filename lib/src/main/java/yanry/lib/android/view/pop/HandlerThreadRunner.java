package yanry.lib.android.view.pop;

import android.os.Handler;
import android.os.HandlerThread;

import yanry.lib.java.model.schedule.ScheduleRunner;

/**
 * Created by yanry on 2019/12/12.
 */
public class HandlerThreadRunner extends HandlerThread implements ScheduleRunner {
    private Handler handler;

    public HandlerThreadRunner(String name) {
        super(name);
        init();
    }

    public HandlerThreadRunner(String name, int priority) {
        super(name, priority);
        init();
    }

    private void init() {
        start();
        handler = new Handler(getLooper());
    }

    @Override
    public void run(Runnable runnable) {
        if (Thread.currentThread().equals(this)) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    @Override
    public void scheduleTimeout(Runnable runnable, long delay) {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, delay);
    }

    @Override
    public void cancelPendingTimeout(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}
