package com.aliyun.homeshell;

import com.aliyun.utility.FeatureUtility;

import android.graphics.Matrix;
import android.provider.Settings;

public class AgedModeUtil {
    private static boolean sIsAgedMode = false;
    private static final int SETTINGS_AGED_MODE = 1;
    public static final int AGED_MODE_FLAG_IN_MSG = 1;
    public static boolean isAgedMode() {
        return sIsAgedMode;
    }

    public static final String TAG = "AgedModeUtil.log";

    public static void setAgedMode(boolean isAgedMode) {
        if(FeatureUtility.hasAgedModeFeature()){
            sIsAgedMode = isAgedMode;
        }
    }

    public static float SCALE_RATIO_FOR_AGED_MODE = 1.25f;

    public static Matrix sScaleUp = null;
    public static Matrix sScaleDown = null;

    static {
        if(FeatureUtility.hasAgedModeFeature()){
            sIsAgedMode = (SETTINGS_AGED_MODE == Settings.Secure.getInt(
                    LauncherApplication.getContext().getContentResolver(), "aged_mode", 0));
            sScaleUp = new Matrix();
            sScaleDown = new Matrix();
            sScaleUp.setScale(SCALE_RATIO_FOR_AGED_MODE, SCALE_RATIO_FOR_AGED_MODE);
            sScaleDown.setScale(1 / SCALE_RATIO_FOR_AGED_MODE, 1 / SCALE_RATIO_FOR_AGED_MODE);
        }else{
            sIsAgedMode = false;
        }
    }

}
