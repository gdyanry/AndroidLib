/**
 * 
 */
package lib.android.model.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * @author yanry
 *
 *         2015年9月26日
 */
public abstract class CommonAdapter extends BaseAdapter implements ViewHolderHook {

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = ViewHolder.get(convertView, parent, position, this);
		display(holder, position);
		return holder.getConvertView();
	}
	
	@Override
	public boolean onRebind(ViewHolder holder, int newPosition) {
		return true;
	}
	
	protected abstract void display(ViewHolder holder, int position);

}
