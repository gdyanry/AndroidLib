package yanry.lib.android.view.animate.reactive.schedule;

import yanry.lib.android.view.animate.reactive.SegmentsHolder;
import yanry.lib.java.model.schedule.Display;

/**
 * Created by yanry on 2020/2/25.
 */
public abstract class AnimateDisplay<D extends AnimateData> extends Display<D> {
    private SegmentsHolder segmentsHolder;

    public AnimateDisplay(SegmentsHolder segmentsHolder) {
        this.segmentsHolder = segmentsHolder;
    }

    @Override
    protected void show(D data) {
        segmentsHolder.showSegment(data);
    }
}
