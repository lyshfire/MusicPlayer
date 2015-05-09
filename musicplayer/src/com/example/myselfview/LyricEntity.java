package com.example.myselfview;

public class LyricEntity {
    private int beginTime;
    private String lyricText;

    public LyricEntity(){
    }

    public int getBeginTime(){
        return beginTime;
    }

    public String getLyricText(){
        return lyricText;
    }

    public void setBeginTime(int beginTime){
        this.beginTime = beginTime;
    }

    public void setLyricText(String lyricText) {
        this.lyricText = lyricText;
    }

    public int getStringLength(){
        return  lyricText.length();
    }
}
