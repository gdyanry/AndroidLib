/**
 * 
 */
package lib.android.model.request.cache.list;

import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import lib.android.model.request.cache.PageLoadingProvider;
import lib.android.view.pull.PullDownSource;
import lib.android.view.pull.loadmore.LoadMore;
import lib.android.view.pull.refresh.RefreshHandler;
import lib.android.widget.RequestListenPage;
import lib.common.model.communication.base.RequestLiveListener;
import lib.common.model.json.JSONObject;

/**
 * 使用{@link AbsListView}和{@link RequestListenPage}的下拉刷新及分页数据处理。
 * 
 * @author yanry
 *
 *         2016年1月25日
 */
public abstract class PullListPageHandler extends PullPagingDataHandler implements OnItemClickListener {
	private BaseAdapter adapter;
	private AbsListView lv;
	private PageLoadingProvider page;

	public PullListPageHandler(PullDownSource pull, RefreshHandler refreshHandler, LoadMore more,
			RequestListenPage pageLoading, AbsListView lv) {
		super(pull, refreshHandler, more);
		lv.setOnItemClickListener(this);
		this.lv = lv;
		page = new PageLoadingProvider(pageLoading) {

			@Override
			public void onClick(View v) {
				refresh();
			}
		};
	}

	public AbsListView getAbsListView() {
		return lv;
	}

	/**
	 * Override this and return false if the list adapter is not simply backed
	 * by data from {@link #getListData()}, so you can totally customize the
	 * item-click callback.
	 * 
	 * @param position
	 * @return return true to trigger {@link #onClickItem(JSONObject)}
	 */
	protected boolean onClickItem(int position) {
		return true;
	}

	/**
	 * Override this method to add header view, etc.
	 * 
	 * @param lv
	 */
	protected void beforeSetAdapter(AbsListView lv) {
	}

	@Override
	protected RequestLiveListener getPageLoading() {
		return page.getPageLoading();
	}

	@Override
	protected void onDataPrepared() {
		if (adapter == null) {
			adapter = createAdapter();
			beforeSetAdapter(lv);
			lv.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (parent instanceof ListView) {
			// adjust position
			position -= ((ListView) parent).getHeaderViewsCount();
		}
		if (position >= 0 && onClickItem(position)) {
			if (position < getListData().length()) {
				onClickItem(getListData().getJSONObject(position));
			}
		}
	}

	protected abstract void onClickItem(JSONObject itemData);

	protected abstract BaseAdapter createAdapter();
}
