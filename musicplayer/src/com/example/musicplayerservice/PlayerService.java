package com.example.musicplayerservice;

import java.util.List;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

import com.example.application.MApplication;
import com.example.fileinfo.FileInfo;
import com.example.utils.AppConstant;

@SuppressLint({ "NewApi", "HandlerLeak" })
public class PlayerService extends Service {
	private MediaPlayer mediaPlayer; // 媒体播放器对象	
	private String path; 			// 音乐文件路径
	private int msg;
	private boolean isPause; 		// 暂停状态
	private int current = 0; 		// 记录当前正在播放的音乐
	private List<FileInfo> mp3Infos;	//存放Mp3Info对象的集合
	private int status =2;			//播放状态，默认为全部循环
	private MyReceiver myReceiver;	//自定义广播接收器
	private int currentTime=0;		//当前播放进度
	
	MApplication appState;
	
	//服务要发送的一些Action
	public static final String UPDATE_ACTION = "com.example.musicplayerservice.UPDATE_ACTION";  //更新动作  
    public static final String CTL_ACTION = "com.example.musicplayerservice.CTL_ACTION";        //控制动作  
    public static final String MUSIC_CURRENT = "com.example.musicplayerservice.MUSIC_CURRENT";  //音乐当前时间改变动作  
    public static final String MUSIC_PREVIOUE = "com.example.musicplayerservice.MUSIC_PREVIOUE";  //上一首广播  
    public static final String MUSIC_PLAY_PAUSE = "com.example.musicplayerservice.MUSIC_PLAY_PAUSE";  //播放暂停广播         
    public static final String MUSIC_NEXT = "com.example.musicplayerservice.MUSIC_NEXT";  //下一首广播 
    public static final String MUSIC_PLAY_STATE = "com.example.musicplayerservice.MUSIC_PLAY_STATE";  //音乐播放状态信息广播
    //  public static final String MUSIC_DURATION = "com.example.musicplayerservice.MUSIC_DURATION";//音乐时长  
//    public static final String MUSIC_PLAYING = "com.example.musicplayerservice.MUSIC_PLAYING";  //音乐正在播放动作  
//    public static final String REPEAT_ACTION = "com.example.musicplayerservice.REPEAT_ACTION";  //音乐重复播放动作  
//    public static final String SHUFFLE_ACTION = "com.example.musicplayerservice.SHUFFLE_ACTION";//音乐随机播放动作  
  	/**
	 * handler用来接收消息，来发送广播更新播放时间
	 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 2) {
				if(mediaPlayer != null) {
					currentTime = mediaPlayer.getCurrentPosition(); // 获取当前音乐播放的位置
					Intent intent = new Intent();
					intent.setAction(MUSIC_CURRENT);
					intent.putExtra("currentTime", currentTime);
					sendBroadcast(intent); 
					handler.sendEmptyMessageDelayed(2, 1000);
				}				
			}
		};
	};

	@Override
	public void onCreate() {
		super.onCreate();		
		mediaPlayer =new MediaPlayer();
		appState = ((MApplication)getApplicationContext());        
		mp3Infos = appState.getFileInfos();
		//mp3Infos = MediaUtil.getFileInfos(PlayerService.this);
		
		//设置音乐播放完成时的监听器
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				if (status == 1) { // 单曲循环					
					play(0);
				} 
				else if (status == 2) { // 全部循环
					current++;
					if(current > mp3Infos.size() - 1) {	//变为第一首的位置继续播放
						current = 0;
					}
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);					
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				} 
				else if(status == 3) {	//随机播放
					current = getRandomIndex(mp3Infos.size() - 1);					
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);					
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}
				
			}
		});
		
		 //为多媒体数据库数据改变注册监听器
		 getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				 true, new MusicDataObserver(new Handler()));
		
        //广播接收器只接收CTL_ACTION控制动作
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//媒体库更新
		registerReceiver(myReceiver, filter);
	}

	//获取随机位置
	protected int getRandomIndex(int end) {
		int index = (int) (Math.random() * end);
		return index;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){		
		Bundle bundle = intent.getExtras();
		path = bundle.getString("url");		//歌曲路径
		current = bundle.getInt("listPosition", -1);	//当前播放歌曲的在mp3Infos的位置
		msg = bundle.getInt("MSG");      //取出绑定在intent上的操作数值		
		if (msg == AppConstant.PlayerMsg.PLAY_MSG) {	//直接播放音乐
			play(0);
		} else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {	//暂停
			pause();	
		} else if (msg == AppConstant.PlayerMsg.STOP_MSG) {		//停止
			stop();
		} else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) {	//继续播放
			resume();	
		} else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) {	//进度更新
			currentTime = intent.getIntExtra("progress", -1);			
			play(currentTime);
		} else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) { //正在播放
			handler.sendEmptyMessage(2);
		}
		Intent sendIntent = new Intent(UPDATE_ACTION);
		sendIntent.putExtra("current", current);					
		sendBroadcast(sendIntent);
		return super.onStartCommand(intent, flags, startId); 
	}

	//播放音乐
	private void play(int currentTime) {
		try {
			mediaPlayer.reset();// 把各项参数恢复到初始状态
			mediaPlayer.setDataSource(path);			
			mediaPlayer.prepare(); // 进行缓冲		
			mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器		
			handler.sendEmptyMessage(2);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    //暂停音乐
	private void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPause = true;
		}
	}
    //恢复播放
	private void resume() {
		if (isPause) {
			mediaPlayer.start();
			isPause = false;
		}
	}
	
	//停止音乐
	private void stop() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onDestroy() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		
	}

	//实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
	private final class PreparedListener implements OnPreparedListener {
		private int currentTime;
		public PreparedListener(int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start(); // 开始播放
			if (currentTime > 0) { // 如果音乐不是从头播放
				mediaPlayer.seekTo(currentTime);
			}			
			//Intent intent = new Intent();
			//intent.setAction(MUSIC_DURATION);
			//int duration = mediaPlayer.getDuration();
			//intent.putExtra("duration", duration);	//通过Intent来传递歌曲的总长度
			//sendBroadcast(intent);
		}
	}
    //接收PlayerActivity活动组件发送的广播
	public class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction(); 
			if(action.equals(PlayerService.CTL_ACTION)){
			    int playOrder = intent.getIntExtra("playOrder", -1);
			    switch (playOrder) {
			             case 1:   status = 1; // 将播放状态置为1表示：单曲循环
				                      break;
			             case 2:   status = 2;	//将播放状态置为2表示：全部循环
				                      break;
			             case 3:   status = 3;	//将播放状态置为3表示：随机播放
				                      break;			             
			   }
			}	
			else if(action.equals(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)){						
				mp3Infos = appState.getFileInfos();				
			}
		}
	}
	
	private final class MusicDataObserver extends ContentObserver{
		public MusicDataObserver(Handler handler) {
			super(handler);			
		}
		@Override
		public void onChange(boolean selfChange) {
			mp3Infos = appState.getFileInfos();	
		}
    	
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
