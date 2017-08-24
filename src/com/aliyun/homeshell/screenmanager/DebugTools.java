package com.aliyun.homeshell.screenmanager;

import android.util.Log;

class DebugTools {
    private static boolean INFO_LOG_SWITCH = true;

    private static Throwable mTempThrowable = new Throwable();

    private DebugTools() {}

    static void log(String msg, Throwable tr) {
        if (INFO_LOG_SWITCH) {
            Log.i(Const.TAG, msg, tr);
        }
    }

    static void log(String msg, boolean callStack) {
        if (INFO_LOG_SWITCH) {
            if (callStack) {
                Log.i(Const.TAG, msg, mTempThrowable);
            } else {
                Log.i(Const.TAG, msg);
            }
        }
    }
}
