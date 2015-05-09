package com.example.musicplayer;

import java.util.ArrayList;
import java.util.List;

import com.example.application.MApplication;
import com.example.database.DBHelper;
import com.example.fileinfo.FileInfo;
import com.example.musicplayerservice.PlayerService;
import com.example.shake.ShakeEvent;
import com.example.shake.ShakeEvent.OnShakeListener;
import com.example.utils.AppConstant;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint({ "NewApi", "ClickableViewAccessibility" })
public class MainActivity extends Activity {
	private Button jumpplaying_bt;
	private Button localmusiclist_bt;
	private Button downloadmusiclist_bt;
	private Button lovemusiclist_bt;
	private Button nearmusiclist_bt;
	private Button music_search_bt;
	private Button btn_love;
	private TextView localmusiccount_tv;
	private TextView downloadmusiccount_tv;
	private TextView lovemusiccount_tv;
	private TextView nearmusiccount_tv;
	private TextView music_name_tv;
	private TextView music_siger_tv;
	private ToggleButton mainmusic_play_bt;
	private RelativeLayout bottombar_layout;
	private ImageView music_album_iv;
	
	private boolean isFirstTime = true;   
    private boolean isPlaying; // 正在播放  
    private boolean isPause; // 暂停 
    private int msg = -1;
    private boolean play_bt_press = false;
    private boolean play_bt_check = false;
	private List<FileInfo> fileInfos = new ArrayList<FileInfo>();  
	private int listPosition = 0;   //标识列表位置
	private PlayerReceiver playerReceiver; //接收PlayerService服务的广播接收器
	
	/***********图片动画**************/
	private ObjectAnimator anim;
	private ObjectAnimator animSongName;
	private ObjectAnimator animSongSinger;
	private LinearInterpolator lin; //旋转速率,匀速
	
	/************手势****************************/
	GestureDetector  myGestureDetector;
	
	/************摇晃切歌*******************************/
	private ShakeEvent  shakeEvent;
	
	private SharedPreferences preferences;
	private Editor editor;
	private MApplication appState;
	private  DBHelper helper;
	private  DBHelper helperFavorite;
	private  DBHelper helperDownload;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_main);		
		preferences = getSharedPreferences("songPosition", MODE_PRIVATE);
		listPosition = preferences.getInt("listPosition", 0);
		editor = preferences.edit();
		
		appState = ((MApplication)getApplicationContext());        
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		helperDownload = appState.getDBHelperDownload();
		fileInfos = appState.getFileInfos();

		
		findViewById();
		setViewOnclickListener();		
		initLocalMusicList();		
		
		playerReceiver = new PlayerReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(PlayerService.UPDATE_ACTION);    
        filter.addAction(PlayerService.MUSIC_PREVIOUE);
        filter.addAction(PlayerService.MUSIC_PLAY_PAUSE); 
        filter.addAction(PlayerService.MUSIC_NEXT); 
        filter.addAction(PlayerService.MUSIC_PLAY_STATE); 
        registerReceiver(playerReceiver, filter);         
	}	

	//从界面上根据id获取控件
    @SuppressWarnings("deprecation")
	private void findViewById() {  
		jumpplaying_bt = (Button)findViewById(R.id.jumpplaying_bt);
		localmusiclist_bt = (Button)findViewById(R.id.localmusiclist_bt);
		downloadmusiclist_bt = (Button)findViewById(R.id.downloadmusiclist_bt);
		lovemusiclist_bt = (Button)findViewById(R.id.lovemusiclist_bt);
		nearmusiclist_bt = (Button)findViewById(R.id.nearmusiclist_bt);
		music_search_bt = (Button)findViewById(R.id.music_search_bt);
		btn_love             = (Button) findViewById(R.id.btn_love);
		localmusiccount_tv = (TextView)findViewById(R.id.localmusiccount_tv);
		downloadmusiccount_tv = (TextView)findViewById(R.id.downloadmusiccount_tv);
		lovemusiccount_tv = (TextView)findViewById(R.id.lovemusiccount_tv);
		nearmusiccount_tv = (TextView)findViewById(R.id.nearmusiccount_tv);
		
		music_name_tv = (TextView)findViewById(R.id.music_name_tv);
    	music_siger_tv = (TextView)findViewById(R.id.music_siger_tv);  
    	mainmusic_play_bt = (ToggleButton)findViewById(R.id.mainmusic_play_bt);
    	music_album_iv = (ImageView)findViewById(R.id.music_album_iv);
    	
    	myGestureDetector = new GestureDetector(new myGestureListener());
    	bottombar_layout = (RelativeLayout)findViewById(R.id.bottombar_layout);    	
    	    	
	}   
  
    //给每一个按钮设置监听器
    private void setViewOnclickListener() {  
    	ViewOnClickListener viewOnClickListener = new ViewOnClickListener();  
		jumpplaying_bt.setOnClickListener(viewOnClickListener);
		localmusiclist_bt.setOnClickListener(viewOnClickListener);
		downloadmusiclist_bt.setOnClickListener(viewOnClickListener);
		lovemusiclist_bt.setOnClickListener(viewOnClickListener);
		nearmusiclist_bt.setOnClickListener(viewOnClickListener);
		music_search_bt.setOnClickListener(viewOnClickListener);
		mainmusic_play_bt.setOnClickListener(viewOnClickListener);
		music_album_iv.setOnClickListener(viewOnClickListener);
		btn_love.setOnClickListener(viewOnClickListener);
		
		bottombar_layout.setOnClickListener(viewOnClickListener);	
		bottombar_layout.setOnTouchListener(new OnTouchListener() {	
			//捕获bottombar_layout的触摸事件,并传递给GestureDetector
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				myGestureDetector.onTouchEvent(event);
				return false;
			}
		});
		shakeEvent = new ShakeEvent(this);
		shakeEvent.setOnShakeListener(new OnShakeEventListener());
		
	}
    private class myGestureListener  extends SimpleOnGestureListener{
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
    	//监听传递过来的触摸事件进行判断
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
    		if(e1.getX() - e2.getX()  > 50){
    	    	msg = AppConstant.PlayerMsg.PLAY_MSG;  
                isFirstTime = false;  
                isPlaying = true;  
                isPause = false;  
                listPosition = listPosition - 1;
                if(listPosition < 0){
                	listPosition = fileInfos.size() - 1;
                }	               
    		}else if(e2.getX() - e1.getX() > 50){
    			msg = AppConstant.PlayerMsg.PLAY_MSG;  
                isFirstTime = false;  
                isPlaying = true;  
                isPause = false;  
                listPosition = listPosition + 1;
                if(listPosition > fileInfos.size() - 1){
                	listPosition = 0;
                }
    		}
    		if (anim != null  &&  isPlaying == true) {  
    	        	if(!anim.isStarted()){
    	       	         anim.start();//开始旋转
    	       	    }else{
    	       		    anim.resume();//继续旋转
    	         	}
    	    	} 
    		 play_bt_press = false;
             play_bt_check = true;
    		mainmusic_play_bt.setPressed(play_bt_press);
        	mainmusic_play_bt.setChecked(play_bt_check);
    		FileInfo mp3Info = fileInfos.get(listPosition); 
    		music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
            music_name_tv.setText(fileInfos.get(listPosition).getTitle());
	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	 }    	       	         	        	             		 	
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
	    	intentToService.putExtras(bundle);//将bundle绑定到intent
	        startService(intentToService); 
    		return super.onFling(e1, e2, velocityX, velocityY);
    	}
    }
    /*****************摇晃切歌监听*******************************************/
    private class OnShakeEventListener implements OnShakeListener{
		Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onShake() {
        	msg = AppConstant.PlayerMsg.PLAY_MSG;  
            isFirstTime = false;  
            isPlaying = true;  
            isPause = false;  
            listPosition = listPosition + 1;
            if(listPosition > fileInfos.size() - 1){
            	listPosition = 0;
            }	
           if (anim != null  &&  isPlaying == true) {  
	        	if(!anim.isStarted()){
	       	         anim.start();//开始旋转
	       	    }else{
	       		    anim.resume();//继续旋转
	         	}
	    	} 
		 play_bt_press = false;
         play_bt_check = true;
		mainmusic_play_bt.setPressed(play_bt_press);
    	mainmusic_play_bt.setChecked(play_bt_check);
            music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
            music_name_tv.setText(fileInfos.get(listPosition).getTitle());
	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	 }    	       	        
	        FileInfo mp3Info = fileInfos.get(listPosition);  	
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
	    	intentToService.putExtras(bundle);//将bundle绑定到intent
	        startService(intentToService);   				
		}			
	}
    
    //自定义的监听器类
    private class ViewOnClickListener implements OnClickListener {    	
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onClick(View v) {
			switch (v.getId()) {  
			     case R.id.jumpplaying_bt :  { // 跳到播放界面			          
				         Intent intent = new Intent(MainActivity.this,PlayingActivity.class);	
				         Bundle toPlayingData = new Bundle();
				         toPlayingData.putInt("listPosition", listPosition);
				         toPlayingData.putBoolean("bt_press", false);
				         toPlayingData.putBoolean("bt_check",isPlaying);
				         toPlayingData.putBoolean("isFirstTime", isFirstTime);
				         toPlayingData.putBoolean("isPlaying", isPlaying);
				         toPlayingData.putBoolean("isPause", isPause);			         
				         toPlayingData.putInt("msg", msg);
				         intent.putExtras(toPlayingData);			             
				         startActivityForResult(intent, 0);				         
				         break; }       
			     case R.id.localmusiclist_bt: {  // 本地列表界面			         
			        	 Intent intent = new Intent(MainActivity.this,LocalActivity.class);	
			        	 Bundle toLocalData = new Bundle();
			        	 toLocalData.putInt("listPosition", listPosition);
			        	 toLocalData.putBoolean("bt_press", false);
			        	 toLocalData.putBoolean("bt_check",isPlaying);
			        	 toLocalData.putBoolean("isFirstTime", isFirstTime);
			        	 toLocalData.putBoolean("isPlaying", isPlaying);
			        	 toLocalData.putBoolean("isPause", isPause);
			        	 toLocalData.putInt("msg", msg);
				         intent.putExtras(toLocalData);			             
				         startActivityForResult(intent, 0);
			             break; }
			     case R.id.downloadmusiclist_bt: {  // 下载列表界面			         
		        	 Intent intent = new Intent(MainActivity.this,DownloadActivity.class);								        
		        	 Bundle toLocalData = new Bundle();
		        	 toLocalData.putInt("listPosition", listPosition);
		        	 toLocalData.putBoolean("bt_press", false);
		        	 toLocalData.putBoolean("bt_check",isPlaying);
		        	 toLocalData.putBoolean("isFirstTime", isFirstTime);
		        	 toLocalData.putBoolean("isPlaying", isPlaying);
		        	 toLocalData.putBoolean("isPause", isPause);
		        	 toLocalData.putInt("msg", msg);
			         intent.putExtras(toLocalData);			             
			         startActivityForResult(intent, 0);
		             break; }
			     case R.id.lovemusiclist_bt: {  // 收藏列表界面			         
		        	 Intent intent = new Intent(MainActivity.this,LoveActivity.class);								        
		        	 Bundle toLocalData = new Bundle();
		        	 toLocalData.putInt("listPosition", listPosition);
		        	 toLocalData.putBoolean("bt_press", false);
		        	 toLocalData.putBoolean("bt_check",isPlaying);
		        	 toLocalData.putBoolean("isFirstTime", isFirstTime);
		        	 toLocalData.putBoolean("isPlaying", isPlaying);
		        	 toLocalData.putBoolean("isPause", isPause);
		        	 toLocalData.putInt("msg", msg);
			         intent.putExtras(toLocalData);			             
			         startActivityForResult(intent, 0);
		             break; }
			     case R.id.nearmusiclist_bt: {  // 最近播放列表界面			         
		        	 Intent intent = new Intent(MainActivity.this,NearActivity.class);								        
		        	 Bundle toLocalData = new Bundle();
		        	 toLocalData.putInt("listPosition", listPosition);
		        	 toLocalData.putBoolean("bt_press", false);
		        	 toLocalData.putBoolean("bt_check",isPlaying);
		        	 toLocalData.putBoolean("isFirstTime", isFirstTime);
		        	 toLocalData.putBoolean("isPlaying", isPlaying);
		        	 toLocalData.putBoolean("isPause", isPause);
		        	 toLocalData.putInt("msg", msg);
			         intent.putExtras(toLocalData);			             
			         startActivityForResult(intent, 0);
		             break; }
			     case R.id.music_search_bt: {  // 歌曲搜索界面			         
		        	 Intent intent = new Intent(MainActivity.this,SearchActivity.class);								        
		        	 Bundle toLocalData = new Bundle();
		        	 toLocalData.putInt("listPosition", listPosition);
		        	 toLocalData.putBoolean("bt_press", false);
		        	 toLocalData.putBoolean("bt_check",isPlaying);
		        	 toLocalData.putBoolean("isFirstTime", isFirstTime);
		        	 toLocalData.putBoolean("isPlaying", isPlaying);
		        	 toLocalData.putBoolean("isPause", isPause);	
		        	 toLocalData.putInt("msg", msg);
			         intent.putExtras(toLocalData);			             
			         startActivityForResult(intent, 0);
		             break; }
			     case R.id.music_album_iv :  { // 跳到播放界面			          
			         Intent intent = new Intent(MainActivity.this,PlayingActivity.class);	
			         Bundle toPlayingData = new Bundle();
			         toPlayingData.putInt("listPosition", listPosition);
			         toPlayingData.putBoolean("bt_press", false);
			         toPlayingData.putBoolean("bt_check",isPlaying);
			         toPlayingData.putBoolean("isFirstTime", isFirstTime);
			         toPlayingData.putBoolean("isPlaying", isPlaying);
			         toPlayingData.putBoolean("isPause", isPause);
			         toPlayingData.putInt("msg", msg);
			         intent.putExtras(toPlayingData);			             
			         startActivityForResult(intent, 0);				         
			         break; }   
			     case R.id.btn_love:   {// 添加歌曲到收藏夹	
			    	     songFavorite();
		    	    	 lovemusiccount_tv.setText(String.valueOf( helperFavorite.getMP3InfoDBCount()));		    	     
			         break;  }
			     case R.id.mainmusic_play_bt:   {//播放音乐
			    	 if(isFirstTime) {                      
		                	msg = AppConstant.PlayerMsg.PLAY_MSG;  
		                    isFirstTime = false;  
		                    isPlaying = true;  
		                    isPause = false; 	
		                    play_bt_press = false;
			                play_bt_check = true;
		                    if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
			    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
			    	              nearmusiccount_tv.setText(String.valueOf( helper.getMP3InfoDBCount()));
			    	    	 }	                    		                  
		                    anim.start();	               
		                    //animSongName.start();
		                    //animSongSinger.start();
		                    showNofificationInfo(listPosition);//显示通知   
		                    Toast.makeText(MainActivity.this,"开始播放",Toast.LENGTH_SHORT).show(); 
		                } else {  
		                    if (isPlaying) {  		                                            
		                        msg = AppConstant.PlayerMsg.PAUSE_MSG;   
		                        isPlaying = false;  
		                        isPause = true;      
		                        play_bt_press = false;
				                play_bt_check = false;
				                anim.pause();
		                        Toast.makeText(MainActivity.this,"暂停播放  ",Toast.LENGTH_SHORT).show();  
		                    } else if (isPause) {  
		                        msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
		                        isPlaying = true; 
		                        isPause = false;  		
		                        play_bt_press = false;
				                play_bt_check = true;
				                anim.resume();	
				                //animSongName.start();
			                    //animSongSinger.start();
		                        Toast.makeText(MainActivity.this,"继续播放",Toast.LENGTH_SHORT).show();  
		                    }  
		                }  
			    	   FileInfo mp3Info = fileInfos.get(listPosition);  	
			    	   editor.putInt("listPosition", listPosition);
			    	   editor.commit();
			           Bundle bundle  = new Bundle();
			           bundle.putInt("listPosition", listPosition);  
			           bundle.putString("url", mp3Info.getUrl());
			    	   bundle.putInt("MSG", msg);//将msg存放于bundle中 
			    	   intentToService.putExtras(bundle);//将bundle绑定到intent
			           startService(intentToService); 
			           break; }
			    
			}		
		}     	
    }
	
    //初始化歌曲列表
    private void initLocalMusicList(){                
        localmusiccount_tv.setText(String.valueOf( fileInfos.size())); 
        downloadmusiccount_tv.setText(String.valueOf( helperDownload.getMP3InfoDBCount()));
        lovemusiccount_tv.setText(String.valueOf( helperFavorite.getMP3InfoDBCount()));
        nearmusiccount_tv.setText(String.valueOf( helper.getMP3InfoDBCount()));
        music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
    	music_name_tv.setText(fileInfos.get(listPosition).getTitle());       	
    	 
    	anim = ObjectAnimator.ofFloat(music_album_iv, "rotation", 0, 360);  
        lin = new LinearInterpolator();
        anim.setDuration(6000);
        anim.setRepeatCount(-1);
        anim.setRepeatMode(ObjectAnimator.RESTART);
        anim.setInterpolator(lin);  //匀速 	  
        
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("alpha", 1f, 0f, 1f);  
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleX", 1f, 0, 1f);  
        PropertyValuesHolder pvhZ = PropertyValuesHolder.ofFloat("scaleY", 1f, 0, 1f); 
        animSongName = ObjectAnimator.ofPropertyValuesHolder(music_name_tv, pvhX, pvhY,pvhZ); //渐现
        animSongName.setDuration(1000);
        animSongName.setRepeatCount(0);
        animSongName.setRepeatMode(ObjectAnimator.RESTART);
        
       animSongSinger = ObjectAnimator.ofPropertyValuesHolder(music_siger_tv, pvhX, pvhY,pvhZ); //渐现
       animSongSinger.setDuration(1000);
       animSongSinger.setRepeatCount(0);
       animSongSinger.setRepeatMode(ObjectAnimator.RESTART); 
    }
    
    //设置是否歌曲收藏状态
    private void setFavoriteDefault(){
		 if(helperFavorite.getMP3Info(fileInfos.get(listPosition))){	    	    	  
	        	btn_love.setSelected(true);
	     }else{
	    		 btn_love.setSelected(false);
	      }
    }
  //歌曲收藏与取消收藏
    private void songFavorite(){    	
    	  if(helperFavorite.getMP3Info(fileInfos.get(listPosition))){					
				helperFavorite.deleteMP3InfoDB(fileInfos.get(listPosition));
				for(int i=0;i<fileInfos.size();i++){
			          if(fileInfos.get(i).getTitle().equals(fileInfos.get(listPosition).getTitle())){
			        	  fileInfos.get(i).setFavorite(false);	
			          }
				}      	       
	            btn_love.setSelected(false);
	            Toast.makeText(MainActivity.this,"取消收藏",Toast.LENGTH_SHORT).show();  
         } else  {    	    	  
    	          helperFavorite.addMP3InfoDB(fileInfos.get(listPosition));
    	          fileInfos.get(listPosition).setFavorite(true);	
    	    	  btn_love.setSelected(true);
    	          Toast.makeText(MainActivity.this,"收藏成功",Toast.LENGTH_SHORT).show();     	    	         		
         }    	  
    }
    
  //用来接收从PlayerService传回来的广播的内部类
    public class PlayerReceiver extends BroadcastReceiver {     	  
        @Override  
        public void onReceive(Context context, Intent intent) {  
        	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
            String action = intent.getAction();           
            if(action.equals(PlayerService.UPDATE_ACTION)) {  
                //获取Intent中的current消息，current代表当前正在播放的歌曲  
                listPosition = intent.getIntExtra("current", -1);             
                if(listPosition >= 0) {                       
                	music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
                	music_name_tv.setText(fileInfos.get(listPosition).getTitle());                   	
                	animSongName.start();
                    animSongSinger.start();
                	editor.putInt("listPosition", listPosition);
			    	editor.commit();
			    	showNofificationInfo(listPosition);//显示通知                       
			    	if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	    	 }  			    	
                }                
            }else if(action.equals(PlayerService.MUSIC_PREVIOUE)){
            	msg = AppConstant.PlayerMsg.PLAY_MSG;  
                isFirstTime = false;  
                isPlaying = true;  
                isPause = false;  
                listPosition = listPosition - 1;
                if(listPosition < 0){
                	listPosition = fileInfos.size() - 1;
                }	
                music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
                music_name_tv.setText(fileInfos.get(listPosition).getTitle());
    	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
    	    	 }    	       
    	        //animSongName.start();
    	        //animSongSinger.start();
    	        
    	        FileInfo mp3Info = fileInfos.get(listPosition);  	
    	        Bundle bundle  = new Bundle();
    	        bundle.putInt("listPosition", listPosition);  
    	        bundle.putString("url", mp3Info.getUrl());
    	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
    	    	intentToService.putExtras(bundle);//将bundle绑定到intent
    	        startService(intentToService);    	        
            }else if(action.equals(PlayerService.MUSIC_PLAY_PAUSE)){
                if (isPlaying) {  		                                            
                    msg = AppConstant.PlayerMsg.PAUSE_MSG;   
                    isPlaying = false;  
                    isPause = true;      
                    play_bt_press = false;
	                play_bt_check = false;
	                anim.pause(); 
                } else if (isPause) {  
                    msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
                    isPlaying = true; 
                    isPause = false;  		
                    play_bt_press = false;
	                play_bt_check = true;
	                anim.resume();	
	                //animSongName.start();
                    //animSongSinger.start();
                }  
                mainmusic_play_bt.setPressed(play_bt_press);
            	mainmusic_play_bt.setChecked(play_bt_check);
                FileInfo mp3Info = fileInfos.get(listPosition);  	
    	        Bundle bundle  = new Bundle();
    	        bundle.putInt("listPosition", listPosition);  
    	        bundle.putString("url", mp3Info.getUrl());
    	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
    	    	intentToService.putExtras(bundle);//将bundle绑定到intent
    	        startService(intentToService);    	 
            }else if(action.equals(PlayerService.MUSIC_NEXT)){
            	msg = AppConstant.PlayerMsg.PLAY_MSG;  
                isFirstTime = false;  
                isPlaying = true;  
                isPause = false;  
                listPosition = listPosition + 1;
                if(listPosition > fileInfos.size() - 1){
                	listPosition = 0;
                }	
                music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
                music_name_tv.setText(fileInfos.get(listPosition).getTitle());
    	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
    	    	 }    	       
    	        
    	        FileInfo mp3Info = fileInfos.get(listPosition);  	
    	        Bundle bundle  = new Bundle();
    	        bundle.putInt("listPosition", listPosition);  
    	        bundle.putString("url", mp3Info.getUrl());
    	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
    	    	intentToService.putExtras(bundle);//将bundle绑定到intent
    	        startService(intentToService);    	        
            }else if(action.equals(PlayerService.MUSIC_PLAY_STATE)){
            	listPosition = intent.getIntExtra("listPosition", -1);
            	play_bt_press =  intent.getBooleanExtra("bt_press", false);
                play_bt_check =  intent.getBooleanExtra("bt_check", false);
                isFirstTime = intent.getBooleanExtra("isFirstTime", true);  
                isPlaying = intent.getBooleanExtra("isPlaying", false);
                isPause = intent.getBooleanExtra("isPause", false);      
                msg = intent.getIntExtra("msg", -1);
                mainmusic_play_bt.setPressed(play_bt_press);
            	mainmusic_play_bt.setChecked(play_bt_check);
            }
            setFavoriteDefault();//设置是否歌曲收藏状态
      }            
    }  
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, intent);
		if(requestCode == 0  && resultCode == 0){
			Bundle fromLocalData = intent.getExtras();
			listPosition = fromLocalData.getInt("listPosition");     
			play_bt_press = fromLocalData.getBoolean("bt_press");
			play_bt_check = fromLocalData.getBoolean("bt_check");
			isFirstTime  = fromLocalData.getBoolean("isFirstTime");
			isPlaying  = fromLocalData.getBoolean("isPlaying");
			isPause  = fromLocalData.getBoolean("isPause");		
			msg      = fromLocalData.getInt("msg");
			music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
        	music_name_tv.setText(fileInfos.get(listPosition).getTitle());    
        	mainmusic_play_bt.setPressed(play_bt_press);
        	mainmusic_play_bt.setChecked(play_bt_check);
        	editor.putInt("listPosition", listPosition);
	    	editor.commit();
		}
	}
    
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();		
		appState = ((MApplication)getApplicationContext());        
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		helperDownload = appState.getDBHelperDownload();
		fileInfos = appState.getFileInfos();
		localmusiccount_tv.setText(String.valueOf( fileInfos.size()));
		downloadmusiccount_tv.setText(String.valueOf( helperDownload.getMP3InfoDBCount()));
		lovemusiccount_tv.setText(String.valueOf( helperFavorite.getMP3InfoDBCount()));
        nearmusiccount_tv.setText(String.valueOf( helper.getMP3InfoDBCount()));
        setFavoriteDefault();//设置是否歌曲收藏状态
        if (anim != null  &&  isPlaying == true) {  
        	if(!anim.isStarted()){
       	         anim.start();//开始旋转
       	    }else{
       		    anim.resume();//继续旋转
         	}
    	} else{
    		anim.pause(); //停止旋转
    	}
	}    	

	public void showNofificationInfo(int pos){													 		
		//创建一个 NotificationManager的引用
		NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				
    	//创建Nofification并设置其各种属性
		Notification notification = new Notification();
		notification.icon = R.drawable.ic_launcher; //通知图标
		notification.tickerText =fileInfos.get(pos).getTitle(); //状态栏显示的通知文本提示
		notification.when = System.currentTimeMillis(); //通知产生的时间，会在通知信息里显示
						
		//创建RemoteViews实例
		RemoteViews contentView = new RemoteViews(getPackageName(),R.layout.notification_view);			
		contentView.setImageViewResource(R.id.notification_pic_iv, R.drawable.ic_launcher);
		contentView.setTextViewText(R.id.notification_name_tv, fileInfos.get(pos).getTitle());
		contentView.setTextViewText(R.id.notification_singer_tv,  fileInfos.get(pos).getArtist());
		contentView.setOnClickPendingIntent(R.id.notification_pre_bt, PendingIntent.getBroadcast(MainActivity.this,0,new Intent(PlayerService.MUSIC_PREVIOUE),PendingIntent.FLAG_UPDATE_CURRENT));
		contentView.setOnClickPendingIntent(R.id.notification_play_bt, PendingIntent.getBroadcast(MainActivity.this,0,new Intent(PlayerService.MUSIC_PLAY_PAUSE),PendingIntent.FLAG_UPDATE_CURRENT));
		contentView.setOnClickPendingIntent(R.id.notification_next_bt, PendingIntent.getBroadcast(MainActivity.this,0,new Intent(PlayerService.MUSIC_NEXT),PendingIntent.FLAG_UPDATE_CURRENT));

		notification.contentView = contentView;							
		mNotificationManager.notify(0,notification);//发送通知
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
