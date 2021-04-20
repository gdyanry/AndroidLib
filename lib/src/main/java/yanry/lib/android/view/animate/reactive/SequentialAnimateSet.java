package yanry.lib.android.view.animate.reactive;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.view.View;

import java.util.LinkedList;

import yanry.lib.java.model.log.Logger;

/**
 * 由多个子动画顺序播放组成的动画。
 * <p>
 * Created by yanry on 2020/5/8.
 */
public class SequentialAnimateSet extends AnimateSegment implements AnimateStateWatcher {
    private LinkedList<AnimateSegment> segments = new LinkedList<>();

    public SequentialAnimateSet insertSegment(AnimateSegment animateSegment) {
        segments.addFirst(animateSegment);
        animateSegment.getAnimateStateRegistry().register(this);
        return this;
    }

    public SequentialAnimateSet appendSegment(AnimateSegment... animateSegments) {
        for (AnimateSegment animateSegment : animateSegments) {
            segments.addLast(animateSegment);
            animateSegment.getAnimateStateRegistry().register(this);
        }
        return this;
    }

    @Override
    public Logger getLogger() {
        Logger logger = super.getLogger();
        if (logger != Logger.getDefault()) {
            return logger;
        }
        AnimateSegment first = segments.peekFirst();
        if (first != null) {
            return first.getLogger();
        }
        return logger;
    }

    @Override
    public int getZOrder() {
        AnimateSegment first = segments.peekFirst();
        if (first != null) {
            return first.getZOrder();
        }
        return super.getZOrder();
    }

    @Override
    protected void prepare(View animateView) {
        AnimateSegment animateSegment = segments.peekFirst();
        if (animateSegment != null) {
            animateSegment.prepare(animateView);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        AnimateSegment animateSegment = segments.peekFirst();
        if (animateSegment != null) {
            animateSegment.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected boolean isSupportExitAnimate() {
        AnimateSegment animateSegment = segments.peekFirst();
        if (animateSegment != null) {
            return animateSegment.isSupportExitAnimate();
        }
        return super.isSupportExitAnimate();
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
                    first.prepare(getAnimateView());
                }
            } else {
                return duration;
            }
        }
        return -1;
    }

    @Override
    public void setPause(boolean pause) {
        AnimateSegment first = segments.peekFirst();
        if (first != null) {
            first.setPause(pause);
        }
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
