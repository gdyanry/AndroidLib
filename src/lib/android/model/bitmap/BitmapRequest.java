/**
* 
*/
package lib.android.model.bitmap;

import java.io.File;

import com.lib.android.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import lib.android.entity.MainHandler;
import lib.android.model.bitmap.access.BitmapAccess;
import lib.android.util.CommonUtils;
import lib.android.util.ViewUtil;
import lib.common.model.Singletons;
import lib.common.model.resourceaccess.AccessHook;
import lib.common.util.ConsoleUtil;

/**
 * @author yanry
 *
 *         2016年5月8日
 */
public class BitmapRequest implements AccessHook<Bitmap>, BitmapOption {
	private static final int ACTION_START = 1;
	private static final int ACTION_FINISH = 2;
	private static final int ACTION_ERROR = 3;
	private Activity activity;
	private ThumbDimension width;
	private ThumbDimension height;
	private Object src;
	private LoadHook load;
	private ImageView iv;
	private int loadingRes;
	private int errorRes;
	private BitmapLoader loader;
	private DownloadHook download;
	private boolean useNearScale;

	BitmapRequest(BitmapLoader loader) {
		this.loader = loader;
	}

	BitmapRequest from(Object src) {
		if (src instanceof File || src instanceof Uri || src instanceof Integer) {
			this.src = src;
		} else {
			String strSrc = src.toString();
			if (strSrc.startsWith("http://") || strSrc.startsWith("https://")) {
				this.src = strSrc;
			} else if (strSrc.startsWith("file://") || strSrc.startsWith("content://")
					|| strSrc.startsWith("android.resource://")) {
				this.src = Uri.parse(strSrc);
			} else {
				this.src = new File(strSrc);
			}
		}
		return this;
	}

	public void commit() {
		if (src == null) {
			reset();
			return;
		}
		if (iv != null) {
			LayoutParams params = iv.getLayoutParams();
			if (width == null && params.width > 0) {
				width = new ThumbDimension(params.width);
			}
			if (height == null && params.height > 0) {
				height = new ThumbDimension(params.height);
			}
		}
		if (src instanceof String) {
			BitmapAccess<String> urlAccess = loader.urlFileAccess == null ? loader.urlBlobAccess : loader.urlFileAccess;
			if (urlAccess != null) {
				urlAccess.get(new CacheKey<String>((String) src, width, height, useNearScale), this, this);
			}
		} else if (src instanceof File) {
			loader.fileAccess.get(new CacheKey<File>((File) src, width, height, useNearScale), this, this);
		} else if (src instanceof Uri && loader.uriAccess != null) {
			loader.uriAccess.get(new CacheKey<Uri>((Uri) src, width, height, useNearScale), this, this);
		} else if (src instanceof Integer && loader.resAccess != null) {
			loader.resAccess.get(new CacheKey<Integer>((Integer) src, width, height, useNearScale), this, this);
		}
	}

	public BitmapRequest into(ImageView iv) {
		iv.setTag(R.id.tag_bitmap_source, src);
		Activity activity = ViewUtil.getActivityByView(iv);
		if (activity != null) {
			bind(activity);
		}
		this.iv = iv;
		return this;
	}

	public BitmapRequest placeholder(int res) {
		this.loadingRes = res;
		if (errorRes == 0) {
			this.errorRes = res;
		}
		return this;
	}

	public BitmapRequest error(int res) {
		this.errorRes = res;
		return this;
	}

	public BitmapRequest load(LoadHook hook) {
		this.load = hook;
		return this;
	}

	public BitmapRequest download(DownloadHook hook) {
		this.download = hook;
		return this;
	}

	/**
	 * Bind this task to an activity, so that when activity is destroyed, this
	 * task will be cancelled if it's not started. This only works on system not
	 * lower than {@link Build.VERSION_CODES#JELLY_BEAN_MR1}.
	 * 
	 * @param activity
	 * @return
	 */
	public BitmapRequest bind(Activity activity) {
		this.activity = activity;
		return this;
	}

	public BitmapRequest width(ThumbDimension width) {
		this.width = width;
		return this;
	}

	public BitmapRequest height(ThumbDimension height) {
		this.height = height;
		return this;
	}

	public BitmapRequest fitWidth(int widthPx) {
		this.width = new ThumbDimension(widthPx);
		return this;
	}

	public BitmapRequest fitHeight(int heightPx) {
		this.height = new ThumbDimension(heightPx);
		return this;
	}

	/**
	 * 
	 * @param widthDp
	 *            a value between 0 (exclusive, which means ignore) and 1
	 *            (inclusive) indicates the scale of desired width to window
	 *            width.
	 * @param heightDp
	 *            a value between 0 (exclusive, which means ignore) and 1
	 *            (inclusive) indicates the scale of desired height to window
	 *            height.
	 * @return
	 */
	public BitmapRequest fitSize(float widthDp, float heightDp) {
		width = getDimension(widthDp, true);
		height = getDimension(heightDp, false);
		return this;
	}

	private ThumbDimension getDimension(float dpDimen, boolean isWidth) {
		if (dpDimen > 1) {
			return new ThumbDimension(CommonUtils.dip2px(dpDimen));
		} else {
			if (dpDimen > 0) {
				return new ThumbDimension((int) ((CommonUtils.getWindowDimension(isWidth)) * dpDimen));
			}
		}
		return null;
	}

	/**
	 * @see BitmapThumb#useNearScale()
	 * @return
	 */
	public BitmapRequest useNearScale() {
		this.useNearScale = true;
		return this;
	}

	@Override
	public boolean diyThumb(BitmapThumb bt) throws Exception {
		if (isAlive("quit on creating thumb")) {
			bt.width(width);
			bt.height(height);
			if (useNearScale) {
				bt.useNearScale();
			}
			if (loader.isDebug) {
				bt.debug();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onStartGenerate(Bitmap cachedValue) {
		if (isAlive("quit on downloading")) {
			if (cachedValue != null) {
				dispatch(cachedValue, ACTION_FINISH);
				return false;
			}
			if (src.toString().length() > 0) {
				dispatch(null, ACTION_START);
				if (load == null || load.onStartLoading()) {
					return true;
				} else {
					return false;
				}
			} else {
				// invalid source
				dispatch(null, ACTION_ERROR);
			}
		} else {
//			reset();
		}
		return false;
	}

	@Override
	public boolean onStartCache(Bitmap generatedValue) {
		if (generatedValue != null) {
			if (isAlive("quit on displaying")) {
				dispatch(generatedValue, ACTION_FINISH);
			} else {
//				reset();
			}
		}
		return true;
	}

	@Override
	public void onGenerateException(Exception e) {
		if (download != null) {
			download.action = DownloadHook.ACTION_ERROR;
			CommonUtils.runOnUiThread(download);
		}
		if (isAlive(e.getMessage())) {
			dispatch(null, ACTION_ERROR);
		} else {
//			reset();
		}
	}

	private Bitmap getBmpByResId(int res) {
		Bitmap bm = loader.getCache().get(res);
		if (bm == null && iv != null) {
			bm = BitmapFactory.decodeResource(iv.getResources(), res);
			if (bm != null) {
				loader.getCache().put(res, bm);
			}
		}
		return bm;
	}

	@SuppressLint("NewApi")
	private String isAlive() {
		if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
			return "activity is destroyed.";
		}
		if (iv != null && src != null && !src.equals(iv.getTag(R.id.tag_bitmap_source))) {
			return "source of ImageView has been changed.";
		}
		if (load != null && load.isAbort()) {
			return "request is aborted by user.";
		}
		return null;
	}

	private boolean isAlive(String errPrefix) {
		String msg = isAlive();
		if (msg == null) {
			return true;
		}
		if (loader.isDebug) {
			ConsoleUtil.error(getClass(), String.format("%s: %s (%s)", errPrefix, msg, src));
		}
		return false;
	}

	private void dispatch(final Bitmap bm, final int action) {
		if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
			show(bm, action);
		} else {
			Singletons.get(MainHandler.class).post(new Runnable() {
				public void run() {
					show(bm, action);
				}
			});
		}
	}

	private void show(Bitmap bmp, int action) {
		if (isAlive() == null) {
			switch (action) {
			case ACTION_ERROR:
				bmp = getBmpByResId(errorRes);
				if (load != null) {
					load.onError();
				}
				break;
			case ACTION_FINISH:
				if (load != null) {
					load.onShow(bmp);
				}
				break;
			case ACTION_START:
				bmp = getBmpByResId(loadingRes);
				break;
			}
			if (iv != null && bmp != null) {
				iv.setImageBitmap(bmp);
			}
		}
		if (action != ACTION_START) {
			reset();
		}
	}

	private void reset() {
		activity = null;
		width = null;
		height = null;
		src = null;
		load = null;
		iv = null;
		loadingRes = 0;
		errorRes = 0;
		download = null;
		useNearScale = false;
		loader.requestPool.restore(this);
	}

	@Override
	public boolean isStop() {
		// downloading is not supposed to be stopped
		return false;
	}

	@Override
	public int getUpdateInterval() {
		return download == null ? Integer.MAX_VALUE : download.getUpdateInterval();
	}

	@Override
	public void onUpdate(long transferedBytes) {
		if (download != null) {
			download.action = DownloadHook.ACTION_UPDATE;
			download.currentPos = download.startPos + transferedBytes;
			CommonUtils.runOnUiThread(download);
		}
	}

	@Override
	public void onFinish(boolean isStopped) {
		if (download != null) {
			download.action = DownloadHook.ACTION_FINISH;
			CommonUtils.runOnUiThread(download);
		}
	}

	@Override
	public int getBufferSize() {
		return 4096;
	}

	@Override
	public boolean onReadyToDownload(long startPos, long totalLen) {
		if (download != null) {
			download.action = DownloadHook.ACTION_START;
			download.startPos = startPos;
			download.totalLen = totalLen;
			CommonUtils.runOnUiThread(download);
		}
		return true;
	}
}
