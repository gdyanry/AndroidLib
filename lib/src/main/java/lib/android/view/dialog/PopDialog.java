/**
 * 
 */
package lib.android.view.dialog;

import android.app.Activity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yanry
 */
public abstract class PopDialog {
	private static Activity activity;
	private static List<PopDialog> list = new LinkedList<PopDialog>();
	private static boolean pop;

	public PopDialog() {
		if (pop) {
			if (activity != null) {
				show(activity);
			}
		} else {
			list.add(this);
		}
	}

	/**
	 * clear backed dialogs.
	 */
	public static void clear() {
		activity = null;
		list.clear();
	}

	/**
	 * register activity that pops dialogs when the activity becomes active.
	 */
	public static void resume(Activity activity) {
		pop = list.size() == 0;
		PopDialog.activity = activity;
		if (list.size() > 0) {
			list.remove(0).show(activity);
		}
	}

	/**
	 * called on the registered activity to disable popping dialogs when the
	 * activity becomes not active.
	 */
	public static void pause() {
		pop = false;
	}

	protected abstract void show(Activity ctx);
}
