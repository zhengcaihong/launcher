package com.aliyun.homeshell.screenmanager;

import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.Animator.AnimatorListener;
import android.view.ViewGroup;

class ExchangeScreenAnimationPlayer implements AnimatorListener {

    private static final int EXCHANGE_DURING = 230;

    private ExchangeScreenAnimationGenerator mAnimationGenerator;
    private AnimatorListener mAnimatorListener;
    private AnimatorSet mStackAnimSet;
    private ExchangeScreenAnimationPlayer() {}
    private static ExchangeScreenAnimationPlayer mPlayer
                = new ExchangeScreenAnimationPlayer();

    static ExchangeScreenAnimationPlayer getPlayer() {
        return mPlayer;
    }

    void open (ViewGroup recordView, ScreenManagerParam smParam,
            List<ScreenCardView> screenCards, int[] screenIndexs,
            int dragIndex, int dropIndex) {
        mAnimationGenerator = new ExchangeScreenAnimationGenerator(smParam,
                screenCards, screenIndexs, dragIndex, dropIndex);
    }

    void play(AnimatorListener l) {
        mAnimatorListener = l;
        AnimatorSet stackAnimSet = new AnimatorSet();
        stackAnimSet.addListener(this);
        stackAnimSet.playTogether(mAnimationGenerator.getExchangeAnimation());
        stackAnimSet.setDuration(EXCHANGE_DURING);
        stackAnimSet.start();
        mStackAnimSet = stackAnimSet;
    }

    void close() {
        mAnimationGenerator = null;
    }

    @Override
    public void onAnimationCancel(Animator arg0) {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationCancel(arg0);
        }
    }

    @Override
    public void onAnimationEnd(Animator arg0) {
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationEnd(arg0);
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
        if (mAnimatorListener != null) {
            mAnimatorListener.onAnimationStart(arg0);
        }
    }

    public void cancel() {
        if (mStackAnimSet != null && mStackAnimSet.isRunning()) {
            mStackAnimSet.cancel();
        }
    }
}
