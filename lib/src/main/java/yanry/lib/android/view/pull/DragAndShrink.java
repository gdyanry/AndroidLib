/**
 * 
 */
package yanry.lib.android.view.pull;

import yanry.lib.android.view.pull.shrink.DecelerateShrinkController;
import yanry.lib.android.view.pull.shrink.ShrinkController;

/**
 * @author yanry
 *
 *         2016年3月21日
 */
public abstract class DragAndShrink implements ShrinkController.OnUpdateOffsetListener, PullDownHook {
	private float dragFactor;
	private ShrinkController controller;
	private int logScaledDistance;
	private boolean logIsPulling;
	
	/**
	 * 
	 * @param dragFactor
	 *            the proportion of scaled distance to drag distance.
	 */
	public DragAndShrink(float dragFactor) {
		this(new DecelerateShrinkController(), dragFactor);
	}

	/**
	 * 
	 * @param controller
	 * @param dragFactor
	 *            the proportion of scaled distance to drag distance.
	 */
	public DragAndShrink(ShrinkController controller, float dragFactor) {
		this.dragFactor = dragFactor;
		this.controller = controller;
		controller.addOnUpdateOffsetListener(this);
		logScaledDistance = -1;
	}

	public ShrinkController getController() {
		return controller;
	}
	
	public float getDragFactor() {
		return dragFactor;
	}
	
	public void commitDistanceChange(int scaledDistance, boolean isPulling) {
        if (scaledDistance != logScaledDistance || isPulling != logIsPulling) {
			onDistanceChange(scaledDistance, isPulling);
			logScaledDistance = scaledDistance;
			logIsPulling = isPulling;
		}
    }

	@Override
	public void onPullingDown(int distance) {
		controller.cancel();
		commitDistanceChange((int) (distance * dragFactor), true);
	}

	@Override
	public void onRelease(int distance) {
		controller.startShrink(distance);
	}

	@Override
	public void onUpdateOffset(int offset) {
		commitDistanceChange((int) (offset * dragFactor), false);
	}

	protected abstract void onDistanceChange(int scaledDistance, boolean isPulling);
}
