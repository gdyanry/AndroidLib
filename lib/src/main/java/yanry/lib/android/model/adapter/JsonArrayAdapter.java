/**
 * 
 */
package yanry.lib.android.model.adapter;

import yanry.lib.java.model.json.JSONArray;

/**
 * @author yanry
 *
 * 2015年10月7日
 */
public abstract class JsonArrayAdapter extends CommonAdapter {

	@Override
	public int getCount() {
		return null == getData() ? 0 : getData().length();
	}

	@Override
	public Object getItem(int position) {
		return getData().get(position);
	}

	@Override
	protected void display(ViewHolder holder, int position) {
		Object itemData = getData().get(position);
		display(holder, itemData, position);
	}
	
	protected abstract JSONArray getData();

	protected abstract void display(ViewHolder holder, Object itemData, int position);
}
