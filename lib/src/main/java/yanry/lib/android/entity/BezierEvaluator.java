package yanry.lib.android.entity;

import android.animation.TypeEvaluator;

import yanry.lib.java.util.MathUtil;

/**
 * Created by rongyu.yan on 3/21/2017.
 */

public class BezierEvaluator implements TypeEvaluator<Integer> {
    private int[] medianValues;

    public BezierEvaluator(int... medianValues) {
        this.medianValues = medianValues;
    }

    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
        return MathUtil.bezierEvaluate(fraction, startValue, endValue, medianValues);
    }
}
