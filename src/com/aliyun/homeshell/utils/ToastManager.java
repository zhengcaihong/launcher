package com.aliyun.homeshell.utils;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;


public class ToastManager {
    public final static int DRAG_ICON_CREATE_FOLDER = 0;
    public final static int NOT_ALLOW_EDIT_IN_DOWNING = 1;
    public final static int APP_NOT_ALLOW_UNINSTALL_IN_USB = 2;
    public final static int APP_UNAVAILABLE_IN_USB = 3;

    public final static int APP_NOT_FOUND = 4;
    public final static int OUT_OF_SPACE = 5;

    public final static int CLOSE_MOBILE_DATA = 6;
    public final static int OPEN_MOBILE_DATA = 7;
    public final static int OPEN_SOUNND_NORMAL_MODE = 8;
    public final static int OPEN_SOUNND_SILENT_MODE = 9;
    public final static int OPEN_SOUNND_VIBRATE_MODE = 10;
    public final static int OPEN_WLAN = 11;
    public final static int CLOSE_WLAN = 12;

    public final static int BOOKMARK_ADDED = 13;
    public final static int BOOKMARK_EXIST = 14;
    public final static int BOOKMARK_UPDATED = 15;
    public final static int CLOUDAPP_ADDED = 16;
    public final static int CLOUDAPP_EXIST = 17;
    public final static int CLOUDAPP_UPDATED = 18;

    public final static int SHORTCUT_ADDED_OR_UPDATED = 19;
    public final static int CLOUDAPP_DELETED = 20;
    public final static int APPLICATION_DELETE_FAILED = 21;
    public final static int APPLICATION_DELETED = 22;
    public final static int BOOKMARK_DELETED = 23;

    public final static int APPLICATION_NOT_SHOW_NO_SPACE = 24;
    public final static int SDCARD_APPLICATION_UNAVAILABLE = 25;
    public final static int APP_IN_UPDATING = 26;
    
    public final static int ADD_WIDGET_SUCCESS = 35;
    public final static int APP_EXIST = 36;
    public final static int NOT_ALLOW_EDIT_IN_RESTORE = 41;

    public final static int APP_UNAVAILABLE_BEING_UNFROZEN = 42;
    public final static int SHORTCUT_UNAVAILABLE_DUE_TO_FROZEN = 43;
    private static final String TAG = "ToastManager";

    public static void makeToast(int id) {
        makeToast(id, (Object[])null);
    }

    // params are message should be passed in to show
    public static void makeToast(int id, Object...params) {
        int resId = -1;

        switch (id) {
            case DRAG_ICON_CREATE_FOLDER :
                resId = R.string.in_edit_staus;
                break;
            case APP_NOT_FOUND :
                resId = R.string.application_unavailable;
                break;
            case OUT_OF_SPACE :
                resId = R.string.out_of_space;
                break;
            case CLOSE_MOBILE_DATA :
                resId = R.string.close_mobile_data;
                break;
            case OPEN_MOBILE_DATA :
                resId = R.string.open_mobile_data;
                break;
            case OPEN_SOUNND_NORMAL_MODE :
                resId = R.string.open_soundmode_normal;
                break;
            case OPEN_SOUNND_SILENT_MODE :
                resId = R.string.open_soundmode_silent;
                break;
            case OPEN_SOUNND_VIBRATE_MODE :
                resId = R.string.open_soundmode_vibrate;
                break;
            case OPEN_WLAN :
                resId = R.string.open_wlan;
                break;
            case CLOSE_WLAN :
                resId = R.string.close_wlan;
                break;
            case NOT_ALLOW_EDIT_IN_DOWNING :
                resId = R.string.allapp_updating_not_allowed_edit;
                break;
            case APP_NOT_ALLOW_UNINSTALL_IN_USB :
                resId = R.string.application_not_deleted_in_usb;
                break;
            case APP_UNAVAILABLE_IN_USB :
                resId = R.string.sdcard_application_unavailable_usb;
                break;
            case BOOKMARK_ADDED:
                resId = R.string.bookmark_add_succeed;
                break;
            case BOOKMARK_EXIST:
                resId = R.string.bookmark_exist;
                break;
            case BOOKMARK_UPDATED:
                resId = R.string.bookmark_updated;
                break;
            case CLOUDAPP_ADDED:
                resId = R.string.cloudapp_add_succeed;
                break;
            case CLOUDAPP_EXIST:
                resId = R.string.cloudapp_exist;
                break;
            case CLOUDAPP_UPDATED:
                resId = R.string.cloudapp_updated;
                break;
            case CLOUDAPP_DELETED:
                resId = R.string.delete_cloudapp_succeed;
                break;
            case APPLICATION_DELETE_FAILED:
                resId = R.string.delete_failed;
                break;
            case APPLICATION_DELETED:
                resId = R.string.delete_succeed;
                break;
            case BOOKMARK_DELETED:
                resId = R.string.delete_bookmark_succeed;
                break;
            case SHORTCUT_ADDED_OR_UPDATED:
                resId = R.string.shortcut_added;
                break;
            case APPLICATION_NOT_SHOW_NO_SPACE:
                resId = R.string.application_not_show_no_space;
                break;
            case SDCARD_APPLICATION_UNAVAILABLE:
                resId = R.string.sdcard_application_unavailable;
                break;
            case APP_IN_UPDATING :
                resId = R.string.app_updating;
                break;
            case ADD_WIDGET_SUCCESS :
                resId = R.string.add_widgets_succes;
                break;
            case APP_EXIST:
                resId = R.string.app_exist;
                break;
            case NOT_ALLOW_EDIT_IN_RESTORE:
                resId = R.string.restore_not_allowed_edit;
                break;
            case APP_UNAVAILABLE_BEING_UNFROZEN:
                resId = R.string.application_unavailable_being_unfrozen;
                break;
            case SHORTCUT_UNAVAILABLE_DUE_TO_FROZEN:
                resId = R.string.shortcut_unavailable_due_to_frozen;
                break;
        }

        if (resId > 0) {
            
            String text = null;
            Context context = LauncherApplication.getContext();
            if (context != null) {
                Resources res = context.getResources();

                if (params == null) {
                    text = res.getString(resId);
                } else {
                    text = res.getString(resId, params);
                }

                Log.d(TAG, "text=" + text);
                if (text != null) {
                    makeToast(context, text);
                }
            }
        }
    }
    /*YUNOS BEGIN*/
    //##date:2013/11/28 ##author:xindong.zxd ##BugId: 67447
    private static void makeToast(Context context, CharSequence text) {
        String className = "android.widget.Toast";
        try {
            Class<?> clz = Class.forName(className);
            Field duration = clz.getField("LENGTH_SHORT");
            int d = duration.getInt(null);
            
            Method m = clz.getMethod("makeText", new Class[] {Context.class, CharSequence.class, int.class});
            Object obj = m.invoke(null, context, text, d);

            m = clz.getMethod("show");
            m.invoke(obj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in makeToast : " + e.getMessage());
        }
    }
    /*YUNOS END*/
}
