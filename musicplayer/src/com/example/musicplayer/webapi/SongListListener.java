package com.example.musicplayer.webapi;

import java.util.List;

public interface SongListListener {
	public void songlist(List<JsonSong>songlist);
	public void error();
}
