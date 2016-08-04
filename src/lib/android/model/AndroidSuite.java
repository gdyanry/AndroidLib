/**
 * 
 */
package lib.android.model;

import java.io.File;

import android.content.ContextWrapper;
import android.content.SharedPreferences;
import lib.android.util.CommonUtils;
import lib.common.model.LoginHandler;

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
	private NetworkConnMngr connMngr;
	private File rootDir;

	public AndroidSuite(ContextWrapper ctx) {
		this.ctx = ctx;
		connMngr = new NetworkConnMngr(ctx);
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

	public NetworkConnMngr getNetworkConnectionManager() {
		return connMngr;
	}

	public File getCacheRoot() {
		return rootDir;
	}

	public void release() {
		connMngr.release();
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void startUp() {
		connMngr.init();
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
		getSharedPreferences().edit().putString(key, value).commit();
	}

	@Override
	protected void removeEntry(String key) {
		getSharedPreferences().edit().remove(key).commit();
	}

}
