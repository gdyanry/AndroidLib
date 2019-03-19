/**
 * 
 */
package yanry.lib.android.model.bitmap.access;

import java.util.concurrent.Executor;

import yanry.lib.android.model.UrlSqliteAccess;
import yanry.lib.android.model.dao.AndroidBaseDao;

/**
 * @author yanry
 *
 * 2016年7月14日
 */
public class Level2BlobAccess extends UrlSqliteAccess {

	public Level2BlobAccess(AndroidBaseDao dao, String table, String keyField, String blobField) {
		super(dao, table, keyField, blobField);
	}

	@Override
	protected Executor getGenerationExecutor() {
		return null;
	}

}
