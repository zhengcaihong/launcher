package com.aliyun.homeshell.smartsearch;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.util.Log;

class AppInfo {
    private static final String TAG = "AppInfo";
    long id;
    String intent;
    String title;
    Intent intent2;
    List<String> fullPinyin;
    private int[][] mFullPinyinStartIndex;
    List<String> shortPinyin;
    int type;

    AppInfo(int id, String title, String intent, int type) {
        this.id = id;
        this.intent = intent;
        this.title = title;
        this.type = type;

        List<List<String>> pyList = HanziToPinyin
                .getInstance().getHanziPinyin(title);
        int count = pyList.size();
        mFullPinyinStartIndex = new int[count][];
        fullPinyin = new ArrayList<String>();
        shortPinyin = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            List<String> py = pyList.get(i);
            StringBuilder full = new StringBuilder();
            StringBuilder shortn = new StringBuilder();

            mFullPinyinStartIndex[i] = new int[py.size() + 1];
            int index = 0;
            for (int j = 0; j < py.size(); j++) {
                boolean first = true;
                char[] pinyin = py.get(j).trim().toLowerCase().toCharArray();
                for (char c : pinyin) {
                    char num = (c >= 'a' && c <= 'z') ?
                            HanziToPinyin.Data_Letters_To_T9[c - 'a'] : c;
                    if (first) {
                        shortn.append(num);
                        mFullPinyinStartIndex[i][j] = index;
                        first = false;
                     }
                    full.append(num);
                    index ++;
                }
            }
            mFullPinyinStartIndex[i][py.size()] = index;
            fullPinyin.add(full.toString());
            shortPinyin.add(shortn.toString());
        }

        if (intent != null && !intent.isEmpty()) {
            try {
                intent2 = Intent.parseUri(intent, 0);
            } catch (URISyntaxException e) {
                Log.w(TAG, "parseUri error:title=" + title + ",intent="+intent);
                fullPinyin.clear();
                shortPinyin.clear();
            }
        } else {
            Log.w(TAG, "null intent error: title=" + title);
            fullPinyin.clear();
            shortPinyin.clear();
        }
    }

    MatchResult getFullPinyinMatchResult(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        for (int i = 0; i < fullPinyin.size(); i ++ ) {
            int index = fullPinyin.get(i).indexOf(s);
            if (index == -1) {
                continue;
            }
            for (int j = 0; j <  mFullPinyinStartIndex[i].length; j++) {
                if (mFullPinyinStartIndex[i][j] == index) {
                    int length = s.length();
                    for (int k = 0; k < mFullPinyinStartIndex[i].length - j; k++) {
                        if (mFullPinyinStartIndex[i][j] + length <= mFullPinyinStartIndex[i][j + k]) {
                            MatchResult match = new MatchResult();
                            match.startIndex = j;
                            match.matchLength = k;
                            match.id = id;
                            match.intent = intent;
                            match.intent2 = intent2;
                            match.title = title;
                            match.type = type;
                            return match;
                        }
                    }
                }
            }
        }
        return null;
    }

    MatchResult getShortPinyinMatchResult(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }

        for (int i = 0; i < shortPinyin.size(); i ++ ) {
            int index = shortPinyin.get(i).indexOf(s);
            if (index >= 0) {
                MatchResult match = new MatchResult();
                match.startIndex = index;
                match.matchLength = s.length();
                match.id = id;
                match.intent = intent;
                match.intent2 = intent2;
                match.title = title;
                match.type = type;
                return match;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AppInfo) {
            return ((AppInfo)o).id == id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)id;
    }
}
