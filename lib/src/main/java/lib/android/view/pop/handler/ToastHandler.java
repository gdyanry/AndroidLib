package lib.android.view.pop.handler;

import android.content.Context;
import android.widget.Toast;

import lib.android.view.pop.DataViewHandler;

public class ToastHandler extends DataViewHandler<Toast> {

    /**
     * 使用自定义Toast需要重写此方法。
     *
     * @param context
     * @param data
     * @return
     */
    protected Toast createToast(Context context, Object data) {
        return Toast.makeText(context, data.toString(), Toast.LENGTH_LONG);
    }

    @Override
    protected boolean accept(Object data) {
        return data instanceof CharSequence;
    }

    @Override
    protected Toast showData(Toast currentInstance, Context context, Object data) {
        if (currentInstance != null) {
            currentInstance.cancel();
        }
        Toast toast = createToast(context, data);
        toast.show();
        return toast;
    }

    @Override
    protected void dismiss(Toast popInstance) {
        popInstance.cancel();
    }

    @Override
    protected boolean isShowing(Toast popInstance) {
        return popInstance != null;
    }
}
