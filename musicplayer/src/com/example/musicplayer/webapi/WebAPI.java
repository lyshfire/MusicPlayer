package com.example.musicplayer.webapi;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.AbstractHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.JsonHttpResponseHandler;

public class WebAPI {
	AsyncHttpClient client;
	private static CookieStore cookie;
	public WebAPI(){
		client = new AsyncHttpClient();
		cookie = null;
	}
	public void searchSongs(String songname,final SongListListener listener){
		String path = "http://mp3.baidu.com/dev/api/?tn=getinfo&ct=0&ie=utf-8&format=json";		
        //String path = "http://tingapi.ting.baidu.com/v1/restserver/ting?from=webapp_music&method=baidu.ting.search.catalogSug&format=json&callback=&query=";       
        RequestParams rp = new RequestParams();
		rp.put("word", songname);
		if(cookie!=null){
			client.setCookieStore(cookie);
		}		
		client.get(path, rp, new JsonHttpResponseHandler(){
			public void onSuccess(JSONArray obj){
				super.onSuccess(obj);
				Type listtype = new TypeToken<ArrayList<JsonSong>>(){}.getType();
				Gson gson = new Gson();
				ArrayList<JsonSong> songs = gson.fromJson(obj.toString(), listtype);
				cookie = ((AbstractHttpClient)client.getHttpClient()).getCookieStore();
				if(listener!=null){
					listener.songlist(songs);
				}
				
			}
			public void onFailure(Throwable t, String str){
				super.onFailure(t, str);
				if(listener!=null){
					listener.error();
				}
			}
		});
	}
	public void songDownload(String songid,final SongInfoListener listener){
		String path = "http://ting.baidu.com/data/music/links";
		RequestParams rp = new RequestParams();
		rp.put("songIds", songid);
		client.get(path, rp, new JsonHttpResponseHandler(){
			public void onSuccess(JSONObject obj){
				super.onSuccess(obj);
				if(listener!=null){
					try {
					JSONObject jo = obj.getJSONObject("data");
					if(jo.has("songList")){
												
							JSONArray jarr = jo.getJSONArray("songList");
						
							Gson gson = new Gson();
							Type listtype = new TypeToken<ArrayList<JsonSongInfo>>(){}.getType();
							ArrayList<JsonSongInfo> songs = gson.fromJson(jarr.toString(), listtype);
							if(!songs.isEmpty()){								 
								listener.songinfo(songs.get(0));
							}else{
								listener.error();
							}

					}else{
						listener.error();
					}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						listener.error();
					}
				}
			}
			public void onFailure(Throwable t,String err){
				super.onFailure(t, err);
				if(listener!=null){
					listener.error();
				}
			}
		});
	}
	
}
