/**
 * 
 */
package yanry.lib.android.model.adapter;

import yanry.lib.android.model.runner.UiRunner;
import yanry.lib.java.model.Singletons;

/**
 * @author yanry
 *
 * 2016年5月21日
 */
public abstract class ComplexItem<T, H extends PositionHolder> implements Runnable {
	private H holder;
	private int position;
	private T data;
	
	public ComplexItem(T data) {
		this.data = data;
	}
	
	public T getData() {
		return data;
	}
	
	public H getLastViewHolder() {
		return holder;
	}
	
	public int getCurrentPosition() {
		return position;
	}

	public void display(H holder) {
		this.holder = holder;
		this.position = holder.getPosition();
		display(holder, data, false);
	}
	
	/**
	 * Invoke this in the data_state-change-callback methods if you have set callback on this item.
	 */
	public void notifyStateChange() {
		Singletons.get(UiRunner.class).run(this);
	}
	
	public boolean isActive() {
		return holder != null && holder.getPosition() == position;
	}
	
	protected abstract void display(H holder, T data, boolean fromNotify);
	
	@Override
	public final void run() {
		if (isActive()) {
			display(holder, data, true);
		}
	}
}
