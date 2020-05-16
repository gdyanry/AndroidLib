package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.task.SingleThreadExecutor;

/**
 * Created by yanry on 2020/5/15.
 */
public abstract class SingleFrameSegment extends AnimateSegment implements Runnable {
    private SingleThreadExecutor decoder;
    private boolean startDecoding;
    private Drawable frame;

    public SingleFrameSegment(SingleThreadExecutor decoder) {
        this.decoder = decoder;
        decoder.enqueue(this, false);
    }

    protected abstract Drawable decodeFrame();

    @Override
    protected void prepare() {
        super.prepare();
        if (!startDecoding) {
            decoder.enqueue(this, true);
        }
    }

    @Override
    protected long draw(Canvas canvas) {
        if (frame != null) {
            frame.draw(canvas);
        } else {
            getLogger().dd("decoding is slower than drawing: ", this);
        }
        return 0;
    }

    @Override
    public void run() {
        if (!startDecoding) {
            startDecoding = true;
            frame = decodeFrame();
            setPause(false);
        }
    }
}
