package yanry.lib.android.model.runner;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.runner.Runner;

/**
 * Created by yanry on 2019/12/11.
 */
public class UiScheduleRunner implements Runner {
    @Override
    public void run(Runnable runnable) {
        CommonUtils.runOnUiThread(runnable);
    }

    @Override
    public void scheduleTimeout(Runnable runnable, long delay) {
        CommonUtils.scheduleTimeout(runnable, delay);
    }

    @Override
    public void cancelPendingTimeout(Runnable runnable) {
        CommonUtils.cancelPendingTimeout(runnable);
    }
}
