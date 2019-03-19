/**
 * 
 */
package yanry.lib.android.model.dao;

/**
 * @author yanry
 *
 *         2016年2月5日
 */
public interface DaoCreator {

	/**
	 * This might be time-consumed when the database didn't exist and being created, so it's
	 * better to be called on worker thread.
	 * 
	 * @return
	 */
	AndroidBaseDao createDao();
}
