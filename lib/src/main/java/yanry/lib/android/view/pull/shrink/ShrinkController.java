/**
 * 
 */
package yanry.lib.android.view.pull.shrink;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the relationship of time and space of some object that shrink to 0 from a given offset.
 * 
 * @author yanry
 *
 *         2016年3月21日
 */
public abstract class ShrinkController implements AnimatorUpdateListener {
	private Interpolator interpolator;
	private ValueAnimator va;
	private int currentOffset;
	private List<OnUpdateOffsetListener> listeners;

	public ShrinkController() {
		interpolator = new LinearInterpolator();
		listeners = new LinkedList<ShrinkController.OnUpdateOffsetListener>();
	}

	public void startShrink(int offset) {
		if (va != null) {
			va.cancel();
		}
		int shrinkTime = getLeftTimeByOffset(offset);
		va = ValueAnimator.ofInt(shrinkTime, 0).setDuration(shrinkTime);
		va.setInterpolator(interpolator);
		va.addUpdateListener(this);
		va.start();
	}

	public boolean pause() {
		if (va != null && va.isStarted()) {
			va.cancel();
			return true;
		}
		return false;
	}

	public boolean resume() {
		if (va != null && !va.isStarted() && currentOffset > 0) {
			startShrink(currentOffset);
			return true;
		}
		return false;
	}

	public void cancel() {
		if (va != null && va.isStarted()) {
			va.cancel();
		}
	}

	public void end() {
		if (va != null && va.isStarted()) {
			va.end();
		}
	}

	public void addOnUpdateOffsetListener(OnUpdateOffsetListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		int timeLeft = (Integer) animation.getAnimatedValue();
		// calculate offset by time
		currentOffset = getOffsetByLeftTime(timeLeft);
		for (OnUpdateOffsetListener l : listeners) {
			l.onUpdateOffset(currentOffset);
		}
	}
	
	protected abstract int getLeftTimeByOffset(int offset);
	
	protected abstract int getOffsetByLeftTime(int leftTime);

	public interface OnUpdateOffsetListener {
		void onUpdateOffset(int offset);
	}
}
