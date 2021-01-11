/**
 * 
 */
package yanry.lib.android.model.request.cache;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.cache.CacheDataHook;
import yanry.lib.java.model.cache.CacheDataManager;
import yanry.lib.java.model.cache.http.DataSource;
import yanry.lib.java.model.communication.base.RequestDataHook;
import yanry.lib.java.model.communication.base.RequestPack;
import yanry.lib.java.model.communication.base.ResponseParser;

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
		Singletons.get(UiRunner.class).run(() -> {
			Object bzData = getBusinessSuccessData(responseData);
			if (bzData != null) {
				String cacheKey = getCacheKey();
				if (onData(bzData, DataSource.SERVER) && cacheKey != null) {
					getCacheDataManager().saveCache(cacheKey, bzData, ifRemoveCacheOnLogout());
				}
			}
		});
	}
}
