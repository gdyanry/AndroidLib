/**
 *
 */
package yanry.lib.android.model.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;

import java.security.InvalidParameterException;

import yanry.lib.android.model.bitmap.BitmapThumb.Decoder;
import yanry.lib.android.util.BitmapUtil;
import yanry.lib.java.model.log.Logger;

/**
 * @author yanry
 * <p>
 * 2016年5月26日
 */
public class ThumbDimension {
    private int value;
    private FitType type;

    public ThumbDimension(int value) {
        if (value <= 0) {
            throw new InvalidParameterException("value must be larger than 0");
        }
        this.value = value;
        this.type = FitType.RoundNear;
    }

    public int getValue() {
        return value;
    }

    public FitType getType() {
        return type;
    }

    /**
     * 生成缩略图时，默认采用{@link FitType#RoundNear}类型，即通过{@link Options#inSampleSize}
     * 来设定缩放比例，而且会先把运算得到的比例值向下转换为2的指数次幂，比如3变成2，7变成4，20变成16，再进行缩放。
     * 这样导致的结果是得到的缩略图很可能比你给定的宽高要大一些，一般对于显示图片的场景而言这是没有问题的。如果你希望得到的缩略图不大于给定的宽高，
     * 则可以把类型指定为{@link FitType#RoundFar}；如果你希望得到的缩略图切确等于给定宽高，则使用类型
     * {@link FitType#Exact}，但是此类型会先使用{@link FitType#RoundNear}
     * 生成一张较大缩略图，再进行矩阵变换得到切确宽高的{@link Bitmap}，性能较差，如非必要都不应该使用此类型。
     *
     * @param type
     * @return
     */
    public ThumbDimension setType(FitType type) {
        if (type == null) {
            throw new NullPointerException();
        }
        this.type = type;
        return this;
    }

    public float getScale(int originDimen) {
        switch (type) {
            case RoundFar:
                return 1.0f / roundSampleSize((int) Math.ceil(originDimen * 1.0 / value), true);
            case RoundNear:
                return 1.0f / roundSampleSize(originDimen / value, false);
            default:
                return value * 1.0f / originDimen;
        }
    }

    /**
     * @param originDimen
     * @param decoder
     * @param opts        {@link Options} that contains width and height info of the source file
     * @param srcPath
     * @return
     * @throws Exception
     */
    public Bitmap createBitmap(int originDimen, Decoder decoder, Options opts, String srcPath) throws Exception {
        float scale = getScale(originDimen);
        if (opts == null) {
            opts = new Options();
        } else if (!BitmapUtil.checkMemory(opts, scale)) {
            Logger.getDefault().ee("not enough memory for bitmap creation");
            return null;
        }
        opts.inJustDecodeBounds = false;
        if (type == FitType.RoundFar) {
            opts.inSampleSize = roundSampleSize((int) Math.ceil(originDimen * 1.0 / value), true);
        } else {
            opts.inSampleSize = originDimen / value;
        }
        Bitmap bitmap = decoder.decode(opts);
        // do matrix
        if (bitmap != null) {
            Matrix matrix = BitmapUtil.getRotateMatrix(srcPath);
            if (type == FitType.Exact && value < originDimen && opts.inSampleSize != originDimen * 1f / value) {
                if (matrix == null) {
                    matrix = new Matrix();
                }
                matrix.postScale(scale, scale);
            }
            if (matrix != null) {
                if (BitmapUtil.checkMemory(opts, scale)) {
                    Logger.getDefault().dd("convert bitmap: ", matrix);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                } else {
                    Logger.getDefault().ee("not enough memory for bitmap translation");
                }
            }
        }
        return bitmap;
    }

    private int roundSampleSize(int inSampleSize, boolean up) {
        if (inSampleSize == 0) {
            return 1;
        }
        if (inSampleSize <= 2) {
            return inSampleSize;
        }
        int i = 0;
        while (inSampleSize > 2 << i) {
            i++;
        }
        return 2 << (up ? i : --i);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + value;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ThumbDimension other = (ThumbDimension) obj;
        if (type != other.type)
            return false;
        if (value != other.value)
            return false;
        return true;
    }

}
