package com.aliyun.homeshell.utils;

import android.util.Log;

public class HLog {
    private static final boolean DEBUG_D = true;
    private static final boolean DEBUG_I = true;
    private static final boolean DEBUG_E = true;
    
    public static void d(String tag, String msg){
        if (DEBUG_D) {
            Log.d(tag, msg);
        }
    }
    
    public static void e(String tag, String msg){
        if (DEBUG_E) {
            Log.e(tag, msg);
        }
    }
    
    public static void i(String tag, String msg){
        if (DEBUG_I) {
            Log.i(tag, msg);
        }
    }
}
