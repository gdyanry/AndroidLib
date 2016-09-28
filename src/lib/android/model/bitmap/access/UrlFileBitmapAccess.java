/**
 * 
 */
package lib.android.model.bitmap.access;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import java.io.File;

/**
 * @author yanry
 *
 * 2015年11月14日
 */
public abstract class UrlFileBitmapAccess extends UrlBitmapAccess<File> {

	@Override
	protected Bitmap getBitmap(File medium, Options opts) {
		boolean exists = medium.exists();
		if (exists) {
			Bitmap bmp = BitmapFactory.decodeFile(medium.getAbsolutePath(), opts);
			return bmp;
		}
		return null;
	}
}
