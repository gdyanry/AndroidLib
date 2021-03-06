/**
 * 
 */
package yanry.lib.android.model.bitmap.access;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.File;

import yanry.lib.android.model.bitmap.BitmapOption;
import yanry.lib.android.model.bitmap.BitmapThumb.Decoder;
import yanry.lib.android.model.bitmap.CacheKey;
import yanry.lib.java.model.resourceaccess.AccessHook;

/**
 * @author yanry
 *
 * 2015年11月14日
 */
public abstract class FileBitmapAccess extends BitmapAccess<File> {

	@Override
	protected Decoder getDecoder(final CacheKey<File> key, BitmapOption option, AccessHook<Bitmap> hook) {
		return new Decoder() {
			
			@Override
            public Bitmap decode(Options opts) {
				return BitmapFactory.decodeFile(key.getSrc().getAbsolutePath(), opts);
			}
		};
	}

	@Override
	protected String getPath(CacheKey<File> key) {
		return key.getSrc().getAbsolutePath();
	}
}
