/**
 * 
 */
package yanry.lib.android.model.bitmap.access;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;

import java.io.File;

import yanry.lib.android.model.bitmap.BitmapOption;
import yanry.lib.android.model.bitmap.BitmapThumb.Decoder;
import yanry.lib.android.model.bitmap.CacheKey;
import yanry.lib.java.model.resourceaccess.AccessHook;
import yanry.lib.java.model.resourceaccess.CacheResourceAccess;
import yanry.lib.java.model.resourceaccess.UrlOption;

/**
 * @param <M>
 *            type of intermediate object that can be decoded to bitmap and can
 *            be saved locally, typically {@link File} or byte[] (stored in
 *            database);
 * 
 * @author yanry
 *
 *         2015年11月14日
 */
public abstract class UrlBitmapAccess<M> extends BitmapAccess<String> {

	public UrlBitmapAccess() {
//		enableCacheTask(taskCacheLimit);
	}

	/**
	 * 
	 * @return a {@link CacheResourceAccess} responsible for downloading
	 *            resource from a given URL. It must work in single-thread mode!
	 */
	protected abstract CacheResourceAccess<String, M, UrlOption, AccessHook<M>> getLevel2Access();
	
	protected abstract Bitmap getBitmap(M medium, Options opts);
	
	protected abstract boolean isConnected();

	@Override
	protected Decoder getDecoder(CacheKey<String> key, BitmapOption option, AccessHook<Bitmap> hook) {
		return new UrlDecoder(key, option, hook);
	}

	private class UrlDecoder implements Decoder, AccessHook<M> {
		private Bitmap bmp;
		private Options opts;
		private CacheKey<String> key;
		private BitmapOption option;
		private AccessHook<Bitmap> hook;

		UrlDecoder(CacheKey<String> key, BitmapOption option, AccessHook<Bitmap> hook) {
			this.key = key;
			this.option = option;
			this.hook = hook;
		}

		@Override
		public Bitmap decode(Options opts) {
			this.opts = opts;
			synchronized (key) {
				getLevel2Access().get(key.getSrc(), option, this);
			}
			return bmp;
		}

		@Override
		public boolean onStartGenerate(M cachedValue) {
			bmp = getBitmap(cachedValue, opts);
			if (opts.inJustDecodeBounds || bmp != null) {
				return false;
			}
			return isConnected();
		}

		@Override
		public void onGenerateException(Exception e) {
			hook.onGenerateException(e);
		}

		@Override
		public boolean onStartCache(M generatedValue) {
			bmp = getBitmap(generatedValue, opts);
			return bmp != null;
		}
	}
}
