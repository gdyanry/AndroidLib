package yanry.lib.android.view.pop;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;

import yanry.lib.java.model.schedule.ReusableDisplay;

public abstract class FloatDisplay<D extends ContextShowData> extends ReusableDisplay<D, View> {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    @Override
    protected View createInstance(D data) {
        if (windowManager == null) {
            windowManager = (WindowManager) data.getContext().getSystemService(Context.WINDOW_SERVICE);
            layoutParams = getLayoutParams();
        }
        return getContentView(data.getContext());
    }

    @Override
    protected void showView(View instance) {
        windowManager.addView(instance, layoutParams);
    }

    @Override
    protected void dismiss(View popInstance) {
        windowManager.removeView(popInstance);
    }

    @Override
    protected boolean isShowing(@NonNull View popInstance) {
        return popInstance.getParent() != null;
    }

    protected abstract WindowManager.LayoutParams getLayoutParams();

    protected abstract View getContentView(Context context);
}
