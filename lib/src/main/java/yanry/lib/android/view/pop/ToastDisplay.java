package yanry.lib.android.view.pop;

import android.support.annotation.NonNull;
import android.widget.Toast;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.schedule.SyncDisplay;

public class ToastDisplay extends SyncDisplay<ContextShowData, Toast> {

    @Override
    protected Toast showData(Toast currentInstance, ContextShowData data) {
        if (currentInstance != null) {
            currentInstance.cancel();
        }
        Toast toast = Toast.makeText(data.getContext(), data.toString(), Toast.LENGTH_LONG);
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
