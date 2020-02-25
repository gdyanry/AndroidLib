package yanry.lib.android.view.animate.reactive.schedule;

import android.graphics.Canvas;

import java.util.Iterator;
import java.util.LinkedList;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.schedule.OnDataStateChangeListener;
import yanry.lib.java.model.schedule.ShowData;

/**
 * 为了保证帧动画启动流畅，对本实例调用{@link yanry.lib.java.model.schedule.Scheduler#show(ShowData, Class)}的时候应先调用prepare(false)。
 * <p>
 * Created by yanry on 2020/2/20.
 */
public class AnimateData extends ShowData implements AnimateSegment, OnDataStateChangeListener {
    private LinkedList<AnimateSegment> animateSegments;
    private int zOrder;

    public AnimateData() {
        animateSegments = new LinkedList<>();
        addOnStateChangeListener(this);
    }

    public AnimateData appendSegment(AnimateSegment segment) {
        if (segment != null) {
            animateSegments.add(segment);
        }
        return this;
    }

    public AnimateData insertSegment(AnimateSegment segment) {
        if (segment != null) {
            animateSegments.addFirst(segment);
        }
        return this;
    }

    public AnimateData setZOrder(int zOrder) {
        this.zOrder = zOrder;
        return this;
    }

    protected LinkedList<AnimateSegment> getSegments() {
        return animateSegments;
    }

    @Override
    public void prepare(boolean urgent) {
        boolean setUrgent = false;
        for (AnimateSegment animateSegment : animateSegments) {
            if (urgent) {
                if (!setUrgent) {
                    animateSegment.prepare(true);
                    setUrgent = true;
                    // 先不返回，还得准备替补
                } else {
                    // 准备好一个替补后返回
                    animateSegment.prepare(false);
                    return;
                }
            } else {
                // 非紧急情况下只要准备一个替补就可以了
                animateSegment.prepare(false);
                return;
            }
        }
    }

    @Override
    public boolean hasNext() {
        int state = getState();
        if (state == 0 || state == STATE_ENQUEUE || state == STATE_SHOWING) {
            Iterator<AnimateSegment> iterator = animateSegments.iterator();
            boolean needSetUrgent = false;
            while (iterator.hasNext()) {
                AnimateSegment next = iterator.next();
                if (needSetUrgent) {
                    // 替补上场
                    next.prepare(true);
                }
                if (next.hasNext()) {
                    if (needSetUrgent && iterator.hasNext()) {
                        // 准备下一个替补
                        iterator.next().prepare(false);
                    }
                    return true;
                } else {
                    next.release();
                    iterator.remove();
                    needSetUrgent = true;
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
