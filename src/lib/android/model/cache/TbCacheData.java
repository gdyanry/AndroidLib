/**
 * 
 */
package lib.android.model.cache;

import java.util.List;
import java.util.Map;

import lib.android.model.dao.AndroidBaseDao;

/**
 * @author yanry
 *
 * 2015年10月28日
 */
public class TbCacheData extends AndroidBaseDao.Table {
	public static String _ID;
	public static String KEY;
	public static String DATA;
	public static String UPDATE_TIME;
	public static String IS_USER_RELATED;
	
	public TbCacheData(AndroidBaseDao dao) {
		dao.super();
	}

	@Override
	protected void addColumns(Map<String, String> columnDefinition) {
		columnDefinition.put(_ID, AndroidBaseDao.INT_PK_AI);
		columnDefinition.put(KEY, "text unique on conflict replace");
		columnDefinition.put(DATA, "text not null");
		columnDefinition.put(UPDATE_TIME, "bigint not null default 0");
		columnDefinition.put(IS_USER_RELATED, "bit not null default 0");
	}

	@Override
	protected String getConstrainStmt() {
		return null;
	}

	@Override
	protected String getTableOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void statementsAfterCreation(List<String> stmts) {
		// TODO Auto-generated method stub
		
	}
}
