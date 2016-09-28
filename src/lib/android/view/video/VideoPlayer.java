/**
 * 
 */
package lib.android.view.video;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;

import java.util.Map;

/**
 * @author yanry
 *
 * 2016年6月25日
 */
public interface VideoPlayer {

	int getAudioSessionId();

	void setAudioSessionId(int sessionId);
	
	void setDataSource(Context context, Uri uri, Map<String, String> headers) throws Exception;
	
	void setDisplay(SurfaceHolder surfaceHolder);
	
	void setAudioStreamType(int streamType);
	
	void prepareAsync() throws Exception;
	
	int getVideoWidth();
	
	int getVideoHeight();
	
	void stop();
	
	void release();
	
	void reset();
	
	boolean isPlaying();
	
	void start();
	
	void pause();
	
	long getDuration();
	
	long getCurrentPosition();
	
	int getBufferPercentage();
	
	void seekTo(long msec);
}
