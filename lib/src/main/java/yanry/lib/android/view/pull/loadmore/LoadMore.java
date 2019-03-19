/**
 * 
 */
package yanry.lib.android.view.pull.loadmore;

/**
 * @author yanry
 *
 * 2016年3月31日
 */
public interface LoadMore {

	void setEnable(boolean enable);
	
	boolean isLoading();
	
	void stopLoading();
	
	void setHasMore(boolean hasMore);
	
	void setOnLoadMoreListener(OnLoadMoreListener l);
}
