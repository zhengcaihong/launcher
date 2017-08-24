package com.aliyun.homeshell;

import java.util.ArrayList;

import com.aliyun.homeshell.utils.Utils;

public class FilterList {
    private static ArrayList<String> mBlackList = new ArrayList<String>();
    static{
        mBlackList.add(LauncherApplication.getContext().getPackageName());
        mBlackList.add("com.ykq.vivo.theme");
        mBlackList.add("com.android.launcher");
        mBlackList.add("com.yunos.switchskin");
        //mBlackList.add("com.yunos.assistant");
        if (Utils.isCMCC()) {
            mBlackList.add("com.yunos.assistant");
        }
    }

    public static ArrayList<String> getBlackList(){
        return mBlackList;
    }
}
