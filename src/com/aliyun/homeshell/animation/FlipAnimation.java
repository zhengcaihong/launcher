package com.aliyun.homeshell.animation;


import aliyun.util.ImageUtils;

import app.aliyun.v3.gadget.GadgetView;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.FolderUtils;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherAnimUtils;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.utility.utils.ACA;

/**
 * Created by wenliang.dwl on 14-7-25.
 */
public class FlipAnimation extends BroadcastReceiver{

    public static final String TAG = "FlipAnimation";

    private Launcher mContext;
    private View mDownView;
    private View mUpView;
    private View mBackground;
    private View mTargetBubble;
    private boolean mIsShowing;
    private boolean mIsWaitingForStart;
    private BlurTask mBlurTask;

    private Animator mStartStageOfSmallAnimator;
    private Animator mStartStageOfBigAnimator;
    private Animator mEndStageOfSmallAnimator;
    private Animator mEndStageOfBigAnimator;
    private Animator mBackgroundAlphaAnimator;

    private static final long INTERVAL_DOWN = 180;
    private static final long INTERVAL_UP = 320;

    private float mDownViewStartX;
    private float mDownViewStartY;
    private float mDownViewFinalX;
    private float mDownViewFinalY;

    private float mUpViewStartX;
    private float mUpViewStartY;
    private float mUpViewFinalX;
    private float mUpViewFinalY;

    private DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    public final static String CLOSE_ACTION = "com.aliyun.homeshell.CLOSE_CARD";
    public final static String FAST_CLOSE_ACTION = "com.aliyun.homeshell.FAST_CLOSE_CARD";

    public FlipAnimation(Launcher launcher){
        mContext = launcher;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CLOSE_ACTION);
        intentFilter.addAction(FAST_CLOSE_ACTION);
        mContext.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null && isShowing() && !isAnimating()) {
            if (FAST_CLOSE_ACTION.equals(intent.getAction())) {
                clear();
            } else if (CLOSE_ACTION.equals(intent.getAction())) {
                disappear();
            }
        }
    }

    public FlipAnimation(View down, View up){
        mDownView = down;
        mUpView = up;
        mIsShowing = false;
        mIsWaitingForStart = false;
        computeValues();
    }

    public void setIsWaiting(boolean isWaiting){
        mIsWaitingForStart = isWaiting;
    }

    public boolean isWaiting(){
        return mIsWaitingForStart;
    }

    public void setSmallCard(View small){
        mDownView = small;
    }

    public void setBigCard(View big){
        mUpView = big;
    }
    public View getBigCard() {
        return mUpView;
    }

    public void setBackground(View bg){
        mBackground = bg;
        mBackgroundAlphaAnimator = ObjectAnimator.ofFloat(mBackground, View.ALPHA, 0, 1);
        mBackgroundAlphaAnimator.setDuration(INTERVAL_UP);
        mBackgroundAlphaAnimator.addListener(new FolderUtils.Listener() {
            public void onAnimationEnd(Animator animation) {
                if(mUpView instanceof GadgetView) {
                    GadgetView gadget = (GadgetView) mUpView;
                    gadget.onResume();
                    gadget.getRoot().onCommand("resume");
                }
                //setWorkspaceAlpha(0);
            }
        });
    }

    public void setThatBubble(View bubble){
        mTargetBubble = bubble;
    }

    final public void computeValues(){
        // animating in the same parent
        float parentCenterX = 0;
        float parentCenterY = 0;
        ViewGroup parent = (ViewGroup)mUpView.getParent();
        if (null != parent) {
            parentCenterX = parent.getWidth()/2;
            parentCenterY = parent.getHeight()/2;
        } else {
            Log.e(TAG, "computeValues() getParent is null!");
        }

        final int downX = mDownView.getWidth()/2;
        final int downY = mDownView.getHeight()/2;
        final int upX = mUpView.getWidth()/2;
        final int upY = mUpView.getHeight()/2;
        final float startX = (mDownViewStartX = mDownView.getX());
        final float startY = (mDownViewStartY = mDownView.getY());
        mDownViewFinalX = startX + (parentCenterX - startX) / 3;
        mDownViewFinalY = startY + (parentCenterY - startY) / 3;

        mUpViewStartX = mDownViewFinalX + downX - upX;
        mUpViewStartY = mDownViewFinalY + downY - upY;
        mUpViewFinalX = parentCenterX - upX;
        mUpViewFinalY = parentCenterY - upY;

        mUpView.setCameraDistance(10000);
        mDownView.setCameraDistance(8000);
    }

    public void appear(){
        mIsShowing = true;
        startStageOfSmall();
    }

    public void disappear(){
        endStageOfBig();
    }

    public void disappear(boolean anim) {
        if (anim) {
            disappear();
        } else {
            clear();
        }
    }

    /**
     * small card rotates from 0 to 90, if finished, call startStagedOfBig
     */
    private void startStageOfSmall(){
        mDownView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mUpView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mStartStageOfSmallAnimator = LauncherAnimUtils.ofPropertyValuesHolder(
                mDownView, PropertyValuesHolder.ofFloat(View.X, mDownViewStartX, mDownViewFinalX),
                PropertyValuesHolder.ofFloat(View.Y, mDownViewStartY, mDownViewFinalY),
                PropertyValuesHolder.ofFloat(View.ROTATION_X, 0, -90));
        mStartStageOfSmallAnimator.addListener(mStartStageOfSmallAnimatorListener);
        mStartStageOfSmallAnimator.setDuration(INTERVAL_DOWN);
        mStartStageOfSmallAnimator.setInterpolator(mDecelerateInterpolator);
        mStartStageOfSmallAnimator.start();
    }

    /**
     * Big card rotates from 90° to 0° , appearing on the top of homeshell
     */
    private void startStageOfBig(){
        mStartStageOfBigAnimator = LauncherAnimUtils.ofPropertyValuesHolder(
                mUpView, PropertyValuesHolder.ofFloat(View.X, mUpViewStartX, mUpViewFinalX),
                PropertyValuesHolder.ofFloat(View.Y, mUpViewStartY, mUpViewFinalY),
                PropertyValuesHolder.ofFloat(View.ROTATION_X, 90, 0),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f));
        mStartStageOfBigAnimator.addListener(mStartStageOfBigAnimatorListener);
        mStartStageOfBigAnimator.setDuration(INTERVAL_UP);
        mStartStageOfBigAnimator.setInterpolator(mDecelerateInterpolator);
        mStartStageOfBigAnimator.start();
    }

    /**
     * small card return to its origin position and then clear everything
     */
    private void endStageOfSmall(){
        if (mDownView == null) {
            clear();
            return;
        }
        mEndStageOfSmallAnimator = LauncherAnimUtils.ofPropertyValuesHolder(
                mDownView, PropertyValuesHolder.ofFloat(View.X, mDownViewFinalX, mDownViewStartX),
                PropertyValuesHolder.ofFloat(View.Y, mDownViewFinalY, mDownViewStartY),
                PropertyValuesHolder.ofFloat(View.ROTATION_X, -90, 0));
        mEndStageOfSmallAnimator.addListener(mEndStageOfSmallAnimatorListener);
        mEndStageOfSmallAnimator.setDuration(INTERVAL_DOWN);
        mEndStageOfSmallAnimator.setInterpolator(mDecelerateInterpolator);
        mEndStageOfSmallAnimator.start();
    }

//    private void setWorkspaceAlpha(float alpha) {
//        if (null != mContext && null != mContext.getWorkspace() && null != mContext.getHotseat() && null != mContext.getIndicatorView()) {
//            ACA.View.setTransitionAlpha(mContext.getWorkspace(), alpha);
//            ACA.View.setTransitionAlpha(mContext.getHotseat(), alpha);
//            ACA.View.setTransitionAlpha(mContext.getIndicatorView(), alpha);
//            Folder folder = mContext.getWorkspace().getOpenFolder();
//            if (folder != null)
//                ACA.View.setTransitionAlpha(folder, alpha);
//        }
//    }

    /**
     * if user close big card ,call this method.The big card flip back.
     */
    private void endStageOfBig(){
        if( !mIsShowing ) return;// for bug 5224963
        mUpView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mDownView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        mEndStageOfBigAnimator = LauncherAnimUtils.ofPropertyValuesHolder(
                mUpView, PropertyValuesHolder.ofFloat(View.X, mUpViewFinalX, mUpViewStartX),
                PropertyValuesHolder.ofFloat(View.Y, mUpViewFinalY, mUpViewStartY),
                PropertyValuesHolder.ofFloat(View.ROTATION_X, 0, 90),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.5f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.5f));
        mEndStageOfBigAnimator.addListener(mEndStageOfBigAnimatorListener);
        mEndStageOfBigAnimator.setDuration(INTERVAL_UP);
        mEndStageOfBigAnimator.setInterpolator(mDecelerateInterpolator);
        mEndStageOfBigAnimator.start();
    }

    public boolean isShowing(){
        return mIsShowing;
    }

    private class BlurTask extends AsyncTask<Void, Void, Drawable> {
        private View mView;
        BlurTask(View view) {
            mView = view;
        }

        protected Drawable doInBackground(Void... params) {
            final float scaleFactor = 18;
            final int radius = 5;
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
            long startMs = System.currentTimeMillis();
            int dstWidth = (int) (LauncherApplication.getScreenWidth() / scaleFactor);
            int dstHeight = (int) (LauncherApplication.getScreenHeight() / scaleFactor);
            Bitmap screenshot = ACA.SurfaceControl.screenshot(dstWidth, dstHeight);
            if (screenshot == null || screenshot.getHeight() == 0 || screenshot.getWidth() == 0) {
                return null;
            }
            try {
                Bitmap blur = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
                blur.eraseColor(0xff000000);
                ImageUtils.fastBlur(screenshot, blur, radius);
                screenshot.recycle();
                blur.setHasAlpha(false);
                Canvas c = new Canvas(blur);
                c.drawColor(0x70000000);
                c.setBitmap(null);
                long endMs = System.currentTimeMillis();
                Log.d(TAG, "blur time = " + (endMs - startMs));
                return new OpaqueFastBitmapDrawable(blur);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "BlurTask", e);
                return null;
            }
        }

        protected void onPostExecute(Drawable result) {
            if (result == null || mView == null) {
                // if screenshot is null or contains nothing,ignore blur and add
                // gray background
                Log.d(TAG, "screenshot=null or size abnormal");
                mBackground.setBackgroundColor(0x55555555);
                mBackgroundAlphaAnimator.start();
            } else {
                mView.setBackground(result);
                mBackgroundAlphaAnimator.start();
            }
        }
    }

    private static class OpaqueFastBitmapDrawable extends FastBitmapDrawable {
        public OpaqueFastBitmapDrawable(Bitmap b) {
            super(b);
        }
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    private void cancelBlurTask() {
        if (mBlurTask != null && mBlurTask.getStatus() != AsyncTask.Status.FINISHED)
            mBlurTask.cancel(true);
        mBlurTask = null;
        if(mBackgroundAlphaAnimator != null && mBackgroundAlphaAnimator.isStarted())
            mBackgroundAlphaAnimator.cancel();
    }

    public boolean isAnimating(){
        if( mStartStageOfBigAnimator != null && mStartStageOfBigAnimator.isRunning() ) return true;
        if( mStartStageOfSmallAnimator != null && mStartStageOfSmallAnimator.isRunning() ) return true;
        if( mEndStageOfBigAnimator != null && mEndStageOfBigAnimator.isRunning() ) return true;
        if( mEndStageOfSmallAnimator != null && mEndStageOfSmallAnimator.isRunning() ) return true;
        return false;
    }

    public void clear(){
        cancelBlurTask();
        if( mDownView != null ){
            mDownView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if( mUpView != null ) {
            mUpView.setAlpha(0);
            if (mUpView instanceof GadgetView) {
                GadgetView gadget = (GadgetView) mUpView;
                gadget.getRoot().onCommand("pause");
                if (FolderUtils.LOW_RAM)
                    gadget.cleanUp();
            }
            mUpView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if( mTargetBubble != null ){
            mTargetBubble.setVisibility(View.VISIBLE);
        }
        if (mDownView != null) {
            mDownView.setAlpha(0);
            ViewGroup parent = (ViewGroup) mDownView.getParent();
            if (parent != null) {
                parent.removeView(mDownView);
                parent.removeView(mUpView);
                parent.removeView(mBackground);
            }
        }
        //setWorkspaceAlpha(1);
        mIsShowing = false;
        stopAndClearAnimatorSet();
        mDownView = null;
        mUpView = null;
        mBackground = null;
    }

    private void stopAndClearAnimatorSet(){
        if( mStartStageOfBigAnimator != null && mStartStageOfBigAnimator.isStarted() )
            mStartStageOfBigAnimator.cancel();
        if( mStartStageOfSmallAnimator != null && mStartStageOfSmallAnimator.isStarted() )
            mStartStageOfSmallAnimator.cancel();
        if( mEndStageOfBigAnimator != null && mEndStageOfBigAnimator.isStarted() )
            mEndStageOfBigAnimator.cancel();
        if( mEndStageOfSmallAnimator != null && mEndStageOfSmallAnimator.isStarted() )
            mEndStageOfSmallAnimator.cancel();
        mStartStageOfSmallAnimator = null;
        mStartStageOfBigAnimator = null;
        mEndStageOfSmallAnimator = null;
        mEndStageOfBigAnimator = null;
    }

    private AnimatorListener mStartStageOfSmallAnimatorListener = new AnimatorListener() {

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mIsShowing) {
                return;
            }
            float startX = mDownView.getX() + mDownView.getWidth() / 2 - mUpView.getWidth() / 2;
            mUpView.setX(startX);
            mUpView.setY(mDownView.getY());
            /*YUNOS BEGIN*/
            //#BugID:8486688, #author: xy83652
            //#Desc: use the {@link #executeOnExecutor} version of this method
            // with {@link #THREAD_POOL_EXECUTOR} to make parallel execution.
            if (mBlurTask == null)
                (mBlurTask = new BlurTask(mBackground)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            /*YUNOS END*/
            startStageOfBig();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {}

        @Override
        public void onAnimationStart(Animator animation) {
            Launcher launcher = (Launcher) mContext;
            launcher.getHotseat().preHideHotseat();
            mTargetBubble.setVisibility(View.INVISIBLE);
            mUpView.setAlpha(0);
            if (mUpView instanceof GadgetView) {
                ((GadgetView) mUpView).onPause();
            }
        }};

        private AnimatorListener mStartStageOfBigAnimatorListener = new AnimatorListener() {
            public void onAnimationCancel(Animator animation) {}
            public void onAnimationRepeat(Animator animation) {}
            public void onAnimationStart(Animator animation) {
                mUpView.setAlpha(1);
            }
            public void onAnimationEnd(Animator animation) {
                if(!mIsShowing) {
                    return;
                }
                if (mDownView != null) {
                    mDownView.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                if (mUpView != null) {
                    mUpView.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
        };

        private AnimatorListener mEndStageOfSmallAnimatorListener = new AnimatorListener() {
            public void onAnimationCancel(Animator animator) {}
            public void onAnimationRepeat(Animator animator) {}
            public void onAnimationStart(Animator animator) {
            }
            public void onAnimationEnd(Animator animator) {
                if(mIsShowing ) {
                    clear();
                }
            }
        };

        private AnimatorListener mEndStageOfBigAnimatorListener = new AnimatorListener() {
            public void onAnimationCancel(Animator animation) {}
            public void onAnimationRepeat(Animator animation) {}
            public void onAnimationStart(Animator animation) {
                if (mUpView instanceof GadgetView) {
                    ((GadgetView) mUpView).onPause();
                }
                cancelBlurTask();
                mBackground.setBackground(null);
                //setWorkspaceAlpha(1);
            }
            public void onAnimationEnd(Animator animation) {
                if (mIsShowing) {
                    endStageOfSmall();
                    mUpView.setAlpha(0);
                    Launcher launcher = (Launcher) mContext;
                    launcher.getHotseat().afterShowHotseat();
                }
            }
        };
}
