package com.simpletour.lib.autoupdate;
/**
 * 包名：com.simpletour.lib.autoupdate
 * 描述：自动更新状态码
 * 创建者：yankebin
 * 日期：2016/5/18
 */
public class UpdateStatus {
    public static final int Yes = 0;
    public static final int No = 1;
    public static final int NoneWifi = 2;
    public static final int Timeout = 3;
    public static final int IsUpdate = 4;
    public static final int Update = 5;
    public static final int NotNow = 6;
    public static final int Ignore = 7;
    public static final int STYLE_DIALOG = 0;
    public static final int STYLE_NOTIFICATION = 1;
    public static final int DOWNLOAD_COMPLETE_FAIL = 0;
    public static final int DOWNLOAD_COMPLETE_SUCCESS = 1;
    public static final int DOWNLOAD_NEED_RESTART = 3;

    public UpdateStatus() {
    }
}