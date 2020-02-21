package yanry.lib.android.view.animate.reactive;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

/**
 * 动画片段。
 */
public interface AnimateSegment extends Comparable<AnimateSegment> {
    /**
     * 动画开始前的初始化。
     *
     * @param urgent true表示该动画片段已经放到{@link SegmentsHolder}中，马上就要开始绘制；false表示后面（可能）会进行绘制，时间上还不是那么紧急。
     */
    void prepare(boolean urgent);

    /**
     * 该动画是否需要绘制下一帧，可通过此方法控制动画的结束。
     *
     * @return 若返回true则执行{@link #draw(Canvas)}方法，否则结束动画
     */
    boolean hasNext();

    /**
     * 绘制动画的当前帧。
     *
     * @param canvas
     */
    void draw(Canvas canvas);

    /**
     * 动画结束后释放资源。
     */
    void release();

    /**
     * 该动画所在的z轴坐标，z值大的动画会覆盖在z值小的动画上面。
     *
     * @return
     */
    int getZOrder();

    /**
     * 控制动画的暂停与播放。
     *
     * @param pause
     */
    void setPause(boolean pause);

    @Override
    default int compareTo(@NonNull AnimateSegment o) {
        return getZOrder() - o.getZOrder();
    }
}
