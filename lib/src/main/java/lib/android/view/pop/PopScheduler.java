package lib.android.view.pop;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import lib.android.util.CommonUtils;
import lib.android.view.pop.display.ToastDisplay;
import lib.common.model.log.Logger;

/**
 * 本类适用的场景为：需要为不同的数据弹出不同的界面，同一时刻最多只显示一个界面，比如显示推送通知。
 * 当前有数据正在显示的情况下，新来的数据可以采取替换当前数据界面或进入等待队列等策略，而被替换的数据也可以相应采取接受或拒绝等策略。
 */
public class PopScheduler {
    private static LinkedList<ShowTask> queue = new LinkedList<>();
    private static HashMap<PopScheduler, HashSet<PopScheduler>> conflictedSchedulers = new HashMap<>();
    private static HashMap<Object, PopScheduler> instances = new HashMap<>();
    ShowTask current;
    private LinkedList<Display> displays;

    private PopScheduler() {
        displays = new LinkedList<>();
        registerDisplay(new ToastDisplay());
    }

    public static void link(PopScheduler a, PopScheduler b) {
        addLink(a, b);
        addLink(b, a);
    }

    private static void addLink(PopScheduler scheduler, PopScheduler addTo) {
        HashSet<PopScheduler> linkedSchedulers = conflictedSchedulers.get(addTo);
        if (linkedSchedulers == null) {
            linkedSchedulers = new HashSet<>();
            conflictedSchedulers.put(addTo, linkedSchedulers);
        }
        linkedSchedulers.add(scheduler);
    }

    public static PopScheduler get(@NonNull Object tag) {
        if (tag == null) {
            throw new NullPointerException();
        }
        PopScheduler scheduler = instances.get(tag);
        if (scheduler == null) {
            scheduler = new PopScheduler();
            instances.put(tag, scheduler);
        }
        return scheduler;
    }

    static void loop() {
        Iterator<ShowTask> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ShowTask next = iterator.next();
            if (next.scheduler.getSchedulerOfDisplayingRequest() == null) {
                iterator.remove();
                Logger.getDefault().vv("loop and show: ", next.data);
                doShow(next);
            }
        }
    }

    private static void doShow(ShowTask task) {
        CommonUtils.runOnUiThread(() -> {
            task.onShow();
            task.display.show(task.context, task.data);
        });
        if (task.duration > 0) {
            CommonUtils.scheduleTimeout(task, task.duration);
        }
        task.scheduler.current = task;
    }

    public static void cancelByTag(Object tag) {
        // 清理队列
        Iterator<ShowTask> it = queue.iterator();
        while (it.hasNext()) {
            ShowTask request = it.next();
            if (request.tag == tag) {
                it.remove();
                CommonUtils.cancelPendingTimeout(request);
                Logger.getDefault().vv("cancelled by context: ", request.data);
            }
        }
        // 清理当前显示的窗口
        for (PopScheduler scheduler : instances.values()) {
            if (scheduler.current != null && scheduler.current.tag == tag) {
                scheduler.dismissCurrent();
            }
        }
        loop();
    }

    /**
     * 撤消显示所有的数据。
     *
     * @param dismissCurrent 是否关闭当前正在显示的界面。
     */
    public static void cancelAll(boolean dismissCurrent) {
        queue.clear();
        if (dismissCurrent) {
            for (PopScheduler scheduler : instances.values()) {
                scheduler.dismissCurrent();
            }
        }
    }

    public void cancel(boolean dismissCurrent) {
        Iterator<ShowTask> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ShowTask next = iterator.next();
            if (next.scheduler == this) {
                iterator.remove();
            }
        }
        if (dismissCurrent) {
            dismissCurrent();
        }
        loop();
    }

    public void registerDisplay(Display display) {
        if (!displays.contains(display)) {
            displays.add(display);
            display.scheduler = this;
        }
    }

    public void show(ShowTask request) {
        request.scheduler = this;
        // 寻找匹配的Handler
        for (Display handler : displays) {
            if (handler.accept(request.displayIndicator)) {
                Logger.getDefault().vv("find display " + handler + " for type: ", request.displayIndicator);
                request.display = handler;
                break;
            }
        }
        if (request.display == null) {
            Logger.getDefault().ww("no display found for type: ", request.displayIndicator);
            return;
        }
        // 清理队列
        Iterator<ShowTask> it = queue.iterator();
        while (it.hasNext()) {
            ShowTask next = it.next();
            if (request.scheduler == this && request.expelWaitingTask(next) && !next.rejectExpelled()) {
                it.remove();
                CommonUtils.cancelPendingTimeout(next);
                Logger.getDefault().vv("expelled from queue: ", next.data);
            }
        }
        // 处理当前正在显示的task
        ShowTask current = null;
        PopScheduler scheduler = getSchedulerOfDisplayingRequest();
        if (scheduler != null) {
            current = scheduler.current;
        }
        switch (request.getStrategy()) {
            case ShowTask.STRATEGY_SHOW_IMMEDIATELY:
                if (current != null && current.display.isShowing()) {
                    if (current.rejectDismissed()) {
                        // 当前正在显示的task不肯dismiss，只能放到队首等待
                        Logger.getDefault().vv("insert head: ", request.data);
                        queue.addFirst(request);
                    } else if (current.display != request.display) {
                        // handler不相同时才dismiss，否则只需要更换显示的数据就可以了
                        CommonUtils.cancelPendingTimeout(current);
                        Logger.getDefault().vv("dismiss on expelled: ", current.data);
                        current.onDismiss(true);
                        current.display.internalDismiss();
                        scheduler.current = null;
                    }
                }
                break;
            case ShowTask.STRATEGY_INSERT_HEAD:
                if (current != null && current.display.isShowing()) {
                    Logger.getDefault().vv("insert head: ", request.data);
                    queue.addFirst(request);
                }
                break;
            default:
                if (current != null && current.display.isShowing() || getNextToShow() != null) {
                    Logger.getDefault().vv("append tail: ", request.data);
                    queue.addLast(request);
                }
                break;
        }
        boolean show = !queue.contains(request);
        if (show) {
            Logger.getDefault().vv("show directly: ", request.data);
            doShow(request);
        } else if (current == null) {
            loop();
        }
    }

    private ShowTask getNextToShow() {
        HashSet<PopScheduler> schedulers = conflictedSchedulers.get(this);
        for (ShowTask request : queue) {
            if (request.scheduler == this || schedulers != null && schedulers.contains(request.scheduler)) {
                return request;
            }
        }
        return null;
    }

    /**
     * 获取当前调度器以及与当前调度器关联的调度器中正在显示的任务（如果有的话）所属的调节器实例。
     *
     * @return
     */
    private PopScheduler getSchedulerOfDisplayingRequest() {
        if (current != null) {
            return this;
        }
        HashSet<PopScheduler> schedulers = conflictedSchedulers.get(this);
        if (schedulers != null) {
            for (PopScheduler scheduler : schedulers) {
                if (scheduler.current != null) {
                    return scheduler;
                }
            }
        }
        return null;
    }

    private void dismissCurrent() {
        if (current != null) {
            CommonUtils.cancelPendingTimeout(current);
            if (current.display.isShowing()) {
                Logger.getDefault().vv("dismiss on cancelled: ", current.data);
                current.onDismiss(true);
                current.display.internalDismiss();
            }
            current = null;
        }
    }
}
