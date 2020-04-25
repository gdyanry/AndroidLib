package yanry.lib.android.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.NonNull;

import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.runner.Runner;
import yanry.lib.java.model.watch.BooleanHolder;
import yanry.lib.java.model.watch.BooleanHolderImpl;
import yanry.lib.java.model.watch.BooleanWatcher;

/**
 * Created by yanry on 2020/1/8.
 */
public abstract class AidlConnector<S extends IInterface> implements ServiceConnection, Runnable {
    private Runner runner;
    private Context context;
    private Intent serviceIntent;
    private int connectFlags;
    private S service;
    private Availability availability;
    private BooleanHolderImpl isAlive;

    /**
     * @param runner
     * @param context
     * @param serviceIntent Identifies the service to connect to.  The Intent must
     *                      specify an explicit component name.
     * @param connectFlags  Operation options for the binding.  May be 0,
     *                      {@link Context#BIND_AUTO_CREATE}, {@link Context#BIND_DEBUG_UNBIND},
     *                      {@link Context#BIND_NOT_FOREGROUND}, {@link Context#BIND_ABOVE_CLIENT},
     *                      {@link Context#BIND_ALLOW_OOM_MANAGEMENT}, or
     *                      {@link Context#BIND_WAIVE_PRIORITY}.
     */
    public AidlConnector(@NonNull Runner runner, @NonNull Context context, @NonNull Intent serviceIntent, int connectFlags) {
        this.runner = runner;
        this.context = context;
        this.serviceIntent = serviceIntent;
        this.connectFlags = connectFlags;
        isAlive = new BooleanHolderImpl();
        availability = new Availability();
        isAlive.addWatcher(availability);
    }

    /**
     * 发起连接。
     */
    public void connect(long delay) {
        if (isAlive.setValue(true)) {
            Logger.getDefault().concat(1, LogLevel.Debug, "bind: ", serviceIntent);
            doConnect(delay);
        } else {
            Logger.getDefault().ww(serviceIntent, " has already connected.");
        }
    }

    private void doConnect(long delay) {
        runner.scheduleTimeout(this, delay);
    }

    /**
     * 断开连接。
     */
    public void disconnect() {
        if (isAlive.setValue(false)) {
            Logger.getDefault().concat(1, LogLevel.Debug, "unbind: ", serviceIntent);
            context.unbindService(this);
        } else {
            Logger.getDefault().ww(serviceIntent, " has already disconnected.");
        }
    }

    public BooleanHolder getIsAlive() {
        return isAlive;
    }

    public BooleanHolder getAvailability() {
        return availability;
    }

    public S getService() {
        return service;
    }

    /**
     * 根据IBinder对象返回目标service。对于AIDL连接应返回AIDLService.Stub.asInterface(iBinder)。
     *
     * @param service
     * @return 目标service。
     */
    protected abstract S getService(IBinder service);

    protected abstract long getReconnectDelay();

    @Override
    public final void run() {
        if (isAlive.getValue() && !context.bindService(serviceIntent, this, connectFlags)) {
            Logger.getDefault().ww("bind service fail: ", serviceIntent);
            long delay = getReconnectDelay();
            if (delay > 0) {
                Logger.getDefault().vv("will retry connect in ", delay, " ms.");
                runner.scheduleTimeout(this, delay);
            } else {
                isAlive.setValue(false);
            }
        }
    }

    @Override
    public final void onServiceConnected(ComponentName name, IBinder service) {
        if (isAlive.getValue()) {
            Logger.getDefault().dd("connected: ", name);
            this.service = getService(service);
            if (this.service != null) {
                availability.setValue(true);
            } else {
                Logger.getDefault().ee("cannot get service: ", name);
                isAlive.setValue(false);
                context.unbindService(this);
            }
        }
    }

    @Override
    public final void onServiceDisconnected(ComponentName name) {
        /*
         * this binding to the service will remain active, and you will receive a call
         * to {@link #onServiceConnected} when the Service is next running.
         */
        if (isAlive.getValue()) {
            Logger.getDefault().dd("disconnected: ", name);
            availability.setValue(false);
            // 按照官方文档的说法，当目标服务重新起来后会自动连接，所以这里就不执行重连了
        }
    }

    @Override
    public final void onBindingDied(ComponentName name) {
        if (isAlive.getValue()) {
            Logger.getDefault().dd("binding died: ", name);
            availability.setValue(false);
            context.unbindService(this);
            doConnect(0);
        }
    }

    @Override
    public final void onNullBinding(ComponentName name) {
        if (isAlive.setValue(false)) {
            Logger.getDefault().ee("null binding: ", name);
            context.unbindService(this);
        }
    }

    private class Availability extends BooleanHolderImpl implements BooleanWatcher {
        @Override
        public void onValueChange(boolean to) {
            if (!to) {
                setValue(false);
                service = null;
            }
        }
    }
}
