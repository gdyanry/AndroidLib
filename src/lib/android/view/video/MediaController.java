/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.android.view.video;

import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public abstract class MediaController implements OnSeekBarChangeListener {

	private MediaPlayerControl mPlayer;
	private ProgressBar mProgress;
	private boolean mShowing;
	private boolean mDragging;
	private static final int sDefaultTimeout = 3000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private ControllerHandler mHandler;

	public MediaController(ProgressBar pb) {
		mHandler = new ControllerHandler(this);
		mProgress = pb;
		if (mProgress != null) {
			if (mProgress instanceof SeekBar) {
				SeekBar seeker = (SeekBar) mProgress;
				// There are two scenarios that can trigger the seekbar listener
				// to trigger:
				//
				// The first is the user using the touchpad to adjust the
				// posititon of the
				// seekbar's thumb. In this case onStartTrackingTouch is called
				// followed by
				// a number of onProgressChanged notifications, concluded by
				// onStopTrackingTouch.
				// We're setting the field "mDragging" to true for the duration
				// of the dragging
				// session to avoid jumps in the position in case of ongoing
				// playback.
				//
				// The second scenario involves the user operating the scroll
				// ball, in this
				// case there WON'T BE onStartTrackingTouch/onStopTrackingTouch
				// notifications,
				// we will simply apply the updated position without suspending
				// regular updates.
				seeker.setOnSeekBarChangeListener(this);
			}
			mProgress.setMax(1000);
		}
	}

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
		updatePausePlay(mPlayer.isPlaying());
	}

	/**
	 * Show the controller on screen. It will go away automatically after 3
	 * seconds of inactivity.
	 */
	public void show() {
		show(sDefaultTimeout);
	}

	/**
	 * Show the controller on screen. It will go away automatically after
	 * 'timeout' milliseconds of inactivity.
	 * 
	 * @param timeout
	 *            The timeout in milliseconds. Use 0 to show the controller
	 *            until hide() is called.
	 */
	public void show(int timeout) {
		if (!mShowing) {
			setProgress();
			switchVisibility(true);
			mShowing = true;
		}
		updatePausePlay(mPlayer.isPlaying());

		// cause the progress bar to be updated even if mShowing
		// was already true. This happens, for example, if we're
		// paused with the progress bar showing the user hits play.
		mHandler.sendEmptyMessage(SHOW_PROGRESS);

		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	public boolean isShowing() {
		return mShowing;
	}

	/**
	 * Remove the controller from the screen.
	 */
	public void hide() {
		if (mShowing) {
			mHandler.removeMessages(SHOW_PROGRESS);
			switchVisibility(false);
			mShowing = false;
		}
	}

	private long setProgress() {
		if (mPlayer == null || mDragging) {
			return 0;
		}
		long position = mPlayer.getCurrentPosition();
		long duration = mPlayer.getDuration();
		if (mProgress != null) {
			if (duration > 0) {
				// use long to avoid overflow
				long pos = 1000L * position / duration;
				mProgress.setProgress((int) pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgress.setSecondaryProgress(percent * 10);
		}

		updateTime(position, duration);

		return position;
	}

	public void pauseResume() {
		doPauseResume();
		show(sDefaultTimeout);
	}

	private void doPauseResume() {
		if (mPlayer != null) {
			boolean playing = mPlayer.isPlaying();
			if (playing) {
				mPlayer.pause();
			} else {
				mPlayer.start();
			}
			updatePausePlay(!playing);
		}
	}
	
	public void rewind(long jumpMillis) {
		long pos = mPlayer.getCurrentPosition();
		pos -= jumpMillis; // milliseconds
		mPlayer.seekTo(pos);
		setProgress();

		show(sDefaultTimeout);
	}

	public void forward(long jumpMillis) {
		long pos = mPlayer.getCurrentPosition();
		pos += jumpMillis; // milliseconds
		mPlayer.seekTo(pos);
		setProgress();

		show(sDefaultTimeout);
	}

	public void setEnabled(boolean enabled) {
		if (mProgress != null) {
			mProgress.setEnabled(enabled);
		}
	}

	protected abstract void updateTime(long position, long duration);

	protected abstract void updatePausePlay(boolean isPlaying);
	
	protected abstract void switchVisibility(boolean show);

	private static class ControllerHandler extends Handler {
		private MediaController controller;
		
		public ControllerHandler(MediaController controller) {
			this.controller = controller;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FADE_OUT:
				controller.hide();
				break;
			case SHOW_PROGRESS:
				long pos = controller.setProgress();
				if (!controller.mDragging && controller.mShowing && controller.mPlayer.isPlaying()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar bar) {
		show(3600000);
		mDragging = true;

		// By removing these pending progress messages we make sure
		// that a) we won't update the progress while the user adjusts
		// the seekbar and b) once the user is done dragging the thumb
		// we will post one of these messages to the queue again and
		// this ensures that there will be exactly one message queued up.
		mHandler.removeMessages(SHOW_PROGRESS);
	}

	@Override
	public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
		if (!fromuser) {
			// We're not interested in programmatically generated changes to
			// the progress bar's position.
			return;
		}

		long duration = mPlayer.getDuration();
		long newposition = (duration * progress) / 1000L;
		mPlayer.seekTo((int) newposition);
		updateTime(newposition, duration);
	}

	@Override
	public void onStopTrackingTouch(SeekBar bar) {
		mDragging = false;
		setProgress();
		updatePausePlay(mPlayer.isPlaying());
		show(sDefaultTimeout);

		// Ensure that progress is properly updated in the future,
		// the call to show() does not guarantee this because it is a
		// no-op if we are already showing.
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
	}

}
