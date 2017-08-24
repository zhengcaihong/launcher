package com.aliyun.homeshell.screenmanager;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.view.ViewGroup;

class TransitionAnimationPlayer implements AnimatorListener {

    private static final int[] TRANS_DURING = new int[]{300, 300, 300, 300, 350, 350, 450, 450, 500, 600, 700, 800};
    private static final int[] ROTATE_DURING = new int[]{230, 230, 230, 230, 260, 260, 290, 330, 360, 400, 450, 500};

    private static TransitionAnimationPlayer mInstance = new TransitionAnimationPlayer();

    private AnimatorListener mAnimatorListener;
    private TransitionAnimationGenerator mAnimationGenerator;

    private boolean mStage2Flag;
    private AnimatorSet mStackAnimSet;
    private int mType;
    private AnimationViewsProvider mAnimationViewsProvider;
    private ViewGroup mRecordView;
    private ScreenManagerParam mScreenManagerParam;
    private int mCurrentScreen;
    private int[] mNewIndex;
    private boolean mCancelFlag;

    private TransitionAnimationPlayer() {
    }

    static TransitionAnimationPlayer getPlayer() {
        return mInstance;
    }

    void play(int type, AnimatorListener l) {
        mAnimatorListener = l;
        mStage2Flag = false;
        mCancelFlag = false;
        mType = type;
        int index = mScreenManagerParam.getSize() - 1;
        index = index >= Const.MAX_SCREENS ? Const.MAX_SCREENS - 1 : index;
        AnimatorSet stackAnimSet = new AnimatorSet();
        stackAnimSet.addListener(this);
        if (mAnimationGenerator != null) {
            mAnimationGenerator.close();
        }
        DebugTools.log("TransitionAnimationPlayer,play:" + type, false);
        switch (type) {
        case Const.TYPE_ENTER:
            mAnimationGenerator = new TransitionAnimationGenerator(mRecordView,
                    mScreenManagerParam, mAnimationViewsProvider.getEntryViews(),
                    mAnimationViewsProvider.getEntryParams(),
                    mAnimationViewsProvider.getCellLayoutX(),
                    mAnimationViewsProvider.getCellLayoutY(),
                    mCurrentScreen,
                    mAnimationViewsProvider.getHotSeat(),
                    mAnimationViewsProvider.getHotSeatOffsetX(),
                    mAnimationViewsProvider.getHotSeatOffseatY());
            stackAnimSet.playTogether(mAnimationGenerator.getEnterStage1Animation());
            stackAnimSet.setDuration(TRANS_DURING[index]);
            break;
        case Const.TYPE_EXIT:
            mAnimationGenerator = new TransitionAnimationGenerator(mRecordView,
                    mScreenManagerParam, mAnimationViewsProvider.getExitViewsStage1(mNewIndex),
                    mAnimationViewsProvider.getExitParamsStage1(mNewIndex),
                    mAnimationViewsProvider.getCellLayoutX(),
                    mAnimationViewsProvider.getCellLayoutY(),
                    mCurrentScreen,
                    mAnimationViewsProvider.getHotSeat(),
                    mAnimationViewsProvider.getHotSeatOffsetX(),
                    mAnimationViewsProvider.getHotSeatOffseatY());
            stackAnimSet.playTogether(mAnimationGenerator.getExitStage1Animation());
            stackAnimSet.setDuration(ROTATE_DURING[index]);
            break;
        }
        stackAnimSet.start();
        mStackAnimSet = stackAnimSet;
    }

    void stop() {
        DebugTools.log("TransitionAnimationPlayer,stop:" + mCancelFlag, false);
        mCancelFlag = true;
        if (mStackAnimSet != null) {
            mStackAnimSet.cancel();
        }
    }

    void open(ViewGroup recordView,
            ScreenManagerParam smParam,
            AnimationViewsProvider provider,
             int currentScreen,
             int[] newIndex) {
       DebugTools.log("TransitionAnimationPlayer,open", false);
       if (mAnimationGenerator != null) {
           mAnimationGenerator.close();
       }
       mRecordView = recordView;
       mScreenManagerParam = smParam;
       mAnimationViewsProvider = provider;
       mCurrentScreen = currentScreen;
       mNewIndex = newIndex;
    }

    void close() {
        DebugTools.log("TransitionAnimationPlayer,close", false);
        mAnimationGenerator.close();
    }

    @Override
    public void onAnimationCancel(Animator arg0) {
        DebugTools.log("TransitionAnimationPlayer,onAnimationCancel", false);
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationCancel(arg0);
        }
    }

    @Override
    public void onAnimationEnd(Animator arg0) {
        DebugTools.log("TransitionAnimationPlayer,onAnimationEnd:" + mStage2Flag + "," + mCancelFlag, false);
        if (mStage2Flag) {
            close();
            if (mAnimatorListener != null) {
                mAnimatorListener.onAnimationEnd(arg0);
            }
        } else {
            if (mCancelFlag) {
                return;
            }
            AnimatorSet stackAnimSet = new AnimatorSet();
            stackAnimSet.addListener(this);
            int index = mScreenManagerParam.getSize() - 1;
            index = index >= Const.MAX_SCREENS ? Const.MAX_SCREENS - 1 : index;
            switch (mType) {
            case Const.TYPE_ENTER:
                mAnimationGenerator.close();
                mAnimationGenerator = new TransitionAnimationGenerator(mRecordView,
                        mScreenManagerParam, mAnimationViewsProvider.getEntryViews(),
                        mAnimationViewsProvider.getEntryParams(),
                        mAnimationViewsProvider.getCellLayoutX(),
                        mAnimationViewsProvider.getCellLayoutY(),
                        mCurrentScreen,
                        mAnimationViewsProvider.getHotSeat(),
                        mAnimationViewsProvider.getHotSeatOffsetX(),
                        mAnimationViewsProvider.getHotSeatOffseatY());
                stackAnimSet.playTogether(mAnimationGenerator.getEnterStage2Animation());
                stackAnimSet.setDuration(ROTATE_DURING[index]);
                break;
            case Const.TYPE_EXIT:
                mAnimationGenerator.close();
                mAnimationGenerator = new TransitionAnimationGenerator(mRecordView,
                        mScreenManagerParam, mAnimationViewsProvider.getExitViewsStage2(mNewIndex, mCurrentScreen),
                        mAnimationViewsProvider.getExitParamsStage2(mNewIndex, mCurrentScreen),
                        mAnimationViewsProvider.getCellLayoutX(),
                        mAnimationViewsProvider.getCellLayoutY(),
                        mCurrentScreen,
                        mAnimationViewsProvider.getHotSeat(),
                        mAnimationViewsProvider.getHotSeatOffsetX(),
                        mAnimationViewsProvider.getHotSeatOffseatY());
                stackAnimSet.playTogether(mAnimationGenerator.getExitStage2Animation());
                stackAnimSet.setDuration(TRANS_DURING[index]);
                break;
            }
            stackAnimSet.start();
            mStackAnimSet = stackAnimSet;
            mStage2Flag = true;
        }
    }

    @Override
    public void onAnimationRepeat(Animator arg0) {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationRepeat(arg0);
        }
    }

    @Override
    public void onAnimationStart(Animator arg0) {
        DebugTools.log("TransitionAnimationPlayer,onAnimationStart", false);
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationStart(arg0);
        }
    }

}
