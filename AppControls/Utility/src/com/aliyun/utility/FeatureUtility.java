package com.aliyun.utility;

import com.aliyun.utility.utils.ACA;

public class FeatureUtility {
    private static final String STR_YES = "yes";
    private static final String STR_NO = "no";
    private static final String STR_TRUE = "true";
    private static final String STR_FALSE = "false";
    private static final String STR_UNKOWN = "unknown";
    private static final boolean SHORTCUT_FEATURE = true;
    private static final boolean BIGICON_FEATURE = true;
    private static final boolean BIGCARD_FEATURE = true;
    private static final boolean AGEDMODE_FEATURE = true;
    private static final boolean HIDESEAT_FEATURE = true;
    private static final boolean INTERNATIONAL_FEATURE = true;
    private static final boolean NOTIFICATION_WIDGET_FEATURE = true;
    private static final boolean FULLSCREENWIDGET_FEATURE = true;
    private static final boolean EFFECT_3D_FEATURE = true;
    private static final boolean PUSHTALK_FEATURE = true;
    private static final boolean DOUBLE_CLICK_FEATURE = true;
    private static final boolean ARRANGE_CURRENTSCREEN_FEATURE = true;
    private static final boolean ARRANGE_ALLSCREEN_FEATURE = false;
    private static final boolean ARRANGE_CLASSIFY_FEATURE = false;
    private static final boolean SCREENEDIT_FEATURE = true;
    private static final boolean THREEFINGERLOCK_FEATURE = true;
    private static final boolean FLINGICON_FEATURE = true;
    private static final boolean PULLDOWNSEARCH_FEATURE = true;
    private static final boolean LANDSCAPE_SUPPORT_FEATURE = true;
    private static final boolean ICON_DYNC_COLOR = true;

    private static final boolean sShortcutFeature = STR_YES.equals(ACA.SystemProperties
            .get("ro.yunos.support.shortcut", STR_UNKOWN)) && SHORTCUT_FEATURE;
    private static final boolean sBigIconFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.bigicon", STR_TRUE)) && BIGICON_FEATURE;
    private static final boolean sBigCardFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.bigcard", STR_TRUE)) && !isYunOS2_9System() && BIGCARD_FEATURE;
    private static final boolean sAgedModeFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.agedmode", STR_TRUE)) && AGEDMODE_FEATURE;
    private static final boolean sHideSeatFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.hideseat", STR_TRUE)) && HIDESEAT_FEATURE && !isYunOS2_9System();
    private static final boolean sNotigicationWidgetFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.notiwidget", STR_TRUE)) && NOTIFICATION_WIDGET_FEATURE;
    private static final boolean sYunOS29Feature = ACA.SystemProperties
            .get("ro.yunos.version").startsWith("2.9");
    private static final boolean sInternationalFeature = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.international")) && INTERNATIONAL_FEATURE;
    private static final boolean sCTAFeature = STR_YES.equals(ACA.SystemProperties
            .get("ro.yunos.support.cta", STR_NO));
    private static final boolean sFullScreenWidget = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.fullwidget", STR_UNKOWN)) && FULLSCREENWIDGET_FEATURE;
    private static final boolean sEffect3D = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.effect3d", STR_UNKOWN)) && EFFECT_3D_FEATURE;
    private static final boolean sPushTalk = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.pushtalk", STR_UNKOWN)) && PUSHTALK_FEATURE;
    private static final boolean sScreenEdit = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.screenedit", STR_TRUE)) && SCREENEDIT_FEATURE;
    private static final boolean sThreeFingerLock = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.thrfingerlock", STR_TRUE)) && THREEFINGERLOCK_FEATURE;
    private static final boolean sFlingIcon = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.flingicon", STR_TRUE)) && FLINGICON_FEATURE;
    private static final boolean sPullDownSearch = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.pulldownsearch", STR_TRUE)) && PULLDOWNSEARCH_FEATURE;
    private static final boolean sLandscapeSupport = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.fulllandscape", STR_FALSE)) && LANDSCAPE_SUPPORT_FEATURE;
    private static final boolean sBranchM = ACA.SystemProperties.get("ro.product.model", "").endsWith("_M");
    
    // Xunhu start: Modified by yangj on 20161031, set sIconDyncColor=false as default  @{{{
    // private static final boolean sIconDyncColor = STR_TRUE.equals(ACA.SystemProperties
    //         .get("ro.yunos.support.dynccolor", STR_TRUE)) && ICON_DYNC_COLOR;
    private static final boolean sIconDyncColor = STR_TRUE.equals(ACA.SystemProperties
            .get("ro.yunos.support.dynccolor", STR_FALSE)) && ICON_DYNC_COLOR;
    // Xunhu end: Modified by yangj on 20161031  @}}}

    public static boolean isLandscapeSupport() {
        return sLandscapeSupport;
    }

    public static boolean hasShortCutFeature() {
        return sShortcutFeature;
    }

    public static boolean hasBigIconFeature() {
        return sBigIconFeature;
    }

    public static boolean hasBigCardFeature() {
        return sBigCardFeature;
    }

    public static boolean hasAgedModeFeature() {
        return sAgedModeFeature;
    }

    public static boolean hasHideSeatFeature() {
        return sHideSeatFeature;
    }
    public static boolean hasNotificationFeature() {
        return sNotigicationWidgetFeature;
    }

    public static boolean isYunOS2_9System() {
        return sYunOS29Feature;
    }

    public static boolean isYunOSInternational() {
        return sInternationalFeature;
    }

    public static boolean isYunOSForCTA() {
        return sCTAFeature;
    }

    public static boolean hasFullScreenWidget() {
        return sFullScreenWidget;
    }

    public static boolean has3dEffect() {
        return sEffect3D;
    }

    public static boolean hasPushTalk() {
        return sPushTalk;
    }

    public static boolean hasDoubleClickFeature() {
        return DOUBLE_CLICK_FEATURE;
    }

    public static boolean hasArrangeCurrentScreenFeature() {
        return ARRANGE_CURRENTSCREEN_FEATURE;
    }

    public static boolean hasArrangeAllScreenFeature() {
        return ARRANGE_ALLSCREEN_FEATURE;
    }

    public static boolean hasArrangeClassifyFeature() {
        return ARRANGE_CLASSIFY_FEATURE;
    }

    public static boolean hasScreenEditFeature() {
        return sScreenEdit;
    }

    public static boolean hasThreeFingerLockFeature() {
        return sThreeFingerLock;
    }

    public static boolean hasFlingIconFeature() {
        return sFlingIcon;
    }

    public static boolean hasPullDownSearchFeature() {
        return sPullDownSearch;
    }

    public static boolean isBranchM() {
        return sBranchM;
    }

    public static boolean supportDyncColor() {
        return sIconDyncColor;
    }
}
