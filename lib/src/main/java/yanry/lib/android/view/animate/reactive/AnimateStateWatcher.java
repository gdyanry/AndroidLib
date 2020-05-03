package yanry.lib.android.view.animate.reactive;

/**
 * Created by yanry on 2020/5/1.
 */
public interface AnimateStateWatcher {
    /**
     * 动画状态回调，状态值为{@link AnimateSegment#ANIMATE_STATE_PLAYING}、{@link AnimateSegment#ANIMATE_STATE_PAUSED}、{@link AnimateSegment#ANIMATE_STATE_STOPPED}。
     *
     * @param animateSegment
     * @param toState
     * @param fromState
     */
    void onAnimateStateChange(AnimateSegment animateSegment, int toState, int fromState);
}
