/**
 * 
 */
package yanry.lib.android.model.request;

import yanry.lib.android.entity.enums.RequestState;
import yanry.lib.android.util.CommonUtils;
import yanry.lib.java.model.communication.base.RequestLiveListener;

/**
 * @author yanry
 *
 * 2016年5月31日
 */
public abstract class AndroidRequestLiveListener implements RequestLiveListener, Runnable {
	private RequestState lastState;
	private Object data;
	
	protected abstract void onStateChange(RequestState state, Object data);

	@Override
	public void onNoConnection() {
		changeState(RequestState.NoConnection, null);
	}

	@Override
	public void onStartRequest() {
		changeState(RequestState.Start, null);
	}

	@Override
	public void onConnectionError(Object e) {
		changeState(RequestState.Error, e);
	}

	@Override
	public void onFinish(Object data) {
		changeState(RequestState.Finish, data);
	}

	@Override
	public void onCancel() {
		changeState(RequestState.Cancel, data);
	}

	private synchronized void changeState(RequestState state, Object data) {
		this.data = data;
		lastState = state;
		CommonUtils.runOnUiThread(this);
	}

	@Override
    public final synchronized void run() {
		if (lastState != null) {
			// uncertain runtime exception might happen.
			try {
				onStateChange(lastState, data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			lastState = null;
		}
	}
}
