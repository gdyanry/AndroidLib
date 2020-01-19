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

    /**
     * @param context
     * @param serviceIntent Identifies the service to connect to.  The Intent must
     *                      specify an explicit component name.
     * @param connectFlags  Operation options for the binding.  May be 0,
     *                      {@link Context#BIND_AUTO_CREATE}, {@link Context#BIND_DEBUG_UNBIND},
     *                      {@link Context#BIND_NOT_FOREGROUND}, {@link Context#BIND_ABOVE_CLIENT},
     *                      {@link Context#BIND_ALLOW_OOM_MANAGEMENT}, or
     *                      {@link Context#BIND_WAIVE_PRIORITY}.
     */
    public ServiceConnectionManager(Context context, Intent serviceIntent, int connectFlags) {
        this.context = context;
        this.serviceIntent = serviceIntent;
        this.connectFlags = connectFlags;
    }

    /**
     * 发起连接。
     */
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

    /**
     * 断开连接。
     */
    public void disconnect() {
        if (isOpen) {
            isOpen = false;
            Logger.getDefault().concat(1, LogLevel.Debug, "unbind: ", serviceIntent.getComponent());
            context.unbindService(this);
        } else {
            Logger.getDefault().ww(serviceIntent.getComponent(), " has already disconnected.");
        }
    }

    /**
     * open状态指的是调用{@link #connect()}之后、直到发生如下事件之间的状态：
     * 调用{@link #disconnect()}；
     * 连接失败且{@link #getReconnectDelay()}返回0；
     * 触发{@link #onNullBinding(ComponentName)}回调；
     * {@link #getService(IBinder)}返回null。
     *
     * @return 当前是否处理open状态。
     */
    public boolean isOpen() {
        return isOpen;
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

    /**
     * open状态中的事件回调。
     *
     * @param event 可取值为{@link #EVENT_BIND_FAILED}、{@link #EVENT_DISCONNECTED}、{@link #EVENT_BINDING_DIED}、
     *              {@link #EVENT_NULL_BINDING}、{@link #EVENT_CONNECTED}、{@link #EVENT_EXIT}。
     */
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
        /*
         * this binding to the service will remain active, and you will receive a call
         * to {@link #onServiceConnected} when the Service is next running.
         */
        service = null;
        if (isOpen) {
            Logger.getDefault().dd("disconnected: ", name);
            onInternalEvent(EVENT_DISCONNECTED);
            // 按照官方文档的说法，当目标服务重新起来后会自动连接，所以这里就不执行重连了
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
