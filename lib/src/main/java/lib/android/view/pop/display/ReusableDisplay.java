package lib.android.view.pop.display;

import android.content.Context;

import lib.android.view.pop.Display;

public abstract class ReusableDisplay<D, V> extends Display<D, V> {

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
