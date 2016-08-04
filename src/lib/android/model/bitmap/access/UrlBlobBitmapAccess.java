/**
 * 
 */
package lib.android.model.bitmap.access;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import lib.android.model.bitmap.CacheKey;

/**
 * @author yanry
 *
 *         2016年5月8日
 */
public abstract class UrlBlobBitmapAccess extends UrlBitmapAccess<byte[]> {

	@Override
	protected Bitmap getBitmap(byte[] medium, Options opts) {
		return BitmapFactory.decodeByteArray(medium, 0, medium.length, opts);
	}

	@Override
	protected String getPath(CacheKey<String> key) {
		return null;
	}

}
