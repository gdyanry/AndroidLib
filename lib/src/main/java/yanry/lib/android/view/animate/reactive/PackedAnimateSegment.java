package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import java.util.LinkedList;

import yanry.lib.java.model.log.Logger;

/**
 * Created by yanry on 2020/5/8.
 */
public class PackedAnimateSegment extends AnimateSegment implements AnimateStateWatcher {
    private LinkedList<AnimateSegment> segments;

    public PackedAnimateSegment() {
        segments = new LinkedList<>();
    }

    public PackedAnimateSegment insertSegment(AnimateSegment animateSegment) {
        segments.addFirst(animateSegment);
        animateSegment.addAnimateStateWatcher(this);
        return this;
    }

    public PackedAnimateSegment appendSegment(AnimateSegment... animateSegments) {
        for (AnimateSegment animateSegment : animateSegments) {
            segments.addLast(animateSegment);
            animateSegment.addAnimateStateWatcher(this);
        }
        return this;
    }

    @Override
    protected Logger getLogger() {
        AnimateSegment first = segments.peekFirst();
        if (first != null) {
            return first.getLogger();
        }
        return Logger.getDefault();
    }

    @Override
    protected int getZOrder() {
        AnimateSegment first = segments.peekFirst();
        if (first != null) {
            return first.getZOrder();
        }
        return 0;
    }

    @Override
    protected void prepare() {
        AnimateSegment animateSegment = segments.peekFirst();
        if (animateSegment != null) {
            animateSegment.prepare();
        }
    }

    @Override
    protected long draw(Canvas canvas) {
        AnimateSegment first = segments.peekFirst();
        while (first != null) {
            long duration = first.draw(canvas);
            if (duration < 0) {
                segments.pollFirst();
                first = segments.peekFirst();
                if (first != null) {
                    first.prepare();
                }
            } else {
                return duration;
            }
        }
        return -1;
    }

    @Override
    public boolean pauseAnimate() {
        AnimateSegment first = segments.peekFirst();
        return first != null && first.pauseAnimate();
    }

    @Override
    public boolean resumeAnimate() {
        AnimateSegment first = segments.peekFirst();
        return first != null && first.resumeAnimate();
    }

    @Override
    public boolean stopAnimate() {
        AnimateSegment first;
        while ((first = segments.pollFirst()) != null) {
            first.stopAnimate();
        }
        return true;
    }

    @Override
    public void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState) {
        switch (toState) {
            case AnimateSegment.ANIMATE_STATE_PAUSED:
            case AnimateSegment.ANIMATE_STATE_PLAYING:
                setAnimateState(toState);
                break;
            case AnimateSegment.ANIMATE_STATE_STOPPED:
                if (animateSegment == segments.peekLast()) {
                    setAnimateState(toState);
                }
                break;
        }
    }

    @Override
    public String toString() {
        return segments.toString();
    }
}
