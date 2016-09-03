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
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.sensoro.beacon.kit.Beacon;
import com.sensoro.experience.tool.MainActivity.OnBeaconChangeListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.InitListener;

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
	private SpeechSynthesizer mTts;
	private Toast mToast;
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	private float preDistance=0;
	private float curDistance=0;
	private int process;

	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败,错误码："+code);
			} else {
				// 初始化成功，之后可以调用startSpeaking方法
				// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
				// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

	private SynthesizerListener mTtsListener = new SynthesizerListener() {

		@Override
		public void onSpeakBegin() {
			showTip("开始播放");
		}

		@Override
		public void onSpeakPaused() {
			showTip("暂停播放");
		}

		@Override
		public void onSpeakResumed() {
			showTip("继续播放");
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos,
									 String info) {
			// 合成进度
			mPercentForBuffering = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			// 播放进度
			mPercentForPlaying = percent;
			showTip(String.format(getString(R.string.tts_toast_format),
					mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error == null) {
				showTip("播放完成");
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

	@Override
	public void onAttach(Activity activity) {

		mTts = SpeechSynthesizer.createSynthesizer(getActivity(), mTtsInitListener);
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
		mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速
		mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");
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
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
		soundId1 = soundPool.load(getActivity(),R.raw.slow,1);
		//soundId2 = soundPool.load(getActivity(),R.raw.music,2);
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
		curDistance= (float) beacon.getAccuracy();
		//走近
		if(preDistance-curDistance>0){
			if(curDistance>2.30f){
				process=1;
			}else if(curDistance<=2.30f&&curDistance>=1.80f){
				process=2;
			}else if(curDistance<1.80f){
				process=3;
			}
		}//远离
		else if(preDistance-curDistance<0){
			if(curDistance>2.30f){
				process=5;
			}else if(curDistance<=2.30f&&curDistance>=1.80f){
				process=4;
			}else if(curDistance<1.80f){
				process=3;
			}
		}else if(preDistance-curDistance==0){

		}
		playRate = (float) (1/ beacon.getAccuracy());
		switch (process)
		{
			case 1:
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
//				soundPool.setRate(soundId1,playRate);
//				soundPool.autoResume();
				break;
			case 2:
				if(isPlayed==false){
					soundPool.autoPause();
					mTts.startSpeaking(getString(R.string.tts_text), mTtsListener);
					isPlayed=true;
				}else {
					soundPool.autoPause();
				}
				break;
			case 3:
				soundPool.autoPause();
				break;
			case 4:
				soundPool.autoPause();
				mTts.stopSpeaking();
				mTts.startSpeaking(getString(R.string.tts_bye),mTtsListener);
				break;
			case 5:
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
				//soundPool.setRate(soundId1,playRate);
				//soundPool.autoResume();
				break;
			default:
				break;

		}



		/*playRate = (float) (1/ beacon.getAccuracy());
		if(playRate > 2.5f){
			if (isPlayed == false) {

				//soundPool.play(soundId2,1.0f,1.0f,2,-1,1);
				mTts.startSpeaking(getString(R.string.tts_text), mTtsListener);
				isPlayed=true;
			}
			else {
				soundPool.stop(soundId1);
				//soundPool.resume(soundId2);
			}
		}
 		else if(playRate < 2.5f){

			//soundPool.pause(soundId2);
			soundPool.setRate(soundId1,playRate);
			soundPool.autoResume();
			*//*soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);*//*
		}*/

		DecimalFormat format = new DecimalFormat("#");
		String distance = format.format(beacon.getAccuracy() * 100);
		distanceTextView.setText(distance + " cm");
		Log.d(TAG, "updateView: isplayer:"+isPlayed+"distance:"+distance+" playRate:"+playRate+" process:"+process+" preDistance:"+preDistance+" curDistance:"+curDistance);
		preDistance = (float) beacon.getAccuracy();
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
		preDistance= (float) beacon.getAccuracy();
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





	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
}
