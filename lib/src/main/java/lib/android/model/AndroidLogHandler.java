package lib.android.model;

import android.util.Log;

import lib.common.model.log.LogFormatter;
import lib.common.model.log.LogHandler;
import lib.common.model.log.LogLevel;

public class AndroidLogHandler extends LogHandler {
    public static final String DEFAULT_TAG = "tag:default";
    private static final int MAX_LEN = 2500;
    private boolean splitLongLine;

    public AndroidLogHandler(LogFormatter formatter, LogLevel level, boolean splitLongLine) {
        super(formatter, level);
        this.splitLongLine = splitLongLine;
    }

    @Override
    protected void handleLog(LogLevel logLevel, Object tag, String log, int messageStart, int messageEnd) {
        int priority = 0;
        switch (logLevel) {
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
        String strTag = tag == null ? DEFAULT_TAG : tag.toString();
        if (splitLongLine) {
            int length = log.length();
            for (int i = 0; i < length; ) {
                int endIndex = Math.min(i + MAX_LEN, length);
                Log.println(priority, strTag, log.substring(i, endIndex));
                i = endIndex;
            }
        } else {
            Log.println(priority, strTag, log);
        }
    }

    @Override
    protected void catches(Object tag, Exception e) {
        Log.wtf(tag.toString(), e);
    }
}
