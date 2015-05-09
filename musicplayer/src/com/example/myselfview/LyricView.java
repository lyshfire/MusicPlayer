package com.example.myselfview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.example.utils.MusicUtils;

public class LyricView extends View {
    private List<LyricEntity> lyricEntityList = new ArrayList<LyricEntity>();
    private TextPaint surplusPaint,currentPaint;
    private boolean LyricFlag = false;
    private int index =0;
    private int textSize = 2;

    public LyricView(Context context) {
        super(context);
        init();
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // Set up a default TextPaint object
        surplusPaint = new TextPaint();
        surplusPaint.setTextAlign(Paint.Align.CENTER);
        surplusPaint.setDither(true);
        surplusPaint.setAlpha(180);
        surplusPaint.setColor(Color.WHITE);
        surplusPaint.setTextSize(textSize);

        currentPaint = new TextPaint();
        currentPaint.setTextAlign(Paint.Align.CENTER);
        currentPaint.setDither(true);
        currentPaint.setAlpha(255);
        currentPaint.setColor(Color.YELLOW);
        currentPaint.setTextSize(textSize);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(LyricFlag){
            if(getWidth()/getMax()<25)
                textSize = 25;
            else
                textSize = getWidth()/getMax();
            currentPaint.setTextSize(textSize);
            surplusPaint.setTextSize(textSize);
            LyricEntity entity = lyricEntityList.get(index);
            canvas.drawText(entity.getLyricText(),(getWidth()-entity.getStringLength())/2,getHeight()/2,currentPaint);
            for(int count=index-1,number=1;count>=0;count--,number++){
                entity = lyricEntityList.get(count);
                if((getHeight()/2-(textSize+MusicUtils.INTERVAL)*number)<0)
                    break;
                canvas.drawText(entity.getLyricText(),(getWidth()-entity.getStringLength())/2,
                        getHeight()/2-(textSize+MusicUtils.INTERVAL)*number,surplusPaint);
            }
            for(int count=index+1,number=1;count<lyricEntityList.size();count++,number++){
                entity = lyricEntityList.get(count);
                if((getHeight()/2+(textSize+MusicUtils.INTERVAL)*number)>this.getHeight())
                    break;
                canvas.drawText(entity.getLyricText(),(getWidth()-entity.getStringLength())/2,
                        getHeight()/2+(textSize+MusicUtils.INTERVAL)*number,surplusPaint);
            }
        }
        else{
            currentPaint.setTextSize(35);
            canvas.drawText("没有歌词文件",getWidth()/2,getHeight()/2,currentPaint);
        }

    }

    public void readLyricFile(String file) throws IOException {
        String LyricData;
        File LyricFile = new File(file);
        lyricEntityList.clear();
        if(LyricFile.exists()){
            LyricFlag = true;
        }
        else {
            LyricFlag = false;
            return;
        }
        FileInputStream inputStream = new FileInputStream(LyricFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
        while ((LyricData=reader.readLine())!=null) {
            LyricData = LyricData.replace("[","");
            LyricData = LyricData.replace("]","@");
            String splitData[] = LyricData.split("@");
            String tempData = splitData[0];
            tempData = tempData.replace(":",".");
            tempData = tempData.replace(".","@");
            String timeData[] = tempData.split("@");
            if(timeData.length==3) {
                int minute = Integer.parseInt(timeData[0]);
                int second = Integer.parseInt(timeData[1]);
                int ms = Integer.parseInt(timeData[2]);
                int beginTime = (minute * 60 + second) * 1000 + ms * 10;
                LyricEntity entity = new LyricEntity();
                entity.setBeginTime(beginTime);
                if(LyricData.endsWith("@")){
                    entity.setLyricText("");
                }
                else {
                    entity.setLyricText(splitData[1]);
                }
                lyricEntityList.add(entity);
            }
        }
        inputStream.close();
        getMax();
    }

    public int getMax(){
        if(!LyricFlag){
            return 0;
        }
        int max = lyricEntityList.get(0).getStringLength();
        for(int count = 0;count<lyricEntityList.size();count++) {
            if(max<lyricEntityList.get(count).getStringLength())
                max = lyricEntityList.get(count).getStringLength();
        }
        return max;
    }

    public void SelectIndex(int time){
        int lyricIndex = 0;
        if(!LyricFlag) {
            index=-1;
            return;
        }
        for(int count = 0;count<lyricEntityList.size();count++){
            int beginTime = lyricEntityList.get(count).getBeginTime();
            if(beginTime<time)
                lyricIndex++;
        }
        index = lyricIndex-1;
        if(index<0)
            index = 0;
    }

}
