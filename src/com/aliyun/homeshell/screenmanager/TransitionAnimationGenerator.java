package com.aliyun.homeshell.screenmanager;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

import com.aliyun.homeshell.CellLayout;

class TransitionAnimationGenerator {
    private ViewGroup mRecordView;
    private ScreenManagerParam mScreenManagerParam;
    private List<List<View>> mViews;
    private List<List<CellLayout.LayoutParams>> mWorkSpaceParams;
    private float mOffsetX;
    private float mOffsetY;
    private int mCurrentScreen;

    private View mHotSeat;
    private float mHotSeatX;
    private float mHotSeatY;

    TransitionAnimationGenerator(ViewGroup recordView,
            ScreenManagerParam smParam,
            List<List<View>> views,
            List<List<CellLayout.LayoutParams>> wsParams,
            float offsetX, float offsetY,
             int currentScreen, View hotSeat,
             float hotSeatX, float hotSeatY) {
        mRecordView = recordView;
        mScreenManagerParam = smParam;
        mViews = views;
        mWorkSpaceParams = wsParams;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mCurrentScreen = currentScreen;
        mHotSeat = hotSeat;
        mHotSeatX = hotSeatX;
        mHotSeatY = hotSeatY;
    }

    ArrayList<Animator> getExitStage1Animation() {
        ArrayList<Animator> stackAnims = new ArrayList<Animator>();
        for (int i = 0; i < mViews.size(); i ++) {
            List<View> list = mViews.get(i);
            int count = list.size();
            count = count > Const.MAX_CARDS ? Const.MAX_CARDS : count;
            List<CellLayout.LayoutParams> wsParamList =
                    mWorkSpaceParams.get(i);

            float tx = mScreenManagerParam.getX(i) + Const.FIRST_CARD_OFFSET_X;
            float ty = mScreenManagerParam.getY(i) + Const.FIRST_CARD_OFFSET_Y;
            for (int j = count - 1; j >= 0; j --) {
                View v = list.get(j);
                CellLayout.LayoutParams lp = wsParamList.get(j);
                v.setLayoutParams(new LayoutParams(
                        lp.width, lp.height));
                mRecordView.addView(v);
                if (j == count - 1 && j != 0) {
                    v.setAlpha(0.5f);
                }

                Animator animation = AnimationUtils.createUnfolderAnimator(v,
                        -Const.ROTATES[j], 0, Const.CARD_WIDTH / 2, Const.CARD_HEIGHT / 2,
                        tx - Const.RIGHT_TRANS[j], tx,
                        ty + Const.DOWN_TRANS[j], ty);
                stackAnims.add(animation);
            }
        }

        for (int i = 0; i <  mViews.size(); i++) {
            TextView textView = new TextView(mRecordView.getContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Const.LABEL_TEXT_SIZE);
            textView.setText(((i + 1) / 10) + "" + ((i + 1) % 10));
            mRecordView.addView(textView);
            textView.setX(mScreenManagerParam.getX(i) + Const.LABEL_TEXT_OFFSET_X);
            textView.setY(mScreenManagerParam.getY(i) + Const.LABEL_TEXT_OFFSET_Y);
            textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            textView.setTextColor(i == mCurrentScreen ? Color.RED : Color.WHITE);
            Animator animation = AnimationUtils.createFadeAnimator(textView, 1, 0);
            stackAnims.add(animation);
        }
        return stackAnims;
    }

    ArrayList<Animator> getExitStage2Animation() {
        ArrayList<Animator> stackAnims = new ArrayList<Animator>();
        List<View> viewList = mViews.get(mCurrentScreen);
        List<CellLayout.LayoutParams> wsParamList = mWorkSpaceParams.get(mCurrentScreen);
        float dstX = mScreenManagerParam.getX(mCurrentScreen) + Const.FIRST_CARD_OFFSET_X;
        float dstY = mScreenManagerParam.getY(mCurrentScreen) + Const.FIRST_CARD_OFFSET_Y;
        for (int i = viewList.size() - 1; i >= 0; i--) {
            View v = viewList.get(i);
            v.setAlpha(1.0f);
            CellLayout.LayoutParams lp = wsParamList.get(i);
            v.setLayoutParams(new LayoutParams(
                    lp.width, lp.height));
            mRecordView.addView(v);
            Animator animation = AnimationUtils.createTranslateAnimator(v,
                    dstX , lp.getX() + mOffsetX, dstY, lp.getY() + mOffsetY);
            stackAnims.add(animation);
        }

        for (int i = 0; i < mViews.size(); i ++) {
            if (i == mCurrentScreen) {
                continue;
            }
            dstX = mScreenManagerParam.getX(i) + Const.FIRST_CARD_OFFSET_X;
            dstY = mScreenManagerParam.getY(i) + Const.FIRST_CARD_OFFSET_Y;
            List<View> list = mViews.get(i);
            wsParamList = mWorkSpaceParams.get(i);
            int count = list.size();
            count = count > Const.MAX_CARDS ? Const.MAX_CARDS : count;
            for (int j = count - 1; j >= 0; j --) {
                View v = list.get(j);
                CellLayout.LayoutParams lp = wsParamList.get(j);
                mRecordView.addView(v, new LayoutParams(
                            lp.width, lp.height));
                Animator animation = AnimationUtils.createTranslateAnimator(v,
                        dstX, dstX + (dstX - mScreenManagerParam.getX(mCurrentScreen)) * 5,
                        dstY, dstY + (dstY - mScreenManagerParam.getY(mCurrentScreen)) * 5);
                stackAnims.add(animation);
            }
        }

        mRecordView.addView(mHotSeat, new LayoutParams(
                mHotSeat.getWidth(), mHotSeat.getHeight()));
        Animator animation = AnimationUtils.createTranslateAnimator(mHotSeat,
                mHotSeatX, mHotSeatX,
                mHotSeatY + mHotSeat.getHeight() * 4, mHotSeatY);
        stackAnims.add(animation);
        return stackAnims;
    }

    ArrayList<Animator> getEnterStage1Animation() {
        ArrayList<Animator> stackAnims = new ArrayList<Animator>();
        List<View> viewList = mViews.get(mCurrentScreen);
        List<CellLayout.LayoutParams> wsParamList = mWorkSpaceParams.get(mCurrentScreen);
        float dstX = mScreenManagerParam.getX(mCurrentScreen) + Const.FIRST_CARD_OFFSET_X;
        float dstY = mScreenManagerParam.getY(mCurrentScreen) + Const.FIRST_CARD_OFFSET_Y;
        for (int i = viewList.size() - 1; i >= 0; i--) {
            View v = viewList.get(i);
            CellLayout.LayoutParams lp = wsParamList.get(i);
            v.setLayoutParams(new LayoutParams(
                    lp.width, lp.height));
            mRecordView.addView(v);
            Animator animation = AnimationUtils.createTranslateAnimator(v,
                    lp.getX() + mOffsetX, dstX , lp.getY() + mOffsetY, dstY );
            stackAnims.add(animation);
        }

        for (int i = 0; i < mViews.size(); i ++) {
            if (i == mCurrentScreen) {
                continue;
            }
            dstX = mScreenManagerParam.getX(i) + Const.FIRST_CARD_OFFSET_X;
            dstY = mScreenManagerParam.getY(i) + Const.FIRST_CARD_OFFSET_Y;
            List<View> list = mViews.get(i);
            wsParamList = mWorkSpaceParams.get(i);
            int count = list.size();
            count = count > Const.MAX_CARDS ? Const.MAX_CARDS : count;
            for (int j = count - 1; j >= 0; j --) {
                View v = list.get(j);
                CellLayout.LayoutParams lp = wsParamList.get(j);
                v.setLayoutParams(new LayoutParams(
                        lp.width, lp.height));
                mRecordView.addView(v);
                Animator animation = AnimationUtils.createTranslateAnimator(v,
                        dstX + (dstX - mScreenManagerParam.getX(mCurrentScreen)) * 5, dstX ,
                        dstY + (dstY - mScreenManagerParam.getY(mCurrentScreen)) * 5, dstY );
                stackAnims.add(animation);
            }
        }

        mRecordView.addView(mHotSeat, new LayoutParams(
                mHotSeat.getWidth(), mHotSeat.getHeight()));
        Animator animation = AnimationUtils.createTranslateAnimator(mHotSeat,
                mHotSeatX, mHotSeatX, mHotSeatY,
                mHotSeatY + mHotSeat.getHeight() * 4);
        stackAnims.add(animation);

        return stackAnims;
    }

    ArrayList<Animator> getEnterStage2Animation() {
        ArrayList<Animator> stackAnims = new ArrayList<Animator>();
        for (int i = 0; i < mViews.size(); i ++) {
            List<View> list = mViews.get(i);
            List<CellLayout.LayoutParams> wsParamList = mWorkSpaceParams.get(i);
            int count = list.size();
            count = count > Const.MAX_CARDS ? Const.MAX_CARDS : count;
            for (int j = count - 1; j >= 0; j --) {
                View v = list.get(j);
                CellLayout.LayoutParams lp = wsParamList.get(j);
                v.setLayoutParams(new LayoutParams(
                        lp.width, lp.height));
                mRecordView.addView(v);
                if (j == count - 1 && j != 0) {
                    v.setAlpha(0.5f);
                }
                float tx = mScreenManagerParam.getX(i) + Const.FIRST_CARD_OFFSET_X;
                float ty = mScreenManagerParam.getY(i) + Const.FIRST_CARD_OFFSET_Y;
                Animator animation = AnimationUtils.createUnfolderAnimator(v,
                        0, -Const.ROTATES[j], Const.CARD_WIDTH / 2,
                        Const.CARD_HEIGHT / 2, tx , tx - Const.RIGHT_TRANS[j],
                        ty, ty + Const.DOWN_TRANS[j]);
                stackAnims.add(animation);
            }
        }

        for (int i = 0; i <  mViews.size(); i++) {
            TextView textView = new TextView(mRecordView.getContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Const.LABEL_TEXT_SIZE);
            textView.setText(((i + 1) / 10) + "" + ((i + 1) % 10));
            mRecordView.addView(textView);
            textView.setX(mScreenManagerParam.getX(i) + Const.LABEL_TEXT_OFFSET_X);
            textView.setY(mScreenManagerParam.getY(i) + Const.LABEL_TEXT_OFFSET_Y);
            textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            textView.setTextColor(i == mCurrentScreen ? Color.RED : Color.WHITE);
            Animator animation = AnimationUtils.createFadeAnimator(textView, 0, 1);
            stackAnims.add(animation);
        }
        return stackAnims;
    }

    void close() {
        DebugTools.log("TransitionAnimationGenerator,close", false);
        mRecordView.removeAllViews();
    }
}
