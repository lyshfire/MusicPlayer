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
	private MediaPlayer mediaPlayer; // ý�岥��������	
	private String path; 			// �����ļ�·��
	private int msg;
	private boolean isPause; 		// ��ͣ״̬
	private int current = 0; 		// ��¼��ǰ���ڲ��ŵ�����
	private List<FileInfo> mp3Infos;	//���Mp3Info����ļ���
	private int status =2;			//����״̬��Ĭ��Ϊȫ��ѭ��
	private MyReceiver myReceiver;	//�Զ���㲥������
	private int currentTime=0;		//��ǰ���Ž���
	
	MApplication appState;
	
	//����Ҫ���͵�һЩAction
	public static final String UPDATE_ACTION = "com.example.musicplayerservice.UPDATE_ACTION";  //���¶���  
    public static final String CTL_ACTION = "com.example.musicplayerservice.CTL_ACTION";        //���ƶ���  
    public static final String MUSIC_CURRENT = "com.example.musicplayerservice.MUSIC_CURRENT";  //���ֵ�ǰʱ��ı䶯��  
    public static final String MUSIC_PREVIOUE = "com.example.musicplayerservice.MUSIC_PREVIOUE";  //��һ�׹㲥  
    public static final String MUSIC_PLAY_PAUSE = "com.example.musicplayerservice.MUSIC_PLAY_PAUSE";  //������ͣ�㲥         
    public static final String MUSIC_NEXT = "com.example.musicplayerservice.MUSIC_NEXT";  //��һ�׹㲥 
    public static final String MUSIC_PLAY_STATE = "com.example.musicplayerservice.MUSIC_PLAY_STATE";  //���ֲ���״̬��Ϣ�㲥
    //  public static final String MUSIC_DURATION = "com.example.musicplayerservice.MUSIC_DURATION";//����ʱ��  
//    public static final String MUSIC_PLAYING = "com.example.musicplayerservice.MUSIC_PLAYING";  //�������ڲ��Ŷ���  
//    public static final String REPEAT_ACTION = "com.example.musicplayerservice.REPEAT_ACTION";  //�����ظ����Ŷ���  
//    public static final String SHUFFLE_ACTION = "com.example.musicplayerservice.SHUFFLE_ACTION";//����������Ŷ���  
  	/**
	 * handler����������Ϣ�������͹㲥���²���ʱ��
	 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 2) {
				if(mediaPlayer != null) {
					currentTime = mediaPlayer.getCurrentPosition(); // ��ȡ��ǰ���ֲ��ŵ�λ��
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
		
		//�������ֲ������ʱ�ļ�����
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				if (status == 1) { // ����ѭ��					
					play(0);
				} 
				else if (status == 2) { // ȫ��ѭ��
					current++;
					if(current > mp3Infos.size() - 1) {	//��Ϊ��һ�׵�λ�ü�������
						current = 0;
					}
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);					
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				} 
				else if(status == 3) {	//�������
					current = getRandomIndex(mp3Infos.size() - 1);					
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);					
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}
				
			}
		});
		
		 //Ϊ��ý�����ݿ����ݸı�ע�������
		 getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				 true, new MusicDataObserver(new Handler()));
		
        //�㲥������ֻ����CTL_ACTION���ƶ���
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);//ý������
		registerReceiver(myReceiver, filter);
	}

	//��ȡ���λ��
	protected int getRandomIndex(int end) {
		int index = (int) (Math.random() * end);
		return index;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){		
		Bundle bundle = intent.getExtras();
		path = bundle.getString("url");		//����·��
		current = bundle.getInt("listPosition", -1);	//��ǰ���Ÿ�������mp3Infos��λ��
		msg = bundle.getInt("MSG");      //ȡ������intent�ϵĲ�����ֵ		
		if (msg == AppConstant.PlayerMsg.PLAY_MSG) {	//ֱ�Ӳ�������
			play(0);
		} else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) {	//��ͣ
			pause();	
		} else if (msg == AppConstant.PlayerMsg.STOP_MSG) {		//ֹͣ
			stop();
		} else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) {	//��������
			resume();	
		} else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) {	//���ȸ���
			currentTime = intent.getIntExtra("progress", -1);			
			play(currentTime);
		} else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) { //���ڲ���
			handler.sendEmptyMessage(2);
		}
		Intent sendIntent = new Intent(UPDATE_ACTION);
		sendIntent.putExtra("current", current);					
		sendBroadcast(sendIntent);
		return super.onStartCommand(intent, flags, startId); 
	}

	//��������
	private void play(int currentTime) {
		try {
			mediaPlayer.reset();// �Ѹ�������ָ�����ʼ״̬
			mediaPlayer.setDataSource(path);			
			mediaPlayer.prepare(); // ���л���		
			mediaPlayer.setOnPreparedListener(new PreparedListener(currentTime));// ע��һ��������		
			handler.sendEmptyMessage(2);	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    //��ͣ����
	private void pause() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPause = true;
		}
	}
    //�ָ�����
	private void resume() {
		if (isPause) {
			mediaPlayer.start();
			isPause = false;
		}
	}
	
	//ֹͣ����
	private void stop() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare(); // �ڵ���stop�������Ҫ�ٴ�ͨ��start���в���,��Ҫ֮ǰ����prepare����
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

	//ʵ��һ��OnPrepareLister�ӿ�,������׼���õ�ʱ��ʼ����
	private final class PreparedListener implements OnPreparedListener {
		private int currentTime;
		public PreparedListener(int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start(); // ��ʼ����
			if (currentTime > 0) { // ������ֲ��Ǵ�ͷ����
				mediaPlayer.seekTo(currentTime);
			}			
			//Intent intent = new Intent();
			//intent.setAction(MUSIC_DURATION);
			//int duration = mediaPlayer.getDuration();
			//intent.putExtra("duration", duration);	//ͨ��Intent�����ݸ������ܳ���
			//sendBroadcast(intent);
		}
	}
    //����PlayerActivity�������͵Ĺ㲥
	public class MyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction(); 
			if(action.equals(PlayerService.CTL_ACTION)){
			    int playOrder = intent.getIntExtra("playOrder", -1);
			    switch (playOrder) {
			             case 1:   status = 1; // ������״̬��Ϊ1��ʾ������ѭ��
				                      break;
			             case 2:   status = 2;	//������״̬��Ϊ2��ʾ��ȫ��ѭ��
				                      break;
			             case 3:   status = 3;	//������״̬��Ϊ3��ʾ���������
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
