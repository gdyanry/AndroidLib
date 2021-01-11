/**
 * 
 */
package yanry.lib.android.model;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.Singletons;

/**
 * @author yanry
 *
 * 2016年1月4日
 */
public abstract class SimpleCountDown {
	private boolean stop;
	private int counter;
	private int intervalMillis;
	
	public SimpleCountDown(int initCounter, int intervalMillis) {
		counter = initCounter;
		this.intervalMillis = intervalMillis;
		onCounterChange(initCounter);
		tick();
	}
	
	public void stop() {
		stop = true;
	}
	
	private void tick() {
		if (stop) {
			return;
		}
		Singletons.get(UiRunner.class).postDelayed(new Runnable() {
			public void run() {
				if (--counter < 0) {
					onFinish();
				} else {
					tick();
					onCounterChange(counter);
				}
			}
		}, intervalMillis);
	}
	
	protected abstract void onFinish();
	
	protected abstract void onCounterChange(int count);
}
