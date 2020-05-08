package yanry.lib.android.view.animate.reactive;

import yanry.lib.java.model.schedule.Display;
import yanry.lib.java.model.schedule.Scheduler;
import yanry.lib.java.model.schedule.ShowData;
import yanry.lib.java.model.watch.ValueHolder;
import yanry.lib.java.model.watch.ValueHolderImpl;
import yanry.lib.java.model.watch.ValueWatcher;

import static yanry.lib.java.model.schedule.ShowData.FLAG_EXPEL_WAITING_DATA;
import static yanry.lib.java.model.schedule.ShowData.STRATEGY_APPEND_TAIL;
import static yanry.lib.java.model.schedule.ShowData.STRATEGY_SHOW_IMMEDIATELY;

/**
 * Created by yanry on 2020/5/8.
 */
public class PackedAnimateSegment implements AnimateStateWatcher, ValueWatcher<Integer> {
    private AnimateSegment[] segments;
    private ValueHolderImpl<Integer> animateState;

    public PackedAnimateSegment(AnimateSegment... segments) {
        this.segments = segments;
        animateState = new ValueHolderImpl<>();
        int i = 0;
        for (AnimateSegment segment : segments) {
            segment.addAnimateStateWatcher(this);
            if (i == 0) {
                segment.addFlag(FLAG_EXPEL_WAITING_DATA);
                segment.setStrategy(STRATEGY_SHOW_IMMEDIATELY);
            } else {
                segment.setStrategy(STRATEGY_APPEND_TAIL);
            }
            if (i == segments.length - 1) {
                segment.getState().addWatcher(this);
            }
        }
    }

    public ValueHolder<Integer> getAnimateState() {
        return animateState;
    }

    public void show(Scheduler scheduler, Class<? extends Display<? extends AnimateSegment>> displayType) {
        for (AnimateSegment segment : segments) {
            scheduler.show(segment, displayType);
        }
    }

    @Override
    public void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState) {
        if (toState != AnimateSegment.ANIMATE_STATE_STOPPED) {
            animateState.setValue(toState);
        }
    }

    @Override
    public void onValueChange(Integer to, Integer from) {
        if (to == ShowData.STATE_DEQUEUE || to == ShowData.STATE_DISMISS) {
            animateState.setValue(AnimateSegment.ANIMATE_STATE_STOPPED);
        }
    }
}
