/**
 * 
 */
package lib.android.model;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import lib.android.util.CommonUtils;
import lib.common.entity.SimpleInfoHandler;
import lib.common.model.Singletons;
import lib.common.util.ConsoleUtil;

/**
 * @author yanry
 *
 * 2014年11月10日 上午9:20:38
 */
public class AndroidInfoHandler extends SimpleInfoHandler implements Runnable {
	private Context ctx;
	private Set<String> showingMsgs;
	private long lockDuration;
	private Map<String, Integer> msgsToShow;
	private Toast toast;
	
	public AndroidInfoHandler(Context ctx, long lockDuration) {
		this.ctx = ctx;
		this.lockDuration = lockDuration;
		showingMsgs = new HashSet<String>();
		msgsToShow = new HashMap<String, Integer>();
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
		if (lockDuration > 0) {
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
		} else {
			msgsToShow.clear();
			msgsToShow.put(msg, duration);
			CommonUtils.runOnUiThread(this);
		}
	}

	@Override
	public void debug(String msg) {
		if (getLevel() <= LEVEL_DEBUG) {
			Log.d(getClass().getSimpleName(), ConsoleUtil.getLog(getClass(), msg));
		}
	}

	@Override
	public void error(String msg) {
		if (getLevel() <= LEVEL_ERROR) {
			Log.e(getClass().getSimpleName(), ConsoleUtil.getLog(getClass(), msg));
		}
	}

	@Override
	public synchronized void run() {
		if (lockDuration > 0) {
			for (String k : msgsToShow.keySet()) {
				Toast.makeText(ctx, k, msgsToShow.get(k)).show();
			}
			msgsToShow.clear();
		} else {
			if (toast != null) {
				toast.cancel();
			}
			for (String k : msgsToShow.keySet()) {
				toast = Toast.makeText(ctx, k, msgsToShow.get(k));
				toast.show();
			}
		}
	}

}
