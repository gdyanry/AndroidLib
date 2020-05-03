package yanry.lib.android.view.pop;

import android.widget.Toast;

import androidx.annotation.NonNull;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.schedule.imple.SyncDisplay;

public class ToastDisplay extends SyncDisplay<ContextShowData, Toast> {

    @Override
    protected Toast showData(Toast currentInstance, ContextShowData data) {
        if (currentInstance != null) {
            currentInstance.cancel();
        }
        Toast toast = Toast.makeText(data.getContext(), data.toString(), Toast.LENGTH_LONG);
        toast.show();
        Singletons.get(UiScheduleRunner.class).schedule(() -> notifyDismiss(toast), (long) 3500);
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
