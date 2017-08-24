
package com.aliyun.homeshell;

import static android.view.View.LAYER_TYPE_HARDWARE;
import static android.view.View.LAYER_TYPE_NONE;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.aliyun.utility.FeatureUtility;
import static com.aliyun.homeshell.UnlockAnimation.AnimatorType.*;
import static com.aliyun.homeshell.UnlockAnimation.AnimatorState.*;

public class UnlockAnimation {
    private static final boolean DEBUG = "eng".equals(Build.TYPE);
    private static final String TAG = "UnlockAnimation";
    private static final int DURATION = 300;
    private static final boolean SCALE_DOWN_PER_ROW_ROTATE = false;
    private static final float INIT_SCALE = 5f;
    private int mPageIndex;
    private boolean mScreenOn;
    private Launcher mLauncher;
    private Animator mAnimator;
    private Runnable mInitRunnable;
    private AnimatorState mState = Finished;
    private AnimatorType mType = TranslationPerRow;

    private static final float INIT_ALPHA = 0f;
    private int mInitTranslationY = 200;

    static enum AnimatorType {
        ScaleUp, ScaleDown, ScaleDownPerRow, TranslationPerRow
    }

    static enum AnimatorState {
        Inited, Prepared, Started, Finished
    }

    public UnlockAnimation(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setPageIndex(int pageIndex) {
        mPageIndex = pageIndex;
    }

    private void initAnimator() {
        final View root = mLauncher.getDragLayer();
        mInitTranslationY = mLauncher.getResources().getDimensionPixelSize(R.dimen.unlock_view_translation_y);
        final int page = mLauncher.getCurrentScreen();
        final Workspace w = mLauncher.getWorkspace();
        final CellLayout cl = (CellLayout) w.getPageAt(page);
        final ShortcutAndWidgetContainer ca = cl.getShortcutAndWidgetContainer();
        final ArrayList<Animator> as = new ArrayList<Animator>();
        final HashSet<View> hs = new HashSet<View>();
        final View indicator = mLauncher.getIndicatorView();
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(6732199) ##date:2015/12/04
        // ##description: hotseat overlap after unlock in widget page.
        ViewGroup widgetHotSeat = (ViewGroup) w.getWidgetHotseatView(page);
        final ViewGroup hotSeat = (widgetHotSeat != null) ? widgetHotSeat : mLauncher.getHotseat();
        /* YUNOS END PB */
        switch (mType) {
            case ScaleDown:
            case ScaleUp:
                if (mAnimator != null)
                    return;
                final float initVal = mType == AnimatorType.ScaleDown ? 5f : .2f;
                mAnimator = ObjectAnimator.ofPropertyValuesHolder(root,
                        PropertyValuesHolder.ofFloat(View.ALPHA, initVal, 1),
                        PropertyValuesHolder.ofFloat(View.SCALE_X, initVal, 1),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, .2f, 1));
                mAnimator.setDuration(DURATION);
                mAnimator.setInterpolator(new DecelerateInterpolator());
                mInitRunnable = new Runnable() {
                    public void run() {
                        root.setScaleX(initVal);
                        root.setScaleY(initVal);
                        root.setAlpha(.2f);
                        root.setLayerType(LAYER_TYPE_HARDWARE, null);
                    }
                };
                mAnimator.addListener(new FolderUtils.Listener() {
                    public void onAnimationEnd(Animator animation) {
                        root.setScaleX(1f);
                        root.setScaleY(1f);
                        root.setAlpha(1f);
                        root.setLayerType(LAYER_TYPE_NONE, null);
                        mState = Finished;
                    }
                });
                break;
            case ScaleDownPerRow : {
                if (mAnimator != null && mPageIndex == page)
                    return;
                w.setPageSwitchListener(new PagedView.PageSwitchListener() {
                    public void onPageSwitch(View newPage, int newPageIndex) {
                        if (mState == Started) {
                            finish();
                        }
                    }
                });
                int row = 1;
                for (int y = 0, N = cl.getCountY(); y < N; y++) {
                    // for (int y = cl.getCountY() - 1; y >= 0; y--) {
                    final View[] vs = new View[cl.getCountX()];
                    int column = 0;
                    for (int x = 0, M = cl.getCountX(); x < M; x++) {
                        View v = cl.getChildAt(x, y);
                        if (v != null && !hs.contains(v)) {
                            hs.add(v);
                            column++;
                            vs[x] = v;
                        }
                    }
                    if (column > 0) {
                        ValueAnimator va = ValueAnimator.ofFloat(INIT_SCALE, 1);
                        as.add(va);
                        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float val = (Float) animation.getAnimatedValue();
                                float fa = animation.getAnimatedFraction();
                                for (View v : vs) {
                                    if (v != null) {
                                        v.setScaleX(val);
                                        v.setScaleY(val);
                                        if (SCALE_DOWN_PER_ROW_ROTATE)
                                            v.setRotationX((val - 1) * 15);
                                        v.setAlpha(fa);
                                    }
                                }
                            }
                        });
                        va.setDuration(DURATION - row * 20);
                        va.setStartDelay(row * 50);
                        row++;
                    }
                }
                ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(hotSeat,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f),
                        PropertyValuesHolder.ofFloat(View.ALPHA, 1f))
                        .setDuration(DURATION - row * 30);
                oa.setStartDelay(row * 50);
                as.add(oa);

                oa = ObjectAnimator.ofFloat(indicator, View.ALPHA, 1)
                        .setDuration(DURATION - row * 30);
                oa.setStartDelay(row * 50);
                as.add(oa);

                AnimatorSet set = new AnimatorSet();
                set.setInterpolator(new DecelerateInterpolator());
                set.playTogether(as);

                mInitRunnable = new Runnable() {
                    public void run() {
                        cl.setLayerType(LAYER_TYPE_NONE, null);
                        destroyHardwareResources(cl);
                        configLayerClip(false, cl, ca, hotSeat);
                        final int clCenter = (cl.getWidth() - cl.getPaddingLeft() - cl
                                .getPaddingRight()) / 2;
                        for (int i = 0, N = ca.getChildCount(); i < N; i++) {
                            View v = ca.getChildAt(i);
                            v.setPivotX(clCenter - v.getX());
                            v.setPivotY(0);
                            v.setScaleX(INIT_SCALE);
                            v.setScaleY(INIT_SCALE);
                            v.setAlpha(0);
                            v.setLayerType(LAYER_TYPE_HARDWARE, null);
                        }
                        hotSeat.setAlpha(0);
                        hotSeat.setPivotX(hotSeat.getWidth() / 2);
                        hotSeat.setPivotY(0);
                        hotSeat.setScaleX(INIT_SCALE);
                        hotSeat.setScaleY(INIT_SCALE);
                        indicator.setAlpha(0);
                        configLayerType(true, hotSeat, indicator);
                    }
                };
                mAnimator = set;
                mAnimator.addListener(new FolderUtils.Listener() {
                    public void onAnimationStart(Animator animation) {
                        mInitRunnable.run();
                    }

                    public void onAnimationEnd(Animator animation) {
                        w.setPageSwitchListener(null);
                        ShortcutAndWidgetContainer ca = cl.getShortcutAndWidgetContainer();
                        for (int i = 0, N = ca.getChildCount(); i < N; i++) {
                            View v = ca.getChildAt(i);
                            v.setScaleX(1f);
                            v.setScaleY(1f);
                            v.setPivotX(0);
                            v.setAlpha(1f);
                            if (SCALE_DOWN_PER_ROW_ROTATE)
                                v.setRotationX(0);
                            v.setLayerType(LAYER_TYPE_NONE, null);
                        }
                        configLayerClip(true, cl, ca, hotSeat);
                        cl.setLayerType(LAYER_TYPE_HARDWARE, null);
                        hotSeat.setAlpha(1f);
                        hotSeat.setScaleX(1f);
                        hotSeat.setScaleY(1f);
                        indicator.setAlpha(1f);
                        configLayerType(false, hotSeat, indicator);
                        mState = Finished;
                    }
                });
                mPageIndex = page;
            }
                break;

            case TranslationPerRow : {

                if (mAnimator != null && mPageIndex == page)
                    return;
                w.setPageSwitchListener(new PagedView.PageSwitchListener() {
                    public void onPageSwitch(View newPage, int newPageIndex) {
                        if (mState == Started) {
                            finish();
                        }
                    }
                });

                int row = 1;
                for (int y = 0, N = cl.getCountY(); y < N; y++) {
                    // for (int y = cl.getCountY() - 1; y >= 0; y--) {
                    final View[] vs = new View[cl.getCountX()];
                    int column = 0;
                    for (int x = 0, M = cl.getCountX(); x < M; x++) {
                        View v = cl.getChildAt(x, y);
                        if (v != null && !hs.contains(v)) {
                            hs.add(v);
                            column++;
                            vs[x] = v;
                        }
                    }
                    if (column > 0) {
                        ValueAnimator va = ValueAnimator.ofFloat(INIT_ALPHA, 1);
                        as.add(va);
                        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float val = (Float) animation.getAnimatedValue();
                                float fa = animation.getAnimatedFraction();
                                for (View v : vs) {
                                    if (v != null) {
                                        final float translationY = mInitTranslationY * (1 - fa);
                                        v.setAlpha(val);
                                        v.setTranslationY(translationY);
                                    }
                                }
                            }
                        });
                        va.setDuration(DURATION - row * 20);
                        va.setStartDelay(row * 50);
                        row++;
                    }
                }
                ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(hotSeat,
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, mInitTranslationY, 0),
                        PropertyValuesHolder.ofFloat(View.ALPHA, 0, 1f))
                        .setDuration(DURATION - row * 30);
                oa.setStartDelay(row * 50);
                as.add(oa);

                oa = ObjectAnimator.ofPropertyValuesHolder(indicator,
                        PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, mInitTranslationY, 0),
                        PropertyValuesHolder.ofFloat(View.ALPHA, 0, 1f))
                        .setDuration(DURATION - row * 30);
                oa.setStartDelay(row * 50);
                as.add(oa);

                AnimatorSet set = new AnimatorSet();
                set.setInterpolator(new DecelerateInterpolator());
                set.playTogether(as);

                mInitRunnable = new Runnable() {
                    public void run() {
                        cl.setLayerType(LAYER_TYPE_NONE, null);
                        destroyHardwareResources(cl);
                        configLayerClip(false, cl, ca, hotSeat);
                        for (int i = 0, N = ca.getChildCount(); i < N; i++) {
                            View v = ca.getChildAt(i);
                            v.setTranslationY(mInitTranslationY);
                            v.setAlpha(0);
                            v.setLayerType(LAYER_TYPE_HARDWARE, null);
                        }
                        hotSeat.setAlpha(0);
                        hotSeat.setTranslationY(mInitTranslationY);
                        indicator.setAlpha(0);
                        indicator.setTranslationY(mInitTranslationY);
                        configLayerType(true, hotSeat, indicator);
                    }
                };
                mAnimator = set;
                mAnimator.addListener(new FolderUtils.Listener() {
                    public void onAnimationStart(Animator animation) {
                        mInitRunnable.run();
                    }

                    public void onAnimationEnd(Animator animation) {
                        w.setPageSwitchListener(null);
                        ShortcutAndWidgetContainer ca = cl.getShortcutAndWidgetContainer();
                        for (int i = 0, N = ca.getChildCount(); i < N; i++) {
                            View v = ca.getChildAt(i);
                            v.setScaleX(1f);
                            v.setScaleY(1f);
                            v.setPivotX(0);
                            v.setAlpha(1f);
                            v.setTranslationY(0);
                            if (SCALE_DOWN_PER_ROW_ROTATE)
                                v.setRotationX(0);
                            v.setLayerType(LAYER_TYPE_NONE, null);
                        }
                        configLayerClip(true, cl, ca, hotSeat);
                        cl.setLayerType(LAYER_TYPE_HARDWARE, null);
                        hotSeat.setAlpha(1f);
                        hotSeat.setScaleX(1f);
                        hotSeat.setScaleY(1f);
                        hotSeat.setTranslationY(0);
                        indicator.setAlpha(1f);
                        indicator.setTranslationY(0);
                        configLayerType(false, hotSeat, indicator);
                        mState = Finished;
                    }
                });
                mPageIndex = page;
            }
                break;

            default:
                break;
        }
    }

    private void configLayerType(boolean hardware, View... vs) {
        if (hardware) {
            for (View v : vs) {
                v.setLayerType(LAYER_TYPE_HARDWARE, null);
                try {
                    v.buildLayer();
                } catch (IllegalStateException e) {
                    // window may not attached
                    Log.e(TAG, e.toString());
                }
            }
        } else {
            for (View v : vs) {
                v.setLayerType(LAYER_TYPE_NONE, null);
            }
        }
    }

    private static Method sMethodDestroyHardwareResources;
    static {
        try {
            sMethodDestroyHardwareResources = View.class
                    .getDeclaredMethod("destroyHardwareResources");
            sMethodDestroyHardwareResources.setAccessible(true);
        } catch (Exception e) {
        }
    }

    public static void destroyHardwareResources(View v) {
        try {
            if (sMethodDestroyHardwareResources != null)
                sMethodDestroyHardwareResources.invoke(v);
        } catch (Exception e) {
        }
    }

    private void configLayerClip(boolean clip, ViewGroup... vs) {
        for (ViewGroup vg : vs) {
            vg.setClipChildren(clip);
            vg.setClipToPadding(clip);
        }
    }

    public void screenOff() {
        mScreenOn = false;
        if (DEBUG)
            logd("screenOff");
        if (mState == Inited || mState == Prepared)
            return;
        if (mState != Finished)
            finish();
        if (!isTopActivity() || !mLauncher.shouldPlayUnlockAnimation())
            return;
        if (isPlaying())
            mAnimator.cancel();
        mAnimator = null;
        initAnimator();
        mState = Inited;
        if (DEBUG)
            logd("screenOff init");
    }

    public void screenOn() {
        mScreenOn = true;
        if (DEBUG)
            logd("screenOn");
        /* YUNOS BEGIN */
        //##modules(homeshell) BugID: 7821244
        //##date: 2016/01/22 author: xiangnan.xn@alibaba-inc.com
        //# UnlockAnimation can't handle selectFlags from edit mode
        //# it makes selectFlags still showing when UnlockAnimation plays
        if (mLauncher.isInLauncherEditMode())
            mLauncher.getWorkspace().clearSelectFlag();
        /* YUNOS END */
        if (mState == Finished)
            return;
        if (mState == Prepared)
            play();
        else if (!isScreenLocked())
            finish();
        else
            mInitRunnable.run();
    }

    public void standby() {
        /*YUNOS BEGIN*/
        //##modules(homeshell)
        //##BugID:6737617
        //##date: 2015-12.07 author: ruijie.lrj@alibaba-inc.com
        if (isScreenLocked()){
            return;
        }
        /*YUNOS_END*/
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(7762370) ##date:2016/01/09
        // ##description: hotseat overlap after three-finger lock.
        int page = mLauncher.getCurrentScreen();
        ViewGroup widgetHotSeat = (ViewGroup) mLauncher.getWorkspace().getWidgetHotseatView(page);
        final ViewGroup hotSeat = (widgetHotSeat != null) ? widgetHotSeat : mLauncher.getHotseat();
        if (mState == Finished && hotSeat.getTranslationY() != 0
                && !mLauncher.isInLauncherEditMode()) {
            hotSeat.setTranslationY(0);
        }
        /* YUNOS END PB */

        if (mState == Finished || mState == Started)
            return;
        if (!isTopActivity()) {
            if (DEBUG)
                logd("standby not on top");
            if (mState == Inited)
                finish();
            return;
        }
        if (mState == Prepared)
            return;
        if (mState != Inited)
            screenOff();
        // this action has been bind to refresh gadget time and it'll just send
        // to homeshell
        Intent intent = new Intent("com.aliyun.ams.netstate.action.zerohour");
        intent.setPackage(mLauncher.getPackageName());
        mLauncher.sendBroadcast(intent);
        getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
        mLauncher.getDragLayer().postInvalidate();
        scheduleTimeout(1000);
        mLauncher.overridePendingTransition(0, 0);
        mState = Prepared;
        if (DEBUG)
            logd("standby for preDraw");
    }

    public void finish() {
        if (mState == Finished)
            return;
        if (mState == Prepared) {
            unscheduleTimeout();
            getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
        }
        if (mAnimator != null) {
            if (isPlaying())
                mAnimator.cancel();
            mAnimator.getListeners().get(0).onAnimationEnd(mAnimator);
            mLauncher.getWindow().getDecorView().postInvalidate();
        }
        if (DEBUG)
            logd("finish:" + mAnimator);
    }

    public boolean isPlaying() {
        return mAnimator != null && mAnimator.isStarted();
    }

    private void logd(String msg) {
        Log.d(TAG, msg + getCaller());
    }

    private String getCaller() {
        for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
            if (s.getClassName().startsWith("com.aliyun.homeshell.Launcher")) {
                return " [HomeShell." + s.getMethodName() + ':' + s.getLineNumber()
                        + ']';
            }
        }
        return "";
    }

    public void forceStart() {
        if (mAnimator != null && mState == Finished) {
            getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
            mState = Prepared;
        }
        if (DEBUG)
            logd("forceStart:" + mState);
    }

    private ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            // remove onPreDraw callback immediately
            getViewTreeObserver().removeOnPreDrawListener(mOnPreDrawListener);
            if (mAnimator != null) {
                play();
            }
            return true;
        }
    };

    private boolean play() {
        if (mLauncher.isInLauncherEditMode()) {
            mLauncher.exitLauncherEditMode(false);
        }
        if (mScreenOn && mPageIndex == mLauncher.getCurrentScreen()) {
            if (!isPlaying()) {
                unscheduleTimeout();
                mAnimator.start();
                mState = Started;
                if (DEBUG)
                    logd("play");
                return true;
            }
        } else {
            finish();
            if (DEBUG)
                logd("play failed [targetPage:" + mPageIndex + "!=currentPage:"
                        + mLauncher.getCurrentScreen());
        }
        return false;
    }

    private ViewTreeObserver getViewTreeObserver() {
        return mLauncher.getWindow().getDecorView().getViewTreeObserver();
    }

    private Runnable mTimeoutRunnable = new Runnable() {
        public void run() {
            if (DEBUG)
                logd("timeout:" + mAnimator);
            finish();
        }
    };

    public void scheduleTimeout(int timeout) {
        unscheduleTimeout();
        mLauncher.getWorkspace().postDelayed(mTimeoutRunnable, timeout);
        if (DEBUG)
            Log.d(TAG, "scheduleTimeout for " + timeout + " ms");
    }

    public void unscheduleTimeout() {
        try {
            mLauncher.getWorkspace().removeCallbacks(mTimeoutRunnable);
        } catch (Exception e) {
            // getWorkspace() maybe return null
            Log.e(TAG, "error: " + e.toString());
        }
    }

    private boolean isScreenLocked() {
        return ((android.app.KeyguardManager) mLauncher.getSystemService(Context.KEYGUARD_SERVICE))
                .inKeyguardRestrictedInputMode();
    }

    private boolean isTopActivity() {
        try {
            ActivityManager am = (ActivityManager) mLauncher
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> runningTaskInfos = am.getRunningTasks(1);
            return runningTaskInfos != null && runningTaskInfos.size() > 0
                    && runningTaskInfos.get(0).topActivity.getPackageName().equals(
                            mLauncher.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }
}
