package yanry.lib.android.entity;

import android.animation.TypeEvaluator;

import yanry.lib.java.util.MathUtil;

/**
 * Created by rongyu.yan on 3/21/2017.
 */

public class BezierEvaluator implements TypeEvaluator<Float> {
    private float[] medianValues;

    public BezierEvaluator(float... medianValues) {
        this.medianValues = medianValues;
    }

    @Override
    public Float evaluate(float fraction, Float startValue, Float endValue) {
        return MathUtil.bezierEvaluate(fraction, startValue, endValue, medianValues);
    }
}
