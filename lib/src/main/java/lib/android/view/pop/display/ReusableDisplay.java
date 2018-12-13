package lib.android.view.pop.display;

import android.content.Context;

import lib.android.view.pop.SyncDisplay;

public abstract class ReusableDisplay<D, V> extends SyncDisplay<D, V> {

    @Override
    protected final V showData(V currentInstance, Context context, D data) {
        if (currentInstance == null || !enableReuse()) {
            currentInstance = createInstance(context);
        }
        if (currentInstance != null) {
            if (!isShowing(currentInstance)) {
                showView(currentInstance);
            }
            setData(currentInstance, data);
        }
        return currentInstance;
    }

    protected boolean enableReuse() {
        return true;
    }

    protected abstract V createInstance(Context context);

    protected abstract void setData(V instance, D data);

    protected abstract void showView(V instance);
}
