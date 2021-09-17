package yanry.lib.android.model.runner;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.concurrent.Executor;

import yanry.lib.java.model.runner.Runner;

/**
 * Created by yanry on 2019/12/12.
 */
public class HandlerThreadRunner extends HandlerThread implements Runner, Executor {
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

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void run(Runnable runnable) {
        handler.removeCallbacks(runnable);
        if (Thread.currentThread().equals(this)) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    @Override
    public void schedule(Runnable runnable, long delay) {
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, delay);
    }

    @Override
    public void cancel(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    @Override
    public void terminate() {
        getLooper().quitSafely();
    }

    @Override
    public void execute(Runnable command) {
        handler.post(command);
    }
}
