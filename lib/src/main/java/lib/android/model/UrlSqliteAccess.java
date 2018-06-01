/**
 * 
 */
package lib.android.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import lib.android.model.dao.AndroidBaseDao;
import lib.common.model.resourceaccess.UrlBytesAccess;
import lib.common.model.resourceaccess.UrlOption;

/**
 * @author yanry
 *
 *         2016年5月8日
 */
public abstract class UrlSqliteAccess extends UrlBytesAccess {
	private AndroidBaseDao dao;
	private String table;
	private String keyField;
	private String blobField;

	public UrlSqliteAccess(AndroidBaseDao dao, String table, String keyField, String blobField) {
		this.dao = dao;
		this.table = table;
		this.keyField = keyField;
		this.blobField = blobField;
	}

	public AndroidBaseDao getDao() {
		return dao;
	}
	
	public String getTable() {
		return table;
	}

	@Override
	protected byte[] getCacheValue(String key, UrlOption option) {
		String sql = String.format("select %s from %s where %s=?", blobField, table, keyField);
		String[] selectionArgs = { key };
		Cursor cs = dao.rawQuery(sql, selectionArgs);
		byte[] bytes = null;
		if (cs.moveToNext()) {
			bytes = cs.getBlob(0);
		}
		cs.close();
		return bytes;
	}

	@Override
	protected void cache(String key, UrlOption option, byte[] generated) {
		if (generated != null) {
			SQLiteDatabase db = dao.getDatabase(true);
			db.beginTransaction();
			ContentValues initialValues = new ContentValues(2);
			initialValues.put(keyField, key);
			initialValues.put(blobField, generated);
			db.insertWithOnConflict(table, null, initialValues, SQLiteDatabase.CONFLICT_REPLACE);
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}

}
