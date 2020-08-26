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

    public SingleFrameAnimate(SingleThreadExecutor initExecutor) {
        super(initExecutor);
    }

    protected void drawFrame(Canvas canvas, Drawable bitmap) {
        bitmap.draw(canvas);
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
        init();
        if (frame != null) {
            drawFrame(canvas, frame);
        } else {
            getLogger().ee("decode frame failed for animate: ", this);
        }
        return 0;
    }
}
