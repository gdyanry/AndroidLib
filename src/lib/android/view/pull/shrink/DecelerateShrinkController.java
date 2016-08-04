/**
 * 
 */
package lib.android.view.pull.shrink;

/**
 * 时间与距离为反比例关系，取第四象限的平移曲线
 * 
 * @author yanry
 *
 *         2016年3月21日
 */
public class DecelerateShrinkController extends ShrinkController {
	private int shrinkTimeLimit;
	private int shrinkFactor;

	public DecelerateShrinkController() {
		this(1200);
	}
	
	/**
	 * 
	 * @param shrinkTimeLimit
	 *            in millisecond.
	 */
	public DecelerateShrinkController(int shrinkTimeLimit) {
		this(shrinkTimeLimit, 50000);
	}

	/**
	 * 
	 * @param shrinkTimeLimit
	 *            in millisecond.
	 * @param shrinkFactor
	 *            the bigger the shrink factor is, the more the shrink acts
	 *            linearly. As for screen pixel calculation, evaluate a proper
	 *            value at the scale of ten thousand.
	 */
	public DecelerateShrinkController(int shrinkTimeLimit, int shrinkFactor) {
		this.shrinkTimeLimit = shrinkTimeLimit;
		this.shrinkFactor = shrinkFactor;
	}

	@Override
	protected int getLeftTimeByOffset(int offset) {
		return shrinkTimeLimit * shrinkTimeLimit * offset / (shrinkTimeLimit * offset + shrinkFactor);
	}

	@Override
	protected int getOffsetByLeftTime(int leftTime) {
		return shrinkFactor * leftTime / shrinkTimeLimit / (shrinkTimeLimit - leftTime);
	}
}
