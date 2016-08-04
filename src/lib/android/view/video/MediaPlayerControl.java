/**
 * 
 */
package lib.android.view.video;

/**
 * @author yanry
 *
 *         2016年6月23日
 */
public interface MediaPlayerControl {
	void start();

	void pause();

	long getDuration();

	long getCurrentPosition();

	void seekTo(long pos);

	boolean isPlaying();

	int getBufferPercentage();
}
