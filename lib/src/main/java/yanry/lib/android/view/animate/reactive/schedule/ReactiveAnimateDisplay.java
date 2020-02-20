package yanry.lib.android.view.animate.reactive.schedule;

import yanry.lib.android.view.animate.reactive.ReactiveAnimateView;
import yanry.lib.android.view.animate.reactive.SegmentsHolder;
import yanry.lib.java.model.schedule.imple.ReusableDisplay;

/**
 * Created by yanry on 2020/2/20.
 */
public abstract class ReactiveAnimateDisplay extends ReusableDisplay<ReactiveAnimateData, SegmentsHolder> {

    protected abstract ReactiveAnimateView getAnimateView();

    @Override
    protected SegmentsHolder createView(ReactiveAnimateData data) {
        return new SegmentsHolder();
    }

    @Override
    protected void setData(SegmentsHolder view, ReactiveAnimateData data) {
        view.showSegment(data);
    }

    @Override
    protected void showView(SegmentsHolder view) {
        view.bindRenderView(getAnimateView());
    }

    @Override
    protected void dismiss(SegmentsHolder view) {
        view.release();
    }

    @Override
    protected boolean isShowing(SegmentsHolder view) {
        return view.isBound();
    }
}
