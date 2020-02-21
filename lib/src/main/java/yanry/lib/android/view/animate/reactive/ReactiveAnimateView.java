package yanry.lib.android.view.animate.reactive;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 承载绘制动画的View，适用于播放帧动画（结合{@link yanry.lib.android.view.animate.reactive.frame.AnimateFrameSegment}）或可交互动画的场景。
 */
public class ReactiveAnimateView extends View implements Runnable {
    private SegmentsHolder segmentsHolder;
    private boolean freeze;
    private long refreshInterval = 30;

    public ReactiveAnimateView(Context context) {
        this(context, null);
    }

    public ReactiveAnimateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 设置该View刷新的时间间隔，默认是30ms。
     *
     * @param refreshInterval 刷新时间，必须大于0。
     */
    public void setRefreshInterval(long refreshInterval) {
        if (refreshInterval > 0) {
            this.refreshInterval = refreshInterval;
        }
    }

    void setFreeze(boolean freeze) {
        if (this.freeze != freeze) {
            this.freeze = freeze;
            if (!freeze) {
                postInvalidate();
            }
        }
    }

    void setSegmentsHolder(SegmentsHolder segmentsHolder) {
        this.segmentsHolder = segmentsHolder;
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        boolean valid = segmentsHolder != null && segmentsHolder.check(this);
        if (valid) {
            segmentsHolder.prepareNext();
            segmentsHolder.draw(canvas);
            if (!freeze) {
                postDelayed(this, refreshInterval);
            }
        }
    }

    @Override
    public void invalidate() {
        removeCallbacks(this);
        super.invalidate();
    }

    @Override
    public void postInvalidate() {
        removeCallbacks(this);
        super.postInvalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (segmentsHolder != null) {
            segmentsHolder.unbindRenderView(this);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void run() {
        invalidate();
    }
}
