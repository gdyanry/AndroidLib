/**
 * 
 */
package lib.android.view.image.select;

import java.io.File;

import android.app.Activity;
import android.view.View;
import lib.android.util.CommonUtils;
import lib.android.view.image.select.interfaces.LeadItem;

/**
 * @author yanry
 *
 *         2016年1月16日
 */
public abstract class ImageCaptureItem extends ImageCaptureHandler implements LeadItem {
	private Activity activity;

	protected abstract ImageSelectHandler getImageSelectHandler();
	
	protected abstract ImageSelectViewHelper getImageSelectViewHelper();
	
	@Override
	public boolean onClick(View view) {
		activity = CommonUtils.getActivity(view.getContext());
		if (activity != null) {
			start(activity);
		}
		return false;
	}
	
	@Override
	protected ImageCrop customizeImageCrop(ImageCrop rawCrop) {
		return getImageSelectHandler().getHook().customizeImageCrop(rawCrop);
	}
	
	@Override
	protected void onResult(File file) {
		ImageSelectHandler imageSelectHandler = getImageSelectHandler();
		if (imageSelectHandler.getHook().isMultiSelect()) {
			getImageSelectViewHelper().getImageItem().onClick(null, file);
			getImageSelectViewHelper().notifyDataSetChanged();
		} else {
			getImageSelectHandler().getHook().onSelectImage(file);
			if (activity != null) {
				activity.finish();
			}
		}
	}
}
