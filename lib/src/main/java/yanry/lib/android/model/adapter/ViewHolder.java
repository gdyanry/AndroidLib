/**
 *
 */
package yanry.lib.android.model.adapter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import yanry.lib.android.R;

/**
 * @author yanry
 *
 *         2015年7月29日 上午10:54:35
 */
public class ViewHolder implements PositionHolder {
    private SparseArray<View> views;
    private int position;
    private int viewType;
    private View convertView;
    private Object extra;

    public ViewHolder(View layoutView) {
        views = new SparseArray<View>();
        convertView = layoutView;
        convertView.setTag(R.id.tag_view_holder, this);
    }

    private ViewHolder(View layoutView, int viewType) {
        this(layoutView);
        this.viewType = viewType;
    }

    public static ViewHolder get(View convertView, ViewGroup parent, int position, ViewHolderHook hook) {
        ViewHolder holder = null;
        int viewType = hook.getItemViewType(position);
        if (convertView != null) {
            ViewHolder h = (ViewHolder) convertView.getTag(R.id.tag_view_holder);
            if (h != null && h.viewType == viewType) {
                holder = h;
            }
        }
        if (holder == null || !hook.onRebind(holder, position)) {
            View itemView = hook.getItemView(parent, viewType);
            if (itemView == null) {
                int itemViewId = hook.getItemViewId(viewType);
                if (itemViewId > 0) {
                    itemView = LayoutInflater.from(parent.getContext()).inflate(itemViewId, parent, false);
                } else {
                    throw new RuntimeException("must offer view or view id for view type " + viewType);
                }
            }
            holder = new ViewHolder(itemView, viewType);
        }
        holder.position = position;
        return holder;
    }

    public View getConvertView() {
        return convertView;
    }

    public View getView(int viewId) {
        View v = views.get(viewId);
        if (v == null) {
            v = convertView.findViewById(viewId);
            views.put(viewId, v);
        }
        return v;
    }

    public TextView getTextView(int viewId) {
        return (TextView) getView(viewId);
    }

    public ImageView getImageView(int viewId) {
        return (ImageView) getView(viewId);
    }

    public Object getExtra() {
        return extra;
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }

    @Override
    public int getPosition() {
        return position;
    }
}
