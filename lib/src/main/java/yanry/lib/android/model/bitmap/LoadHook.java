/**
 * 
 */
package yanry.lib.android.model.bitmap;

import android.graphics.Bitmap;

/**
 * @author yanry
 *
 * 2016年7月6日
 */
public interface LoadHook {

	/**
	 * @return whether do loading.
	 */
	boolean onStartLoading();
	
	boolean isAbort();

	void onShow(Bitmap bmp);

	void onError();
}
