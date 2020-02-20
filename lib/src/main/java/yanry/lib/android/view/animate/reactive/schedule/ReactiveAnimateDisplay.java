package yanry.lib.android.view.animate.reactive.schedule;

import yanry.lib.android.view.animate.reactive.ReactiveAnimateView;
import yanry.lib.android.view.animate.reactive.SegmentsHolder;
import yanry.lib.java.model.schedule.imple.ReusableDisplay;

/**
 * Created by yanry on 2020/2/20.
 */
public abstract class ReactiveAnimateDisplay<D extends ReactiveAnimateData> extends ReusableDisplay<D, SegmentsHolder> {

    protected abstract ReactiveAnimateView getAnimateView();

    @Override
    protected SegmentsHolder createView(D data) {
        return new SegmentsHolder();
    }

    @Override
    protected void setData(SegmentsHolder view, D data) {
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
