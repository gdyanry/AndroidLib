/**
 * 
 */
package yanry.lib.android.view.pull;

import android.view.View;
import android.widget.AbsListView;

/**
 * @author yanry
 *
 *         2016年3月15日
 */
public class PullDownAdapterView extends PullDownSource {
	private AbsListView pullView;

	public PullDownAdapterView(AbsListView pullView) {
		super(pullView);
		this.pullView = pullView;
	}

	@Override
	protected boolean isReadyToPull() {
		View firstChild = pullView.getChildAt(0);
		// 如果ListView中没有元素，也应该允许下拉刷新
		// 如果首个元素的上边缘，距离父布局值为0，就说明ListView滚动到了最顶部，此时应该允许下拉刷新
		return firstChild == null || (pullView.getFirstVisiblePosition() == 0 && firstChild.getTop() >= 0);
	}
}
