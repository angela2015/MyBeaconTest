package com.sensoro.experience.tool;

import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sensoro.beacon.kit.Beacon;
import com.sensoro.experience.tool.MainActivity.OnBeaconChangeListener;

import java.text.DecimalFormat;
import java.util.ArrayList;

/*
 * Distance to the beacon.
 */
public class DistanceFragment extends Fragment implements OnBeaconChangeListener {

	private static final String TAG ="DistanceFragment" ;
	Beacon beacon;
	TextView distanceTextView;
	MainActivity activity;
	SoundPool soundPool;
	int soundId1,soundId2;
	float playRate;
	boolean isPlayed;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_distance, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		initCtrl();
		setTitle();
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC,0);
		soundId1 = soundPool.load(getActivity(),R.raw.slow,1);
		soundId2 = soundPool.load(getActivity(),R.raw.music,2);
		isPlayed =false;

		super.onActivityCreated(savedInstanceState);
	}

	private void setTitle() {
		activity.setTitle(R.string.back);
	}

	private void updateView(Beacon beacon) {
		if (beacon == null) {
			return;
		}
		playRate = (float) (1/ beacon.getAccuracy());
		if(playRate > 2.5f){
			if (isPlayed == false) {
				soundPool.stop(soundId1);
				soundPool.play(soundId2,1.0f,1.0f,2,-1,1);
			}
			else {
				soundPool.stop(soundId1);
				soundPool.resume(soundId2);
			}
		}
 		else if(playRate < 2.5f){
			soundPool.pause(soundId2);
			soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
		}

		DecimalFormat format = new DecimalFormat("#");
		String distance = format.format(beacon.getAccuracy() * 100);
		distanceTextView.setText(distance + " cm");
		Log.d(TAG, "updateView: distance:"+distance+" playRate:"+playRate);
	}

	private void initCtrl() {
		beacon = (Beacon) getArguments().get(MainActivity.BEACON);
		activity = (MainActivity) getActivity();
		setHasOptionsMenu(true);
		distanceTextView = (TextView) activity.findViewById(R.id.fragment_distance_tv);

	}

	@Override
	public void onResume() {
		updateView(beacon);
		registerBeaconChangeListener();
		soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				playRate = (float) (1/ beacon.getAccuracy());
				Log.d(TAG, "onLoadComplete: Rate"+playRate);
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
			}
		});
		super.onResume();
	}

	@Override
	public void onStop() {
		unregisterBeaconChangeListener();
		soundPool.autoPause();
		super.onStop();
	}

	/*
	 * Register beacon change listener.
	 */
	private void registerBeaconChangeListener() {
		activity.registerBeaconChangerListener(this);
	}

	/*
	 * Register beacon change listener.
	 */
	private void unregisterBeaconChangeListener() {
		activity.unregisterBeaconChangerListener(this);
	}

	@Override
	public void onBeaconChange(ArrayList<Beacon> beacons) {
		//boolean exist = false;		//这一次找不到不代表Beacon已经离开区域, by caisenchuan
		for (Beacon beacon : beacons) {
			if (beacon.getSerialNumber() != null && beacon.getSerialNumber().equals(this.beacon.getSerialNumber())) {
				this.beacon = beacon;
				updateView(beacon);
				//exist = true;
				break;
			}
		}
		/*if (!exist) {
			distanceTextView.setText(R.string.disappear);
		}*/
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			activity.onBackPressed();
			soundPool.release();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
