package yanry.lib.android.view.animate.reactive.schedule;

import yanry.lib.android.view.animate.reactive.AnimateLayout;
import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.java.model.schedule.imple.ReusableDisplay;

/**
 * Created by yanry on 2020/5/12.
 */
public abstract class AnimateDisplay<D extends AnimateData> extends ReusableDisplay<D, AnimateLayout> {

    protected abstract void onBindAnimateSegment(AnimateLayout view, D data, AnimateSegment.ScheduleBinding binding);

    @Override
    protected final void setData(AnimateLayout view, D data) {
        AnimateSegment animateSegment = data.getAnimateSegment();
        if (animateSegment != null) {
            onBindAnimateSegment(view, data, animateSegment.bindShowData(data, view));
        }
    }

    @Override
    protected boolean isShowing(AnimateLayout view) {
        return view.getAnimateCount().getValue() > 0;
    }
}
