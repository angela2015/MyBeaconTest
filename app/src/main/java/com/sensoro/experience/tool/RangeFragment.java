package com.sensoro.experience.tool;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.sensoro.beacon.kit.Beacon;
import com.sensoro.beacon.kit.Beacon.Proximity;
import com.sensoro.experience.tool.MainActivity.OnBeaconChangeListener;

import java.util.ArrayList;

/*
 * The range of the beacon.
 */
public class RangeFragment extends Fragment implements OnBeaconChangeListener {

	Beacon beacon;
	MainActivity activity;
	RelativeLayout immediateLayout;
	RelativeLayout nearLayout;
	RelativeLayout farLayout;
	TTFIcon userIcon;
	SoundPool soundPool;
	float playRate;
	String TAG="RangeFragmentTAG";
	int[] immediatePostion;
	int[] nearPosition;
	int[] farPosition;
	int[] unknowPosition;
	int soundId;
	boolean isPlayed;
	private SpeechSynthesizer mTts;
	private Toast mToast;
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	TranslateAnimation animation;
	Beacon.Proximity preProximity;


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
		super.onAttach(activity);
		mTts = SpeechSynthesizer.createSynthesizer(getActivity(), mTtsInitListener);
		mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
		mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速
		mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");
		Log.d(TAG, "onAttach: "+activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView: ");
		return inflater.inflate(R.layout.fragment_range, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		initCtrl();
		setTitle();
		Log.d(TAG, "onActivityCreated: "+getActivity());
		soundPool=new SoundPool(1, AudioManager.STREAM_MUSIC,0);
		soundId = soundPool.load(getActivity(), R.raw.slow,1);
		isPlayed=false;
		super.onActivityCreated(savedInstanceState);
	}



	private void setTitle() {
		activity.setTitle(R.string.back);
	}

	private void updateView(Beacon beacon) {
		if (beacon == null) {
			return;
		}
		changePos(beacon.getProximity());
	}

	@Override
	public void onDestroyView() {
		soundPool.release();
		super.onDestroyView();

	}

	private void changePos(Proximity proximity) {

		//playRate = (float) (1/ beacon.getAccuracy());
		if(beacon.getAccuracy()>4.0f){
			playRate=0.5f;
		}else if(beacon.getAccuracy()>=2.0f&&beacon.getAccuracy()<=4.0f){
			playRate=1.5f;
		}
		/*if(isPlayed==true) {
			soundPool.pause(soundId);
			isPlayed=false;
		}*/

		if (beacon == null) {
			return;
		}
		if (beacon.getProximity() == proximity) {
			return;
		}

		float fromX = userIcon.getX();
		float fromY = userIcon.getY();
		float toX = 0;
		float toY = 0;

			if (proximity == Proximity.PROXIMITY_IMMEDIATE) {
				toX = immediatePostion[0];
				toY = immediatePostion[1];
				if(preProximity==Proximity.PROXIMITY_NEAR && isPlayed==false)
				{
					soundPool.autoPause();
					mTts.startSpeaking(getString(R.string.tts_text), mTtsListener);
					isPlayed=true;
				}

			} else if (proximity == Proximity.PROXIMITY_NEAR) {
				toX = nearPosition[0];
				toY = nearPosition[1];
				if(preProximity==Proximity.PROXIMITY_IMMEDIATE){
					if(mTts.isSpeaking()){
						soundPool.pause(soundId);
					}
					else{
						soundPool.pause(soundId);
						mTts.startSpeaking(getString(R.string.tts_bye), mTtsListener);
					}

				}else {
					soundPool.play(soundId, 1.0f, 1.0f, 1, -1, playRate);
				}

			} else if (proximity == Proximity.PROXIMITY_FAR) {
				toX = farPosition[0];
				toY = farPosition[1];
				soundPool.play(soundId, 1.0f, 1.0f, 1, -1, playRate);
			}

		ObjectAnimator animator = ObjectAnimator.ofFloat(userIcon, "translationY", fromY, toY);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setDuration(500);
		ObjectAnimator.setFrameDelay(200);

		animator.addListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
			}

			@Override
			public void onAnimationCancel(Animator animation) {
	}
		});
		animator.start();

	}

	private void initUserPos() {
		if (beacon == null) {
			userIcon.setX(unknowPosition[0]);
			userIcon.setY(unknowPosition[1]);
			// userIcon.setX(immediatePostion[0]);
			// userIcon.setY(immediatePostion[1]);
		} else if (beacon.getProximity() == Proximity.PROXIMITY_IMMEDIATE) {
			userIcon.setX(immediatePostion[0]);
			userIcon.setY(immediatePostion[1]);
		} else if (beacon.getProximity() == Proximity.PROXIMITY_NEAR) {
			userIcon.setX(nearPosition[0]);
			userIcon.setY(nearPosition[1]);
			// userIcon.setX(immediatePostion[0]);
			// userIcon.setY(immediatePostion[1]);
		} else if (beacon.getProximity() == Proximity.PROXIMITY_FAR) {
			userIcon.setX(farPosition[0]);
			userIcon.setY(farPosition[1]);
			// userIcon.setX(immediatePostion[0]);
			// userIcon.setY(immediatePostion[1]);
		}
	}

	private void initCtrl() {
		beacon = (Beacon) getArguments().get(MainActivity.BEACON);
		activity = (MainActivity) getActivity();
		setHasOptionsMenu(true);

		immediateLayout = (RelativeLayout) activity.findViewById(R.id.fragment_range_immediate);
		nearLayout = (RelativeLayout) activity.findViewById(R.id.fragment_range_near);
		farLayout = (RelativeLayout) activity.findViewById(R.id.fragment_range_far);
		userIcon = (TTFIcon) activity.findViewById(R.id.fragment_range_iv);

		initCircle();
	}

	private void initCircle() {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int wid = metrics.widthPixels;
		int height = metrics.heightPixels;

		int length = (height > wid) ? wid : height;

		int radius = length / 8;
		int curLength = radius * 2;
		int curRadius = radius;
		LayoutParams params = new LayoutParams(curLength, curLength);
		immediateLayout.setLayoutParams(params);
		immediateLayout.setX(wid / 2 - curRadius);
		immediateLayout.setY(height / 2 - curRadius);

		curLength = 4 * radius;
		curRadius = 2 * radius;

		params = new LayoutParams(curLength, curLength);
		nearLayout.setLayoutParams(params);
		nearLayout.setX(wid / 2 - curRadius);
		nearLayout.setY(height / 2 - curRadius);

		curLength = 6 * radius;
		curRadius = 3 * radius;
		params = new LayoutParams(curLength, curLength);
		farLayout.setLayoutParams(params);
		farLayout.setX(wid / 2 - curRadius);
		farLayout.setY(height / 2 - curRadius);

		int userRaduis = radius / 5 * 2;
		params = new LayoutParams(userRaduis * 2, userRaduis * 2);
		userIcon.setLayoutParams(params);

		initPosition(wid, height);

		initUserPos();

		// immediateLayout.setVisibility(View.GONE);
		// nearLayout.setVisibility(View.GONE);
		// farLayout.setVisibility(View.GONE);

	}

	private void initPosition(int wid, int height) {

		int length = (height > wid) ? wid : height;

		int radius = length / 8;
		int userRaduis = radius / 5 * 2;

		immediatePostion = new int[2];
		immediatePostion[0] = wid / 2 - userRaduis;
		immediatePostion[1] = height / 2 - userRaduis;

		nearPosition = new int[2];
		nearPosition[0] = wid / 2 - userRaduis;
		nearPosition[1] = (int) (height / 2 - userRaduis + 1.5 * radius);

		farPosition = new int[2];
		farPosition[0] = wid / 2 - userRaduis;
		farPosition[1] = (int) (height / 2 - userRaduis + 2.5 * radius);

		unknowPosition = new int[2];
		unknowPosition[0] = wid / 2 - userRaduis;
		unknowPosition[1] = (int) (height / 2 - userRaduis + 4 * radius);
	}

	@Override
	public void onResume() {
		if(beacon.getProximity()==Proximity.PROXIMITY_NEAR){
			playRate=0.5f;
		}else if(beacon.getProximity()==Proximity.PROXIMITY_FAR){
			playRate=1.5f;
		}

		playRate = (float) (1/ beacon.getAccuracy());
		preProximity=beacon.getProximity();
		updateView(beacon);
		registerBeaconChangeListener();
		soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				Log.d(TAG, "onLoadComplete: " + sampleId + " status" + status);
				switch (beacon.getProximity())
				{
					case PROXIMITY_FAR:
						break;
					case PROXIMITY_NEAR:
						soundPool.play(soundId, 1.0f, 1.0f, 1, -1, playRate);
						break;
					case PROXIMITY_IMMEDIATE:
						mTts.startSpeaking(getString(R.string.tts_text), mTtsListener);
						isPlayed=true;
						break;
					default:
						break;
				}

			}
		});
		super.onResume();
	}

	@Override
	public void onStop() {
		unregisterBeaconChangeListener();
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
		for (Beacon beacon : beacons) {
			if (beacon.getSerialNumber() != null && beacon.getSerialNumber().equals(this.beacon.getSerialNumber())) {
				updateView(beacon);
				this.beacon = beacon;
				break;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case android.R.id.home:
			activity.onBackPressed();
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
