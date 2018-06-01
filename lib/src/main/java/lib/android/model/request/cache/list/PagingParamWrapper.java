/**
 * 
 */
package lib.android.model.request.cache.list;

/**
 * @author yanry
 *
 * 2016年6月7日
 */
public interface PagingParamWrapper {

	void setPageSize(int pageSize);
	
	void setMaxIdByTailItem(Object tailItem);
	
	void removeMaxId();
	
	Object getParam();
}
