/**
 * 
 */
package yanry.lib.android.view.pull.refresh;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import yanry.lib.android.view.pull.DragAndShrink;
import yanry.lib.android.view.pull.loadmore.LoadMore;
import yanry.lib.android.view.pull.shrink.DecelerateShrinkController;
import yanry.lib.android.view.pull.shrink.ShrinkController;

/**
 * A pull-to-refresh view model that has four states: {@link RefreshState#Reset}
 * , {@link RefreshState#PullToRefresh}, {@link RefreshState#ReleaseToRefresh},
 * {@link RefreshState#Refreshing}.
 * 
 * @author yanry
 *
 *         2016年3月30日
 */
public abstract class RefreshHandler extends DragAndShrink implements AnimatorUpdateListener {

	private RefreshState state;
	private boolean startByPulling;
	private LoadMore more;
	private OnRefreshListener listener;
	private Interpolator linear;
	private int refreshDistance;
	
	public RefreshHandler(float dragFactor) {
		this(new DecelerateShrinkController(), dragFactor);
	}

	public RefreshHandler(ShrinkController controller, float dragFactor) {
		super(controller, dragFactor);
		linear = new LinearInterpolator();
		state = RefreshState.Reset;
	}

	/**
	 * 
	 * @param pullMillis
	 *            time of pulling before refreshing. if >0, the refresh will act
	 *            like and be treated as started by pulling.
	 */
	public void startRefreshing(int pullMillis) {
		if (more != null && more.isLoading()) {
			return;
		}
		if (state == RefreshState.Reset) {
			refreshDistance = getScaledDistanceThreshold() + 1;
			if (pullMillis > 0) {
				startByPulling = true;
				ValueAnimator va = ValueAnimator.ofInt(0, refreshDistance).setDuration(pullMillis);
				va.setInterpolator(linear);
				va.addUpdateListener(this);
				va.start();
			} else {
				startByPulling = false;
				state = RefreshState.ReleaseToRefresh;
				updateView(refreshDistance);
				getController().startShrink((int) (refreshDistance / getDragFactor()));
			}
		}
	}

	public void stopRefreshing() {
		if (state == RefreshState.Refreshing) {
			state = RefreshState.Reset;
			onStateChange(RefreshState.Refreshing);
			getController().resume();
			if (more != null) {
				more.setEnable(true);
			}
		}
	}

	public boolean isStartedByPulling() {
		return state != RefreshState.Reset && startByPulling;
	}

	public RefreshState getCurrentState() {
		return state;
	}

	/**
	 * Call this to avoid refreshing and loading more at the same time.
	 * 
	 * @param more
	 */
	public void dealWithLoadMore(LoadMore more) {
		this.more = more;
	}

	public void setOnRefreshListener(OnRefreshListener l) {
		this.listener = l;
	}

	@Override
	protected void onDistanceChange(int scaledDistance, boolean isPulling) {
		if (isPulling) {
			switch (state) {
			case PullToRefresh:
				if (scaledDistance > getScaledDistanceThreshold()) {
					state = RefreshState.ReleaseToRefresh;
					onStateChange(RefreshState.PullToRefresh);
				}
				break;
			case ReleaseToRefresh:
				if (scaledDistance <= getScaledDistanceThreshold()) {
					state = RefreshState.PullToRefresh;
					onStateChange(RefreshState.ReleaseToRefresh);
				}
				break;
			case Reset:
				if (scaledDistance > 0) {
					if (scaledDistance <= getScaledDistanceThreshold()) {
						state = RefreshState.PullToRefresh;
					} else {
						state = RefreshState.ReleaseToRefresh;
					}
					onStateChange(RefreshState.Reset);
				}
				break;
			case Refreshing:
				break;
			}
		} else {
			switch (state) {
			case PullToRefresh:
				state = RefreshState.Reset;
				onStateChange(RefreshState.PullToRefresh);
				break;
			case ReleaseToRefresh:
				state = RefreshState.Refreshing;
				onStateChange(RefreshState.ReleaseToRefresh);
				if (more != null) {
					more.setEnable(false);
				}
				if (listener != null) {
					listener.onRefresh(startByPulling);
				}
				break;
			case Refreshing:
				if (scaledDistance <= getScaledDistanceThreshold()) {
					getController().pause();
					updateView(getScaledDistanceThreshold());
					return;
				}
				break;
			case Reset:
				break;
			}
		}
		updateView(scaledDistance);
	}

	@Override
	public boolean onPreparedToPull() {
		boolean canPull = state != RefreshState.Refreshing && state != RefreshState.ReleaseToRefresh;
		if (more != null) {
			canPull = !more.isLoading() && canPull;
		}
		if (canPull) {
			startByPulling = true;
		}
		return canPull;
	}

	@Override
	public void onAnimationUpdate(ValueAnimator animation) {
		Integer distance = (Integer) animation.getAnimatedValue();
		commitDistanceChange(distance, true);
		if (distance == refreshDistance) {
			getController().startShrink((int) (refreshDistance / getDragFactor()));
		}
	}

	/**
	 * 
	 * @return the scaled distance threshold to trigger refresh action.
	 */
	protected abstract int getScaledDistanceThreshold();

	protected abstract void onStateChange(RefreshState previousSate);

	protected abstract void updateView(int scaledDistance);
}
