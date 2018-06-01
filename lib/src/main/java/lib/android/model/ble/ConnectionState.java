package lib.android.model.ble;

/**
 * Created by rongyu.yan on 2017/9/18.
 */

public enum ConnectionState {
    //已连接
    Connected,
    //已取消连接
    Disconnected,
    //正在连接
    Connecting,
    //正在取消连接
    Disconnecting
}
