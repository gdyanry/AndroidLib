package lib.android.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import com.phicomm.speaker.constants.yanry.Config;
import com.phicomm.speaker.constants.yanry.WifiSecurity;
import com.phicomm.speaker.interfaces.StreamTransferHook;
import com.phicomm.speaker.model.common.Singletons;
import com.phicomm.speaker.model.common.http.HttpGet;
import com.phicomm.speaker.model.common.http.HttpPost;
import com.phicomm.speaker.util.LogUtils;
import com.phicomm.speaker.util.yanry.ConfigNetUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * @author rongyu.yan
 * @date 2017/9/8
 */

public abstract class ApClient extends BroadcastReceiver {
    private static final long RECONNECT_INTERVAL = 5000;
    private static final int CONNECTED = 1;
    private static final int ADDED = 2;
    private static final int FAIL = 3;

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private String baseAddress;
    private Context context;
    private int connectingNetworkId;
    private TimerTask timerTask;
    private long supplicantStateChangeTimestamp;

    public static int getRssiLevel(int dBm) {
        return WifiManager.calculateSignalLevel(dBm, 3) + 1;
    }

    public void init(Context context) {
        LogUtils.yanry("init.");
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.registerReceiver(this, filter);
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public String getConnectedSsid() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            String extraInfo = networkInfo.getExtraInfo();
            if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return trimSsid(extraInfo);
            }
        }
        return null;
    }

    public void setEnable(boolean enable) {
        if (enable ^ wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(enable);
        } else {
            onWifiStateChange(enable);
        }
    }

    public void scan() {
        if (!onScanResult(wifiManager.getScanResults())) {
            LogUtils.yanry("start scanning...");
            wifiManager.startScan();
        }
    }

    private String trimSsid(String ssid) {
        if (ssid.length() > 1 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    public boolean connect(ScanResult scanResult, String password) {
        return connect(scanResult.SSID, scanResult.BSSID, password, ConfigNetUtil.getWifiSecurity(scanResult.capabilities));
    }

    public boolean connect(String ssid, String bssid, String password, WifiSecurity security) {
        connectingNetworkId = 0;
        LogUtils.yanry("connecting to %s...", ssid);
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (trimSsid(existingConfig.SSID).equals(trimSsid(ssid))) {
                LogUtils.yanry("existing network: %s.", existingConfig.networkId);
                ConfigNetUtil.setupConfiguration(existingConfig, password, security);
                connectingNetworkId = wifiManager.updateNetwork(existingConfig);
                LogUtils.yanry("update network: %s %s.", connectingNetworkId, existingConfig.networkId);
                if (connectingNetworkId <= 0) {
                    connectingNetworkId = existingConfig.networkId;
                }
                break;
            }
        }
        if (connectingNetworkId <= 0) {
            connectingNetworkId = wifiManager.addNetwork(ConfigNetUtil.createWifiConfiguration(ssid, bssid, password, security, true));
            LogUtils.yanry("add network: %s", connectingNetworkId);
        }
        int connect = connect();
        if (connect == ADDED) {
            listenConnection();
        }
        return connect != FAIL;
    }

    public boolean connectToExistingNetwork(String ssid) {
        connectingNetworkId = 0;
        LogUtils.yanry("connecting to %s...", ssid);
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (trimSsid(existingConfig.SSID).equals(trimSsid(ssid))) {
                LogUtils.yanry("existing network: %s.", existingConfig.networkId);
                connectingNetworkId = existingConfig.networkId;
                break;
            }
        }
        int connect = connect();
        if (connect == ADDED) {
            listenConnection();
        }
        return connect != FAIL;
    }

    private int connect() {
        String connectedSsid = getConnectedSsid();
        if (connectedSsid != null) {
            LogUtils.yanry("wifi is connected to %s.", connectedSsid);
            if (onConnected(connectedSsid)) {
                return CONNECTED;
            }
        }
        if (connectingNetworkId > 0) {
            LogUtils.yanry("try enabling network: %s.", connectingNetworkId);
            if (!wifiManager.enableNetwork(connectingNetworkId, true)) {
                LogUtils.yanry("enable network %s fail.", connectingNetworkId);
            } else {
                return ADDED;
            }
        }
        return FAIL;
    }

    public void listenConnection() {
        LogUtils.yanry("listening connection...");
        releaseTimerTask(null);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - supplicantStateChangeTimestamp > RECONNECT_INTERVAL && connect() != ADDED) {
                    releaseTimerTask(this);
                }
            }
        };
        Singletons.get(Timer.class).schedule(timerTask, RECONNECT_INTERVAL, RECONNECT_INTERVAL);
    }

    private void releaseTimerTask(TimerTask task) {
        if (task == null) {
            task = timerTask;
        } else if (timerTask != null) {
            timerTask.cancel();
        }
        if (task != null) {
            LogUtils.yanry("cancel timer task.");
            task.cancel();
        }
        timerTask = null;
    }

    public void release() {
        if (context != null) {
            context.unregisterReceiver(this);
            context = null;
        }
        releaseTimerTask(null);
    }


    public void initHttp() {
        baseAddress = String.format("http://%s:%s", Formatter.formatIpAddress(wifiManager.getDhcpInfo().gateway), Config.AP_PORT);
        LogUtils.yanry("init http: %s", baseAddress);
    }

    public String get(String path, Map<String, Object> urlParams) throws IOException {
        HttpGet httpGet = new HttpGet(String.format("%s/%s", baseAddress, path), urlParams, 0);
        LogUtils.yanry("get: %s", httpGet.getConnection().getURL());
        httpGet.send();
        if (httpGet.isSuccess()) {
            String resp = httpGet.getString(Config.CHARSET);
            LogUtils.yanry("get response: %s", resp);
            return resp;
        } else {
            LogUtils.yanry("http fail on status: %s", httpGet.getConnection().getResponseCode());
            return null;
        }
    }

    public String post(String path, String data) throws IOException {
        HttpPost post = new HttpPost(String.format("%s/%s", baseAddress, path), null) {
            @Override
            protected StreamTransferHook getUploadHook() {
                return null;
            }
        };
        LogUtils.yanry("post: %s (%s)", post.getConnection().getURL(), data);
        post.send(data.getBytes(Config.CHARSET));
        if (post.isSuccess()) {
            String resp = post.getString(Config.CHARSET);
            LogUtils.yanry("post response: %s", resp);
            return resp;
        } else {
            LogUtils.yanry("http fail on status: %s", post.getConnection().getResponseCode());
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                boolean enabled = wifiManager.isWifiEnabled();
                LogUtils.yanry("wifi state change: enabled=%s.", enabled);
                onWifiStateChange(enabled);
                break;

            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                scan();
                break;

            case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                supplicantStateChangeTimestamp = System.currentTimeMillis();
                SupplicantState newState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (newState == SupplicantState.COMPLETED) {
                    supplicantStateChangeTimestamp -= RECONNECT_INTERVAL;
                }
                LogUtils.yanry("supplicant state change: %s.", newState);
                onSupplicantStateChange(newState);
                break;
            default:
                break;
        }
    }

    /**
     * @param scanResults
     * @return return false to trigger wifi scan action immediately.
     */
    protected abstract boolean onScanResult(List<ScanResult> scanResults);

    /**
     * Note that this method is called on worker thread.
     *
     * @param ssid
     * @return return true to release listening connectivity event.
     */
    protected abstract boolean onConnected(String ssid);

    protected abstract void onSupplicantStateChange(SupplicantState state);

    protected abstract void onWifiStateChange(boolean isEnabled);
}
