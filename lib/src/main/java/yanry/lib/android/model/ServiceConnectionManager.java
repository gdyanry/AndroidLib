package yanry.lib.android.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;

/**
 * Created by yanry on 2020/1/8.
 */
public abstract class ServiceConnectionManager<S extends IInterface> implements ServiceConnection, Runnable {
    public static final int EVENT_BIND_FAILED = 1;
    public static final int EVENT_DISCONNECTED = 2;
    public static final int EVENT_BINDING_DIED = 3;
    public static final int EVENT_NULL_BINDING = 4;
    public static final int EVENT_CONNECTED = 5;
    public static final int EVENT_EXIT = 6;

    private Context context;
    private Intent serviceIntent;
    private int connectFlags;
    private boolean isOpen;
    private S service;

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

    public S getService() {
        return service;
    }

    protected abstract S getService(IBinder service);

    protected abstract void onInternalEvent(int event);

    protected abstract long getReconnectDelay();

    @Override
    public void run() {
        if (isOpen && service == null) {
            boolean bindService = context.bindService(serviceIntent, this, connectFlags);
            if (!bindService) {
                Logger.getDefault().ww("bind service fail: ", serviceIntent.getComponent());
                onInternalEvent(EVENT_BIND_FAILED);
                long delay = getReconnectDelay();
                if (delay > 0) {
                    Logger.getDefault().vv("will retry connect in ", delay, " ms.");
                    CommonUtils.scheduleTimeout(this, delay);
                } else {
                    onInternalEvent(EVENT_EXIT);
                    isOpen = false;
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (isOpen) {
            Logger.getDefault().dd("connected: ", name);
            this.service = getService(service);
            if (this.service != null) {
                onInternalEvent(EVENT_CONNECTED);
            } else {
                Logger.getDefault().ee("cannot get service: ", name);
                onInternalEvent(EVENT_EXIT);
                isOpen = false;
                context.unbindService(this);
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        if (isOpen) {
            Logger.getDefault().dd("disconnected: ", name);
            onInternalEvent(EVENT_DISCONNECTED);
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        service = null;
        if (isOpen) {
            Logger.getDefault().dd("binding died: ", name);
            onInternalEvent(EVENT_BINDING_DIED);
            context.unbindService(this);
            doConnect();
        }
    }

    @Override
    public void onNullBinding(ComponentName name) {
        if (isOpen) {
            Logger.getDefault().ee("null binding: ", name);
            onInternalEvent(EVENT_NULL_BINDING);
            onInternalEvent(EVENT_EXIT);
            isOpen = false;
            context.unbindService(this);
        }
    }
}
