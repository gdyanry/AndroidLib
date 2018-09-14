package com.yanry.android.test;

import lib.android.model.AndroidLogHandler;
import lib.common.model.log.Logger;
import lib.common.model.log.SimpleFormatterBuilder;

public class MyLog {

    public static Logger get() {
        Logger logger = Logger.get("yanrylog");
        if (!logger.isReady()) {
            logger.addHandler(new AndroidLogHandler(new SimpleFormatterBuilder().method().stackDepth(10).build(), null));
        }
        return logger;
    }
}
