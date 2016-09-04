package com.sensoro.experience.tool;

import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.hardware.SensorManager;
import android.content.Context;
import com.sensoro.beacon.kit.Beacon;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.widget.Toast;
import java.util.TimerTask;
import java.util.Timer;
/*
 * Control the notification of beacon.
 */
public class NotificationFragment extends Fragment {

	SwitchButton switchButton;
	Beacon beacon;
	float mAccelLast;
	MainActivity activity;
	SoundPool soundPool;
	int soundId1,soundId2;
	MediaPlayer player1, player2;

	Timer timer = new Timer(true);
	private SensorManager mSensorManager;
	private float mAccel; // acceleration apart from gravity
	private float mAccelCurrent; //

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		activity = (MainActivity) getActivity();
		return inflater.inflate(R.layout.fragment_notification, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		initCtrl();
		setTitle();
		super.onActivityCreated(savedInstanceState);
		player1 = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.m1);
		player2 = MediaPlayer.create(getActivity().getApplicationContext(), R.raw.m2);
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
		soundId1 = soundPool.load(getActivity(),R.raw.slow,1);
		soundId2 = soundPool.load(getActivity(),R.raw.music,2);
		mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		mAccel = 0.00f;
		mAccelCurrent = SensorManager.GRAVITY_EARTH;
		mAccelLast = SensorManager.GRAVITY_EARTH;

	//	timer.schedule(task1, 1000, 10000);
		timer.schedule(task2, 1000, 7000);
	}

	private void initCtrl() {
		beacon = (Beacon) getArguments().get(MainActivity.BEACON);
		activity = (MainActivity) getActivity();
		setHasOptionsMenu(true);

		switchButton = (SwitchButton) activity.findViewById(R.id.fragment_notification_sb);
	}

	private void setTitle() {
		activity.setTitle(R.string.back);
	}


	private final SensorEventListener mSensorListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent se) {
			float x = se.values[0];
			float y = se.values[1];
			float z = se.values[2];
			mAccelLast = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
			float delta = mAccelCurrent - mAccelLast;
			mAccel = mAccel * 0.9f + delta; // perform low-cut filter

			if (mAccel > 0 && mAccel < 2) {
				//		Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 1.", Toast.LENGTH_SHORT);
				//		toast.show();
				//		soundPool.play(soundId1,1.0f,1.0f,2,-1,1);
			}
			else if ( mAccel > 2) {
				//		Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 2.", Toast.LENGTH_SHORT);
				//		toast.show();
				//		soundPool.play(soundId2,1.0f,1.0f,2,-1,1);
			}
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	@Override
	public void onResume() {
		initState(beacon);
	/*	soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				//	playRate = (float) (1/ beacon.getAccuracy());
				//	Log.d(TAG, "onLoadComplete: Rate"+playRate);
				//			soundPool.play(soundId1,1.0f,1.0f,2,-1,1);
			}
		});
*/
		super.onResume();
	}


	public void onStop() {
		player2.stop();
		player2.release();
		//player2 = null;
		super.onStop();
	}
	private void initState(Beacon beacon) {
		if (beacon == null) {
			switchButton.setChecked(false);
		}
		String key = activity.getKey(beacon);
		boolean state = activity.sharedPreferences.getBoolean(key, false);
		switchButton.setChecked(state);
		//switchButton.setOnCheckedChangeListener(this);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case android.R.id.home:
				activity.onBackPressed();
				soundPool.release();
			//	player1.stop();

				break;

			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

/*	TimerTask task1 = new TimerTask() {
		@Override
		public void run() {
			player1.start();
			if (mAccel > 0 && mAccel < 2) {
//				Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 1.", Toast.LENGTH_SHORT);
//				toast.show();
				//		soundPool.play(soundId1,1.0f,1.0f,2,-1,1);
			}
			else if ( mAccel > 2) {
//				Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 2.", Toast.LENGTH_SHORT);
//				toast.show();
				//		soundPool.play(soundId2,1.0f,1.0f,2,-1,1);
			}

		}
	};
*/
	TimerTask task2 = new TimerTask() {
		@Override
		public void run() {
			//player2.start();
			if (mAccel < 2) {
				//		Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 1.", Toast.LENGTH_SHORT);
				//		toast.show();
				//		soundPool.play(soundId1,1.0f,1.0f,2,-1,1);
				player2.start();
			}
			else if (mAccel > 2) {
				//		Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Device 1.", Toast.LENGTH_SHORT);
				//		toast.show();
				//		soundPool.play(soundId1,1.0f,1.0f,2,-1,1);
				player1.start();
			}


		}
	};
}
