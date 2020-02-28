package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Bitmap;

import java.util.Queue;

/**
 * 动画帧。
 * Created by yanry on 2020/2/21.
 */
public class Frame implements Runnable {
    private Bitmap bitmap;
    private int index;
    private Queue<Bitmap> recycledPool;

    Frame(Bitmap bitmap, int index, Queue<Bitmap> recycledPool) {
        this.bitmap = bitmap;
        this.index = index;
        this.recycledPool = recycledPool;
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
        return recycledPool != null;
    }

    @Override
    public void run() {
        if (recycledPool != null) {
            recycledPool.offer(bitmap);
        }
    }
}
