package com.aliyun.homeshell.screenmanager;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.graphics.Point;
import android.view.Display;
import android.view.View;

import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.screenmanager.ScreenManagerView.OnItemClickListener;

public class ScreenManager implements OnItemClickListener {
    public static interface ScreenManagerListener {
        void onExit(Object stopTag);
        void onItemClick(int index);
    }

    private ScreenManagerView mScreenManagerView;
    private AnimationViewsProvider mAnimationViewsProvider;
    private TransitionAnimationPlayer mAnimationPlayer;
    private ScreenManagerParam mParam;
    private Launcher mLauncher;
    private ScreenManagerListener mScreenManagerListener;
    private int mCurrentScreen;
    private Object mStopTag;

    private boolean mTransitionAnimationPlaying;

    public ScreenManager(Launcher launcher) {
        mScreenManagerView = new ScreenManagerView(launcher);
        mAnimationViewsProvider = new AnimationViewsProvider(launcher);
        mAnimationPlayer = TransitionAnimationPlayer.getPlayer();
        mLauncher = launcher;
        mScreenManagerView.setOnItemClickListener(this);
    }

    public void setScreenManagerListener(ScreenManagerListener l) {
        mScreenManagerListener = l;
    }

    private AnimatorListener mEnterAnimatorListener = new AnimatorListener(){

        @Override
        public void onAnimationCancel(Animator arg0) {
            DebugTools.log("ScreenManager,enter,onAnimationCancel", false);
        }

        @Override
        public void onAnimationEnd(Animator arg0) {
            DebugTools.log("ScreenManager,enter,onAnimationEnd:"
                    + mTransitionAnimationPlaying, false);
            if (mTransitionAnimationPlaying) {
                madePreviewIcon();
                mTransitionAnimationPlaying = false;
            }
        }

        @Override
        public void onAnimationRepeat(Animator arg0) {
        }

        @Override
        public void onAnimationStart(Animator arg0) {
            DebugTools.log("ScreenManager,enter,onAnimationStart", false);
        }};

        private AnimatorListener mExitAnimatorListener = new AnimatorListener(){

            @Override
            public void onAnimationCancel(Animator arg0) {
                DebugTools.log("ScreenManager,exit,onAnimationStart", false);
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                DebugTools.log("ScreenManager,exit,onAnimationEnd:"
                        + mTransitionAnimationPlaying, false);
                if (!mTransitionAnimationPlaying) {
                    return;
                }

                mAnimationViewsProvider.close();
                if (mScreenManagerView != null) {
                    mScreenManagerView.setVisibility(View.GONE);
                }
                if (mScreenManagerListener != null) {
                    mScreenManagerListener.onExit(mStopTag);
                }
                mTransitionAnimationPlaying = false;
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
            }

            @Override
            public void onAnimationStart(Animator arg0) {
                DebugTools.log("ScreenManager,exit,onAnimationStart", false);
            }};

    public boolean start() {
        DebugTools.log("ScreenManager,start:" + mTransitionAnimationPlaying, false);
        if (mTransitionAnimationPlaying) {
            return false;
        }
        mTransitionAnimationPlaying = true;
        mAnimationViewsProvider.open();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        /*YUNOS BEGIN PB*/
        //Desc:BugID:6432567:ignore nav bar insets to fix layout issues
        //##Date: Oct 8, 2015 6:52:54 PM ##Author:chao.lc@alibaba-inc.com
        display.getRealSize(displaySize);
        /*YUNOS END PB*/
        mCurrentScreen = mAnimationViewsProvider.getCurrentScreen();
        mParam = ScreenmanagerParamFactory.getFactory()
                .createScreenManagerPamaram(mLauncher,
                displaySize.x, displaySize.y,
                mAnimationViewsProvider.getViews().size(),
                mCurrentScreen);
        mAnimationPlayer.open(mScreenManagerView, mParam,
                mAnimationViewsProvider,
                mCurrentScreen, null);
        mAnimationPlayer.play(Const.TYPE_ENTER,
                mEnterAnimatorListener);
        return true;
    }

    public View getRootView() {
        return mScreenManagerView;
    }

    public boolean stop(Boolean tag, int index) {
        DebugTools.log("ScreenManager,stop2:" + mTransitionAnimationPlaying, false);
        if (mTransitionAnimationPlaying) {
            return false;
        }
        mStopTag = tag;
        mTransitionAnimationPlaying = true;
        stop(index);
        return true;
    }

    public boolean stop(boolean animate, Object tag) {
        DebugTools.log("ScreenManager,stop1:" + animate, false);
        if (animate) {
            if (mTransitionAnimationPlaying) {
                return false;
            }
            mStopTag = tag;
            mTransitionAnimationPlaying = true;
            stop(mAnimationViewsProvider.
                    getCurrentScreen(mScreenManagerView.getExchangeResult()));
        } else {
            closeAll(tag);
        }
        return true;
    }

    private void closeAll(Object tag) {
        if (mAnimationPlayer != null) {
            mAnimationPlayer.stop();
            mAnimationPlayer.close();
        }
        if (mScreenManagerView != null) {
            mScreenManagerView.close();
        }
        mAnimationViewsProvider.close();
        if (mScreenManagerView != null) {
            mScreenManagerView.setVisibility(View.GONE);
        }
        if (mScreenManagerListener != null) {
            mScreenManagerListener.onExit(tag);
        }
        mTransitionAnimationPlaying = false;
    }

    private void stop(int currentScreen) {
        mCurrentScreen = currentScreen;
        destoryPreviewIcon();
        mAnimationPlayer.open(mScreenManagerView, mParam,
                mAnimationViewsProvider,
                currentScreen,
                mScreenManagerView.getExchangeResult());
        mAnimationPlayer.play(Const.TYPE_EXIT,
                mExitAnimatorListener);
    }

    private void madePreviewIcon() {
        mScreenManagerView.open(mAnimationViewsProvider.getEntryViews(), mParam);
    }

    private void destoryPreviewIcon() {
        mScreenManagerView.close();
    }

    public List<Integer> getNewIndexs() {
        List<Integer> list = new ArrayList<Integer>();
        if (mScreenManagerView != null) {
            int[] array = mScreenManagerView.getExchangeResult();
            for (int i : array) {
                list.add(i);
            }
        }
        return list;
    }

    @Override
    public void onItemClicked(int index) {
        if (mTransitionAnimationPlaying) {
            return;
        }
        if (mScreenManagerListener != null) {
            mScreenManagerListener.onItemClick(index);
        }
    }

    public int getCurrentPage() {
        return mCurrentScreen;
    }

    public boolean lockWorkspace() {
        return mAnimationViewsProvider != null
                && mAnimationViewsProvider.opened();
    }
}
