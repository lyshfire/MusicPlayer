package com.example.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.example.fileinfo.FileInfo;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

public class MediaUtil {	
	public static List<FileInfo> getFileInfos(Context context) {  
        Cursor cursor = context.getContentResolver().query(  
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,  
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);  
        List<FileInfo> fileInfos = new ArrayList<FileInfo>();  
        for (int i = 0; i < cursor.getCount(); i++) {  
            cursor.moveToNext();  
            FileInfo mp3Info = new FileInfo();  
            long id = cursor.getLong(cursor  
                    .getColumnIndex(MediaStore.Audio.Media._ID));               //音乐id  
            String title = cursor.getString((cursor   
                    .getColumnIndex(MediaStore.Audio.Media.TITLE)));            //音乐标题  
            String artist = cursor.getString(cursor  
                    .getColumnIndex(MediaStore.Audio.Media.ARTIST));            //歌手名  
            long duration = cursor.getLong(cursor  
                    .getColumnIndex(MediaStore.Audio.Media.DURATION));          //时长  
            long size = cursor.getLong(cursor  
                    .getColumnIndex(MediaStore.Audio.Media.SIZE));              //文件大小  
            String url = cursor.getString(cursor  
                    .getColumnIndex(MediaStore.Audio.Media.DATA));              //文件路径         
           
                mp3Info.setId(id);  
                mp3Info.setTitle(title);  
                mp3Info.setArtist(artist);  
                mp3Info.setDuration(duration);  
                mp3Info.setSize(size);  
                mp3Info.setUrl(url); 
                mp3Info.setFavorite(false);
                fileInfos.add(mp3Info);  
        }  
        return fileInfos;  
    }  

	public static List<HashMap<String, String>> getMusicMaps(List<FileInfo> fileInfos) {  
        List<HashMap<String, String>> mp3list = new ArrayList<HashMap<String, String>>();  
        Iterator<FileInfo> iterator = null;
        for (iterator = fileInfos.iterator(); iterator.hasNext();) {  
        	FileInfo mp3Info = (FileInfo) iterator.next();  
            HashMap<String, String> map = new HashMap<String, String>(); 
            map.put("duration", formatTime(mp3Info.getDuration())); 
            map.put("title", mp3Info.getTitle());  
            map.put("artist", mp3Info.getArtist());               
            map.put("size", String.valueOf(mp3Info.getSize()));  
            map.put("url", mp3Info.getUrl());  
            mp3list.add(map);  
        }  
        return mp3list;  
    }  

	 public static String formatTime(long time) {  
	        String min = time / (1000 * 60) + "";  
	        String sec = time % (1000 * 60) + "";  
	        if (min.length() < 2) {  
	            min = "0" + time / (1000 * 60) + "";  
	        } else {  
	            min = time / (1000 * 60) + "";  
	        }  
	        if (sec.length() == 4) {  
	            sec = "0" + (time % (1000 * 60)) + "";  
	        } else if (sec.length() == 3) {  
	            sec = "00" + (time % (1000 * 60)) + "";  
	        } else if (sec.length() == 2) {  
	            sec = "000" + (time % (1000 * 60)) + "";  
	        } else if (sec.length() == 1) {  
	            sec = "0000" + (time % (1000 * 60)) + "";  
	        }  
	        return min + ":" + sec.trim().substring(0, 2);  
	    }  
	 
}
