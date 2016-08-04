/**
 * 
 */
package lib.android.model.bitmap.access;

import java.util.concurrent.Executor;

import lib.common.model.resourceaccess.FileHashMapper;
import lib.common.model.resourceaccess.UrlFileAccess;

/**
 * @author yanry
 *
 * 2016年7月14日
 */
public abstract class Level2FileAccess extends UrlFileAccess {

	public Level2FileAccess(FileHashMapper mapper) {
		super(mapper);
	}

	@Override
	protected Executor getGenerationExecutor() {
		return null;
	}

}
