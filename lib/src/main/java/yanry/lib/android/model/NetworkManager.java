package yanry.lib.android.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.BooleanHolder;
import yanry.lib.java.model.watch.BooleanHolderImpl;

/**
 * need permission
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.INTERNET" />
 * <p>
 * Created by yanry on 2020/6/17.
 */
public class NetworkManager {
    private ConnectivityManager manager;
    private BooleanHolderImpl internetAvailability;

    public NetworkManager(Context context) {
        manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = getActiveNetworkInfo();
        Logger.getDefault().ii("active network info: ", networkInfo);
        internetAvailability = new BooleanHolderImpl(networkInfo != null && networkInfo.isConnected());
        manager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), new Callback());
    }

    public ConnectivityManager getConnectivityManager() {
        return manager;
    }

    public BooleanHolder getInternetAvailability() {
        return internetAvailability;
    }

    public NetworkInfo getActiveNetworkInfo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = manager.getActiveNetwork();
            if (activeNetwork != null) {
                return manager.getNetworkInfo(activeNetwork);
            }
        }
        return manager.getActiveNetworkInfo();
    }

    /**
     * @param transportType 详见NetworkCapabilities.TRANSPORT_常量
     * @return
     */
    public boolean hasTransport(int transportType) {
        for (Network network : manager.getAllNetworks()) {
            NetworkCapabilities networkCapabilities = manager.getNetworkCapabilities(network);
            if (networkCapabilities.hasTransport(transportType)) {
                return true;
            }
        }
        return false;
    }

    private class Callback extends ConnectivityManager.NetworkCallback implements Runnable {
        private int retryCount;

        @Override
        public void onAvailable(Network network) {
            Logger.getDefault().dd("on network available: ", network);
            updateNetworkState();
        }

        private void updateNetworkState() {
            retryCount = 0;
            Singletons.get(UiScheduleRunner.class).run(this);
        }

        @Override
        public void onLost(Network network) {
            Logger.getDefault().dd("on network lost: ", network);
            updateNetworkState();
        }

        @Override
        public void run() {
            NetworkInfo networkInfo = getActiveNetworkInfo();
            Logger.getDefault().dd("active network info(", retryCount, "): ", networkInfo);
            internetAvailability.setValue(networkInfo != null && networkInfo.isConnected());
            // 多检测几次，防止判断错误
            if (++retryCount < 3) {
                Singletons.get(UiScheduleRunner.class).schedule(this, 3000);
            }
        }
    }
}
