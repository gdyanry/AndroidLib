/**
 *
 */
package yanry.lib.android.entity;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import yanry.lib.java.model.log.Logger;

/**
 * @author yanry
 * <p>
 * 2016年5月22日
 */
public class DefaultExecutor extends ThreadPoolExecutor {

    public DefaultExecutor() {
        super(32, Integer.MAX_VALUE, 2, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(512, true));
        allowCoreThreadTimeOut(true);
    }

    @Override
    public void execute(Runnable command) {
        Logger.getDefault().ii(this);
        super.execute(command);
    }
}
