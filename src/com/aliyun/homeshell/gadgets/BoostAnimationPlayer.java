package com.aliyun.homeshell.gadgets;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.util.Pair;

final class BoostAnimationPlayer {
    private static BoostAnimationPlayer mPlayer;
    private static final int STAGE_1_DURATION = 600;
    private static final int STAGE_2_DURATION = 1000;
    private static final int STAGE_3_DURATION = 600;

    private boolean mPlaying;
    private BoostAnimationListener mBoostAnimationListener;
    private int mValue;

    private AnimatorListener mAnimatorListener = new AnimatorListener(){

        @Override
        public void onAnimationStart(Animator animation) {
            if (mBoostAnimationListener != null) {
                mBoostAnimationListener.onAnimationStart(BoostAnimationPlayer.this);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mPlaying = false;
            if (mBoostAnimationListener != null) {
                mBoostAnimationListener.onAnimationEnd(BoostAnimationPlayer.this);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            if (mBoostAnimationListener != null) {
                mBoostAnimationListener.onAnimationCancel(BoostAnimationPlayer.this);
            }
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
            if (mBoostAnimationListener != null) {
                mBoostAnimationListener.onAnimationRepeat(BoostAnimationPlayer.this);
            }
        }};
    private AnimatorUpdateListener mUpdateListener = new AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (mBoostAnimationListener != null) {
                mValue = (Integer)animation.getAnimatedValue();
                mBoostAnimationListener.onAnimationUpdate(BoostAnimationPlayer.this);
            }
        }};

    private BoostAnimationPlayer(Context context) {
    }

    static BoostAnimationPlayer getPlayer(Context context) {
        if (mPlayer == null) {
            synchronized (BoostAnimationPlayer.class) {
                if (mPlayer == null) {
                    mPlayer = new BoostAnimationPlayer(context);
                }
            }
        }
        return mPlayer;
    }

    void play() {
        if (mPlaying) {
             return;
         }
        mPlaying = true;
        startAnimation();
    }

    // YUNOS BEGIN
    // ##modules(HomeShell): ##yongxing.lyx
    // ##BugID:(8113446) ##date:2016/04/13
    // ##description: emerge black screen when press OneKeyAccerate quickly
    boolean isPlaying() {
        return mPlaying;
    }
    // YUNOS END

    BoostAnimationPlayer setBoostAnimationListener(BoostAnimationListener l) {
        mBoostAnimationListener = l;
        return this;
    }

    Pair<Integer, Integer> getStageAndProgress() {
        if (mPlaying) {
            if (mValue < STAGE_1_DURATION) {
                return new Pair<Integer, Integer>(GadgetsConsts.ANIMATION_STAGE_1,
                        mValue * GadgetsConsts.FULL_PROGRESS / STAGE_1_DURATION);
            } else if (mValue < STAGE_1_DURATION + STAGE_2_DURATION) {
                return new Pair<Integer, Integer>(GadgetsConsts.ANIMATION_STAGE_2,
                        (mValue - STAGE_1_DURATION) *
                        GadgetsConsts.FULL_PROGRESS / STAGE_2_DURATION);
            } else {
                return new Pair<Integer, Integer>(GadgetsConsts.ANIMATION_STAGE_3,
                        (mValue - STAGE_1_DURATION - STAGE_2_DURATION) *
                        GadgetsConsts.FULL_PROGRESS / STAGE_3_DURATION);
            }
        } else {
            return new Pair<Integer, Integer>(GadgetsConsts.NORMAL_STAGE, 0);
        }
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofInt(0,
                STAGE_1_DURATION + STAGE_2_DURATION + STAGE_3_DURATION);
        animator.addListener(mAnimatorListener);
        animator.addUpdateListener(mUpdateListener);
        animator.setDuration(STAGE_1_DURATION + STAGE_2_DURATION + STAGE_3_DURATION);
        animator.start();
    }

}
