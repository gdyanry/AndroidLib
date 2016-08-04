/**
 * 
 */
package lib.android.view.video;

import java.util.Map;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.view.SurfaceHolder;

/**
 * @author yanry
 *
 *         2016年6月28日
 */
public class DefaultVideoPlayer implements VideoPlayer, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener, OnPreparedListener, OnVideoSizeChangedListener {
	private MediaPlayer mPlayer;
	private VideoSurfaceView mVideoView;
	private int mCurrentBufferPercentage;

	public DefaultVideoPlayer(VideoSurfaceView videoView) {
		mVideoView = videoView;
		mPlayer = new MediaPlayer();
		mPlayer.setOnBufferingUpdateListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnVideoSizeChangedListener(this);
	}

	@Override
	public void setAudioSessionId(int sessionId) {
		mPlayer.setAudioSessionId(sessionId);
	}

	@Override
	public int getAudioSessionId() {
		return mPlayer.getAudioSessionId();
	}

	@Override
	public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws Exception {
		mPlayer.setDataSource(context, uri, headers);
	}

	@Override
	public void setDisplay(SurfaceHolder surfaceHolder) {
		mPlayer.setDisplay(surfaceHolder);
	}

	@Override
	public void setAudioStreamType(int streamType) {
		mPlayer.setAudioStreamType(streamType);
	}

	@Override
	public void prepareAsync() throws Exception {
		mPlayer.prepareAsync();
	}

	@Override
	public int getVideoWidth() {
		return mPlayer.getVideoWidth();
	}

	@Override
	public int getVideoHeight() {
		return mPlayer.getVideoHeight();
	}

	@Override
	public void stop() {
		mPlayer.stop();
	}

	@Override
	public void release() {
		mPlayer.release();
	}

	@Override
	public void reset() {
		mPlayer.reset();
	}

	@Override
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	@Override
	public void start() {
		mPlayer.start();
	}

	@Override
	public void pause() {
		mPlayer.pause();
	}

	@Override
	public long getDuration() {
		return mPlayer.getDuration();
	}

	@Override
	public long getCurrentPosition() {
		return mPlayer.getCurrentPosition();
	}

	@Override
	public int getBufferPercentage() {
		return mCurrentBufferPercentage;
	}

	@Override
	public void seekTo(long msec) {
		mPlayer.seekTo((int) msec);
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		mCurrentBufferPercentage = percent;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		mVideoView.onCompletion();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mVideoView.onError();
		return true;
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
		mVideoView.onVideoSizeChanged();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mVideoView.onPrepared();
	}

}
