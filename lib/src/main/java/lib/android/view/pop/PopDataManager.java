package lib.android.view.pop;

import android.content.Context;
import android.os.Looper;

import java.util.Iterator;
import java.util.LinkedList;

import lib.android.entity.MainHandler;
import lib.android.util.CommonUtils;
import lib.common.model.Singletons;
import lib.common.model.log.Logger;

/**
 * 本类适用的场景为：需要为不同的数据弹出不同的界面，同一时刻最多只显示一个界面，比如显示推送通知。
 * 当前有数据正在显示的情况下，新来的数据可以采取替换当前数据界面或进入等待队列等策略，而被替换的数据也可以相应采取接受或拒绝等策略。
 */
public class PopDataManager {
    LinkedList<ShowTask> queue;
    LinkedList<DataViewHandler> dataViewHandlers;
    ShowTask currentTask;

    public PopDataManager() {
        queue = new LinkedList<>();
        dataViewHandlers = new LinkedList<>();
    }

    public void registerHandler(DataViewHandler handler) {
        dataViewHandlers.add(handler);
        handler.manager = this;
    }

    public void show(ShowTask task) {
        task.manager = this;
        // 寻找匹配的Handler
        for (DataViewHandler handler : dataViewHandlers) {
            if (handler.accept(task.typeId)) {
                Logger.getDefault().v("find handler %s for type: %s", handler, task.typeId);
                task.handler = handler;
                break;
            }
        }
        if (task.handler == null) {
            Logger.getDefault().e("no handler found for type: %s", task.typeId);
            return;
        }
        // 清理队列
        Iterator<ShowTask> it = queue.iterator();
        while (it.hasNext()) {
            ShowTask waitingTask = it.next();
            if (task.expelWaitingTask(waitingTask) && !waitingTask.rejectExpelled()) {
                Logger.getDefault().v("expelled from queue: %s", waitingTask.data);
                it.remove();
            }
        }
        // 处理当前正在显示的task
        switch (task.getStrategy()) {
            case ShowTask.STRATEGY_SHOW_IMMEDIATELY:
                if (currentTask != null && currentTask.handler.isShowing()) {
                    if (currentTask.rejectDismissed()) {
                        // 当前正在显示的task不肯dismiss，只能放到队首等待
                        Logger.getDefault().v("insert head: %s", task.data);
                        queue.addFirst(task);
                    } else if (currentTask.handler != task.handler) {
                        // handler不相同时才dismiss，否则只需要更换显示的数据就可以了
                        Singletons.get(MainHandler.class).removeCallbacks(currentTask);
                        Logger.getDefault().v("dismiss on expelled: %s", currentTask.data);
                        currentTask.handler.dismiss();
                        currentTask.onDismiss(true);
                        currentTask = null;
                    }
                }
                break;
            case ShowTask.STRATEGY_INSERT_HEAD:
                if (currentTask != null && currentTask.handler.isShowing()) {
                    Logger.getDefault().v("insert head: %s", task.data);
                    queue.addFirst(task);
                }
                break;
            default:
                if (currentTask != null && currentTask.handler.isShowing() || !queue.isEmpty()) {
                    Logger.getDefault().v("append tail: %s", task.data);
                    queue.addLast(task);
                }
                break;
        }
        boolean ifShow = !queue.contains(task);
        if (ifShow) {
            Logger.getDefault().v("show directly: %s", task.data);
            doShow(task);
        }
    }

    void loop() {
        ShowTask task = queue.pollFirst();
        if (task != null) {
            Logger.getDefault().v("loop and show: %s", task.data);
            doShow(task);
        }
    }

    private void doShow(ShowTask task) {
        CommonUtils.runOnUiThread(() -> {
            task.handler.show(task.context, task.data);
            task.onShow();
        });
        Singletons.get(MainHandler.class).postDelayed(task, task.duration);
        currentTask = task;
    }

    public void cancelTasksByContext(Context context) {
        Iterator<ShowTask> it = queue.iterator();
        while (it.hasNext()) {
            ShowTask task = it.next();
            if (task.context == context) {
                Logger.getDefault().v("cancelled by context: %s", task.data);
                it.remove();
            }
        }
        if (currentTask != null && currentTask.context == context) {
            dismissCurrent();
            loop();
        }
    }

    private void dismissCurrent() {
        Singletons.get(MainHandler.class).removeCallbacks(currentTask);
        if (currentTask.handler.isShowing()) {
            Logger.getDefault().v("dismiss on cancelled: %s", currentTask.data);
            currentTask.handler.dismiss();
            currentTask.onDismiss(true);
        }
        currentTask = null;
    }

    /**
     * 撤消显示所有的数据。
     *
     * @param dismissCurrent 是否关闭当前正在显示的界面。
     */
    public void cancelAll(boolean dismissCurrent) {
        queue.clear();
        if (dismissCurrent && currentTask != null) {
            dismissCurrent();
        }
    }
}
