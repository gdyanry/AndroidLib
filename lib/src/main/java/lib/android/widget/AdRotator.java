/**
 * 
 */
package lib.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import lib.android.R;

/**
 * An advertisement displayer. You can disable manual flip by calling
 * {@link #setFlipDistance(int)} or {@link #setFlipVelocity(float)} with a
 * big-enough parameter.
 * 
 * @author yanry
 *
 *         2015年10月21日
 */
public class AdRotator extends ViewFlipper implements OnGestureListener {
	private GestureDetector gd;
	private int distance;
	private float velocity;
	private Context ctx;
	private ViewGroup indicator;
	private int normalIndicatorRes;
	private int selectedIndicatorRes;
	private OnAdRotatorItemClickListener mOnAdRotatorItemClickListener = null;

	public AdRotator(Context context, AttributeSet attrs) {
		super(context, attrs);
		gd = new GestureDetector(context, this);
		distance = 10;
		ctx = context;
	}

	public void init(ViewGroup indicator, int normalIndicatorRes, int selectedIndicatorRes) {
		this.indicator = indicator;
		this.normalIndicatorRes = normalIndicatorRes;
		this.selectedIndicatorRes = selectedIndicatorRes;
		if (isFlipping()) {
			stopFlipping();
		}
		removeAllViews();
		if (null != indicator) {
			indicator.removeAllViews();
		}
	}

	public void addFrame(View v) {
		addView(v);
		if (null != indicator) {
			ImageView item = new ImageView(ctx);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			params.setMargins(5, 0, 5, 0);
			item.setLayoutParams(params);
			indicator.addView(item);
			// first view
			if (1 == getChildCount()) {
				item.setBackgroundResource(selectedIndicatorRes);
			} else {
				item.setBackgroundResource(normalIndicatorRes);
			}
		}
	}

	public void setFlipDistance(int minDistance) {
		distance = minDistance;
	}

	public void setFlipVelocity(float minVelocity) {
		velocity = minVelocity;
	}

	@Override
	public void showNext() {
		setInAnimation(ctx, R.anim.slide_in_right);
		setOutAnimation(ctx, R.anim.slide_out_left);
		int oldChild = getDisplayedChild();
		super.showNext();
		updateIndicator(oldChild, getDisplayedChild());
	}

	@Override
	public void showPrevious() {
		setInAnimation(ctx, android.R.anim.slide_in_left);
		setOutAnimation(ctx, android.R.anim.slide_out_right);
		int oldChild = getDisplayedChild();
		super.showPrevious();
		updateIndicator(oldChild, getDisplayedChild());
	}

	private void updateIndicator(int oldChild, int newChild) {
		if (null != indicator) {
			View o = indicator.getChildAt(oldChild);
			if (o != null) {
				o.setBackgroundResource(normalIndicatorRes);
			}

			View n = indicator.getChildAt(newChild);
			if (n != null) {
				n.setBackgroundResource(selectedIndicatorRes);
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		requestDisallowInterceptTouchEvent(true);
		gd.onTouchEvent(event);
		return true;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (mOnAdRotatorItemClickListener != null) {
			mOnAdRotatorItemClickListener.onAdRotatorItemClick(getDisplayedChild());
		}
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (Math.abs(velocityX) > velocity && e1 != null && e2 != null) {
			if (e1.getX() - e2.getX() > distance) {
				showNext();
				return true;
			}
			if (e2.getX() - e1.getX() > distance) {
				showPrevious();
				return true;
			}
		}
		return false;
	}
	
	public void setOnAdRotatorItemClickListener(OnAdRotatorItemClickListener l) {
		this.mOnAdRotatorItemClickListener = l;
	}
	
	public interface OnAdRotatorItemClickListener {
		void onAdRotatorItemClick(int position);
	}
	
}
