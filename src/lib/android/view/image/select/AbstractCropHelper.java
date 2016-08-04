/**
 * 
 */
package lib.android.view.image.select;

import java.io.File;

import android.net.Uri;

/**
 * @author yanry
 *
 *         2016年7月13日
 */
abstract class AbstractCropHelper {
	/**
	 * 
	 * @param src
	 *            file to crop.
	 * @param dest
	 *            the crop output file.
	 * @return return an {@link ImageCrop} object used to crop image, or null if
	 *         you don't need to crop image.
	 */
	ImageCrop getImageCrop(File src, File dest) {
		if (getCropImageRequestCode() > 0) {
			return customizeImageCrop(new ImageCrop(Uri.fromFile(src), Uri.fromFile(dest)) {

				@Override
				protected int getRequestCode() {
					return getCropImageRequestCode();
				}
			});
		} else {
			return null;
		}
	}

	File createTempFile() {
		File dir = getTempFolder();
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		return new File(dir, System.currentTimeMillis() + ".jpg");
	}

	/**
	 * 
	 * @return the returned value must be >=0, other wise image crop is disable.
	 */
	protected abstract int getCropImageRequestCode();

	protected abstract ImageCrop customizeImageCrop(ImageCrop rawCrop);

	protected abstract File getTempFolder();
}
