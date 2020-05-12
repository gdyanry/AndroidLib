package yanry.lib.android.view.animate.reactive.schedule;

import yanry.lib.android.view.animate.reactive.AnimateSegment;
import yanry.lib.android.view.animate.reactive.PackedAnimateSegment;
import yanry.lib.java.model.schedule.ShowData;

/**
 * Created by yanry on 2020/5/12.
 */
public class AnimateData extends ShowData {
    private AnimateSegment animateSegment;

    public AnimateData(AnimateSegment... animateSegments) {
        if (animateSegments == null || animateSegments.length == 0) {
            addFlag(FLAG_DISMISS_ON_SHOW);
        } else if (animateSegments.length == 1) {
            animateSegment = animateSegments[0];
        } else {
            animateSegment = new PackedAnimateSegment().appendSegment(animateSegments);
        }
    }

    public AnimateSegment getAnimateSegment() {
        return animateSegment;
    }

    @Override
    public String toString() {
        if (animateSegment != null) {
            return animateSegment.toString();
        }
        return super.toString();
    }
}
