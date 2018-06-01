/**
 * 
 */
package lib.android.view.pull.loadmore;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * Load more will be triggered when user scrolls the {@link AbsListView} to the
 * end.
 * 
 * @author yanry
 *
 *         2016年3月31日
 */
public class AutoLoadMore implements OnScrollListener, LoadMore {
	private boolean isLoading;
	private LoadMoreView loadingView;
	private boolean isEnable;
	private boolean hasMore;
	private OnLoadMoreListener listener;
	private boolean isEnd;

	public AutoLoadMore(LoadMoreView loadingView, AbsListView view) {
		this.loadingView = loadingView;
		isEnable = true;
		view.setOnScrollListener(this);
	}

	@Override
	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

	@Override
	public void stopLoading() {
		if (isLoading) {
			isLoading = false;
			loadingView.onStopLoading();
		}
	}

	@Override
	public void setEnable(boolean enable) {
		this.isEnable = enable;
	}

	@Override
	public boolean isLoading() {
		return isLoading;
	}

	@Override
	public void setOnLoadMoreListener(OnLoadMoreListener l) {
		this.listener = l;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_IDLE && isEnd && !isLoading && isEnable && hasMore) {
			isLoading = true;
			loadingView.onStartLoading();
			if (listener != null) {
				listener.onLoadMore();
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		isEnd = view.getLastVisiblePosition() == totalItemCount - 1;
	}
}
