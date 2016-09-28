/**
 * 
 */
package lib.android.model.bitmap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html.ImageGetter;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lib.common.model.resourceaccess.AccessHook;
import lib.common.model.resourceaccess.UrlFileAccess;

/**
 * @author yanry
 *
 *         2016年5月8日
 */
public class HtmlImageGetter implements ImageGetter {
	private UrlFileAccess access;
	private Map<String, Drawable> result;
	private Drawable defaultDrawable;
	private TextView tv;
	private Set<String> busyKeys;

	/**
	 * 
	 * @param tv
	 * @param defaultDrawable
	 * @param access
	 *            a {@link UrlFileAccess} object that supports asynchronous
	 *            generation
	 */
	public HtmlImageGetter(TextView tv, Drawable defaultDrawable, UrlFileAccess access) {
		this.access = access;
		this.tv = tv;
		this.defaultDrawable = defaultDrawable;
		busyKeys = new HashSet<String>();
		result = new HashMap<String, Drawable>();
	}

	@Override
	public Drawable getDrawable(final String source) {
		access.get(source, null, new AccessHook<File>() {

			@Override
			public boolean onStartGenerate(File cached) {
				if (result.get(source) != null) {
					return false;
				} else if (cached.isFile()) {
					Drawable drawable = Drawable.createFromPath(cached.getAbsolutePath());
					drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
					result.put(source, drawable);
					return false;
				} else {
					// avoid repeat generation
					return busyKeys.add(source);
				}
			}

			@SuppressLint("NewApi")
			@Override
			public boolean onStartCache(File generated) {
				busyKeys.remove(source);
				if (generated.isFile()) {
					if (!(tv.getContext() instanceof Activity
							&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
							&& ((Activity) tv.getContext()).isDestroyed())) {
						tv.postInvalidate();
					}
				}
				return false;
			}

			@Override
			public void onGenerateException(Exception e) {
				e.printStackTrace();
				busyKeys.remove(source);
			}
		});
		Drawable drawable = result.get(source);
		return drawable == null ? defaultDrawable : drawable;
	}
}
