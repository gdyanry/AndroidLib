/**
 * 
 */
package lib.android.util;

import java.io.IOException;

import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import lib.common.entity.CheckedOutOfMemoryException;
import lib.common.util.ConsoleUtil;

/**
 * @author yanry
 *
 *         2016年5月27日
 */
public class BitmapUtil {

	public static Matrix getRotateMatrix(String srcPath) throws IOException {
		if (srcPath != null) {
			int degrees = 0;
			ExifInterface exifInterface = new ExifInterface(srcPath);
			int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degrees = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degrees = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degrees = 270;
				break;
			}
			if (degrees != 0) {
				ConsoleUtil.debug(BitmapUtil.class, String.format("rotate %s degrees: %s", degrees, srcPath));
				Matrix matrix = new Matrix();
				matrix.postRotate(degrees);
				return matrix;
			}
		}
		return null;
	}

	public static boolean checkMemory(Options opts, double scale) throws CheckedOutOfMemoryException {
		return opts.outWidth * scale * opts.outHeight * scale * 4 < Runtime.getRuntime().maxMemory()
				- Runtime.getRuntime().totalMemory();
	}
}
