/**
 * 
 */
package lib.android.widget;

import java.io.IOException;
import java.io.InputStream;

import com.almeros.android.multitouch.MoveGestureDetector;
import com.almeros.android.multitouch.MoveGestureDetector.OnMoveGestureListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author yanry
 *
 *         2015年11月27日
 */
public class LargeImageView extends View {
	private BitmapRegionDecoder mDecoder;
	/**
	 * 图片的宽度和高度
	 */
	private int mImageWidth, mImageHeight;
	/**
	 * 绘制的区域
	 */
	private volatile Rect mRect = new Rect();

	private MoveGestureDetector mDetector;

	private static final BitmapFactory.Options options = new BitmapFactory.Options();

	static {
		options.inPreferredConfig = Bitmap.Config.RGB_565;
	}

	public void setInputStream(InputStream is) throws IOException {
		mDecoder = BitmapRegionDecoder.newInstance(is, false);
		BitmapFactory.Options tmpOptions = new BitmapFactory.Options();
		// Grab the bounds for the scene dimensions
		tmpOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(is, null, tmpOptions);
		mImageWidth = tmpOptions.outWidth;
		mImageHeight = tmpOptions.outHeight;

		requestLayout();
		invalidate();
	}

	private void checkWidth() {
		if (mRect.right > mImageWidth) {
			mRect.right = mImageWidth;
			mRect.left = mImageWidth - getWidth();
		}

		if (mRect.left < 0) {
			mRect.left = 0;
			mRect.right = getWidth();
		}
	}

	private void checkHeight() {
		if (mRect.bottom > mImageHeight) {
			mRect.bottom = mImageHeight;
			mRect.top = mImageHeight - getHeight();
		}

		if (mRect.top < 0) {
			mRect.top = 0;
			mRect.bottom = getHeight();
		}
	}

	public LargeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mDetector = new MoveGestureDetector(getContext(), new SimpleMoveGestureDetector() {
			@Override
			public boolean onMove(MoveGestureDetector detector) {
				int moveX = (int) detector.getFocusX();
				int moveY = (int) detector.getFocusY();

				if (mImageWidth > getWidth()) {
					mRect.offset(-moveX, 0);
					checkWidth();
					invalidate();
				}
				if (mImageHeight > getHeight()) {
					mRect.offset(0, -moveY);
					checkHeight();
					invalidate();
				}

				return true;
			}
		});
	}
	
	@Override
	public boolean performClick() {
		return super.performClick();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mDetector.onTouchEvent(event) || performClick();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Bitmap bm = mDecoder.decodeRegion(mRect, options);
		canvas.drawBitmap(bm, 0, 0, null);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = getMeasuredWidth();
		int height = getMeasuredHeight();

		int imageWidth = mImageWidth;
		int imageHeight = mImageHeight;

		// 默认直接显示图片的中心区域，可以自己去调节
		mRect.left = imageWidth / 2 - width / 2;
		mRect.top = imageHeight / 2 - height / 2;
		mRect.right = mRect.left + width;
		mRect.bottom = mRect.top + height;

	}

	private class SimpleMoveGestureDetector implements OnMoveGestureListener {

		@Override
		public boolean onMoveBegin(MoveGestureDetector detector) {
			return true;
		}

		@Override
		public boolean onMove(MoveGestureDetector detector) {
			return false;
		}

		@Override
		public void onMoveEnd(MoveGestureDetector detector) {
		}
	}
}
