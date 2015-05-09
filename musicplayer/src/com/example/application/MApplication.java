package com.example.application;

import java.util.ArrayList;
import java.util.List;

import com.example.database.DBHelper;
import com.example.database.MediaUtil;
import com.example.fileinfo.FileInfo;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

 public class MApplication extends Application {
	 private  String db_nearplaying = "nearplaying_music.db";//数据库名
	 private  String db_favorite = "favorite_music.db";//数据库名
	 private  String db_download = "download_music.db";//数据库名
	 private  DBHelper helper;
	 private  DBHelper helperFavorite;
	 private  DBHelper helperDownload;
	 private List<FileInfo> fileInfos = new ArrayList<FileInfo>();  	 
	 public void onCreate(){
		 fileInfos = MediaUtil.getFileInfos(getApplicationContext()); //获取歌曲对象集合  
		 helper = new DBHelper(this,db_nearplaying);
		 helperFavorite = new DBHelper(this,db_favorite);
		 helperDownload = new DBHelper(this,db_download);
		 		
		}	 
	public List<FileInfo> getFileInfos(){
		    fileInfos = MediaUtil.getFileInfos(getApplicationContext()); //获取歌曲对象集合  
			return fileInfos;
	}
	public void setFileInfos(List<FileInfo> fileInfos){
		this.fileInfos = fileInfos;
   }
	public DBHelper getDBHelper(){
		return helper;
	}
	public DBHelper getDBHelperFavorite(){
		return helperFavorite;
	}
	public DBHelper getDBHelperDownload(){
		return helperDownload;
	}
	 //获取无后缀名的文件名
    public  String getFileNameNoEx(String filename) {   
        if ((filename != null) && (filename.length() > 0)) {   
            int dot = filename.lastIndexOf('.');   
            if ((dot >-1) && (dot < (filename.length()))) {   
                return filename.substring(0, dot);   
            }   
        }   
        return filename;   
    }   
    
    public Uri addToMediaDB(Context context, FileInfo mp3Info) {
		if (context == null) {
			return null;
		}
		ContentValues cv = new ContentValues();				
		cv.put(MediaStore.Audio.Media.IS_MUSIC, "0");
		cv.put(MediaStore.Audio.Media.TITLE, mp3Info.getTitle());
		cv.put(MediaStore.Audio.Media.DATA, mp3Info.getUrl());
		cv.put(MediaStore.Audio.Media.SIZE, mp3Info.getSize());
		cv.put(MediaStore.Audio.Media.DURATION, mp3Info.getDuration());
		cv.put(MediaStore.Audio.Media.MIME_TYPE,"audio/x-mpeg");
		cv.put(MediaStore.Audio.Media.ARTIST,mp3Info.getArtist());		
	
		ContentResolver resolver = context.getContentResolver();
		Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;		
		Uri result = resolver.insert(base, cv);
		// Notify those applications such as Music listening to the
		// scanner events that a recorded audio file just created.
		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,result));
		return result;
	}

    public void deleteMediaDB(Context context, FileInfo mp3Info) {
    	if (context != null) {
    		ContentResolver resolver = context.getContentResolver();
    		Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;		
    		resolver.delete(base, MediaStore.Audio.Media.TITLE+"=?", new String[] { String.valueOf(mp3Info.getTitle())});
    		context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,null));
		}
    }
    
    
    
    
}
