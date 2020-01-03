package yanry.lib.android.model.log;

import android.util.Log;

import yanry.lib.java.model.log.LogHandler;
import yanry.lib.java.model.log.LogRecord;

/**
 * Created by yanry on 2020/1/2.
 */
public class BufferedLogcatHandler extends LogHandler {
    private BufferedLogcat bufferedLogcat;

    public BufferedLogcatHandler(BufferedLogcat bufferedLogcat) {
        this.bufferedLogcat = bufferedLogcat;
    }

    @Override
    protected void handleFormattedLog(LogRecord logRecord, String formattedLog) {
        bufferedLogcat.printLog(formattedLog);
    }

    @Override
    protected void catches(Object tag, Exception e) {
        Log.e(bufferedLogcat.getLogcatTag(), e.getMessage(), e);
    }
}
