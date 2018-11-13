package lib.android.view.pop.display;

import android.app.Activity;

import lib.android.interfaces.Function;
import lib.android.view.pop.AsyncDisplay;

/**
 * Activity需要在onCreate()中调用{@link #async(Function)}，在onDestroy()中调用{@link #notifyDismiss()}。
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class ActivityDisplay<D> extends AsyncDisplay<D, Activity> {

    @Override
    protected void dismiss(Activity popInstance) {
        popInstance.finish();
    }

    @Override
    protected boolean isShowing(Activity popInstance) {
        return true;
    }
}
