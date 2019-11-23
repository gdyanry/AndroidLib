/**
 * 
 */
package yanry.lib.android.model.request.cache;

import android.view.View;

import yanry.lib.android.model.request.RequestListenPage;
import yanry.lib.java.model.communication.base.RequestLiveListener;

/**
 * @author yanry
 *
 *         2016年1月20日
 */
public abstract class MainCachePageRequest extends MainCacheRequest {
	private PageLoadingProvider page;

	public MainCachePageRequest(RequestListenPage pageLoading) {
		this.page = new PageLoadingProvider(pageLoading) {
			
			@Override
			public void onClick(View v) {
				send();
			}
		};
	}
	
	@Override
	protected RequestLiveListener getPageLoading() {
		return page.getPageLoading();
	}
}
