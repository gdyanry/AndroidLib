package yanry.lib.android.view.pop;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.log.Logger;

/**
 * 本类适用的场景为：需要为不同的数据弹出不同的界面，同一时刻最多只显示一个界面，比如显示推送通知。
 * 当前有数据正在显示的情况下，新来的数据可以采取替换当前数据界面或进入等待队列等策略，而被替换的数据也可以相应采取接受或拒绝等策略。
 */
public class PopScheduler {
    private static LinkedList<ShowData> queue = new LinkedList<>();
    private static HashMap<PopScheduler, HashSet<PopScheduler>> conflictedSchedulers = new HashMap<>();
    private static HashMap<Object, PopScheduler> instances = new HashMap<>();

    public static PopScheduler get(@NonNull Object tag) {
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

    private static void doShow(ShowData data) {
        data.display.show(data.context, data);
        data.onShow();
        if (data.duration > 0) {
            CommonUtils.scheduleTimeout(data, data.duration);
        }
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
                scheduler.dismissCurrent(null);
            }
        }
    }

    public static boolean hasScheduler(@NonNull Object tag) {
        return instances.get(tag) != null;
    }

    public static void cancelByTag(Object tag) {
        // 清理队列
        Iterator<ShowData> it = queue.iterator();
        while (it.hasNext()) {
            ShowData data = it.next();
            if (data.tag == tag) {
                it.remove();
                Logger.getDefault().vv("cancelled by tag: ", data);
            }
        }
        // 清理当前显示的窗口
        HashSet<Display> displaysToDismiss = new HashSet<>();
        for (PopScheduler scheduler : instances.values()) {
            if (scheduler.current.tag == tag) {
                scheduler.dismissCurrent(displaysToDismiss);
            }
        }
        rebalance(null, displaysToDismiss);
    }

    ShowData current;

    static void rebalance(ShowData showData, HashSet<Display> displaysToDismiss) {
        LinkedList<ShowData> tasksToShow = new LinkedList<>();
        if (showData != null) {
            // 此处调用是为了后面getConcernedShowingTasks()能得到正确的结果
            showData.scheduler.current = showData;
        }
        // display不相同时才dismiss，否则只需要更换显示的数据就可以了
        Iterator<ShowData> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ShowData next = iterator.next();
            if (next.scheduler.getConcernedShowingTasks().isEmpty()) {
                tasksToShow.add(next);
                iterator.remove();
                if (displaysToDismiss != null) {
                    displaysToDismiss.remove(next.display);
                }
                // 此处调用同样是为了后续getConcernedShowingTasks()能得到正确的结果
                next.scheduler.current = next;
            }
        }
        if (displaysToDismiss != null) {
            for (Display display : displaysToDismiss) {
                display.internalDismiss();
            }
        }
        if (showData != null) {
            doShow(showData);
        }
        for (ShowData data : tasksToShow) {
            Logger.getDefault().vv("loop and show: ", data);
            doShow(data);
        }
    }
    private HashMap<Class<? extends Display>, Display> displays;

    private PopScheduler() {
        displays = new HashMap<>();
        HashSet<PopScheduler> set = new HashSet<>();
        set.add(this);
        conflictedSchedulers.put(this, set);
    }

    public void addLink(PopScheduler... schedulers) {
        HashSet<PopScheduler> linkedSchedulers = conflictedSchedulers.get(this);
        linkedSchedulers.addAll(Arrays.asList(schedulers));
    }

    public void cancel(boolean dismissCurrent) {
        Iterator<ShowData> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ShowData next = iterator.next();
            if (next.scheduler == this) {
                iterator.remove();
            }
        }
        if (dismissCurrent) {
            HashSet<Display> displaysToDismiss = new HashSet<>();
            dismissCurrent(displaysToDismiss);
            rebalance(null, displaysToDismiss);
        }
    }

    public <T extends Display> T getDisplay(Class<T> displayType) {
        T display = (T) displays.get(displayType);
        if (display == null) {
            try {
                display = displayType.newInstance();
                display.setScheduler(this);
                displays.put(displayType, display);
            } catch (Exception e) {
                Logger.getDefault().catches(e);
            }
        }
        return display;
    }

    public void show(ShowData data, Class<? extends Display> displayType) {
        data.scheduler = this;
        data.display = getDisplay(displayType);
        // 根据request的需要清理队列
        Iterator<ShowData> it = queue.iterator();
        while (it.hasNext()) {
            ShowData next = it.next();
            if (next.scheduler == this && data.expelWaitingTask(next) && !next.rejectExpelled()) {
                it.remove();
                Logger.getDefault().vv("expelled from queue: ", next);
            }
        }
        // 处理当前正在显示的关联task
        HashSet<ShowData> concernedShowingTasks = getConcernedShowingTasks();
        HashSet<Display> displaysToDismiss = null;
        switch (data.getStrategy()) {
            case ShowData.STRATEGY_SHOW_IMMEDIATELY:
                for (ShowData showingTask : concernedShowingTasks) {
                    if (showingTask.rejectDismissed()) {
                        // 存在显示中的不愿结束的task，只能放到队首等待，此时调度器的暂稳态未发生改变，可直接返回
                        Logger.getDefault().vv("dismiss others failed, so insert head: ", data);
                        queue.addFirst(data);
                        return;
                    }
                }
                // 立即显示，先收集需要关闭的正在显示的display
                displaysToDismiss = new HashSet<>();
                for (ShowData showingTask : concernedShowingTasks) {
                    CommonUtils.cancelPendingTimeout(showingTask);
                    showingTask.scheduler.current = null;
                    Logger.getDefault().vv("dismiss on expelled: ", showingTask);
                    // 结束当前正在显示的关联任务
                    showingTask.onDismiss(true);
                    if (data.display != showingTask.display) {
                        displaysToDismiss.add(showingTask.display);
                    }
                }
                break;
            case ShowData.STRATEGY_INSERT_HEAD:
                if (!concernedShowingTasks.isEmpty()) {
                    Logger.getDefault().vv("insert head: ", data);
                    queue.addFirst(data);
                    return;
                }
                break;
            default:
                if (!concernedShowingTasks.isEmpty() || hasWaitingTask()) {
                    Logger.getDefault().vv("append tail: ", data);
                    queue.addLast(data);
                    return;
                }
        }
        Logger.getDefault().vv("show directly: ", data);
        // 显示及取消显示使得调度器处于非稳态，需要重新平衡到次稳态
        rebalance(data, displaysToDismiss);
    }

    private boolean hasWaitingTask() {
        HashSet<PopScheduler> schedulers = conflictedSchedulers.get(this);
        for (ShowData task : queue) {
            if (schedulers.contains(task.scheduler)) {
                return true;
            }
        }
        return false;
    }

    private HashSet<ShowData> getConcernedShowingTasks() {
        HashSet<ShowData> result = new HashSet<>();
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
            ShowData currentTask = this.current;
            current = null;
            CommonUtils.cancelPendingTimeout(currentTask);
            Logger.getDefault().vv("dismiss on cancelled: ", currentTask);
            currentTask.onDismiss(true);
            if (displaysToDismiss == null) {
                currentTask.display.internalDismiss();
            } else {
                displaysToDismiss.add(currentTask.display);
            }
        }
    }
}
