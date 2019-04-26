package yanry.lib.android.view.pop;

import android.content.Context;

/**
 * rongyu.yan
 * 2018/11/13
 **/
public abstract class SyncDisplay<D, V> extends Display<D, V> {

    @Override
    protected void show(Context context, D data) {
        setPopInstance(showData(getPopInstance(), context, data));
    }

    /**
     * @param currentInstance may be null.
     * @param context
     * @param data
     * @return
     */
    protected abstract V showData(V currentInstance, Context context, D data);
}
