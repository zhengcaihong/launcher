package com.aliyun.homeshell.smartsearch;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

class SearchTask {

    private Context mContext;
    private OnSearchDoneListener mOnSearchDoneListener;
    private String mSearchString;
    private SearchRunnable mRunnable;
    private HandlerThread mThread;
    private Handler mHandler;

    static interface OnSearchDoneListener {
        void onSearchDone(final List<MatchResult> match);
    }

    SearchTask(Context context, OnSearchDoneListener l) {
        mContext = context;
        mOnSearchDoneListener = l;
        mRunnable = new SearchRunnable();
        mThread = new HandlerThread("SearchThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

    }

    void doSearch(String s) {
        mSearchString = s;
        mHandler.removeCallbacks(mRunnable);
        mHandler.post(mRunnable);
    }

    private class SearchRunnable extends Thread {
        private SmartSearchEngine mSmartSearchEngine;
        @Override
        public void run() {
            if (mSmartSearchEngine == null) {
                mSmartSearchEngine = new SmartSearchEngine(mContext);
            }
            if (mOnSearchDoneListener != null) {
                List<MatchResult> list = mSmartSearchEngine.pinyinSearch(mSearchString);
                mOnSearchDoneListener.onSearchDone(list);
            }
        }
    }

    public void cancelSearch() {
        mSearchString = "";
        mHandler.removeCallbacks(mRunnable);
    }

}
