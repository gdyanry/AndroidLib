package yanry.lib.android.model.cookie;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import yanry.lib.android.model.dao.AndroidBaseDao;

/**
 * Created by yanrongyu on 16/9/21.
 */

public class LocalCookieStore extends AndroidBaseDao implements CookieStore {
    private InMemoryCookieStore cookieStore;
    private HashMap<HttpCookie, Integer> cookieIds;

    public LocalCookieStore(Context context, String dbName, int dbVersion) {
        super(context, dbName, dbVersion, true);
        cookieStore = new InMemoryCookieStore();
        cookieIds = new HashMap<>();
    }

    public void init() {
        String[] columns = {TbCookie.ID, TbCookie.URI, TbCookie.COOKIE};
        Cursor cursor = query(TbCookie.class, columns, null, null, TbCookie.TIMESTAMP);
        if (cursor != null) {
            HashSet<Integer> deleteIds = new HashSet<>();
            // initial cookie store from database to memory
            while (cursor.moveToNext()) {
                List<HttpCookie> cookies = HttpCookie.parse(cursor.getString(2));
                for (HttpCookie cookie : cookies) {
                    int id = cursor.getInt(0);
                    if (cookie.hasExpired()) {
                        deleteIds.add(id);
                    } else {
                        String uri = cursor.getString(1);
                        cookieStore.add(uri.length() > 0 ? URI.create(uri) : null, cookie);
                        cookieIds.put(cookie, id);
                    }
                }
            }
            cursor.close();
            // delete expire cookies
            if (!deleteIds.isEmpty()) {
                StringBuilder where = new StringBuilder(String.format("%s in(", TbCookie.ID));
                for (int id : deleteIds) {
                    where.append(id).append(",");
                }
                where.deleteCharAt(where.length() - 1).append(")");
                delete(TbCookie.class, where.toString(), null);
            }
        }
    }

    @Override
    protected boolean onDbUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        return false;
    }

    @Override
    protected void onDbCreated(SQLiteDatabase db, String sql) {

    }

    @Override
    protected void instantiateDbObjects() {
        new TbCookie(this);
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        if (cookie != null && cookie.getMaxAge() != 0) {
            cookieStore.add(uri, cookie);
            // insert or update database
            ContentValues values = new ContentValues();
            values.put(TbCookie.URI, uri == null ? "" : uri.toString());
            values.put(TbCookie.COOKIE, cookie.toString());
            values.put(TbCookie.TIMESTAMP, System.currentTimeMillis());
            Integer id = cookieIds.get(cookie);
            if (id == null) {
                id = (int) insert(TbCookie.class, values, SQLiteDatabase.CONFLICT_REPLACE);
            } else {
                update(TbCookie.class, values, String.format("%s=%s", TbCookie.ID, id), null);
            }
            cookieIds.put(cookie, id);
        }
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return cookieStore.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookieStore.getCookies();
    }

    @Override
    public List<URI> getURIs() {
        return cookieStore.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        if (cookie != null) {
            Integer id = cookieIds.remove(cookie);
            if (id != null) {
                delete(TbCookie.class, String.format("%s=%s", TbCookie.ID, id), null);
            }
        }
        return cookieStore.remove(uri, cookie);
    }

    @Override
    public boolean removeAll() {
        cookieIds.clear();
        execSql("delete from " + TbCookie.class.getSimpleName(), null);
        return cookieStore.removeAll();
    }
}
