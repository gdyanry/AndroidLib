/**
 * 
 */
package lib.android.model.request.cache;

import android.view.View.OnClickListener;
import lib.android.widget.RequestListenPage;
import lib.common.model.communication.base.RequestLiveListener;

/**
 * @author yanry
 *
 * 2016年7月22日
 */
public abstract class PageLoadingProvider implements OnClickListener {
	private RequestListenPage page;

	public PageLoadingProvider(RequestListenPage page) {
		page.setOnClickRetryListener(this);
		this.page = page;
	}
	
	public RequestLiveListener getPageLoading() {
		return page;
	}
}
