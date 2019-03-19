/**
 * 
 */
package yanry.lib.android.entity;

import android.os.Handler;
import android.os.Looper;

/**
 * @author yanry
 *
 * 2016年6月1日
 */
public class MainHandler extends Handler {
	public MainHandler() {
		super(Looper.getMainLooper());
	}
}
