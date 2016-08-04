/**
 * 
 */
package lib.android.widget;

import java.util.HashMap;
import java.util.Map;

import com.lib.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.ViewDragHelper;
import android.support.v4.widget.ViewDragHelper.Callback;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

/**
 * @author yanry
 *
 * 2015年12月9日
 */
public class ViewDragLayout extends LinearLayout {
	private boolean autoBack;
	private boolean horizontalInside;
	private boolean verticalInside;
	private ViewDragHelper helper;
	private Map<View, int[]> originPos;

	public ViewDragLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ViewDragLayout);
		autoBack = ta.getBoolean(R.styleable.ViewDragLayout_auto_back, false);
		horizontalInside = ta.getBoolean(R.styleable.ViewDragLayout_horizontal_inside, false);
		verticalInside = ta.getBoolean(R.styleable.ViewDragLayout_vertical_inside, false);
		ta.recycle();
		
		if (autoBack) {
			originPos = new HashMap<View, int[]>();
		}
		helper = ViewDragHelper.create(this, new Callback() {
			
			@Override
			public boolean tryCaptureView(View arg0, int arg1) {
				return true;
			}
			
			@Override
			public int clampViewPositionHorizontal(View child, int left, int dx) {
				if (horizontalInside) {
					return Math.min(Math.max(left, getPaddingLeft()), getWidth() - getPaddingRight() - child.getWidth());
				} else {
					return left;
				}
			}
			
			@Override
			public int clampViewPositionVertical(View child, int top, int dy) {
				if (verticalInside) {
					return Math.min(Math.max(top, getPaddingTop()), getHeight() - getPaddingBottom() - child.getHeight());
				} else {
					return top;
				}
			}
			
			@Override
			public void onViewReleased(View releasedChild, float xvel, float yvel) {
				if (autoBack) {
					int[] pos = originPos.get(releasedChild);
					helper.settleCapturedViewAt(pos[0], pos[1]);
//					因为其内部使用的是mScroller.startScroll，所以别忘了需要invalidate()以及结合computeScroll方法一起。
					invalidate();
				}
			}
		});
	}
	
	@Override
	public void computeScroll() {
		if (autoBack && helper.continueSettling(true)) {
			invalidate();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (autoBack) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				originPos.put(child, new int[] {child.getLeft(), child.getTop()});
			}
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_DOWN:
			helper.cancel(); // 相当于调用 processTouchEvent收到ACTION_CANCEL
			return false;
		}
		return helper.shouldInterceptTouchEvent(ev);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		helper.processTouchEvent(event);
		return true;
	}
}
