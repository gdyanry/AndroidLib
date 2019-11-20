/**
 * 
 */
package yanry.lib.android.model.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import yanry.lib.android.model.dao.AndroidBaseDao;
import yanry.lib.java.model.cache.CacheDataManager;

/**
 * @author yanry
 *
 *         2015年10月30日
 */
public class SqliteCacheDataManager extends CacheDataManager {
	private AndroidBaseDao dao;
	private String whereClause;
	private long retentionMillis;

	/**
	 * 
	 * @param ctx
	 * @param dbFileName
	 *            a database with the given name will be created if it doesn't
	 *            exist. This database has only one {@link TbCacheData} table.
	 * @param retentionHours
	 *            retention time of each cache data in database, starting from
	 *            its latest updated time. A value <=0 means disabling cache.
	 */
	public SqliteCacheDataManager(int memCacheSize, Context ctx, String dbFileName, int retentionHours) {
		super(memCacheSize);
		this.dao = new AndroidBaseDao(ctx, dbFileName, 1, true) {

			@Override
			protected void instantiateDbObjects() {
				new TbCacheData(this);
			}

			@Override
			protected boolean onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				return false;
			}

			@Override
			protected void onDbCreated(SQLiteDatabase db, String sql) {

			}
		};
		this.retentionMillis = retentionHours * 3600000L;
		whereClause = String.format("%s=?", TbCacheData.KEY);
		if (retentionHours > 0) {
			cleanExpireCache();
		}
	}

	public void cleanExpireCache() {
		String whereClause = String.format("%s<%s", TbCacheData.UPDATE_TIME, System.currentTimeMillis() - retentionMillis);
		dao.delete(TbCacheData.class, whereClause, null);
	}

	public void cleanUserCache() {
		memCache.evictAll();
		String whereClause = String.format("%s=1", TbCacheData.IS_USER_RELATED);
		dao.delete(TbCacheData.class, whereClause, null);
	}

	public AndroidBaseDao getDao() {
		return dao;
	}

	@Override
	protected String getLocalCache(String key) {
		String[] columns = { TbCacheData.DATA };
		Cursor cs = dao.query(TbCacheData.class, columns, whereClause, getWhereArgs(key), null);
		String value = null;
		if (cs.moveToNext()) {
			value = cs.getString(0);
		}
		cs.close();
		return value;
	}

	private String[] getWhereArgs(String key) {
		return new String[] { key };
	}

	@Override
	protected void saveToLocalCache(String key, Object data, boolean removeOnLogout) {
		ContentValues values = new ContentValues();
		values.put(TbCacheData.KEY, key);
		values.put(TbCacheData.DATA, data.toString());
		values.put(TbCacheData.UPDATE_TIME, System.currentTimeMillis());
		values.put(TbCacheData.IS_USER_RELATED, removeOnLogout);
		dao.insert(TbCacheData.class, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	@Override
	protected boolean ifBypassCache() {
		return retentionMillis <= 0;
	}

	@Override
	protected void removeLocalCache(String key) {
		dao.delete(TbCacheData.class, whereClause, getWhereArgs(key));
	}
}
