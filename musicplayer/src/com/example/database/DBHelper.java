package com.example.database;

import java.util.ArrayList;
import java.util.List;

import com.example.fileinfo.FileInfo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	Context context;//Ӧ�û���������  
	// ���ݿ�汾
    private static final int DATABASE_VERSION = 1;

    //mp3Infos����
    private static final String TABLE_MP3INFOS = "mp3Infos";

    //mp3Infos�������
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "songname";
    private static final String KEY_ARTIST = "singername";
    private static final String KEY_TIME = "songtime";
    private static final String KEY_SIZE = "songsize";
    private static final String KEY_URL = "songurl";
    
    public DBHelper(Context context, String name) {
        super(context, name, null, DATABASE_VERSION);
    }

    // ������
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_MP3INFOS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_TITLE + " TEXT,"
                + KEY_ARTIST + " TEXT," + KEY_TIME+ " LONG," + KEY_SIZE+ " LONG," 
                + KEY_URL+ " TEXT" +")";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    // ���±�
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // ɾ���ɱ�
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MP3INFOS);
        //�ٴδ�����
        onCreate(db);
    }

    // �����µĸ���
    public void addMP3InfoDB(FileInfo mp3Info) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, mp3Info.getTitle());
        values.put(KEY_ARTIST, mp3Info.getArtist());
        values.put(KEY_TIME, mp3Info.getDuration());
        values.put(KEY_SIZE, mp3Info.getSize());
        values.put(KEY_URL, mp3Info.getUrl());
        // ������
        db.insert(TABLE_MP3INFOS, null, values);
        db.close(); // �ر����ݿ������
    }

   
    
    // ��ȡ���и���
    public List<FileInfo> getAllMP3InfoDB() {
        List<FileInfo> mp3InfoList = new ArrayList<FileInfo>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_MP3INFOS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
            	FileInfo mp3Info = new FileInfo();
            	mp3Info.setId(Integer.valueOf(cursor.getString(0)).intValue());  
                mp3Info.setTitle(cursor.getString(1));  
                mp3Info.setArtist(cursor.getString(2));  
                mp3Info.setDuration(Integer.valueOf(cursor.getString(3)).intValue());  
                mp3Info.setSize(Integer.valueOf(cursor.getString(4)).intValue());  
                mp3Info.setUrl(cursor.getString(5)); 
                mp3Info.setFavorite(false);
                mp3InfoList.add(mp3Info);
            } while (cursor.moveToNext());
        }
        return mp3InfoList;
    }
    
 // ��ȡ���и���
    public boolean getMP3Info(FileInfo mp3Info) {    
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_MP3INFOS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {            	            	
                 if(mp3Info.getTitle().equals(cursor.getString(1)) ){
                	return true;
                }          
            } while (cursor.moveToNext());
        }
       return false;
    }
    
    // �������ݿ���
    public int updateMP3InfoDB(FileInfo mp3Info) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, mp3Info.getTitle());
        values.put(KEY_ARTIST, mp3Info.getArtist());
        values.put(KEY_TIME, mp3Info.getDuration());
        values.put(KEY_SIZE, mp3Info.getSize());
        values.put(KEY_URL, mp3Info.getUrl());
        //������
        return db.update(TABLE_MP3INFOS, values, KEY_ID + " = ?",
                new String[] { String.valueOf(mp3Info.getId()) });
    }

    // ɾ��ѡ�����ݿ���
    public void deleteMP3InfoDB(FileInfo mp3Info) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MP3INFOS, KEY_TITLE + " = ?",
                new String[] { String.valueOf(mp3Info.getTitle()) });
        db.close();
    }

 // ɾ��������
    public void deleteALLMP3InfoDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MP3INFOS, null,null);
        db.close();
    }

    // ��ȡ���ݿ�������
    public int getMP3InfoDBCount() {
    	 int count=0;   	
         // Select All Query
         String selectQuery = "SELECT  * FROM " + TABLE_MP3INFOS;

         SQLiteDatabase db = this.getWritableDatabase();
         Cursor cursor = db.rawQuery(selectQuery, null);

         if (cursor.moveToFirst()) {
             do {
             	count++;
             } while (cursor.moveToNext());
         }
         return count;
    	
        //String countQuery = "SELECT  * FROM " + TABLE_MP3INFOS;
       // SQLiteDatabase db = this.getWritableDatabase();
       // Cursor cursor = db.rawQuery(countQuery, null);
      //  cursor.close();
      //  return cursor.getCount();
    }
}

