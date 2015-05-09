package com.example.utils;

import android.os.Environment;


public class MusicUtils {
    public static final int DELETE_FAILED = 0x1001;

    public static final int DATA_DELETE = 0x1002;

    public static final int EMPTY_CONTENT = 0x0101;

    public static final int SONG_EXIST = 0x0102;

    public static final int FLIP_INSTANCE = 300;

    public static final int INTERVAL = 6;

    public static final int CONNECT_SUCCEED = 0x0201;

    public static final int CONNECT_FAILED = 0x0202;

    public static final int GETTING_IMAGE = 0x03ff;

    public static final int GETIMAGE_FAILED = 0x03fd;

    public static final int GETTING_FILE_URL = 0x03fe;

    public static final int GETTING_FILE = 0x0300;

    public static final int GETFILE_SUCCEED = 0x0301;

    public static final int GETFILE_FAILED = 0x0302;

    public static final int GETLYRIC_SUCCEED = 0x0303;

    public static final int GETLYRIC_FAILED = 0x0304;

    public static final int UPDATE_IMAGE_SCHEDULE = 0x0401;

    public static final int UPDATE_FILE_SCHEDULE = 0x0402;

    public static final int UPDATE_PROGRESS_SCHEDULE = 0x0403;

    public static final int REQUEST_CODE = 0x0501;

    public static final int RESULT_CODE_ONE = 0x0502;

    public static final int RESULT_CODE_TWO = 0x0503;

    public static final int GETTING_RESOURCE = 0x0601;

    public static final int NEXT_CODE = 0x0701;

    public static final int ERROR_CODE = 0x0702;

    public static final int NOTIFICATION_MUSIC = 0x0800;

    public static final String musicFileDir = Environment.getExternalStorageDirectory()+"/Music";
}
