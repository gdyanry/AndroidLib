package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Collections;

import yanry.lib.android.model.runner.UiScheduleRunner;
import yanry.lib.java.model.Singletons;

/**
 * 动画片段容器，具备以下特性：
 * - 可同时播放多个动画片段，互不干扰。
 * - 可动态无缝切换AnimateView，当新的AnimateView未就绪时仍然在旧的AnimateView上绘制，切换过程无中断感。
 * - 支持暂停所有动画片段。
 */
public class SegmentsHolder implements Runnable {
    private ArrayList<AnimateSegment> segments;
    private ArrayList<AnimateSegment> temp;
    private ReactiveAnimateView renderView;
    private ReactiveAnimateView pendingView;
    private boolean isPreparing;

    public SegmentsHolder() {
        segments = new ArrayList<>();
        temp = new ArrayList<>();
    }

    /**
     * 将该容器绑定到AnimateView中。
     *
     * @param view
     */
    public void bindRenderView(ReactiveAnimateView view) {
        view.segmentsHolder = this;
        if (pendingView != view && renderView != view) {
            pendingView = view;
            view.postInvalidate();
        }
    }

    /**
     * 将该容器与指定AnimateView解绑，AnimateView在onDetachedFromWindow()回调中自动调用该方法，一般不需要手动调用。
     *
     * @param renderView
     */
    public void unbindRenderView(ReactiveAnimateView renderView) {
        if (this.renderView == renderView) {
            this.renderView = null;
            // 清理
            Singletons.get(UiScheduleRunner.class).post(this);
        }
    }

    /**
     * @return 该容器是否已绑定到某个AnimateView中。
     */
    public boolean isBound() {
        return pendingView != null || renderView != null;
    }

    boolean check(ReactiveAnimateView view) {
        if (pendingView == view) {
            this.renderView = view;
            pendingView = null;
            return true;
        }
        return this.renderView == view;
    }

    /**
     * 将指定动画片段添加到本容器中播放。
     *
     * @param segment
     * @return 是否成功添加，若容器中已存在该动画片段或者该动画片段已经结束时返回false。
     */
    public synchronized boolean showSegment(AnimateSegment segment) {
        if (!segments.contains(segment)) {
            segment.prepare(true);
            if (isPreparing) {
                if (segment.hasNext()) {
                    segments.add(segment);
                } else {
                    segment.release();
                    return false;
                }
            } else {
                segments.add(segment);
            }
            Collections.sort(segments);
            if (renderView != null) {
                renderView.postInvalidate();
            }
            return true;
        }
        return false;
    }

    /**
     * 暂停本容器中的所有动画片段。
     *
     * @param pause
     */
    public void setPause(boolean pause) {
        if (pendingView != null) {
            pendingView.setFreeze(pause);
        }
        if (renderView != null) {
            renderView.setFreeze(pause);
        }
        for (AnimateSegment segment : segments) {
            segment.setPause(pause);
        }
    }

    synchronized void prepareNext() {
        if (segments.size() > 0) {
            temp.addAll(segments);
            isPreparing = true;
            for (AnimateSegment segment : temp) {
                if (!segment.hasNext()) {
                    segment.release();
                    segments.remove(segment);
                }
            }
            isPreparing = false;
            temp.clear();
        }
    }

    synchronized boolean draw(Canvas canvas) {
        if (segments.size() > 0) {
            for (AnimateSegment segment : segments) {
                segment.draw(canvas);
            }
            return true;
        }
        return false;
    }

    /**
     * 结束并释放本容器中的所有动画片段。
     */
    public synchronized void release() {
        pendingView = null;
        renderView = null;
        for (AnimateSegment segment : segments) {
            segment.release();
        }
        segments.clear();
    }

    @Override
    public final void run() {
        prepareNext();
    }
}
