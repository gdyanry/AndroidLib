package yanry.lib.android.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

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
public class NetworkManager extends ConnectivityManager.NetworkCallback {
    private ConnectivityManager manager;
    private BooleanHolderImpl internetAvailability;

    public NetworkManager(Context context) {
        manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        internetAvailability = new BooleanHolderImpl();
        manager.registerNetworkCallback(new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), this);
    }

    public ConnectivityManager getConnectivityManager() {
        return manager;
    }

    public BooleanHolder getInternetAvailability() {
        return internetAvailability;
    }

    @Override
    public void onAvailable(Network network) {
        Logger.getDefault().dd("on network available: ", network);
        updateNetworkState();
    }

    private void updateNetworkState() {
        NetworkInfo networkInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = manager.getActiveNetwork();
            if (activeNetwork != null) {
                networkInfo = manager.getNetworkInfo(activeNetwork);
            }
        } else {
            networkInfo = manager.getActiveNetworkInfo();
        }
        internetAvailability.setValue(networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public void onLost(Network network) {
        Logger.getDefault().dd("on network lost: ", network);
        updateNetworkState();
    }
}
