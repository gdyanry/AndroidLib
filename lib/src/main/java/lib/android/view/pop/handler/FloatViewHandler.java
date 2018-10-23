package lib.android.view.pop.handler;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;

public abstract class FloatViewHandler<D> extends ReusableViewHandler<D, View> {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    @Override
    protected View createInstance(Context context) {
        if (windowManager == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            layoutParams = getLayoutParams();
        }
        return getContentView();
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
    protected boolean isShowing(View popInstance) {
        return popInstance.getParent() != null;
    }

    protected abstract WindowManager.LayoutParams getLayoutParams();

    protected abstract View getContentView();
}
