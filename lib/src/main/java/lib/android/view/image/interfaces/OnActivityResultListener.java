/**
 * 
 */
package lib.android.view.image.interfaces;

import android.content.Intent;

/**
 * @author yanry
 *
 *         2016年1月21日
 */
public interface OnActivityResultListener {

	/**
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @return whether the result is consumed.
	 */
	boolean onActivityResult(int requestCode, int resultCode, Intent data);
}
