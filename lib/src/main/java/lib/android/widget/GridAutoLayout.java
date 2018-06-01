/**
 * 
 */
package lib.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.lib.android.R;

import lib.android.util.CommonUtils;
import lib.common.model.GridCalculator;
import lib.common.model.GridCalculator.ItemWidthType;

/**
 * The layout width of this widget must be {@link MeasureSpec#EXACTLY}.
 * 
 * @author yanry
 *
 *         2016年2月27日
 */
public class GridAutoLayout extends ViewGroup {

	private ContentHook hook;
	private GridCalculator cal;
	private int hPadding;
	private int vPadding;

	public GridAutoLayout(Context context) {
		super(context);
	}

	public GridAutoLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		cal = new GridCalculator() {

			@Override
			protected void onItemWidthCalculated(int val) {
				if (hook != null) {
					hook.onMeasured(ContentHook.MEASURE_TYPE_ITEM_WIDTH, val);
				}
			}

			@Override
			protected void onColumnNumberCalculated(int val) {
				if (hook != null) {
					hook.onMeasured(ContentHook.MEASURE_TYPE_COLUMN_NUMBER, val);
				}
			}
		};
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GridAutoLayout);
		cal.columnNumber(ta.getInt(R.styleable.GridAutoLayout_columnNumber, 0));
		int suggestItemWidth = ta.getDimensionPixelSize(R.styleable.GridAutoLayout_suggestItemWidth, 0);
		if (suggestItemWidth > 0) {
			cal.itemWidthType(ItemWidthType.Suggest).itemWidth(suggestItemWidth);
		}
		int minItemWidth = ta.getDimensionPixelSize(R.styleable.GridAutoLayout_minItemWidth, 0);
		if (minItemWidth > 0) {
			cal.itemWidthType(ItemWidthType.Min).itemWidth(minItemWidth);
		}
		int itemWidth = ta.getDimensionPixelSize(R.styleable.GridAutoLayout_itemWidth, 0);
		if (itemWidth > 0) {
			cal.itemWidthType(ItemWidthType.Fixed).itemWidth(itemWidth);
		}
		hPadding = ta.getDimensionPixelSize(R.styleable.GridAutoLayout_paddingHorizontal, 0);
		cal.itemSpace(hPadding);
		vPadding = ta.getDimensionPixelSize(R.styleable.GridAutoLayout_paddingVertical, 0);
		ta.recycle();
	}

	public void setContentHook(ContentHook hook) {
		this.hook = hook;
		addChildren();
	}

	public void notifyContentChange() {
		addChildren();
	}

	public void setColumnNumer(int columnNum) {
		cal.columnNumber(columnNum);
	}

	public int getColumnNumber() {
		return cal.getColumnNumber();
	}

	/**
	 * 
	 * @param widthDp
	 * @param type
	 */
	public void setItemWidth(float widthDp, ItemWidthType type) {
		cal.itemWidthType(type).itemWidth(CommonUtils.dip2px(widthDp));
	}

	public void setItemPadding(float padding, boolean isHorizontal) {
		if (isHorizontal) {
			cal.itemSpace(CommonUtils.dip2px(padding));
		} else {
			vPadding = CommonUtils.dip2px(padding);
		}
	}

	private void addChildren() {
		if (hook != null && cal.getColumnNumber() > 0) {
			removeAllViews();
			for (int i = 0; i < hook.getItemCount(); i++) {
				addView(hook.getView(this, cal.getItemWidth(), i));
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		if (width == 0) {
			setMeasuredDimension(width, height);
			return;
		}
		int availableWidth = width - getPaddingLeft() - getPaddingRight();
		cal.availableWidth(availableWidth).calculate();
		// calculate content height taking vertical paddings in count
		int contentHeight = getPaddingBottom() + getPaddingTop();
		if (hook != null) {
			int count = hook.getItemCount();
			if (count > 0 && getChildCount() == 0) {
				addChildren();
			}
			// TODO when returned item height is 0, make its height to wrap content
			int itemHeight = hook.getItemHeight(cal.getItemWidth());
			// measure children in exactly mode
			for (int i = 0; i < getChildCount(); i++) {
				getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cal.getItemWidth(), MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(itemHeight, MeasureSpec.EXACTLY));
			}

			int rowNum = count / cal.getColumnNumber() + ((count % cal.getColumnNumber() > 0) ? 1 : 0);
			contentHeight += (rowNum > 0 ? (itemHeight * rowNum + vPadding * (rowNum - 1)) : 0);
			hook.onMeasured(ContentHook.MEASURE_TYPE_CONTENT_HEIGHT, contentHeight);
		}

		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		if (wMode != MeasureSpec.EXACTLY) {
			// change width to wrap content
			// TODO test
			width = getPaddingLeft() + getPaddingRight() + cal.getItemWidth() * cal.getColumnNumber();
		}
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		if (hMode != MeasureSpec.EXACTLY) {
			// change height to wrap content
			height = contentHeight;
		}
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (hook != null) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				int row = i / cal.getColumnNumber();// 求出第几行
				int column = i % cal.getColumnNumber(); // 求出一行的第几列
				int left = getPaddingLeft() + cal.getItemWidth() * column + hPadding * column;
				// TODO when child views' height is different from each other
				int height = child.getMeasuredHeight();
				int top = getPaddingTop() + height * row + vPadding * row;
				child.layout(left, top, left + cal.getItemWidth(), top + height);
			}
		}
	}

	public interface ContentHook {
		int MEASURE_TYPE_COLUMN_NUMBER = 1;
		int MEASURE_TYPE_ITEM_WIDTH = 2;
		int MEASURE_TYPE_CONTENT_HEIGHT = 3;

		void onMeasured(int type, int value);

		/**
		 * @param width
		 *            the measured item width in pixel.
		 * @return item height in pixel.
		 */
		int getItemHeight(int width);

		int getItemCount();

		View getView(ViewGroup parent, int itemWidth, int position);
	}

}
