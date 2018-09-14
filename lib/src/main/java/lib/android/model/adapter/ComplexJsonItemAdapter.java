/**
 * 
 */
package lib.android.model.adapter;

import lib.common.model.cache.ResizableLruCache;

/**
 * @author yanry
 *
 * 2016年6月12日
 */
public abstract class ComplexJsonItemAdapter extends JsonArrayAdapter {
	private ResizableLruCache<Object, ComplexItem<Object, ViewHolder>> map;
	
	public ComplexJsonItemAdapter() {
		map = new ResizableLruCache<Object, ComplexItem<Object, ViewHolder>>(getCount() + 1) {
			@Override
			protected ComplexItem<Object, ViewHolder> create(Object key) {
				return createComplexItem(key);
			}
		};
	}
	
	public ResizableLruCache<Object, ComplexItem<Object, ViewHolder>> getCache() {
		return map;
	}

	@Override
	protected void display(ViewHolder holder, Object itemData, int position) {
		map.get(itemData).display(holder);
	}

	@Override
	public void notifyDataSetChanged() {
		map.resize(getCount() + 1);
		super.notifyDataSetChanged();
	}
	
	protected abstract ComplexItem<Object, ViewHolder> createComplexItem(Object itemData);
}
