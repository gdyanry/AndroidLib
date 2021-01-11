package yanry.lib.android.model.runner;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.runner.Runner;

/**
 * Created by yanry on 2019/12/11.
 */
public class UiRunner extends Handler implements Runner, Executor {
    public UiRunner() {
        super(Looper.getMainLooper());
    }

    @Override
    public void run(Runnable runnable) {
        removeCallbacks(runnable);
        if (Thread.currentThread().equals(getLooper().getThread())) {
            runnable.run();
        } else {
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
    public void terminate() {
        Logger.getDefault().concat(LogLevel.Warn, "ui runner is not supposed to be terminated.");
    }

    @Override
    public void execute(Runnable command) {
        post(command);
    }
}
