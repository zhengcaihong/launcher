package com.aliyun.homeshell.lifecenter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.text.TextUtils;
import android.provider.Settings;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.lifecenter.LifeCenterReceiver.LifecenterListener;
import com.aliyun.homeshell.utils.Utils;

public class CardBridge implements LifecenterListener {

    public static int LEFT_SCREEN_INDEX = 0;
    public static int MAIN_SCREEN_INDEX = 1;

    public static final String LIFECENTER_PKG_NAME = "com.yunos.lifecard";

    private static final boolean isSupportGlobalSearch = true;

    private static final boolean isSupportScrollingBlur = true;

    private static final String TAG = "CardBridge";

    public static final int HOST_CAPABILITY_SCROLLINGBLUR = 0x1;
    public static final int HOST_CAPABILITY_LIFECIRCLE = 0x2;
    public static final int HOST_CAPABILITY_ORIENTATION = 0x4;

    private static final String KEY_EVENT_ID = "event_id";
    private static final String SEARCH_BAR = "search_bar";

    public static final String CLOUDCARD_ENABLE = "cloudcard_enable";
    public static final String AUTHORITY = "com.yunos.lifecard";
    private static final String CLOUDCARD = "cloudcard";

    private Context mNativeContext = null;
    private Object maObj = null;
    private Method mGetRootView = null;
    private Method mEnterApp = null;
    private Method mExitApp = null;
    private Method mIdleApp = null;
    private Method mShowCard = null;
    private Method mShowCardWithIntent = null;
    private Method mAccountChange = null;
    private Method mSetScrolling = null;
    private Method mDispatchActivityResult = null;
    private Method mLifecenterConsumed = null;
    private Method mEnableGlobalPullDown = null;
    private Method mOnCreate = null;
    private Method onDestroy = null;
    private Method mOnPause = null;
    private Method mOnResume = null;
    private Method mSetHostCapability = null;
    private Method mSetListener = null;

    private Method mDestroyHardwareRes = null;
    private String mUrl = null;

    private Intent mUrlIntent = null;
    private Launcher mLauncher;
    private final int CARD_POSITION;

    public CardBridge(Launcher launcher) {
        mLauncher = launcher;
        CARD_POSITION = launcher.getResources().getDisplayMetrics().widthPixels
                * (LEFT_SCREEN_INDEX + 1);

        build();
    }

    private int mCapability = 0;

    private void setHostCapability(int capability) {
        if (mSetHostCapability == null) {
            return;
        }

        Log.d(TAG, "setHostCapability : " + capability);

        try {
            mSetHostCapability.invoke(maObj, capability);
            mCapability = capability;
        } catch (Exception e) {
            Log.e(TAG, "Failed in setHostCapability : " + e.getMessage());
        }
    }

    public void setHostCapabilityEnable(int capBit, boolean enable) {
        int capability = mCapability;
        if (enable) {
            if ((capability & capBit) != 0) {
                return;
            }

            capability |= capBit;
        } else {
            if ((capability & capBit) == 0) {
                return;
            }

            capability &= (~capBit);
        }

        setHostCapability(capability);
    }
    private static final Uri ClOUDCARD_ENABLE_URI = Uri.parse("content://" + AUTHORITY + "/"
            + "lifecard_enable");
    public static boolean getCloudCardEnableValue(Context context) {
        String value = "0";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ClOUDCARD_ENABLE_URI, null, null,
                    null, null);
            if (cursor != null) {
                cursor.moveToNext();
                value = cursor.getString(0);
                Log.d(TAG, "getCloudCardEnableValue : " + value);
            }else{
                Log.d(TAG, "getCloudCardEnableValue : cursor==null");
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed in getCloudCardEnableValue : " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "1".equals(value);
    }
    private static final Uri ClOUDCARD_ENABLE_FLAG_URI = Uri.parse("content://" + AUTHORITY + "/"
            + "lifecard_enable_check_flag");
    public static boolean isCMCC(Context context) {
        String value = "false";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ClOUDCARD_ENABLE_FLAG_URI, null, null,
                    null, null);
            if (cursor != null) {
                cursor.moveToNext();
                value = cursor.getString(0);
                Log.d(TAG, "isCMCC : " + value);
                return "true".equals(value);
            }else{
                Log.d(TAG, "isCMCC : cursor==null");
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed in isCMCC : " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return Utils.isCMCC();
    }

    public static boolean checkLifecenterPackage(Context context) {
        boolean checked = false;

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(LIFECENTER_PKG_NAME,
                    PackageManager.GET_ACTIVITIES);
            if (info != null) {
                checked = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed in checkLifecenterPackage : " + e.getMessage());
        }

        return checked;
    }

    private void build() {
        try {
            Log.d(TAG, "build start.");
            Context context = mLauncher;
            mNativeContext = context.createPackageContext(LIFECENTER_PKG_NAME,
                    Context.CONTEXT_INCLUDE_CODE
                            | Context.CONTEXT_IGNORE_SECURITY);
            Class<?> mClass = Class.forName(
                    LIFECENTER_PKG_NAME + ".CardBridge", true,
                    mNativeContext.getClassLoader());

            Constructor<?> con = mClass.getConstructor(Context.class,
                    Context.class);
            maObj = con.newInstance(mNativeContext, context);

            mGetRootView = mClass.getDeclaredMethod("getRootView");

            mEnterApp = mClass.getDeclaredMethod("enterApp");

            mExitApp = mClass.getDeclaredMethod("exitApp");

            mIdleApp = mClass.getDeclaredMethod("idleApp");

            mShowCard = mClass.getDeclaredMethod("showCard", String.class);

            mShowCardWithIntent = mClass.getDeclaredMethod(
                    "showCardWithIntent", String.class, Intent.class);

            mAccountChange = mClass.getDeclaredMethod("accountChange",
                    boolean.class);

            mSetScrolling = mClass.getDeclaredMethod("setScrolling",
                    boolean.class);
            mDispatchActivityResult = mClass.getDeclaredMethod(
                    "dispatchActivityResult", int.class, int.class,
                    Intent.class);

            mLifecenterConsumed = mClass
                    .getDeclaredMethod("isLifecenterConsumed");

            mSetListener = mClass.getDeclaredMethod("setListener", Object.class);
            mSetListener.invoke(maObj, this);

            if (isSupportGlobalSearch) {
                mEnableGlobalPullDown = mClass
                        .getDeclaredMethod("isEnableGlobalPullDown");
            }

            mOnCreate = mClass.getDeclaredMethod("onCreate");
            mOnResume = mClass.getDeclaredMethod("onResume");
            mOnPause = mClass.getDeclaredMethod("onPause");
            onDestroy = mClass.getDeclaredMethod("onDestroy");

            try {
                mDestroyHardwareRes = View.class
                        .getDeclaredMethod("destroyHardwareResources");
                mDestroyHardwareRes.setAccessible(true);
            } catch (Exception e) {
                Log.d(TAG,
                        "Failed in get destroyHardwareResources : "
                                + e.getMessage());
            }

            try {
                mSetHostCapability = mClass.getDeclaredMethod(
                        "setHostCapability", int.class);
            } catch (Exception e) {
                Log.d(TAG,
                        "CardBridge have not SetCapabilitySwitch : "
                                + e.getMessage());
            }

            int capability = HOST_CAPABILITY_LIFECIRCLE;
            if (ConfigManager.isLandOrienSupport()) {
                capability |= HOST_CAPABILITY_ORIENTATION;
            }

            setHostCapabilityEnable(capability, true);

            Log.d(TAG, "build finished.");
        } catch (Exception e) {
            Log.e(TAG, "Failed in build : " + e.getMessage());
        }
    }

    private int mContainerWidth = 0;

    public int getCardContainerWidth() {
        if (mContainerWidth == 0) {
            try {
                String pkgName = mNativeContext.getPackageName();
                Resources res = mNativeContext.getResources();
                int resId = res.getIdentifier("now_mycard_width", "dimen",
                        pkgName);
                mContainerWidth = res.getDimensionPixelSize(resId);

                resId = res.getIdentifier("now_mycard_padding", "dimen",
                        pkgName);
                mContainerWidth += res.getDimensionPixelSize(resId) * 2;
            } catch (Exception e) {
                mContainerWidth = 1080;
            }
            Log.d(TAG, "getCardContainerWidth : " + mContainerWidth);
        }

        return mContainerWidth;
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
            Log.e(TAG, "Failed in getRootView : " + e.getMessage());
        }

        return view;
    }

    public void enterApp() {
        Log.d(TAG, "enterApp");

        if (mEnterApp == null) {
            Log.e(TAG, "Failed in enterApp : mEnterApp is null.");
            return;
        }

        try {
            mEnterApp.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in enterApp : " + e.getMessage());
        }

        if (mRestart) {
            mLauncher.getLifeCenterReceiver().showPackageUpateDialog();
        }
    }

    public void exitApp() {
        Log.d(TAG, "exitApp");
        if (mExitApp == null) {
            Log.e(TAG, "Failed in exitApp : mExitApp is null.");
            return;
        }

        try {
            mExitApp.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in exitApp : " + e.getMessage());
        }
    }

    public void idleApp() {
        Log.d(TAG, "idleApp");
        if (mIdleApp == null) {
            Log.e(TAG, "Failed in exitApp : mExitApp is null.");
            return;
        }

        try {
            mIdleApp.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in idleApp : " + e.getMessage());
        }
    }

    public void showCard(String card) {
        Log.d(TAG, "ShowCard");
        if (mShowCard == null) {
            Log.e(TAG, "Failed in showCard : mShowCard is null.");
            return;
        }

        try {
            mShowCard.invoke(maObj, card);
        } catch (Exception e) {
            Log.e(TAG, "Failed in showCard : " + e.getMessage());
        }
    }

    public void showCardWithIntent(String card, Intent intent) {
        Log.d(TAG, "showCardWithIntent");
        if (mShowCardWithIntent == null) {
            Log.e(TAG,
                    "Failed in showCardWithIntent : mShowCardWithIntent is null.");
            showCard(card);
            return;
        }

        try {
            mShowCardWithIntent.invoke(maObj, card, intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed in showCardWithIntent : " + e.getMessage());
        }
    }

    public void showCard() {
        showCardWithIntent(mUrl, mUrlIntent);
    }

    public void accountChange(boolean login) {
        Log.d(TAG, "accountChange login : " + login);

        if (mAccountChange == null) {
            return;
        }

        try {
            mAccountChange.invoke(maObj, login);
        } catch (Exception e) {
            Log.e(TAG, "Failed in accountChange : " + e.getMessage());
        }
    }

    public void setScrolling(boolean scrolling) {
        if (mSetScrolling == null) {
            return;
        }

        try {
            mSetScrolling.invoke(maObj, scrolling);
        } catch (Exception e) {
            Log.e(TAG, "Failed in setScrolling : " + e.getMessage());
        }
    }

    public void dispatchActivityResult(final int requestCode,
            final int resultCode, final Intent intent) {
        Log.d(TAG, "dispatchActivityResult");

        if (mDispatchActivityResult == null) {
            return;
        }

        try {
            mDispatchActivityResult.invoke(maObj, requestCode, resultCode,
                    intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed in dispatchActivityResult : " + e.getMessage());
        }
    }

    public boolean isLifecenterConsumed() {
        if (mLifecenterConsumed == null) {
            return false;
        }

        boolean consumed = false;

        try {
            consumed = (Boolean) mLifecenterConsumed.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in isLifecenterConsumed : " + e.getMessage());
        }

        return consumed;
    }

    public boolean isEnableGlobalPullDown() {
        if (mEnableGlobalPullDown == null) {
            return false;
        }

        boolean enable = false;

        try {
            enable = (Boolean) mEnableGlobalPullDown.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in mEnableGlobalPullDown : " + e.getMessage());
        }

        return enable;
    }

    public void onResume() {
        if (mOnResume == null) {
            return;
        }

        try {
            mOnResume.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in mOnResume : " + e.getMessage());
        }

        return;
    }

    public void onPause() {
        if (mOnPause == null) {
            return;
        }

        try {
            mOnPause.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in mOnPause : " + e.getMessage());
        }

        return;
    }

    public void onCreate() {
        if (mOnCreate == null) {
            return;
        }

        try {
            mOnCreate.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in mOnCreate : " + e.getMessage());
        }

        return;
    }

    public void onDestroy() {
        if (onDestroy == null) {
            return;
        }

        try {
            onDestroy.invoke(maObj);
        } catch (Exception e) {
            Log.e(TAG, "Failed in onDestroy : " + e.getMessage());
        }

        return;
    }

    public void enterShowDetailCard(String url, Intent intent) {
        Log.d(TAG, "enterShowDetailCard url : " + url);

        mUrl = url;
        mUrlIntent = intent;
    }

    public void exitShowDetailCard() {
        Log.d(TAG, "exitShowDetailCard url : " + mUrl);

        mUrl = null;
        mUrlIntent = null;
    }

    private boolean mRestart = false;

    public void onPageBeginMoving(int curPage) {
        Log.d(TAG, "onPageBeginMoving curPage : " + curPage);
    }

    public void onPageEndMoving(int curPage, int lastPage) {
        Log.d(TAG, "onMovingEnded curPage : " + curPage + " lastPage : "
                + lastPage);
        if (curPage != LEFT_SCREEN_INDEX) {
            if (lastPage == LEFT_SCREEN_INDEX) {
                exitApp();
                destroyHardWareResources();
            }
        } else {
            if (lastPage != LEFT_SCREEN_INDEX) {
                if (mUrl != null && mUrl.length() > 0) {
                    showCard();
                    mUrl = null;
                    mUrlIntent = null;
                } else {
                    enterApp();
                }
            }
            idleApp();
        }
    }

    private void destroyHardWareResources() {
        if (mDestroyHardwareRes != null) {
            try {
                View cellLayout = (View) mHostRootView.getParent();
                if (cellLayout.getLayerType() == View.LAYER_TYPE_HARDWARE) {
                    mDestroyHardwareRes.invoke(cellLayout);
                    Log.d(TAG, "destroyHardWareResources");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in destroyHardWareResources.");
            }
        }
    }

    private boolean isScrolling = false;

    public void onScrolling(boolean scrolling) {
        if (isScrolling == scrolling) {
            return;
        }

        Log.d(TAG, "onScrolling : " + scrolling);

        isScrolling = scrolling;
        setScrolling(scrolling);
    }

    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        boolean scrolling = (l > -CARD_POSITION && l < 0)
                || (l > 0 && l < CARD_POSITION);

        onScrolling(scrolling);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        View lifeCardRoot = null;
        // child 0 : ShortcutAndWidgetContainer; child 1 : LifeCardsRootView
        try {
            lifeCardRoot = mHostRootView.getChildAt(0);
        } catch (Exception e) {
            Log.e(TAG, "Failed in dispatchKeyEvent : " + e.getMessage());
        }

        Log.d(TAG, "dispatchKeyEvent : " + lifeCardRoot + " event : " + event);

        if (lifeCardRoot != null && lifeCardRoot.dispatchKeyEvent(event)) {
            return true;
        }

        boolean consumed = false;

        int action = event.getAction();
        int keyCode = event.getKeyCode();
        if (action == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
                if (mLauncher.isSupportLifeCenter()) {
                    mLauncher.gotoMainScreen();
                    consumed = true;
                } else if (mLauncher.isSupportLeftScreen()) {
                    mLauncher.closeLeftScreen(false);
                    consumed = true;
                }
                break;
            }
        } else if (action == KeyEvent.ACTION_UP) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
                consumed = true;
                break;
            }
        }

        Log.d(TAG, "dispatchKeyEvent consumed : " + consumed);
        return consumed;
    }

    private static final int KEY_HOME_LIFECENTER = 1001;

    public void dispatchHome(boolean alreadyOnHome) {
        long now = SystemClock.uptimeMillis();
        int key_home = alreadyOnHome ? KeyEvent.KEYCODE_HOME
                : KEY_HOME_LIFECENTER;
        Log.d(TAG, "dispatchHome" + key_home);
        final KeyEvent keyDown = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                key_home, 0);
        mLauncher.getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dispatchKeyEvent(keyDown);
            }
        }, 0);

        mLauncher.getLifeCenterReceiver().dismissDialog();
    }

    @Override
    public void onLifeCardInstalled() {
        mRestart = true;
        if (mLauncher.isInLeftScreen()) {
            mLauncher.getLifeCenterReceiver().showPackageUpateDialog();
        }
    }

    @Override
    public void onAccountChanged(boolean login) {
        accountChange(login);
    }

    private BlurBackground mBlurBackground;
    public View getBlurBackground() {
        if (isSupportScrollingBlur) {
            if (mBlurBackground == null) {
                mBlurBackground = new BlurBackground(mLauncher, this);
            }
        }

        return mBlurBackground;
    }

    public void onWorkspaceScrolled(float progress) {
        if (mBlurBackground != null) {
            mBlurBackground.onWorkspaceScrolled(progress);
        }
    }

    private ViewGroup mHostRootView;
    public void setHostRootView(ViewGroup root) {
        mHostRootView = root;
        if (isShowLeftScreenGuide()) {
            mLauncher.getHandler().post(new Runnable() {
                public void run() {
                    Resources res = mNativeContext.getResources();
                    int resId = res.getIdentifier("btn_right_white_normal", "drawable",
                            mNativeContext.getPackageName());
                    Drawable d = null;
                    if (resId > 0) {
                        d = res.getDrawable(resId);
                    }

                    mGuidView = new LeftScreenGuideView(mLauncher, d);
                    ViewGroup p = (ViewGroup) mHostRootView.getParent();
                    p.addView(mGuidView);
                }
            });
        }
    }

    public void openLeftScreen() {
        float start = mHostRootView.getTranslationX();
        float end = 0;
        Log.d(TAG, "openLeftScreen start : " + start + " end : " + end);

        ValueAnimator va = ValueAnimator.ofFloat(start, end);
        va.setDuration(350);
        va.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                mHostRootView.setTranslationX((Float) arg0.getAnimatedValue());
            }
        });

        va.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mHostRootView.setVisibility(View.VISIBLE);
                setScrolling(true);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                setScrolling(false);
                if (mUrl != null && mUrl.length() > 0) {
                    showCard();
                    mUrl = null;
                    mUrlIntent = null;
                    hideLeftScreenGuide();
                } else {
                    enterApp();
                    removeLeftScreenGuide();
                }
                idleApp();
            }

            @Override
            public void onAnimationCancel(Animator arg0) {

            }
        });

        va.start();
    }

    public void closeLeftScreen() {
        float start = mHostRootView.getTranslationX();
        float end = -mContainerWidth;
        Log.d(TAG, "closeLeftScreen start : " + start + " end : " + end);

        ValueAnimator va = ValueAnimator.ofFloat(start, end);
        va.setDuration(350);
        va.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator arg0) {
                mHostRootView.setTranslationX((Float) arg0.getAnimatedValue());
            }
        });

        va.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setScrolling(true);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setScrolling(false);
                exitApp();
                mHostRootView.setVisibility(View.GONE);
                showLeftScreenGuide();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        va.start();
    }

    private int mDownX = 0;
    private int mDownTransX = 0;

    public boolean handleTouchEvent(MotionEvent e) {
        if (!mLauncher.isSupportLeftScreen()) {
            return false;
        }

        boolean handled = false;
        int x = (int) e.getX();
        int action = e.getAction();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            int transX = (int) mHostRootView.getTranslationX();
            if (!isScrolling()) {
                if (mLauncher.isLeftScreenOpened()) {
                    if (x > mContainerWidth) {
                        handled = true;
                    }
                } else {
                    if (x < 150) {
                        handled = true;
                    }
                }
            }

            if (handled) {
                mHostRootView.setVisibility(View.VISIBLE);
                setScrolling(true);
                mDownX = x;
                mDownTransX = transX;
            } else {
                mDownX = -1;
                mDownTransX = -1;
            }
            Log.d(TAG, "handleTouchEvent mDownX : " + mDownX);
            break;
        case MotionEvent.ACTION_MOVE:
            if (mDownX != -1) {
                int diffX = x - mDownX;
                transX = mDownTransX + diffX;
                transX = checkTranslateX(transX);
                mHostRootView.setTranslationX(transX);
                handled = true;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if (mDownX != -1) {
                transX = (int) mHostRootView.getTranslationX();
                int diffX = x - mDownX;
                Log.d(TAG, "handleTouchEvent UP diffX : " + transX);
                if (Math.abs(diffX) > 5 && transX > -mContainerWidth / 2) {
                    mLauncher.openLeftScreen(true);
                } else {
                    mLauncher.closeLeftScreen(true);
                }
                handled = true;
            }
            break;
        }

        return handled;
    }

    private int checkTranslateX(int transX) {
        if (transX > 0) {
            transX = 0;
        } else if (transX < -mContainerWidth) {
            transX = -mContainerWidth;
        }

        return transX;
    }

    public boolean isScrolling() {
        return isScrolling;
    }

    private LeftScreenGuideView mGuidView;

    public boolean isShowLeftScreenGuide() {
        if (!mLauncher.isSupportLeftScreen()) {
            return false;
        }

        return LeftScreenGuideView.isShow(mLauncher);
    }

    public void showLeftScreenGuide() {
        if (mGuidView == null) {
            return;
        }

        if (mGuidView.getVisibility() != View.VISIBLE) {
            mGuidView.setVisibility(View.VISIBLE);
        }
    }

    public void hideLeftScreenGuide() {
        if (mGuidView != null) {
            mGuidView.setVisibility(View.GONE);
        }
    }

    public void removeLeftScreenGuide() {
        if (mGuidView != null) {
            ViewGroup p = (ViewGroup) mHostRootView.getParent();
            p.removeView(mGuidView);
            LeftScreenGuideView.update(mLauncher, false);
            mGuidView = null;
        }
    }

    public void onNotify(Bundle params) {

              Log.d(TAG, "onNotify:"+params);

              String id = params.getString(KEY_EVENT_ID);

              if (SEARCH_BAR.equals(id)) {

                  mLauncher.updateSearchAnimationProgress(params);
              }
          }
}
