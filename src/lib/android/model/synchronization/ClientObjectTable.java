package lib.android.model.synchronization;

import java.util.List;
import java.util.Map;

import lib.common.model.dao.BaseDao;

/**
 * Created by rongyu.yan on 12/14/2016.
 */

public abstract class ClientObjectTable extends BaseDao.Table {
    /**
     * 本地数据状态：created, modified, deleted, synchronizing, synchronized
     */
    public static String data_state;

    String[] clientIdColumn;
    String[] serverIdColumn;

    public ClientObjectTable(BaseDao dao, SynchronizeClient client) {
        dao.super();
        clientIdColumn = getClientIdColumn();
        serverIdColumn = getServerIdColumn();
        client.tableInstances.put(getClass(), this);
    }

    protected abstract String[] getClientIdColumn();
    protected abstract String[] getServerIdColumn();

    @Override
    protected void addColumns(Map<String, String> columnDefinition) {
        columnDefinition.put(data_state, "integer not null");
    }

    @Override
    protected void statementsAfterCreation(List<String> stmts) {
        stmts.add(String.format("create index if not exists index_%s on %s(%s)", data_state, getClass().getSimpleName(), data_state));
    }
}
