package com.aliyun.homeshell.lifecenter;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

public class BlurBackground extends FrameLayout {

    private static final String TAG = "BlurBackground";
    private BlurBitmapChangeReceiver mReceiver;

    private Drawable mBlurDrawable;
    private boolean isLiveWallpaper = false;
    private final static int DEFAULT_COLOR = 0x2D000000;

    public BlurBackground(Context context, CardBridge bridge) {
        this(context);
        bridge.setHostCapabilityEnable(CardBridge.HOST_CAPABILITY_SCROLLINGBLUR, true);
    }

    public BlurBackground(Context context) {
        super(context);
    }

    private void checkLiveWallpaper(Context context, boolean firstCheck) {
        boolean liveWallpaper = WallpaperManager.getInstance(context).getWallpaperInfo() != null;
        if (!firstCheck && isLiveWallpaper == liveWallpaper) {
            return;
        }

        isLiveWallpaper = liveWallpaper;
        if (isLiveWallpaper) {
            setBackgroundColor(DEFAULT_COLOR);
        } else {
            setBackground(mBlurDrawable);
        }
    }

    private void init(final Context context) {
        setAlpha(0.f);

        checkLiveWallpaper(context, true);
        mReceiver = new BlurBitmapChangeReceiver(context, this);
        mReceiver.register();
    }

    private boolean startBlurService(Context context) {
        String serviceName = "com.yunos.lifecard.action.LifeCardWallPaperService";
        String action = "com.yunos.lifecard.action.getWallpaper";

        Intent intent = new Intent(serviceName);
        intent.putExtra("action", action);
        //BugID:8364585:Service Intent must be explicit.
        intent.setPackage("com.yunos.lifecard");

        boolean res = false;
        try {
            if (context.startService(intent) != null) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed in : " + e.getMessage());
        }

        return res;
    }

    private void deInit(Context context) {
        if (mReceiver != null) {
            mReceiver.unregister();
        }

        recycleDrawable(mBlurDrawable);
    }

    public void onWorkspaceScrolled(float progress) {
        if (progress < 0) {
            progress = -progress;
        }

        setAlpha(progress);
    }

    @Override
    protected void onAttachedToWindow() {
        init(getContext());
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        deInit(getContext());
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus) {
            checkLiveWallpaper(getContext(), false);
            Log.d(TAG, "onWindowFocusChanged isLiveWallpaper : " + isLiveWallpaper);
        }

    }

    private class BlurBitmapChangeReceiver extends BroadcastReceiver {
        private static final String ACTION__BLURBITMAP_CHANGE = "com.yunos.lifecard.action.wallpaperchange";

        private static final String BLUR_BITMAP = "blur_bitmap";

        private boolean isBlurFromLifeCenter = true;

        private View mBlurView;
        private Context mContext;

        public BlurBitmapChangeReceiver(Context context, View blurView) {
            mContext = context;
            mBlurView = blurView;

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (isBlurFromLifeCenter) {
                        startBlurService(mContext);
                    } else {
                        mBlurDrawable = getBlurWallpaper(mContext);
                        if (mBlurDrawable != null) {
                            mBlurView.setBackground(mBlurDrawable);
                        }
                    }
                }
            });
        }

        public void register() {
            IntentFilter filter = new IntentFilter();
            @SuppressWarnings("deprecation")
            String action = isBlurFromLifeCenter ? ACTION__BLURBITMAP_CHANGE
                    : Intent.ACTION_WALLPAPER_CHANGED;
            filter.addAction(action);

            mContext.registerReceiver(this, filter);
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive : " + action);

            Drawable drawable = null;
            try {
                if (ACTION__BLURBITMAP_CHANGE.equals(action)) {
                    byte[] bis = intent.getByteArrayExtra(BLUR_BITMAP);
                    Bitmap bmp = BitmapFactory.decodeByteArray(bis, 0,
                            bis.length);

                    Log.d(TAG,
                            "onReceive blur bitmap size : "
                                    + bmp.getAllocationByteCount() + " w :"
                                    + bmp.getWidth() + " h : "
                                    + bmp.getHeight());

                    drawable = new BitmapDrawable(bmp);
                } else if (Intent.ACTION_WALLPAPER_CHANGED.equals(action)) {
                    drawable = getBlurWallpaper(context);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in onReceive : " + e.getMessage());
            }

            if (drawable != null) {
                recycleDrawable(mBlurDrawable);
                mBlurDrawable = drawable;
                if (!isLiveWallpaper) {
                    mBlurView.setBackground(drawable);
                }
            }
        }
    }

    private static void recycleDrawable(Drawable d) {
        if (d instanceof BitmapDrawable) {
            ((BitmapDrawable)d).getBitmap().recycle();
        }
    }

    private Drawable getBlurWallpaper(Context context) {
        long start = System.currentTimeMillis();
        Drawable d = null;

        try {
            Bitmap wallpaper = getWallPaper(context);
            if (wallpaper != null) {
                d = blur(wallpaper, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed in getBlurWallpaper : " + e.getMessage());
        }

        Log.d(TAG, "getBlurWallpaper duration : "
                + (System.currentTimeMillis() - start));
        return d;
    }

    private Bitmap getWallPaper(Context context) {
        WallpaperManager wallpaper = WallpaperManager.getInstance(context);
        Drawable paper = wallpaper.getDrawable();
        return ((BitmapDrawable) paper).getBitmap();
    }

    @SuppressWarnings("deprecation")
    private static Drawable blur(Bitmap bmp, View blurView) {
        float factor = 6f;
        float r = 12;

        int w = blurView.getMeasuredWidth();
        int h = blurView.getMeasuredHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap overlay = Bitmap.createBitmap(
                (int) (w / factor),
                (int) (h / factor),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(overlay);
        canvas.translate((-blurView.getLeft()) / factor, (-blurView.getTop())
                / factor);
        canvas.scale(1 / factor, 1 / factor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bmp, 0, 0, paint);

        RenderScript mRenderScript = RenderScript.create(blurView.getContext());
        ScriptIntrinsicBlur mScript = ScriptIntrinsicBlur.create(mRenderScript,
                Element.U8_4(mRenderScript));
        final Allocation input = Allocation.createFromBitmap(mRenderScript,
                overlay, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(mRenderScript,
                input.getType());
        mScript.setRadius(r);
        mScript.setInput(input);
        mScript.forEach(output);
        output.copyTo(overlay);

        Log.d(TAG, "blur bitmap size :" + overlay.getAllocationByteCount());

        return new BitmapDrawable(overlay);
    }
}
