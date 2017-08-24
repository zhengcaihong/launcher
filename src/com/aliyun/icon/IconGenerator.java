/*
 * Created by Tian Junlin (junlin.tjl@alibaba-inc.com)
 * Date : 2014/05/13
 * use libicongenerator.so to create big icon.
 */
package com.aliyun.icon;

import android.graphics.Bitmap;

public class IconGenerator {
    static {
        System.loadLibrary("icongenerator");
    }

    public native int generator(int input[], int parameters[], int output[]);

    public native static long getBitmapAverageColor(Bitmap srcBitmap, int startX, int startY,
            int w, int h, int stepX, int stepY);
}
