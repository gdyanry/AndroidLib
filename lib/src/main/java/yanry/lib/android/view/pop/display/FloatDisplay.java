package yanry.lib.android.view.pop.display;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;

public abstract class FloatDisplay<D> extends ReusableDisplay<D, View> {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    @Override
    protected View createInstance(Context context) {
        if (windowManager == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            layoutParams = getLayoutParams();
        }
        return getContentView(context);
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
