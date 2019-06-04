package yanry.lib.android.view.pop.display;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.android.view.pop.ShowData;
import yanry.lib.android.view.pop.SyncDisplay;

public class ToastDisplay extends SyncDisplay<ShowData, Toast> {

    @Override
    protected Toast showData(Toast currentInstance, Context context, ShowData data) {
        if (currentInstance != null) {
            currentInstance.cancel();
        }
        Toast toast = Toast.makeText(context, data.toString(), Toast.LENGTH_LONG);
        toast.show();
        CommonUtils.scheduleTimeout(() -> notifyDismiss(toast), 3500);
        return toast;
    }

    @Override
    protected void dismiss(Toast popInstance) {
        popInstance.cancel();
    }

    @Override
    protected boolean isShowing(@NonNull Toast popInstance) {
        return popInstance != null;
    }
}
