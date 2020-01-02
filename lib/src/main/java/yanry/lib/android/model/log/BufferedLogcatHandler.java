package yanry.lib.android.model.log;

import android.util.Log;

import yanry.lib.java.model.BufferedStringDisplay;
import yanry.lib.java.model.log.LogHandler;
import yanry.lib.java.model.log.LogRecord;
import yanry.lib.java.model.schedule.Scheduler;
import yanry.lib.java.model.schedule.ShowData;

/**
 * Created by yanry on 2020/1/2.
 */
public class BufferedLogcatHandler extends LogHandler {
    private Scheduler logScheduler;
    private int logcatLevel;
    private String logcatTag;
    private long bufferTime;

    /**
     * @param logScheduler 应特别注意此Scheduler所在的SchedulerManager所使用的Logger不可再添加当前LogHandler！
     * @param logcatLevel  日志在logcat中输出所使用的等级，可用值为{@link Log}中定义的等级常量。
     * @param logcatTag    日志在logcat中输出所使用的tag。
     * @param bufferTime
     */
    public BufferedLogcatHandler(Scheduler logScheduler, int logcatLevel, String logcatTag, long bufferTime) {
        this.logScheduler = logScheduler;
        this.logcatLevel = logcatLevel;
        this.logcatTag = logcatTag;
        this.bufferTime = bufferTime;
        logScheduler.setDisplay(BufferedLogcatDisplay.class, new BufferedLogcatDisplay());
    }

    @Override
    protected void handleFormattedLog(LogRecord logRecord, String formattedLog) {
        logScheduler.show(new ShowData().setDuration(bufferTime).setExtra(formattedLog), BufferedLogcatDisplay.class);
    }

    @Override
    protected void catches(Object tag, Exception e) {
        Log.e(logcatTag, e.getMessage(), e);
    }

    private class BufferedLogcatDisplay extends BufferedStringDisplay {

        public BufferedLogcatDisplay() {
            super(LogcatHandler.MAX_LOG_LEN, "--->>>" + System.lineSeparator(), System.lineSeparator(), true);
        }

        @Override
        protected void onFlush(String segment) {
            Log.println(logcatLevel, logcatTag, segment);
        }
    }
}
