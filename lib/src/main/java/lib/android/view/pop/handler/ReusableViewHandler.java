package lib.android.view.pop.handler;

import android.content.Context;

import lib.android.view.pop.DataViewHandler;

public abstract class ReusableViewHandler<D, V> extends DataViewHandler<D, V> {

    @Override
    protected final V showData(V currentInstance, Context context, D data) {
        if (currentInstance == null) {
            currentInstance = createInstance(context);
        }
        if (currentInstance != null) {
            setData(currentInstance, data);
            if (!isShowing(currentInstance)) {
                showView(currentInstance);
            }
        }
        return currentInstance;
    }

    protected abstract V createInstance(Context context);

    protected abstract void setData(V instance, D data);

    protected abstract void showView(V instance);
}
