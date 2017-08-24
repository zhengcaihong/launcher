package com.aliyun.homeshell.gadgets;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Region;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.SparseArray;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.homeshell.utils.MemoryUtils;
import com.aliyun.homeshell.utils.Utils;

public final class HomeShellGadgetsRender {
    private static final int RESUME_REFRESH_DELAY = 500;
    private static final int REFRESH_INTERVAL = 30000;
    private static final int ACCERATE_CARD_BG_COLOR = 0xffefebe4;
    private static final int LOCK_CARD_BG_COLOR = 0xff33beff;
    private static final int ACCERATE_CARD_TITLE_COLOR = 0xff000000;
    private static final int LOCK_CARD_TITLE_COLOR = 0xffffffff;
    private static Drawable CONTAINER_DRAWABLE;
    private static Drawable CLEAN_DRAWABLE;
    private static Drawable WAVE_DRAWABLE_GREEN;
    private static Drawable WAVE_DRAWABLE_YELLOW;
    private static Drawable WAVE_DRAWABLE_RED;
    private static Drawable ROCKET_DRAWABLE;
    private static Drawable FAN_DRAWABLE;
    private static HomeShellGadgetsRender mInstance;
    private static int mMemoryUsageValue;
    private static int mDisplayMemUsageValue;
    private static final int MEMORY_USAGE_INTENSE_VALUE = 75;
    private static final int MEMORY_USAGE_CRITICAL_VALUE = 90;

    private int mIconHeight;
    private static List<WeakReference<BubbleTextView> > sAllAccelerateIcon =
            new ArrayList<WeakReference<BubbleTextView>>();
    private Bitmap mUnifiedIconBitmap;
    private AnimationParams mParams = new AnimationParams();
    //TODO: for temp solution
    private static boolean sIsAndroid44 = System.getProperty("http.agent").contains("Android 4.4");

    //refresh handler
    private Handler mHandler = new Handler();

    private int mStartMemoryUsage;
    private int mEndMemoryUsage;
    private BoostAnimationListener mBoostAnimationListener = new BoostAnimationListener(){

        @Override
        public void onAnimationStart(BoostAnimationPlayer player) {
            Context context = LauncherApplication.getContext();
            mStartMemoryUsage = (int) MemoryUtils.getMemoryPercent(context);
            mEndMemoryUsage = (int) MemoryUtils.getMemoryPercent(context);
            mDisplayMemUsageValue = mStartMemoryUsage;
        }

        @Override
        public void onAnimationEnd(BoostAnimationPlayer player) {
            Context context = LauncherApplication.getContext();
            AnimationParamsProvider provider = AnimationParamsProvider.getProvider();
            provider.buildParams(player.getStageAndProgress());
            updateMemoryUsage(context);
        }

        @Override
        public void onAnimationCancel(BoostAnimationPlayer player) {
        }

        @Override
        public void onAnimationRepeat(BoostAnimationPlayer player) {
        }

        @Override
        public void onAnimationUpdate(BoostAnimationPlayer player) {
            AnimationParamsProvider provider = AnimationParamsProvider.getProvider();
            provider.buildParams(player.getStageAndProgress());
            int stage = provider.getStage();
            if (stage == GadgetsConsts.ANIMATION_STAGE_1) {
                mMemoryUsageValue = mStartMemoryUsage * provider.getMemoryUsageRate()
                        / GadgetsConsts.FULL_PROGRESS;
            } else if (stage == GadgetsConsts.ANIMATION_STAGE_3) {
                double multiplier = provider.getMemoryUsageRate() / (double) GadgetsConsts.FULL_PROGRESS;
                mMemoryUsageValue = (int) (mEndMemoryUsage * multiplier);
                mDisplayMemUsageValue = mStartMemoryUsage + (int) ((mEndMemoryUsage - mStartMemoryUsage) * multiplier);
            } else {
                mEndMemoryUsage = (int) MemoryUtils.getMemoryPercent(LauncherApplication.getContext());
            }
            updateTitleInAnimation();
        }};

    //mRefreshLauncher only used for fresh !!!
    private Launcher mRefreshLauncher;
    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refresh(mRefreshLauncher);
        }
    };
    private Runnable mSchedulerRunnable = new Runnable() {
        @Override
        public void run() {
            LauncherModel.postRunnableIdle(mRefreshRunnable);
        }
    };

    private HomeShellGadgetsRender() {}

    public static HomeShellGadgetsRender getRender() {
        if (mInstance == null) {
            synchronized (HomeShellGadgetsRender.class) {
                if (mInstance == null) {
                    mInstance = new HomeShellGadgetsRender();
                }
            }
        }
        return mInstance;
    }

    public void playAccelarateAnimation(Context context) {
        BoostAnimationPlayer.getPlayer(context)
            .setBoostAnimationListener(mBoostAnimationListener).play();
    }

    // YUNOS BEGIN
    // ##modules(HomeShell): ##yongxing.lyx
    // ##BugID:(8113446) ##date:2016/04/13
    // ##description: emerge black screen when press OneKeyAccerate quickly
    public boolean isAccelarateAnimPlaying(Context context) {
        return BoostAnimationPlayer.getPlayer(context).isPlaying();
    }
    // YUNOS BEGIN

    public void registerIcon(BubbleTextView v) {
        boolean hasReg = false;
        for (WeakReference<BubbleTextView> erf : sAllAccelerateIcon) {
            BubbleTextView v1 = erf.get();
            if (v1 == v) {
                hasReg = true;
                break;
            }
        }
        if (!hasReg) {
            if (sAllAccelerateIcon.size() == 0) {
                mMemoryUsageValue = (int) MemoryUtils.getMemoryPercent(v.getContext());
                doStartRender();
            }
            sAllAccelerateIcon.add(new WeakReference<BubbleTextView>(v));
        }
        checkAndRemoveEmpty();
    }

    public void updateMemoryUsage(Context context) {
        mMemoryUsageValue = (int) MemoryUtils.getMemoryPercent(context);
        if (sAllAccelerateIcon.isEmpty()) {
            return;
        }
        String title = mMemoryUsageValue + "%";
        for (WeakReference<BubbleTextView> erf : sAllAccelerateIcon) {
            BubbleTextView v = erf.get();
            if (v != null) {
                if (!title.equals(v.getText())) {
                    v.setText(title);
                }
                // refresh icon in folder
                v.postInvalidate();
            }
        }
    }

    private void doStartRender() {
        // TODO Auto-generated method stub
    }

    private void checkAndRemoveEmpty() {
        List<WeakReference<BubbleTextView>> delList = new ArrayList<WeakReference<BubbleTextView>>();
        for (WeakReference<BubbleTextView> erf : sAllAccelerateIcon) {
            BubbleTextView v1 = erf.get();
            if (v1 == null) {
                delList.add(erf);
            }
        }
        sAllAccelerateIcon.removeAll(delList);
        if (sAllAccelerateIcon.size() == 0) {
            doDestoryRender();
        }
    }

    private void doDestoryRender() {
        // TODO Auto-generated method stub
    }

    private void updateTitleInAnimation() {
        for (WeakReference<BubbleTextView> erf : sAllAccelerateIcon) {
            BubbleTextView v = erf.get();
            if (v != null) {
                v.setText(mDisplayMemUsageValue + "%");
            }
        }
    }

    public String getTitle(Context context) {
        if (sAllAccelerateIcon.size() == 0) {
            mMemoryUsageValue = (int) MemoryUtils.getMemoryPercent(context);
        }
        return mMemoryUsageValue + "%";
    }


    public void drawAccelerateStatus(Context context, Canvas canvas, int width, int height, boolean hotseatMode, boolean supportCard) {
        if (supportCard) {
            int stage = AnimationParamsProvider.getProvider().getStage();
            if(stage == GadgetsConsts.NORMAL_STAGE) {
                drawAccelerateStatusStage0(context, canvas,width, mIconHeight, hotseatMode, supportCard);
            } else if (stage == GadgetsConsts.ANIMATION_STAGE_2) {
                drawAccelerateStatusStage2(context, canvas, width, height, hotseatMode, supportCard);
            } else {
                drawAccelerateStatusStage13(context, canvas, width, height, hotseatMode, supportCard);
            }
        } else {
            Bitmap bm = getUnifiedIconBitmap(context, hotseatMode);
            if (bm != null && !bm.isRecycled()) {
                canvas.drawBitmap(bm, (width - bm.getWidth()) / 2, hotseatMode ? 0 :
                        (width - bm.getWidth()) / 2, null);
            }
        }
    }

    public void drawAccelerateStatusInFolder(Context context, Canvas canvas, int width, int height, boolean supportCard) {
        if (supportCard) {
            drawAccelerateStatusStage0(context, canvas, width, height, false, supportCard);
        } else {
            Bitmap bm = getUnifiedIconBitmap(context, false);
            if (bm != null && !bm.isRecycled()) {
                canvas.drawBitmap(bm, (width - bm.getWidth()) / 2, (height - bm.getWidth()) / 2, null);
            }
        }
    }

    private Bitmap getUnifiedIconBitmap(Context context, boolean hotseatMode) {
        int animationOffset = AnimationParamsProvider.getProvider().getAnimationOffset();
        int waterWaveX = AnimationParamsProvider.getProvider().getWaterWaveX();
        int stage = AnimationParamsProvider.getProvider().getStage();
        if (mUnifiedIconBitmap == null || mUnifiedIconBitmap.isRecycled() ||
                !mParams.isEqual(mMemoryUsageValue, waterWaveX, animationOffset, hotseatMode, stage)) {
            if (mUnifiedIconBitmap != null && !mUnifiedIconBitmap.isRecycled()) {
                mUnifiedIconBitmap.recycle();
            }
            if (CLEAN_DRAWABLE == null) {
                initDrawables(context);
            }
            Drawable d = CLEAN_DRAWABLE;
            int w = d.getIntrinsicWidth();
            int h = mIconHeight = d.getIntrinsicHeight();

            mUnifiedIconBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas c = new Canvas(mUnifiedIconBitmap);
            c.drawColor(Color.TRANSPARENT);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            drawAccelerateStatus(context, c, w, h, hotseatMode, true);
            if (ConfigManager.isLandOrienSupport()) {
                int suggestwidth = ThemeUtils.getIconSize(context);
                if (suggestwidth < 0) {
                    suggestwidth = context.getResources().getDimensionPixelSize(
                            R.dimen.bubble_icon_width);
                }

                int iconwidth = mUnifiedIconBitmap.getWidth();
                if (iconwidth > 0 && iconwidth != suggestwidth) {
                    float scaleW = suggestwidth / (float) iconwidth;
                    mUnifiedIconBitmap = Utils.scaleBitmap(mUnifiedIconBitmap, scaleW, scaleW);
                }
            } else {
                mUnifiedIconBitmap = ThemeUtils.buildUnifiedIcon(context, mUnifiedIconBitmap,
                        ThemeUtils.ICON_TYPE_BROSWER_SHORTCUT);
                mParams.setParams(mMemoryUsageValue, waterWaveX, animationOffset, hotseatMode, stage);
            }
        }
        return mUnifiedIconBitmap;
    }

    private void drawAccelerateStatusStage2(Context context, Canvas canvas, int w, int h, boolean hotseatMode, boolean supportCard) {
        Drawable d0 = CONTAINER_DRAWABLE;
        int w1 = d0.getIntrinsicWidth();
        int h1 = d0.getIntrinsicHeight();
        if (hotseatMode) {
            d0.setBounds((w - w1) / 2, (mIconHeight - h1) / 2, (w + w1) / 2, (mIconHeight + h1) / 2);
        } else {
            d0.setBounds((w - w1) / 2, (w - h1) / 2, (w + w1) / 2, (w + h1) / 2);
        }
        d0.draw(canvas);
        Drawable d = FAN_DRAWABLE;
        float rotate = 0;
        int alpha = 0;
        int animationOffset = AnimationParamsProvider.getProvider().getAnimationOffset();
        if (animationOffset < 20) {
            rotate = animationOffset * 2.7f;
            alpha = animationOffset * 255 / 20;
        } else if (animationOffset < 80) {
            rotate = 54f + (animationOffset - 20) * 5.4f;
            alpha = 255;
        } else {
            rotate = 378f + (animationOffset - 80) * 2.7f;
            alpha = (100 - animationOffset) * 255 / 20;
        }
        if (hotseatMode) {
            canvas.translate(w / 2, mIconHeight / 2);
            canvas.rotate(rotate);
            canvas.translate(-w / 2, -mIconHeight / 2);
        } else {
            canvas.translate(w / 2, w / 2);
            canvas.rotate(rotate);
            canvas.translate(-w / 2, -w / 2);
        }
        int w0 = d.getIntrinsicWidth();
        int h0 = d.getIntrinsicHeight();
        d.setAlpha(alpha);
        if (hotseatMode) {
            d.setBounds((w - w0) / 2, (mIconHeight - h0) / 2, (w + w0) / 2, (mIconHeight + h0) / 2);
        } else {
            d.setBounds((w - w0) / 2, (w - h0) / 2, (w + w0) / 2, (w + h0) / 2);
        }
        d.draw(canvas);
        if (hotseatMode) {
            canvas.translate(w / 2, mIconHeight / 2);
            canvas.rotate(-rotate);
            canvas.translate(-w / 2, -mIconHeight / 2);
        } else {
            canvas.translate(w / 2, w / 2);
            canvas.rotate(-rotate);
            canvas.translate(-w / 2, -w / 2);
        }
        Drawable d2 = ROCKET_DRAWABLE;
        int w2 = d2.getIntrinsicWidth();
        int h2 = d2.getIntrinsicHeight();
        if (hotseatMode) {
            d2.setBounds((w - w2) / 2, (mIconHeight - h2) / 2, (w + w2) / 2, (mIconHeight + h2) / 2);
        } else {
            d2.setBounds((w - w2) / 2, (w - h2) / 2, (w + w2) / 2, (w + h2) / 2);
        }
        d2.draw(canvas);
        return;
    }

    private void drawAccelerateStatusStage13(Context context, Canvas canvas, int w, int h, boolean hotseatMode, boolean supportCard) {
        Drawable layers[] = new Drawable[]{CLEAN_DRAWABLE, CONTAINER_DRAWABLE, getWaveDrawable(), ROCKET_DRAWABLE};
        int w1 = layers[1].getIntrinsicWidth();
        int h1 = layers[1].getIntrinsicHeight();
        if (hotseatMode) {
            layers[1].setBounds((w - w1) / 2, (mIconHeight - h1) / 2, (w + w1) / 2, (mIconHeight + h1) / 2);
        } else {
            layers[1].setBounds((w - w1) / 2, (w - h1) / 2, (w + w1) / 2, (w + h1) / 2);
        }

        canvas.save();
        int centerX,centerY;
        if (hotseatMode) {
            centerX = w / 2;
            centerY = mIconHeight / 2;
        } else {
            centerY = centerX = w / 2;
        }
        Path path = new Path();
        path.addCircle(centerX, centerY, w1 / 2, Direction.CCW);
        canvas.clipPath(path, Region.Op.REPLACE);
        layers[1].setFilterBitmap(true);
        layers[1].draw(canvas);
        int w2 = layers[2].getIntrinsicWidth();
        int h2 = layers[2].getIntrinsicHeight();
        int x2, y2;
        int waterWaveX = AnimationParamsProvider.getProvider().getWaterWaveX();
        if (hotseatMode) {
            x2 = (w - w1) / 2 - w2 * waterWaveX / 100;
            y2 = (mIconHeight - h1) / 2 + ((mMemoryUsageValue == 100) ?
                    -h2 / 10 : h2 * (100 - mMemoryUsageValue) / 100);
        } else {
            x2 = (w - w1) / 2 - w2 * waterWaveX / 100;
            y2 = (w - h1) / 2 +  ((mMemoryUsageValue == 100) ?
                    -h2 / 10 : h2 * (100 - mMemoryUsageValue) / 100);
        }
        layers[2].setBounds(x2, y2, x2 + w2, y2 + h2);

        layers[2].draw(canvas);
        int w3 = layers[3].getIntrinsicWidth();
        int h3 = layers[3].getIntrinsicHeight();
        int animationOffset = AnimationParamsProvider.getProvider().getAnimationOffset();
        if (hotseatMode) {
            layers[3].setBounds((w - w3) / 2, (mIconHeight - h3) / 2 - animationOffset * w1 / 60,
                    (w + w3) / 2, (mIconHeight + h3) / 2 - animationOffset * w1 / 60);
        } else {
            layers[3].setBounds((w - w3) / 2, (w - h3) / 2 - animationOffset * w1 / 60,
                    (w + w3) / 2, (w + h3) / 2 - animationOffset * w1 / 60);
        }
        layers[3].draw(canvas);
        canvas.restore();
        return;
    }

    private static Drawable getWaveDrawable() {
        if (mMemoryUsageValue < MEMORY_USAGE_INTENSE_VALUE) {
            return WAVE_DRAWABLE_GREEN;
        } else if (mMemoryUsageValue < MEMORY_USAGE_CRITICAL_VALUE) {
            return WAVE_DRAWABLE_YELLOW;
        } else {
            return WAVE_DRAWABLE_RED;
        }
    }

    private void drawAccelerateStatusStage0(Context context, Canvas canvas, int w, int h, boolean hotseatMode, boolean supportCard) {
        if (CLEAN_DRAWABLE == null) {
            initDrawables(context);
        }
        Drawable layers[] = new Drawable[]{CLEAN_DRAWABLE, CONTAINER_DRAWABLE, getWaveDrawable(), ROCKET_DRAWABLE};

        int w1 = layers[1].getIntrinsicWidth();
        int h1 = layers[1].getIntrinsicHeight();
        if (hotseatMode) {
            layers[1].setBounds((w - w1) / 2, (h - h1) / 2, (w + w1) / 2, (h + h1) / 2);
        } else {
            layers[1].setBounds((w - w1) / 2, (w - h1) / 2, (w + w1) / 2, (w + h1) / 2);
        }
        layers[1].draw(canvas);

        canvas.save();
        int centerX;
        centerX = w / 2;
        int w2 = layers[2].getIntrinsicWidth();
        int h2 = layers[2].getIntrinsicHeight();
        Bitmap bm = Bitmap.createBitmap(w1, h1, Config.ARGB_8888);
        bm.eraseColor(Color.TRANSPARENT);
        Canvas c = new Canvas(bm);
        layers[2].setBounds(0, ((mMemoryUsageValue == 100) ? -h2 / 10 :
            h2 * (100 - mMemoryUsageValue) / 100), w2, ((mMemoryUsageValue == 100) ? -h2 / 10
                    : h2 * (100 - mMemoryUsageValue) / 100) + h2);
        layers[2].draw(c);

        if (!sIsAndroid44 && ConfigManager.isLandOrienSupport()) {
            BitmapShader bs = new BitmapShader(bm, TileMode.CLAMP, TileMode.CLAMP);
            Paint pt = new Paint();
            pt.setAntiAlias(true);
            pt.setShader(bs);
            canvas.drawCircle(centerX, centerX, w1/2, pt);
        } else {
            int centerY = hotseatMode ? h / 2 : w / 2;
            for (int i = 0; i < w1; i ++) {
                for (int j = 0; j < h1; j ++) {
                    if ((i - w1 / 2) * (i - w1 / 2) + (j - h1 / 2) * (j - h1 / 2) > w1 * w1 / 4) {
                        bm.setPixel(i, j, Color.TRANSPARENT);
                    }
                }
            }
            Paint pt = new Paint();
            pt.setAntiAlias(true);
            canvas.drawBitmap(bm, centerX - w1 / 2, centerY - w1 / 2, pt);
        }
        int w3 = layers[3].getIntrinsicWidth();
        int h3 = layers[3].getIntrinsicHeight();
        int animationOffset = AnimationParamsProvider.getProvider().getAnimationOffset();
        if (hotseatMode) {
            layers[3].setBounds((w - w3) / 2, (h - h3) / 2-animationOffset * w1 / 60,
                    (w + w3) / 2, (h + h3) / 2 - animationOffset * w1 / 60);
        } else {
            layers[3].setBounds((w - w3)/2, (w - h3) / 2- animationOffset * w1 / 60,
                    (w + w3) / 2, (w + h3) / 2 - animationOffset * w1 / 60);
        }
        layers[3].draw(canvas);
        canvas.restore();
        return;
    }

    public Drawable getAccesalarateIcon(Context context, boolean supportCard) {
        if (CLEAN_DRAWABLE == null) {
            initDrawables(context);
        }
        Drawable[] layer = new Drawable[2];
        layer[0] = CLEAN_DRAWABLE;
        layer[1] = CONTAINER_DRAWABLE;
        int w0 = layer[0].getIntrinsicWidth();
        int w1 = layer[1].getIntrinsicWidth();
        int h0 = mIconHeight = layer[0].getIntrinsicHeight();
        int h1 = layer[1].getIntrinsicHeight();

        layer[0].setBounds(0, 0, w0, h0);
        layer[1].setBounds((w0 - w1) / 2, (h0 - h1) / 2,
                (w0 + w1) / 2, (h0 + h1) / 2);
        Bitmap bm = Bitmap.createBitmap(w0, h0, Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        c.drawColor(Color.TRANSPARENT);
        if (supportCard) {
            layer[0].draw(c);
            layer[1].draw(c);
        }
        return new BitmapDrawable(context.getResources(), bm);
    }

    public static boolean isOneKeyAccerateShortCut(ItemInfo info) {
        return info instanceof ShortcutInfo && info.itemType ==
                LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT &&
                ((ShortcutInfo)info).intent != null &&
                GadgetsConsts.TYPE_ONE_KEY_ACCELERATE ==
                ((ShortcutInfo)info).intent.getIntExtra(GadgetsConsts.TYPE_SHORTCUT, GadgetsConsts.TYPE_ERROE);
    }

    public static boolean isOneKeyLockShortCut(ItemInfo info) {
        return info instanceof ShortcutInfo && info.itemType ==
                LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT &&
                ((ShortcutInfo)info).intent != null &&
                GadgetsConsts.TYPE_ONE_KEY_LOCK ==
                ((ShortcutInfo)info).intent.getIntExtra(GadgetsConsts.TYPE_SHORTCUT, GadgetsConsts.TYPE_ERROE);
    }

    public static boolean isHomeShellGadgets(ItemInfo info) {
        return info instanceof ShortcutInfo && info.itemType ==
                LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT &&
                ((ShortcutInfo)info).intent != null &&
                GadgetsConsts.TYPE_ERROE !=
                ((ShortcutInfo)info).intent.getIntExtra(GadgetsConsts.TYPE_SHORTCUT, GadgetsConsts.TYPE_ERROE);
    }

    public void onResume(Launcher l) {
        mRefreshLauncher = l;
        mHandler.postDelayed(mSchedulerRunnable, RESUME_REFRESH_DELAY);
    }

    private void refresh(final Launcher l) {
        if (l == null) {
            return;
        }
        if (!shouldSkipUpdate(l)) {
            updateMemoryUsage(l);
            mHandler.postDelayed(mSchedulerRunnable, REFRESH_INTERVAL);
        } else {
            mHandler.postDelayed(mSchedulerRunnable, RESUME_REFRESH_DELAY);
        }
    }

    private boolean shouldSkipUpdate(Launcher l) {
        // TODO : should skip update for better performance
        // such as page moving etc.
        return l.getWorkspace().isPageMoving();
    }

    public void onPause(Launcher l) {
        mRefreshLauncher = null;
        mHandler.removeCallbacks(mSchedulerRunnable);
    }

    public void onThemeChange() {
        if (mUnifiedIconBitmap != null) {
            mUnifiedIconBitmap.recycle();
            mUnifiedIconBitmap = null;
        }
    }

    public Drawable createBackgroundIcon(Resources res, ItemInfo info, Bitmap icon){
        int key = getKey(info);
        Drawable d = mBgCache.get(key);
        if (d == null) {
            int width = res.getDimensionPixelSize(R.dimen.workspace_cell_width);
            int height = res.getDimensionPixelSize(R.dimen.workspace_cell_height);
            int toppadding = res.getDimensionPixelSize(R.dimen.card_icon_generator_top_padding);
            d = new GadgetsCardIconDrawable(res, getBgColor(info), width, height, toppadding);
            mBgCache.put(key, d);
        }
        return d;
    }
    private int getBgColor(ItemInfo info) {
        return isOneKeyAccerateShortCut(info) ? ACCERATE_CARD_BG_COLOR : LOCK_CARD_BG_COLOR;
    }
    
    public static int getTitleColor(ItemInfo info) {
        return isOneKeyAccerateShortCut(info) ? ACCERATE_CARD_TITLE_COLOR : LOCK_CARD_TITLE_COLOR;
    }

    private int getKey(ItemInfo info) {
        return isOneKeyAccerateShortCut(info) ? 0 : 1;
    }
    private SparseArray<Drawable> mBgCache = new SparseArray<Drawable>();

    private void initDrawables(Context context) {
        CLEAN_DRAWABLE = context.getResources().getDrawable(R.drawable.bg_clean_up);
        CONTAINER_DRAWABLE = context.getResources().getDrawable(R.drawable.bg_container);
        WAVE_DRAWABLE_GREEN = context.getResources().getDrawable(R.drawable.bg_waterlines);
        WAVE_DRAWABLE_YELLOW = context.getResources().getDrawable(R.drawable.bg_waterlines_yellow);
        WAVE_DRAWABLE_RED = context.getResources().getDrawable(R.drawable.bg_waterlines_red);
        ROCKET_DRAWABLE = context.getResources().getDrawable(R.drawable.ic_rocket);
        FAN_DRAWABLE = context.getResources().getDrawable(R.drawable.bg_fan);
    }
}
