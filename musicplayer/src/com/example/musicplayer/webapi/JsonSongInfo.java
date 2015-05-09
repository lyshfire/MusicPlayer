package com.example.musicplayer.webapi;

public class JsonSongInfo {
	private String showLink;
	private String songName;
	private String lrcLink;
	private int time;
	private long size;
	public String getShowLink() {
		return showLink;
	}
	public void setShowLink(String showLink) {
		this.showLink = showLink;
	}
	public String getSongName() {
		return songName;
	}
	public void setSongName(String songName) {
		this.songName = songName;
	}
	public String getlrcLink() {
		return lrcLink;
	}
	public void setlrcLink(String lrcLink) {
		this.lrcLink = lrcLink;
	}
	public long getSongTime(){
		return time;
	}
	public void setSongTime(int time){
		this.time = time;
	}
	public long getSongSize(){
		return size;
	}
	public void setSongSize(long size){
		this.size = size;
	}
	
	
}
