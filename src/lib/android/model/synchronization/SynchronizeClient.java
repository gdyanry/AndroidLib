package lib.android.model.synchronization;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import lib.common.model.json.JSONArray;
import lib.common.model.json.JSONObject;
import lib.common.model.synchronization.ClientControlTable;
import lib.common.model.synchronization.SyncConst;
import lib.common.util.ReflectionUtil;

/**
 * 双向同步客户端，顺序执行同步请求，即只有处理完先提交的同步对象后才会处理后提交的同步对象。
 *
 * Created by rongyu.yan on 12/13/2016.
 */

public abstract class SynchronizeClient {
    private static final int DATA_STATE_CREATED = 0;
    private static final int DATA_STATE_MODIFIED = 1;
    private static final int DATA_STATE_DELETED = 2;
    private static final int DATA_STATE_SYNCHRONIZING = 3;
    private static final int DATA_STATE_SYNCHRONIZED = 4;

    private Queue<Class<? extends ClientObjectTable>> queue;
    Map<Class<? extends ClientObjectTable>, ClientObjectTable> tableInstances;

    public SynchronizeClient() {
        queue = new LinkedList<>();
        tableInstances = new HashMap<>();
        ReflectionUtil.initStaticStringFields(SyncConst.class);
    }

    public void synchronize(Class<? extends ClientObjectTable>... tables) {
        boolean start = queue.isEmpty() && tables.length > 0;
        for (Class<? extends ClientObjectTable> t : tables) {
            if (!queue.contains(t)) {
                queue.offer(t);
            }
        }
        if (start) {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            dequeue(db);
        }
    }

    public void processResponse(String table, JSONObject data) {
        // 响应数据格式：
        // {"sync_time":sync_time,"created":[[CLIENT_ID,SERVER_ID], ...],"modified":[SERVER_ID, ...],"deleted":[SERVER_ID, ...],"push":{"modified":[{"server_id":SERVER_ID, ...}, ...],"deleted":[SERVER_ID, ...]}}
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        // created
        JSONArray created = data.optJSONArray(SyncConst.created);
        if (created != null) {
            ContentValues values = new ContentValues(2);
            String whereClause = String.format("%s=?", ClientObjectTable.id);
            String[] whereArgs = {null};
            for (int i = 0; i < created.length(); i++) {
                JSONArray pair = created.getJSONArray(i);
                whereArgs[0] = pair.optString(0);
                values.clear();
                values.put(ClientObjectTable.server_id, pair.getLong(1));
                values.put(ClientObjectTable.data_state, DATA_STATE_SYNCHRONIZED);
                db.update(table, values, whereClause, whereArgs);
            }
        }
        // modified
        String serverIdSelection = String.format("%s=?", ClientObjectTable.server_id);
        String[] serverIdSelectionArgs = {null};
        JSONArray modified = data.optJSONArray(SyncConst.modified);
        if (modified != null) {
            String[] columns = {ClientObjectTable.data_state};
            ContentValues values = new ContentValues(1);
            for (int i = 0; i < modified.length(); i++) {
                serverIdSelectionArgs[0] = modified.optString(i);
                Cursor cursor = db.query(table, columns, serverIdSelection, serverIdSelectionArgs, null, null, null);
                if (cursor.moveToNext() && cursor.getInt(0) == DATA_STATE_SYNCHRONIZING) {
                    values.clear();
                    values.put(ClientObjectTable.data_state, DATA_STATE_SYNCHRONIZED);
                    db.update(table, values, serverIdSelection, serverIdSelectionArgs);
                }
                cursor.close();
            }
        }
        // deleted
        JSONArray deleted = data.optJSONArray(SyncConst.deleted);
        if (deleted != null) {
            for (int i = 0; i < deleted.length(); i++) {
                serverIdSelectionArgs[0] = deleted.optString(i);
                db.delete(table, serverIdSelection, serverIdSelectionArgs);
            }
        }
        // update log
        String tableWhere = TbSynchronizeControl.table + "=?";
        String[] tableWhereArgs = {table};
        ContentValues values = new ContentValues(2);
        values.put(TbSynchronizeControl.request_data, "");
        values.put(TbSynchronizeControl.sync_time, data.getLong(TbSynchronizeControl.sync_time));
        db.update(TbSynchronizeControl.class.getSimpleName(), values, tableWhere, tableWhereArgs);
        // push
        JSONObject push = data.optJSONObject(SyncConst.push);
        if (push != null) {
            // push delete
            JSONArray pushDelete = push.optJSONArray(SyncConst.deleted);
            if (pushDelete != null) {
                StringBuilder sb = new StringBuilder(ClientObjectTable.server_id).append(" in(");
                for (int i = 0; i < pushDelete.length(); i++) {
                    sb.append(pushDelete.get(i)).append(",");
                }
                sb.deleteCharAt(sb.length() - 1).append(")");
                db.delete(table, sb.toString(), null);
            }
            // push modified
            JSONArray pushModified = push.optJSONArray(SyncConst.modified);
            if (pushModified != null) {
                values = new ContentValues();
                for (int i = 0; i < pushModified.length(); i++) {
                    JSONObject item = pushModified.getJSONObject(i);
                    values.clear();
                    values.put(ClientObjectTable.data_state, DATA_STATE_SYNCHRONIZED);
                    for (String key : item.keySet()) {
                        values.put(key, item.optString(key));
                    }
                    String[] columns = {ClientObjectTable.data_state};
                    serverIdSelectionArgs[0] = item.optString(ClientObjectTable.server_id);
                    Cursor cursor = db.query(table, columns, serverIdSelection, serverIdSelectionArgs, null, null, null);
                    if (cursor.moveToNext()) {
                        if (cursor.getInt(0) == DATA_STATE_SYNCHRONIZED)
                            // update
                            db.update(table, values, serverIdSelection, serverIdSelectionArgs);
                    } else {
                        // insert
                        db.insert(table, null, values);
                    }
                    cursor.close();
                }
            }
        }
        // see if is pending
        String[] columns = {TbSynchronizeControl.if_pending};
        Cursor cursor = db.query(TbSynchronizeControl.class.getSimpleName(), columns, tableWhere, tableWhereArgs, null, null, null);
        if (cursor.moveToNext() && cursor.getInt(0) == 1) {
            cursor.close();
            dequeue(db);
        } else {
            cursor.close();
            db.setTransactionSuccessful();
        }
    }

    private void dequeue(SQLiteDatabase db) {
        Class<? extends ClientObjectTable> table = queue.peek();
        if (table == null) {
            db.setTransactionSuccessful();
            commitRequest();
        } else {
            String[] columns = {TbSynchronizeControl.sync_time, TbSynchronizeControl.request_data};
            String controlWhere = String.format("%s=?", TbSynchronizeControl.table);
            String[] controlArgs = {table.getSimpleName()};
            Cursor cursor = db.query(TbSynchronizeControl.class.getSimpleName(), columns, controlWhere, controlArgs, null, null, null);
            long syncTime = 0;
            if (cursor.moveToNext()) {
                String requestData = cursor.getString(1);
                boolean isPending = requestData != null && requestData.length() > 0;
                ContentValues values = new ContentValues(1);
                values.put(TbSynchronizeControl.if_pending, isPending ? 1 : 0);
                db.update(TbSynchronizeControl.class.getSimpleName(), values, controlWhere, controlArgs);
                if (isPending) {
                    cursor.close();
                    db.setTransactionSuccessful();
                    // send pending request
                    appendRequest(table, new JSONObject(requestData));
                    commitRequest();
                    return;
                } else {
                    syncTime = cursor.getLong(0);
                }
            } else {
                ContentValues values = new ContentValues(1);
                values.put(TbSynchronizeControl.table, table.getSimpleName());
                db.insert(TbSynchronizeControl.class.getSimpleName(), null, values);
            }
            cursor.close();
            // extract request data
            queue.poll();
            String syncWhere = String.format("%s in(%s,%s,%s)", ClientObjectTable.data_state, DATA_STATE_CREATED, DATA_STATE_MODIFIED, DATA_STATE_MODIFIED);
            cursor = db.query(table.getSimpleName(), null, syncWhere, null, null, null, null);
            // 请求数据格式：
            // {"sync_time":sync_time,"created":[{"id":CLIENT_ID, ...}, ...],"modified":[{"server_id":SERVER_ID, ...}, ...],"deleted":[SERVER_ID, ...]}
            JSONObject data = new JSONObject(false);
            data.put(ClientControlTable.sync_time, syncTime);
            int stateIndex = cursor.getColumnIndex(ClientObjectTable.data_state);
            ClientObjectTable t = tableInstances.get(table);
            int idColumnCount = t.clientIdColumn.length;
            while (cursor.moveToNext()) {
                switch (cursor.getInt(stateIndex)) {
                    case DATA_STATE_CREATED:
                        collectData(data, cursor, true, t);
                        break;
                    case DATA_STATE_DELETED:
                        JSONObject serverId = new JSONObject();
                        data.append(SyncConst.deleted, serverId);
                        Bundle bundle = cursor.getExtras();
                        for (int i = 0; i < idColumnCount; i++) {
                            serverId.put(t.clientIdColumn[i], bundle.get(t.serverIdColumn[i]));
                        }
                        break;
                    case DATA_STATE_MODIFIED:
                        collectData(data, cursor, false, t);
                        break;
                }
            }
            cursor.close();
            // log request data
            ContentValues values = new ContentValues(1);
            values.put(TbSynchronizeControl.request_data, data.toString());
            db.update(TbSynchronizeControl.class.getSimpleName(), values, controlWhere, controlArgs);
            // update data data_state
            values.clear();
            values.put(ClientObjectTable.data_state, DATA_STATE_SYNCHRONIZING);
            db.update(table.getSimpleName(), values, syncWhere, null);
            // iterate
            dequeue(db);
        }
    }

    private void collectData(JSONObject data, Cursor cursor, boolean isCreated, ClientObjectTable t) {
        JSONObject item = new JSONObject(false);
        Bundle bundle = cursor.getExtras();
        int idColumnCount = t.clientIdColumn.length;
        if (isCreated) {
            // no server id
            JSONObject clientId = new JSONObject();
            item.put(SyncConst.client_id, clientId);
            for (int i = 0; i < idColumnCount; i++) {
                String cidCol = t.clientIdColumn[i];
                clientId.put(cidCol, bundle.get(cidCol));
                bundle.remove(cidCol);
                bundle.remove(t.serverIdColumn[i]);
            }
        } else {
            // don't need client id
            JSONObject serverId = new JSONObject();
            item.put(SyncConst.server_id, serverId);
            for (int i = 0; i < idColumnCount; i++) {
                String cidCol = t.clientIdColumn[i];
                String sidCol = t.serverIdColumn[i];
                serverId.put(cidCol, bundle.get(sidCol));
                bundle.remove(cidCol);
                bundle.remove(sidCol);
            }
        }
        bundle.remove(ClientObjectTable.data_state);
        for (String key : bundle.keySet()) {
            item.put(key, bundle.get(key));
        }
        data.append(isCreated ? SyncConst.created : SyncConst.modified, item);
    }

    protected abstract SQLiteDatabase getWritableDatabase();

    protected abstract void appendRequest(Class<? extends ClientObjectTable> table, JSONObject data);

    protected abstract void commitRequest();

}
