
package com.aliyun.homeshell.globalsearch;

import java.util.Map;
import java.util.HashMap;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.aliyun.homeshell.Insettable;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.utils.Utils;
import com.aliyun.utility.FeatureUtility;

public class LauncherContainer extends FrameLayout implements Insettable{
    private float mScreenDensity = LauncherApplication.getScreenDensity();
    private final boolean DEBUG = false;
    private final String TAG = "LauncherContainer";
    private final int MIN_GLOBALSEARCH_FLING_VELOCITY = (int) (1000 * mScreenDensity);
    private final int VELOCITY_UNIT = 1000;
    private final int YX_ANGLE = 3;
    private final int ANIMATION_DURATION = 200;
    private final int MIN_DISTANCE = (int) (50 * mScreenDensity);
    private final int SLIDE_DISTANCE = (int) (25 * mScreenDensity);
    private final int MAX_IGNORE_EVENT_COUNT = 3;
    private final int MIN_ANIMATE_OFFSET = 3;
    private final int RESTORE_DELAY_TIME = 1000;
    private final String COMMAND_HOMESHELL_RESTORE = "com.aliyun.homeshell.globalsearch.restore";
    private final int MIN_CLICK_DISTANCE = (int) (5 * mScreenDensity);
    private final float SHOW_THRESHOLD = 0.4f;

    private Context mContext = null;
    private boolean mIsMultiPointerEvent = false;
    private VelocityTracker mGlobalSearchVelocityTracker = VelocityTracker.obtain();
    private int mGlobalSearchLastY = 0;
    private int mGlobalSearchStartY = 0;
    private int mGlobalSearchStartX = 0;
    private int mMoveEventCount = 0;
    private boolean mIsSearchShow = false;
    private boolean mIsGlobalSearchBarScolling = false;
    private boolean mDowInLeftArea;
    private boolean mIgnoreEvent = false;
    private Scroller mGlobalSearchScroller = null;
    //private boolean mIsLongClickEvent = false;
    private boolean mBlockTouchEvent = false;
    private boolean mSupportSearch = false;
    private final Intent mSearchIntent = new Intent("android.intent.action.ALISEARCH");

    private static final String SEARCH_SRP_QUERY_FROM_APP = "query_from_app";
    private static final String SEARCH_SRP_QUERY_FROM_APP_SECTION = "query_from_app_section";
    private static final String SEARCH_SRP_QUERY_FROM_VALUE = "homeshell";

    private Bundle mSearchStartOptions = null;

    private final Rect mInsets = new Rect();

    private final BroadcastReceiver mGlobalSearchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (COMMAND_HOMESHELL_RESTORE.equals(action)) {
                Log.d(TAG, "receive action : retore homeshell");
                restoreHomeShell();
            }
        }
    };

    public LauncherContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public LauncherContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LauncherContainer(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSupportSearch = Utils.isSupportSearch(context);
        if (!mSupportSearch) {
            return;
        }

        mSearchStartOptions = ActivityOptions.makeCustomAnimation(mContext,
                android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
        mGlobalSearchScroller = new Scroller(mContext);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_HOMESHELL_RESTORE);
        try {
            mContext.registerReceiver(mGlobalSearchReceiver, filter);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mSupportSearch && !((Launcher)mContext).isSupportLeftScreen()) {
            return super.onInterceptTouchEvent(ev);
        }

        if (!mIgnoreEvent) {
            if (mGlobalSearchVelocityTracker == null) {
                mGlobalSearchVelocityTracker = VelocityTracker.obtain();
            }
            mGlobalSearchVelocityTracker.addMovement(ev);
        }

        handleTouchEvent(ev);
        if (blockTouchDown(ev)) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mSupportSearch && !((Launcher)mContext).isSupportLeftScreen()) {
            return super.onTouchEvent(event);
        }
        super.onTouchEvent(event);
        if (!mIgnoreEvent) {
            if (mGlobalSearchVelocityTracker == null) {
                mGlobalSearchVelocityTracker = VelocityTracker.obtain();
            }
            mGlobalSearchVelocityTracker.addMovement(event);
        }
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            // don't handle ACTION_DONW to avoid duplicate down event
            handleTouchEvent(event);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (!mSupportSearch) {
            super.computeScroll();
            return;
        }

        if (mGlobalSearchScroller.computeScrollOffset()) {
            int currY = mGlobalSearchScroller.getCurrY();
            scrollTo(mGlobalSearchScroller.getCurrX(), currY);
            int startY = mGlobalSearchScroller.getStartY();
            if (startY > 0 || startY < -SLIDE_DISTANCE) {
                return;
            }
        }
    }

    public void unregisterReceiver() {
        if (!mSupportSearch) {
            return;
        }
        try {
            mContext.unregisterReceiver(mGlobalSearchReceiver);
        } catch (Exception e) {
        }
    }

    private void handleTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        dumpEvent(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                clear();
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                onMultiTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onMultiTouchUp(event);
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(event);
                clear();
                break;
            case MotionEvent.ACTION_CANCEL:
                onTouchCancel(event);
                clear();
                break;
            default:
                clear();
                break;
        }
    }

    private void onTouchDown(MotionEvent event) {
        mIsMultiPointerEvent = false;
        handleSingleFingerDown(event);
    }

    private void onMultiTouchDown(MotionEvent event) {
        if (!mIsGlobalSearchBarScolling) {
            mIsMultiPointerEvent = true;
        }
    }

    private void onTouchMove(MotionEvent event) {
        if (hasDragging() || ((Launcher) mContext).isInLauncherEditMode()) {
            mIgnoreEvent = true;
            return;
        }
        handleSingleFingerMove(event);
    }

    private void onMultiTouchUp(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_POINTER_1_UP) {
            handleSingleFingerUp(event);
        }
    }

    private void onTouchUp(MotionEvent event) {
        handleSingleFingerUp(event);
    }

    private void onTouchCancel(MotionEvent event) {
        handleSingleFingerUp(event);
    }

    private void clear() {
        mGlobalSearchLastY = -1;
        mGlobalSearchStartY = -1;
        mGlobalSearchStartX = -1;
        mIsSearchShow = false;
        mBlockTouchEvent = false;
        mIsGlobalSearchBarScolling = false;
    }

    private void handleSingleFingerDown(MotionEvent event) {
        if (mIsMultiPointerEvent) {
            return;
        }

        if (!isInIdleStatus()) {
            mIgnoreEvent = true;
            return;
        } else {
            mIgnoreEvent = false;
        }

        if (mIgnoreEvent) {
            return;
        }

        if (mGlobalSearchScroller != null) {
            if (!mGlobalSearchScroller.isFinished()) {
                if (Math.abs(mGlobalSearchScroller.getFinalY() - mGlobalSearchScroller.getCurrY()) > MIN_ANIMATE_OFFSET) {
                    return;
                } else {
                    mGlobalSearchScroller.abortAnimation();
                }
            }
        }

        //mIsLongClickEvent = false;
        mMoveEventCount = 0;
        mGlobalSearchLastY = (int) event.getY();
        mGlobalSearchStartY = (int) event.getY();
        mGlobalSearchStartX = (int) event.getX();
        mIsSearchShow = false;
        mBlockTouchEvent = false;
        mIsGlobalSearchBarScolling = false;
        mDowInLeftArea = false;

        if (mContext instanceof Launcher) {
            if (((Launcher) mContext).getCardBridge() != null) {
                if (((Launcher) mContext).getCardBridge().handleTouchEvent(
                        event)) {
                    mDowInLeftArea = true;
                }
            }
        }
    }

    private void handleSingleFingerMove(MotionEvent event) {
        if (mIgnoreEvent) {
            return;
        }

        if (!mIsGlobalSearchBarScolling && mIsMultiPointerEvent) {
            mIgnoreEvent = true;
            return;
        }

        if (isClickEvent(event)) {
            return;
        }

        if (FeatureUtility.hasFullScreenWidget() &&
                ((Launcher)mContext).getWorkspace().isCurPageConsumedFlingDown()) {
            mIgnoreEvent = true;
            return;
        }

        if (mMoveEventCount++ <= MAX_IGNORE_EVENT_COUNT) {
            return;
        }

        if (!mIsGlobalSearchBarScolling && !isInIdleStatus()) {
            mIgnoreEvent = true;
            return;
        }

        if (isSearchShow()) {
            return;
        }

        if (mDowInLeftArea) {
            ((Launcher) mContext).getCardBridge().handleTouchEvent(event);
            return;
        }

        final VelocityTracker velocityTracker = mGlobalSearchVelocityTracker;
        velocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
        int velocityY = (int) velocityTracker.getYVelocity();
        int velocityX = (int) velocityTracker.getXVelocity();

        if (velocityY > MIN_GLOBALSEARCH_FLING_VELOCITY) {
            if (velocityY > YX_ANGLE * Math.abs(velocityX)) {
                showSearch();
                mIsGlobalSearchBarScolling = true;
                mBlockTouchEvent = true;
            }
        } else {
            if (!mIsGlobalSearchBarScolling) {
                if (!isMoveDown(event)) {
                    mIgnoreEvent = true;
                    return;
                }
                if (Math.abs((int) event.getY() - mGlobalSearchStartY) <= MIN_DISTANCE) {
                    mGlobalSearchLastY = (int) event.getY();
                    return;
                } else {
                    mIsGlobalSearchBarScolling = true;
                    mBlockTouchEvent = true;
                }
            }

            float offset = (int) (event.getY()) - mGlobalSearchStartY - MIN_DISTANCE;
            int deltaY = (int) (event.getY()) - mGlobalSearchLastY;
            if (mGlobalSearchLastY == mGlobalSearchStartY) {
                deltaY -= MIN_DISTANCE;
            }
            mGlobalSearchLastY = (int) event.getY();
            int scrollY = (int) getScrollY();
            if (scrollY <= -SLIDE_DISTANCE) {
                (new Handler()).postDelayed(
                        new Runnable() {

                            @Override
                            public void run() {
                                restoreHomeShell();
                            }
                        }, RESTORE_DELAY_TIME);
                return;
            }
            int tagertscrollY = (int) (scrollY - deltaY);

            if (offset > SLIDE_DISTANCE) {
                offset = SLIDE_DISTANCE;
            } else if (offset < 0) {
                offset = 0;
            }

            if (tagertscrollY >= 0) {
                deltaY = scrollY;
            } else if (tagertscrollY <= -SLIDE_DISTANCE) {
                deltaY = scrollY + SLIDE_DISTANCE;
            }

            scrollBy(0, -(int) deltaY);
            animateSlide((offset) / (SLIDE_DISTANCE));
            mBlockTouchEvent = true;
            if (offset == SLIDE_DISTANCE) {
                startSearchApp();
            } else if (offset == 0) {
                if (getHomeShellView().getAlpha() != 1) {
                    getHomeShellView().setAlpha(1);
                }
            }
        }
    }

    private void handleSingleFingerUp(MotionEvent event) {
        if (mIsMultiPointerEvent) {
            return;
        }

        if (!isInIdleStatus()) {
            return;
        }

        if (mIgnoreEvent) {
            return;
        }

        if (mDowInLeftArea) {
            ((Launcher)mContext).getCardBridge().handleTouchEvent(event);
            return;
        }

        if (isClickEvent(event)) {
            return;
        }

        if (isSearchShow()) {
            return;
        }
        mGlobalSearchLastY = (int) event.getY();

        final VelocityTracker velocityTracker = mGlobalSearchVelocityTracker;
        velocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
        int velocityY = (int) velocityTracker.getYVelocity();
        int velocityX = (int) velocityTracker.getXVelocity();

        if (velocityY > MIN_GLOBALSEARCH_FLING_VELOCITY) {
            if (velocityY > YX_ANGLE * Math.abs(velocityX)) {
                showSearch();
            }
        } else {
            if (isMoveDown(event) && willShowSearch()) {
                animateShow();
            } else {
                animateHide();
            }
        }
        if (mGlobalSearchVelocityTracker != null) {
            mGlobalSearchVelocityTracker.clear();
            mGlobalSearchVelocityTracker.recycle();
            mGlobalSearchVelocityTracker = null;
        }

        mGlobalSearchLastY = 0;
        mGlobalSearchStartY = 0;
        mGlobalSearchStartX = 0;
        mIsSearchShow = false;
        mIsGlobalSearchBarScolling = false;
    }

    private void dumpEvent(MotionEvent event) {
        int id = event.getPointerId(event.getActionIndex());
        String loginfo = "";
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                loginfo = "ACTION_DOWN";
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                loginfo = "ACTION_POINTER_DOWN";
                break;
            case MotionEvent.ACTION_MOVE:
                loginfo = "ACTION_MOVE";
                break;
            case MotionEvent.ACTION_UP:
                loginfo = "ACTION_UP";
                break;
            case MotionEvent.ACTION_POINTER_UP:
                loginfo = "ACTION_POINTER_UP";
                break;
            case MotionEvent.ACTION_CANCEL:
                loginfo = "ACTION_CANCEL";
            default:
                break;
        }
        if (DEBUG) {
            Log.d(TAG,
                    id + " move " + loginfo + " point x = " + event.getX() + " y : " + event.getY());
        }
    }

    private boolean isSearchShow() {
        return mIsSearchShow;
    }

    private void showSearch() {
        if (!isSearchShow()) {
            animateShow();
        }
    }

    private void startSearchApp() {
        if (!mIsSearchShow) {
            mIsSearchShow = true;
            mSearchIntent.putExtra(SEARCH_SRP_QUERY_FROM_APP, SEARCH_SRP_QUERY_FROM_VALUE);
            mSearchIntent.putExtra(SEARCH_SRP_QUERY_FROM_APP_SECTION, SEARCH_SRP_QUERY_FROM_VALUE);
            try{
                mContext.startActivity(mSearchIntent, mSearchStartOptions);
            }catch(ActivityNotFoundException e){
                Log.w(TAG,"Search app doesn't exist");
            }
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        int screen = ((Launcher) mContext).getCurrentScreen();
                        Map<String, String> lMap = new HashMap<String, String>();
                        lMap.put("arg1", "launch_search");
                        lMap.put("screen", String.valueOf(screen));
                        UserTrackerHelper.commitEvent("homeshell_slidedown_search", lMap); 
                    }catch(ActivityNotFoundException e){
                        Log.w(TAG,"Search app doesn't exist");
                    }catch(NullPointerException ex) {    //BugID:6179568:null pointer exception
                        Log.w(TAG,"NullPointerException ", ex);
                    }
                }
            })).start();

            (new Handler()).postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            restoreHomeShell();
                        }
                    }, RESTORE_DELAY_TIME);
        }
    }

    private void animateShow() {
        mGlobalSearchScroller.abortAnimation();
        mGlobalSearchScroller.startScroll((int) getX(), (int) getScrollY(), 0, -SLIDE_DISTANCE
                - (int) getScrollY(), ANIMATION_DURATION);
        invalidate();

        float alpha = getHomeShellView().getAlpha();
        int duration = (int) (alpha * ANIMATION_DURATION);
        getHomeShellView().animate().alpha(0.0f).setDuration(duration)
                .setListener(new AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                        startSearchApp();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        // TODO Auto-generated method stub

                    }
                }).start();

    }

    private void animateHide() {
        mGlobalSearchScroller.abortAnimation();
        mGlobalSearchScroller.startScroll((int) getX(), (int) getScrollY(), 0, -(int) getScrollY(),
                ANIMATION_DURATION);
        invalidate();

        float alpha = getHomeShellView().getAlpha();
        int duration = (int) ((1 - alpha) * ANIMATION_DURATION);
        getHomeShellView().animate().alpha(1.0f).setDuration(duration).setListener(null).start();
    }

    private void animateSlide(float progress) {
        getHomeShellView().setAlpha(1 - progress);
    }

    private boolean willShowSearch() {
        return (mGlobalSearchLastY - mGlobalSearchStartY) > (MIN_DISTANCE + SLIDE_DISTANCE) * SHOW_THRESHOLD;
    }

    private void restoreHomeShell() {
        if (mGlobalSearchScroller != null) {
            if (!mGlobalSearchScroller.isFinished()) {
                mGlobalSearchScroller.abortAnimation();
            }
        }

        getHomeShellView().setAlpha(1);
        if (getScrollY() == 0) {
            return;
        }
        scrollTo((int) getX(), 0);
        postInvalidate();
    }

    private View getHomeShellView() {
        return this;
    }

    private boolean isMoveDown(MotionEvent event) {
        boolean isdown = false;
        float offsetX = Math.abs(event.getX() - mGlobalSearchStartX);
        int offsetY = (int) event.getY() - mGlobalSearchStartY;
        if (YX_ANGLE * offsetX < offsetY && mGlobalSearchStartX >=0 && mGlobalSearchStartY >=0) {
            isdown = true;
        }
        return isdown;
    }

    private boolean hasDragging() {
        return ((Launcher) mContext).getGestureLayer().hasDragging();
    }

    private boolean isInIdleStatus() {
        return ((Launcher) mContext).isInIdleStatus();
    }

    private boolean blockTouchDown(MotionEvent ev) {
        boolean block = false;
        if (((Launcher) mContext).blockTouchDown() && isMoveDown(ev)) {
            block = true;
        }

        if (mBlockTouchEvent || mDowInLeftArea) {
            block = true;
        }
        return block;
    }

    private boolean isClickEvent(MotionEvent event) {
        return Math.abs(event.getY() - mGlobalSearchStartY) < MIN_CLICK_DISTANCE
                && Math.abs(event.getX() - mGlobalSearchStartX) < MIN_CLICK_DISTANCE;
    }

    @Override
    public void setInsets(Rect insets) {
        final int n = getChildCount();
        for (int i = 0; i < n; i++) {
            final View child = getChildAt(i);
            if (child instanceof Insettable) {
                ((Insettable) child).setInsets(insets);
            }
        }
        mInsets.set(insets);
    }

}
