/**
 * 
 */
package lib.android.model.request.cache.list;

import lib.android.entity.enums.DataSource;
import lib.android.model.request.cache.MainCacheRequest;
import lib.common.model.communication.base.RequestPack;
import lib.common.model.json.JSONArray;
import lib.common.model.json.JSONObject;

/**
 * 分页加载数据处理。
 * 
 * @author yanry
 *
 *         2016年1月25日
 */
public abstract class PagingDataHandler extends MainCacheRequest {
	private PagingParamWrapper param;
	private boolean isLoadMore;
	private JSONArray listData;
	private boolean hasMore;

	public PagingDataHandler() {
		listData = new JSONArray();
	}

	/**
	 * This should be called to send request instead of {@link #send()} on page
	 * loading.
	 * 
	 * @param param
	 *            request parameter that does not contain page-size and max-id
	 *            field, cann't be null.
	 */
	public void loadPage(PagingParamWrapper param) {
		this.param = param;
		sendRequest(false);
	}

	private void sendRequest(boolean loadMore) {
		isLoadMore = loadMore;
		if (isLoadMore) {
			if (hasStableOrder()) {
				JSONObject tailItem = listData.getJSONObject(listData.length() - 1);
				param.setMaxIdByTailItem(tailItem);
			} else {
				param.setPageSize(listData.length() + getPageSize());
			}
			RequestPack request = createRequest();
			appendRequestTo(request);
			request.send(getSmallLoading());
		} else {
			if (hasStableOrder()) {
				param.removeMaxId();
			}
			param.setPageSize(getPageSize());
			send();
		}
	}

	/**
	 * Refresh page with the same parameter with {@link #loadPage(JSONObject)}.
	 */
	public void refresh() {
		sendRequest(false);
	}

	public void loadMore() {
		sendRequest(true);
	}

	public JSONArray getListData() {
		return listData;
	}

	public boolean isEnableLoadMore() {
		return hasMore;
	}

	protected int getPageSize() {
		return 20;
	}

	protected int getPageLimit() {
		return 20;
	}

	@Override
	public Object getRequestParam() {
		return param.getParam();
	}

	@Override
	public boolean onData(Object data, DataSource from) {
		if (from == DataSource.SERVER) {
			JSONArray ja = getListFromSuccessData(data);
			if (!isLoadMore || !hasStableOrder()) {
				listData = ja;
			} else if (ja != null) {
				for (int i = 0; i < ja.length(); i++) {
					listData.put(ja.get(i));
				}
			}
			updateUI(ja);
			// save to cache.
			if (getCacheKey() != null) {
				getCacheDataManager().saveCache(getCacheKey(), listData, ifRemoveCacheOnLogout());
			}
			// return false to stop super class from saving cache (again).
			return false;
		} else {
			// cached data
			listData = (JSONArray) data;
			updateUI(null);
			return ifRefreshWhenCacheExists(from == DataSource.MEMORY);
		}
	}

	private void updateUI(JSONArray appendedData) {
		hasMore = !(appendedData == null || appendedData.length() == 0 || appendedData.length() % getPageSize() != 0
				|| listData.length() >= getPageSize() * getPageLimit());
		setHasMore(hasMore);
		onDataPrepared();
	}

	@Override
	public Object deserializeData(String localCachedData) {
		JSONArray ja = new JSONArray(localCachedData);
		return ja.length() > 0 ? ja : null;
	}

	/**
	 * You can get the prepared data by calling {@link #getListData()}.
	 */
	protected abstract void onDataPrepared();

	protected abstract void setHasMore(boolean hasMore);

	protected abstract boolean ifRefreshWhenCacheExists(boolean inMemory);

	protected abstract JSONArray getListFromSuccessData(Object data);

	protected abstract boolean hasStableOrder();
}
