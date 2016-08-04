/**
 * 
 */
package lib.android.view.pull;

import java.util.LinkedList;
import java.util.List;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.GridView;

/**
 * This class is to specify a view to be pulled and add {@link PullDownHook} to
 * it.
 * 
 * @author yanry
 *
 *         2016年3月15日
 */
public abstract class PullDownSource implements OnTouchListener {
	private static final int INVALID_POINTER_ID = -110;
	private float yStart;
	private List<PullDownHook> hooks;
	private int activePointerId;

	/**
	 * 
	 * @param pullView
	 *            the view used to handle onTouch event so as to generate pull
	 *            down action.
	 */
	public PullDownSource(View pullView) {
		pullView.setOnTouchListener(this);
		hooks = new LinkedList<PullDownHook>();
		activePointerId = INVALID_POINTER_ID;
	}

	public void addHook(PullDownHook hook) {
		if (!hooks.contains(hook)) {
			hooks.add(hook);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int actionIndex = event.getActionIndex();
		int pointerId = event.getPointerId(actionIndex);
		float y = event.getY(actionIndex);
		int distance;
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
		case MotionEvent.ACTION_MOVE:
			if (pointerId == activePointerId) {
				// is pulling
				distance = (int) (y - yStart);
				if (distance > 0) {
					for (PullDownHook h : hooks) {
						h.onPullingDown(distance);
					}
					return true;
				} else {
					// turn off pulling state
					for (PullDownHook h : hooks) {
						h.onPullingDown(0);
					}
					activePointerId = INVALID_POINTER_ID;
				}
			} else if (activePointerId == INVALID_POINTER_ID) {
				// not pulling currently
				if (isReadyToPull()) {
					for (PullDownHook h : hooks) {
						if (!h.onPreparedToPull()) {
							// pulling is deny by consumer
							return false;
						}
					}
					activePointerId = pointerId;
					yStart = y;
					if (!(v instanceof GridView) && (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)) {
						return true;
					}
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if (pointerId == activePointerId) {
				// is pulling
				distance = (int) (y - yStart);
				if (distance > 0) {
					// change event type to cancel to prevent click event
					event.setAction(MotionEvent.ACTION_CANCEL);
					for (PullDownHook h : hooks) {
						h.onRelease(distance);
					}
				}
				activePointerId = INVALID_POINTER_ID;
			}
		}
		return false;
	}

	protected abstract boolean isReadyToPull();
}
