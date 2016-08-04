/**
 * 
 */
package lib.android.model.adapter;

import android.view.View;

/**
 * @author yanry
 *
 * 2016年6月13日
 */
public interface ViewHolderHook {

	int getItemViewType(int position);

	int getItemViewId(int viewType);

	View getItemView(int viewType);
	
	boolean onRebind(ViewHolder holder, int newPosition);
}
