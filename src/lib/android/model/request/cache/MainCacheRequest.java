/**
 * 
 */
package lib.android.model.request.cache;

import java.util.LinkedList;
import java.util.List;

import lib.android.util.CommonUtils;
import lib.common.model.cache.http.DataSource;
import lib.common.model.communication.base.RequestLiveListener;
import lib.common.model.communication.base.RequestPack;

/**
 * @author yanry
 *
 *         2016年1月20日
 */
public abstract class MainCacheRequest extends AbstractCacheRequest implements Runnable {
	private List<SubCacheRequest> subRequests;
	private List<SubCacheRequest> preRequests;
	private RequestPack request;
	private RequestLiveListener visualizer;

	public MainCacheRequest() {
		subRequests = new LinkedList<SubCacheRequest>();
		preRequests = new LinkedList<SubCacheRequest>();
	}

	RequestPack getRequest() {
		return request;
	}

	/**
	 * Add a sub request that will be handled before the main request.
	 * 
	 * @param r
	 * @return
	 */
	public MainCacheRequest preposeSubCacheRequest(SubCacheRequest r) {
		r.setMainCacheRequest(this);
		preRequests.add(r);
		return this;
	}

	/**
	 * Add a sub request that will be handled after the main request.
	 * 
	 * @param r
	 * @return
	 */
	public MainCacheRequest appendSubCacheRequest(SubCacheRequest r) {
		r.setMainCacheRequest(this);
		subRequests.add(r);
		return this;
	}

	/**
	 * This can be call repeatedly.
	 */
	public void send() {
		CommonUtils.runOnUiThread(this);
	}

	/**
	 * Host this request (along with sub requests) to another request created
	 * and to be sent from outside.
	 * 
	 * @param hostRequest
	 */
	public void batchAppendTo(RequestPack hostedRequest) {
		request = hostedRequest;
		String cacheKey = getCacheKey();
		if (cacheKey == null) {
			onNoData();
		} else {
			getCacheDataManager().getData(cacheKey, this);
		}
	}

	public void cancel() {
		if (request != null) {
			request.cancel();
		}
	}

	/**
	 * 
	 * @return the {@link RequestLiveListener} to use when cache does not exist.
	 */
	protected abstract RequestLiveListener getPageLoading();

	/**
	 * 
	 * @return the {@link RequestLiveListener} to use when cache exists.
	 */
	protected abstract RequestLiveListener getSmallLoading();

	protected abstract RequestPack createRequest();

	@Override
	public void run() {
		batchAppendTo(createRequest());
		request.send(visualizer);
	}

	@Override
	public void onDataLoaded(boolean inMemory, Object data) {
		if (getPageLoading() != null) {
			getPageLoading().onFinish(data);
		}
		handleSubRequestsOnDataLoaded(true);
		if (onData(data, inMemory ? DataSource.MEMORY : DataSource.LOCAL)) {
			appendRequestTo(request);
		}
		handleSubRequestsOnDataLoaded(false);
		visualizer = getSmallLoading();
	}

	private void handleSubRequestsOnDataLoaded(boolean isPreposed) {
		for (SubCacheRequest r : isPreposed ? preRequests : subRequests) {
			String cacheKey = r.getCacheKey();
			if (cacheKey == null) {
				r.appendRequestTo(request);
			} else {
				getCacheDataManager().getData(cacheKey, r);
			}
		}
	}

	@Override
	public void onNoData() {
		for (SubCacheRequest r : preRequests) {
			r.appendRequestTo(request);
		}
		appendRequestTo(request);
		for (SubCacheRequest r : subRequests) {
			r.appendRequestTo(request);
		}
		visualizer = getPageLoading();
	}

}
