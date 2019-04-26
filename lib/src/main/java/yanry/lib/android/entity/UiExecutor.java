package yanry.lib.android.entity;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

import yanry.lib.java.model.Singletons;

/**
 * rongyu.yan
 * 2019/4/23
 **/
public class UiExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable command) {
        Singletons.get(MainHandler.class).post(command);
    }
}
