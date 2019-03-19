/**
 * 
 */
package yanry.lib.android.model.request.cache;

import android.view.View.OnClickListener;

import yanry.lib.android.widget.RequestListenPage;
import yanry.lib.java.model.communication.base.RequestLiveListener;

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
