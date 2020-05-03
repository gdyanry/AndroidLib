package yanry.lib.android.model.runner;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import yanry.lib.java.model.runner.Runner;

/**
 * Created by yanry on 2019/12/11.
 */
public class UiScheduleRunner extends Handler implements Runner, Executor {
    public UiScheduleRunner() {
        super(Looper.getMainLooper());
    }

    @Override
    public void run(Runnable runnable) {
        if (Thread.currentThread().equals(getLooper().getThread())) {
            runnable.run();
        } else {
            removeCallbacks(runnable);
            post(runnable);
        }
    }

    @Override
    public void schedule(Runnable runnable, long delay) {
        removeCallbacks(runnable);
        postDelayed(runnable, delay);
    }

    @Override
    public void cancel(Runnable runnable) {
        removeCallbacks(runnable);
    }

    @Override
    public void execute(Runnable command) {
        post(command);
    }
}
