package yanry.lib.android.model.log;

import android.util.Log;

import yanry.lib.java.model.schedule.Display;
import yanry.lib.java.model.schedule.Scheduler;
import yanry.lib.java.model.schedule.SchedulerManager;
import yanry.lib.java.model.schedule.ShowData;
import yanry.lib.java.model.schedule.extend.BufferedStringDisplay;

/**
 * Created by yanry on 2020/1/2.
 */
public class BufferedLogcat extends BufferedStringDisplay {
    private int logcatLevel;
    private String logcatTag;
    private long maxIdleTime;
    private long minFlushInterval;
    private Scheduler bufferScheduler;
    private Scheduler regularizeScheduler;

    /**
     * @param manager          应特别注意此SchedulerManager所使用的Logger不可再添加当前LogHandler！
     * @param logcatLevel      日志在logcat中输出所使用的等级，可用值为{@link Log}中定义的等级常量。
     * @param logcatTag        日志在logcat中输出所使用的tag。
     * @param maxIdleTime      最长闲置时间，超出此时间未写入新的日志则将缓冲已有的日志输出到Logcat中。
     * @param minFlushInterval 往Logcat中输出日志的最短间隔。
     */
    public BufferedLogcat(SchedulerManager manager, int logcatLevel, String logcatTag, long maxIdleTime, long minFlushInterval) {
        super(LogcatHandler.MAX_LOG_LEN, "--->>>" + System.lineSeparator(), System.lineSeparator(), true);
        this.logcatLevel = logcatLevel;
        this.logcatTag = logcatTag;
        this.maxIdleTime = maxIdleTime;
        this.minFlushInterval = minFlushInterval;
        bufferScheduler = manager.get(this);
        bufferScheduler.setDisplay(BufferedLogcat.class, this);
        LogcatRegularizeDisplay regularizeDisplay = new LogcatRegularizeDisplay();
        regularizeScheduler = manager.get(regularizeDisplay);
        regularizeScheduler.setDisplay(LogcatRegularizeDisplay.class, regularizeDisplay);
    }

    public void printLog(String logContent) {
        bufferScheduler.show(new ShowData().setDuration(maxIdleTime).setExtra(logContent), BufferedLogcat.class);
    }

    public String getLogcatTag() {
        return logcatTag;
    }

    @Override
    protected void onFlush(String segment) {
        if (minFlushInterval > 0) {
            ShowData data = new ShowData().setExtra(segment).setDuration(minFlushInterval).setStrategy(ShowData.STRATEGY_APPEND_TAIL);
            regularizeScheduler.show(data, LogcatRegularizeDisplay.class);
        } else {
            Log.println(logcatLevel, logcatTag, segment);
        }
    }

    private class LogcatRegularizeDisplay extends Display<ShowData> {
        @Override
        protected void internalDismiss() {
        }

        @Override
        protected void show(ShowData data) {
            Log.println(logcatLevel, logcatTag, data.getExtra().toString());
        }
    }
}
