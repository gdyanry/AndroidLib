package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动画帧。
 * Created by yanry on 2020/2/21.
 */
public class Frame implements Runnable {
    private Bitmap bitmap;
    private int index;
    private HashMap<Bitmap, AtomicInteger> bmpLock;

    Frame(Bitmap bitmap, int index, HashMap<Bitmap, AtomicInteger> bmpLock) {
        this.bitmap = bitmap;
        this.index = index;
        this.bmpLock = bmpLock;
        if (bmpLock != null) {
            AtomicInteger state = bmpLock.get(bitmap);
            if (state == null) {
                state = new AtomicInteger();
                bmpLock.put(bitmap, state);
            }
            state.set(AnimateFrameSegment.BMP_STATE_IN_USE);
        }
    }

    /**
     * 帧位图。
     *
     * @return
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * 该帧在序列帧中的序号。
     *
     * @return
     */
    public int getIndex() {
        return index;
    }

    boolean isRecyclable() {
        return bmpLock != null;
    }

    @Override
    public void run() {
        if (bmpLock != null) {
            bmpLock.get(bitmap).compareAndSet(AnimateFrameSegment.BMP_STATE_TO_BE_IDLE, AnimateFrameSegment.BMP_STATE_IDLE);
        }
    }
}
