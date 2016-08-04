/**
 * 
 */
package lib.android.view.image.select.interfaces;

import java.io.File;
import java.util.List;

import lib.android.view.image.select.ImageCrop;

/**
 * @author yanry
 *
 *         2016年1月21日
 */
public interface ImageSelectHook {
	void onSelectImage(File image);

	void onSelectImages(List<File> images);

	boolean isMultiSelect();

	/**
	 * 
	 * @param rawCrop
	 *            a not null raw {@link ImageCrop} object to be customized
	 * @return the customized {@link ImageCrop} object, or null if image crop is
	 *         not needed.
	 */
	ImageCrop customizeImageCrop(ImageCrop rawCrop);
}
