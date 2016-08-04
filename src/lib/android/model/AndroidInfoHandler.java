/**
 * 
 */
package lib.android.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import lib.android.util.CommonUtils;
import lib.common.entity.InfoHandler;
import lib.common.model.Singletons;
import lib.common.util.StringUtil;

/**
 * @author yanry
 *
 * 2014年11月10日 上午9:20:38
 */
public class AndroidInfoHandler implements InfoHandler, Runnable {
	private Context ctx;
	private int level;
	private Set<String> showingMsgs;
	private long lockDuration;
	private Map<String, Integer> msgsToShow;
	
	public AndroidInfoHandler(Context ctx, long lockDuration) {
		this.ctx = ctx;
		this.lockDuration = lockDuration;
		showingMsgs = new HashSet<String>();
		msgsToShow = new HashMap<String, Integer>();
	}

	@Override
	public void handleException(Exception e) {
		if (level <= LEVEL_EXCEPTION) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleThrowable(Throwable e) {
		if (level <= LEVEL_EXCEPTION) {
			e.printStackTrace();
		}
	}

	@Override
	public void showError(String msg) {
		show(msg, Toast.LENGTH_LONG);
	}

	@Override
	public void showMessage(String msg) {
		show(msg, Toast.LENGTH_SHORT);
	}
	
	private synchronized void show(final String msg, int duration) {
		if (showingMsgs.add(msg)) {
			msgsToShow.put(msg, duration);
			CommonUtils.runOnUiThread(this);
			Singletons.get(Timer.class).schedule(new TimerTask() {
				
				@Override
				public void run() {
					showingMsgs.remove(msg);
				}
			}, lockDuration);
		}
	}

	@Override
	public void debug(Class<?> tag, String msg) {
		if (level <= LEVEL_DEBUG) {
			Log.d(StringUtil.getLogTag(tag), String.format("(%s)%s", Thread.currentThread().getName(), msg));
		}
	}

	@Override
	public void error(Class<?> tag, String msg) {
		if (level <= LEVEL_ERROR) {
			Log.e(StringUtil.getLogTag(tag), String.format("(%s)%s", Thread.currentThread().getName(), msg));
		}
	}

	@Override
	public void setLevel(int level) {
		this.level = level;
	}

	@Override
	public synchronized void run() {
		for (String k : msgsToShow.keySet()) {
			Toast.makeText(ctx, k, msgsToShow.get(k)).show();
		}
		msgsToShow.clear();
	}

}
