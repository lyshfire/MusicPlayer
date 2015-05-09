package com.example.musicplayer.webapi;

public interface SongInfoListener {
	public void songinfo(JsonSongInfo songinfo);
	public void error();
}
