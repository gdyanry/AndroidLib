package yanry.lib.android.entity;

import android.os.Build;
import android.os.Handler;
import android.os.MessageQueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;

import yanry.lib.java.interfaces.CommonCallback;

/**
 * 在目标线程空闲时才执行的非紧急任务
 */
public interface NonEmergencyAction extends MessageQueue.IdleHandler, Runnable, CommonCallback {
    default void scheduleExecution() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getTargetHandler().getLooper().getQueue().addIdleHandler(this);
        } else {
            getTargetHandler().post(this);
        }
    }

    /**
     * 获取目标线程的Handler对象
     *
     * @return
     */
    @NonNull
    Handler getTargetHandler();

    /**
     * 最终执行此任务的Executor对象，注意返回的Executor不可以包含目标线程，若希望在目标线程中执行此任务，则返回null即可。
     *
     * @return
     */
    @Nullable
    Executor getExecutor();

    @Override
    default boolean queueIdle() {
        Executor executor = getExecutor();
        if (executor == null) {
            callback();
        } else {
            executor.execute(this);
        }
        return false;
    }

    @Override
    default void run() {
        if (Thread.currentThread().equals(getTargetHandler().getLooper().getThread())) {
            Executor executor = getExecutor();
            if (executor != null) {
                executor.execute(this);
            } else {
                callback();
            }
        } else {
            callback();
        }
    }
}
