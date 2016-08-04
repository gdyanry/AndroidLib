/**
 * 
 */
package lib.android.view;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * Scroll a view that is wrapped by a {@link ScrollView} or
 * {@link HorizontalScrollView} automatically and repeatedly.
 * 
 * @author yanry
 *
 *         2016年1月30日
 */
public class AutoScroller implements OnTouchListener {

	private TranslateAnimation ta;

	public AutoScroller(final HorizontalScrollView hsv, final long durationMillis) {
		hsv.setOnTouchListener(this);
		Activity activity = (Activity) hsv.getContext();
		activity.getWindow().getDecorView().post(new Runnable() {
			public void run() {
				View v = hsv.getChildAt(0);
				int dx = v.getWidth() - hsv.getWidth();
				ta = new TranslateAnimation(Animation.ABSOLUTE, -dx, 0, 0);
				ta.setRepeatCount(Animation.INFINITE);
				ta.setRepeatMode(Animation.REVERSE);
				ta.setDuration(durationMillis);
				v.startAnimation(ta);
			}
		});
	}

	public AutoScroller(final ScrollView sv, final long durationMillis) {
		sv.setOnTouchListener(this);
		Activity activity = (Activity) sv.getContext();
		activity.getWindow().getDecorView().post(new Runnable() {
			public void run() {
				View v = sv.getChildAt(0);
				int dy = v.getHeight() - sv.getHeight();
				ta = new TranslateAnimation(0, 0, Animation.ABSOLUTE, -dy);
				ta.setRepeatCount(Animation.INFINITE);
				ta.setRepeatMode(Animation.REVERSE);
				ta.setDuration(durationMillis);
				v.startAnimation(ta);
			}
		});
	}

	public boolean startScroll() {
		if (ta != null) {
			ta.reset();
			ta.startNow();
			return true;
		}
		return false;
	}

	public boolean stopScroll() {
		if (ta != null) {
			ta.cancel();
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

}
