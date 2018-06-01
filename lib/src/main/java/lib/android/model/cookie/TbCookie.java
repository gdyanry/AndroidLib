package lib.android.model.cookie;

import java.util.List;
import java.util.Map;

import lib.android.model.dao.AndroidBaseDao;

/**
 * Created by yanrongyu on 16/9/21.
 */

public class TbCookie extends AndroidBaseDao.Table {
    public static String ID;
    public static String URI;
    public static String COOKIE;
    public static String TIMESTAMP;

    public TbCookie(AndroidBaseDao dao) {
        dao.super();
    }

    @Override
    protected void addColumns(Map<String, String> columnDefinition) {
        columnDefinition.put(ID, AndroidBaseDao.INT_PK_AI);
        columnDefinition.put(URI, "text not null default ''");
        columnDefinition.put(COOKIE, "text not null");
        columnDefinition.put(TIMESTAMP, "bigint not null default 0");
    }

    @Override
    protected String getConstrainStmt() {
        return null;
    }

    @Override
    protected String getTableOptions() {
        return null;
    }

    @Override
    protected void statementsAfterCreation(List<String> stmts) {

    }
}
