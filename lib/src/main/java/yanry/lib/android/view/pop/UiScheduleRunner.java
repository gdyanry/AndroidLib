package yanry.lib.android.view.pop;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.schedule.ScheduleRunner;

/**
 * Created by yanry on 2019/12/11.
 */
public class UiScheduleRunner implements ScheduleRunner {
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
