package lib.android.view.pop.handler;

import android.content.Context;
import android.widget.Toast;

import lib.android.entity.MainHandler;
import lib.android.view.pop.DataViewHandler;
import lib.common.model.Singletons;

public class ToastHandler extends DataViewHandler<Object, Toast> implements Runnable {

    @Override
    protected boolean accept(Object typeId) {
        return typeId == ToastHandler.class;
    }

    @Override
    protected Toast showData(Toast currentInstance, Context context, Object data) {
        if (currentInstance != null) {
            currentInstance.cancel();
        }
        Toast toast = Toast.makeText(context, data.toString(), Toast.LENGTH_LONG);
        toast.show();
        Singletons.get(MainHandler.class).postDelayed(this, 3500);
        return toast;
    }

    @Override
    protected void dismiss(Toast popInstance) {
        Singletons.get(MainHandler.class).removeCallbacks(this);
        popInstance.cancel();
    }

    @Override
    protected boolean isShowing(Toast popInstance) {
        return popInstance != null;
    }

    @Override
    public void run() {
        notifyDismiss();
    }
}
