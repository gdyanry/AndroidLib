package yanry.lib.android.model;

import android.util.Log;

import yanry.lib.java.model.log.LogHandler;
import yanry.lib.java.model.log.LogRecord;

public class LogcatHandler extends LogHandler {
    private static final int MAX_LEN = 2500;
    private boolean splitLongLine;

    public LogcatHandler(boolean splitLongLine) {
        this.splitLongLine = splitLongLine;
    }

    @Override
    protected void handleFormattedLog(LogRecord logRecord, String formattedLog) {
        int priority = 0;
        switch (logRecord.getLevel()) {
            case Verbose:
                priority = Log.VERBOSE;
                break;
            case Debug:
                priority = Log.DEBUG;
                break;
            case Info:
                priority = Log.INFO;
                break;
            case Warn:
                priority = Log.WARN;
                break;
            case Error:
                priority = Log.ERROR;
                break;
        }
        Object tag = logRecord.getTag();
        String strTag = tag == null ? "yanry.lib" : tag.toString();
        if (splitLongLine) {
            int length = formattedLog.length();
            for (int i = 0; i < length; ) {
                int endIndex = Math.min(i + MAX_LEN, length);
                Log.println(priority, strTag, formattedLog.substring(i, endIndex));
                i = endIndex;
            }
        } else {
            Log.println(priority, strTag, formattedLog);
        }
    }

    @Override
    protected void catches(Object tag, Exception e) {
        /*
         * Log.wtf() and Log.e() both have the same priority, ERROR.
         *
         * The difference is that Log.wtf() calls for onTerribleFailure() call back, which
         * "Report a serious error in the current process. May or may not cause the process to terminate (depends on system settings)."
         *
         * So, in other words, Log.wtf() could crash your app.
         */
        Log.e(tag == null ? getClass().getSimpleName() : tag.toString(), e.getMessage(), e);
    }
}