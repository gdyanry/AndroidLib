/**
 * 
 */
package lib.android.model.request.cache;

import lib.android.entity.enums.DataSource;
import lib.android.util.CommonUtils;
import lib.common.model.cache.CacheDataHook;
import lib.common.model.cache.CacheDataManager;
import lib.common.model.communication.base.RequestDataHook;
import lib.common.model.communication.base.RequestPack;
import lib.common.model.communication.base.ResponseParser;

/**
 * @author yanry
 *
 *         2016年4月4日
 */
abstract class AbstractCacheRequest implements RequestDataHook, ResponseParser, CacheDataHook {

	/**
	 * Host this request to another request created and to be sent from outside.
	 * 
	 * @param hostedRequest
	 */
	public void appendRequestTo(RequestPack hostedRequest) {
		hostedRequest.append(getApiTag(), getRequestParam(), this);
	}

	/**
	 * You can override this method to provide your own cache key, or return
	 * null to disable cache for this request.
	 * 
	 * @return
	 */
	protected String getCacheKey() {
		Object requestParam = getRequestParam();
		return String.format("%s@%s", getApiTag(), requestParam == null ? null : requestParam.toString());
	}

	protected abstract CacheDataManager getCacheDataManager();

	protected abstract String getApiTag();

	protected abstract Object getRequestParam();

	/**
	 * This should be called on UI thread.
	 * 
	 * @param data
	 *            might be null for {@link SubCacheRequest} when there's no
	 *            cached data.
	 * @param from
	 * @return whether send request to get the latest data when current data is
	 *         from memory or local cache, or whether save data to cache when
	 *         current data is from server.
	 */
	protected abstract boolean onData(Object data, DataSource from);

	protected abstract boolean ifRemoveCacheOnLogout();

	@Override
	public void onResponse(final Object responseData) {
		CommonUtils.runOnUiThread(new Runnable() {
			public void run() {
				Object bzData = getBusinessSuccessData(responseData);
				if (bzData != null) {
					String cacheKey = getCacheKey();
					if (onData(bzData, DataSource.SERVER) && cacheKey != null) {
						getCacheDataManager().saveCache(cacheKey, bzData, ifRemoveCacheOnLogout());
					}
				}
			}
		});
	}
}
