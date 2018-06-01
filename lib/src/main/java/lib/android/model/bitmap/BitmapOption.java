/**
 * 
 */
package lib.android.model.bitmap;

import lib.common.model.resourceaccess.UrlOption;

/**
 * Implementation class should override {@link #toString()} to indicate bitmap
 * loading parameter.
 * 
 * @author yanry
 *
 *         2015年11月14日
 */
public interface BitmapOption extends UrlOption {
	/**
	 * 
	 * @param bt
	 * @return return true to create thumb.
	 * @throws Exception
	 */
	boolean diyThumb(BitmapThumb bt) throws Exception;
}
