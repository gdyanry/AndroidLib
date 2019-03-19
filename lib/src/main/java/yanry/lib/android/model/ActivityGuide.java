/**
 * 
 */
package yanry.lib.android.model;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * @author yanry
 *
 *         2016年6月3日
 */
public abstract class ActivityGuide {
	private FrameLayout root;
	private String prefKey;
	private View currentGuideView;

	/**
	 * 
	 * @param activity
	 *            the activity to show guide
	 * @param versionTag
	 *            tag of this activity guide, same activity with different tag
	 *            (in case of version upgrade) uses separate guided counts.
	 */
	public ActivityGuide(Activity activity, String versionTag) {
		root = (FrameLayout) activity.findViewById(android.R.id.content);
		prefKey = String.format("%s_%s_%s", getClass().getSimpleName(), activity.getClass().getName(), versionTag);
	}

	/**
	 * 
	 * @return times of previous guides on the activity, which is exactly the
	 *         times {@link #finish()} has been called.
	 */
	public int getGuidedCount() {
		return getUserPreference().getInt(prefKey, 0);
	}

	public View setGuideView(int viewId) {
		View guideView = LayoutInflater.from(root.getContext()).inflate(viewId, root, false);
		setGuideView(guideView);
		return guideView;
	}

	public void setGuideView(View guideView) {
		abort();
		root.addView(guideView);
		currentGuideView = guideView;
	}

	public void finish() {
		if (currentGuideView != null) {
			root.removeView(currentGuideView);
			SharedPreferences pref = getUserPreference();
			if (pref != null) {
				int count = pref.getInt(prefKey, 0);
                pref.edit().putInt(prefKey, ++count).apply();
			}
		}
	}
	
	public void abort() {
		if (currentGuideView != null) {
			root.removeView(currentGuideView);
			currentGuideView = null;
		}
	}

	protected abstract SharedPreferences getUserPreference();
}
