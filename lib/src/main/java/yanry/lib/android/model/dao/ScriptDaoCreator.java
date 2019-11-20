/**
 * 
 */
package yanry.lib.android.model.dao;

import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Database will be created by executing sql statement from a script file in the
 * assets folder. Runtime exception will be thrown on database creation error.
 * 
 * @author yanry
 *
 *         2016年2月5日
 */
public abstract class ScriptDaoCreator implements DaoCreator {

	@Override
	public AndroidBaseDao createDao() {
		return new AndroidBaseDao(getContextWrapper(), getScriptFileName(), getDbVersion(), isSupportConcurrent()) {

			@Override
			protected void instantiateDbObjects() {
			}

			@Override
			protected boolean onDbVersionChange(SQLiteDatabase db, int oldVersion, int newVersion) {
				return false;
			}

			@Override
			protected void onDbCreated(SQLiteDatabase db, String sql) {
				db.beginTransactionNonExclusive();
				try {
					BufferedReader br;
					br = new BufferedReader(new InputStreamReader(
							getContextWrapper().getAssets().open(getScriptFileName()), getScriptCharset()));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						if (buildStatement(sb, line)) {
							db.execSQL(sb.toString());
							// clear buffer
							sb.delete(0, sb.length());
						}
					}
					db.setTransactionSuccessful();
				} catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					db.endTransaction();
				}
			}
		};
	}

	protected abstract ContextWrapper getContextWrapper();

	protected abstract String getScriptFileName();

	protected abstract String getScriptCharset();

	/**
	 * 
	 * @return database version number, starting at 1.
	 */
	protected abstract int getDbVersion();

	protected abstract boolean isSupportConcurrent();

	/**
	 * 
	 * @param builder
	 * @param newLine
	 * @return return true if a complete sql statement is built.
	 */
	protected abstract boolean buildStatement(StringBuilder builder, String newLine);
}
