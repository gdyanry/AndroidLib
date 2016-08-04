/**
 * 
 */
package lib.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ViewSwitcher;
import lib.android.entity.enums.RequestState;
import lib.android.model.request.AndroidRequestLiveListener;
import lib.common.model.communication.base.RequestLiveListener;

/**
 * 应晓亮的奇葩要求把所有抽象方法改成public，一开始我是拒绝的。
 * 
 * @author yanry
 *
 *         2015年11月18日
 */
public abstract class RequestListenPage extends ViewSwitcher implements RequestLiveListener {
	private AndroidRequestLiveListener l;
	private boolean showingContent;
	private boolean showingMessage;

	public RequestListenPage(Context context, AttributeSet attrs) {
		super(context, attrs);
		l = new AndroidRequestLiveListener() {

			@Override
			protected void onStateChange(RequestState state, Object data) {
				switch (state) {
				case NoConnection:
					showError(true);
					break;
				case Error:
					showError(false);
					break;
				case Finish:
					if (!showingMessage) {
						showNext();
					}
					break;
				case Start:
					showingMessage = false;
					showPrevious();
					setErrorViewVisibility(false);
					setMessageViewVisibility(false);
					setLoadingViewVisibility(true);
					startLoadingAnimation();
					break;
				default:
					break;
				}
			}
		};
	}

	public void setOnClickRetryListener(OnClickListener listener) {
		View vRetry = getRetryView();
		if (vRetry != null) {
			vRetry.setOnClickListener(listener);
		}
	}

	/**
	 * Should be called in UI thread.
	 * 
	 * @param msg
	 */
	public void showMessage(String msg) {
		showingMessage = true;
		showPrevious();
		setErrorViewVisibility(false);
		stopLoadingAnimation();
		setLoadingViewVisibility(false);
		setMessageViewVisibility(true);
		if (msg != null) {
			setMessageText(msg);
		}
	}

	private void showError(boolean isNoConnection) {
		showPrevious();
		setErrorViewVisibility(true);
		stopLoadingAnimation();
		setLoadingViewVisibility(false);
		setMessageViewVisibility(false);
		View vRetry = getRetryView();
		if (vRetry != null) {
			vRetry.setVisibility(isNoConnection ? GONE : VISIBLE);
		}
		setErrorText(isNoConnection);
	}

	public boolean isShowingContent() {
		return showingContent;
	}

	public abstract void setErrorViewVisibility(boolean show);

	public abstract void setLoadingViewVisibility(boolean show);

	public abstract void setMessageViewVisibility(boolean show);

	public abstract View getRetryView();

	public abstract void setMessageText(String msg);

	public abstract void setErrorText(boolean isNoConnection);

	public abstract void startLoadingAnimation();

	public abstract void stopLoadingAnimation();

	@Override
	public void onNoConnection() {
		l.onNoConnection();
	}

	@Override
	public void onStartRequest() {
		l.onStartRequest();
	}

	@Override
	public void onConnectionError(Object e) {
		l.onConnectionError(e);
	}

	@Override
	public void onFinish(Object data) {
		l.onFinish(data);
	}

	@Override
	public void onCancel() {
		l.onCancel();
	}

	@Override
	public void showPrevious() {
		if (showingContent) {
			showingContent = false;
			super.showPrevious();
		}
	}

	@Override
	public void showNext() {
		if (!showingContent) {
			showingContent = true;
			super.showNext();
		}
	}

}
