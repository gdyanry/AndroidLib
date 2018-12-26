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

    public static void link(PopScheduler a, PopScheduler b) {
        a.addLink(b);
        b.addLink(a);
    }

    private static void doShow(ShowTask task) {
        task.scheduler.current = task;
        task.display.show(task.context, task.data);
        task.onShow();
        if (task.duration > 0) {
            CommonUtils.scheduleTimeout(task, task.duration);
        }
    }

    ShowTask current;

    /**
     * 撤消显示所有的数据。
     *
     * @param dismissCurrent 是否关闭当前正在显示的界面。
     */
    public static void cancelAll(boolean dismissCurrent) {
        queue.clear();
        if (dismissCurrent) {
            for (PopScheduler scheduler : instances.values()) {
                scheduler.dismissCurrent(null);
            }
        }
    }

    public static void cancelByTag(Object tag) {
        // 清理队列
        Iterator<ShowTask> it = queue.iterator();
        while (it.hasNext()) {
            ShowTask request = it.next();
            if (request.tag == tag) {
                it.remove();
                CommonUtils.cancelPendingTimeout(request);
                Logger.getDefault().vv("cancelled by tag: ", request.data);
            }
        }
        // 清理当前显示的窗口
        HashSet<Display> displaysToDismiss = new HashSet<>();
        for (PopScheduler scheduler : instances.values()) {
            if (scheduler.current != null && scheduler.current.tag == tag) {
                scheduler.dismissCurrent(displaysToDismiss);
            }
        }
        loop(displaysToDismiss);
    }

    private LinkedList<Display> displays;

    private PopScheduler() {
        displays = new LinkedList<>();
        registerDisplay(new ToastDisplay());
        HashSet<PopScheduler> set = new HashSet<>();
        set.add(this);
        conflictedSchedulers.put(this, set);
    }

    static void loop(HashSet<Display> displaysToDismiss) {
        // display不相同时才dismiss，否则只需要更换显示的数据就可以了
        LinkedList<ShowTask> taskToShow = new LinkedList<>();
        Iterator<ShowTask> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ShowTask next = iterator.next();
            if (next.scheduler.getConcernedShowingTasks().isEmpty()) {
                taskToShow.add(next);
                iterator.remove();
                if (displaysToDismiss != null) {
                    displaysToDismiss.remove(next.display);
                }
            }
        }
        if (displaysToDismiss != null) {
            for (Display display : displaysToDismiss) {
                display.internalDismiss();
            }
        }
        for (ShowTask showTask : taskToShow) {
            Logger.getDefault().vv("loop and show: ", showTask.data);
            doShow(showTask);
        }
    }

    public void addLink(PopScheduler... schedulers) {
        HashSet<PopScheduler> linkedSchedulers = conflictedSchedulers.get(this);
        for (PopScheduler scheduler : schedulers) {
            linkedSchedulers.add(scheduler);
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
            HashSet<Display> displaysToDismiss = new HashSet<>();
            dismissCurrent(displaysToDismiss);
            loop(displaysToDismiss);
        }
    }

    public void registerDisplay(Display display) {
        if (!displays.contains(display)) {
            displays.add(display);
            display.scheduler = this;
        }
    }

    public void show(ShowTask request) {
        request.scheduler = this;
        // 寻找匹配的display
        for (Display display : displays) {
            if (display.accept(request.displayIndicator)) {
                request.display = display;
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
            if (next.scheduler == this && request.expelWaitingTask(next) && !next.rejectExpelled()) {
                it.remove();
                CommonUtils.cancelPendingTimeout(next);
                Logger.getDefault().vv("expelled from queue: ", next.data);
            }
        }
        // 处理当前正在显示的task
        HashSet<ShowTask> concernedShowingTasks = getConcernedShowingTasks();
        HashSet<Display> displaysToDismiss = null;
        switch (request.getStrategy()) {
            case ShowTask.STRATEGY_SHOW_IMMEDIATELY:
                for (ShowTask showingTask : concernedShowingTasks) {
                    if (showingTask.rejectDismissed()) {
                        // 正在显示的task不肯dismiss，只能放到队首等待
                        Logger.getDefault().vv("dismiss others failed, so insert head: ", request.data);
                        queue.addFirst(request);
                        break;
                    }
                }
                if (queue.peekFirst() != request) {
                    // 不在队首，那就是可以立即显示，先把当前显示的任务关掉
                    displaysToDismiss = new HashSet<>();
                    for (ShowTask showingTask : concernedShowingTasks) {
                        CommonUtils.cancelPendingTimeout(showingTask);
                        showingTask.scheduler.current = null;
                        Logger.getDefault().vv("dismiss on expelled: ", showingTask.data);
                        showingTask.onDismiss(true);
                        if (request.display != showingTask.display) {
                            displaysToDismiss.add(showingTask.display);
                        }
                    }
                }
                break;
            case ShowTask.STRATEGY_INSERT_HEAD:
                if (!concernedShowingTasks.isEmpty()) {
                    Logger.getDefault().vv("insert head: ", request.data);
                    queue.addFirst(request);
                }
                break;
            default:
                if (!concernedShowingTasks.isEmpty() || getNextToShow() != null) {
                    Logger.getDefault().vv("append tail: ", request.data);
                    queue.addLast(request);
                }
                break;
        }
        boolean show = !queue.contains(request);
        loop(displaysToDismiss);
        if (show) {
            Logger.getDefault().vv("show directly: ", request.data);
            doShow(request);
        }
    }

    private ShowTask getNextToShow() {
        HashSet<PopScheduler> schedulers = conflictedSchedulers.get(this);
        for (ShowTask request : queue) {
            if (schedulers.contains(request.scheduler)) {
                return request;
            }
        }
        return null;
    }

    private HashSet<ShowTask> getConcernedShowingTasks() {
        HashSet<ShowTask> result = new HashSet<>();
        HashSet<PopScheduler> schedulers = conflictedSchedulers.get(this);
        for (PopScheduler scheduler : schedulers) {
            if (scheduler.current != null) {
                result.add(scheduler.current);
            }
        }
        return result;
    }

    private void dismissCurrent(HashSet<Display> displaysToDismiss) {
        if (current != null) {
            ShowTask currentTask = this.current;
            current = null;
            CommonUtils.cancelPendingTimeout(currentTask);
            if (currentTask.display.isShowing()) {
                Logger.getDefault().vv("dismiss on cancelled: ", currentTask.data);
                currentTask.onDismiss(true);
                if (displaysToDismiss == null) {
                    currentTask.display.internalDismiss();
                } else {
                    displaysToDismiss.add(currentTask.display);
                }
            }
        }
    }
}
