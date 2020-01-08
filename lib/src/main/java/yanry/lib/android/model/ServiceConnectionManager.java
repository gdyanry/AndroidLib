package yanry.lib.android.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;

/**
 * Created by yanry on 2020/1/8.
 */
public abstract class ServiceConnectionManager implements ServiceConnection, Runnable {
    public static final int FAIL_ON_BIND = 1;
    public static final int FAIL_ON_DISCONNECTED = 2;
    public static final int FAIL_ON_DIED = 3;
    public static final int FAIL_ON_NULL = 4;

    private Context context;
    private Intent serviceIntent;
    private int connectFlags;
    private boolean isOpen;
    private boolean isConnected;

    public ServiceConnectionManager(Context context, Intent serviceIntent, int connectFlags) {
        this.context = context;
        this.serviceIntent = serviceIntent;
        this.connectFlags = connectFlags;
    }

    public void connect() {
        if (isOpen) {
            Logger.getDefault().ww(serviceIntent.getComponent(), " has already connected.");
        } else {
            isOpen = true;
            Logger.getDefault().concat(1, LogLevel.Debug, "bind: ", serviceIntent.getComponent());
            doConnect();
        }
    }

    private void doConnect() {
        CommonUtils.cancelPendingTimeout(this);
        run();
    }

    public void disconnect() {
        if (isOpen) {
            isOpen = false;
            Logger.getDefault().concat(1, LogLevel.Debug, "unbind: ", serviceIntent.getComponent());
            context.unbindService(this);
        } else {
            Logger.getDefault().ww(serviceIntent.getComponent(), " has already disconnected.");
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isConnected() {
        return isConnected;
    }

    protected abstract void onConnected(IBinder service);

    protected abstract void onConnectFail(int type);

    protected abstract long getReconnectDelay();

    @Override
    public void run() {
        if (isOpen && !isConnected) {
            boolean bindService = context.bindService(serviceIntent, this, connectFlags);
            if (!bindService) {
                Logger.getDefault().ww("bind service fail: ", serviceIntent.getComponent());
                onConnectFail(FAIL_ON_BIND);
                long delay = getReconnectDelay();
                if (delay > 0) {
                    Logger.getDefault().vv("will retry connect in ", delay, " ms.");
                    CommonUtils.scheduleTimeout(this, delay);
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        isConnected = true;
        if (isOpen) {
            Logger.getDefault().dd("connected: ", name);
            onConnected(service);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isConnected = false;
        if (isOpen) {
            Logger.getDefault().dd("disconnected: ", name);
            onConnectFail(FAIL_ON_DISCONNECTED);
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        isConnected = false;
        if (isOpen) {
            Logger.getDefault().dd("binding died: ", name);
            onConnectFail(FAIL_ON_DIED);
            context.unbindService(this);
            doConnect();
        }
    }

    @Override
    public void onNullBinding(ComponentName name) {
        Logger.getDefault().ee("null binding: ", name);
        onConnectFail(FAIL_ON_NULL);
        disconnect();
    }
}
