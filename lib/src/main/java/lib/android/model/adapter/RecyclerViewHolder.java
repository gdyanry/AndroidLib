/**
 * 
 */
package lib.android.model.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author yanry
 *
 * 2016年2月3日
 */
public class RecyclerViewHolder extends ViewHolder implements PositionHolder {
	private SparseArray<View> views;

	public RecyclerViewHolder(View itemView) {
		super(itemView);
		views = new SparseArray<View>();
	}

	public View getViewById(int id) {
		View v = views.get(id);
		if (v == null) {
			v = itemView.findViewById(id);
			views.put(id, v);
		}
		return v;
	}
	
	public TextView getTextView(int id) {
		return (TextView) getViewById(id);
	}
	
	public ImageView getImageView(int id) {
		return (ImageView) getViewById(id);
	}
}
