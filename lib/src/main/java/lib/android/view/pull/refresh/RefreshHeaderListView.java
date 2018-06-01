/**
 * 
 */
package lib.android.view.pull.refresh;

import android.view.View;
import android.widget.ListView;

import lib.android.view.pull.PullDownAdapterView;
import lib.android.view.pull.PullDownSource;
import lib.android.view.pull.shrink.DecelerateShrinkController;
import lib.android.view.pull.shrink.ShrinkController;

/**
 * @author yanry
 *
 * 2016年7月25日
 */
public abstract class RefreshHeaderListView extends RefreshViewHandler {
	private PullDownAdapterView pull;

	public RefreshHeaderListView(float dragFactor, View refreshView, ListView lv) {
		this(new DecelerateShrinkController(), dragFactor, refreshView, lv);
	}
	
	public RefreshHeaderListView(ShrinkController controller, float dragFactor, View refreshView, ListView lv) {
		super(controller, dragFactor, refreshView);
		lv.addHeaderView(refreshView);
		initRefreshViewLayoutParams();
		pull = new PullDownAdapterView(lv);
		pull.addHook(this);
	}

	public PullDownSource getPullDownSource() {
		return pull;
	}
}
