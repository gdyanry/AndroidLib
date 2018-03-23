package lib.android.model;

import com.phicomm.speaker.interfaces.BooleanSupplier;
import com.phicomm.speaker.util.LogUtils;

import java.util.concurrent.atomic.AtomicInteger;

import lib.android.entity.MainHandler;

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
            LogUtils.yanry("setup fail, action is pending currently!");
        } else {
            mark = atomicInteger.incrementAndGet();
            MainHandler mainHandler = Singletons.get(MainHandler.class);
            mainHandler.removeCallbacks(this);
            mainHandler.postDelayed(this, timeout);
            LogUtils.yanry("[%s]setup: %s.", mark, timeout);
        }
    }

    public synchronized void knock(BooleanSupplier ifStop) {
        int tempNum = mark;
        if (mark == atomicInteger.get() && (ifStop == null || ifStop.get() && tempNum == atomicInteger.get())) {
            atomicInteger.incrementAndGet();
            Singletons.get(MainHandler.class).removeCallbacks(this);
            LogUtils.yanry("[%s]finish.", mark);
            onFinish(false);
        }
    }

    protected abstract void onFinish(boolean isTimeout);

    @Override
    public synchronized void run() {
        atomicInteger.incrementAndGet();
        LogUtils.yanry("[%s]timeout.", mark);
        onFinish(true);
    }
}
