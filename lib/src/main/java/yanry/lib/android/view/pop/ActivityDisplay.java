package yanry.lib.android.view.pop;

import android.app.Activity;
import android.support.annotation.NonNull;

import yanry.lib.java.model.schedule.imple.AsyncDisplay;

/**
 * Activity需要在onCreate()中调用{@link #notifyCreate(yanry.lib.java.model.schedule.AsyncBridge)}，在onDestroy()中调用{@link #notifyDismiss(Activity)} 。
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class ActivityDisplay<D extends ContextShowData> extends AsyncDisplay<D, Activity> {

    @Override
    protected void dismiss(Activity popInstance) {
        popInstance.finish();
    }

    @Override
    protected boolean isShowing(@NonNull Activity popInstance) {
        return true;
    }
}
