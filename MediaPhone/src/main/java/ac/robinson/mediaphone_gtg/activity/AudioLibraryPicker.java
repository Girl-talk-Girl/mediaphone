package ac.robinson.mediaphone_gtg.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.View;

import ac.robinson.mediaphone_gtg.MediaPhoneActivity;
import ac.robinson.mediaphone_gtg.R;

public class AudioLibraryPicker extends MediaPhoneActivity {

	private MediaPlayer mMediaPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.audio_library_chooser);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mMediaPlayer = null;
	}

	public void handleAudioLibraryClick(View view) {
		final int audioId;
		final int viewId = view.getId();
		switch (viewId) {
			case R.id.audio_library_subway:
				audioId = R.raw.subway;
				break;
			case R.id.audio_library_ambulance:
				audioId = R.raw.ambulance;
				break;
			case R.id.audio_library_footsteps:
				audioId = R.raw.footsteps;
				break;
			case R.id.audio_library_sunny_day:
				audioId = R.raw.sunny_day;
				break;
			case R.id.audio_library_door_knock:
				audioId = R.raw.door_knock;
				break;
			case R.id.audio_library_door_close:
				audioId = R.raw.door_close;
				break;
			case R.id.audio_library_crowd_talking:
				audioId = R.raw.crowd_talking;
				break;
			case R.id.audio_library_car_honk:
				audioId = R.raw.car_honk;
				break;
			case R.id.audio_library_glass_breaking:
				audioId = R.raw.glass_breaking;
				break;
			case R.id.audio_library_woman_laughing_1:
				audioId = R.raw.woman_laughing_1;
				break;
			case R.id.audio_library_woman_laughing_2:
				audioId = R.raw.woman_laughing_2;
				break;

			default:
				audioId = -1;
				break;
		}

		if (audioId > 0) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra(getString(R.string.extra_resource_id), audioId);
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
			return;
		}

		switch (viewId) {
			case R.id.audio_library_subway_play:
				playSound(R.raw.subway);
				break;
			case R.id.audio_library_ambulance_play:
				playSound(R.raw.ambulance);
				break;
			case R.id.audio_library_footsteps_play:
				playSound(R.raw.footsteps);
				break;
			case R.id.audio_library_sunny_day_play:
				playSound(R.raw.sunny_day);
				break;
			case R.id.audio_library_door_knock_play:
				playSound(R.raw.door_knock);
				break;
			case R.id.audio_library_door_close_play:
				playSound(R.raw.door_close);
				break;
			case R.id.audio_library_crowd_talking_play:
				playSound(R.raw.crowd_talking);
				break;
			case R.id.audio_library_car_honk_play:
				playSound(R.raw.car_honk);
				break;
			case R.id.audio_library_glass_breaking_play:
				playSound(R.raw.glass_breaking);
				break;
			case R.id.audio_library_woman_laughing_1_play:
				playSound(R.raw.woman_laughing_1);
				break;
			case R.id.audio_library_woman_laughing_2_play:
				playSound(R.raw.woman_laughing_2);
				break;
		}
	}

	@Override
	protected void onDestroy() {
		releaseMediaPlayer();
		super.onDestroy();
	}

	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}
		mMediaPlayer = null;
	}

	private void playSound(int audioId) {
		releaseMediaPlayer();
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		AssetFileDescriptor file = getResources().openRawResourceFd(audioId);
		try {
			mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
			file.close();
			// mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
			mMediaPlayer.prepare();
		} catch (Throwable t) {
			releaseMediaPlayer();
		}

		if (mMediaPlayer != null) {
			try {
				mMediaPlayer.start();
			} catch (Throwable t) {
				releaseMediaPlayer();
			}
		}
	}


	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {

	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {

	}
}
