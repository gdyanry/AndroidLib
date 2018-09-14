/**
 * 
 */
package lib.android.model.adapter;

import lib.common.model.cache.ResizableLruCache;

/**
 * @author yanry
 *
 * 2016年5月21日
 */
public abstract class ComplexItemAdapter<T> extends ListAdapter<T> {
	private ResizableLruCache<T, ComplexItem<T, ViewHolder>> map;
	
	public ComplexItemAdapter() {
		map = new ResizableLruCache<T, ComplexItem<T, ViewHolder>>(getCount() + 1) {
			@Override
			protected ComplexItem<T, ViewHolder> create(T key) {
				return createComplexItem(key);
			}
		};
	}
	
	public ResizableLruCache<T, ComplexItem<T, ViewHolder>> getCache() {
		return map;
	}

	@Override
	protected void display(ViewHolder holder, T itemData, int position) {
		map.get(itemData).display(holder);
	}
	
	@Override
	public void notifyDataSetChanged() {
		map.resize(getCount() + 1);
		super.notifyDataSetChanged();
	}

	protected abstract ComplexItem<T, ViewHolder> createComplexItem(T itemData);
}
