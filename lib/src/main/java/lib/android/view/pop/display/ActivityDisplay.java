package lib.android.view.pop.display;

import android.app.Activity;

import lib.android.view.pop.AsyncDisplay;

/**
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
