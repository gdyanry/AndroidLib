/**
 * 
 */
package lib.android.model.bitmap.access;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;

import lib.android.model.bitmap.BitmapOption;
import lib.android.model.bitmap.BitmapThumb.Decoder;
import lib.android.model.bitmap.CacheKey;
import lib.common.model.resourceaccess.AccessHook;

/**
 * <h5>Accepts the following URI schemes:</h5>
 * <ul>
 * <li>content</li>
 * <li>android.resource</li>
 * <li>file</li>
 * </ul>
 * 
 * @author yanry
 *
 *         2015年11月14日
 */
public abstract class UriBitmapAccess extends BitmapAccess<Uri> {
	
	protected abstract ContentResolver getContentResolver();

	@Override
	protected Decoder getDecoder(final CacheKey<Uri> key, BitmapOption option, AccessHook<Bitmap> hook) {
		return new Decoder() {

			@Override
			public Bitmap decode(Options opts) throws Exception {
				return BitmapFactory.decodeStream(getContentResolver().openInputStream(key.getSrc()), null, opts);
			}
		};
	}

	@Override
	protected String getPath(CacheKey<Uri> key) {
		if (key.toString().startsWith("file://")) {
			return key.toString().replaceFirst("file://", "");
		}
		return null;
	}
}
