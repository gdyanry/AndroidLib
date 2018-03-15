package lib.android.model;

import lib.android.entity.MainHandler;
import lib.android.interfaces.BooleanSupplier;

/**
 * @author rongyu.yan
 * @date 2018/1/9
 */

public abstract class PendingAction implements Runnable {
    private boolean isPending;

    public boolean isPending() {
        return isPending;
    }

    public void setup(long timeout) {
        if (isPending) {
        } else {
            isPending = true;
            MainHandler mainHandler = Singletons.get(MainHandler.class);
            mainHandler.removeCallbacks(this);
            mainHandler.postDelayed(this, timeout);
        }
    }

    public void knock(BooleanSupplier ifStop) {
        if (isPending && (ifStop == null || ifStop.get() && isPending)) {
            isPending = false;
            Singletons.get(MainHandler.class).removeCallbacks(this);
            onFinish(false);
        }
    }

    protected abstract void onFinish(boolean isTimeout);

    @Override
    public void run() {
        isPending = false;
        onFinish(true);
    }
}
