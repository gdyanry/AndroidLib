/**
 * 
 */
package lib.android.model.bitmap;

/**
 * @author yanry
 *
 * 2016年7月15日
 */
public abstract class DownloadHook implements Runnable {
	static final int ACTION_START = 1;
	static final int ACTION_UPDATE = 2;
	static final int ACTION_FINISH = 3;
	static final int ACTION_ERROR = 4;
	int action;
	long startPos;
	long currentPos;
	long totalLen;

	protected abstract int getUpdateInterval();
	
	protected abstract void onStart(long startPos, long totalLen);
	
	protected abstract void onUpdate(long currentPos);
	
	protected abstract void onFinish();
	
	protected abstract void onError();
	
	@Override
	public void run() {
		switch (action) {
		case ACTION_START:
			onStart(startPos, totalLen);
			break;
		case ACTION_UPDATE:
			onUpdate(currentPos);
			break;
		case ACTION_FINISH:
			onFinish();
			break;
		case ACTION_ERROR:
			onError();
			break;
		}
	}
}
