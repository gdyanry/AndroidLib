/**
 * 
 */
package yanry.lib.android.view.image.interfaces;

import android.view.View;

import java.io.File;

import yanry.lib.android.model.adapter.ViewHolder;

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
