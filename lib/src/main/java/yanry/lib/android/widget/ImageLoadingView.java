/**
 * 
 */
package yanry.lib.android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ViewSwitcher;

import yanry.lib.android.R;

/**
 * @author yanry
 *
 *         2015年4月29日 下午6:01:12
 */
public class ImageLoadingView extends FrameLayout {
	private ImageView iv;
	private ViewSwitcher vs;

	public ImageLoadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.view_image_loading, this, true);

		iv = (ImageView) findViewById(R.id.iv);
		vs = (ViewSwitcher) findViewById(R.id.vs);
	}

	public void setScaleType(ScaleType scaleType) {
		iv.setScaleType(scaleType);
	}
	
	public void setImageBitmap(Bitmap bmp) {
		iv.setImageBitmap(bmp);
		vs.showNext();
	}
	
	public void setImageResource(int res) {
		iv.setImageResource(res);
		vs.showNext();
	}
}
