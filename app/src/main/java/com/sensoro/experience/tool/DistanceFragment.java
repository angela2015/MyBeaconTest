package com.sensoro.experience.tool;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.thirdparty.J;
import com.sensoro.beacon.kit.Beacon;
import com.sensoro.experience.tool.MainActivity.OnBeaconChangeListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechUnderstander;

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
	boolean isBye;
	boolean isPlaying;
	private SpeechSynthesizer mTts;
	private Toast mToast;
	// 缓冲进度
	private int mPercentForBuffering = 0;
	// 播放进度
	private int mPercentForPlaying = 0;
	private float preDistance=0;
	private float curDistance=0;
	private int process;

	@Override
	public void onPause() {
		if(mTts.isSpeaking()){
			mTts.stopSpeaking();
		}
		if(mSpeechUnderstander.isUnderstanding()){// 开始前检查状态
			mSpeechUnderstander.stopUnderstanding();
			showTip("停止录音");
		}
		super.onPause();
	}

	//语义理解
	private SpeechUnderstander mSpeechUnderstander;
	private SharedPreferences mSharedPreferences;
	private EditText mUnderstanderText;

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
			isPlaying=true;
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
				if(isPlayed==false)
				{
					isPlayed=true;
				}
			} else if (error != null) {
				showTip(error.getPlainDescription(true));
			}
			isPlaying=false;
			ret = mSpeechUnderstander.startUnderstanding(mSpeechUnderstanderListener);
			if(ret != 0){
				showTip("语义理解失败,错误码:"	+ ret);
			}else {
				showTip("请开始说话！");
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

	private SpeechUnderstanderListener mSpeechUnderstanderListener = new SpeechUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			if (null != result) {
				Log.d(TAG, result.getResultString());

				// 显示
				String text = result.getResultString();
				if (!TextUtils.isEmpty(text)) {
					mUnderstanderText.setText(text);
					JsonElement rootEle = new JsonParser().parse(text);
					JsonObject jsonObject = rootEle.getAsJsonObject().getAsJsonObject("answer");
					JsonArray jsonArr = rootEle.getAsJsonObject().getAsJsonArray("moreResults");
					if(jsonObject!=null){
						showTip(jsonObject.get("text").getAsString());
						Log.d(TAG, "onResult: jsonObject: "+jsonObject.get("text").getAsString());
						mTts.startSpeaking(jsonObject.get("text").getAsString(),mTtsListener);
					}else  if(jsonArr!=null){
						List<String> rsList = new ArrayList<String>(jsonArr.size());
						for(JsonElement objElem:jsonArr){
							JsonObject jObj = objElem.getAsJsonObject().getAsJsonObject("answer");
							rsList.add(jObj.get("text").getAsString());
						}
						for(String s: rsList){
							Log.d(TAG, "onResult: rsList:"+s);
						}
					}else if(jsonObject==null)
					{
						mTts.startSpeaking("Sorry,I don't understand",mTtsListener);
					}



					/*InputStream json1   =   new ByteArrayInputStream(text.getBytes());
					JsonReader reader = new JsonReader(new InputStreamReader(json1));
					try{
						reader.beginObject();
						while(reader.hasNext()){
							String keyName = reader.nextName();
							if("answer".equals(keyName)){
								String anStr = reader.nextString();
								InputStream json2  =   new ByteArrayInputStream(anStr.getBytes());
								JsonReader reader2 = new JsonReader(new InputStreamReader(json2));
								reader2.beginObject();
								while (reader2.hasNext()){
									if("text".equals(reader2.nextName())){
										showTip(reader2.nextString());
									}
									reader2.endObject();
									reader2.close();
								}

							}
						}
						reader.endObject();
					} catch (IOException e) {
						e.printStackTrace();
					} finally{
						try {
							reader.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}*/

				}
			} else {
				showTip("识别结果不正确。");
				mTts.startSpeaking("Sorry,I don't understand",mTtsListener);

			}
		}

		@Override
		public void onVolumeChanged(int volume, byte[] data) {
			showTip("当前正在说话，音量大小：" + volume);
			Log.d(TAG, data.length+"");
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
		}

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			Log.e(TAG, "onError: " );
			showTip(error.getPlainDescription(true));
			mTts.startSpeaking("I didn't catch what you said",mTtsListener);
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};
	private int ret;


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
		soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC,0);
		soundId1 = soundPool.load(getActivity(),R.raw.slow,1);
		//soundId2 = soundPool.load(getActivity(),R.raw.music,2);

		isPlayed =false;
		isBye = false;
		isPlaying = false;

		mTts = SpeechSynthesizer.createSynthesizer(getActivity(), mTtsInitListener);
		mSpeechUnderstander = SpeechUnderstander.createUnderstander(getActivity(), null);
		/*mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");*/
		mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("understander_vadbos_preference", "4000"));

		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("understander_vadeos_preference", "1000"));

		// 设置标点符号，默认：1（有标点）
		//mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("understander_punc_preference", "1"));

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		//mSpeechUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		//mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");

		mTts.setParameter(SpeechConstant.VOICE_NAME, "catherine"); //设置发音人
		mTts.setParameter(SpeechConstant.SPEED, "50");//设置语速
		mTts.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
		mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端
		mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");


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
		if((Math.abs(preDistance-curDistance))>5.0f)
			return;
		//走近
		if(preDistance-curDistance>0){
			if(curDistance>5.0f){
				process=1;
			}else if(curDistance<=5.0f&&curDistance>=2.0f){
				process=2;
			}else if(curDistance<2.f){
				process=3;
			}
		}//远离
		else if(preDistance-curDistance<0){
			if(curDistance>5.0f){
				process=5;
			}else if(curDistance<=5.0f&&curDistance>=2.0f){
				process=4;
			}else if(curDistance<2.0f){
				process=3;
			}
		}else if(preDistance-curDistance==0){

		}
		playRate = (float) (1/ beacon.getAccuracy());
		switch (process)
		{
			case 1:
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
				break;
			case 2:
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
				break;
			case 3:
				if(mTts.isSpeaking())
					break;
				else{
					if(isPlayed==false){
						soundPool.stop(soundId1);

						mTts.startSpeaking(getString(R.string.tts_intro),mTtsListener);

						//mTts.startSpeaking(getString(R.string.tts_text), mTtsListener);

					}else if(isPlayed==true){
						soundPool.stop(soundId1);
						if(mSpeechUnderstander.isUnderstanding())
							break;
						else if(!mSpeechUnderstander.isUnderstanding()){
							ret = mSpeechUnderstander.startUnderstanding(mSpeechUnderstanderListener);
							if(ret != 0){
								showTip("语义理解失败,错误码:"	+ ret);
							}else {
								showTip("请开始说话！");
							}
						}
						/*if(mSpeechUnderstander.isUnderstanding()){// 开始前检查状态
							mSpeechUnderstander.stopUnderstanding();
							showTip("停止录音");
						}else {
							ret = mSpeechUnderstander.startUnderstanding(mSpeechUnderstanderListener);
							if(ret != 0){
								showTip("语义理解失败,错误码:"	+ ret);
							}else {
								showTip("请开始说话！");
							}
						}*/
					}
				}

				break;
			case 4:
				if(mTts.isSpeaking()){
					break;
				}else if(isBye==false){
					soundPool.stop(soundId1);
					mTts.stopSpeaking();
					mTts.startSpeaking(getString(R.string.tts_bye),mTtsListener);
					isBye=true;
					break;
				}else if(isBye==true){
					soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
				}

			case 5:
				soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
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
		mUnderstanderText = (EditText)activity.findViewById(R.id.understander_text);
		mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
		mSharedPreferences = getActivity().getSharedPreferences("com.iflytek.setting", Activity.MODE_PRIVATE);

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
				//soundPool.play(soundId1,1.0f,1.0f,1,-1,playRate);
			}
		});
		super.onResume();
	}

	@Override
	public void onStop() {
		unregisterBeaconChangeListener();
		soundPool.autoPause();
		if(mTts.isSpeaking()){
			mTts.stopSpeaking();
		}
		if(mSpeechUnderstander.isUnderstanding()){// 开始前检查状态
			mSpeechUnderstander.stopUnderstanding();
			showTip("停止录音");
		}
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
