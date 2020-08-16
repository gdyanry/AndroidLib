package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import yanry.lib.java.model.task.SingleThreadExecutor;

/**
 * 初始化放到子线程中执行的动画。
 * <p>
 * Created by yanry on 2020/5/27.
 */
public abstract class AsyncInitAnimate extends AnimateSegment implements Runnable {
    private static final int INITIALIZING = 1;
    private static final int INITIALIZED = 2;
    private SingleThreadExecutor initExecutor;
    private int initState;

    public AsyncInitAnimate(SingleThreadExecutor initExecutor) {
        this.initExecutor = initExecutor;
        initExecutor.enqueue(this, false);
    }

    protected abstract void init();

    protected abstract long doDraw(Canvas canvas);

    @Override
    protected void prepare() {
        super.prepare();
        if (initState == 0) {
            initExecutor.enqueue(this, true);
        }
    }

    @Override
    protected final long draw(Canvas canvas) {
        if (initState == INITIALIZED) {
            return doDraw(canvas);
        }
        getLogger().dd("initialization is slower than drawing: ", this);
        return 16;
    }

    @Override
    public final void run() {
        if (initState == 0) {
            initState = INITIALIZING;
            init();
            initState = INITIALIZED;
            seekTo(0);
            setPause(false);
        }
    }
}
