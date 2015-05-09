package com.example.musicplayer;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.application.MApplication;
import com.example.database.DBHelper;
import com.example.database.MediaUtil;
import com.example.fileinfo.FileInfo;
import com.example.musicplayer.webapi.HttpDownloader;
import com.example.musicplayer.webapi.JsonSong;
import com.example.musicplayer.webapi.JsonSongInfo;
import com.example.musicplayer.webapi.SongInfoListener;
import com.example.musicplayer.webapi.SongListListener;
import com.example.musicplayer.webapi.WebAPI;
import com.example.musicplayerservice.PlayerService;
import com.example.utils.AppConstant;


import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

@SuppressLint({ "HandlerLeak", "NewApi" })
public class SearchActivity  extends Activity{
	private Button return_bt;
	 private TextView music_name_tv;
	 private TextView music_singer_tv;
	 private ToggleButton music_play_bt;
	 private Button btn_love;
	 private RelativeLayout bottombar_layout;
	 private ImageView music_album_iv;
	 private EditText  music_name_et;
	 private Button search_bt;
	 private ListView songlist_lv;
	 
	 private ProgressDialog dialog ;
	 private List<JsonSong>searchlist;
	 private List<FileInfo> searchlistFromJson = new ArrayList<FileInfo>();
	 private SimpleAdapter mAdapter = null; // 简单适配器
	 
	 private List<FileInfo> fileInfos = new ArrayList<FileInfo>();  
	 private int listPosition = 0;   //标识列表位置
	 private boolean isFirstTime = true;   
	 private boolean isPlaying; // 正在播放  
	 private boolean isPause; // 暂停 
	 private int msg = -1;
	 private boolean play_bt_press = false;
	 private boolean play_bt_check = false;
	 private Intent intentFromMain;
	 private PlayerReceiver playerReceiver; //接收PlayerService服务的广播接收器
	 private SharedPreferences preferences;
	 private Editor editor;
	 
	 /***********图片动画**************/
	private ObjectAnimator anim;
	private ObjectAnimator animSongName;
	private ObjectAnimator animSongSinger;
	private LinearInterpolator lin;		
		  
	/************手势****************************/
	GestureDetector  myGestureDetector;
	
	 private  DBHelper helper; 
	 private  DBHelper helperFavorite;
	 private  DBHelper helperDownload;
	 private  MApplication appState;
	 private Handler handler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch(msg.what){
				case 0:
					Toast.makeText(SearchActivity.this, "开始下载", Toast.LENGTH_SHORT).show();
					break;
				case 1:
					Toast.makeText(SearchActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
					
					dialog.cancel();
					break;
				case -1:
					Toast.makeText(SearchActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
					dialog.cancel();
					break;
				case -2:
					Toast.makeText(SearchActivity.this, "文件同名，下载失败", Toast.LENGTH_SHORT).show();
					dialog.cancel();
					break;
				}
			}
		};
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		setContentView(R.layout.activity_search);
		
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
		editor = preferences.edit();
		
		appState = ((MApplication)getApplicationContext());   
		fileInfos = appState.getFileInfos();
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		helperDownload = appState.getDBHelperDownload();
		
		findViewById();
		setViewOnclickListener();
		initLocalMusicList();
		
		playerReceiver = new PlayerReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(PlayerService.UPDATE_ACTION);   
        filter.addAction(PlayerService.MUSIC_PLAY_PAUSE);
        registerReceiver(playerReceiver, filter);		
	}
    
	//从界面上根据id获取按钮
    private void findViewById() { 
    	return_bt = (Button)findViewById(R.id.return_bt);
    	music_name_tv = (TextView)findViewById(R.id.music_name_tv);
    	music_singer_tv = (TextView)findViewById(R.id.music_singer_tv);  
    	music_play_bt = (ToggleButton)findViewById(R.id.music_play_bt);
    	btn_love          = (Button)findViewById(R.id.btn_love);
    	music_album_iv = (ImageView)findViewById(R.id.music_album_iv);
    	music_name_et = (EditText)findViewById(R.id.music_name_et);
    	search_bt = (Button)findViewById(R.id.search_bt);
    	songlist_lv = (ListView)findViewById(R.id.songlist_lv);
    	
    	myGestureDetector = new GestureDetector(new myGestureListener());
    	bottombar_layout = (RelativeLayout)findViewById(R.id.bottombar_layout);
    }
    
  //给每一个按钮设置监听器
    private void setViewOnclickListener() {  
    	ViewOnClickListener viewOnClickListener = new ViewOnClickListener();  
    	return_bt.setOnClickListener(viewOnClickListener);
    	music_play_bt.setOnClickListener(viewOnClickListener);
    	music_album_iv.setOnClickListener(viewOnClickListener);
    	search_bt.setOnClickListener(viewOnClickListener);
    	btn_love.setOnClickListener(viewOnClickListener);
    	songlist_lv.setOnItemClickListener(new MusicListItemClickListener()); //列表监听
    	
    	bottombar_layout.setOnClickListener(viewOnClickListener);
    	bottombar_layout.setOnTouchListener(new OnTouchListener() {	
			//捕获bottombar_layout的触摸事件,并传递给GestureDetector
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				myGestureDetector.onTouchEvent(event);
				return false;
			}
		});
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
             music_play_bt.setPressed(play_bt_press);
             music_play_bt.setChecked(play_bt_check);
    		FileInfo mp3Info = fileInfos.get(listPosition); 
    		music_singer_tv.setText(fileInfos.get(listPosition).getArtist());  
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
    
    //自定义的监听器类
    private class ViewOnClickListener implements OnClickListener {
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
		@Override
		public void onClick(View v) {
			switch (v.getId()) {  
			     case R.id.return_bt: {// 跳到播放界面
			    	 Bundle toMainData = new Bundle();
			    	    toMainData.putInt("listPosition", listPosition);
			    	    toMainData.putBoolean("bt_press", false);
			    	    toMainData.putBoolean("bt_check",isPlaying);
			    	    toMainData.putBoolean("isFirstTime", isFirstTime);
			    	    toMainData.putBoolean("isPlaying", isPlaying);
			    	    toMainData.putBoolean("isPause", isPause);
			    	    toMainData.putInt("msg", msg);
			    	    intentFromMain.putExtras(toMainData);
			    	    SearchActivity.this.setResult(0, intentFromMain); 
			    	    SearchActivity.this.finish();
				         break; }
			     case R.id.music_album_iv :  { // 跳到播放界面			          
			         Intent intent = new Intent(SearchActivity.this,PlayingActivity.class);	
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
			         break;  }
			     case R.id.search_bt:  {
						String song = music_name_et.getText().toString();
						if(song!=null&&!song.trim().equals("")){
							WebAPI api = new WebAPI();
							dialog.show();
							api.searchSongs(song, new SongListListener (){
								@Override
								public void songlist(List<JsonSong> songlist) {
									dialog.cancel();
									searchlist = songlist;
									if(searchlist==null){
										searchlist = new ArrayList<JsonSong>();
									}			
									JsonToFileInfos(searchlist,searchlistFromJson);
									setListAdpter(MediaUtil.getMusicMaps(searchlistFromJson));
								}
								@Override
								public void error() {	
									dialog.cancel();
									Toast.makeText(SearchActivity.this,"搜索失败",Toast.LENGTH_SHORT).show();
								}});						
							Toast.makeText(SearchActivity.this,song,Toast.LENGTH_SHORT).show();
						}
					break;}			    	 
			     case R.id.music_play_bt:   {//播放音乐
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
		                    anim.start();  //开始旋转	
		                   // animSongName.start();
		                   // animSongSinger.start();
		                    Toast.makeText(SearchActivity.this,"开始播放",Toast.LENGTH_SHORT).show(); 
		                } else {  
		                    if (isPlaying) {  		                                            
		                        msg = AppConstant.PlayerMsg.PAUSE_MSG;   
		                        isPlaying = false;  
		                        isPause = true;    
		                        play_bt_press = false;
				                play_bt_check = false;
		                        anim.pause(); //停止旋转
		                        Toast.makeText(SearchActivity.this,"暂停播放  ",Toast.LENGTH_SHORT).show();  
		                    } else if (isPause) {  
		                        msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
		                        isPause = false;  
		                        isPlaying = true;  
		                        play_bt_press = false;
				                play_bt_check = true;
				                if(!anim.isStarted()){
					       	         anim.start();//开始旋转
					       	    }else{
					       		    anim.resume();//继续旋转
					         	}	
				               // animSongName.start();
			                    //animSongSinger.start();
		                        Toast.makeText(SearchActivity.this,"继续播放",Toast.LENGTH_SHORT).show();  
		                    }  
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
    
  //显示歌曲列表
    public   void setListAdpter(List<HashMap<String, String>> mp3list) {  
        if(mp3list != null){
        	mAdapter = new SimpleAdapter(SearchActivity.this, mp3list,  
                    R.layout.music_list_item_layout, new String[] {"duration","title","artist"}, 
                    new int[] {R.id.music_duration,R.id.music_title,R.id.music_artist });  
            songlist_lv.setAdapter(mAdapter);           
        }else{
        	Toast.makeText(SearchActivity.this, "本地列表没有歌曲", Toast.LENGTH_SHORT).show();
        }       
    }
    
  //初始化歌曲列表
    private void initLocalMusicList(){ 
    	//fileInfos = MediaUtil.getFileInfos(getApplicationContext()); //获取歌曲对象集合    
    	dialog = new ProgressDialog(this);
        music_singer_tv.setText(fileInfos.get(listPosition).getArtist());  
    	music_name_tv.setText(fileInfos.get(listPosition).getTitle());    
    	music_play_bt.setPressed(play_bt_press);
    	music_play_bt.setChecked(play_bt_check);
    	
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
        
       animSongSinger = ObjectAnimator.ofPropertyValuesHolder(music_singer_tv, pvhX, pvhY,pvhZ); //渐现
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
	            Toast.makeText(SearchActivity.this,"取消收藏",Toast.LENGTH_SHORT).show();  
         } else  {    	    	  
    	          helperFavorite.addMP3InfoDB(fileInfos.get(listPosition));
    	          fileInfos.get(listPosition).setFavorite(true);	
    	    	  btn_love.setSelected(true);
    	          Toast.makeText(SearchActivity.this,"收藏成功",Toast.LENGTH_SHORT).show();     	    	         		
         }    	  
    }
    
  //用来接收从PlayerService传回来的广播的内部类
    public class PlayerReceiver extends BroadcastReceiver {     	  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            String action = intent.getAction();           
            if(action.equals(PlayerService.UPDATE_ACTION)) {  
                //获取Intent中的current消息，current代表当前正在播放的歌曲  
                listPosition = intent.getIntExtra("current", -1);                    
                if(listPosition >= 0) {                       
                	music_singer_tv.setText(fileInfos.get(listPosition).getArtist());  
                	music_name_tv.setText(fileInfos.get(listPosition).getTitle());  
                	animSongName.start();
                    animSongSinger.start();
                	if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	    	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	    	 }
                }                
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
	          	 anim.resume();//继续旋转
	             animSongName.start();
	             animSongSinger.start();
             }  
        	 music_play_bt.setPressed(play_bt_press);
        	 music_play_bt.setChecked(play_bt_check);    	 
        }    
            setFavoriteDefault();//设置是否歌曲收藏状态
      }            
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
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
			music_singer_tv.setText(fileInfos.get(listPosition).getArtist());  
        	music_name_tv.setText(fileInfos.get(listPosition).getTitle());    
        	music_play_bt.setPressed(play_bt_press);
        	music_play_bt.setChecked(play_bt_check);
		}
	}
    
  //点击列表更改播放状态
    private class MusicListItemClickListener implements OnItemClickListener {    
        @Override  
        public void onItemClick(AdapterView<?> parent, View view, int position,long id) {  
            FileInfo mp3Info = searchlistFromJson.get(position);
            final JsonSong js = new JsonSong();
			js.setSong(mp3Info.getTitle());
			js.setSong_id(String.valueOf(mp3Info.getId()));
			js.setSinger(mp3Info.getArtist());
			js.setAlbum(mp3Info.getUrl());
			
			WebAPI api = new WebAPI();
			dialog.show();
			handler.sendEmptyMessage(0);
			api.songDownload(js.getSong_id(), new SongInfoListener(){			
				@Override
				public void songinfo(JsonSongInfo songinfo) {
					final JsonSongInfo jsi = songinfo;
					new Thread (){
						public void run(){
							HttpDownloader downloader = new HttpDownloader();
							File dir = Environment.getExternalStorageDirectory();
							File musicdir = new File(dir.getAbsolutePath()+"/Music/song");				
							downloader.downFile("http://ting.baidu.com"+jsi.getlrcLink(), musicdir.getAbsolutePath()+"/", jsi.getSongName()+".lrc");	
							int result = downloader.downFile(jsi.getShowLink(), musicdir.getAbsolutePath()+"/", jsi.getSongName()+".mp3");								
							if(result==0){ //0:文件下载成功
								handler.sendEmptyMessage(1);
							}else if(result == 1){ //1:文件已经存在
								handler.sendEmptyMessage(-2);
							}else if(result == -1){ //-1:文件下载出错
								handler.sendEmptyMessage(-1);
							}else {
								handler.sendEmptyMessage(-1);
							}
							 FileInfo mp3Info = new FileInfo();  								
				    		 mp3Info.setId( Integer.valueOf(js.getSong_id()).intValue());  
				             mp3Info.setTitle(js.getSong());  
				             mp3Info.setArtist(js.getSinger());  
				             mp3Info.setDuration(jsi.getSongTime() * 1000);//
				             mp3Info.setSize(jsi.getSongSize());  
				             mp3Info.setUrl(musicdir.getAbsolutePath()+"/" + jsi.getSongName()+".mp3"); 
				             mp3Info.setFavorite(false);
				             appState.addToMediaDB(SearchActivity.this,mp3Info);//加入多媒体数据库
				             helperDownload.addMP3InfoDB(mp3Info); //加入下载数据库
				             List<FileInfo> newFileInfos = MediaUtil.getFileInfos(getApplicationContext()); //获取歌曲对象集合
							appState.setFileInfos(newFileInfos);
						}
					}.start();
				}
				@Override
				public void error() {					
					dialog.cancel();	
					handler.sendEmptyMessage(-1);
				}});
		}    
           
    }  
    
	
	
    protected void JsonToFileInfos(List<JsonSong>searchlist, List<FileInfo> searchlistFromJson){
    	 for(int i=0;i<searchlist.size();i++){
    		 FileInfo mp3Info = new FileInfo();  
    		 mp3Info.setId( Integer.valueOf(searchlist.get(i).getSong_id()).intValue());  
             mp3Info.setTitle(searchlist.get(i).getSong());  
             mp3Info.setArtist(searchlist.get(i).getSinger());  
             mp3Info.setDuration(0);  
             mp3Info.setSize(0);  
             mp3Info.setUrl(searchlist.get(i).getAlbum()); 
             mp3Info.setFavorite(false);
             searchlistFromJson.add(mp3Info);  
    	 }
    }
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		appState = ((MApplication)getApplicationContext());        
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		fileInfos = appState.getFileInfos();
		music_play_bt.setPressed(play_bt_press);
    	music_play_bt.setChecked(play_bt_check);    
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
    
}
