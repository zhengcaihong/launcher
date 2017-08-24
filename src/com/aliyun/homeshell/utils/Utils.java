/**
 * Copyright 2012 Alibaba Group
 *
 * @version   1.0 2 Dec 2012
 * @author   zujin.zhangzj 
 */

package com.aliyun.homeshell.utils;
   
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.DeferredHandler;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.backuprestore.BackupUitil;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.utility.utils.ACA;
import com.aliyun.homeshell.searchui.SearchBridge;

public class Utils {
    private static final String TAG = "Utils";
    public static final boolean DEBUG = true;
    
    /* YUNOS BEGIN */
    // ##date:2014/4/21 ##author:hongchao.ghc ##BugID:111144
    public static final boolean RUN_ON_YUN_OS = false;
    /* YUNOS END */
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;//android 2.3 没有 DisplayMetrics.DENSITY_XXHIGH
    private static final String mSearchWidgetPackage = "com.yunos.alimobilesearch";
    private static final String mSearchNowCardWidgetPackage = "com.yunos.lifecard";
    private static  final Intent mSearchIntent = new Intent("android.intent.action.ALISEARCH");
    private static  final Intent mThemeIntent = (new Intent("com.aliyun.auitheme.action.VIEW")).addCategory("com.aliyun.auitheme.category.THEMEMANAGER");
    private static  final Intent mOldThemeIntent = (new Intent("com.aliyun.auitheme.action.VIEW")).addCategory("com.aliyun.auitheme.category.LOCALTHEME");
    private static  final Intent mWallpaperIntent = new Intent("com.aliyun.auitheme.action.VIEW").addCategory("com.aliyun.auitheme.category.WALLPAPERMANAGER");
    private static  Intent mSearchPageChangedIntent = new Intent("com.aliyun.homeshell.searchwidget.page");
    private static DeferredHandler mHandler = new DeferredHandler();
//    
//    public static int ICON_WIDTH;
//    public static int ICON_HEIGHT;
//    private static int STATUSBAR_HEIGHT = -1;
//    private static String DEVICE = null;
//
//    public static final String PREFERENCES_CONFIG = "config";
//    public static final String PREFERENCES_FILE = "/data/data/com.aliyun.homeshell/shared_prefs/";
//    public static final String REFS_NOTIFICATION_HASNEW = "refs_notification_hasnew";
//    public static final String REFS_ICONSTYLE = "refs_iconstyle";
//    public static final String RUN_FIRST_TIME = "run_first_time";
//    public static final String LAST_DOIC_TIME = "last_doic_time";
      public static final boolean FLAG_DEBUG = true;
//    public static final String ALLAPPS_PRESET_PREFIX = "allapps_icon_list_";
//    public static final String UNIFY_NAME_DEFAULT = "default";
//    public static final String DEFINE_TYPE_STRING = "string";
//    public static final String DEFINE_TYPE_XMLFILE = "xml";
//
//    public static final String ACTION_EXTERNAL_APPLICATIONS_LOADING =
//            "android.intent.action.EXTERNAL_APPLICATIONS_LOADING";
//    public static final String ACTION_EXTERNAL_APPLICATIONS_DEXOPT =
//            "android.intent.action.EXTERNAL_APPLICATIONS_DEXOPT";
//
//    public final static String ACTION_WIDGET_ADDED = "com.aliyun.homeshell.action.ADD_WIDGET";
//    public final static String WIDGET_COMPONENT = "component";
//    public final static int DEFAULT_CELL_COUNT_X = 4;
//    public final static int DEFAULT_CELL_COUNT_Y = 4;
//    public final static int WIDGETS_PER_PAGE = 6;
//    
//    // ********screen layout, UI-related start*******
//    public static int CELL_COUNT_LONG_AXIS;
//    public static int CELL_COUNT_SHORT_AXIS;
//
//    public static int BUBBLE_WIDTH;
//    public static int BUBBLE_HEIGHT;
//
//    // *******currently is 0**********
//    public static int WORKSPACE_LONG_AXIS_START_PADDING;
//    public static int WORKSPACE_LONG_AXIS_END_PADDING;
//    public static int WORKSPACE_SHORT_AXIS_START_PADDING;
//    public static int WORKSPACE_SHORT_AXIS_END_PADDING;
//    public static int WORKSPACE_BETWEEN_SCREEN_GAP;
//    
//    public static String NEW_MARK_SHORTCUT;
//    public static String LONG_TITLE_SUFFIX;
//    
//    private static Paint mBubbleTitlePaint;
//    
//    /****************dock param*********************/
//    public static int DOCK_WINDOW_HEIGHT = 600;
//    public static int DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT = 4;
//    public static int DOCK_WINDOW_Y_WAIT_SHOW_OFFSET = -DOCK_WINDOW_HEIGHT + DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT;
//    public static int DOCK_WINDOW_Y_HIDE_SHOW_OFFSET = -DOCK_WINDOW_HEIGHT - DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT;
//    public static int DOCK_HEIGHT;
//    public static int DOCK_WIDTH;
//    public static int DOCK_BACKGROUD_HEIGHT;
//    public static boolean IS_SHOWDOCK_INVERT = false;
//    public static boolean IS_SHOW_GLOBAL_DOCK = false;
//    public static int DOCK_SHOW_COVER_HEIGHT;
//
//    public static int MENU_BACKGROUD_HEIGHT;
//    public static void initParams(Resources r) {
//        ICON_WIDTH = r.getDimensionPixelSize(R.dimen.bubble_icon_width);
//        ICON_HEIGHT = r.getDimensionPixelSize(R.dimen.bubble_icon_height);
//        
//        CELL_COUNT_LONG_AXIS = r.getInteger(R.integer.cell_count_long_axis);
//        CELL_COUNT_SHORT_AXIS = r.getInteger(R.integer.cell_count_short_axis);
//
//        BUBBLE_WIDTH = r.getDimensionPixelSize(R.dimen.workspace_cell_width);
//        BUBBLE_HEIGHT = r.getDimensionPixelSize(R.dimen.workspace_cell_height);
//
//        WORKSPACE_LONG_AXIS_START_PADDING = r.getDimensionPixelSize(R.dimen.workspace_long_axis_start_padding);
//        WORKSPACE_LONG_AXIS_END_PADDING = r.getDimensionPixelSize(R.dimen.workspace_long_axis_end_padding);
//        WORKSPACE_SHORT_AXIS_START_PADDING = r.getDimensionPixelSize(R.dimen.workspace_short_axis_start_padding);
//        WORKSPACE_SHORT_AXIS_END_PADDING = r.getDimensionPixelSize(R.dimen.workspace_short_axis_end_padding);
//        WORKSPACE_BETWEEN_SCREEN_GAP = r.getDimensionPixelSize(R.dimen.workspace_between_screen_gap);
//
//        DOCK_WINDOW_HEIGHT = r.getDisplayMetrics().heightPixels;//r.getDimensionPixelSize(R.dimen.dock_window_height);
//        DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT = r.getDimensionPixelSize(R.dimen.dock_window_y_wait_show_height);
//        DOCK_WINDOW_Y_WAIT_SHOW_OFFSET = -DOCK_WINDOW_HEIGHT + DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT;
//        DOCK_WINDOW_Y_HIDE_SHOW_OFFSET = -DOCK_WINDOW_HEIGHT - DOCK_WINDOW_Y_WAIT_SHOW_HEIGHT;
//        DOCK_WIDTH = BUBBLE_WIDTH;
//        DOCK_HEIGHT = BUBBLE_HEIGHT + r.getDimensionPixelSize(R.dimen.dock_height_add_padding);
//        DOCK_BACKGROUD_HEIGHT = r.getDimensionPixelSize(R.dimen.dock_backgroud_height);
//        IS_SHOWDOCK_INVERT = r.getInteger(R.integer.is_show_dock_invert) == 1 ? true : false;
//
//        //*************** 设置是否显示全局Dock **************
//        SharedPreferences shared_pref = 
//                PreferenceManager.getDefaultSharedPreferences(LauncherApplication.mApplicationContext);
//        boolean defValue = r.getInteger(R.integer.is_show_global_dock) == 1 ? true : false;
//        IS_SHOW_GLOBAL_DOCK = 
//                shared_pref.getBoolean(HomeShellConfigActivity.CAN_QUICK_LAUNCH, defValue);
//        //*************** 设置是否显示全局Dock **************
//        //页面间切换动画的设置
//        AnimationTime.PAGE_SWITCH_ANIMATION.value = shared_pref.getInt(
//                FlingAnimationActivity.ANIM_INDEX, 
//                FlingAnimationActivity.DEFAULT_ANIM_INDEX);
//        
//        DOCK_SHOW_COVER_HEIGHT = DOCK_WINDOW_HEIGHT - r.getDimensionPixelSize(R.dimen.dock_show_cover_height);
//        
//        MENU_BACKGROUD_HEIGHT = r.getDimensionPixelSize(R.dimen.menu_bg_height);
//        
//        mBubbleTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mBubbleTitlePaint.setColor(Color.WHITE);
//        mBubbleTitlePaint.setTextSize(r.getDimensionPixelSize(R.dimen.bubble_title_size));
//
//        reInitParamsIfNeeded(r);
//        initYunOSFlag();
//    }
//    
//    public static void releaseRes() {
//    }
//
//    private static void initYunOSFlag() {
//        boolean isYunos = false;
//        String yunosStr = "0";
//
//        yunosStr = getSystemProperties("persist.sys.yunosflag");
//        isYunos = yunosStr.equals("1");
//
//        RUN_ON_YUN_OS = isYunos;
//
//        HLog.d(TAG, "RUN_ON_YUN_OS : " + RUN_ON_YUN_OS );
//    }
//
//    public static boolean checkUsbMode() {
//        boolean isUsbMode = false;
//
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_SHARED.equals(state)) {
//            isUsbMode = true;
//        }
//        HLog.d(TAG, "isUsbMode : " + isUsbMode);
//        return isUsbMode;
//    }
//
//    public static void reInitParamsIfNeeded(Resources r) {
//        NEW_MARK_SHORTCUT = r.getString(R.string.new_mark_shortcut);
//        LONG_TITLE_SUFFIX = r.getString(R.string.long_title_suffix);
//    }
//
//    public static String getBubbleTitle(final String str) {
//        String measureStr = str;
//        final Paint paint = mBubbleTitlePaint;
//        int titleW = (int) paint.measureText(measureStr);
//        if (titleW > BUBBLE_WIDTH) {
//            int dotsWidth = (int) paint.measureText("...");
//            int pos = paint.breakText(measureStr, true, BUBBLE_WIDTH - dotsWidth, null);
//            measureStr = measureStr.substring(0, pos) + "...";
//        }
//        return measureStr;
//    }
//
//    public static void writeBitmap(ContentValues values, Bitmap icon) {
//        if (icon == null) {
//            return;
//        }
//
//        Bitmap scaledIcon = null;
//        try {
//            scaledIcon = Bitmap.createScaledBitmap(icon, ICON_WIDTH, ICON_HEIGHT, true);
//            int size = scaledIcon.getWidth() * scaledIcon.getHeight() * 4;
//            ByteArrayOutputStream out = new ByteArrayOutputStream(size);
//
//            scaledIcon.compress(Bitmap.CompressFormat.PNG, 100, out);
//            out.flush();
//            out.close();
//            values.put(LauncherSettings.Favorites.ICON, out.toByteArray());
//        } catch (IOException e) {
//            HLog.e(TAG, "Failed in writeBitmap : " + e.toString());
//        } finally {
//            if (scaledIcon != null) {
//                scaledIcon.recycle();
//            }
//        }
//    }
//
//    public static Bitmap combineBitmap(Bitmap foreground, Bitmap background,
//                                       int left, int top){
//        if (foreground == null || background == null) {
//            return null;
//        }
//
//        int w = background.getWidth();
//        int h = background.getHeight();
//        Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
//        Canvas c = new Canvas(bmp);
//
//        c.drawBitmap(background, 0, 0, null);
//        c.drawBitmap(foreground, left, top, null);
//
//        return bmp;
//    }
//
//    public static Bitmap buildRoundedBitmap(Bitmap bitmap, float radius){
//        if(bitmap == null){
//            return null;
//        }
//
//        int w = bitmap.getWidth();
//        int h = bitmap.getHeight();
//
//        Bitmap roundedBmp = Bitmap.createBitmap(w, h, Config.ARGB_4444);
//
//        Paint paint = new Paint();
//        paint.setAntiAlias(true);
//        paint.setColor(0xff424242);
//
//        Rect rect = new Rect(0, 0, w, h);
//        RectF rectF = new RectF(rect);
//
//        Canvas canvas = new Canvas(roundedBmp);
//        canvas.drawARGB(0, 0, 0, 0);
//        canvas.drawRoundRect(rectF, radius, radius, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, rect, rect, paint);
//
//        return roundedBmp;
//    }
//    
//    // widthScaledHeight: whether width and height of bitmap should scale by 1:1
//    public static Bitmap buildDesktopIcon(Bitmap source, Bitmap mask,
//                                          Bitmap alpha, Bitmap bg,
//                                          boolean widthScaledHeight) {
//        int w = source.getWidth();
//        int h = source.getHeight();
//        final int icon_width = ICON_WIDTH;
//        final int icon_height = ICON_HEIGHT;
//
//        if (widthScaledHeight) {
//            source = Bitmap.createScaledBitmap(source, icon_width, icon_height, true);
//        } else {
//            boolean reset = false;
//            if (w > h && w > icon_width) {
//                h = h * icon_width / w;
//                w = icon_width;
//                if (h < 2) {
//                    h = 2;
//                }
//                reset = true;
//            } else if (h > icon_height) {
//                w = w * icon_height / h;
//                h = icon_height;
//                if (w < 2) {
//                    w = 2;
//                }
//                reset = true;
//            }
//            
//            if (reset) {
//                source = Bitmap.createScaledBitmap(source, w, h, true);
//                h = source.getHeight();
//                w = source.getWidth();
//            }
//        }
//
//        Bitmap desktopIcon = Bitmap.createBitmap(icon_width, icon_height, Config.ARGB_8888);
//        Canvas c = new Canvas(desktopIcon);
//
//        c.drawBitmap(bg, 0, 0, null);
//
//        if (widthScaledHeight) {
//            c.drawBitmap(source, 0, 0, null);
//        } else {
//            c.drawBitmap(source, (icon_width - w) / 2.0f, (icon_height - h) / 2.0f, null);
//        }
//        
//        final int[]  shadowPixels = new int[icon_width * icon_height];
//        alpha.getPixels(shadowPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//        
//        final int[]  iconPixels = new int[icon_width * icon_height];
//        desktopIcon.getPixels(iconPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//        
//        for (int i = 0; i < icon_width; i++) {
//            for (int j = 0; j < icon_height; j++) {
//                final int idx = i * icon_width + j;
//                iconPixels[idx] = (iconPixels[idx] & 0xffffff) |
//                     (shadowPixels[idx] & 0xff000000);
//            }
//        }
//        desktopIcon.setPixels(iconPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//        
//        if (mask != null) {
//            c.drawBitmap(mask, 0, 0, null);
//        }
//
//        return desktopIcon;
//    }
//    
//    public static Bitmap buildDesktopIcon(Drawable source, Bitmap mask,
//                                          Bitmap alpha, Bitmap bg) {
//        // scale source bitmap if needed.
//        int w = source.getIntrinsicWidth();
//        int h = source.getIntrinsicHeight();
//        boolean bchange = false;
//        Bitmap sourceBmp = null;
//        final int icon_width = ICON_WIDTH;
//        final int icon_height = ICON_HEIGHT;
//
//        source.setBounds(new Rect(0, 0, w, h));
//        sourceBmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
//        
//        Canvas sc = new Canvas(sourceBmp);
//        source.draw(sc);
//        
//        if (w > h && w > icon_width) {
//            bchange = true;
//            h = h * icon_width / w;
//            w = icon_width;
//            if (h < 2)
//                h = 2;
//        } else if (h > icon_height) {
//            bchange = true;
//            w = w * icon_height / h;
//            h = icon_height;
//            if (w < 2)
//                w = 2;
//        }
//        
//        if (bchange) {
//            sourceBmp = Bitmap.createScaledBitmap(sourceBmp, w, h, true);
//            h = sourceBmp.getHeight();
//            w = sourceBmp.getWidth();
//        }
//
//        Bitmap desktopIcon = Bitmap.createBitmap(icon_width, icon_height, Config.ARGB_8888);
//        Canvas c = new Canvas(desktopIcon);
//        if(bg != null)
//            c.drawBitmap(bg, 0, 0, null);
//
//        // draw the icon content
//        c.drawBitmap(sourceBmp, (icon_width - w) / 2.0f, (icon_height - h) / 2.0f, null);
//        sourceBmp.recycle();
//        sourceBmp = null;
//
//        if(alpha != null){
//            // cut the bitmap to shape
//            final int[]  shadowPixels = new int[icon_width * icon_height];
//            alpha.getPixels(shadowPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//            
//            final int[]  iconPixels = new int[icon_width * icon_height];
//            desktopIcon.getPixels(iconPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//            
//            for (int i = 0; i < icon_width; i++) {
//                for (int j = 0; j < icon_height; j++) {
//                    final int idx = i * icon_width + j;
//                    iconPixels[idx] = (iconPixels[idx] & 0xffffff) |
//                         (shadowPixels[idx] & 0xff000000);
//                }
//            }
//            desktopIcon.setPixels(iconPixels, 0, icon_width, 0, 0, icon_width, icon_height);
//        }
//        if (mask != null) {
//            c.drawBitmap(mask, 0, 0, null);
//        }
//        return desktopIcon;
//    }
//

    /* YUNOS BEGIN */
    // ##date:2015/2/15 ##author:zhanggong.zg ##BugID:5742538
    // fix bug about downloading icon might block worker thread
    private static final int DOWNLOAD_TIMEOUT_MILLIS = 5000;
    private static final int MAX_SIMULTANEOUS_DOWNLOADS = 3;
    private static ExecutorService sBitmapDownloadExecutor = null;

    public static interface DownloadTaskHandler {
        /**
         * Pass in the result bitmap or null if failed.
         */
        void onDownloadFinished(Bitmap bitmap);
    }

    public static void asyncGetBitmapFromRemoteUri(final String iconAddr, final DownloadTaskHandler handler) {
        Log.d(TAG, "asyncGetBitmapFromRemoteUri: begin");
        if (sBitmapDownloadExecutor == null) {
            // create a thread pool
            sBitmapDownloadExecutor = Executors.newFixedThreadPool(MAX_SIMULTANEOUS_DOWNLOADS);
        }
        sBitmapDownloadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                try {
                    bitmap = getBitmapFromRemoteUri(iconAddr);
                } finally {
                    Log.d(TAG, "asyncGetBitmapFromRemoteUri: download finish");
                    handler.onDownloadFinished(bitmap);
                }
            }
        });
        Log.d(TAG, "asyncGetBitmapFromRemoteUri: end");
    }

    public static Bitmap getBitmapFromRemoteUri(String iconAddr) {
        Bitmap icon = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(iconAddr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(DOWNLOAD_TIMEOUT_MILLIS);
            conn.setReadTimeout(DOWNLOAD_TIMEOUT_MILLIS);
            if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStream inputStream = conn.getInputStream();
                icon = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed in getBitmapFromRemoteUri", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return icon;
    }
    /* YUNOS END */

    /**-
     * the isSdcardApp parameter is false for default
     * @param intent
     * @param context
     * @param info, the parameter passed in to start an activity
     * @return
     */
    public static boolean startActivitySafely(Intent intent, Context context, ShortcutInfo info) {
        int id = -1;
        String reason = null;
        int origFlag = intent.getFlags();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            intent.setFlags(origFlag);
            return true;
        } catch (Exception e) {
        	//edit by dongjun
//            reason = e.toString();
//            if (info != null) {
//                if (info.isDownloading()) {
//                    id = ToastManager.APP_IN_UPDATING;
//                } else if (info.isSdcardApp() && Utils.isInUsbMode()) {
//                    id = ToastManager.APP_UNAVAILABLE_IN_USB;
//                } else {
//                    id = ToastManager.APP_NOT_FOUND;
//                }
//            } else {
//                id = ToastManager.APP_NOT_FOUND; 
//            }
        }
        intent.setFlags(origFlag);

        HLog.e(TAG, "Failed startActivitySafely info : " + info +
                   " intent : " + intent + " reason : " + reason);
        ToastManager.makeToast(id); 
        return false;
    }
//
//    public static String getPackageNameByIntent(Context context, String intent) {
//        String []s = intent.split(";");
//        ComponentName cn = new ComponentName(context, s[3]);
//        String packageName = cn.getPackageName();
//        HLog.d(TAG, "getPackagename, pkgName:" + packageName + ", s[3]:" + s[3]);
//
//        return packageName;
//    }
//
//    public static void assertTrue(){
//
//    }
//
    public static Bitmap scaleBitmap(Bitmap src, float scaleW, float scaleH) {
        if (src.getWidth() <= 0 || src.getHeight() <= 0) {
            return null;
        }
        
        Matrix matrix = new Matrix();
        matrix.postScale(scaleW, scaleH);

        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                    src.getHeight(), matrix, true);
        } catch (Exception e) {
            HLog.e(TAG, "Failed in scaleBitmap : " + e.toString());
        }
        if (bitmap == null) {
            bitmap = src;
        }
        return bitmap;
    }
//
//    public static int getWindowDisplayTop(Activity a) {
//        Rect r = new Rect();
//        a.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
//        HLog.d(TAG, "getWindowDisplayTop : " + r.top);
//
//        return r.top;
//    }
//
//    public static final String getUnifiedNameByDeviceName() {
//        if (DEVICE != null) {
//            return DEVICE;
//        }
//
//        DEVICE = android.os.Build.MODEL;
//        if (DEVICE == null) {
//            DEVICE = "";
//        } else {
//            DEVICE = DEVICE.toLowerCase();
//        }
//        Pattern p = Pattern.compile("[^a-z0-9_]");
//        Matcher m = p.matcher(DEVICE);
//        DEVICE = m.replaceAll("_");
//
//        HLog.d(TAG, "getUnifiedNameByDeviceName DEVICE : " + DEVICE);
//        return DEVICE;
//    }
//
//    public static final int getResIdByName(Resources res, String prefix, String name, String defType, String defPackage) {
//        return res.getIdentifier(prefix + name, defType, defPackage);
//    }
//
//    public static final int getResIdByUnifiedName(Resources res, String prefix, String defType, String defPackage) {
//        int id = getResIdByName(res, prefix, getUnifiedNameByDeviceName(), defType, defPackage);
//        if (id == 0) {
//            id = getResIdByName(res, prefix, UNIFY_NAME_DEFAULT, defType, defPackage);
//        }
//        return id;
//    }
//
//    public static Bitmap getBitmap(Drawable loadIcon) {
//        if(loadIcon == null)
//            return null;
//        if(loadIcon instanceof BitmapDrawable)
//            return ((BitmapDrawable) loadIcon).getBitmap();
//        
//        return buildDesktopIcon(loadIcon, null, null, null);
//    }
//    
//    public static int getAppType(int flags) {
//        int type = AppType.UNKNOW_APP;
//
//        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
//            type = AppType.SYSTEM_APP;
//        } else if ((flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) {
//            type = AppType.USER_APP_SDCARD;
//        } else if ((flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
//            type = AppType.SYSTEM_UPDATE_APP;
//        } else {
//            type = AppType.USER_APP_DATA;
//        }
//
//        return type;
//    }
//
//    public static boolean isSdcardAvailable() {
//        boolean isAvailable = false;
//
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            // in case sdcard is formated and current sd card is available
//            isAvailable = true;
//        } else if (Environment.MEDIA_SHARED.equals(state)) {
//            // in usb mode
//            isAvailable = false;
//        }
//
//        HLog.d(TAG, "isSdcardAvailable : " + isAvailable + " state : " + state);
//        return isAvailable;
//    }
//
//    public static boolean isAppAvailable(String pkgName) {
//        PackageManager pm = LauncherApplication.mApplicationContext.getPackageManager();
//
//        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
//        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        mainIntent.setPackage(pkgName);
//
//        boolean isAvailabe = true;
//        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
//        if (apps == null || apps.size() == 0) {
//            isAvailabe = false;
//        }
//
//        HLog.d(TAG,  pkgName + " isAvailable : " + isAvailabe);
//        return isAvailabe;
//    }
//

    public static boolean isInUsbMode() {
        String state = Environment.getExternalStorageState();
        HLog.d(TAG, "sdcard state : " + state);
        if (Environment.MEDIA_SHARED.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean isForCMCC() {
        String YUNOS_CARRIER = "ro.yunos.carrier.custom";
        String CHINA_MOBILE = "CMCC";
        String spec = ACA.SystemProperties.get(YUNOS_CARRIER);

        if (CHINA_MOBILE.equals(spec)) {
            return true;
        }
        return false;
    }

    /*YUNOS BEGIN*/
    //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
    // fato sd app icon error
    public static boolean isSdcardApp(int flags) {
        return ((flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0);
    }
    /*YUNOS END*/
//
//    public static void setLongClickListener(final View v, final OnLongClickListener longClickListener) {
//        v.setOnTouchListener(new OnTouchListener() {
//            GestureDetector gestureDetector = new GestureDetector(
//                    v.getContext(), new GestureDetector.OnGestureListener() {
//                        public boolean onSingleTapUp(MotionEvent e) {
//                            return false;
//                        }
//                        public void onShowPress(MotionEvent e) {
//                        }
//                        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
//                                float distanceY) {
//                            return false;
//                        }
//                        public void onLongPress(MotionEvent e) {
//                            longClickListener.onLongClick(v);
//                        }
//                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//                                float velocityY) {
//                            return false;
//                        }
//                        public boolean onDown(MotionEvent e) {
//                            return false;
//                        }
//                    });
//            public boolean onTouch(View v, MotionEvent event) {
//                if(gestureDetector.onTouchEvent(event))
//                    return true;
//                return v.onTouchEvent(event);
//            }
//        });
//    }
//    
//    public static boolean isNeedMenu(Context context){
//        boolean flag = true;
//        if(context == null)
//            return flag;
//        
//        String noNeedMenuDevices[] = context.getResources().getStringArray(R.array.not_need_menu);
//        String deviceName = getUnifiedNameByDeviceName();
//        if (noNeedMenuDevices != null && !TextUtils.isEmpty(deviceName)) {
//            for (String name : noNeedMenuDevices) {
//                if(deviceName.equals(name)){
//                    flag = false;
//                    break;
//                }
//            }
//        }
//        
//        return flag;
//    }
//    
//    public static String getCurProcessName(Context context) {
//        int pid = android.os.Process.myPid();
//        ActivityManager mActivityManager = (ActivityManager) context
//                .getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager
//                .getRunningAppProcesses()) {
//            if (appProcess.pid == pid) {
//
//                return appProcess.processName;
//            }
//        }
//        return null;
//    }
//    
//    public static boolean isYunOSSystem() {
//        String yunosStr = "0";
//
//        yunosStr = getSystemProperties("persist.sys.yunosflag");
//        if (System.getProperty("java.vm.name").toLowerCase().contains("lemur")
//                || (null != System.getProperty("ro.yunos.version")) || yunosStr.equals("1")) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//    
//    private static String getSystemProperties(String key) {
//        String value = null;
//        Class<?> cls = null;
//        ;
//        try {
//            cls = Class.forName("android.os.SystemProperties");
//            Method hideMethod = cls.getMethod("get", String.class);
//            Object object = cls.newInstance();
//            value = (String) hideMethod.invoke(object, key);
//        } catch (SecurityException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        return value;
//    }
//    public static int getStatusBarHeight(Activity a) {
//        if (STATUSBAR_HEIGHT == -1) {
//            Rect frame = new Rect();  
//            a.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);  
//            STATUSBAR_HEIGHT = frame.top;
//        }
//
//        return STATUSBAR_HEIGHT;
//    }
//    
//    public static void addStartActivityClearTask(Intent intent) {
//        int version = android.os.Build.VERSION.SDK_INT;
//
//        if (version >= 11) {
//            // beginnig android 3.0,add Intent.FLAG_ACTIVITY_CLEAR_TASK
//            final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
//            intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
//        }
//    }

    /* YUNOS BEGIN */
    //##date:2014/03/24 ##author:nater.wg ##BugID:107102
    // Check the app's package name is in the restore app list
    public static boolean isRestoreApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        String allAppListString = BackupUitil.getBackupAppListString("");
        if (TextUtils.isEmpty(allAppListString)) {
            return false;
        }
        if (allAppListString.indexOf(packageName) > 0) {
            return true;
        }
        return false;
    }
    /* YUNOS END */


    /* YUNOS BEGIN */
    // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
    static Dialog mProcessDialog = null;

    public static void showLoadingDialog(Context context, int resId) {
        if (context == null) {
            return;
        }
        String msg = context.getResources().getString(resId);
        showLoadingDialog(context,msg);
    }

    public static void showLoadingDialog(Context context) {
        if (context == null) {
            return;
        }
        String msg = context.getResources().getString(R.string.str_load);
        showLoadingDialog(context,msg);
    }

    public static void showLoadingDialog(Context context,String msg) {
        if (context == null) {
            return;
        }
        if (mProcessDialog == null) {
            RelativeLayout progressdialog = (RelativeLayout) LayoutInflater.from(context).inflate(
                R.layout.dialog_progress_view, null, false);
            mProcessDialog = new Dialog(context, R.style.ProgressBarDialog);
            TextView mProgressMessage = (TextView) progressdialog
                    .findViewById(R.id.dialog_progress_message_text);
            mProgressMessage.setText(context.getResources().getString(R.string.str_load));
            mProcessDialog.setContentView(progressdialog);
            mProcessDialog.setCancelable(false);
        }
        mProcessDialog.show();
    }

    public static Dialog getLoadingDialog(Context context) {
        return mProcessDialog;
    }
    public static void dismissLoadingDialog() {
        if (mProcessDialog != null && mProcessDialog.isShowing()) {
            mProcessDialog.dismiss();
            mProcessDialog = null;
        }
    }

    public static boolean isLoadingDialogShowing() {
        return mProcessDialog != null && mProcessDialog.isShowing();
    }
    /* YUNOS END */

    static public boolean isSupportAppStoreQuickControl(Context context){
        String pkgOfAppStore = "com.aliyun.wireless.vos.appstore";
        int supportVersionCode = 20140808;
        try{
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(pkgOfAppStore,0);
            Log.d(TAG,"appstore version code: "+info.versionCode);
            return info.versionCode >= supportVersionCode;
        }catch(NameNotFoundException e){
            Log.d(TAG,pkgOfAppStore+" doesn't exist");
        }
        return false;
    }

    public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {
        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, sentBitmap,
                Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs,
                Element.U8_4(rs));
        script.setRadius(radius /* e.g. 3.f */);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);
        return bitmap;
    }

    public static int getStatusBarSize(Resources res) {
        int result = 0;
        if (res == null) {
            return result;
        }
        int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean isNetworkConnected() {
        try {
            Context context = LauncherApplication.getContext();
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo info = connectivity
                        .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (info != null && info.isConnected()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.v("error", e.toString());
        }
        return false;
    }

    public static boolean isSupportSearch(Context mContext) {
        boolean support = false;
        if (FeatureUtility.hasPullDownSearchFeature()) {
            if (isPackageExist(mContext, mSearchWidgetPackage) ||
                isPackageExist(mContext, mSearchNowCardWidgetPackage)) {
                support = isIntentAvailable(mContext,mSearchIntent);
            }
        }
        return support;
    }

    public static boolean isPackageExist(Context mContext, String packageName) {
        boolean exist = true;
            try {
                mContext.getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                exist = false;
            }
        return exist;
    }

    public static boolean isIntentAvailable(Context mContext, Intent intent) {
        final PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
    }
    private static final String UTF_SPACE = new String(new byte[] { (byte) 0xc2, (byte) 0xa0 }, Charset.forName("UTF-8"));
    public static CharSequence trimUTFSpace(CharSequence src) {
        if(src.toString().startsWith(UTF_SPACE)) {
            src = src.toString().replace(UTF_SPACE, "");
        }
        return src;
    }

    public static void notifySearchPageChanged(final Context context,final boolean hasSearchWidget, final boolean hasSearchNowCardWidget, final int screen){
        mHandler.postIdle(new Runnable(){
            @Override
            public void run() {
                if (SearchBridge.isHomeShellSupportGlobalSearchUI(context)){
                    context.sendBroadcast(new Intent("com.yunos.alisearch.intent.TOGGLE_HOTWORD_MSG"));
                }else{
                    mSearchPageChangedIntent.putExtra("hasSearchWidget", hasSearchWidget);
                    mSearchPageChangedIntent.putExtra("hasSearchNowCardWidget", hasSearchNowCardWidget);
                    mSearchPageChangedIntent.putExtra("screen", screen);
                    context.sendBroadcast(mSearchPageChangedIntent);
                }
            }});
    }

    public static boolean isForLightUpdate(){
        return false;
    }

    public static boolean isCMCC() {
        return "CMCC".equals(ACA.SystemProperties.get("ro.yunos.carrier.custom","NONE"));
    }

    public static boolean isSupportTheme(Context context){
        boolean support = false;
        try {
            support = isIntentAvailable(context,mThemeIntent) || isIntentAvailable(context,mOldThemeIntent);
        } catch (Exception e) {
            support = false;
        }
        if (support) {
            support = support && !ConfigManager.isLandOrienSupport();
        }
        return support;
    }

    public static boolean isSupportWallPaper(Context context){
        boolean support = false;
        try {
            support = isIntentAvailable(context,mWallpaperIntent);
        } catch (Exception e) {
            support = false;
        }
        return support;
    }

    public static boolean isLowSpace(){
        boolean low = false;
        File file = new File(Environment.getDataDirectory().getPath());
        float fremem = file.getUsableSpace();
        if(fremem < 5242880){ // 5 * 1024 * 1024
            low = true;
        }
        return low;
    }
}
