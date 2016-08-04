/**
 * 
 */
package lib.android.view.image.select;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

/**
 * @author yanry
 *
 *         2016年1月16日
 */
public abstract class ImageCrop {

	private Intent intent;

	/**
	 * 
	 * @param data
	 *            裁剪对象，若为null，则从系统相册中选取图片进行裁剪。
	 * @param outUri
	 *            裁剪结果的输出地址，若为null。则裁剪结果通过
	 *            {@link OnCropResultListener#onCropResult(Bitmap)}
	 *            返回Bitmap对象。Actually the secret of cropping photos on Android
	 *            is using Uri if the photo is in large size, or using Bitmap if
	 *            you need but make sure that the bitmap is not too big.
	 */
	public ImageCrop(Uri data, Uri outUri) {
		String type = "image/*";
		this.intent = data == null
				? new Intent(Intent.ACTION_GET_CONTENT).setDataAndType(Images.Media.EXTERNAL_CONTENT_URI, type)
				: new Intent("com.android.camera.action.CROP").setDataAndType(data, type);
		intent.putExtra("crop", "true").putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		if (outUri == null) {
			intent.putExtra("return-data", true);
		} else {
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outUri);
		}
	}

	public ImageCrop setAspect(int x, int y) {
		intent.putExtra("aspectX", x).putExtra("aspectY", y);
		return this;
	}

	public ImageCrop setOutput(int x, int y) {
		intent.putExtra("outputX", x).putExtra("outputY", y);
		return this;
	}

	public ImageCrop keepScale() {
		intent.putExtra("scale", true);
		return this;
	}

	public ImageCrop noFaceDetection() {
		intent.putExtra("noFaceDetection", true);
		return this;
	}

	public ImageCrop circleCrop() {
		intent.putExtra("circleCrop", "true");
		return this;
	}

	/**
	 * 进入系统裁剪界面。
	 * 
	 * @param ctx
	 *            当前activity。需要重写此activity的onActivityResult方法并调用
	 *            {@link #onActivityResult(int, int, Intent, OnCropResultListener)}
	 *            .
	 */
	public void execute(Activity ctx) {
		if (intent.resolveActivity(ctx.getPackageManager()) != null) {
			ctx.startActivityForResult(intent, getRequestCode());
		}
	}

	/**
	 * 在接收回调的activity的onActivityResult方法中调用此方法
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @param listener
	 * @return
	 */
	public boolean onActivityResult(int requestCode, int resultCode, Intent data, OnCropResultListener listener) {
		if (requestCode == getRequestCode()) {
			if (resultCode == Activity.RESULT_OK) {
				Bitmap bmp = null;
				if (data != null && data.getExtras() != null) {
					// 若outUri为null，此时的intent会有一个"inline-data"的Action，并且以extras.getParcelable("data")的方式返回Bitmap对象。
					bmp = data.getExtras().getParcelable("data");
				}
				if (listener != null) {
					listener.onCropResult(bmp);
				}
			}
			return true;
		}
		return false;
	}

	protected abstract int getRequestCode();

	public interface OnCropResultListener {
		/**
		 * 裁剪结果的回调
		 * 
		 * @param bmp
		 *            如果构造函数的outUri参数为null，则此参数不为null，反之该参数为null。
		 */
		void onCropResult(Bitmap bmp);
	}
}
