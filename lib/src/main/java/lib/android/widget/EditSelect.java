/**
 * 
 */
package lib.android.widget;

import android.R.color;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.lib.android.R;

/**
 * @author yanry
 *
 * 2015年12月10日
 */
public abstract class EditSelect extends FrameLayout implements View.OnClickListener, OnItemClickListener {

	private EditText et;
	private ImageView iv;
	private PopupWindow pop;
	private SelectListView popView;
	private int dropMode;
	private OnItemClickListener itemClickListener;
	
	public EditSelect(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.edit_select, this, true);
		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EditSelect);
		dropMode = ta.getInt(R.styleable.EditSelect_drop_mode, 0);
		ta.recycle();
		
		popView = new SelectListView(context);
	}
	
	public void setAdapter(BaseAdapter adapter) {
		popView.setAdapter(adapter);
		pop = new PopupWindow(popView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		pop.setBackgroundDrawable(new ColorDrawable(color.transparent));
		pop.setFocusable(true); // 让popwin获取焦点
	}
	
	public void setOnListItemClickListener(OnItemClickListener listener) {
		this.itemClickListener = listener;
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		popView.setOnItemClickListener(this);
		et = (EditText) findViewById(R.id.edit_select_et);
		et.setSelectAllOnFocus(true);
		iv = (ImageView) findViewById(R.id.edit_select_iv);
		iv.setOnClickListener(this);
		init(et, iv);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		// 如果布局发生改
		// 并且dropMode是flower_parent
		// 则设置ListView的宽度
		if(changed && 0 == dropMode) {
			popView.mWidth = getMeasuredWidth();
		}
	}

	@Override
	public void onClick(View v) {
		if(pop.isShowing()) {
			pop.dismiss();
		} else {
			pop.showAsDropDown(this, 0, 5);
		}
		onListStateChange(iv, pop.isShowing());
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		et.setText(popView.getAdapter().getItem(position).toString());
		pop.dismiss();
		onListStateChange(iv, false);
		if (itemClickListener != null) {
			itemClickListener.onItemClick(parent, view, position, id);
		}
	}
	
	protected abstract void init(EditText et, ImageView iv);
	
	protected abstract void onListStateChange(ImageView iv, boolean isShow);
	
	private class SelectListView extends ListView {
		
		private int mWidth;
		
		public SelectListView(Context context) {
			super(context);
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			for (int i = 0; i < getChildCount(); i++) {
				mWidth = Math.max(mWidth, getChildAt(i).getMeasuredWidth());
			}
			setMeasuredDimension(mWidth, getMeasuredHeight());
		}
	}
}
