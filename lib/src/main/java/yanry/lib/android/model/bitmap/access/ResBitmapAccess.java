/**
 * 
 */
package yanry.lib.android.model.bitmap.access;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import yanry.lib.android.model.bitmap.BitmapOption;
import yanry.lib.android.model.bitmap.BitmapThumb.Decoder;
import yanry.lib.android.model.bitmap.CacheKey;
import yanry.lib.java.model.resourceaccess.AccessHook;

/**
 * @author yanry
 *
 * 2016年7月6日
 */
public abstract class ResBitmapAccess extends BitmapAccess<Integer> {

	protected abstract Resources getResources();
	
	@Override
	protected Decoder getDecoder(final CacheKey<Integer> key, BitmapOption option, AccessHook<Bitmap> hook) {
		return new Decoder() {
			
			@Override
			public Bitmap decode(Options opts) throws Exception {
				return BitmapFactory.decodeResource(getResources(), key.getSrc(), opts);
			}
		};
	}

	@Override
	protected String getPath(CacheKey<Integer> key) {
		return null;
	}

}
