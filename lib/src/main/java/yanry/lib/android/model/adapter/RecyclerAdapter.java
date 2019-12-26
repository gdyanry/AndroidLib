/**
 * 
 */
package yanry.lib.android.model.adapter;

import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author yanry
 *
 * 2016年2月3日
 */
public abstract class RecyclerAdapter extends Adapter<RecyclerViewHolder> {

	@Override
	public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = getItemView(parent, viewType);
		if (itemView == null) {
			int itemId = getItemViewId(viewType);
			if (itemId > 0) {
				itemView = LayoutInflater.from(parent.getContext()).inflate(itemId, parent, false);
			}
		}
		return new RecyclerViewHolder(itemView);
	}

	protected abstract int getItemViewId(int viewType);

	protected abstract View getItemView(ViewGroup parent, int viewType);
}
