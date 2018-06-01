/**
 * 
 */
package lib.android.view.image.preview;

import android.app.Activity;

import uk.co.senab.photoview.PhotoView;

/**
 * @author yanry
 *
 * 2016年1月28日
 */
public interface PreviewImageData {

	int getCount();
	
	int getInitialPosition();
	
	String getThumb(int position);
	
	String getImage(int position);
	
	void delete(int position);
	
	void initPhotoView(PhotoView view, int position, Activity previewActivity);
}
