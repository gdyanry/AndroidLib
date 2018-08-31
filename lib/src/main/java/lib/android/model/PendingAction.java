package lib.android.model;

import java.util.concurrent.atomic.AtomicInteger;

import lib.android.entity.MainHandler;
import lib.android.interfaces.BooleanSupplier;
import lib.common.interfaces.Loggable;
import lib.common.model.Singletons;

/**
 * @author rongyu.yan
 * @date 2018/1/9
 */

public abstract class PendingAction implements Runnable, Loggable {
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
            debug("setup fail, action is pending currently!");
        } else {
            mark = atomicInteger.incrementAndGet();
            MainHandler mainHandler = Singletons.get(MainHandler.class);
            mainHandler.removeCallbacks(this);
            mainHandler.postDelayed(this, timeout);
            debug("[%s]setup: %s.", mark, timeout);
        }
    }

    public synchronized void knock(BooleanSupplier ifStop) {
        int tempNum = mark;
        if (mark == atomicInteger.get() && (ifStop == null || ifStop.get() && tempNum == atomicInteger.get())) {
            atomicInteger.incrementAndGet();
            Singletons.get(MainHandler.class).removeCallbacks(this);
            debug("[%s]finish.", mark);
            onFinish(false);
        }
    }

    protected abstract void onFinish(boolean isTimeout);

    @Override
    public synchronized void run() {
        atomicInteger.incrementAndGet();
        debug("[%s]timeout.", mark);
        onFinish(true);
    }
}
