/**
 * 
 */
package lib.android.model.request.cache;

import lib.android.entity.enums.DataSource;
import lib.common.model.cache.CacheDataManager;

/**
 * @author yanry
 *
 * 2016年7月1日
 */
public abstract class SubCacheRequest extends AbstractCacheRequest {
	private MainCacheRequest main;
	
	void setMainCacheRequest(MainCacheRequest main) {
		this.main = main;
	}

	@Override
	public void onDataLoaded(boolean inMemory, Object data) {
		if (this.onData(data, inMemory ? DataSource.MEMORY : DataSource.LOCAL)) {
			appendRequestTo(main.getRequest());
		}
	}

	@Override
	public void onNoData() {
//		this.onData(null, DataSource.LOCAL);
		appendRequestTo(main.getRequest());
	}
	
	@Override
	protected CacheDataManager getCacheDataManager() {
		return main.getCacheDataManager();
	}
}
