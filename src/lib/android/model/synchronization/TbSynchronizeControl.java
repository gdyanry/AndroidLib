package lib.android.model.synchronization;

import java.util.List;
import java.util.Map;

import lib.common.model.dao.BaseDao;
import lib.common.model.synchronization.ClientControlTable;

/**
 * Created by rongyu.yan on 12/14/2016.
 */

public class TbSynchronizeControl extends ClientControlTable {
    public TbSynchronizeControl(BaseDao dao) {
        super(dao);
    }

    @Override
    protected void addColumns(Map<String, String> columnDefinition) {
        columnDefinition.put(table, "text primary key");
        columnDefinition.put(sync_time, "integer not null default 0");
        columnDefinition.put(request_data, "text not null default ''");
        columnDefinition.put(if_pending, "integer not null default 0");
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
