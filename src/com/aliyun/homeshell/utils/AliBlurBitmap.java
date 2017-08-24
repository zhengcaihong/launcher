package com.aliyun.homeshell.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class AliBlurBitmap {
    private static final boolean DEBUG = false;
    private static final String TAG = "AliOverlayDisplayWindow";
    private static RenderScript mRS;
    private static ScriptIntrinsicBlur mBlurScript;

    public static Bitmap blurBitmap(Context context, Bitmap bitmap){
        if(DEBUG) Log.d(TAG, "blurBitmap( bitmap = " + bitmap + ", context = " + context + " );");
        if(context == null){
            return null;
        }
        if(bitmap == null){
            return null;
        }
        if(mRS == null){
            mRS = RenderScript.create(context);
        }
        if(DEBUG) Log.d(TAG, "blurBitmap: mRS = " + mRS);
        if(mBlurScript == null){
            mBlurScript = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        }
        if(DEBUG) Log.d(TAG, "blurBitmap: mBlurScript = " + mBlurScript);

        Allocation allIn = Allocation.createFromBitmap(mRS, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation allOut = Allocation.createTyped(mRS, allIn.getType());

        //Set the radius of the blur
        mBlurScript.setRadius(25.f);
        //Perform the Renderscript
        mBlurScript.setInput(allIn);
        mBlurScript.forEach(allOut);

        //Copy the final bitmap created by the out Allocation to the bitmap
        allOut.copyTo(bitmap);

        return bitmap;
    }
}

