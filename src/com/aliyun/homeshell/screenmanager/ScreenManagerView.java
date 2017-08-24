package com.aliyun.homeshell.screenmanager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ScreenManagerView extends FrameLayout
                implements OnLongClickListener, ExchangeScreenListener,
                OnClickListener {

    private ScreenManagerParam mParam;
    private ExchangeController mExchangeController;
    private ExchangeScreenAnimationPlayer mAnimationPlayer;
    private List<ScreenCardView> mScreenCardList = new ArrayList<ScreenCardView>();
    private OnItemClickListener mOnItemClickListener;
    private List<TextView> mLabels;
    private boolean mOpenning;

    public static interface OnItemClickListener {
        void onItemClicked(int index);
    }

    private int[] mIndexs;
    private int mCurrentScreen = Const.IVALID_SCREEN_INDEX;

    public ScreenManagerView(Context context) {
        super(context);
    }

    public ScreenManagerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenManagerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void open(List<List<View>> list, ScreenManagerParam param) {
        mParam = param;
        mExchangeController = new ExchangeController(param);
        mExchangeController.setExchangeScreenListener(this);
        mAnimationPlayer = ExchangeScreenAnimationPlayer.getPlayer();
        int count = list.size();
        mIndexs = new int[count];
        for (int i = 0; i < count; i++) {
            ScreenCardView card = new ScreenCardView(getContext());
            card.open(list.get(i));
            mScreenCardList.add(card);
            card.setLayoutParams(new LayoutParams(mParam.getW(i), mParam.getH(i)));
            addView(card);
            card.setX(mParam.getX(i));
            card.setY(mParam.getY(i));
            mIndexs[i] = i;
            CardInfo info = new CardInfo();
            info.index = i;
            card.setTag(info);
            card.setOnLongClickListener(this);
            card.setOnClickListener(this);
        }

        mLabels = new ArrayList<TextView>();
        for (int i = 0; i < count; i++) {
            TextView textView = new TextView(getContext());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Const.LABEL_TEXT_SIZE);
            textView.setText(((i + 1) / 10) + "" + ((i + 1) % 10));
            addView(textView);
            textView.setX(mParam.getX(i) + Const.LABEL_TEXT_OFFSET_X);
            textView.setY(mParam.getY(i) + Const.LABEL_TEXT_OFFSET_Y);
            textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            textView.setTextColor(Color.WHITE);
            mLabels.add(textView);
        }

        setCurrentLabel(mParam.getCurrentScreen());
        mCurrentScreen = mParam.getCurrentScreen();
        mOpenning = true;
    }

    private void setCurrentLabel(int currentScreen) {
        if (mCurrentScreen != currentScreen) {
            if (mCurrentScreen != Const.IVALID_SCREEN_INDEX) {
                mLabels.get(mCurrentScreen).setTextColor(Color.WHITE);
            }
            mCurrentScreen = currentScreen;
            mLabels.get(mCurrentScreen).setTextColor(Color.RED);
        }
    }

    public void close() {
        mOpenning = false;
        if (mAnimationPlayer != null) {
            mAnimationPlayer.cancel();
            mAnimationPlayer.close();
            mAnimationPlayer = null;
        }
        for (ScreenCardView card : mScreenCardList) {
            card.close();
        }
        mScreenCardList.clear();
        mCurrentScreen = Const.IVALID_SCREEN_INDEX;
    }

    @Override
    public boolean onLongClick(View v) {
        CardInfo info =  (CardInfo)v.getTag();
        for (int i : mIndexs) {
            if (mIndexs[i] == info.index && mExchangeController != null) {
                mExchangeController.startDrag(v, info.index);
                break;
            }
        }
        return  true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mOpenning) {
            if (mExchangeController != null) {
                return mExchangeController.onInterceptTouchEvent(ev);
            }
            return super.onInterceptTouchEvent(ev);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mExchangeController != null) {
            return mExchangeController.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void onDragOver(int dragIndex, int dropIndex) {
        if (mAnimationPlayer != null) {
            mAnimationPlayer.open(this, mParam, mScreenCardList,
                    mIndexs, dragIndex, dropIndex);
            mAnimationPlayer.play(null);
        }
    }

    @Override
    public void onDrop(int dropIndex) {
        /* YUNOS BEGIN */
        // ##date:2015/2/17 ##author:zhanggong.zg ##BugID:AF1-298
        if (!mOpenning) {
            return;
        }
        /* YUNOS END */
        //BugID:6120344:onDrop null pointer when a call income
        if (mAnimationPlayer != null) {
            mAnimationPlayer.close();
        }
        int current = mCurrentScreen;
        for (int i = 0; i < mIndexs.length; i++) {
            if (mScreenCardList.size() <= mIndexs[i]) {
                continue;
            }
            ScreenCardView v = mScreenCardList.get(mIndexs[i]);
            v.setX(mParam.getX(i));
            v.setY(mParam.getY(i));
            CardInfo info = (CardInfo)v.getTag();

            if (info.index == mCurrentScreen) {
                current = i;
            }
            info.index = i;
        }
        setCurrentLabel(current);
    }

    public int[] getExchangeResult() {
        return mIndexs;
    }

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClicked(((CardInfo)v.getTag()).index);
        }
    }

    private static class CardInfo {
        int index;
    }

}
