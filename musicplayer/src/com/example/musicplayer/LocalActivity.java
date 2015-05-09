package com.example.musicplayer;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.application.MApplication;
import com.example.database.DBHelper;
import com.example.database.MediaUtil;
import com.example.fileinfo.FileInfo;
import com.example.musicplayerservice.PlayerService;
import com.example.myselfview.DisableFavoritePopupWindow;
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
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ToggleButton;

@SuppressLint("NewApi")
public class LocalActivity  extends Activity{
	
	private ListView songlist_lv = null; // �����б�  	
    private List<FileInfo> fileInfos = new ArrayList<FileInfo>();  
    private SimpleAdapter mAdapter = null; // ��������  
    
	private Button return_bt;
	private TextView music_name_tv;
	private TextView music_siger_tv;
	private ToggleButton music_play_bt;
	private Button btn_love;
	private RelativeLayout bottombar_layout;
	private ImageView music_album_iv;
	
	private int listPosition = 0;   //��ʶ�б�λ��
    private boolean isFirstTime = true;   
    private boolean isPlaying; // ���ڲ���  
    private boolean isPause; // ��ͣ 
    private int msg = -1;
    private boolean play_bt_press = false;
    private boolean play_bt_check = false;
    private Intent intentFromMain;
    private PlayerReceiver playerReceiver; //����PlayerService����Ĺ㲥������
    private SharedPreferences preferences;
	private Editor editor;
	
	/***********ͼƬ����**************/
	private ObjectAnimator anim;
	private ObjectAnimator animSongName;
	private ObjectAnimator animSongSinger;
	private LinearInterpolator lin;
	
	/************����****************************/
	GestureDetector  myGestureDetector;
	
	//�Զ���ĵ�������
	DisableFavoritePopupWindow menuWindow;
	private int localListPosition = -1;   //��ʶ�ղ��б�λ��
	private boolean isAllDelete=false;// false��ʾɾ������,true��ʾɾ������
	
	private MApplication appState;
	private  DBHelper helper;
	private  DBHelper helperFavorite;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);// ȥ��������
		setContentView(R.layout.activity_local);
		
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
		helper = appState.getDBHelper();
		helperFavorite = appState.getDBHelperFavorite();
		fileInfos = appState.getFileInfos();
		
		findViewById(); //�ӽ����ϸ���id��ȡ�ؼ�
		setViewOnclickListener(); //��ÿһ���ؼ����ü�����
		initLocalMusicList();//��ʼ�������б�	
		
		playerReceiver = new PlayerReceiver();  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(PlayerService.UPDATE_ACTION);      
        filter.addAction(PlayerService.MUSIC_PLAY_PAUSE);
        registerReceiver(playerReceiver, filter); 
        
	}
    
	//�ӽ����ϸ���id��ȡ��ť
    private void findViewById() { 
    	return_bt = (Button)findViewById(R.id.return_bt);
    	songlist_lv = (ListView)findViewById(R.id.songlist_lv);
    	music_name_tv = (TextView)findViewById(R.id.music_name_tv);
    	music_siger_tv = (TextView)findViewById(R.id.music_siger_tv);   	
    	music_play_bt = (ToggleButton)findViewById(R.id.music_play_bt);
    	music_album_iv = (ImageView)findViewById(R.id.music_album_iv);
    	btn_love            = (Button)findViewById(R.id.btn_love);
    	myGestureDetector = new GestureDetector(new myGestureListener());
    	bottombar_layout = (RelativeLayout)findViewById(R.id.bottombar_layout);
    }
    
  //��ÿһ���ؼ����ü�����
    private void setViewOnclickListener() {  
    	ViewOnClickListener viewOnClickListener = new ViewOnClickListener();  
    	return_bt.setOnClickListener(viewOnClickListener);
    	music_play_bt.setOnClickListener(viewOnClickListener);
    	music_album_iv.setOnClickListener(viewOnClickListener); 
    	btn_love.setOnClickListener(viewOnClickListener); 
    	songlist_lv.setOnItemClickListener(new MusicListItemClickListener()); //�б����
    	songlist_lv.setOnItemLongClickListener(new ListItemLongClickListener());//����ɾ��
    	
    	bottombar_layout.setOnClickListener(viewOnClickListener);
    	bottombar_layout.setOnTouchListener(new OnTouchListener() {	
			//����bottombar_layout�Ĵ����¼�,�����ݸ�GestureDetector
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				myGestureDetector.onTouchEvent(event);
				return false;
			}
		});
    }
    
    private class myGestureListener  extends SimpleOnGestureListener{
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService");
    	//�������ݹ����Ĵ����¼������ж�
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
    	       	         anim.start();//��ʼ��ת
    	       	    }else{
    	       		    anim.resume();//������ת
    	         	}
    	    	} 
    		 play_bt_press = false;
             play_bt_check = true;
             music_play_bt.setPressed(play_bt_press);
             music_play_bt.setChecked(play_bt_check);
    		FileInfo mp3Info = fileInfos.get(listPosition); 
    		music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
            music_name_tv.setText(fileInfos.get(listPosition).getTitle());
	        if(helper.getMP3Info(fileInfos.get(listPosition)) == false)	{	    	    	  
	              helper.addMP3InfoDB(fileInfos.get(listPosition));
	    	 }    	       	         	        	             		 	
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//��msg�����bundle�� 
	    	intentToService.putExtras(bundle);//��bundle�󶨵�intent
	        startService(intentToService); 
    		return super.onFling(e1, e2, velocityX, velocityY);
    	}
    }
    
    //�Զ���ļ�������
    private class ViewOnClickListener implements OnClickListener {   	
    	Intent intentToService = new Intent("com.example.musicplayerservice.PlayerService"); 
		@Override
		public void onClick(View v) {
			switch (v.getId()) {  
			     case R.id.return_bt:   {// �������Ž���				         
			    	    Bundle toMainData = new Bundle();
			    	    toMainData.putInt("listPosition", listPosition);
			    	    toMainData.putBoolean("bt_press", false);
			    	    toMainData.putBoolean("bt_check",isPlaying);
			    	    toMainData.putBoolean("isFirstTime", isFirstTime);
			    	    toMainData.putBoolean("isPlaying", isPlaying);
			    	    toMainData.putBoolean("isPause", isPause);
			    	    toMainData.putInt("msg", msg);
			    	    intentFromMain.putExtras(toMainData);
			            LocalActivity.this.setResult(0, intentFromMain); 
			            LocalActivity.this.finish();
				         break;  }
			     case R.id.btn_love:   {// ��Ӹ������ղؼ�	
			    	 songFavorite();    	 
			         break;  }
			     case R.id.music_album_iv :  { // �������Ž���			          
			         Intent intent = new Intent(LocalActivity.this,PlayingActivity.class);	
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
			     case R.id.music_play_bt:   {//��������
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
		                    Toast.makeText(LocalActivity.this,"��ʼ����",Toast.LENGTH_SHORT).show(); 
		                } else {  
		                    if (isPlaying) {  		                                            
		                        msg = AppConstant.PlayerMsg.PAUSE_MSG;   
		                        isPlaying = false;  
		                        isPause = true;   
		                        play_bt_press = false;
				                play_bt_check = false;
		                        anim.pause();	
		                        Toast.makeText(LocalActivity.this,"��ͣ����  ",Toast.LENGTH_SHORT).show();  
		                    } else if (isPause) {  
		                        msg = AppConstant.PlayerMsg.CONTINUE_MSG;   
		                        isPause = false;  
		                        isPlaying = true;  
		                        play_bt_press = false;
				                play_bt_check = true;
				                if(!anim.isStarted()){
					       	         anim.start();//��ʼ��ת
					       	    }else{
					       		    anim.resume();//������ת
					         	}
				                //animSongName.start();
				    	        //animSongSinger.start();
		                        Toast.makeText(LocalActivity.this,"��������",Toast.LENGTH_SHORT).show();  
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
			    	   bundle.putInt("MSG", msg);//��msg�����bundle�� 
			    	   intentToService.putExtras(bundle);//��bundle�󶨵�intent
			           startService(intentToService); 
			           break; }
			}
			
		}     	
    }
    //��ʼ�������б�
    private void initLocalMusicList(){ 
    	//fileInfos = MediaUtil.getFileInfos(getApplicationContext()); //��ȡ�������󼯺�         
        setListAdpter(MediaUtil.getMusicMaps(fileInfos));//��ʾ�����б�        
        music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
    	music_name_tv.setText(fileInfos.get(listPosition).getTitle());    
    	music_play_bt.setPressed(play_bt_press);
    	music_play_bt.setChecked(play_bt_check);
    	
    	anim = ObjectAnimator.ofFloat(music_album_iv, "rotation", 0, 360);  
    	 lin = new LinearInterpolator();
         anim.setDuration(6000);
         anim.setRepeatCount(-1);
         anim.setRepeatMode(ObjectAnimator.RESTART);
         anim.setInterpolator(lin);  //���� 
         
         PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("alpha", 1f, 0f, 1f);  
         PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleX", 1f, 0, 1f);  
         PropertyValuesHolder pvhZ = PropertyValuesHolder.ofFloat("scaleY", 1f, 0, 1f); 
         animSongName = ObjectAnimator.ofPropertyValuesHolder(music_name_tv, pvhX, pvhY,pvhZ); //����
         animSongName.setDuration(1000);
         animSongName.setRepeatCount(0);
         animSongName.setRepeatMode(ObjectAnimator.RESTART);
         
        animSongSinger = ObjectAnimator.ofPropertyValuesHolder(music_siger_tv, pvhX, pvhY,pvhZ); //����
        animSongSinger.setDuration(1000);
        animSongSinger.setRepeatCount(0);
        animSongSinger.setRepeatMode(ObjectAnimator.RESTART); 
    }
  //��ʾ�����б�
    public   void setListAdpter(List<HashMap<String, String>> mp3list) {  
        if(mp3list != null){
        	mAdapter = new SimpleAdapter(LocalActivity.this, mp3list,  
                    R.layout.music_list_item_layout, new String[] {"duration","title","artist"}, 
                    new int[] {R.id.music_duration,R.id.music_title,R.id.music_artist });  
            songlist_lv.setAdapter(mAdapter);
            Toast.makeText(LocalActivity.this, "����б�ɹ�"+fileInfos.size(), Toast.LENGTH_SHORT).show();
        }else{
        	Toast.makeText(LocalActivity.this, "�����б�û�и���", Toast.LENGTH_SHORT).show();
        }       
    }
    
    //����б���Ĳ���״̬
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
            music_play_bt.setPressed(play_bt_press);
            music_play_bt.setChecked(play_bt_check);
            music_siger_tv.setText(mp3Info.getArtist());  
            music_name_tv.setText(mp3Info.getTitle());	    
	        if(helper.getMP3Info(mp3Info) == false)	{	    //��ӵ���������б�	    	  
	              helper.addMP3InfoDB(mp3Info);
	    	 } 	        
           
	        /*************������ת*********************************/
	        if (anim != null  &&  isPlaying == true) {  
	        	if(!anim.isStarted()){
	       	         anim.start();//��ʼ��ת
	       	    }else{
	       		    anim.resume();//������ת
	         	}
	    	} else{
	    		anim.pause(); //ֹͣ��ת
	    	}        
	        //animSongName.start();
	        //animSongSinger.start();
	        
	        Bundle bundle  = new Bundle();
	        bundle.putInt("listPosition", listPosition);  
	        bundle.putString("url", mp3Info.getUrl());
	    	bundle.putInt("MSG", msg);//��msg�����bundle�� 
	    	intentToService.putExtras(bundle);//��bundle�󶨵�intent
	        startService(intentToService); 
        }   
    }        
    
    private class ListItemLongClickListener implements OnItemLongClickListener{
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int pos,long id) {  
			localListPosition = pos;			
			//ʵ����SelectPicPopupWindow
			menuWindow = new DisableFavoritePopupWindow(LocalActivity.this,R.layout.delete_music_dialog,itemsOnClick,RadioGroupOnClick);
			//��ʾ����
			menuWindow.showAtLocation(LocalActivity.this.findViewById(R.id.RelativeLayoutLocalActivity), Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0); //����layout��PopupWindow����ʾ��λ��
//			FileInfo mp3Info = mp3InfoLoveList.get(position);		
//			helperFavorite.deleteMP3InfoDB(mp3Info);
//			for(int i=0;i<fileInfos.size();i++){
//		          if(fileInfos.get(i).getTitle().equals(mp3Info.getTitle())){
//		        	  fileInfos.get(i).setFavorite(false);	
//		          }
//			}
//			if(mp3InfoLoveList.remove(position) != null){
//				 setListAdpter(MediaUtil.getMusicMaps(mp3InfoLoveList));
//			     return true;
//			}
			
			
			return false;
		}   	
    }
    
  //�����Ƿ�����ղ�״̬
    private void setFavoriteDefault(){
		 if(helperFavorite.getMP3Info(fileInfos.get(listPosition))){	    	    	  
	        	btn_love.setSelected(true);
	     }else{
	    		 btn_love.setSelected(false);
	      }
    }
  //�����ղ���ȡ���ղ�
    private void songFavorite(){    	
    	  if(helperFavorite.getMP3Info(fileInfos.get(listPosition))){					
				helperFavorite.deleteMP3InfoDB(fileInfos.get(listPosition));
				for(int i=0;i<fileInfos.size();i++){
			          if(fileInfos.get(i).getTitle().equals(fileInfos.get(listPosition).getTitle())){
			        	  fileInfos.get(i).setFavorite(false);	
			          }
				}      	       
	            btn_love.setSelected(false);
	            Toast.makeText(LocalActivity.this,"ȡ���ղ�",Toast.LENGTH_SHORT).show();  
         } else  {    	    	  
    	          helperFavorite.addMP3InfoDB(fileInfos.get(listPosition));
    	          fileInfos.get(listPosition).setFavorite(true);	
    	    	  btn_love.setSelected(true);
    	          Toast.makeText(LocalActivity.this,"�ղسɹ�",Toast.LENGTH_SHORT).show();     	    	         		
         }    	  
    }
    
    
  //�������մ�PlayerService�������Ĺ㲥���ڲ���
    public class PlayerReceiver extends BroadcastReceiver {     	  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            String action = intent.getAction();           
            if(action.equals(PlayerService.UPDATE_ACTION)) {  
                //��ȡIntent�е�current��Ϣ��current����ǰ���ڲ��ŵĸ���  
                listPosition = intent.getIntExtra("current", -1);                    
                if(listPosition >= 0) {                       
                	music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
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
	          	 anim.resume();//������ת
	             animSongName.start();
	             animSongSinger.start();
             }  
        	 music_play_bt.setPressed(play_bt_press);
        	 music_play_bt.setChecked(play_bt_check);    	 
        }  
            setFavoriteDefault();//�����Ƿ�����ղ�״̬
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
			music_siger_tv.setText(fileInfos.get(listPosition).getArtist());  
        	music_name_tv.setText(fileInfos.get(listPosition).getTitle());    
        	music_play_bt.setPressed(play_bt_press);
        	music_play_bt.setChecked(play_bt_check);       	
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
    	setFavoriteDefault();//�����Ƿ�����ղ�״̬
		 if (anim != null  &&  isPlaying == true) {  
	        	if(!anim.isStarted()){
	       	         anim.start();//��ʼ��ת
	       	    }else{
	       		    anim.resume();//������ת
	         	}
	    	} else{
	    		anim.pause(); //ֹͣ��ת
	    	}
	}

    
	//Ϊ��������ʵ�ּ�����
    private OnClickListener  itemsOnClick = new OnClickListener(){
		public void onClick(View v) {
			menuWindow.dismiss();
			switch (v.getId()) {
			case R.id.btn_cancel:
				break;
			case R.id.btn_ok:	{	
				if(!isAllDelete){ //ɾ�����׸���			
					 deleteSdFile(fileInfos.get(localListPosition).getUrl());//ɾ�������͸���ļ�
					 appState.deleteMediaDB(LocalActivity.this,fileInfos.get(localListPosition));//ɾ�����ݿ�ļ�¼
					 fileInfos.remove(localListPosition);//ɾ���б���
					 setListAdpter(MediaUtil.getMusicMaps(fileInfos));	//������ʾ		
				}
				break;}
			default:
				break;
			}				
		}   	
    };
    
    private OnCheckedChangeListener RadioGroupOnClick = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			// TODO Auto-generated method stub
			if (checkedId == R.id.rb_all) {
				isAllDelete = true;
				menuWindow.getView().setText("������ʶ�ɾ����?");
			}else{
				isAllDelete = false;
				menuWindow.getView().setText("ɾ��\"" + fileInfos.get(localListPosition).getTitle() +"\"?" + "\r"
				                                                  + fileInfos.get(localListPosition).getUrl() );
			}
		}  	
    };
    private void deleteSdFile(String path){
    	 File deleteMp3File = new File(	path); 	
    	 File deleteLyricFile = new File(appState.getFileNameNoEx(path) + ".lrc"); 	
		 deleteMp3File.delete(); //ɾ�������ļ�
		 deleteLyricFile.delete();//ɾ������ļ�
    }
    
}
