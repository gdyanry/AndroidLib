package yanry.lib.android.model;

import java.util.concurrent.atomic.AtomicInteger;

import yanry.lib.android.entity.MainHandler;
import yanry.lib.android.interfaces.BooleanSupplier;
import yanry.lib.java.model.Singletons;
import yanry.lib.java.model.log.Logger;

/**
 * @author rongyu.yan
 * @date 2018/1/9
 */

public abstract class PendingAction implements Runnable {
    private AtomicInteger atomicInteger;
    private int mark;

    public PendingAction() {
        atomicInteger = new AtomicInteger();
    }

    public boolean isPending() {
        return mark == atomicInteger.get();
    }

    public synchronized void setup(long timeout) {
        if (mark == atomicInteger.get()) {
            Logger.getDefault().vv("setup fail, action is pending currently!");
        } else {
            mark = atomicInteger.incrementAndGet();
            MainHandler mainHandler = Singletons.get(MainHandler.class);
            mainHandler.removeCallbacks(this);
            mainHandler.postDelayed(this, timeout);
            Logger.getDefault().v("[%s]setup: %s.", mark, timeout);
        }
    }

    public synchronized void knock(BooleanSupplier ifStop) {
        int tempNum = mark;
        if (mark == atomicInteger.get() && (ifStop == null || ifStop.get() && tempNum == atomicInteger.get())) {
            atomicInteger.incrementAndGet();
            Singletons.get(MainHandler.class).removeCallbacks(this);
            Logger.getDefault().v("[%s]finish.", mark);
            onFinish(false);
        }
    }

    protected abstract void onFinish(boolean isTimeout);

    @Override
    public final synchronized void run() {
        atomicInteger.incrementAndGet();
        Logger.getDefault().v("[%s]timeout.", mark);
        onFinish(true);
    }
}
