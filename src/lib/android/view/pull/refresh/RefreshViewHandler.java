/**
 * 
 */
package lib.android.view.pull.refresh;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import lib.android.view.pull.shrink.DecelerateShrinkController;
import lib.android.view.pull.shrink.ShrinkController;

/**
 * @author yanry
 *
 * 2016年7月21日
 */
public abstract class RefreshViewHandler extends RefreshHandler {

	private View refreshView;
	private int height;
	
	public RefreshViewHandler(float dragFactor, View refreshView) {
		this(new DecelerateShrinkController(), dragFactor, refreshView);
	}
	
	public RefreshViewHandler(ShrinkController controller, float dragFactor, View refreshView) {
		super(controller, dragFactor);
		this.refreshView = refreshView;
		initRefreshViewLayoutParams();
	}

	public void initRefreshViewLayoutParams() {
		LayoutParams params = refreshView.getLayoutParams();
		if (height == 0) {
			height = params.height;
		}
		setLayoutParams(0);
	}
	
	protected abstract void onOffsetChange(int offset, RefreshState state, int offsetThreshold);

	@Override
	protected int getScaledDistanceThreshold() {
		return height;
	}

	@Override
	protected void updateView(int scaledDistance) {
		setLayoutParams(scaledDistance);
		onOffsetChange(scaledDistance, getCurrentState(), height);
	}

	protected void setLayoutParams(int scaledDistance) {
		LayoutParams params = refreshView.getLayoutParams();
		params.height = scaledDistance;
		refreshView.setPadding(refreshView.getPaddingLeft(), scaledDistance - height, refreshView.getPaddingRight(), refreshView.getPaddingBottom());
		refreshView.setLayoutParams(params);
//		refreshView.setVisibility(scaledDistance > 0 ? View.VISIBLE : View.GONE);
	}

}
