package yanry.lib.android.view.animate.reactive;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import yanry.lib.java.model.log.LogLevel;
import yanry.lib.java.model.log.Logger;
import yanry.lib.java.model.watch.BooleanHolderImpl;

/**
 * 承载绘制动画的View，适用于播放帧动画（结合{@link yanry.lib.android.view.animate.reactive.frame.AnimateFrameSegment}）或可交互动画的场景。
 */
public class ReactiveAnimateView extends View implements Runnable {
    SegmentsHolder segmentsHolder;
    private boolean freeze;
    private long refreshInterval = 30;
    private BooleanHolderImpl activeState = new BooleanHolderImpl(true);

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
            } else {
                // 实测存在以下情况：setFreeze(true)->onDraw()未回调->setActiveState(false)未调用->invalidate()无效，所以此处需要调用setActiveState(false)。
                setActiveState(false);
            }
        }
    }

    @Override
    protected final void onDraw(Canvas canvas) {
        boolean valid = segmentsHolder != null && segmentsHolder.check(this);
        if (valid) {
            segmentsHolder.prepareNext();
            if (segmentsHolder.draw(canvas) && !freeze) {
                postDelayed(this, refreshInterval);
                return;
            }
        }
        setActiveState(false);
    }

    private boolean setActiveState(boolean isActive) {
        if (activeState.setValue(isActive)) {
            Logger.getDefault().concat(1, LogLevel.Debug, "active state of animate view: ", this, ' ', isActive);
            return true;
        }
        return false;
    }

    @Override
    public void invalidate() {
        if (setActiveState(true)) {
            removeCallbacks(this);
            super.invalidate();
        }
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
        if (!freeze) {
            removeCallbacks(this);
            super.invalidate();
        }
    }
}
