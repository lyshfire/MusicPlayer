package com.example.musicplayer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.application.MApplication;
import com.example.database.DBHelper;
import com.example.database.MediaUtil;
import com.example.fileinfo.FileInfo;
import com.example.musicplayerservice.PlayerService;
import com.example.myselfview.DragableLuncher;
import com.example.myselfview.LyricView;
import com.example.utils.AppConstant;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("NewApi")
public class PlayingActivity extends Activity {
	private Button jumplist_bt;
	private Button playorder_bt;
	private int playOrder = 2; //播放模式,1--单曲,2--全部循环,3--随机播放
	private TextView playing_title;
	private TextView playing_songname;
	private TextView playing_songsinger;
	private TextView songcount;
	private Button btn_pre;
	private ToggleButton btn_play;
	private Button btn_next;
	private Button btn_love;
	private Button btn_irl;
	private SeekBar music_progressBar;  //歌曲进度  
    private TextView currentProgress_tv;   //当前进度时间  
    private TextView finalProgress_tv;     //歌曲总时间
    
    private ImageView music_pic_iv;
    
	private int listPosition = 0;   //标识列表位置
	private ListView songlist_lv = null; // 音乐列表  	
    private List<FileInfo> fileInfos = new ArrayList<FileInfo>();  
    private SimpleAdapter mAdapter = null; // 简单适配器  
	private boolean isCallIdle = true;   
	private boolean isFirstTime = true;   
    private boolean isPlaying; // 正在播放  
    private boolean isPause; // 暂停 
    private int msg = -1;
    private boolean play_bt_press = false;
    private boolean play_bt_check = false;
    private Intent intentFromMain;
    private PlayerReceiver playerReceiver; //接收PlayerService服务的广播接收器
    private int currentTime=0;    //当前歌曲播放时间
    
    public static LyricView lyricView;
    public static LyricView playinglyricView;
    private boolean isLyricVisible;
    
    /************************ 分页按钮确认 ************************/
	private ImageView oneIv, twoIv, threeIv;
	public static Context mContext;
	private DragableLuncher smallPage; // 声明页面对象
	
	/***********图片动画**************/
	private ObjectAnimator anim;
	private ObjectAnimator animSongName;
	private ObjectAnimator animSongSinger;
	private LinearInterpolator lin; //旋转速率,匀速
	
    private SharedPreferences preferences;
   	private Editor editor;
   	private  DBHelper helper;
   	private  DBHelper helperFavorite;
   	MApplication appState;
   	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				if(lyricView.getVisibility()==View.VISIBLE) {
	                   lyricView.SelectIndex(currentTime);
	                   lyricView.invalidate();
	                   playinglyricView.SelectIndex(currentTime);
					  playinglyricView.invalidate();
	             }					
			}
		}				
	};   	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_play);
		playerReceiver = new PlayerReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(PlayerService.UPDATE_ACTION);  //更新动作 
        filter.addAction(PlayerService.MUSIC_CURRENT);   //音乐当前时间改变动作  
        filter.addAction(PlayerService.MUSIC_PREVIOUE);
        filter.addAction(PlayerService.MUSIC_PLAY_PAUSE); 
        filter.addAction(PlayerService.MUSIC_NEXT); 
        filter.addAction(PlayerService.MUSIC_PLAY_STATE);         
        registerReceiver(playerReceiver, filter); 
		
		intentFromMain = getIntent();
		Bundle bundle = intentFromMain.getExtras();
		listPosition = bundle.getInt("listPosition");
		play_bt_press = bundle.getBoolean("bt_press");
		play_bt_check = bundle.getBoolean("bt_check");
		isFirstTime  = bundle.getBoolean("isFirstTime");
		isPlaying  = bundle.getBoolean("isPlaying");
		isPause  = bundle.getBoolean("isPause");
		msg      = bundle.getInt("msg");
		
		preferences = getSharedPreferences("songPosition", MODE_PRIVATE);		
		isLyricVisible  = preferences.getBoolean("isLyricVisible", false);
		playOrder     = preferences.getInt("playOrder", playOrder);
		editor = preferences.edit();
		
		appState = ((MApplication)getApplicationContext());     
		fileInfos = appState.getFileInfos();
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		
		mContext = PlayingActivity.this;
		findViewById();
		setViewOnclickListener();
		initMusicList();		 
	    
		setPlayOrderDefault(); //设置播放模式、当前播放状态
		setLyricVisibleFavoriteDefault();//设置是否显示歌词、歌曲收藏状态
		
		// 小页面允许触摸执行动画
		smallPage.isOpenTouchAnima(true);
		smallPage.setMusicPlayerAct(this);
		
		// 添加来电监听事件
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); // 获取系统服务
		telManager.listen(new MobliePhoneStateListener(),PhoneStateListener.LISTEN_CALL_STATE);
	}
			
	//初始化歌曲列表
    private void initMusicList(){    	                       
    	playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
        playing_songname.setText(fileInfos.get(listPosition).getTitle());     
        songcount.setText((listPosition+1)+"/"+fileInfos.size());
        music_progressBar.setMax((int)fileInfos.get(listPosition).getDuration());  
        finalProgress_tv.setText(MediaUtil.formatTime(fileInfos.get(listPosition).getDuration()));         
        setListAdpter(MediaUtil.getMusicMaps(fileInfos));//显示歌曲列表       
        
        /********************初始化动画***********************************************/
        anim = ObjectAnimator.ofFloat(music_pic_iv, "rotation", 0, 360);    //旋转 
        lin = new LinearInterpolator();
        anim.setDuration(6000);
        anim.setRepeatCount(-1);
        anim.setRepeatMode(ObjectAnimator.RESTART);
        anim.setInterpolator(lin);  //匀速
        
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("alpha", 1f, 0f, 1f);  
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleX", 1f, 0, 1f);  
        PropertyValuesHolder pvhZ = PropertyValuesHolder.ofFloat("scaleY", 1f, 0, 1f); 
        animSongName = ObjectAnimator.ofPropertyValuesHolder(playing_songname, pvhX, pvhY,pvhZ); //渐现
        animSongName.setDuration(1000);
        animSongName.setRepeatCount(0);
        animSongName.setRepeatMode(ObjectAnimator.RESTART);
        
        animSongSinger = ObjectAnimator.ofPropertyValuesHolder(playing_songsinger, pvhX, pvhY,pvhZ); //渐现
        animSongSinger.setDuration(1000);
        animSongSinger.setRepeatCount(0);
        animSongSinger.setRepeatMode(ObjectAnimator.RESTART);
               
             
    }
    
  //设置播放模式、当前播放状态
  	private void setPlayOrderDefault(){ 		
  		switch(playOrder){
  	     case 1 : {	    	   
  	    	     playorder_bt.setBackgroundResource(R.drawable.repeat_once_bt_selector);
  	    	     break; } 
  	     case 2 : {
     	         playorder_bt.setBackgroundResource(R.drawable.repeat_bt_selector);
     	         break; } 
  	     case 3 : {
     	         playorder_bt.setBackgroundResource(R.drawable.random_bt_selector);
     	         break; } 
  		}
  		/***************设置播放状态************************/     
  		btn_play.setPressed(play_bt_press);
        btn_play.setChecked(play_bt_check);
        if(isPlaying){
        	playing_title.setText(R.string.playing);         		
        }      
        else if(isPause){
        	playing_title.setText(R.string.playingnor);           	
        }        
        else{
        	playing_title.setText(R.string.waitingplay);
        }
  	}
    //设置是否显示歌词、歌曲收藏状态
    private void setLyricVisibleFavoriteDefault(){
    	LyricRead(listPosition);	
		if(isLyricVisible) {
			lyricView.setVisibility(View.VISIBLE);    
			playinglyricView.setVisibility(View.VISIBLE);    
            btn_irl.setSelected(true);
		}
		if(helperFavorite.getMP3Info(fileInfos.get(listPosition)))	{	    	    	  
        	btn_love.setSelected(true);
        }else{
    		 btn_love.setSelected(false); 
       }
    }
    //歌词显示与关闭
    private void LyricVisible(){  	   	
        if(!btn_irl.isSelected()) {
            lyricView.setVisibility(View.VISIBLE); 
            playinglyricView.setVisibility(View.VISIBLE); 
            btn_irl.setSelected(true);
        }
        else {
            lyricView.setVisibility(View.GONE);   
            playinglyricView.setVisibility(View.GONE);  
            btn_irl.setSelected(false);
        }
        editor.putBoolean("isLyricVisible", btn_irl.isSelected());			
	    editor.commit();
    }  
    //读取当前歌曲歌词
    private void LyricRead(int pos){
    	String LyricPath = fileInfos.get(pos).getUrl();
        LyricPath = appState.getFileNameNoEx(LyricPath) + ".lrc";
        try {       	
        	lyricView.readLyricFile(LyricPath);
        	playinglyricView.readLyricFile(LyricPath);
		} catch (IOException e) {			
			e.printStackTrace();
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
	            Toast.makeText(PlayingActivity.this,"取消收藏",Toast.LENGTH_SHORT).show();  
         } else  {    	    	  
    	          helperFavorite.addMP3InfoDB(fileInfos.get(listPosition));
    	          fileInfos.get(listPosition).setFavorite(true);	
    	    	  btn_love.setSelected(true);
    	          Toast.makeText(PlayingActivity.this,"收藏成功",Toast.LENGTH_SHORT).show();     	    	         		
         }    	  
    }
    
	//从界面上根据id获取按钮
    private void findViewById() {  
    	jumplist_bt = (Button)findViewById(R.id.jumplist_bt);
    	playorder_bt = (Button)findViewById(R.id.playorder_bt);
    	playing_title = (TextView)findViewById(R.id.playing_title);
    	btn_love = (Button)findViewById(R.id.btn_love);   	
    	btn_pre = (Button)findViewById(R.id.btn_pre);
    	btn_play = (ToggleButton)findViewById(R.id.btn_play);
    	btn_next = (Button)findViewById(R.id.btn_next);  	
    	btn_irl = (Button)findViewById(R.id.btn_irl);
    	playing_songname = (TextView)findViewById(R.id.playing_songname);
    	playing_songsinger = (TextView)findViewById(R.id.playing_songsinger);
    	songcount =  (TextView)findViewById(R.id.songcount);
    	
    	music_pic_iv = (ImageView)findViewById(R.id.music_pic_iv);
    	songlist_lv = (ListView)findViewById(R.id.songlist_lv);
    	
    	/************************ 分页按钮 ************************/
		oneIv = (ImageView) findViewById(R.id.below_line1_Iv);
		twoIv = (ImageView) findViewById(R.id.below_line2_Iv);
		threeIv = (ImageView) findViewById(R.id.below_line3_Iv);
		// 设置布局动画		
		smallPage = (DragableLuncher) findViewById(R.id.space);
		
    	lyricView = (LyricView)findViewById(R.id.lyric_view);
    	lyricView.setVisibility(View.GONE);
    	playinglyricView = (LyricView)findViewById(R.id.playing_lyric_view);
    	playinglyricView.setVisibility(View.GONE);
    	
    	music_progressBar = (SeekBar) findViewById(R.id.music_progressBar);  
    	currentProgress_tv = (TextView) findViewById(R.id.currentProgress_tv);  
    	finalProgress_tv = (TextView) findViewById(R.id.finalProgress_tv); 
    	   	
	}
   //给每一个按钮设置监听器
    private void setViewOnclickListener() {  
    	ViewOnClickListener viewOnClickListener = new ViewOnClickListener();      	
    	btn_pre.setOnClickListener(viewOnClickListener);
    	btn_play.setOnClickListener(viewOnClickListener);
    	btn_next.setOnClickListener(viewOnClickListener);
    	music_progressBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
    	
    	BtOnClickListener  btOnClickListener = new BtOnClickListener();
    	jumplist_bt.setOnClickListener(btOnClickListener);
    	playorder_bt.setOnClickListener(btOnClickListener);
    	btn_love.setOnClickListener(btOnClickListener);
    	btn_irl.setOnClickListener(btOnClickListener);
    	
    	songlist_lv.setOnItemClickListener(new MusicListItemClickListener()); //列表监听   	
	}
    private class BtOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {			
			switch (v.getId()) {  
		     case R.id.jumplist_bt:  {// 跳到播放界面				        				
			            Bundle toMainData = new Bundle();
			    	    toMainData.putInt("listPosition", listPosition);
			    	    toMainData.putBoolean("bt_press", false);
			    	    toMainData.putBoolean("bt_check",isPlaying);
			    	    toMainData.putBoolean("isFirstTime", isFirstTime);
			    	    toMainData.putBoolean("isPlaying", isPlaying);
			    	    toMainData.putBoolean("isPause", isPause);
			    	    toMainData.putInt("msg", msg);
			    	    intentFromMain.putExtras(toMainData);
			            PlayingActivity.this.setResult(0, intentFromMain); 
			            PlayingActivity.this.finish();
			         break; }
		     case R.id.playorder_bt:   {// 播放模式		
		    	     switch(playOrder){
		    	     case 1 : {
		    	    	     playOrder = 2;  //全部循环
		    	    	     playorder_bt.setBackgroundResource(R.drawable.repeat_bt_selector);
		    	    	     break; } 
		    	     case 2 : {
	    	    	         playOrder = 3; //随机
	    	    	         playorder_bt.setBackgroundResource(R.drawable.random_bt_selector);
	    	    	         break; } 
		    	     case 3 : {
	    	    	         playOrder = 1; //单曲
	    	    	         playorder_bt.setBackgroundResource(R.drawable.repeat_once_bt_selector);
	    	    	         break; } 
		    	     }
		    	     editor.putInt("playOrder", playOrder);
		    	     editor.commit();
		    	     Intent sendIntent = new Intent(PlayerService.CTL_ACTION);
					 sendIntent.putExtra("playOrder", playOrder);					
					 sendBroadcast(sendIntent);
	                 break;  }
		     case R.id.btn_love:   {// 添加歌曲到收藏夹	
		    	     songFavorite();
		             break;  }
		     case R.id.btn_irl:   {// 歌词			    	 
		    	     LyricVisible();
		             break;  }
			}
		} 	
    }
    //自定义的监听器类
    private class ViewOnClickListener implements OnClickListener {
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onClick(View v) {
			switch (v.getId()) {  			     
			     case R.id.btn_play:   {//播放音乐
			    	 if(isFirstTime) {                      
		                	msg = AppConstant.PlayerMsg.PLAY_MSG;  
		                    isFirstTime = false;  
		                    isPlaying = true;  
		                    isPause = false; 
		                    play_bt_press = false;
			                play_bt_check = true;
		                    if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
			    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
			    	    	 }
		                    anim.start();	
		                    //animSongName.start();
		                    //animSongSinger.start();
		                	playing_title.setText(R.string.playing);
		                    Toast.makeText(PlayingActivity.this,"开始播放",Toast.LENGTH_SHORT).show(); 
		                } else {  
		                    if (isPlaying) {  		                                            
		                        msg = AppConstant.PlayerMsg.PAUSE_MSG;   
		                        isPlaying = false;  
		                        isPause = true;     
		                        play_bt_press = false;
				                play_bt_check = false;
		                        anim.pause();
		                        playing_title.setText(R.string.playingnor);
		                        Toast.makeText(PlayingActivity.this,"暂停播放  ",Toast.LENGTH_SHORT).show();  
		                    } else if (isPause) {  
		                        msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
		                        isPlaying = true; 
		                        isPause = false;  	
		                        play_bt_press = false;
				                play_bt_check = true;
				                if(!anim.isStarted()){
				          	         anim.start();//开始旋转
				          	    }else{
				          		    anim.resume();//继续旋转
				            	}	
				                //animSongName.start();
				                //animSongSinger.start();
		                        playing_title.setText(R.string.playing);
		                        Toast.makeText(PlayingActivity.this,"继续播放",Toast.LENGTH_SHORT).show();  
		                    }  
		                }  			    	   
			           break; }
			     case R.id.btn_pre:  {// 上一首  		
			    	    msg = AppConstant.PlayerMsg.PLAY_MSG;  
		                isFirstTime = false;  
		                isPlaying = true;  
		                isPause = false;  
		                listPosition = listPosition - 1;
		                if(listPosition < 0){
		                	listPosition = fileInfos.size() - 1;
		                }	
		                play_bt_press = false;
		                play_bt_check = true;
		                btn_play.setPressed(play_bt_press);
		                btn_play.setChecked(play_bt_check);
		                playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
		    	        playing_songname.setText(fileInfos.get(listPosition).getTitle());
		    	        songcount.setText((listPosition+1)+"/"+fileInfos.size());
		    	        music_progressBar.setMax((int)fileInfos.get(listPosition).getDuration());  
	                    finalProgress_tv.setText(MediaUtil.formatTime(fileInfos.get(listPosition).getDuration())); 
		    	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
		    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
		    	    	 }
		    	        if(helperFavorite.getMP3Info(fileInfos.get(listPosition)))	{	    	    	  
		    	        	btn_love.setSelected(true);
		    	    	 }else{
		    	    		 btn_love.setSelected(false);
		    	    	 }
		    	        LyricRead(listPosition);
		    	        //animSongName.start();
		    	        //animSongSinger.start();
		                Toast.makeText(PlayingActivity.this,"准备播放上一首  ",Toast.LENGTH_SHORT).show();                
		                break; } 
		            case R.id.btn_next: {// 下一首  	
		            	msg = AppConstant.PlayerMsg.PLAY_MSG;  
		                isFirstTime = false;  
		                isPlaying = true;  
		                isPause = false;  
		                listPosition = listPosition + 1;
		                if(listPosition > fileInfos.size() - 1){
		                	listPosition = 0;
		                }	
		                play_bt_press = false;
		                play_bt_check = true;
		                btn_play.setPressed(play_bt_press);
		                btn_play.setChecked(play_bt_check);
		                playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
		    	        playing_songname.setText(fileInfos.get(listPosition).getTitle());
		    	        songcount.setText((listPosition+1)+"/"+fileInfos.size());
		    	        music_progressBar.setMax((int)fileInfos.get(listPosition).getDuration());  
	                    finalProgress_tv.setText(MediaUtil.formatTime(fileInfos.get(listPosition).getDuration())); 
		    	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
		    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
		    	    	 }
		    	        if(helperFavorite.getMP3Info(fileInfos.get(listPosition)))	{	    	    	  
		    	        	btn_love.setSelected(true);
		    	    	 }else{
		    	    		 btn_love.setSelected(false);
		    	    	 }
		    	        LyricRead(listPosition);
		    	        //animSongName.start();
		    	        //animSongSinger.start();
		                Toast.makeText(PlayingActivity.this,"准备播放下一首  ",Toast.LENGTH_SHORT).show(); 
		                break;  }
			}
			Intent sendIntent = new Intent(PlayerService.MUSIC_PLAY_STATE);
			sendIntent.putExtra("listPosition", listPosition);	
			sendIntent.putExtra("bt_press", play_bt_press); 
			sendIntent.putExtra("bt_check", play_bt_check);
			sendIntent.putExtra("isFirstTime", isFirstTime);
			sendIntent.putExtra("isPlaying", isPlaying);
			sendIntent.putExtra("isPause", isPause);
			sendIntent.putExtra("msg", msg);	
			sendBroadcast(sendIntent);
			
			FileInfo mp3Info = fileInfos.get(listPosition);  	
			editor.putBoolean("isLyricVisible", btn_irl.isSelected());			
	    	editor.commit();
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
	    	intentToService.putExtras(bundle);//将bundle绑定到intent
	        startService(intentToService); 	        
		}     	
    } 
    
  //显示歌曲列表
    public   void setListAdpter(List<HashMap<String, String>> mp3list) {  
        if(mp3list != null){
        	mAdapter = new SimpleAdapter(PlayingActivity.this, mp3list,  
                    R.layout.music_list_item_layout, new String[] {"duration","title","artist"}, 
                    new int[] {R.id.music_duration,R.id.music_title,R.id.music_artist });  
            songlist_lv.setAdapter(mAdapter);          
        }else{
        	Toast.makeText(PlayingActivity.this, "本地列表没有歌曲", Toast.LENGTH_SHORT).show();
        }       
    }
   //点击列表更改播放状态
    private class MusicListItemClickListener implements OnItemClickListener {    
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
        @Override  
        public void onItemClick(AdapterView<?> parent, View view, int position,long id) {  
            listPosition = position;   
            FileInfo mp3Info = fileInfos.get(listPosition);  
            msg = AppConstant.PlayerMsg.PLAY_MSG;  
            isFirstTime = false;  
            isPlaying = true;  
            isPause = false;  
            
            play_bt_press = false;
            play_bt_check = true;
            btn_play.setPressed(play_bt_press);
            btn_play.setChecked(play_bt_check);
            playing_songsinger.setText(mp3Info.getArtist());  
	        playing_songname.setText(mp3Info.getTitle());
	        songcount.setText((listPosition+1)+"/"+fileInfos.size());
	        music_progressBar.setMax((int)mp3Info.getDuration());  
            finalProgress_tv.setText(MediaUtil.formatTime(mp3Info.getDuration())); 
	        if(helper.getMP3Info(mp3Info) == false)	{	    //添加到最近播放列表	    	  
	              helper.addMP3InfoDB(mp3Info);
	    	 }
	        LyricRead(listPosition);    
	        
	        /*************设置旋转*********************************/
	        if (anim != null  &&  isPlaying == true) {  
	        	if(!anim.isStarted()){
	       	         anim.start();//开始旋转
	       	    }else{
	       		    anim.resume();//继续旋转
	         	}
	    	} else{
	    		anim.pause(); //停止旋转
	    	}
	        //animSongName.start();
	        //animSongSinger.start();
	        
	        editor.putBoolean("isLyricVisible", btn_irl.isSelected());			
	    	editor.commit();
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
	    	intentToService.putExtras(bundle);//将bundle绑定到intent
	        startService(intentToService); 
        }   
    }  
    private class SeekBarChangeListener implements OnSeekBarChangeListener{
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {				
			currentProgress_tv.setText(MediaUtil.formatTime(progress));  			
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {				
			Toast.makeText(PlayingActivity.this,"左右拖动快进快退",Toast.LENGTH_SHORT).show(); 
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			FileInfo mp3Info = fileInfos.get(listPosition); 
			msg = AppConstant.PlayerMsg.PROGRESS_CHANGE;  
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//将msg存放于bundle中 
	    	intentToService.putExtras(bundle);//将bundle绑定到intent
	    	intentToService.putExtra("progress", seekBar.getProgress());
	        startService(intentToService); 
		}
    	
    }     
    
    /**
	 * @author wwj
	 * 电话监听器类
	 */
	private class MobliePhoneStateListener extends PhoneStateListener {
		Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {			
			switch (state) {
			         case TelephonyManager.CALL_STATE_IDLE: { // 挂机状态
			        	 if( !isCallIdle ){
			        		    isCallIdle = true;
			        		    play_bt_press = false;
	                            play_bt_check = true;
	                            btn_play.setPressed(play_bt_press);
	                            btn_play.setChecked(play_bt_check);
	                            msg = AppConstant.PlayerMsg.CONTINUE_MSG;
	                            isPlaying = true;
					            isPause = false;			
					            Bundle bundle  = new Bundle();			      
							    bundle.putInt("MSG", msg);	//继续播放音乐
							    intentToService.putExtras(bundle);//将bundle绑定到intent
								startService(intentToService);
					            Toast.makeText(PlayingActivity.this,"闲置状态继续播放",Toast.LENGTH_SHORT).show(); 
			        	  }
				            break;}
			        case TelephonyManager.CALL_STATE_OFFHOOK:	//通话状态
			        case TelephonyManager.CALL_STATE_RINGING:	{//响铃状态
			        	        isCallIdle = false;
				            	play_bt_press = false;
	                            play_bt_check = false;
	                            btn_play.setPressed(play_bt_press);
	                            btn_play.setChecked(play_bt_check);						      
				                msg = AppConstant.PlayerMsg.PAUSE_MSG;	//										
				                isPlaying = false;
					            isPause = true;
					            Bundle bundle  = new Bundle();			      
							    bundle.putInt("MSG", msg);	//继续播放音乐
							    intentToService.putExtras(bundle);//将bundle绑定到intent
								startService(intentToService);
					            Toast.makeText(PlayingActivity.this,"来电去电暂停播放  ",Toast.LENGTH_SHORT).show(); 				            
				            break;}
			       default: {
				            break;}
			}
			
		}
	}
      
  //用来接收从PlayerService传回来的广播的内部类
    public class PlayerReceiver extends BroadcastReceiver {     	  
        @Override  
        public void onReceive(Context context, Intent intent) {          	
            String action = intent.getAction();  
            if(action.equals(PlayerService.MUSIC_CURRENT)) {  
                currentTime = intent.getIntExtra("currentTime", -1);  
                currentProgress_tv.setText(MediaUtil.formatTime(currentTime));  
                music_progressBar.setProgress(currentTime);      
                handler.sendEmptyMessage(1);
            }else if(action.equals(PlayerService.UPDATE_ACTION)) {  
            	//获取Intent中的current消息，current代表当前正在播放的歌曲  
                listPosition = intent.getIntExtra("current", -1);                    
                if(listPosition >= 0) {                       
                	playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
                	playing_songname.setText(fileInfos.get(listPosition).getTitle());    
                	songcount.setText((listPosition+1)+"/"+fileInfos.size());
                	animSongName.start();
                    animSongSinger.start();
                	music_progressBar.setMax((int)fileInfos.get(listPosition).getDuration());  
                    finalProgress_tv.setText(MediaUtil.formatTime(fileInfos.get(listPosition).getDuration())); 
                    String LyricPath = fileInfos.get(listPosition).getUrl();
					LyricPath = appState.getFileNameNoEx(LyricPath) + ".lrc";
			        try {
			        	lyricView.readLyricFile(LyricPath);
			        	playinglyricView.readLyricFile(LyricPath);
					} catch (IOException e) {			
						e.printStackTrace();
					}
                	if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	    	 }
                	if(helperFavorite.getMP3Info(fileInfos.get(listPosition)))	{	    	    	  
		    	        	btn_love.setSelected(true);
		    	    }else{
		    	    		 btn_love.setSelected(false);
		    	    }
               }  
          } else if(action.equals(PlayerService.MUSIC_PREVIOUE)){
          	msg = AppConstant.PlayerMsg.PLAY_MSG;  
            isFirstTime = false;  
            isPlaying = true;  
            isPause = false;  
            listPosition = listPosition - 1;
            if(listPosition < 0){
            	listPosition = fileInfos.size() - 1;
            }	
            playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
            playing_songname.setText(fileInfos.get(listPosition).getTitle());
	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	 }    	       
	        animSongName.start();
	        animSongSinger.start();	                
        }else if(action.equals(PlayerService.MUSIC_PLAY_PAUSE)){
        	 if (isPlaying) {  		                                            
                 msg = AppConstant.PlayerMsg.PAUSE_MSG;   
                 isPlaying = false;  
                 isPause = true;     
                 play_bt_press = false;
	                play_bt_check = false;
                 anim.pause();
                 playing_title.setText(R.string.playingnor); 
             } else if (isPause) {  
                 msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
                 isPlaying = true; 
                 isPause = false;  	
                 play_bt_press = false;
	             play_bt_check = true;
	          	 anim.resume();//继续旋转
	             animSongName.start();
	             animSongSinger.start();
                 playing_title.setText(R.string.playing);
             }  
            btn_play.setPressed(play_bt_press);
            btn_play.setChecked(play_bt_check);    	 
        }else if(action.equals(PlayerService.MUSIC_NEXT)){
        	msg = AppConstant.PlayerMsg.PLAY_MSG;  
            isFirstTime = false;  
            isPlaying = true;  
            isPause = false;  
            listPosition = listPosition + 1;
            if(listPosition > fileInfos.size() - 1){
            	listPosition = 0;
            }	
            playing_songsinger.setText(fileInfos.get(listPosition).getArtist());  
            playing_songname.setText(fileInfos.get(listPosition).getTitle());
	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	 }    	       
	        animSongName.start();
	        animSongSinger.start();      
        }           
        }
    }   
    
    public void refreshPage(int page) {
		Message msg = new Message();
		msg.arg1 = 001;
		msg.arg2 = page;
		handlerPage.sendMessage(msg);
	}
    @SuppressLint("HandlerLeak")
	Handler handlerPage = new Handler() {
		public void dispatchMessage(Message msg) {
			switch (msg.arg1) {
			case 001:
				switch (msg.arg2) {
				case 0://第一页
					oneIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator_focused));
					twoIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));
					threeIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));
					break;
				case 1://第二页
					oneIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));
					twoIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator_focused));
					threeIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));					
					break;
				case 2://第三页
					oneIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));
					twoIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator));
					threeIv.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ad_indicator_focused));
					break;				
				}
				break;
			case 002:
				//TODO
				break;
			}
		}
	};   	
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		appState = ((MApplication)getApplicationContext());        
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		fileInfos = appState.getFileInfos();
		btn_play.setPressed(play_bt_press);
        btn_play.setChecked(play_bt_check);
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
	
	
}
