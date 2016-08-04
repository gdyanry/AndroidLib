/**
 * 
 */
package lib.android.view.image.select;

import java.io.File;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import lib.android.model.adapter.CommonAdapter;
import lib.android.model.adapter.ViewHolder;
import lib.android.util.CommonUtils;
import lib.android.view.image.select.interfaces.ImageItem;
import lib.android.view.image.select.interfaces.LeadItem;
import lib.android.view.image.select.interfaces.OnActivityResultListener;

/**
 * @author yanry
 *
 *         2016年6月7日
 */
public abstract class ImageSelectViewHelper extends CommonAdapter implements OnItemClickListener, OnActivityResultListener, Runnable {
	private static final int DEFAULT_IMAGE_WIDTH_DP = 120;
	private GridView gv;
	private int desiredImageSizeDp;
	private LeadItem[] leadItems;
	private ImageItem imageItem;
	private List<File> images;
	private int clickPos;
	private GridView.LayoutParams itemLayoutParams;
	private File currentFolder;
	private ImageSelectHandler handler;

	public ImageSelectViewHelper(ImageSelectHandler handler, GridView gv, final int desiredImageSizeDp, ImageItem imageItem, LeadItem... leadItems) {
		this.handler = handler;
		this.gv = gv;
		this.desiredImageSizeDp = desiredImageSizeDp;
		this.imageItem = imageItem;
		this.leadItems = leadItems == null ? new LeadItem[0] : leadItems;
		clickPos = -1;

		images = handler.getAlbumImages(null);
		calculateItemWidth();
		if (images == null || images.size() == 0) {
			if (gv.getContext().checkCallingOrSelfPermission(
					"android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_DENIED) {
				onPermissionDenied();
			}
		}
	}

	@SuppressLint("NewApi")
	private void calculateItemWidth() {
		int parentWidth = gv.getWidth();
		if (parentWidth > 0) {
			int desireSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					desiredImageSizeDp == 0 ? DEFAULT_IMAGE_WIDTH_DP : desiredImageSizeDp,
					gv.getContext().getResources().getDisplayMetrics());
			if (desireSize > parentWidth) {
				desireSize = parentWidth;
			}
			int gap = 10;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				gap = gv.getHorizontalSpacing();
			}
			int columnNum = (parentWidth + gap) / (desireSize + gap);
			if ((parentWidth - (columnNum - 1) * gap) * (parentWidth - columnNum * gap) > desireSize * desireSize
					* columnNum * (columnNum + 1)) {
				columnNum++;
			}
			gv.setNumColumns(columnNum);
			int itemViewWidth = (parentWidth - gap * (columnNum - 1)) / columnNum;
			onEvaluateItemWidth(itemViewWidth);
			itemLayoutParams = new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, itemViewWidth);
			gv.setAdapter(this);
			gv.setOnItemClickListener(this);
		} else {
			Activity activity = CommonUtils.getActivity(gv.getContext());
			if (activity != null) {
				activity.getWindow().getDecorView().post(this);
			}
		}
	}

	public void setFolder(File folder) {
		currentFolder = folder;
		refresh();
	}

	public void refresh() {
		images = handler.getAlbumImages(currentFolder);
		notifyDataSetChanged();
	}
	
	public ImageItem getImageItem() {
		return imageItem;
	}

	protected void onEvaluateItemWidth(int width) {
	}
	
	protected abstract void onPermissionDenied();

	@Override
	public void run() {
		calculateItemWidth();
	}

	/**
	 * Remember to override the hosted image select activity's
	 * onActivityResult() and call this method.
	 */
	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!imageItem.onActivityResult(requestCode, resultCode, data)) {
			for (OnActivityResultListener l : leadItems) {
				if (l.onActivityResult(requestCode, resultCode, data)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		clickPos = position;
		if (position < leadItems.length) {
			LeadItem leadItemView = leadItems[position];
			if (leadItemView.isClickable()) {
				if (leadItemView.onClick(view)) {
					notifyDataSetChanged();
				}
			}
		} else {
			if (images != null && images.size() > (position - leadItems.length)) {
				if (imageItem.onClick(view, images.get(position - leadItems.length))) {
					notifyDataSetChanged();
				}
			}
		}
	}

	@Override
	public Object getItem(int position) {
		if (position < leadItems.length) {
			return position;
		}
		if (images != null && images.size() > (position - leadItems.length)) {
			return images.get(position - leadItems.length);
		}
		return null;
	}

	@Override
	public int getCount() {
		return images == null ? 0 : images.size() + leadItems.length;
	}

	@Override
	protected void display(ViewHolder holder, int position) {
		boolean isClick = clickPos == position;
		if (isClick) {
			clickPos = -1;
		}
		View v = holder.getConvertView();
		if (v.getLayoutParams().height != itemLayoutParams.height) {
			v.setLayoutParams(itemLayoutParams);
		}
		if (position >= leadItems.length && images != null && images.size() > position - leadItems.length) {
			File file = images.get(position - leadItems.length);
			imageItem.display(holder, file, itemLayoutParams.height, isClick);
		} else {
			leadItems[position].display(holder, isClick);
		}
	}

	@Override
	public int getItemViewId(int viewType) {
		if (viewType < leadItems.length) {
			return leadItems[viewType].getViewId();
		}
		return imageItem.getItemViewId();
	}

	@Override
	public View getItemView(int viewType) {
		return null;
	}

	@Override
	public int getViewTypeCount() {
		return leadItems.length + 1;
	}

	@Override
	public int getItemViewType(int position) {
		if (position < leadItems.length) {
			return position;
		} else {
			return leadItems.length;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}