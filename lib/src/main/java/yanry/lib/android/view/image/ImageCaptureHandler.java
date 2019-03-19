/**
 * 
 */
package yanry.lib.android.view.image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import java.io.File;

import yanry.lib.android.util.CommonUtils;
import yanry.lib.android.view.image.interfaces.OnActivityResultListener;

/**
 * @author yanry
 *
 *         2016年1月16日
 */
public abstract class ImageCaptureHandler extends AbstractCropHelper implements OnActivityResultListener, ImageCrop.OnCropResultListener {
	private File captureOut;
	private File cropOut;
	private ImageCrop crop;
	private Activity ctx;

	/**
	 * 
	 * @param context
	 *            don't forget to call
	 *            {@link #onActivityResult(int, int, Intent)} on this activity.
	 */
	public void start(Activity context) {
		captureOut = createTempFile();
		ctx = context;
		CommonUtils.requestCamera(ctx, captureOut, getRequestCode());
	}

	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == getRequestCode()) {
			if (resultCode == Activity.RESULT_OK) {
				cropOut = createTempFile();
				crop = getImageCrop(captureOut, cropOut);
				if (crop == null) {
					onResult(captureOut);
				} else {
					crop.execute(ctx);
				}
			} else {
				if (captureOut.isFile()) {
					captureOut.delete();
				}
			}
			return true;
		}
		if (crop != null) {
			return crop.onActivityResult(requestCode, resultCode, data, this);
		}
		return false;
	}

	@Override
	public void onCropResult(Bitmap bmp) {
		if (captureOut.isFile()) {
			captureOut.delete();
		}
		onResult(cropOut);
	}

	protected abstract int getRequestCode();
	
	protected abstract void onResult(File file);
}
