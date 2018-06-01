/**
 * 
 */
package lib.android.model.adapter;

import java.util.List;

/**
 * @author yanry
 *
 *         2015年11月25日
 */
public abstract class ListAdapter<T> extends CommonAdapter {

	@Override
	public int getCount() {
		return getData() == null ? 0 : getData().size();
	}

	@Override
	public Object getItem(int position) {
		return getData().get(position);
	}

	@Override
	protected void display(ViewHolder holder, int position) {
		T itemData = getData().get(position);
		display(holder, itemData, position);
	}

	protected abstract List<T> getData();

	protected abstract void display(ViewHolder holder, T itemData, int position);
}
