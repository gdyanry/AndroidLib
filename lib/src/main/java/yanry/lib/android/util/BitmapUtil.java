/**
 *
 */
package yanry.lib.android.util;

import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.IOException;

import yanry.lib.java.model.log.Logger;

/**
 * @author yanry
 * <p>
 * 2016年5月27日
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
                Logger.getDefault().v("rotate %s degrees: %s", degrees, srcPath);
                Matrix matrix = new Matrix();
                matrix.postRotate(degrees);
                return matrix;
            }
        }
        return null;
    }

    public static boolean checkMemory(Options opts, float scale) {
        return opts.outWidth * scale * opts.outHeight * scale * 4 < Runtime.getRuntime().maxMemory()
                - Runtime.getRuntime().totalMemory();
    }
}
