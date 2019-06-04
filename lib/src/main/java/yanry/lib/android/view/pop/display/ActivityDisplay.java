package yanry.lib.android.view.pop.display;

import android.app.Activity;
import android.support.annotation.NonNull;

import yanry.lib.android.interfaces.Function;
import yanry.lib.android.view.pop.AsyncDisplay;
import yanry.lib.android.view.pop.ShowData;

/**
 * Activity需要在onCreate()中调用{@link #notifyCreate(Function)}，在onDestroy()中调用{@link #notifyDismiss(Object)}。
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class ActivityDisplay<D extends ShowData> extends AsyncDisplay<D, Activity> {

    @Override
    protected void dismiss(Activity popInstance) {
        popInstance.finish();
    }

    @Override
    protected boolean isShowing(@NonNull Activity popInstance) {
        return true;
    }
}
