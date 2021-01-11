/**
 * 
 */
package yanry.lib.android.view;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.Singletons;

/**
 * @author yanry
 *
 * 2016年6月14日
 */
public abstract class TimeBarHandler implements Runnable, OnSeekBarChangeListener {
	private SeekBar bar;
	private int increment;
	private boolean inProgress;
	private int updatePeriod;
	
	public TimeBarHandler(int updateIntervalMillis) {
		this.updatePeriod = updateIntervalMillis;
	}

	public void init(SeekBar bar, int durationMillis) {
		this.bar = bar;
		bar.setMax(durationMillis);
		bar.setOnSeekBarChangeListener(this);
		increment = durationMillis / updatePeriod;
	}
	
	public SeekBar getSeekBar() {
		return bar;
	}
	
	public void start() {
		inProgress = true;
		Singletons.get(UiRunner.class).postDelayed(this, updatePeriod);
	}
	
	public void stop() {
		inProgress = false;
	}
	
	protected abstract void onFinish();
	
	protected int getCurrentProgress() {
		return bar.getProgress() + increment;
	}
	
	@Override
    public final void run() {
		if (inProgress) {
			int newProgress = getCurrentProgress();
			if (newProgress < bar.getMax()) {
				Singletons.get(UiRunner.class).postDelayed(this, updatePeriod);
				if (newProgress >= 0) {
					bar.setProgress(newProgress);
				}
			} else {
				bar.setProgress(bar.getMax());
				inProgress = false;
				onFinish();
			}
		}
	}
}
