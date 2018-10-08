package lib.android.model;

import android.util.Log;

import lib.common.model.log.LogFormatter;
import lib.common.model.log.LogHandler;
import lib.common.model.log.LogLevel;

public class AndroidLogHandler extends LogHandler {
    public AndroidLogHandler(LogFormatter formatter, LogLevel level) {
        super(formatter, level);
    }

    @Override
    protected void handleLog(LogLevel logLevel, Object tag, String log) {
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
        Log.println(priority, tag == null ? "tag:default" : tag.toString(), log);
    }

    @Override
    protected void catches(Object tag, Exception e) {
        Log.wtf(tag.toString(), e);
    }
}
