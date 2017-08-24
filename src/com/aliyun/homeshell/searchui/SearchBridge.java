package com.aliyun.homeshell.searchui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.utility.utils.ACA;

import yunos.ui.util.ReflectHelper;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

public class SearchBridge {
    private static final String TAG = "HomeShellSearchBridge";
    public static final String LIFECENTER_PKG_NAME = "com.yunos.lifecard";
    private static final String SEARCH_BRIDGE_CLASS_NAME = "com.yunos.alimobilesearch.homeshellui.SearchBridge";

    public static final int ANIMATION_TYPE_HOMESHELL_DROP = 0;
    public static final int ANIMATION_TYPE_LIFECARD_DROP = 1;
    public static final int ANIMATION_TYPE_SLIDE_TO_LIFECARD = 2;
    public static final int ANIMATION_TYPE_SLIDE_TO_HOMESHELL = 3;

    private static final String KEY_ACTION = "action";
    private static final String KEY_TYPE = "type";
    private static final String KEY_PROGRESS = "progress";
    private static final int ACTION_START = 0;
    private static final int ACTION_UPDATE = 1;
    private static final int ACTION_END = 2;

    private static final Object VIEW_TEXT_BLACK_OBJ = getConstant("android.view.View", "SYSTEM_UI_FLAG_TEXT_BLACK");
    private static final int VIEW_TEXT_BLACK = VIEW_TEXT_BLACK_OBJ == null ? 0 : (int)VIEW_TEXT_BLACK_OBJ;
    private static final int ANIM_TYPE_NONE = 0;
    private static final int ANIM_TYPE_X = 1;
    private static final int ANIM_TYPE_Y = 2;
    private float mAinmProgressY = 0.0f;
    private int mAnimType = ANIM_TYPE_NONE;
    private boolean mStatusBarColorBlack = false; //false is white, true is black

    private Method mGetRootView;
    private Method mGetRootViewSuggestHeight;
    private Method mDestoryBridge;
    private Method mOnAnimationStart;
    private Method mOnAnimationUpdate;
    private Method mOnAnimationUpdate2;
    private Method mOnAnimationEnd;

    private Object maObj = null;
    private Context mContext;
    private Context mNativeContext;

    public SearchBridge(Context context) {
        mContext = context;
        build();
    }

    private void build() {
        try {
            mNativeContext = mContext.createPackageContext(LIFECENTER_PKG_NAME,
                    Context.CONTEXT_INCLUDE_CODE
                            | Context.CONTEXT_IGNORE_SECURITY);
            Class<?> mClass = Class.forName(
                    SEARCH_BRIDGE_CLASS_NAME, true,
                    mNativeContext.getClassLoader());
            Constructor<?> con = mClass.getConstructor(Context.class,
                    Context.class);
            maObj = con.newInstance(mNativeContext, mContext);
            mGetRootView = mClass.getDeclaredMethod("getRootView");
            mGetRootViewSuggestHeight = mClass.getDeclaredMethod("getRootViewSuggestHeight");
            mDestoryBridge = mClass.getDeclaredMethod("destroyBridge");
            mOnAnimationStart = mClass.getDeclaredMethod("onAnimationStart", int.class);
            mOnAnimationUpdate = mClass.getDeclaredMethod("onAnimationUpdate", float.class);
            mOnAnimationUpdate2 = mClass.getDeclaredMethod("onAnimationUpdate", int.class, float.class);
            mOnAnimationEnd = mClass.getDeclaredMethod("onAnimationEnd");
        } catch (NameNotFoundException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "build", e);
        }

    }

    public View getRootView() {
        Log.d(TAG, "getRootView");

        View view = null;
        if (mGetRootView == null) {
            Log.e(TAG, "mGetRootView is null");
            return null;
        }

        try {
            view = (View) mGetRootView.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in getRootView : "+e.getMessage());
        }

        return view;
    }

    public int getRootViewSuggestHeight() {
        if (mGetRootViewSuggestHeight == null) {
            return 0;
        }

        int height = 0;
        try {
           height = (int) mGetRootViewSuggestHeight.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in getRootViewSuggestHeight : "+e.getMessage());
        }
        return height;
    }

    public void destoryBridge() {
        if (mDestoryBridge != null) {
            try {
                mDestoryBridge.invoke(maObj);
             } catch (Exception e) {
                 Log.e(TAG, "Failed in destoryBridge : "+e.getMessage());
             }
        }
    }

    public void onAnimationStart(int type) {
        mAnimType = ANIM_TYPE_X;
        //Log.e(TAG, "AAAAA onAnimationStart type=" + type);
        if (mOnAnimationStart != null) {
            try {
                mOnAnimationStart.invoke(maObj, type);
             } catch (Exception e) {
                 Log.e(TAG, "Failed in onAnimationStart : "+e.getMessage());
             }
        }
    }

    public void onAnimationUpdate(float progress) {
        if (mAnimType != ANIM_TYPE_X)  {
            mAnimType = ANIM_TYPE_Y;
        }
        if (mAnimType == ANIM_TYPE_Y)  {
            mAinmProgressY = progress;
            boolean checkStatusBarColor = getStatusBarColor();
            if (mStatusBarColorBlack != checkStatusBarColor) {
                setStatusBarImmersed(checkStatusBarColor);
            }
        }
        //Log.e(TAG, "AAAAA onAnimationUpdate progress=" + progress);
        if (mOnAnimationUpdate != null) {
            try {
                mOnAnimationUpdate.invoke(maObj, progress);
             } catch (Exception e) {
                 Log.e(TAG, "Failed in onAnimationUpdate : "+e.getMessage());
             }
        }
    }

    public void onAnimationUpdate(int type, float progress) {
        if (mAnimType != ANIM_TYPE_X)  {
            mAnimType = ANIM_TYPE_Y;
        }
        if (mAnimType == ANIM_TYPE_Y)  {
            mAinmProgressY = progress;
            boolean checkStatusBarColor = getStatusBarColor();
            if (mStatusBarColorBlack != checkStatusBarColor) {
                setStatusBarImmersed(checkStatusBarColor);
            }
        }
        if (mOnAnimationUpdate2 != null) {
            try {
                mOnAnimationUpdate2.invoke(maObj, type, progress);
             } catch (Exception e) {
                 Log.e(TAG, "Failed in onAnimationUpdate : "+e.getMessage());
             }
        }
    }

        public void onAnimationEnd() {
        if (mAnimType == ANIM_TYPE_X)  {
                if (isInLeftScreen()) {
                    setStatusBarImmersed(getStatusBarColor());
                } else {
                    setStatusBarImmersed(false);
                }
            }
            mAnimType = ANIM_TYPE_NONE;
            //Log.e(TAG, "AAAAA onAnimationEnd progress=");
            if (mOnAnimationEnd != null) {
                try {
                    mOnAnimationEnd.invoke(maObj);
                 } catch (Exception e) {
                     Log.e(TAG, "Failed in onAnimationEnd : "+e.getMessage());
                 }
            }
        }

        // StatusBar cannot refresh sometimes, must do it in different msg.
        private Handler mHandler = new Handler();
        private boolean mBlackOrWhite = false;
        private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                setStatusBarImmersedMsg(mBlackOrWhite);
            }
        };

        private void setStatusBarImmersed(boolean bBlackOrWhite ) {
            if ( mBlackOrWhite != bBlackOrWhite ) {
                mBlackOrWhite = bBlackOrWhite;
                mHandler.post(mRunnable);
            }
        }

        private void setStatusBarImmersedMsg(boolean bBlackOrWhite ) {
            if (ReflectHelper.hasDarkModeFeature()) {
                Activity launcher = LauncherApplication.getLauncher();
                setStatusBarDarkMode(launcher.getWindow(), bBlackOrWhite);
                mStatusBarColorBlack = bBlackOrWhite;
            }
        }

        public static Object getConstant(String className, String constName) {
            Object res = null;
            try {
                Class<?> ownerClass = Class.forName(className);
                Field field = ownerClass.getField(constName);
                res = field.get(ownerClass);
            } catch (ClassNotFoundException e) {
                Log.d(TAG, "e = " + e.toString());
            } catch (NoSuchFieldException e) {
                Log.d(TAG, "e = " + e.toString());
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "e = " + e.toString());
            } catch (IllegalAccessException e) {
                Log.d(TAG, "e = " + e.toString());
            }
            return res;
        }

        public void setStatusBarDarkMode(Window win, boolean darkmode) {
            if (win == null) {
                return;
            }
            View content = win.getDecorView().findViewById(android.R.id.content);
            if (content != null) {
                setSystemUiVisibility(content, darkmode);
            }
        }


        public void setSystemUiVisibility(View content, boolean darkmode) {
            if (content != null) {
                int oldFlag = content.getSystemUiVisibility();
                if (darkmode) {
                    content.setSystemUiVisibility(oldFlag | VIEW_TEXT_BLACK);
                } else {
                    content.setSystemUiVisibility(oldFlag & ~VIEW_TEXT_BLACK);
                }
            }
        }

        private boolean getStatusBarColor() {
            return mAinmProgressY > 0.1f;
        }

        private boolean isInLeftScreen() {
            return LauncherApplication.getLauncher().isInLeftScreen();
        }

        public void updateSearchAnimationProgress(Bundle params) {
            int action = params.getInt(KEY_ACTION);
            switch (action) {
            case ACTION_START:
                //onAnimationStart(params.getInt(KEY_TYPE));
                break;
            case ACTION_UPDATE:
                onAnimationUpdate(params.getInt(KEY_TYPE), params.getFloat(KEY_PROGRESS));
                break;
            case ACTION_END:
                //onAnimationEnd();
                break;
            default:
                break;
            }
        }
        public static boolean isHomeShellSupportGlobalSearchUI(Context context) {
            if (context != null) {
                    PackageManager pm = context.getPackageManager();
                    try {
                          PackageInfo info = pm.getPackageInfo("com.yunos.lifecard",
                          PackageManager.GET_CONFIGURATIONS | PackageManager.GET_META_DATA);
                          if (info != null && info.applicationInfo != null && info.applicationInfo.metaData != null) {
                              return info.applicationInfo.metaData.getBoolean("SupportGlobalSearch");
                          }
                        } catch (NameNotFoundException e) {
                              Log.e(TAG, "get package info error!!!" + e.getMessage());
                        }
                }
                return false;

            }

}
