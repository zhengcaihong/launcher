package com.aliyun.homeshell;

import aliyun.util.ImageUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class ForstedGlassUtils {
	private static final String TAG = "ForstedGlassUtils";
	//private static final int scale = 10;
	private static final int blur_radius = 2;
	private static Bitmap mScreenShot = null;
    private static final boolean JNI_BLUR = true;

	public static Bitmap getScreenShot() {
		Log.d(TAG, "getscreenshot : begin");
		//int screenWidth = LauncherApplication.getScreenWidth();
		//int screenHeight = LauncherApplication.getScreenHeight();
		if (mScreenShot != null && !mScreenShot.isRecycled()) {
			mScreenShot.recycle();
			mScreenShot = null;
		}
                //modified by dongjun for codebase 3.0 to complie 
//		Bitmap src = null;//Surface.screenshot(screenWidth, screenHeight);
//		if (src != null && src.getWidth() > 0 && src.getHeight() > 0) {
//			mScreenShot = Bitmap.createScaledBitmap(src,
//					(screenWidth + scale - 1) / scale,
//					(screenHeight + scale - 1) / scale, true);
//		}
		Log.d(TAG, "getscreenshot : end");
		return mScreenShot;
	}

	public static void setForstedGlassBackground(Activity activity) {
		Log.d(TAG, "Gaussian blur : begin");
//		Bitmap screenshot = getScreenShot();
//		Bitmap snap = null;
//		if (screenshot != null) {
//			snap = fastblur(activity, screenshot, blur_radius);
//		}
//		activity.getWindow().getDecorView()
//				.setBackground(new BitmapDrawable(snap));
		activity.getWindow().getDecorView().setBackgroundColor(0x55000000);
		Log.d(TAG, "Gaussian blur : end");
	}

    public static BitmapDrawable getForstedGlassBackground(Context context) {
        Log.d(TAG, "Gaussian blur : begin");
        Bitmap screenshot = getScreenShot();
        Bitmap snap = null;
        if (screenshot != null) {
            snap = fastblur(context, screenshot, blur_radius);
        }
        return new BitmapDrawable(snap);
    }

	public static void clearForstedGlassBackground(Activity activity) {
		activity.getWindow().getDecorView().setBackground(null);
		if (mScreenShot != null && !mScreenShot.isRecycled()) {
			mScreenShot.recycle();
			mScreenShot = null;
		}
	}

	public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {
		if (sentBitmap == null) {
			Log.w(TAG, "fastblur : src bitmap is null");
			return null;
		}
		long beginBlur = System.currentTimeMillis();
		if (!JNI_BLUR && VERSION.SDK_INT > 16) {
			Bitmap bitmap = sentBitmap;
			final RenderScript rs = RenderScript.create(context);
			final Allocation input = Allocation.createFromBitmap(rs,
					sentBitmap, Allocation.MipmapControl.MIPMAP_NONE,
					Allocation.USAGE_SCRIPT);
			final Allocation output = Allocation.createTyped(rs,
					input.getType());
			final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs,
					Element.U8_4(rs));
			script.setRadius(radius /* e.g. 3.f */);
			script.setInput(input);
			script.forEach(output);
			output.copyTo(bitmap);
			long endBlur = System.currentTimeMillis();
			Log.d(TAG, "RenderScript blur render script duiration = "
					+ (endBlur - beginBlur));
			return bitmap;
		} else {
            Bitmap bmp = sentBitmap;
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            Bitmap blurred = bmp.copy(Bitmap.Config.ARGB_8888, true);
            blurred = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            blurred.eraseColor(0xff000000);
            ImageUtils.fastBlur(bmp, blurred, 8);
            long endBlur = System.currentTimeMillis();
            Log.d(TAG, "jni blur render script duiration = "
                    + (endBlur - beginBlur));
            return blurred;
		}
	}
}
