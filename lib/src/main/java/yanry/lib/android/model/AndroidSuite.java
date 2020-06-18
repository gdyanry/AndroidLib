/**
 * 
 */
package yanry.lib.android.model;

import android.content.ContextWrapper;
import android.content.SharedPreferences;

import java.io.File;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.LoginHandler;

/**
 * Need permission
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 * <uses-permission android:name="android.permission.INTERNET" />
 * 
 * @author yanry
 *
 *         2015年9月26日
 */
public abstract class AndroidSuite extends LoginHandler {
	private static final String SHARED_PREF_NAME = "android.suite.shared.preferences";

	private ContextWrapper ctx;
    private NetworkManager networkManager;
	private File rootDir;

	public AndroidSuite(ContextWrapper ctx) {
		this.ctx = ctx;
        networkManager = new NetworkManager(ctx);
		rootDir = CommonUtils.getDiskCacheDir(ctx);
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////base

	public SharedPreferences getSharedPreferences() {
		return ctx.getSharedPreferences(SHARED_PREF_NAME, 0);
	}

	public SharedPreferences getUserPreferences() {
		return isLogined() ? ctx.getSharedPreferences(getUid(), 0) : null;
	}

	public ContextWrapper getContext() {
		return ctx;
	}

    public NetworkManager getNetworkManager() {
        return networkManager;
	}

	public File getCacheRoot() {
		return rootDir;
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void startUp() {
		super.startUp();
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	protected boolean containsKey(String key) {
		return getSharedPreferences().contains(key);
	}

	@Override
	protected String getValue(String key) {
		return getSharedPreferences().getString(key, null);
	}

	@Override
	protected void save(String key, String value) {
        getSharedPreferences().edit().putString(key, value).apply();
	}

	@Override
	protected void removeEntry(String key) {
        getSharedPreferences().edit().remove(key).apply();
	}

}
