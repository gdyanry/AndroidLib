/**
 * 
 */
package lib.android.entity;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yanry
 *
 *         2016年5月22日
 */
public class DefaultExecutor extends ThreadPoolExecutor {


	public DefaultExecutor() {
		super(32, Integer.MAX_VALUE, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(512, true));
		allowCoreThreadTimeOut(true);
	}

}
