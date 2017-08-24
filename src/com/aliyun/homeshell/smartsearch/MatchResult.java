package com.aliyun.homeshell.smartsearch;

import android.content.Intent;

class MatchResult {

    long id;
    String intent;
    String title;
    Intent intent2;

    int startIndex;
    int matchLength;
    int type;

    @Override
    public String toString() {
        return title+","+startIndex+","+matchLength;
    }
}
