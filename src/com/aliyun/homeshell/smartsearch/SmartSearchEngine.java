package com.aliyun.homeshell.smartsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.LauncherSettings.Favorites;

public class SmartSearchEngine {
    private Context mContext;

    private static final Comparator<MatchResult> mComparator = new Comparator<MatchResult>() {

        @Override
        public int compare(MatchResult m1, MatchResult m2) {
            return (m1.startIndex - m2.startIndex) * 16 + (m1.title.length() - m2.title.length());
        }
    };

    public SmartSearchEngine(Context context) {
        mContext = context;
    }

    public List<MatchResult> pinyinSearch(String s) {
        List<MatchResult> result = new ArrayList<MatchResult>();
        List<AppInfo> infoList = new ArrayList<AppInfo>();
        Cursor c1 = mContext.getContentResolver()
                .query(LauncherSettings.Favorites.CONTENT_URI,
                new String[] { "_id", "title", "intent", "itemType"},
                "shortPinyin LIKE '%" + s + "%' AND container <> ?",
                new String[] { String.valueOf(Favorites.CONTAINER_HIDESEAT) },
                null);
        if (c1 != null) {
            while(c1.moveToNext()) {
                AppInfo info = new AppInfo(c1.getInt(0),
                        c1.getString(1), c1.getString(2), c1.getInt(3));
                MatchResult match = info.getShortPinyinMatchResult(s);
                if (match != null) {
                    infoList.add(info);
                    result.add(match);
                }
            }
            c1.close();
        }

        Cursor c = mContext.getContentResolver()
                .query(LauncherSettings.Favorites.CONTENT_URI,
                new String[] { "_id", "title", "intent", "itemType"},
                "fullPinyin LIKE '%" + s + "%' AND container <> ?",
                new String[] { String.valueOf(Favorites.CONTAINER_HIDESEAT) },
                null);
        if (c != null) {
            while(c.moveToNext()) {
                AppInfo info = new AppInfo(c.getInt(0), c.getString(1),
                        c.getString(2), c.getInt(3));
                if (!infoList.contains(info)) {
                    MatchResult match = info.getFullPinyinMatchResult(s);
                    if (match != null) {
                        result.add(match);
                    }
                }
            }
            c.close();
        }

        Collections.sort(result, mComparator);
        return result;
    }

}
