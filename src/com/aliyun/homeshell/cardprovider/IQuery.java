package com.aliyun.homeshell.cardprovider;

import android.content.Context;
import android.database.Cursor;

interface IQuery {
        Cursor doQuery(Context context, String[] projection, String selection,
                String[] selectionArgs, String sortOrder);
}
