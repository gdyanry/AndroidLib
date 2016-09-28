package lib.android.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import com.lib.android.R;

/**
 * 自定义view 简单实现圆形和圆角的图片效果
 * 
 * @author lzj
 *
 *         2015年12月29日
 */
public class CustomImageView extends ImageView {

	public static final int TYPE_CIRCLE = 0;
	public static final int TYPE_ROUND = 1;
	/**
	 * TYPE_CIRCLE / TYPE_ROUND
	 */
	private int mType;
	/**
	 * 图片
	 */
	private Bitmap mSrc;

	/**
	 * 圆角大学
	 */
	private int mRadius;

	/**
	 * 控件宽度
	 */
	private int mWidth;
	/**
	 * 控件高度
	 */
	private int mHeight;

	public CustomImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CustomImageView(Context context) {
		this(context, null);
	}

	/**
	 * 自定义参数
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public CustomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomImageView, defStyle, 0);

		int n = a.getIndexCount();
		for (int i = 0; i < n; i++) {
			int attr = a.getIndex(i);
			if (attr == R.styleable.CustomImageView_civ_type) {
				mType = a.getInt(attr, 0);// 默认为圆形
			} else if (attr == R.styleable.CustomImageView_civ_borderRadius) {
				mRadius = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						10f, getResources().getDisplayMetrics()));// 默认是10dp
			}
		}
		a.recycle();
		if (attrs != null) {
			int src_resource = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "src", 0);
			mSrc = BitmapFactory.decodeResource(getResources(), src_resource);
		}
	}

	/**
	 * 测量控件宽高
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		/**
		 * 宽度
		 */
		int specMode = MeasureSpec.getMode(widthMeasureSpec);
		int specSize = MeasureSpec.getSize(widthMeasureSpec);

		if (specMode == MeasureSpec.EXACTLY) // match_parent , accurate
		{
			mWidth = specSize;
		} else {
			// 由图片决定宽度
			int desireByImg = getPaddingLeft() + getPaddingRight() + (mSrc == null ? 0 : mSrc.getWidth());
			if (specMode == MeasureSpec.AT_MOST) // wrap_content
			{
				mWidth = Math.min(desireByImg, specSize);
			} else

				mWidth = desireByImg;
		}

		/***
		 * 高度
		 */

		specMode = MeasureSpec.getMode(heightMeasureSpec);
		specSize = MeasureSpec.getSize(heightMeasureSpec);
		if (specMode == MeasureSpec.EXACTLY) // match_parent , accurate
		{
			mHeight = specSize;
		} else {
			int desire = getPaddingTop() + getPaddingBottom() + (mSrc == null ? 0 : mSrc.getHeight());

			if (specMode == MeasureSpec.AT_MOST) // wrap_content
			{
				mHeight = Math.min(desire, specSize);
			} else
				mHeight = desire;
		}

		setMeasuredDimension(mWidth, mHeight);

	}

	/**
	 * 画控件
	 */
	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		if (mSrc != null) {
			int min = Math.min(mWidth, mHeight);
			/**
			 * 图片大小不一致，按小的值压缩
			 */
			mSrc = Bitmap.createScaledBitmap(mSrc, min, min, false);
			switch (mType) {
			// 圆形控件
			case TYPE_CIRCLE:
				canvas.drawBitmap(createCircleImage(mSrc, min), 0, 0, null);
				break;
			// 圆角控件
			case TYPE_ROUND:
				mSrc = Bitmap.createScaledBitmap(mSrc, min, min, false);
				canvas.drawBitmap(createRoundConerImage(mSrc), 0, 0, null);
				break;
			}
		} else {
			super.onDraw(canvas);
		}

	}

	/**
	 * 根据原图和变长绘制图片
	 * 
	 * @param source
	 * @param min
	 * @return
	 */
	private Bitmap createCircleImage(Bitmap source, int min) {
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		Bitmap target = Bitmap.createBitmap(min, min, Config.ARGB_8888);
		/**
		 * 制作一个画布
		 */
		Canvas canvas = new Canvas(target);
		/**
		 * 画出圆形
		 */
		canvas.drawCircle(min / 2, min / 2, min / 2, paint);
		/**
		 * 取图片和圆形的交集，生成圆形图片
		 */
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		/**
		 * 生成圆形图片
		 */
		canvas.drawBitmap(source, 0, 0, paint);
		return target;
	}

	/**
	 * 生成圆角图片
	 * 
	 * @param source
	 * @return
	 */
	private Bitmap createRoundConerImage(Bitmap source) {
		final Paint paint = new Paint();
		paint.setAntiAlias(true);
		Bitmap target = Bitmap.createBitmap(mWidth, mHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(target);
		RectF rect = new RectF(0, 0, source.getWidth(), source.getHeight());
		canvas.drawRoundRect(rect, mRadius, mRadius, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(source, 0, 0, paint);
		return target;
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		mSrc = bm;
		super.setImageBitmap(bm);

	}

	@Override
	public void setImageResource(int resId) {
		mSrc = BitmapFactory.decodeResource(getResources(), resId);
		super.setImageResource(resId);

	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		BitmapDrawable bmd = (BitmapDrawable) drawable;
		mSrc = bmd.getBitmap();
		super.setImageDrawable(drawable);
	}

	/**
	 * 
	 * @param mType
	 *            TYPE_CIRCLE = 0 circle TYPE_ROUND = 1 round
	 * @param size
	 *            if type is circle radius not use else mRadius = radius
	 */
	public void setType(int type, int radius) {
		this.mType = type;
		mRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, radius,
				getResources().getDisplayMetrics());
	}

}
