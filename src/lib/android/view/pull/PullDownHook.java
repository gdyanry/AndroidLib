/**
 * 
 */
package lib.android.view.pull;

/**
 * @author yanry
 *
 *         2016年3月31日
 */
public interface PullDownHook {
	/**
	 * 
	 * @return return true to enable pulling, otherwise false.
	 */
	boolean onPreparedToPull();

	void onPullingDown(int distance);

	void onRelease(int distance);
}
