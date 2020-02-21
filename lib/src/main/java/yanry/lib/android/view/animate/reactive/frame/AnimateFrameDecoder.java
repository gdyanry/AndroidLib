package yanry.lib.android.view.animate.reactive.frame;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import yanry.lib.java.model.log.Logger;

/**
 * 序列帧解码执行线程。
 * <p>
 * Created by yanry on 2020/2/21.
 */
public class AnimateFrameDecoder extends Thread {
    private boolean terminated;
    private BlockingDeque<AnimateFrameSegment> deque;

    public AnimateFrameDecoder(String name) {
        super(name);
        deque = new LinkedBlockingDeque<>();
    }

    public AnimateFrameDecoder(ThreadGroup group, String name) {
        super(group, name);
    }

    /**
     * 结束线程。
     */
    public void terminate() {
        terminated = true;
        interrupt();
    }

    void enqueue(AnimateFrameSegment segment, boolean urgent) {
        if (terminated) {
            throw new IllegalStateException("decoder is terminated.");
        }
        if (urgent) {
            deque.offerFirst(segment);
        } else {
            deque.offerLast(segment);
        }
    }

    @Override
    public void run() {
        while (!terminated) {
            try {
                deque.takeFirst().decode();
            } catch (InterruptedException e) {
                Logger.getDefault().catches(e);
            }
        }
    }
}
