/**
 * 
 */
package lib.android.model;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import lib.common.util.ConsoleUtil;

/**
 *
 * need permission
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.INTERNET" />
 * 
 * @author yanry
 *
 */
public class NetworkConnMngr extends BroadcastReceiver {
	private ConnectivityManager cm;
	private List<ConnectivityListener> listeners;
	private Context ctx;
	private NetworkInfo currentNetwork;

	public NetworkConnMngr(Context ctx) {
		this.ctx = ctx;
		listeners = new ArrayList<NetworkConnMngr.ConnectivityListener>();
		cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		ctx.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	public void init() {
		currentNetwork = null;
		updateCurrentNetwork();
	}

	private void updateCurrentNetwork() {
		NetworkInfo[] networkInfos = cm.getAllNetworkInfo();
		for (NetworkInfo ni : networkInfos) {
			if (ni.isConnected()) {
				// to avoid same situation triggered twice.
				boolean same = currentNetwork != null && ni.getType() == currentNetwork.getType();
				currentNetwork = ni;
				if (!same) {
					ConsoleUtil.debug(getClass(), "onConnected: " + ni.getTypeName());
					// dispatch callback
					for (ConnectivityListener l : listeners) {
						l.onConnected(ni.getTypeName());
					}
				}
				return;
			}
		}
		// no connected network
		currentNetwork = null;
		ConsoleUtil.debug(getClass(), "lost connection!");
		for (ConnectivityListener l : listeners) {
			l.onDisconnect();
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		updateCurrentNetwork();
	}

	/**
	 * return false if this listener has already been registered; other wise
	 * return true.
	 */
	public boolean register(ConnectivityListener listener) {
		if (listeners.contains(listener)) {
			return false;
		}
		listeners.add(listener);
		return true;
	}

	/**
	 * return false if this listener has not registered before; other wise
	 * return true.
	 */
	public boolean unregister(ConnectivityListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * return null if there's no connected network currently.
	 */
	public NetworkInfo getCurrentConnectedNetworkInfo() {
		if (currentNetwork == null) {
			updateCurrentNetwork();
		}
		return currentNetwork;
	}

	public boolean isConnected() {
		boolean connected = currentNetwork != null && currentNetwork.isConnected();
		if (!connected) {
			init();
			return currentNetwork != null && currentNetwork.isConnected();
		} else {
			return true;
		}
	}

	public void release() {
		listeners.clear();
		if (ctx != null) {
			ctx.unregisterReceiver(this);
		}
	}

	public interface ConnectivityListener {

		void onDisconnect();

		void onConnected(String typeName);
	}
}
