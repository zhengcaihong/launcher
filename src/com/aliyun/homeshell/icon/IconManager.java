/*
 * Created by Dong Jun (jun.dongj@alibaba-inc.com)
 * Date : 2014-1-13
 * The class provide the manage of all Icons. Including creating,building,getting icons(app icon, folder icon).
 */

package com.aliyun.homeshell.icon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONException;
import org.json.JSONObject;

import app.aliyun.aml.FancyDrawable;
import app.aliyun.v3.res.FancyIconsHelper;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
//import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.aliyun.homeshell.AppDownloadManager;
import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.ApplicationInfo;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.FastBitmapDrawable;
//import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.iconupdate.IconUpdateManager;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.homeshell.utils.Utils;
//import com.aliyun.homeshell.vpinstall.VPUtils;
import com.aliyun.utility.FeatureUtility;

public class IconManager {
    private final String TAG = "IconManager";
    private static final int ALPHA_MIN = 230;
    private static final int ALPHA_ATOP = 255;
    private static final float DARKEN_LEVEL = 1.15f;
    private static final int DOWNLOAD_MASK_COLOR = 0xff000000;
    private static final int DEFAULT_CARD_BACKGROUD = 0xffffffff;
    public static final int ICON_TYPE_APP = ThemeUtils.ICON_TYPE_APP;
    public static final int ICON_TYPE_CLOUDAPP = ThemeUtils.ICON_TYPE_CLOUDAPP;
    public static final int ICON_TYPE_BROSWER_SHORTCUT = ThemeUtils.ICON_TYPE_BROSWER_SHORTCUT;
    public static final int ICON_TYPE_FOLDER = ThemeUtils.ICON_TYPE_FOLDER;
    public static final int ICON_TYPE_APP_TEMPORARY = ThemeUtils.ICON_TYPE_APP_TEMPORARY;
    public static final String DOWNLOAD_CLASS = "donwload";
    private static final String CONTACT_INTENT_TYPE = "vnd.android.cursor.item/contact";
    private static final String HDPI_PATH = "drawable-hdpi/";
    private static final String XHDPI_PATH = "drawable-xhdpi/";
    private static final String XXHDPI_PATH = "drawable-xxhdpi/";
    private static final String XXXHDPI_PATH = "drawable-xxxhdpi/";

    private Context mContext = null;
    private ConcurrentHashMap<Key, Drawable> mIconCache = new ConcurrentHashMap<Key, Drawable>();
    private HashMap<Key, Integer> mCardBgColorCache = new HashMap<Key, Integer>();
    private ConcurrentHashMap<Key, Drawable> mCardBgIconCache = new ConcurrentHashMap<Key, Drawable>();
    private List<Integer> mCardBgColorPool = new ArrayList<Integer>();
    private boolean mSupportCard = false;
    private Drawable mCardFolderBg = null;
    private Drawable mCardFolderCover = null;
    private Drawable mDefaultIcon = null;
    private int mDefaultIconResouce = 0;
    private Drawable mCardMask = null;
    private Drawable mCardBorder = null;
    private PackageManager mPackageManager = null;
    private int mIconDpi;
    private boolean mNeedDockCardIcon = false;
    private boolean mNeedCardFolderCircular = true;
    private Drawable mIconBorder = null;

    private final String CONFIG_FILE_FULLPATH = "/data/system/auitheme/com.aliyun.homeshell";
    private final String UNZIP_CONFIG_FILE_SUBPATH = "assets/";
    private final String UNZIP_CONFIG_FILE_NAME = "configure";
    private final String UNZIP_CONFIG_RES_SUBPATH = "res/";
    private final String UNZIP_CONFIG_RES_CARDMASK = "card_bg.png";
    private final String UNZIP_CONFIG_RES_CARDBORDER = "card_boder.png";
    private final String UNZIP_CONFIG_RES_ICONBORDER = "aui_ic_mask_app_unified.png";

    public IconManager(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();
        setup();
    }

    public void clearFancyIconCache() {
        for (Map.Entry<Key, Drawable> entry : mIconCache.entrySet()) {
            if (entry.getValue() instanceof FancyDrawable)
                mIconCache.remove(entry.getKey());
        }
        for (Map.Entry<Key, Drawable> entry : mCardBgIconCache.entrySet()) {
            if (entry.getValue() instanceof FancyDrawable)
                mCardBgIconCache.remove(entry.getKey());
        }
    }

    private void setup() {
        setupConfigure();
        setupResource();
    }

    private void setupResource() {
        Log.d(TAG, "setupResource : begin");
        Resources res = mContext.getResources();
        if(mSupportCard){
            if(!mNeedCardFolderCircular){
                mCardFolderBg = res.getDrawable(R.drawable.card_folder_bg);
                mCardFolderCover = res.getDrawable(R.drawable.card_folder_cover);
            }else{
                mCardFolderBg = res.getDrawable(R.drawable.card_folder_bg_circular);
                mCardFolderCover = res.getDrawable(R.drawable.card_folder_cover_circular);
            }
        }
        mDefaultIconResouce = android.R.mipmap.sym_def_app_icon;
    }

    private void setupConfigure() {
        Log.d(TAG, "setupConfigure : begin");
        ZipFile configureFile = getConfigureFile();
        String configure = loadConfigure(configureFile);
        parseConfigure(configure);
        LoadConfigureRes(configureFile);
        closeConfigureFile(configureFile);
    }

    private void LoadConfigureRes(ZipFile configFile){
        if (configFile != null) {
            InputStream input = null;
            if(mSupportCard){
                try {
                    //load card mask
                    String fullpath = getConfigureResPath() + UNZIP_CONFIG_RES_CARDMASK;
                    ZipEntry entry = configFile.getEntry(fullpath);
                    if (entry != null) {
                        input = configFile.getInputStream(entry);
                        mCardMask = Drawable.createFromStream(input, null);
                    }
                } catch (IOException e) {
                    Log.w(TAG,"load configure card mask error : "+e);
                }finally{
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            Log.w(TAG,"close card mask stream error :"+e);
                        }
                    }
                }

                if(mCardMask == null){
                    mCardMask = LauncherApplication.getContext().getResources().getDrawable(R.drawable.card_bg);
                }

                //load card border
                try {
                    String fullpath = getConfigureResPath() + UNZIP_CONFIG_RES_CARDBORDER;
                    ZipEntry entry = configFile.getEntry(fullpath);
                    if (entry != null) {
                        input = configFile.getInputStream(entry);
                        mCardBorder = Drawable.createFromStream(input, null);
                    }
                } catch (IOException e) {
                    Log.w(TAG,"load configure card border error : "+e);
                }finally{
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            Log.w(TAG,"close card border stream error :"+e);
                        }
                    }
                }

                try {
                    String fullpath = getConfigureResPath() + UNZIP_CONFIG_RES_ICONBORDER;
                    ZipEntry entry = configFile.getEntry(fullpath);
                    if (entry != null) {
                        input = configFile.getInputStream(entry);
                        mIconBorder = Drawable.createFromStream(input, null);
                    }
                } catch (IOException e) {
                    Log.w(TAG,"load configure card border error : "+e);
                }finally{
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            Log.w(TAG,"close card border stream error :"+e);
                        }
                    }
                }
            }
        }
    }

    private String getConfigureResPath(){
        String path = null;
        final float denisty = LauncherApplication.getScreenDensity();
            if(denisty == 4){
                path = XXXHDPI_PATH;
            }else if(denisty == 3){
                path = XXHDPI_PATH;
            }else if(denisty == 2){
                path = XHDPI_PATH;
            }else if(denisty == 1.5){
                //there is no hdpi res for card theme, so use xhdpi as default
                path = XHDPI_PATH;
            }else{
                //HDPI res as default
                path = XHDPI_PATH;
            }
        return UNZIP_CONFIG_RES_SUBPATH + path;
    }

    public Drawable getCardMask(){
        if(mCardMask == null){
            return null;
        }
        if(mCardMask.getAlpha() < 255){
            mCardMask.setAlpha(255);
        }
        mCardMask.clearColorFilter();
        return mCardMask;
    }

    public Drawable getCardBorder(){
        return mCardBorder;
    }

    public Drawable getCardFolderBg(){
        return mCardFolderBg;
    }

    public Drawable getCardFolderCover(){
        return mCardFolderCover;
    }

    private Drawable getIconBorder(){
        return mIconBorder;
    }

    private String loadConfigure(ZipFile configFile) {
        Log.d(TAG, "loadConfigure : begin");
        String configure = null;
        if (configFile != null) {
            InputStream input = null;
            try {
                ZipEntry entry = configFile.getEntry(UNZIP_CONFIG_FILE_SUBPATH
                        + UNZIP_CONFIG_FILE_NAME);
                if (entry != null) {
                    input = configFile.getInputStream(entry);
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(input, "utf-8"));
                    StringBuffer out = new StringBuffer();
                    String data = "";
                    while ((data = br.readLine()) != null) {
                        out.append(data + "\n");
                    }
                    configure = out.toString();
                }
            } catch (IOException e) {
                Log.w(TAG,"load configure content error : ",e);
            }finally{
                try{
                    if (input != null) {
                        input.close();
                    }
                }catch(IOException e){
                    Log.w(TAG,"close configure stream error : ",e);
                }
            }
        }
        return configure;
    }

    private ZipFile getConfigureFile() {
        try {
            ZipFile zipfile = new ZipFile(CONFIG_FILE_FULLPATH);
            return zipfile;
        } catch (IOException e) {
            return null;
        }
    }

    private void closeConfigureFile(ZipFile configFile){
        if(configFile != null){
            try{
                configFile.close();
            }catch(IOException e){
                Log.w(TAG, "close confige file error : ",e);
            }
        }
    }

    private void parseConfigure(String configure) {
        Log.d(TAG, "parseConfigure : begin");
        if (configure == null) {
            Log.w(TAG, "parseConfigure , configure is null");
            return;
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(configure);
        } catch (JSONException e) {
            Log.w(TAG, "parseConfigure error : " + e.toString());
            return;
        }

        if (ConfigManager.isLandOrienSupport()) {
            //Land support device doesn't support card icon
            mSupportCard = false;
        } else {
            try {
                jsonObject = new JSONObject(configure);
                if (FeatureUtility.hasBigIconFeature()){
                    mSupportCard = jsonObject.getBoolean("support_card");
                }
            } catch (JSONException e) {
                Log.w(TAG, "parseConfigure error : " + e.toString());
            }
        }

        try {
            if(mSupportCard){
                mNeedDockCardIcon = jsonObject.getBoolean("card_dockicon");
            }
        } catch (JSONException e) {
            Log.w(TAG, "parseConfigure error : " + e.toString());
        }

        try {
            if(mSupportCard){
                mNeedCardFolderCircular = jsonObject.getBoolean("card_folder_cycle");
            }
        } catch (JSONException e) {
            Log.w(TAG, "parseConfigure error : " + e.toString());
        }
    }

    public void notifyThemeChanged() {
        Log.d(TAG, "notifyThemeChanged : begin");
        reset();
        setup();
    }

    private void reset() {
        Log.d(TAG, "reset : begin");
        ThemeUtils.destroyIconCache();
        mIconCache.clear();
        mCardBgColorCache.clear();
        mCardBgIconCache.clear();
        mCardBgColorPool.clear();
        mDefaultIcon = null;
        mSupportCard = false;
        mCardMask = null;
        mCardBorder = null;
        mIconBorder = null;
        mNeedDockCardIcon = false;
        mNeedCardFolderCircular = true;
        mCardFolderBg = null;
        mCardFolderCover = null;
    }

    public void destroy() {
        Log.d(TAG, "destroy : begin");
        ThemeUtils.destroyIconCache();
        mIconCache.clear();
        mCardBgColorCache.clear();
        mCardBgIconCache.clear();
        mCardBgColorPool.clear();
        mDefaultIcon = null;
    }

    public boolean supprtCardIcon() {
        return mSupportCard;
    }

    public Drawable getAppUnifiedIcon(ItemInfo info, IconCursorInfo cursorinfo) {
        Log.d(TAG, "getAppUnifiedIcon : begin");
        if (info == null) {
            return null;
        }
        Drawable icon = null;
        boolean isdownload = isDownloadItem(info);


        //BugID:117502:download item's icon doesn't change when theme change
        // handle shortcut and download as vp install item
        // BugID:92481
// remove vp install
//        if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) ||
          if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) ||
                (isdownload == true)) {
            if (cursorinfo == null) {
                Cursor c = null;
                ContentResolver contentResolver = mContext.getContentResolver();
                c = contentResolver.query(
                        LauncherSettings.Favorites.CONTENT_URI, null, "_id=?", new String[] {
                            String.valueOf(info.id)
                        }, null);
                if ((c != null) && (c.moveToFirst())) {
                    cursorinfo = new IconCursorInfo(c, (int) info.id);
                }
                // Be carefull!!!
                // At present icon manager doesn't change icon for theme
                // so I build icon base theme
                Drawable originalicon = getIconFromCursor(cursorinfo, false);
                if (originalicon != null) {
                    icon = buildUnifiedIcon(originalicon);
                } else if (info instanceof ShortcutInfo) {
                    Drawable drawable = ((ShortcutInfo)info).mIcon;
                    if (drawable != null) {
                        icon = buildUnifiedIcon(drawable);
                    } else {
                        icon = getDefaultIcon();
                    }
                }
                /* YUNOS BEGIN */
                // ##date:2014/03/13 ##author:hao.liuhaolh ##BugID:100164
                // vp install icon is null after restore
                else {
                    icon = getDefaultIcon();
                }
                /* YUNOS END */
                if (c != null) {
                    c.close();
                }
            } else {
                Drawable origicon = getShortcutIcon((ShortcutInfo) info, cursorinfo);
                if (origicon != null) {
                    icon = buildUnifiedIcon(origicon);
                }
            }
        }
        else {
            Intent intent = getIntentFromInfo(info);
            icon = getAppUnifiedIcon(intent, cursorinfo);
        }

        Log.d(TAG, "getAppUnifiedIcon : end");
        return icon;
    }

    private boolean isDownloadItem(ItemInfo info) {
        boolean ret = false;
        if (info instanceof ShortcutInfo) {
            int downloadstatus = ((ShortcutInfo) info).getAppDownloadStatus();
            int icontype = info.itemType;
            if (icontype == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING
                    || (downloadstatus != AppDownloadStatus.STATUS_NO_DOWNLOAD && downloadstatus != AppDownloadStatus.STATUS_INSTALLED && ((ShortcutInfo) info).mDownloadType == AppDownloadManager.DOWNLOAD_TYPE_DOWNLOAD)) {
                ret = true;
            }
        }
        return ret;
    }

    private Drawable getShortcutIcon(ShortcutInfo info,
            IconCursorInfo cursorinfo) {
        Drawable icon = null;
        icon = info.mIcon;
        if (icon == null) {
            icon = getIconFromCursor(cursorinfo, true);
        }
        if (icon == null) {
            icon = getDefaultIcon();
        }
        return icon;
    }

    private Drawable getAppIconFromIconUpdateMgr(ComponentName cmp) {
        IconUpdateManager iUMgr = IconUpdateManager.getInstance();
        Bitmap bmp = iUMgr.getIconByComponent(cmp);
        if (bmp != null) {
            @SuppressWarnings("deprecation")
		    Drawable drawable =new BitmapDrawable(bmp);
            return drawable;
        }
        return null;
    }

    public Drawable getAppUnifiedIcon(Intent intent,
            IconCursorInfo cursorinfo) {
        if (intent == null) {
            return null;
        }

        ComponentName component = intent.getComponent();
        Drawable icon = null;
        boolean onlyUseDB = false;

        if (cursorinfo != null) {
            onlyUseDB = cursorinfo.mOnlyUseDB;
        }
        if (!onlyUseDB) {
            icon = getAppIconFromCache(intent);
            if (icon == null) {
                icon = getFancyIcon(component);
                if (icon == null) {
                    Drawable oicon = getAppIconFromIconUpdateMgr(component);
                    if (oicon != null) {
                        icon = buildUnifiedIcon(oicon, ICON_TYPE_BROSWER_SHORTCUT);
                    } else {
                        icon = getAppIconFromTheme(component);
	                    if (icon == null) {
	                        icon = getAppIconFromPackage(component);
	                        if (icon == null) {
	                            Drawable origicon = getIconFromCursor(cursorinfo);
	                            /* YUNOS BEGIN */
	                            // ##date:2014/9/2 ##author:zhanggong.zg
	                            // ##BugID:5244146
	                            // If the user manually erased homeshell database,
	                            // the icons of frozen
	                            // apps cannot be retrieved from ThemeUtils any
	                            // more. In this situation,
	                            // we use the original icons instead.
	                            if (origicon == null) {
	                                origicon = IconUtils.getAppOriginalIcon(mContext,
	                                        component.getPackageName());
	                            }
	                            /* YUNOS END */
	                            if (origicon != null) {
	                                icon = buildUnifiedIcon(origicon, ICON_TYPE_BROSWER_SHORTCUT);
	                            }
	                        }
	                    }
                    }
                }
                if (icon != null) {
                    addAppIconToCache(intent, icon);
                }
            }
        } else {
            Drawable origicon = getIconFromCursor(cursorinfo);
            if (origicon != null) {
                icon = buildUnifiedIcon(origicon, ICON_TYPE_BROSWER_SHORTCUT);
            }
            if (icon != null) {
                addAppIconToCache(intent, icon);
            }
        }

        if (icon == null) {
            icon = getDefaultIcon();
        }
        return icon;
    }

    public Drawable getAppUnifiedIcon(Intent intent) {
        return getAppUnifiedIcon(intent, null);
    }

//    // the function is no used now !!!
//    public Bitmap getAppOriginalIcon(ComponentName component) {
//        if (component == null) {
//            Log.w(TAG, "getAppOriginalIcon error : component is null");
//            return null;
//        }
//        Bitmap icon = null;
//        String pkgname = component.getPackageName();
//        try { 
//            icon = ((FastBitmapDrawable) mPackageManager
//                    .getApplicationIcon(pkgname)).getBitmap();
//        } catch (Exception e) {
//            Log.w(TAG, "getAppOriginalIcon erro");
//        }
//        return icon;
//    }

//    /**
//     *  The function is no used now 2015/3/3
//     * @param info
//     * @param cursorinfo
//     * @return
//     */
//    public Drawable getDownloadIcon(ItemInfo info, IconCursorInfo cursorinfo) {
//        Intent intent = null;
//
//        if (info instanceof ShortcutInfo) {
//            intent = ((ShortcutInfo) info).intent;
//            Drawable icon = ((ShortcutInfo) info).mIcon;
//            if (icon != null && !isDefaultIcon(icon)) {
//                return icon;
//            }
//        }
//
//        String pkgname = null;
//
//        if (intent != null) {
//            pkgname = intent
//                    .getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
//        }
//
//        return getAppIconForDownload(pkgname, cursorinfo);
//    }

//    /**
//     * Please be careful when use this method. It only used in this class now 2015/3/3
//     * @param packagename
//     * @param cursorinfo
//     * @return
//     */
//    public Drawable getAppIconForDownload(String packagename,
//            IconCursorInfo cursorinfo) {
//        Drawable icon = null;
//        icon = getIconFromCursor(cursorinfo, false);
//        if (icon != null) {
//            icon = buildUnifiedIcon(icon);
//        } else {
//            icon = getDefaultIcon();
//        }
//        return icon;
//    }

//    public Drawable buildUnifiedIcon(Bitmap src) {
//        return buildUnifiedIcon(src, ICON_TYPE_BROSWER_SHORTCUT);
//    }

    public Drawable buildUnifiedIcon(Bitmap src, int style) {
        return buildUnifiedIcon(new FastBitmapDrawable(src), style);
    }

    public Drawable buildUnifiedIcon(Drawable src) {
        return buildUnifiedIcon(src, ICON_TYPE_APP_TEMPORARY/*ICON_TYPE_BROSWER_SHORTCUT*/);
    }

    public Drawable buildUnifiedIcon(Drawable src, int style) {
        if (src == null) {
            Log.w(TAG, "buildUnifiedIcon error : src is null");
            return null;
        }
        Drawable icon = null;

        Bitmap srcbm = null;
        if (src instanceof FastBitmapDrawable) {
            srcbm = ((FastBitmapDrawable) src).getBitmap();
        } else if (src instanceof BitmapDrawable) {
            srcbm = ((BitmapDrawable) src).getBitmap();
        }

        if (srcbm == null) {
            return null;
        }

        int suggestwidth = ThemeUtils.getIconSize(mContext);
        if (suggestwidth < 0) {
            suggestwidth = mContext.getResources().getDimensionPixelSize(
                    R.dimen.bubble_icon_width);
        }

        int iconwidth = srcbm.getWidth();
        if (iconwidth > 0 && iconwidth != suggestwidth) {
            float scaleW = suggestwidth / (float) iconwidth;
            srcbm = Utils.scaleBitmap(srcbm, scaleW, scaleW);
        }

        Bitmap bm = ThemeUtils.buildUnifiedIcon(mContext, srcbm, style);
        icon = new FastBitmapDrawable(bm);
        return icon;
    }

    public Drawable getDownloadCardMask() {
        final Drawable mask = getCardMask();
        if (mask != null) {
            mask.setColorFilter(DOWNLOAD_MASK_COLOR, PorterDuff.Mode.SRC_ATOP);
            mask.setAlpha(128);
        }
        return mask;
    }

    public Drawable getAppCardBackgroud(ItemInfo info) {
        if (info == null) {
            Log.w(TAG, "getAppCardBackgroud error : info is null");
            return null;
        }

        if (!supprtCardIcon()) {
            Log.d(TAG, "getAppCardBackgroud : not support card");
            return null;
        }

        Intent intent = getIntentFromInfo(info);
//        ComponentName component = intent.getComponent();

        /* YUNOS BEGIN */
        // ##module(Icon)
        // ##date:2014/26/04 ##author:jun.dongj@alibaba-inc.com##114778
        // the card icon are ugly

        // firstly, get preload card icon
        //BugID:5805402:shortcut icon same as app icon
        Drawable cardbg = null;
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            cardbg = getCardBgIcon(intent);
        }

        // thirdly, create card icon according to the icon
        if (cardbg == null) {
            cardbg = createCardBgIcon(info);
        }

        // finally, calculate color for card icon according to icon
        if (cardbg == null) {
            cardbg = getCardBgColorDrawable(info);
        }
        /* YUNOS END */

        return cardbg;

    }

    /* YUNOS BEGIN */
    // ##date:2015/3/17 ##author:zhanggong.zg ##BugID:5741665
    /**
     * Gets card-icon and title color in one call. Equivalent to call
     * {@link #getAppCardBackgroud(ItemInfo)} and {@link #getTitleColor(ShortcutInfo)}.
     * However, this method has better performance when <code>IconManager</code>
     * does not cache the corresponding icon for the specified <code>info</code>.
     * @param info
     * @return a pair object that contains the card-icon and title color
     */
    public Pair<Drawable, Integer> getAppCardBgAndTitleColor(ItemInfo info) {
        if (info == null) {
            Log.w(TAG, "getAppCardBgAndTitleColor error : info is null");
            return new Pair<Drawable, Integer>(null, null);
        }

        Drawable cardbg = getAppCardBackgroud(info);
        if (cardbg == null) {
            return new Pair<Drawable, Integer>(null, null);
        }

        int color = getTitleColor(info, cardbg);
        return new Pair<Drawable, Integer>(cardbg, color);
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##module(Icon)
    // ##date:2014/26/04 ##author:jun.dongj@alibaba-inc.com##114778
    // the card icon are ugly
    // private long totaltimecard = 0;
    private Drawable createCardBgIcon(ItemInfo info) {
        Log.d(TAG, "createCardBgIcon : begin");
        // long time = System.currentTimeMillis();
        Intent intent = getIntentFromInfo(info);
        if (intent == null) {
            return null;
        }

        Key key = createKey(intent);
        Drawable cardbgicon = null;
        cardbgicon = mCardBgIconCache.get(key);
        if (cardbgicon == null) {
            Drawable icon = getAppUnifiedIcon(info, null);
            Bitmap src = null;
            if (icon instanceof FastBitmapDrawable) {
                src = ((FastBitmapDrawable) icon).getBitmap();
            } else {
                /* YUNOS BEGIN */
                // ##date:2014/09/3 ##author:xindong.zxd ##BugId:5221125
                // icon may return null pointer from getAppUnifiedIcon()
                if (icon != null) {
                    src = ((BitmapDrawable) icon.getCurrent()).getBitmap();
                }
                /* YUNOS END */
            }
            if (src == null) {
                Log.w(TAG, "createCardBgIcon: bitmap of unified icon is null. icon=" + icon + ", intent=" + intent);
            }
            if (HomeShellGadgetsRender.isHomeShellGadgets(info)) {
                cardbgicon = HomeShellGadgetsRender.getRender().createBackgroundIcon(mContext.getResources(), info, src);
            } else {
              //BugID:6174728:null pointer exception
                try {
                    cardbgicon = IconUtils.createBackgroundIcon(mContext.getResources(), src);
                } catch (NullPointerException ex) {
                    Log.e(TAG, "create bg icon exception", ex);
                }
            }
            if (cardbgicon != null && needCache(info)) {
                mCardBgIconCache.put(key, cardbgicon);
            }
        }
        // long usedtime = System.currentTimeMillis() - time;
        // totaltimecard += usedtime;
        // Log.e("vqx376",
        // "createCardBgIcon : app = "+info.title+" --- time = "+usedtime +
        // " --- totaltime = "+ totaltimecard);
        Log.d(TAG, "createCardBgIcon : end");
        return cardbgicon;
    }

    /* YUNOS END */

    private static boolean needCache(ItemInfo info) {
        return !(info instanceof ShortcutInfo && info.itemType ==
                LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT &&
                ((ShortcutInfo)info).intent != null && Intent.ACTION_CALL
                .equals(((ShortcutInfo)info).intent.getAction())) &&
                !(info instanceof ShortcutInfo && info.itemType ==
                        LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT &&
                        ((ShortcutInfo)info).intent != null && Intent.ACTION_VIEW
                        .equals(((ShortcutInfo)info).intent.getAction()) &&
                        CONTACT_INTENT_TYPE.equals(((ShortcutInfo)info).intent.getType()));
    }

//    /* YUNOS BEGIN */
//    // ##module(Icon)
//    // ##date:2014/20/06 ##author:jun.dongj@alibaba-inc.com##131443
//    // The card icons of downloading apps are wrong
//    public ComponentName createComponentByIntent(Intent intent) {
//        ComponentName component = null;
//        if (intent == null) {
//            Log.e(TAG, "intent is null, return null");
//            return null;
//        }
//        component = intent.getComponent();
//        if (component == null) {
//            String pkgname = intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
//            if (!TextUtils.isEmpty(pkgname)) {
//                component = new ComponentName(pkgname, DOWNLOAD_CLASS);
//            }
//        }
//        return component;
//    }
//    /* YUNOS END */

    public Boolean isCardBgIcon(Drawable backgroud) {
        if (backgroud == null) {
            return false;
        }
        return mCardBgIconCache.containsValue(backgroud);
    }

//    public Bitmap getFolderUnifiedIcon(FolderInfo info) {
//        Log.d(TAG, "getFolderUnifiedIcon : begin");
//        if (info == null) {
//            Log.w(TAG, "getFolderUnifiedIcon error: info is null");
//            return null;
//        }
//        Bitmap icon = null;
//        // ArrayList<View> items = info.folder.getItemsInReadingOrder(false);
//        // No-Done
//        Log.d(TAG, "getFolderUnifiedIcon : end");
//        return icon;
//    }

//    public Drawable getFolderBackground() {
//        return mFolderBg;
//    }

//    public Drawable getFolderTitleBackgroud() {
//        if (!supprtCardIcon()) {
//            return null;
//        }
//        return mFolderTitleBg;
//    }

    private Drawable getAppIconFromCache(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "getAppIconFromCache error: component is null");
            return null;
        }
        Drawable icon = null;
        Key key = createKey(intent);
        icon = mIconCache.get(key);
        return icon;
    }

    public void addAppIconToCache(Intent intent, Drawable icon) {
        if (intent == null) {
            Log.d(TAG, "addAppIconToCache failed: component is null");
            return;
        }
        Key key = createKey(intent);
        mIconCache.put(key, icon);
    }

    private Drawable getFancyIcon(ComponentName component) {
        if (component == null) {
            Log.w(TAG, "getAppIconFromCache error: component is null");
            return null;
        }
        Drawable icon = null;
        try{
            // only get normal fancyicon, card fancyicon is not included
            icon = FancyIconsHelper.getIconDrawable(mContext, component,
                false);
        }catch(Throwable e){
            icon =  null;
        }
        return icon;
    }

    private Drawable getAppIconFromTheme(ComponentName component) {
        if (component == null) {
            Log.w(TAG, "getAppIconFromTheme error: component is null");
            return null;
        }
        Drawable icon = null;
        Intent i = new Intent();
        i.setComponent(component);
        Bitmap bm = ThemeUtils.getAppIcon(mContext, i);
        if (bm != null) {
            icon = new FastBitmapDrawable(bm);
        }
        return icon;
    }

    private Drawable getAppIconFromPackage(ComponentName component,
            boolean unify) {
        Drawable icon = null;
        // No need to implement now, because theme has get icon from
        // PackageManager.
        return icon;
    }

    private Drawable getAppIconFromPackage(ComponentName component) {
        return getAppIconFromPackage(component, true);
    }

    public Drawable getDefaultIcon() {
        if (mDefaultIcon == null) {
            buildDefaultIcon();
        }
        return mDefaultIcon;
    }

    private void buildDefaultIcon() {
        Drawable src = mContext.getResources().getDrawable(mDefaultIconResouce);
        mDefaultIcon = buildUnifiedIcon(src);
    }

    private Drawable getCardBgIcon(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "getCardBgIcon error: component is null");
            return null;
        }

        Key key = createKey(intent);
        Drawable cardbgicon = null;
        cardbgicon = mCardBgIconCache.get(key);
        if (cardbgicon == null) {
            // get big card fancyicon
            try {
                cardbgicon = FancyIconsHelper
                        .getIconDrawable(mContext, intent.getComponent(), true);
            } catch (IllegalStateException e) {
                Log.e(TAG, "getIconDrawable failed, theme is switching", e);
            }
            if (cardbgicon != null) {
                mCardBgIconCache.put(key, cardbgicon);
            }
        }
        return cardbgicon;
    }

    // private long totaltimecolor = 0;
    private Drawable getCardBgColorDrawable(ItemInfo info) {
        // long time = System.currentTimeMillis();
        if (info == null) {
            Log.w(TAG, "getCardBgIcon error: info is null");
            return null;
        }
        int cardbgcolor = getCardBgColor(info);
        if (cardbgcolor == 0) {
            return null;
        }
        Drawable cardbg = getCardMask();
        if (cardbg != null) {
            cardbg.setColorFilter(cardbgcolor, PorterDuff.Mode.SRC_ATOP);
        }
        // long usedtime = System.currentTimeMillis() - time;
        // totaltimecolor += usedtime;
        // Log.e("vqx376",
        // "getCardBgColorDrawable : app = "+info.title+" --- time = "+usedtime+" --- totaltime = "+totaltimecolor);
        return cardbg;
    }

    /* YUNOS END */

    private int getCardBgColor(ItemInfo info) {
        if (info == null) {
            Log.w(TAG, "getCardBgIcon error: info is null");
            return 0;
        }

        int cardbgcolor = 0;
        cardbgcolor = getCardBgColorFromCache(info);
        if (cardbgcolor == 0) {
            cardbgcolor = getCardBgColorFromPool(info);
            if (cardbgcolor != 0) {
                addCardBgColorToCachel(info, cardbgcolor);
            }
        }

        if (cardbgcolor == 0) {
            cardbgcolor = DEFAULT_CARD_BACKGROUD;
        }
        return cardbgcolor;
    }

    /* YUNOS BEGIN */
    // ##module(Icon)
    // ##date:2014/26/04 ##author:jun.dongj@alibaba-inc.com##114778
    // the card icon are ugly
    private int getAllCardBgColor(ItemInfo info) {
        int cardbgcolor = getCardBgColor(info);
        return cardbgcolor;
    }

    /* YUNOS END */

    public void clearCardBackgroud(Intent intent) {
        Key key = createKey(intent);
        mCardBgColorCache.remove(key);
        Drawable icon = mCardBgIconCache.remove(key);
        if (icon == null) {
            return;
        }

        try {
            Bitmap bmp = null;
            if (icon instanceof FastBitmapDrawable) {
                bmp = ((FastBitmapDrawable) icon).getBitmap();
            } else {
                bmp = ((BitmapDrawable) icon.getCurrent()).getBitmap();
            }
            if (bmp != null && !bmp.isRecycled()) {
                bmp.recycle();
            }
        } catch (Exception e) {
        }
    }

    /**
     * Clear the small icon cache for specified app.
     */
    public void clearIconCache(Intent intent) {
        Key key = createKey(intent);
        mIconCache.remove(key);
        /**
         * Why not call Bitmap.recycle() here?
         * 1. the bitmap might be still used by a BubbleTextView
         * 2. recycle method will be called in finalize method of bitmap automatically
         * 3. IconManager.reset() is not explicitly call recycle() neither
         * by zhanggong.zg
         */
    }

    private int getCardBgColorFromCache(ItemInfo info) {
        if (info == null) {
            Log.w(TAG, "getCardBgColorFromCache error: component is null");
            return 0;
        }

        Intent intent = getIntentFromInfo(info);
        Key key = createKey(intent);
        int color = 0;
        Integer value = mCardBgColorCache.get(key);
        if (value != null) {
            color = value.intValue();
        }
        return color;
    }

    private void addCardBgColorToCachel(ItemInfo info, int color) {
        if (info == null || color == 0) {
            Log.w(TAG, "addCardBgColorToCachel, param wrong");
            return;
        }
        Intent intent = getIntentFromInfo(info);
        Key key = createKey(intent);
        mCardBgColorCache.put(key, color);
    }

    private int getCardBgColorFromPool(ItemInfo info) {
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
            return 0;
        }
        Drawable icon = getAppUnifiedIcon(info, null);
        // iocn is null, return color value 0
        if ((icon == null) || isDefaultIcon(icon)) {
            return 0;
        }
        int color = calculateIconColor(icon);
        color = matchColorInPool(color);
        return color;
    }

    private int calculateIconColor(Drawable icon) {
        // return calculateIconColorEdge(icon);
        int color = calculateIconColorWhole(icon);
        return color;
    }

//    private int calculateIconColorEdge(Drawable icon) {
//        Bitmap src = null;
//        if (icon instanceof FastBitmapDrawable) {
//            src = ((FastBitmapDrawable) icon).getBitmap();
//        } else {
//            src = ((BitmapDrawable) icon.getCurrent()).getBitmap();
//        }
//        int color = 0;
//        // Alpha range is 0...255
//        final int minAlpha = ALPHA_MIN;
//
//        final int stride = 10;
//
//        // Saturation range is 0...1
//        float minSaturation = 0.2f;
//
//        int width = src.getWidth();
//        int height = src.getHeight();
//
//        // Number of pixels to sample
//        int hSamples = width / stride;
//        int vSamples = height / stride;
//
//        int pixels[] = new int[width * height];
//        src.getPixels(pixels, 0, width, 0, 0, width, height);
//
//        // Holds temporary sum of HSV values
//        float[] sampleTotals = {
//                0, 0, 0
//        };
//
//        // Loop through pixels horizontally
//        float[] hsv = new float[3];
//        int sample;
//        int sampleSize = 0;
//        for (int j = 0; j < height / 5; j += 1) {
//            // Loop through pixels horizontal
//            int s = j * width;
//            for (int i = 0; i < width / 5; i += 1) {
//                // Get pixel & convert to HSV format
//                sample = pixels[s + i];
//                // Check pixel matches criteria to be included in sample
//                if ((Color.alpha(sample) > minAlpha)) {
//                    Color.colorToHSV(sample, hsv);
//                    if (hsv[1] >= minSaturation) {
//                        // Add sample values to total
//                        sampleTotals[0] += hsv[0]; // H
//                        sampleTotals[1] += hsv[1]; // S
//                        sampleTotals[2] += hsv[2]; // V
//                        sampleSize++;
//                    }
//                }
//            }
//        }
//        if (sampleSize == 0)
//            return Color.TRANSPARENT;
//
//        sampleTotals[0] /= sampleSize;
//        sampleTotals[1] /= sampleSize;
//        sampleTotals[2] /= sampleSize;
//
//        float deltaR = 256 * 256;
//        float deltaG = 256 * 256;
//        float deltaB = 256 * 256;
//        float delta = 256 * 256 * 3;
//        float tempR;
//        float tempG;
//        float tempB;
//        float temp;
//        float r = 0, g = 0, b = 0;
//        for (int j = height / 10; j < height / 5; j += 1) {
//            // Loop through pixels horizontal
//            int s = j * width;
//            for (int i = width / 10; i < width / 5; i += 1) {
//                // Get pixel & convert to HSV format
//                sample = pixels[s + i];
//                // Check pixel matches criteria to be included in sample
//                if ((Color.alpha(sample) > minAlpha)) {
//                    Color.colorToHSV(sample, hsv);
//                    if (hsv[1] >= minSaturation) {
//                        // Add sample values to total
//                        tempR = Math.abs(sampleTotals[0] - hsv[0]);
//                        tempG = Math.abs(sampleTotals[1] - hsv[1]);
//                        tempB = Math.abs(sampleTotals[2] - hsv[2]);
//                        tempR = tempR * tempR;
//                        tempG = tempG * tempG;
//                        tempB = tempB * tempB;
//                        temp = tempR + tempG + tempB;
//                        if (temp < delta) {
//                            delta = temp;
//                            r = hsv[0];
//                            g = hsv[1];
//                            b = hsv[2];
//                        }
//                        sampleSize++;
//                    }
//                }
//            }
//        }
//
//        sampleTotals[0] = r;
//        sampleTotals[1] = g;
//        sampleTotals[2] = b;
//        sampleTotals[1] /= DARKEN_LEVEL;
//        sampleTotals[2] /= DARKEN_LEVEL;
//
//        // Return average tuplet as RGB color
//        color = Color.HSVToColor(ALPHA_ATOP, sampleTotals);
//        return color;
//    }

    private int calculateIconColorWhole(Drawable icon) {
        Bitmap src = null;
        if (icon instanceof FastBitmapDrawable) {
            src = ((FastBitmapDrawable) icon).getBitmap();
        } else if (icon instanceof app.aliyun.graphics.FastBitmapDrawable) {
            src = ((app.aliyun.graphics.FastBitmapDrawable) icon).getBitmap();
        } else {
            src = ((BitmapDrawable) icon.getCurrent()).getBitmap();
        }
        int color = 0;
        // Alpha range is 0...255
        final int minAlpha = ALPHA_MIN;

        final int stride = 10;

        // Saturation range is 0...1
        float minSaturation = 0.2f;

        int width = src.getWidth();
        int height = src.getHeight();

        // Number of pixels to sample
        int hSamples = width / stride;
        int vSamples = height / stride;

        int pixels[] = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        // Holds temporary sum of HSV values
        float[] sampleTotals = {
                0, 0, 0
        };

        // Loop through pixels horizontally
        float[] hsv = new float[3];
        int sample;
        int sampleSize = 0;
        for (int j = vSamples, sV = height / vSamples; j < height; j += sV) {
            // Loop through pixels horizontal
            int s = j * width;
            for (int i = hSamples, sH = width / hSamples; i < width; i += sH) {
                // Get pixel & convert to HSV format
                sample = pixels[s + i];
                // Check pixel matches criteria to be included in sample
                if ((Color.alpha(sample) > minAlpha)) {
                    Color.colorToHSV(sample, hsv);
                    if (hsv[1] >= minSaturation) {
                        // Add sample values to total
                        sampleTotals[0] += hsv[0]; // H
                        sampleTotals[1] += hsv[1]; // S
                        sampleTotals[2] += hsv[2]; // V
                        sampleSize++;
                    }
                }
            }
        }
        if (sampleSize == 0) {
            Log.w(TAG, "No sample point, return");
            return Color.TRANSPARENT;
        }

        sampleTotals[0] /= sampleSize;
        sampleTotals[1] /= sampleSize;
        sampleTotals[2] /= sampleSize;
        sampleTotals[1] /= DARKEN_LEVEL;
        sampleTotals[2] /= DARKEN_LEVEL;

        // Return average tuplet as RGB color
        color = Color.HSVToColor(ALPHA_ATOP, sampleTotals);
        return color;
    }

    private int matchColorInPool(int color) {
        int result = color;
        // int delta = 0xffffffff;
        // int temp;
        // for (Integer c : mCardBgColorPool) {
        // temp = Math.abs(color - c);
        // if (delta > temp) {
        // temp = delta;
        // result = c;
        // }
        // }
        return result;
    }

    public boolean isDefaultIcon(Drawable icon) {
        boolean ret = (getDefaultIcon() == icon);
        return ret;
    }

    public Drawable getIconFromCursor(IconCursorInfo info) {
        return getIconFromCursor(info, true);
    }

    public Drawable getIconFromCursor(IconCursorInfo info, boolean unify) {
        if (info == null) {
            Log.w(TAG, "getIconFromCursor error: info is null");
            return null;
        }
        final boolean debug = true;
        Cursor c = info.mCursor;
        if (c == null) {
            return null;
        }
//        int iconIndex = info.mIconIndex;
        if (debug) {
            Log.d(TAG,
                    "getIconFromCursor app="
                            + c.getString(c
                                    .getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
        }
        byte[] data = c.getBlob(c.getColumnIndexOrThrow("icon"));
        try {
            Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap originicon = bm;// Utilities.createIconBitmap(
            // bm,
            // mContext);
            // now the icon ,from theme or packgaemanger, has been handled by
            // theme, so the icon id db is also handed by theme
            // the unify is no work,now
            // if (unify) {
            // return buildUnifiedIcon(originicon);
            // } else {
            // return new FastBitmapDrawable(originicon);
            // }
            return new FastBitmapDrawable(originicon);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(),
                android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }
        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public Drawable getFullResIcon(ActivityInfo info) {
        Resources resources;
        try {
            resources = mPackageManager
                    .getResourcesForApplication(info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Intent getIntentFromInfo(ItemInfo info) {
        Intent intent = null;
        if (info instanceof ShortcutInfo) {
            intent = ((ShortcutInfo) info).intent;
        } else if (info instanceof ApplicationInfo) {
            intent = ((ApplicationInfo) info).intent;
        }
        return intent;
    }

    /* YUNOS BEGIN */
    // ##date:2014/9/5 ##author:zhangqiang.zq
    // 5186119
    public final Drawable buildHotSeatIcon(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if(mNeedDockCardIcon){
            Resources res = LauncherApplication.getContext().getResources();
            int newWidth = res.getDimensionPixelSize(R.dimen.hotseat_card_icon_size);
            int newHeight = newWidth;
            int topPadding = res.getDimensionPixelSize(R.dimen.hotseat_card_icon_top_padding);

            Bitmap scaledBitmap = IconUtils.drawable2ScaledBitmap(drawable,
                    newWidth, newHeight);
            Drawable newIcon = IconUtils.createBackgroundIcon(
                    res, scaledBitmap, newWidth, newHeight, topPadding);
            if (scaledBitmap != null && !scaledBitmap.isRecycled()) {
                scaledBitmap.recycle();
            }
            return newIcon;
        }else{
            final Drawable border = getIconBorder();
            if(border != null){
                return new LayerDrawable(new Drawable[]{drawable,border});
            }else{
                return drawable;
            }
        }
    }

    /* YUNOS BEGIN */
    // ##homeshell
    // ##date:2014/04/04 ##author:jun.dongj@alibaba-inc.com##BugID:107918
    // title color changed according to card background color
    public int getTitleColor(ShortcutInfo info) {
        int color = IconUtils.TITLE_COLOR_WHITE;
        if (info == null) {
            Log.w(TAG, "getTitleColor error : info is null");
            return color;
        }

        if (!supprtCardIcon()) {
            Log.d(TAG, "getTitleColor : not support card");
            return color;
        }
        Drawable cardbg = null;
        // get card background icon
        cardbg = getAppCardBackgroud(info);
        return getTitleColor(info, cardbg);
    }
    /* YUNOS END */

    private int getTitleColor(ItemInfo info, Drawable cardbg) {
        if (HomeShellGadgetsRender.isHomeShellGadgets(info)) {
            return HomeShellGadgetsRender.getTitleColor(info);
        }
        int color = IconUtils.TITLE_COLOR_WHITE;
        Bitmap bgicon = null;
        try {

            if (cardbg instanceof CardIconDrawable) {
                // ##date:2015/4/13 ##author:zhanggong.zg ##BugID:5905241
                // Optimize memory usage for big card icon
                CardIconDrawable card = (CardIconDrawable) cardbg;
                int[] colorArray = card.getBackgroundColorArray();
                color = IconUtils.getTitleColor(colorArray,
                            card.getIntrinsicHeight() - card.getIntrinsicWidth(),
                            card.getIntrinsicHeight());
                return color;
            } else if (cardbg instanceof FastBitmapDrawable) {
                bgicon = ((FastBitmapDrawable) cardbg).getBitmap();
            } else if (cardbg instanceof app.aliyun.graphics.FastBitmapDrawable) {
                bgicon = ((app.aliyun.graphics.FastBitmapDrawable) cardbg).getBitmap();
            } else if (cardbg instanceof FancyDrawable) {
                // title color of FancyDrawable is decided by Fancy Icon self
                boolean black_color = Boolean.parseBoolean(((FancyDrawable) cardbg)
                        .getRawAttr("blackLabel"));
                if (black_color) {
                    color = IconUtils.TITLE_COLOR_BLACK;
                }
                return color;
            } else {
                bgicon = ((BitmapDrawable) cardbg.getCurrent()).getBitmap();
            }
            color = IconUtils.getTitleColor(bgicon);
        } catch (NullPointerException e) {
            cardbg = null;
        }

        // get card background color
        if (cardbg == null) {
            int bgcolor = getAllCardBgColor(info);
            color = IconUtils.getTitleColor(bgcolor);
        }
        return color;
    }

    public static int getIconSize(Context context) {
        int suggestwidth = ThemeUtils.getIconSize(context);
        if (suggestwidth < 0) {
            suggestwidth = context.getResources().getDimensionPixelSize(
                    R.dimen.bubble_icon_width);
        }
        return suggestwidth;
    }

    public static class IconCursorInfo {
        public IconCursorInfo() {
        }

        public IconCursorInfo(Cursor c, int index, boolean onlyusedb) {
            mCursor = c;
            mIconIndex = index;
            mOnlyUseDB = onlyusedb;
        }

        public IconCursorInfo(Cursor c, int index) {
            mCursor = c;
            mIconIndex = index;
        }

        public Cursor mCursor = null;;
        public int mIconIndex = -1;;
        public boolean mOnlyUseDB = false;
    }

    private Key createKey(Intent intent) {
        return createKey(intent.toUri(0));
    }

    private Key createKey(String str) {
        return new Key(str);
    }

    private class Key {
        private String mKey = null;

        public Key(String str) {
            generateKey(str);
        }

        private void generateKey(String source) {
            if (source == null) {
                return;
            }

            char hexDigits[] = {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
            };

            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                md.update(source.getBytes());
                byte tmp[] = md.digest();
                char str[] = new char[16 * 2];
                int k = 0;
                for (int i = 0; i < 16; i++) {
                    byte byte0 = tmp[i];
                    str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                    str[k++] = hexDigits[byte0 & 0xf];
                }
                mKey = new String(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return "Key [mKey=" + mKey + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((mKey == null) ? 0 : mKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (mKey == null) {
                if (other.mKey != null)
                    return false;
            } else if (!mKey.equals(other.mKey))
                return false;
            return true;
        }

        private IconManager getOuterType() {
            return IconManager.this;
        }
    }
}
