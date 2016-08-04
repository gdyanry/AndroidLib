/**
 * 
 */
package lib.android.model.dao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import lib.common.util.IOUtil;

/**
 * The database will be created by directly copying database file from the
 * assets folder. Runtime exception will be thrown on database creation error.
 * 
 * @author yanry
 *
 *         2016年2月5日
 */
public abstract class CopyDaoCreator implements DaoCreator {

	@Override
	public AndroidBaseDao createDao() {
		File file = getContextWrapper().getDatabasePath(getDbFileName());
		if (!file.isFile()) {
			// copy from assets
			File dir = file.getParentFile();
			if (!dir.isDirectory()) {
				dir.mkdirs();
			}
			try {
				InputStream is = getContextWrapper().getAssets().open(getDbFileName());
				FileOutputStream os = new FileOutputStream(file);
				IOUtil.transferStream(is, os);
				os.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new AndroidBaseDao(getContextWrapper(), getDbFileName(), getDbVersion(), isSupportConcurrent()) {

			@Override
			protected void instantiateDbObjects() {
			}

			@Override
			protected void onDbCreated(SQLiteDatabase db) {
			}

			@Override
			protected void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			}
		};
	}

	protected abstract ContextWrapper getContextWrapper();

	protected abstract String getDbFileName();

	/**
	 * 
	 * @return database version number, starting at 1.
	 */
	protected abstract int getDbVersion();

	protected abstract boolean isSupportConcurrent();
}
