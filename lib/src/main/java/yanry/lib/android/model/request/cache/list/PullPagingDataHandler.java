/**
 * 
 */
package yanry.lib.android.model.request.cache.list;

import yanry.lib.android.model.request.AndroidRequestLiveListener;
import yanry.lib.android.model.request.RequestState;
import yanry.lib.android.view.pull.PullDownSource;
import yanry.lib.android.view.pull.loadmore.LoadMore;
import yanry.lib.android.view.pull.loadmore.OnLoadMoreListener;
import yanry.lib.android.view.pull.refresh.OnRefreshListener;
import yanry.lib.android.view.pull.refresh.RefreshHandler;
import yanry.lib.java.model.communication.base.RequestLiveListener;

/**
 * 带下拉刷新的分页数据处理。
 * 
 * @author yanry
 *
 *         2016年3月31日
 */
public abstract class PullPagingDataHandler extends PagingDataHandler implements OnRefreshListener, OnLoadMoreListener {

	private RefreshHandler refresh;
	private LoadMore mMore;
	private AndroidRequestLiveListener l;

	public PullPagingDataHandler(PullDownSource pull, RefreshHandler refreshHandler, LoadMore more) {
		refresh = refreshHandler;
		pull.addHook(refresh);
		more.setHasMore(true);
		refresh.dealWithLoadMore(more);
		refresh.setOnRefreshListener(this);
		more.setOnLoadMoreListener(this);
		this.mMore = more;
		l = new AndroidRequestLiveListener() {

			@Override
			protected void onStateChange(RequestState state, Object data) {
				switch (state) {
				case NoConnection:
					refresh.stopRefreshing();
					mMore.stopLoading();
					PullPagingDataHandler.this.onNoConnection();
					break;
				case Start:
					if (!refresh.isStartedByPulling()) {
						refresh.startRefreshing(0);
					}
					break;
				case Error:
					refresh.stopRefreshing();
					mMore.stopLoading();
					PullPagingDataHandler.this.onConnectionError(data);
					break;
				case Finish:
				case Cancel:
					refresh.stopRefreshing();
					mMore.stopLoading();
					break;
				}
			}
		};
	}

	public RefreshHandler getRefreshHandler() {
		return refresh;
	}

	public void refresh(int inMillis) {
		refresh.startRefreshing(inMillis);
	}

	protected abstract void onNoConnection();

	protected abstract void onConnectionError(Object e);

	@Override
	protected void setHasMore(boolean hasMore) {
		mMore.setHasMore(hasMore);
	}

	@Override
	protected RequestLiveListener getSmallLoading() {
		return l;
	}

	@Override
	public void onRefresh(boolean byPulling) {
		if (byPulling) {
			refresh();
		}
	}

	@Override
	public void onLoadMore() {
		loadMore();
	}
}
