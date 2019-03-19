/**
 * 
 */
package yanry.lib.android.model.bitmap;

/**
 * @author yanry
 *
 *         2016年7月17日
 */
public class CacheKey<S> {
	private S src;
	private ThumbDimension width;
	private ThumbDimension height;
	private boolean useNearScale;

	public CacheKey(S src, ThumbDimension width, ThumbDimension height, boolean useNearScale) {
		this.src = src;
		this.width = width;
		this.height = height;
		this.useNearScale = useNearScale;
	}

	public S getSrc() {
		return src;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((height == null) ? 0 : height.hashCode());
		result = prime * result + ((src == null) ? 0 : src.hashCode());
		result = prime * result + (useNearScale ? 1231 : 1237);
		result = prime * result + ((width == null) ? 0 : width.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CacheKey<?> other = (CacheKey<?>) obj;
		if (height == null) {
            if (other.height != null) {
                return false;
            }
        } else if (!height.equals(other.height)) {
            return false;
        }
		if (src == null) {
            if (other.src != null) {
                return false;
            }
        } else if (!src.equals(other.src)) {
            return false;
        }
        if (useNearScale != other.useNearScale) {
            return false;
        }
		if (width == null) {
            return other.width == null;
        } else {
            return width.equals(other.width);
        }
    }

}
