package com.example.musicplayer.webapi;

public class JsonSong {
//	private String songid;
//	private String songname;
//	private String encrypted_songid;
//	private String has_mv;
//	private String yyr_artist;
//	private String artistname;
//	private String control;
//	public String getSong() {
//		return songname;
//	}
//	public void setSong(String songname) {
//		this.songname = songname;
//	}
//	public String getSong_id() {
//		return songid;
//	}
//	public void setSong_id(String songid) {
//		this.songid = songid;
//	}
//	public String getSinger() {
//		return artistname;
//	}
//	public void setSinger(String artistname) {
//		this.artistname = artistname;
//	}
//	public String getAlbum() {
//		return yyr_artist;
//	}
//	public void setAlbum(String yyr_artist) {
//		this.yyr_artist = yyr_artist;
//	}
//	
	
	
	private String song;
	private String song_id;
	private String singer;
	private String album;
	public String getSong() {
		return song;
	}
	public void setSong(String song) {
		this.song = song;
	}
	public String getSong_id() {
		return song_id;
	}
	public void setSong_id(String song_id) {
		this.song_id = song_id;
	}
	public String getSinger() {
		return singer;
	}
	public void setSinger(String singer) {
		this.singer = singer;
	}
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	
}
