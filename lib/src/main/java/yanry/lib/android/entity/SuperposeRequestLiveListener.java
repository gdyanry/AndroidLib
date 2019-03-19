/**
 * 
 */
package yanry.lib.android.entity;

import yanry.lib.java.model.communication.base.RequestLiveListener;

/**
 * @author yanry
 *
 *         2016年2月26日
 */
public class SuperposeRequestLiveListener implements RequestLiveListener {
	private RequestLiveListener backed;

	public SuperposeRequestLiveListener setBacked(RequestLiveListener backed) {
		this.backed = backed;
		return this;
	}

	public RequestLiveListener getBacked() {
		return backed;
	}

	@Override
	public void onNoConnection() {
		if (backed != null) {
			backed.onNoConnection();
		}
	}

	@Override
	public void onStartRequest() {
		if (backed != null) {
			backed.onStartRequest();
		}
	}

	@Override
	public void onConnectionError(Object e) {
		if (backed != null) {
			backed.onConnectionError(e);
		}
	}

	@Override
	public void onFinish(Object data) {
		if (backed != null) {
			backed.onFinish(data);
		}
	}

	@Override
	public void onCancel() {
		if (backed != null) {
			backed.onCancel();
		}
	}

}
