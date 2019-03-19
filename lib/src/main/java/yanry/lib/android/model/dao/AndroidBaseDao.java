package yanry.lib.android.model.dao;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.io.File;

import yanry.lib.java.model.dao.BaseDao;
import yanry.lib.java.model.log.Logger;

public abstract class AndroidBaseDao extends BaseDao {

    public static final String INT_PK_AI = "integer primary key autoincrement";

    private SQLiteOpenHelper openHelper;
    private File dbFile;

    /**
     * @param context           context to use to open or create the database
     * @param dbFileName        name of the database file, or null for an in-memory database
     * @param version           version number of the database (starting at 1); onUpgrade will
     *                          be used to upgrade the database
     * @param supportConcurrent only SDK 16 and above supports concurrency.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public AndroidBaseDao(Context context, String dbFileName, int version, boolean supportConcurrent) {
        openHelper = new SQLiteOpenHelper(context, dbFileName, null, version) {

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                onDbUpgrade(db, oldVersion, newVersion);
            }

            @Override
            public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                onDbUpgrade(db, oldVersion, newVersion);
            }

            @Override
            public void onCreate(final SQLiteDatabase db) {
                getDbObjectCreateStatements(new SqlExecutor() {

                    @Override
                    public void execute(String sql) {
                        db.execSQL(sql);
                    }
                });
                onDbCreated(db);
            }
        };
        if (supportConcurrent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            openHelper.setWriteAheadLoggingEnabled(true);
        }
        dbFile = context.getDatabasePath(dbFileName);
        Logger.getDefault().d("instantiate dao: %s, %s bytes", dbFileName, dbFile.length());
    }

    public SQLiteDatabase getDatabase(boolean writable) {
        return writable ? openHelper.getWritableDatabase() : openHelper.getReadableDatabase();
    }

    public long getDbFileLength() {
        return dbFile.length();
    }

    public Cursor query(Class<? extends AndroidBaseDao.DBObject> table, String[] columns, String whereClause,
                        String[] whereArgs, String orderBy) {
        return openHelper.getReadableDatabase().query(table.getSimpleName(), columns, whereClause, whereArgs, null,
                null, orderBy);
    }

    /**
     * @see {@link SQLiteDatabase#delete(String, String, String[])}.
     */
    public int delete(Class<? extends AndroidBaseDao.DBObject> table, String whereClause, String[] whereArgs) {
        return openHelper.getWritableDatabase().delete(table.getSimpleName(), whereClause, whereArgs);
    }

    /**
     * @see {@link SQLiteDatabase #insertWithOnConflict(String, String, ContentValues, int)}
     */
    public synchronized long insert(Class<? extends AndroidBaseDao.DBObject> table, ContentValues values,
                                    int conflictAlgorithm) {
        if (values != null && values.size() > 0) {
            return openHelper.getWritableDatabase().insertWithOnConflict(table.getSimpleName(), null, values,
                    conflictAlgorithm);
        }
        return -1;
    }

    /**
     * @see {@link SQLiteDatabase #update(String, ContentValues, String, String[])}
     */
    public synchronized int update(Class<? extends AndroidBaseDao.DBObject> table, ContentValues values,
                                   String whereClause, String[] whereArgs) {
        return openHelper.getWritableDatabase().update(table.getSimpleName(), values, whereClause, whereArgs);
    }

    /**
     * @see {@link SQLiteDatabase #rawQuery(String, String[])}
     */
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return openHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
    }

    /**
     * @see {@link SQLiteDatabase #execSQL(String, Object[])}
     */
    public void execSql(String sql, Object[] bindArgs) {
        openHelper.getWritableDatabase().execSQL(sql, bindArgs);
    }

    @Override
    protected String getCreateTableStmt(String tableName) {
        return "create table if not exists " + tableName;
    }

    @Override
    protected String getCreateViewStmt(String viewName) {
        return "create view if not exists " + viewName;
    }

    @Override
    protected String wrapField(String fieldName) {
        return String.format("\"%s\"", fieldName);
    }

    /**
     * Called when the database needs to be upgraded. The implementation should
     * use this method to drop tables, add tables, or do anything else it needs
     * to upgrade to the new schema version. If you add new columns you can use
     * ALTER TABLE to insert them into a live table. If you rename or remove
     * columns you can use ALTER TABLE to rename the old table, then create the
     * new table and then populate the new table with the contents of the old
     * table. This method executes within a transaction. If an exception is
     * thrown, all changes will automatically be rolled back.
     *
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    protected abstract void onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    /**
     * Called when the database is created for the first time. Note that all
     * tables have been created by now.
     *
     * @param db
     */
    protected abstract void onDbCreated(SQLiteDatabase db);

    public abstract class WriteTransaction {
        public void start() {
            SQLiteDatabase db = openHelper.getWritableDatabase();
            // simple test shows that different transaction mode has little
            // impact on performance --!
            db.beginTransactionNonExclusive();
            try {
                inTransaction(db);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                db.endTransaction();
            }
        }

        protected abstract void inTransaction(SQLiteDatabase db) throws Exception;
    }
}
