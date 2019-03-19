/**
 * 
 */
package yanry.lib.android.view.pull;

import android.view.View;
import android.view.ViewGroup.LayoutParams;

import yanry.lib.android.view.pull.shrink.DecelerateShrinkController;
import yanry.lib.android.view.pull.shrink.ShrinkController;

/**
 * @author yanry
 *
 *         2016年3月21日
 */
public class ZoomHandler extends DragAndShrink {
	private View zoomView;
	private int height;

	public ZoomHandler(View zoomView, float dragFactor) {
		this(zoomView, new DecelerateShrinkController(), dragFactor);
	}

	public ZoomHandler(View zoomView, ShrinkController controller, float dragFactor) {
		super(controller, dragFactor);
		this.zoomView = zoomView;
	}

	public View getZoomView() {
		return zoomView;
	}

	public int getZoomViewHeight() {
		return height;
	}

	@Override
	public boolean onPreparedToPull() {
		if (height == 0) {
			height = zoomView.getHeight();
		}
		return true;
	}

	@Override
	protected void onDistanceChange(int scaledDistance, boolean isPulling) {
		LayoutParams p = zoomView.getLayoutParams();
		p.height = height + scaledDistance;
		zoomView.setLayoutParams(p);
		zoomView.setScaleX((height * 1f + scaledDistance) / height);
		zoomView.setScaleY((height * 1f + scaledDistance) / height);
	}

}
