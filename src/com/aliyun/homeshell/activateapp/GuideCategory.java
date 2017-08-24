package com.aliyun.homeshell.activateapp;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.PageIndicatorView;
import android.support.v4.view.ViewPager.OnPageChangeListener;

public class GuideCategory {

    private ViewPager mCategoryShow;
    private ImageView mBgView;
    private LayoutInflater mInflater;
    private PageIndicatorView mIndicator;

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            switch (state) {
                case ViewPager.SCROLL_STATE_IDLE:

                    break;
                case ViewPager.SCROLL_STATE_DRAGGING:

                    break;
                case ViewPager.SCROLL_STATE_SETTLING:
                    if (mIndicator != null && mCategoryShow != null) {
                        mIndicator.setCurrentPos(mCategoryShow.getCurrentItem());
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private MyPagerAdapter mPagerAdapter = new MyPagerAdapter();

    private class MyPagerAdapter extends PagerAdapter {
        ArrayList<View> mViews = new ArrayList<View>();

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getCount() {
            return mViews.size();
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = mViews.get(position);
            ((ViewPager) container).removeView(view);
        };

        public Object instantiateItem(ViewGroup container, int position) {
            ((ViewPager) container).addView(mViews.get(position));
            return mViews.get(position);
        };

        public void addView(View view) {
            mViews.add(view);
        }
    };

    public GuideCategory(Activity activity, ViewGroup group) {
        mInflater = LayoutInflater.from(activity);
        mCategoryShow = (ViewPager) group.findViewById(R.id.guide_category_show);
        mBgView = (ImageView) group.findViewById(R.id.guide_category_bg);
        mIndicator = (PageIndicatorView)group.findViewById(R.id.category_pageindicator_view);
        mCategoryShow.setAdapter(mPagerAdapter);
        mCategoryShow.setOnPageChangeListener(mOnPageChangeListener);
    }

    public void setBitmaps(ArrayList<Bitmap> bitmaps) {
        if (mInflater == null || mPagerAdapter == null) {
            return;
        }
        for (Bitmap bitmap : bitmaps) {
            View view = mInflater.inflate(R.layout.guide_category_show, null);
            ImageView cacheView = (ImageView) view.findViewById(R.id.guide_category_cache);
            cacheView.setImageBitmap(bitmap);
            mPagerAdapter.addView(view);
        }
        if (mIndicator != null) {
            mIndicator.setMax(bitmaps.size());
            mIndicator.setCurrentPos(0);
        }
        mPagerAdapter.notifyDataSetChanged();
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        if (mBgView != null) {
            mBgView.setImageBitmap(bitmap);
        }
    }
}
