/**
 *
 */
package lib.android.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

/**
 * @date 2014-4-11
 */
public class ViewUtil {

	public static boolean switchAnimation(ImageView iv, boolean start) {
		AnimationDrawable anim = (AnimationDrawable) iv.getDrawable();
		if (anim != null) {
			if (start) {
				if (!anim.isRunning()) {
					anim.start();
					return true;
				}
			} else {
				if (anim.isRunning()) {
					anim.stop();
					return true;
				}
			}
		}
		return false;
	}

	public static void hideKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	public static void hideKeyboard(Activity activity) {
		((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	/**
	 * 
	 * @param v
	 * @param xy
	 *            an array of two integers in which to hold the coordinates.
	 * @return the given array, or a new created array if it's null
	 */
	public static int[] getLocationOnScreen(View v, int[] xy) {
		if (xy == null) {
			xy = new int[2];
		}
		v.getLocationOnScreen(xy);
		return xy;
	}
	
	public static Activity getActivityByView(View v) {
		Context context = v.getContext();
		if (context instanceof Activity) {
			return (Activity) context;
		}
		if (context instanceof ContextWrapper) {
			return (Activity) ((ContextWrapper) context).getBaseContext();
		}
		return null;
	}
}
