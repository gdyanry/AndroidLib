package yanry.lib.android.view.animate.reactive.frame;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import yanry.lib.android.view.animate.reactive.AsyncInitAnimate;
import yanry.lib.java.model.task.SingleThreadExecutor;

/**
 * Created by yanry on 2020/5/15.
 */
public abstract class SingleFrameAnimate extends AsyncInitAnimate implements Runnable {
    private Drawable frame;

    public SingleFrameAnimate(SingleThreadExecutor decoder) {
        super(decoder);
    }

    protected long drawFrame(Canvas canvas, Drawable frame) {
        if (frame == null) {
            getLogger().ee("decode frame failed for animate: ", this);
        } else {
            frame.draw(canvas);
        }
        return 0;
    }

    protected abstract Drawable decodeFrame();

    @Override
    protected final void init() {
        if (frame == null) {
            frame = decodeFrame();
        }
    }

    @Override
    protected final long doDraw(Canvas canvas) {
        if (frame == null) {
            getLogger().ww("async decode frame failed for animate: ", this);
            frame = decodeFrame();
        }
        return drawFrame(canvas, frame);
    }
}
