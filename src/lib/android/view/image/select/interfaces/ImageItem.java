/**
 * 
 */
package lib.android.view.image.select.interfaces;

import java.io.File;

import android.view.View;
import lib.android.model.adapter.ViewHolder;

/**
 * @author yanry
 *
 *         2016年1月16日
 */
public interface ImageItem extends OnActivityResultListener {

	int getItemViewId();

	void display(ViewHolder holder, File imageFile, int viewWidth, boolean isClick);

	/**
	 * @param itemView
	 * @param imageFile
	 * @return return true to refresh the hosted adapter view.
	 */
	boolean onClick(View itemView, File imageFile);
}
