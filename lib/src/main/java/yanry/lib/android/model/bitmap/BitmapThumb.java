/**
 *
 */
package yanry.lib.android.model.bitmap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;

import yanry.lib.android.util.BitmapUtil;
import yanry.lib.java.model.log.Logger;

/**
 * @author yanry
 * <p>
 * 2015年11月11日
 */
public class BitmapThumb {
    private Decoder decoder;
    private ThumbDimension width;
    private ThumbDimension height;
    private String srcPath;
    private boolean useNearScale;

    public BitmapThumb(Decoder decoder) {
        this.decoder = decoder;
    }

    public BitmapThumb width(ThumbDimension width) {
        this.width = width;
        return this;
    }

    public BitmapThumb height(ThumbDimension height) {
        this.height = height;
        return this;
    }

    /**
     * 当宽高同时设置时，一般会计算出两个不同的缩放比例，默认采用缩放程度较大的比例，调用此方法则会采用缩放程度较小（更接近1）的比例。
     *
     * @return
     */
    public BitmapThumb useNearScale() {
        useNearScale = true;
        return this;
    }

    /**
     * 某些手机（比如三星）拍照后图片加载出来是被旋转过的，可以调用此方法把图片转回来。
     *
     * @param srcPath path of the rotated picture.
     * @return
     */
    public BitmapThumb autoRotate(String srcPath) {
        this.srcPath = srcPath;
        return this;
    }

    public Bitmap createThumb() throws Exception {
        long start = System.currentTimeMillis();
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        Bitmap bitmap = decoder.decode(opts);// 此时返回bm为空
        float scale = 1;
        if (width != null || height != null) {
            // choose the right dimension to apply scaling
            ThumbDimension validDimen;
            if (width == null) {
                validDimen = height;
            } else if (height == null) {
                validDimen = width;
            } else {
                float widthScale = width.getScale(opts.outWidth);
                float heightScale = height.getScale(opts.outHeight);
                validDimen = (useNearScale ^ (widthScale > heightScale)) ? height : width;
            }
            int originDimen = validDimen == width ? opts.outWidth : opts.outHeight;
            scale = validDimen.getScale(originDimen);
            bitmap = validDimen.createBitmap(originDimen, decoder, opts, srcPath);
        } else {
            if (!BitmapUtil.checkMemory(opts, scale)) {
                Logger.getDefault().ee("not enough memory for bitmap creation");
                return null;
            }
            opts.inJustDecodeBounds = false;
            bitmap = decoder.decode(opts);
            if (srcPath != null && bitmap != null) {
                Matrix matrix = BitmapUtil.getRotateMatrix(srcPath);
                if (matrix != null) {
                    if (BitmapUtil.checkMemory(opts, scale)) {
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    } else {
                        Logger.getDefault().e("not enough memory for bitmap rotation");
                    }
                }
            }
        }
        Logger.getDefault().v("get thumb(%s*%s@%s): %sms", bitmap == null ? null : bitmap.getWidth(),
                bitmap == null ? null : bitmap.getHeight(), scale, System.currentTimeMillis() - start);
        return bitmap;
    }

    public byte[] compress(int sizeLimitKb, CompressFormat format) throws Exception {
        long threshold = sizeLimitKb * 1024;
        Bitmap bitmap = createThumb();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 90;
        bitmap.compress(format, options, baos);
        if (sizeLimitKb > 0) {
            while (baos.size() > threshold && (options -= 15) >= 0) { // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
                long start = System.currentTimeMillis();
                baos.reset();// 重置baos即清空baos
                bitmap.compress(format, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
                Logger.getDefault().v("compress: %s%%, %sBytes, %sms", options, baos.size(), System.currentTimeMillis() - start);
            }
        }
        return baos.toByteArray();
    }

    public interface Decoder {
        /**
         * This will be invoked twice when width or height is limited, which can
         * be distinguished by checking the parameter
         * {@link Options #inJustDecodeBounds}.
         *
         * @param opts
         * @return
         * @throws Exception
         */
        Bitmap decode(Options opts) throws Exception;
    }
}
