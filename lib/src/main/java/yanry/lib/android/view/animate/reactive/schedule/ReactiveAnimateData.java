package yanry.lib.android.view.animate.reactive.schedule;

import android.graphics.Canvas;

import java.util.Iterator;
import java.util.LinkedList;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.schedule.OnDataStateChangeListener;
import yanry.lib.java.model.schedule.ShowData;

/**
 * Created by yanry on 2020/2/20.
 */
public class ReactiveAnimateData extends ShowData implements AnimateSegment, OnDataStateChangeListener {
    private LinkedList<AnimateSegment> animateSegments;
    private int zOrder;

    public ReactiveAnimateData() {
        animateSegments = new LinkedList<>();
        addOnStateChangeListener(this);
    }

    public ReactiveAnimateData appendSegment(AnimateSegment segment) {
        if (segment != null) {
            segment.init();
            animateSegments.add(segment);
        }
        return this;
    }

    public ReactiveAnimateData insertSegment(AnimateSegment segment) {
        if (segment != null) {
            segment.init();
            animateSegments.addFirst(segment);
        }
        return this;
    }

    public ReactiveAnimateData setZOrder(int zOrder) {
        this.zOrder = zOrder;
        return this;
    }

    public int getSegmentsSize() {
        return animateSegments.size();
    }

    @Override
    public void init() {
    }

    @Override
    public boolean hasNext() {
        int state = getState();
        if (state == 0 || state == STATE_ENQUEUE || state == STATE_SHOWING) {
            Iterator<AnimateSegment> iterator = animateSegments.iterator();
            while (iterator.hasNext()) {
                AnimateSegment next = iterator.next();
                if (next.hasNext()) {
                    return true;
                } else {
                    next.release();
                    iterator.remove();
                }
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        AnimateSegment first = animateSegments.peekFirst();
        if (first != null) {
            first.draw(canvas);
        }
    }

    @Override
    public void release() {
        if (getState() != 0) {
            dismiss(0);
        } else {
            releaseSegments();
        }
    }

    @Override
    public int getZOrder() {
        return zOrder;
    }

    private void releaseSegments() {
        for (AnimateSegment animateSegment : animateSegments) {
            animateSegment.release();
        }
    }

    @Override
    public void setPause(boolean pause) {
        AnimateSegment first = animateSegments.peekFirst();
        if (first != null) {
            first.setPause(pause);
        }
    }

    @Override
    public void onDataStateChange(int toState) {
        if (toState == STATE_DEQUEUE || toState == STATE_DISMISS) {
            releaseSegments();
        }
    }
}
