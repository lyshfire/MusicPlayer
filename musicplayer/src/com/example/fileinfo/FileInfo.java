package com.example.fileinfo;

public class FileInfo {
	private long id;//����id
	private String title;//���ֱ���
	private String artist;//������
	private long duration;//ʱ�� 
	private long size;//�ļ���С  
	private String url; //�ļ�·�� 
	private boolean inFavorite; // �Ƿ񱻼����ղؼ�
	
	 public void setId(long id) {
	        this.id = id;
	    }
	 public void setTitle(String title) {
	        this.title = title;
	    }
	 public void setArtist(String artist) {
	        this.artist = artist;
	    }
	 public void setDuration(long duration) {
	        this.duration = duration;
	    }
	 public void setSize(long size) {
	        this.size = size;
	    }
	 public void setUrl(String url) {
	        this.url = url;
	    }
	 public void setFavorite(boolean inFavorite) {
	        this.inFavorite = inFavorite;
	    }
	 public long getId() {
	        return id;
	    }
	 public String getTitle() {
	        return title;
	    }
	 public String getArtist() {
		    return artist;
	    }
	 public long getDuration() {
	        return duration;
	    }
	 public long getSize() {
	        return size;
	    }
	 public String getUrl() {
	        return url;
	    }
	 public boolean getFavorite() {
	        return inFavorite;
	    }
}
