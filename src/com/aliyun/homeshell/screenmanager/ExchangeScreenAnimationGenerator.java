package com.aliyun.homeshell.screenmanager;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.view.View;

class ExchangeScreenAnimationGenerator {
    private ScreenManagerParam mScreenManagerParam;
    private List<ScreenCardView> mCardList;
    private int[] mIndexs;
    private int mDragIndex;
    private int mDropIndex;

    ExchangeScreenAnimationGenerator(ScreenManagerParam smParam,
            List<ScreenCardView> screenCards,
            int[] screenIndexs, int dragIndex, int dropIndex) {
        mScreenManagerParam = smParam;
        mCardList = screenCards;
        mIndexs = screenIndexs;
        mDragIndex = dragIndex;
        mDropIndex = dropIndex;
    }

    private void reorderCards() {
        int temp = mIndexs[mDragIndex];
        if (mDropIndex >= 0 && mDragIndex > mDropIndex) {
            for (int i = mDragIndex; i > mDropIndex; i--) {
                mIndexs[i] = mIndexs[i - 1];
            }
        } else if (mDropIndex >= 0 && mDragIndex < mDropIndex) {
            for (int i = mDragIndex; i < mDropIndex; i++) {
                mIndexs[i] = mIndexs[i + 1];
            }
        }
        mIndexs[mDropIndex] = temp;
        mDragIndex = mDropIndex;
    }

    List<Animator> getExchangeAnimation() {
        ArrayList<Animator> stackAnims = new ArrayList<Animator>();
        if (mDropIndex >= 0 && mDragIndex > mDropIndex) {
            for (int i = mDragIndex; i > mDropIndex; i--) {
                View v = mCardList.get(mIndexs[i - 1]);
                Animator animation = AnimationUtils.createTranslateAnimator(v,
                        mScreenManagerParam.getX(i - 1),
                        mScreenManagerParam.getX(i),
                        mScreenManagerParam.getY(i - 1),
                        mScreenManagerParam.getY(i));
                stackAnims.add(animation);
            }
        } else if (mDropIndex >= 0 && mDragIndex < mDropIndex) {
            for (int i = mDragIndex; i < mDropIndex; i++) {
                View v = mCardList.get(mIndexs[i + 1]);
                Animator animation = AnimationUtils.createTranslateAnimator(v,
                        mScreenManagerParam.getX(i + 1),
                        mScreenManagerParam.getX(i),
                        mScreenManagerParam.getY(i + 1),
                        mScreenManagerParam.getY(i));
                stackAnims.add(animation);
            }
        }
        reorderCards();
        return stackAnims;
    }

}
