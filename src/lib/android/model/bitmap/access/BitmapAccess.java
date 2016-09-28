/**
 * 
 */
package lib.android.model.bitmap.access;

import android.graphics.Bitmap;
import android.util.LruCache;

import lib.android.model.bitmap.BitmapOption;
import lib.android.model.bitmap.BitmapThumb;
import lib.android.model.bitmap.BitmapThumb.Decoder;
import lib.android.model.bitmap.CacheKey;
import lib.common.model.resourceaccess.AccessHook;
import lib.common.model.resourceaccess.CacheResourceAccess;

/**
 * @author yanry
 *
 *         2015年11月14日
 */
public abstract class BitmapAccess<S> extends CacheResourceAccess<CacheKey<S>, Bitmap, BitmapOption, AccessHook<Bitmap>> {

	@Override
	protected Bitmap getCacheValue(CacheKey<S> key, BitmapOption option) {
		return getCache().get(key);
	}

	@Override
	protected Bitmap generate(CacheKey<S> key, Bitmap cachedValue, BitmapOption option, AccessHook<Bitmap> hook)
			throws Exception {
		BitmapThumb bt = new BitmapThumb(getDecoder(key, option, hook));
		String srcPath = getPath(key);
		if (srcPath != null) {
			bt.autoRotate(srcPath);
		}
		return option.diyThumb(bt) ? bt.createThumb() : null;
	}

	@Override
	protected void cache(CacheKey<S> key, BitmapOption option, Bitmap generated) {
		getCache().put(key, generated);
	}

	protected abstract Decoder getDecoder(CacheKey<S> key, BitmapOption option, AccessHook<Bitmap> hook);

	/**
	 * Get the file path used to read picture degree and perform auto rotation.
	 * 
	 * @param key
	 *            pass null to disable auto rotation.
	 * @return
	 */
	protected abstract String getPath(CacheKey<S> key);

	protected abstract LruCache<Object, Bitmap> getCache();
}
