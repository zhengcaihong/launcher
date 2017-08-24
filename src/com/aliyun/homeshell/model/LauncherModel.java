/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.homeshell.model;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;




/*YUNOS BEGIN*/
//##module(HomeShell)
//##date:2014/03/27 ##author:hao.liuhaolh@alibaba-inc.com##BugID:105074
//restore the deleted icon code in launcher model
import app.aliyun.content.res.ThemeResources;
/*YUNOS END*/
import app.aliyun.v3.gadget.GadgetInfo;
import app.aliyun.v3.res.FancyIconsHelper;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.AllAppsList;
import com.aliyun.homeshell.AppDownloadManager;
import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.ApplicationInfo;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.CheckVoiceCommandPressHelper;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.DeferredHandler;
import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.FolderInfo;
import com.aliyun.homeshell.GadgetCardHelper;
import com.aliyun.homeshell.GadgetItemInfo;
import com.aliyun.homeshell.editmode.WidgetPreviewLoader;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.IconDigitalMarkHandler;
import com.aliyun.homeshell.InstallShortcutReceiver;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherAppWidgetInfo;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherGadgetHelper;
import com.aliyun.homeshell.LauncherProvider;
import com.aliyun.homeshell.LauncherProvider.updateArgs;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ScreenPosition;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.Utilities;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.appclone.AppCloneManager.CloneAppKey;
import com.aliyun.homeshell.appclone.CloneResolveInfo;
import com.aliyun.homeshell.appgroup.AppGroupManager;
import com.aliyun.homeshell.appgroup.CategoryInfo;
import com.aliyun.homeshell.backuprestore.BackupManager;
import com.aliyun.homeshell.backuprestore.BackupUitil;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.icon.IconManager.IconCursorInfo;
import com.aliyun.homeshell.icon.IconUtils;
import com.aliyun.homeshell.icon.BubbleResources;
import com.aliyun.homeshell.iconupdate.IconUpdateManager;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.homeshell.smartlocate.AppLaunchManager;
import com.aliyun.homeshell.smartlocate.NewInstallAppHelper;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.homeshell.utils.Utils;
// remove vp install
//import com.aliyun.homeshell.vpinstall.IInstallStateListener;
//import com.aliyun.homeshell.vpinstall.VPInstaller;
//import com.aliyun.homeshell.vpinstall.VPInstaller.AppKey;
//import com.aliyun.homeshell.vpinstall.VPUtils;
//import com.aliyun.homeshell.vpinstall.VPUtils.VPInstallStatus;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.utility.utils.ACA;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* YUNOS BEGIN CMCC */
//author:xianqiu.zbb
//BugID:(8716966) ##date:2016.8.15
//descrpition: 修改移动入库新的应用预置需求
import android.os.SystemProperties;
/* YUNOS END CMCC */

/*YUNOS BEGIN*/
//##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
//vp install
/*YUNOS END*/

/*YUNOS BEGIN*/
//##date:2014/01/10 ##author:hao.liuhaolh ##BugID: 84736
// SD app icon position change in restore
/*YUNOS END*/
/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
// remove vp install
//public class LauncherModel extends BroadcastReceiver implements IInstallStateListener{
public class LauncherModel extends BroadcastReceiver {

    static final boolean DEBUG_LOADERS = true;            //YUNOS open log
    public static final boolean DEBUG_OCCUPY = false;
    static final String TAG = "Launcher.Model";
    private int mWaitBindForOrienChangedCount = 0;
    public static final String NEW_APPS_PAGE_KEY = "apps.new.page";
    public static final String NEW_APPS_LIST_KEY = "apps.new.list";
    public static HomeShellSetting homeshellSetting = null;
    /* YUNOS BEGIN */
    //##date:2014/04/11 ##author:nater.wg ##BugID: 109352
    // Action for reload downloading apps, sent to app store
    public static final String ACTION_RELOAD_DOWNLOADING = "com.aliyun.homeshell.action.RELOAD_DOWNLOADING_APPS";
    /* YUNOS END */
    public static final String ACTION_REMOVE_APP_VIEWS = "com.aliyun.homeshell.action.ACTION_REMOVE_APP_VIEWS";
    public static final String TYPE_PACKAGENAME="packageName";
    //for layout change
    public static final String ACTION_HOMESHELL_LAYOUT_CHANGE = "com.aliyun.homeshell.action.LAYOUT_CHANGE";
    /* YUNOS BEGIN */
    // ##date:2014/06/05 ##author:hongchao.ghc ##BugID:126343
    // for settings show icon mark
    public static final String EXTRA_COUNTX = "countX";
    public static final String EXTRA_COUNTY = "countY";

    public static final String ACTION_UPDATE_LAYOUT = "com.aliyun.homeshell.action.UPDATE_LAYOUT";
    public static final String ACTION_UPDATE_SLIDEUP = "com.aliyun.homeshell.action.UPDATE_SLIDEUP";
    public static final String ACTION_UPDATE_CLONABLE = "com.aliyun.homeshell.action.UPDATE_CLONABLE";
    /* YUNOS END */

    public static final String ACTION_APP_LAUNCHED = "com.yunos.NOTIFY_HOMESHELL_APP_LAUNCHED";

    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    public static final String ACTION_AGED_MODE_CHANGED = "aged_mode_changed";
    /*YUNOS END*/

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons

    /*YUNOS BEGIN*/
    //##date:2014/03/10 ##author:hao.liuhaolh ##BugID: 98027
    //widget and app icon overlay
    private static int mMaxIconScreenCount = ConfigManager.getIconScreenMaxCount();
    /*YUNOS END*/

    private int mBatchSize; // 0 is all apps at once
    private int mAllAppsLoadDelay; // milliseconds between batches

    private final LauncherApplication mApp;

// remove vp install
//    private VPUtils mVPUtils;

    private final Object mLock = new Object();
    private static DeferredHandler mHandler = new DeferredHandler();
    private LoaderTask mLoaderTask;
    private boolean mIsLoaderTaskRunning;
    private volatile boolean mFlushingWorkerThread;

    // Specific runnable types that are run on the main thread deferred handler, this allows us to
    // clear all queued binding runnables when the Launcher activity is destroyed.
//    private static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    private static final int MAIN_THREAD_BINDING_RUNNABLE = 1;
    /* YUNOS BEGIN */
    // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
    // homeshell boot anr
    private static final int MAIN_THREAD_THEME_CHANGE_RUNNABLE = 2;
    /*YUNOS END*/

    private boolean mIsLoadingAndBindingWorkspace;

    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    // theme changed flag
    private boolean mThemeChanged = false;
    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    private boolean mWorkspaceLoaded;
    private boolean mAllAppsLoaded;
    private static boolean mOrienLandedOnLoadStart = false;
    private boolean mClearAllDownload =  false;

    // When we are loading pages synchronously, we can't just post the binding of items on the side
    // pages as this delays the rotation process.  Instead, we wait for a callback from the first
    // draw (in Workspace) to initiate the binding of the remaining side pages.  Any time we start
    // a normal load, we also clear this set of Runnables.
    static final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList<Runnable>();

    /*YUNOS BEGIN*/
    //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
    //vp install
    public static WeakReference<Callbacks> mCallbacks;
    /*YUNOS END*/

    // < only access in worker thread >
    private AllAppsList mBgAllAppsList;

    /*YUNOS BEGIN*/
    //##date:2013/12/15 ##author:hao.liuhaolh ##BugID: 73737
    // SD card app icon lost
    private List<ResolveInfo> appsOnBoot = null;
    /*YUNOS END*/

    // The lock that must be acquired before referencing any static bg data structures.  Unlike
    // other locks, this one can generally be held long-term because we never expect any of these
    // static data structures to be referenced outside of the worker thread except on the first
    // load after configuration change.
    static final Object sBgLock = new Object();

    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    public static final HashMap<Long, ItemInfo> sBgItemsIdMap = new HashMap<Long, ItemInfo>();

    // sBgWorkspaceItems is passed to bindItems, which expects a list of all folders and shortcuts
    //       created by LauncherModel that are directly on the home screen (however, no widgets or
    //       shortcuts within folders).
    public static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();

    /*YUNOS BEGIN LH handle installed app no space */
    // sBgNoSpaceItems is all applications these has no space in workspace.
    // These applications are waiting for the empty cell and will be placed in workspace
    // if space available.
    static final ArrayList<ItemInfo> sBgNoSpaceItems = new ArrayList<ItemInfo>();
    /*YUNOS END*/
    // sBgAppWidgets is all LauncherAppWidgetInfo created by LauncherModel. Passed to bindAppWidget()
    public static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets =
        new ArrayList<LauncherAppWidgetInfo>();

    // sBgFolders is all FolderInfos created by LauncherModel. Passed to bindFolders()
    static final HashMap<Long, FolderInfo> sBgFolders = new HashMap<Long, FolderInfo>();

    static private ArrayList<boolean[][]> sWorkspaceOccupiedLand = null;
    static private ArrayList<boolean[][]> sWorkspaceOccupiedPort = null;
    static private ArrayList<boolean[][]> sWorkspaceOccupiedCurrent = null;
    static private ArrayList<boolean[][]> sWorkspaceOccupiedNonCurrent = null;
    static private ArrayList<boolean[][]> sHideseatOccupied = null;
    /*YUNOS BEGIN*/
    //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
    // client yun icon lost after restore
    // sBgDbIconCache is the set of ItemInfos that need to have their icons updated in the database
    //static final HashMap<Object, byte[]> sBgDbIconCache = new HashMap<Object, byte[]>();
    // </ only access in worker thread >
    /*YUNOS END*/

//    private Bitmap mDefaultIcon;

    //BugID:6002887:ArrayIndexOutOfBoundsException in LauncherModel
    private static int sCellCountX = ConfigManager.getCellCountX();
    private static int sCellCountY = ConfigManager.getCellCountY();

    protected int mPreviousConfigMcc;

    private AppDownloadManager mAppDownloadMgr;
    private AppGroupManager mAppGroupMgr;

    private IconUpdateManager mIconUpdateMgr;
    /*YUNOS BEGIN*/
    //##module(HomeShell)
    //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
    //check restore data to avoid no item in dock
    private ArrayList<String> allComponentList = new ArrayList<String>();
    private List<CloneAppKey> mAllCloneAppList;
    /*YUNOS END*/

    private IconManager mIconManager = null;
    private final PackageUpdateTaskQueue mPackageUpdateTaskQueue = new PackageUpdateTaskQueue();
    private static boolean sEnableThreadCheck = true;
    private static int sUIThreadid;

    private static final int INVALID_MARK_TYPE = -1;
    private static int mMarkType = INVALID_MARK_TYPE;
    private static boolean mShowNewMarkInit;
    private static boolean mShowNewMark;
    private static boolean mShowSlideUpMarkInit;
    private static boolean mShowSlideUpMark;
    private static boolean mShowClonableMarkInit;
    private static boolean mShowClonableMark;
    private static ArrayList<InstallingRecord> sInstallingApps = new ArrayList<InstallingRecord>();
    private static final int APP_INSTALL_TIME_MAX = 2 * 60 * 1000;

    //BugID:5628070:ANR during mode change
    private static Runnable checkNoSpaceListR = new Runnable() {
        @Override
        public void run() {
            checkNoSpaceList();
        }
    };

    public interface Callbacks {
        public boolean setLoadOnResume();
        public int getCurrentWorkspaceScreen();
        public void startBinding();
        public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end);
        public void removeFolder(FolderInfo folder);
        public void bindFolders(HashMap<Long,FolderInfo> folders);
        public void finishBindingItems();
        public void bindAppWidget(LauncherAppWidgetInfo info);
        public void bindAllApplications(ArrayList<ApplicationInfo> apps);
        public void bindAppsAdded(ArrayList<ApplicationInfo> apps);
        public void bindAppsUpdated(ArrayList<ApplicationInfo> apps);
        public void bindComponentsRemoved(ArrayList<String> packageNames,
                        ArrayList<ApplicationInfo> appInfos,
                        boolean matchPackageNamesOnly);
        public void bindPackagesUpdated(ArrayList<Object> widgetsAndShortcuts);
        public boolean isAllAppsVisible();
        public boolean isAllAppsButtonRank(int rank);
        public void onPageBoundSynchronously(int page);
        public void bindRemoveScreen(int screen);
        /*YUNOS BEGIN LH*/
        public void bindItemsUpdated(ArrayList<ItemInfo> items);
        public void bindDownloadItemsRemoved(ArrayList<ItemInfo> items, boolean permanent);
        public void bindItemsRemoved(final ArrayList<ItemInfo> items);
        //only for install app and download
        public void bindItemsAdded(ArrayList<ItemInfo> items);
        public void bindRebuildHotseat(ArrayList<ItemInfo> items);
        /*YUNOS END*/

        public void bindItemsChunkUpdated(ArrayList<ItemInfo> items, int start, int end, boolean themeChange);


        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container

        public void bindRebuildHideseat(ArrayList<ItemInfo> items);
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
        //vp install item icon display
// remove vp install
//        public void startVPInstallActivity(Intent intent, Object tag);
        /*YUNOS END*/

        //BugID:114988:find and remove one item folder after all restore app handled by appstore
        public void bindItemsViewRemoved(ArrayList<ItemInfo> items);
        public void bindItemsViewAdded(ArrayList<ItemInfo> items);
        public void bindWorkspaceItemsViewMoved(ArrayList<ItemInfo> items);
        //BugID:5204915:mLauncher is null in LauncherApplication
        public void resetWorkspaceGridSize(int countX, int countY);
        public void checkAndRemoveEmptyCell();
        public void closeFolderWithoutExpandAnimation();
        public boolean isInEditScreenMode();
        public void collectCurrentViews();
        public void reLayoutCurrentViews();
        public void reLayoutCurrentViews(final List<ItemInfo> allItems);
        public void removeWidgetPages();
        public void makesureWidgetPages();
        public void setLauncherCategoryMode(boolean categoryMode);
        public void enterLauncherCategoryMode();
        public void exitLauncherCategoryMode(boolean ok);
    }

    public LauncherModel(LauncherApplication app) {
        sUIThreadid = Process.myTid();
        mApp = app;
        mBgAllAppsList = new AllAppsList();
        mIconManager = app.getIconManager();
        final Resources res = app.getResources();
        mAllAppsLoadDelay = res.getInteger(R.integer.config_allAppsBatchLoadDelay);
        mBatchSize = res.getInteger(R.integer.config_allAppsBatchSize);
        Configuration config = res.getConfiguration();
        mPreviousConfigMcc = config.mcc;
        mAppDownloadMgr = AppDownloadManager.getInstance();
        mAppDownloadMgr.setup(mApp, this, mHandler);
        mAppGroupMgr = AppGroupManager.getInstance();
        mIconUpdateMgr = IconUpdateManager.getInstance();
        mIconUpdateMgr.loadUpdateIconInfoFromDB();
//remove vp install
//        mVPUtils = new VPUtils(mApp);

        /* YUNOS BEGIN */
        // ##date:2015/3/26 ##author:sunchen.sc##5735130
        // Create UI occupied
        reCreateUiOccupied(ConfigManager.getCellCountX(Configuration.ORIENTATION_PORTRAIT),
                ConfigManager.getCellCountY(Configuration.ORIENTATION_PORTRAIT));
        /*YUNOS END*/
        Log.d(TAG, "max screen count is " + mMaxIconScreenCount);
    }

    /** Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler. */
    /*YUNOS BEGIN*/
    //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
    //vp install
    public static void runOnMainThread(Runnable r) {
    /*YUNOS END*/
        runOnMainThread(r, 0);
    }
    private static void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            /* YUNOS BEGIN */
            // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
            // homeshell boot anr
            mHandler.post(r,type);
            /*YUNOS END*/
        } else {
            r.run();
        }
    }

    /** Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler. */
    public static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }
    public void destroy() {
        appsOnBoot.clear();
        appsOnBoot = null;
        mAppDownloadMgr.unBindAppStoreService();
    }
    public Drawable getFallbackIcon() {
        return mIconManager.getDefaultIcon();
    }

    public void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the " +
                    "main thread");
        }

        // Clear any deferred bind runnables
        mDeferredBindRunnables.clear();
        // Remove any queued bind runnables
        mHandler.cancelAllRunnablesOfType(MAIN_THREAD_BINDING_RUNNABLE);
        // Unbind all the workspace items
        unbindWorkspaceItemsOnMainThread();
    }

    /** Unbinds all the sBgWorkspaceItems and sBgAppWidgets on the main thread */
    void unbindWorkspaceItemsOnMainThread() {
        // Ensure that we don't use the same workspace items data structure on the main thread
        // by making a copy of workspace items first.
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
                @Override
                public void run() {
                   for (ItemInfo item : tmpWorkspaceItems) {
                       item.unbind();
                   }
                   for (ItemInfo item : tmpAppWidgets) {
                       item.unbind();
                   }
                }
            };
        runOnMainThread(r);
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    public static void addOrMoveItemInDatabase(Context context, ItemInfo item,
            long container,
            int screen, int cellX, int cellY) {
        Log.d(TAG, "add or move item in database");
        /*YUNOS BEGIN*/
        //##date:2014/7/1 ##author:yangshan.ys ##BugID:134192
        //it is unnecessary to write to database for editfoldericon
        if(item.itemFlags == Favorites.ITEM_FLAGS_EDIT_FOLDER) {
            Log.d(TAG,"prevent the editfoldericon write to database");
            return;
        }
        /*YUNOS END*/
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    /* YUNOS BEGIN */
    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Added to support pad orientation
    public static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container,
            int screen, int cellXPort, int cellYPort, int cellXLand, int cellYLand) {
        Log.d(TAG, "add or move item in database");
        if(item.itemFlags == Favorites.ITEM_FLAGS_EDIT_FOLDER) {
            Log.d(TAG,"prevent the editfoldericon write to database");
            return;
        }
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screen, cellXPort, cellYPort, cellXLand, cellYLand, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screen, cellXPort, cellYPort, cellXLand, cellYLand);
        }
    }
    /* YUNOS END */

    static void checkItemInfoLocked(
            final long itemId, final ItemInfo item, StackTraceElement[] stackTrace) {
        /*YUNOS BEGIN*/
        //##date:2013/12/11 ##author:hao.liuhaolh ##BugID:73742
        //runtime exception crash
        //This function just throw runtime exception if some error happens,
        //so cancel this function to avoid runtime exception crash
        return;

        /*
        ItemInfo modelItem = sBgItemsIdMap.get(itemId);
        if (modelItem != null && item != modelItem) {
            // check all the data is consistent
            if (modelItem instanceof ShortcutInfo && item instanceof ShortcutInfo) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) &&
                        modelShortcut.intent.filterEquals(shortcut.intent) &&
                        modelShortcut.id == shortcut.id &&
                        modelShortcut.itemType == shortcut.itemType &&
                        modelShortcut.container == shortcut.container &&
                        modelShortcut.screen == shortcut.screen &&
                        modelShortcut.cellX == shortcut.cellX &&
                        modelShortcut.cellY == shortcut.cellY &&
                        modelShortcut.spanX == shortcut.spanX &&
                        modelShortcut.spanY == shortcut.spanY &&
                        ((modelShortcut.dropPos == null && shortcut.dropPos == null) ||
                        (modelShortcut.dropPos != null &&
                                shortcut.dropPos != null &&
                                modelShortcut.dropPos[0] == shortcut.dropPos[0] &&
                        modelShortcut.dropPos[1] == shortcut.dropPos[1]))) {
                    // For all intents and purposes, this is the same object
                    return;
                }
            }

            // the modelItem needs to match up perfectly with item if our model is
            // to be consistent with the database-- for now, just require
            // modelItem == item or the equality check above
            String msg = "item: " + ((item != null) ? item.toString() : "null") +
                    "modelItem: " +
                    ((modelItem != null) ? modelItem.toString() : "null") +
                    "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
            }
            throw e;
        }
        */
        /*YUNOS END*/
    }

    public static void checkItemInfo(final ItemInfo item) {
        //BugID:5610676:checkItemInfoLocked is cancelled, so comment below code too.
        /*
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        Runnable r = new Runnable() {
            public void run() {
                synchronized (sBgLock) {
                    checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };
        runOnWorkerThread(r);
        */
    }

    static void updateItemInDatabaseHelper(Context context, final ContentValues values,
            final ItemInfo item, final String callingFunction, final boolean checkNoSpaceList) {
        final long itemId = item.id;
        final Uri uri = LauncherSettings.Favorites.getContentUri(itemId, false);
        final ContentResolver cr = context.getContentResolver();
//        final Context contextfinal = context;

        //BugID:5610676:stackTrace is no used
        //final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            public void run() {
                cr.update(uri, values, null, null);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    //BugID:5610676:don't call checkItemInfoLocked
                    //checkItemInfoLocked(itemId, item, stackTrace);

                    if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                            item.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                            /*YUNOS BEGIN*/
                            //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                            //add hide icon container
                            item.container != LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                            /*YUNOS END*/
                        // Item is in a folder, make sure this folder exists
                        if (!sBgFolders.containsKey(item.container)) {
                            // An items container is being set to a that of an item which is not in
                            // the list of Folders.
                            String msg = "item: " + item + " container being set to: " +
                                    item.container + ", not in the list of folders";
                            Log.e(TAG, msg);
                            Launcher.dumpDebugLogsToConsole();
                        }
                    }

                    // Items are added/removed from the corresponding FolderInfo elsewhere, such
                    // as in Workspace.onDrop. Here, we just add/remove them from the list of items
                    // that are on the desktop, as appropriate
                    ItemInfo modelItem = sBgItemsIdMap.get(itemId);
                    /*YUNOS BEGIN*/
                    //##date:2013/12/02 ##author:hao.liuhaolh
                    //crash in update item in db
                    if (modelItem == null) {
                        Log.d(TAG, "modelItem is null, itemId is "  + itemId);
                        //sBgWorkspaceItems.remove(item);
                    } else {
                        if (modelItem.container == LauncherSettings.Favorites.CONTAINER_DESKTOP ||
                                modelItem.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT ||
                                /*YUNOS BEGIN*/
                                //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                                //add hide icon container
                                modelItem.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                                /*YUNOS END*/
                            switch (modelItem.itemType) {
                                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                /*YUNOS BEGIN*/
                                //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
                                //vp install
// remove vp install
//                                case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                                /*YUNOS END*/
                                //BugID: 117502: download item's icon doesn't change when theme change
                                //add download item to sBgWorkspaceItems if it is moved from folder to workspace
                                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                                    if (!sBgWorkspaceItems.contains(modelItem)) {
                                        sBgWorkspaceItems.add(modelItem);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            sBgWorkspaceItems.remove(modelItem);
                        }
                    }
                    /*YUNOS END*/
                }

                // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5705812
                if (checkNoSpaceList) {
                    //run checkNoSpaceList after all update happened
                    //BugID:5628070:ANR during mode change
                    postCheckNoSpaceList();
                }

                sWorker.removeCallbacks(mCheckInvalidPosItemsRunnable);
                sWorker.postDelayed(mCheckInvalidPosItemsRunnable, 3000);
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemInDatabaseHelper(Context context, final ContentValues values,
                final ItemInfo item, final String callingFunction) {
        updateItemInDatabaseHelper(context, values, item, callingFunction, true);
    }

    public void flushWorkerThread() {
        mFlushingWorkerThread = true;
        Runnable waiter = new Runnable() {
                public void run() {
                    synchronized (this) {
                        notifyAll();
                        mFlushingWorkerThread = false;
                    }
                }
            };

        synchronized(waiter) {
            runOnWorkerThread(waiter);
            if (mLoaderTask != null) {
                synchronized(mLoaderTask) {
                    mLoaderTask.notify();
                }
            }
            boolean success = false;
            while (!success) {
                try {
                    waiter.wait();
                    success = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    //for pad land mode, temp function to make old code work.
    public static void moveItemInDatabase(Context context, final ItemInfo item,
            final long container,
            final int screen, final int cellX, final int cellY) {
            item.cellX = cellX;
            item.cellY = cellY;
            item.setCurrentOrientationXY(cellX, cellY);
            moveItemInDatabase(context, item, container, screen,
                    item.cellXPort, item.cellYPort, item.cellXLand, item.cellYLand);
            item.dumpXY(" after moveItemInDatabase ");
    }
    //for pad land mode
    public static void moveItemInDatabase(Context context, final ItemInfo item, final long container,
            final int screen, final int cellXPort, final int cellYPort, final int cellXLand, final int cellYLand) {
        Log.d(TAG, "moveItemInDatabase in");
        /*YUNOS BEGIN*/
        int oldscreen = -1;
        long oldcontainer = item.container;
        /*YUNOS END*/
        String transaction = "DbDebug    move item (" + item.title + ") in db, id: " + item.id +
                " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY +
                ") --> " + "(" + container + ", " + screen + ", " + cellXPort+ ", " + cellYPort + ")";
        //Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.container = container;
        item.cellXPort = cellXPort;
        item.cellYPort = cellYPort;
        item.cellXLand = cellXLand;
        item.cellYLand = cellYLand;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            /*YUNOS BEGIN*/
            //##date:2013/11/22 ##author:hao.liuhaolh
            //check empty screen and move items behind empty screen
            if (oldcontainer != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                oldscreen = item.screen;
            }
            /*YUNOS END*/
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellXPort, cellYPort);
        }
        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container
        else if (context instanceof Launcher && screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
            if (oldcontainer != LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                oldscreen = item.screen;
            }
            //TODO: liuhao need mHideseat
            //item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        }
        /*YUNOS END*/
        else {
            /*YUNOS BEGIN*/
            //##date:2013/11/22 ##author:hao.liuhaolh
            //check empty screen and move items behind empty screen
            if ((item.screen != screen) && (screen >= 0)) {
                oldscreen = item.screen;
            }
            Log.d(TAG, "oldscreen is " + oldscreen);
            /*YUNOS END*/
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLXPORT, item.cellXPort);
        values.put(LauncherSettings.Favorites.CELLYPORT, item.cellYPort);
        values.put(LauncherSettings.Favorites.CELLXLAND, item.cellXLand);
        values.put(LauncherSettings.Favorites.CELLYLAND, item.cellYLand);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);
        if (item instanceof ShortcutInfo) {
            values.put(LauncherSettings.Favorites.USER_ID, ((ShortcutInfo) item).userId);
        }

        updateItemInDatabaseHelper(context, values, item, "moveItemInDatabase");
        /*YUNOS END*/
    }

    /**
     * Move and/or resize item in the DB to a new <container, screen, cellX, cellY, spanX, spanY>
     */
    //for pad land mode, temp function to make old code work.
    public static void modifyItemInDatabase(Context context,
            final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY, final int spanX, final int spanY,
            boolean checkNoSpaceList) {
        item.cellX = cellX;
        item.cellY = cellY;
        item.setCurrentOrientationXY(cellX, cellY);
        modifyItemInDatabase(context, item, container, screen, item.cellXPort, item.cellYPort, item.cellXLand,
                item.cellYLand, spanX, spanY, checkNoSpaceList);
    }

    public static void modifyItemInDatabase(Context context, final ItemInfo item,
            final long container, final int screen, final int cellXPort, final int cellYPort,
            final int cellXLand, final int cellYLand, final int spanX, final int spanY,
            boolean checkNoSpaceList) {
        Log.d(TAG, "modify item in db");
        String transaction = "DbDebug    Modify item (" + item.title + ") in db, id: " + item.id +
                " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY +
                ") --> " + "(" + container + ", " + screen + ", " + cellXPort + ", " + cellYPort + ")";
        //Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.cellXPort = cellXPort;
        item.cellYPort = cellYPort;
        item.cellXLand = cellXLand;
        item.cellYLand = cellYLand;
        item.spanX = spanX;
        item.spanY = spanY;

        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellXPort, cellYPort);
        }
        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container
        else if(context instanceof Launcher && screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
            //TODO: liuhao need mHideseat
            //item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        }
        /*YUNOS END*/
        else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLXPORT, item.cellXPort);
        values.put(LauncherSettings.Favorites.CELLYPORT, item.cellYPort);
        values.put(LauncherSettings.Favorites.CELLXLAND, item.cellXLand);
        values.put(LauncherSettings.Favorites.CELLYLAND, item.cellYLand);
        values.put(LauncherSettings.Favorites.SPANX, item.spanX);
        values.put(LauncherSettings.Favorites.SPANY, item.spanY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);
        /*YUNOS BEGIN LH handle installed app no space case*/
        values.put(LauncherSettings.Favorites.ITEM_TYPE, item.itemType);
        /*YUNOS END*/
        updateItemInDatabaseHelper(context, values, item, "modifyItemInDatabase", checkNoSpaceList);
    }

    public static void modifyItemInDatabase(Context context,
            final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY, final int spanX, final int spanY) {
        modifyItemInDatabase(context, item, container, screen, cellX, cellY, spanX, spanY, true);
    }

    //added by dongjun for appstore beginnn
    public static void updateItemInDatabaseForAppDownload(Context context,
            final ShortcutInfo item) {
        Log.d(TAG, "update item in data base for app download");
        final ContentValues values = new ContentValues();
        item.onAddToDatabase(values);
//        LauncherApplication application = (LauncherApplication)context.getApplicationContext();
        //BugID: 117502: download item's icon doesn't change when theme change
        Drawable origIcon = item.mIcon;
        if(origIcon!=null){
            ItemInfo.writeBitmap(values, origIcon);
        }
        updateItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }
    /* YUNOS BEGIN */
    // ##date:2014/06/16 ##author:yangshan.ys
    // batch operations to the icons in folder
    public static ArrayList<ItemInfo> getSbgWorkspaceItems() {
        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
        }
        return tmpWorkspaceItems;
        /* YUNOS END */
    }
    /* YUNOS BEGIN */
    // ##date:2015/03/16 ##author:sunchen.sc
    // Copy bg member to solve concurrent modification issue
    public static HashMap<Long, ItemInfo> getSBgItemsIdMap() {
        HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
        synchronized (sBgLock) {
            itemsIdMap.putAll(sBgItemsIdMap);
        }
        return itemsIdMap;
    }
    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2015/03/16 ##author:sunchen.sc
    // Copy bg member to solve concurrent modification issue
    public static ArrayList<ItemInfo> getAllAppItems() {
        final ArrayList<ItemInfo> allAppItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            allAppItems.addAll(sBgItemsIdMap.values());
        }
        return allAppItems;
    }
    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2015/8/31 ##author:zhanggong.zg ##BugID:6373023
    public interface ItemVisitor {
        boolean visitItem(ItemInfo item);
    }
    public static void traverseAllAppItems(ItemVisitor visitor) {
        synchronized (sBgLock) {
            for (ItemInfo item : sBgItemsIdMap.values()) {
                if (!visitor.visitItem(item)) {
                    break;
                }
            }
        }
    }
    /* YUNOS END */
    public static ArrayList<boolean[][]> getWorkspaceOccupied() {
        return sWorkspaceOccupiedCurrent;
    }

    public static void dumpPortArray(int screenCount) {
        int cellCountX = ConfigManager.getCellCountX(Configuration.ORIENTATION_PORTRAIT);
        int cellCountY = ConfigManager.getCellCountY(Configuration.ORIENTATION_PORTRAIT);
        Log.d(TAG, "sxsexe_pad    dumpPortArray start  cellCountX " + cellCountX + " cellCountY " + cellCountY);
        for (int i = 0; i < screenCount; i++) {
            for (int j = 0; j < cellCountX; j++) {
                for (int k = 0; k < cellCountY; k++) {
                    Log.d(TAG, "sxsexe_pad  dumpPortArray screen  " + i + " x " + j + " y " + k + sWorkspaceOccupiedPort.get(i)[j][k]);
                }
            }
        }
        Log.d(TAG, "sxsexe_pad    dumpPortArray  over");
    }

    public static void dumpLandArray(int screenCount) {
        int cellCountX = ConfigManager.getCellCountX(Configuration.ORIENTATION_LANDSCAPE);
        int cellCountY = ConfigManager.getCellCountY(Configuration.ORIENTATION_LANDSCAPE);
        Log.d(TAG, "sxsexe_pad    dumpLandArray start  cellCountX " + cellCountX + " cellCountY " + cellCountY);
        for (int i = 0; i < screenCount; i++) {
            for (int j = 0; j < cellCountX; j++) {
                for (int k = 0; k < cellCountY; k++) {
                    Log.d(TAG, "sxsexe_pad  dumpLandArray screen  " + i + " x " + j + " y " + k + sWorkspaceOccupiedLand.get(i)[j][k]);
                }
            }
        }
        Log.d(TAG, "sxsexe_pad     dumpLandArray  over");
    }

    public static void dumpWorkspaceOccupied(String mark) {
        int screenCount = mMaxIconScreenCount;
        int cellCountX = getCellCountX();
        int cellCountY = getCellCountY();
        Log.d(TAG, "dumpWorkspaceOccupied begin : " + mark);
        StringBuilder sb;
        for (int i = 0; i < screenCount; i++) {
            Log.d(TAG, "----- screen:" + i);
            for (int y = 0; y < cellCountY; y++) {
                sb = new StringBuilder();
                sb.append('-').append(y).append("- ");
                for (int x = 0; x < cellCountX; x++) {
                    sb.append('[');
                    if (sWorkspaceOccupiedCurrent.get(i)[x][y]) {
                        sb.append('X');
                    } else {
                        sb.append(' ');
                    }
                    sb.append(']');
                }
                sb.append(" --");
                Log.d(TAG, sb.toString());
            }
        }
        Log.d(TAG, "dumpWorkspaceOccupied end");
    }

    public static void dumpUIOccupied() {
        if (!DEBUG_OCCUPY)
            return;
        if (DEBUG_OCCUPY)
            Log.d(TAG, "dumpUIOccupied start");
        int screenCount = mMaxIconScreenCount;
        int cellCountX = getCellCountX();
        int cellCountY = getCellCountY();
        if (DEBUG_OCCUPY)
            Log.d(TAG,
                    "dumpUIOccupied() cellCountY:" + cellCountY + " cellCountX " + cellCountX + " isInLand "
                    + LauncherApplication.isInLandOrientation());
        StringBuilder sb;
        for (int i = 0; i < screenCount; i++) {
            Log.d(TAG, "dumpUIOccupied() sWorkspaceOccupiedCurrent screen:"+i);
            for (int y = 0; y < cellCountY; y++) {
                sb = new StringBuilder();
                sb.append('-').append(y).append("- ");
                for (int x = 0; x < cellCountX; x++) {
                    sb.append('[');
                    if (sWorkspaceOccupiedCurrent.get(i)[x][y]) {
                        sb.append('X'); 
                    } else {
                        sb.append(' ');
                    }
                    sb.append(']');
                }
                sb.append(" --");
                Log.d(TAG, sb.toString());
            }
        }
        for (int i = 0; i < screenCount; i++) {
            Log.d(TAG, "dumpUIOccupied() sWorkspaceOccupiedNonCurrent screen:"+i);
            for (int y = 0; y < cellCountY; y++) {
                sb = new StringBuilder();
                sb.append('-').append(y).append("- ");
                for (int x = 0; x < cellCountX; x++) {
                    sb.append('[');
                    if (sWorkspaceOccupiedCurrent.get(i)[x][y]) {
                        sb.append('X'); 
                    } else {
                        sb.append(' ');
                    }
                    sb.append(']');
                }
                sb.append(" --");
                Log.d(TAG, sb.toString());
            }
        }
        int hideseatScreenCount = ConfigManager.getHideseatScreenMaxCount();
        int hideseatCountX = ConfigManager.getHideseatMaxCountX();
        int hideseatCountY = ConfigManager.getHideseatMaxCountY();
        if (DEBUG_OCCUPY)
            Log.d(TAG, "dumpUIOccupied() hideseatScreenCount:" + hideseatScreenCount);
        for (int i = 0; i < hideseatScreenCount; i++) {
            Log.d(TAG, "dumpUIOccupied() Hideseat screen:"+i);
            for (int y = 0; y < hideseatCountY; y++) {
                sb = new StringBuilder();
                sb.append('-').append(y).append("- ");
                for (int x = 0; x < hideseatCountX; x++) {
                    sb.append('[');
                    if (sHideseatOccupied.get(i)[x][y]) {
                        sb.append('X'); 
                    } else {
                        sb.append(' ');
                    }
                    sb.append(']');
                }
                sb.append(" --");
                Log.d(TAG, sb.toString());
            }
        }
        if (DEBUG_OCCUPY)
            Log.d(TAG, "dumpUIOccupied end");
    }

    public static void dumpCellOccupied(int screen) {
        if (screen >= ConfigManager.getIconScreenMaxCount()) {
            return;
        }
        int cellCountX = getCellCountX();
        int cellCountY = getCellCountY();
        for (int j = 0; j < cellCountX; j++) {
            for (int k = 0; k < cellCountY; k++) {
                if (DEBUG_OCCUPY)
                    Log.d(TAG, "Workspace screen:" + screen + ", x:" + j + ", y:" + k);
                if (DEBUG_OCCUPY)
                    Log.d(TAG, "Workspace occupy: " + (sWorkspaceOccupiedCurrent.get(screen))[j][k]);
            }
        }
    }

    public static void dumpCellOccupied(boolean[][] occupied) {
        int cellCountX = getCellCountX();
        int cellCountY = getCellCountY();
        for (int j = 0; j < cellCountX; j++) {
            for (int k = 0; k < cellCountY; k++) {
                if (DEBUG_OCCUPY)
                    Log.d(TAG, "Celllayout j:" + j + ", y:" + k + ", occupy:" +
                            occupied[j][k]);
            }
        }
    }

    public static void setWorkspaceOccupied(int screen, boolean[][] occupied) {
        if (screen >= 0 && screen < sWorkspaceOccupiedCurrent.size()) {
            sWorkspaceOccupiedCurrent.set(screen, occupied);
        }
    }

    /* YUNOS BEGIN */
    // ## date: 2016/06/03 ## author: yongxing.lyx
    // ## BugID:8364282:icon overlap after exchange screen and fling icon.
    public static void addWorkspaceOccupied(int screen, boolean[][] occupied) {
        if (screen >= 0 && screen < sWorkspaceOccupiedCurrent.size()) {
            sWorkspaceOccupiedCurrent.add(screen, occupied);
            sWorkspaceOccupiedCurrent.remove(sWorkspaceOccupiedCurrent.size() - 1);
        }
    }
    /* YUNOS END */

    private void initWorkspaceOccupiedNonCurrent() {
        boolean isInLand = LauncherApplication.isInLandOrientation();
        ArrayList<boolean[][]> nonCurrentOccupied = sWorkspaceOccupiedNonCurrent;
        int screen;
        int x;
        int y;
        int spanX;
        int spanY;
        for (boolean[][] ocp : nonCurrentOccupied) {
            Log.d(TAG, "liuhao ocp length is " + ocp.length);
            for (int i = 0; i < ocp.length; i++) {
                boolean[] sub = ocp[i];
                for (int j = 0; j < sub.length; j++) {
                    sub[j] = false;
                }
            }
        }
        /* YUNOS BEGIN */
        // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5681074
        // fix concurrent modification exception
        List<ItemInfo> occupyList;
        synchronized (sBgLock) {
            occupyList = new ArrayList<ItemInfo>(sBgWorkspaceItems.size() + sBgAppWidgets.size());
            occupyList.addAll(sBgWorkspaceItems);
            occupyList.addAll(sBgAppWidgets);
        }
        /* YUNOS END */

        for (ItemInfo info : occupyList) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screen = info.screen;
                /* YUNOS BEGIN */
                // ##date:2013/12/17 ##author:wei.sun ##bugid: 75485
                // can not avoid invalid screen absolutely by now.
                if (!(screen > -1 && screen < mMaxIconScreenCount)) {
                    continue;
                }
                /* YUNOS END */

                x = isInLand ? info.cellXPort : info.cellXLand;
                y = isInLand ? info.cellYPort : info.cellYLand;
                spanX = info.spanX;
                spanY = info.spanY;

                /* YUNOS BEGIN */
                // ##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 78360
                // check item's position is reasonable
                int oppisiteOrientation = isInLand ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
                if ((x >= ConfigManager.getCellCountX(oppisiteOrientation)) || (y >= ConfigManager.getCellCountY(oppisiteOrientation))
                        || (x + spanX > ConfigManager.getCellCountX(oppisiteOrientation))
                        || (y + spanY > ConfigManager.getCellCountY(oppisiteOrientation)) || (x < 0)
                        || (y < 0)) {
                    Log.d(TAG, "initOccupied item position error " + info.id);
                    continue;
                }
                /* YUNOS END */

                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        nonCurrentOccupied.get(screen)[x + i][y + j] = true;
                    }
                }
            }
        }
        occupyList.clear();
    }

    public static void markCellsOccupiedInNonCurrent(ItemInfo info, boolean value)
    {
        if(isInvalidPosition(info) || info == null) {
            return;
        }
        boolean island = LauncherApplication.isInLandOrientation();
        if (island) {
            if ((info.cellXPort >= 0) && (info.cellYPort >= 0)) {
                for (int i = 0; i < info.spanX; i++) {
                    for (int j = 0; j < info.spanY; j++) {
                        sWorkspaceOccupiedNonCurrent.get(info.screen)[info.cellXPort + i][info.cellYPort + j] = value;
                    }
                }
            }
        } else {
            if ((info.cellXLand >= 0) && (info.cellYLand >= 0)) {
                for (int i = 0; i < info.spanX; i++) {
                    for (int j = 0; j < info.spanY; j++) {
                        sWorkspaceOccupiedNonCurrent.get(info.screen)[info.cellXLand + i][info.cellYLand + j] = value;
                    }
                }
            }
        }
    }

    public static void removeWorkspaceOccupied(boolean[][] occupied) {
        int index = sWorkspaceOccupiedCurrent.indexOf(occupied);
        if (index >= 0 && index < sWorkspaceOccupiedCurrent.size()) {
            boolean[][] emptyScreen = new boolean[sCellCountX][sCellCountY];
            sWorkspaceOccupiedCurrent.remove(index);
            sWorkspaceOccupiedCurrent.add(emptyScreen);
            if (ConfigManager.isLandOrienSupport()) {
                boolean[][] emptyScreenNonCurrent = new boolean[sCellCountY][sCellCountX];
                sWorkspaceOccupiedNonCurrent.remove(index);
                sWorkspaceOccupiedNonCurrent.add(emptyScreenNonCurrent);
            }
        }
    }
    public static void setHideseatOccupied(int screen, boolean[][] occupied) {
        if (screen >= 0 && screen < sHideseatOccupied.size()) {
            sHideseatOccupied.set(screen, occupied);
        }
    }
    public static void removeHideseatOccupied(int screen) {
        int cellCountX = ConfigManager.getHideseatMaxCountX();
        int cellCountY = ConfigManager.getHideseatMaxCountY();
        boolean[][] tmpOccupied = new boolean[cellCountX][cellCountY];
        if (DEBUG_OCCUPY)
            Log.d(TAG, "removeHideseatOccupied() indexOf(occupied) = " + screen);
        if (screen >= 0 && screen < sHideseatOccupied.size()) {
            sHideseatOccupied.set(screen, tmpOccupied);
        }
    }

    public static void releaseWorkspacePlace(int s, int x, int y) {
        checkRunOnUIThread();
        (sWorkspaceOccupiedCurrent.get(s))[x][y] = false;
    }

    public static void releaseWorkspacePlace(int s, int xPort, int yPort, int xLand, int yLand) {
        checkRunOnUIThread();
        (sWorkspaceOccupiedPort.get(s))[xPort][yPort] = false;
        (sWorkspaceOccupiedLand.get(s))[xLand][yLand] = false;
    }

    public static void releaseHideseatPlace(int s, int x, int y) {
        (sHideseatOccupied.get(s))[x][y] = false;
    }
    private static void reCreateUiOccupied(int cellCountX, int cellCountY) {
        int screenCount = ConfigManager.getIconScreenMaxCount();
        int hideseatScreenCount = ConfigManager.getHideseatScreenMaxCount();
        int hideseatCountX = ConfigManager.getHideseatMaxCountX();
        int hideseatCountY = ConfigManager.getHideseatMaxCountY();
        sWorkspaceOccupiedLand = new ArrayList<boolean[][]>(screenCount);
        sWorkspaceOccupiedPort = new ArrayList<boolean[][]>(screenCount);
        for (int i = 0; i < screenCount; i++) {
            boolean[][] celllayoutLand = new boolean[cellCountY][cellCountX];
            sWorkspaceOccupiedLand.add(celllayoutLand);
            boolean[][] celllayoutPort = new boolean[cellCountX][cellCountY];
            sWorkspaceOccupiedPort.add(celllayoutPort);
        }
        sWorkspaceOccupiedCurrent = LauncherApplication.isInLandOrientation() ? sWorkspaceOccupiedLand : sWorkspaceOccupiedPort;
        sWorkspaceOccupiedNonCurrent = LauncherApplication.isInLandOrientation() ? sWorkspaceOccupiedPort : sWorkspaceOccupiedLand;
        sHideseatOccupied = new ArrayList<boolean[][]>(hideseatScreenCount);
        for (int i = 0; i < hideseatScreenCount; i++) {
            boolean[][] celllayout = new boolean[hideseatCountX][hideseatCountY];
            sHideseatOccupied.add(celllayout);
        }
    }

    public static void assignPlace(int s, int x, int y, int spanX, int spanY, boolean value, CellLayout.Mode container) {
        int screen = s;
        // In folder
        if (screen < 0 || screen >= ConfigManager.getIconScreenMaxCount() || x < 0 || y < 0) {
            return;
        }
        int cellX = x;
        int cellY = y;
        int cellCountX = getCellCountX();
        int cellCountY = getCellCountY();
        int hideseatCountX = ConfigManager.getHideseatMaxCountX();
        int hideseatCountY = ConfigManager.getHideseatMaxCountY();

        if (container == CellLayout.Mode.HIDESEAT) {
            for (int i = cellX; i < cellX + spanX && i < hideseatCountX; i++) {
                for (int j = cellY; j < cellY + spanY && j < hideseatCountY; j++) {
                    (sHideseatOccupied.get(screen))[i][j] = value;
                    if (DEBUG_OCCUPY)
                        Log.d(TAG, "assignPlace Hideseat s " + s + ", x = " + x + ", y = " + ", spanX = " +
                        spanX + ", spanY = " + spanY);
                }
            }
        } else if (container == CellLayout.Mode.NORMAL) {
            for (int i = cellX; i < cellX + spanX && i < cellCountX; i++) {
                for (int j = cellY; j < cellY + spanY && j < cellCountY; j++) {
                    (sWorkspaceOccupiedCurrent.get(screen))[i][j] = value;
                    if (DEBUG_OCCUPY)
                        Log.d(TAG, "assignPlace Workspace s " + s + ", x = " + x + ", y = " + y  +", spanX = " +
                        spanX + ", spanY = " + spanY);
                }
            }
        }
    }

    private static ArrayList<FolderInfo> getAllFolderItems() {
        final ArrayList<FolderInfo> allFolderItems = new ArrayList<FolderInfo>();
        synchronized (sBgLock) {
            allFolderItems.addAll(sBgFolders.values());
        }
        return allFolderItems;
    }
    public static HashMap<Long, FolderInfo> getSBgFolders() {
        HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
        synchronized (sBgLock) {
            folders.putAll(sBgFolders);
        }
        return folders;
    }
    //find emptycells which the amount is reqCount , by default spanX=1,spanY=1
    public static List<ScreenPosition> findEmptyCellsAndOccupy(int reqCount) {
        checkRunOnUIThread();
        Log.d(TAG,"findEmptyCells in");
        if(reqCount == 0 ) {
            return null;
        }
        List<ScreenPosition> posList = new ArrayList<ScreenPosition>();

        int count = 0;
        int scr = ConfigManager.DEFAULT_FIND_EMPTY_SCREEN_START;
        /* YUNOS BEGIN */
        // ##module:homeshell ##BugID:7913954
        // ##date:2016/02/25 ##author:xiangnan.xn@alibaba-inc.com
        // find empty place until there is no valid position in current screen
        // then move to next screen
        while (count < reqCount && scr < mMaxIconScreenCount) {
            ScreenPosition pos = null;
            if (ConfigManager.isLandOrienSupport()) {
                pos = getScreenPositionLandSupport(scr, 1, 1);
            } else {
                pos = getScreenPositionNoLandSupport(scr, 1, 1);
            }
            // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
            // Lock occupy the empty space, if the empty place has been
            // occupied, continue find empty space
            if (pos != null) {
                posList.add(pos);
                count++;
            } else {
                // no valid position in current screen, move to next
                scr++;
            }
        }
        /* YUNOS BEGIN */

        Log.d(TAG,"findEmptyCells end");
        dumpUIOccupied();
        return posList;
    }
    /* YUNOS END */
    //added by dongjun for appstore end
    /**
     * Update an item to the database in a specified container.
     */
    public static void updateItemInDatabase(Context context, final ItemInfo item) {
        Log.d(TAG, "update item in data base");
        final ContentValues values = new ContentValues();
        final Context finalContext = context;

        //BugID:5215861:anr in updateItemsInDatabase
        //run in worker thread to avoid block UI thread
        Runnable r = new Runnable() {
            @Override
            public void run() {
                item.onAddToDatabase(values);

                /*YUNOS BEGIN*/
                //##date:2014/01/06 ##author:hao.liuhaolh ##BugID: 82849
                // the app icon store in database should be original icon
                /*save the original icon to database if it is a sd app*/
                /* YUNOS BEGIN */
                // ##date:2014/07/25 ##author:hongchao.ghc ##BugID: 140209
                if (item instanceof ShortcutInfo) {
                    switch (item.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION: {
                            if (((ShortcutInfo) item).isSDApp == 1) {
                                // BugID:122827:sdcard app icon wrong on theme
                                // change when sdcard unmount
                                Drawable origIcon = IconUtils.getAppOriginalIcon(finalContext,
                                        (ShortcutInfo) item);
                                if (origIcon != null) {
                                    ItemInfo.writeBitmap(values, origIcon);
                                } else {
                                    if (values.containsKey(LauncherSettings.Favorites.ICON)) {
                                        values.remove(LauncherSettings.Favorites.ICON);
                                    }
                                }
                            } else {
                                values.put(LauncherSettings.Favorites.ICON, new byte[0]);
                            }
                            values.put(LauncherSettings.Favorites.USER_ID,
                                    ((ShortcutInfo) item).userId);
                        }
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_BOOKMARK:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
// remove vp install
//                    case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                    {
                        if (values.containsKey(LauncherSettings.Favorites.ICON)) {
                            values.remove(LauncherSettings.Favorites.ICON);
                        }
                    }
                        break;
                    default:
                        break;
                    }
                    /* YUNOS END */
                }
                /*YUNOS END*/

                values.put(LauncherSettings.Favorites.CONTAINER, item.container);
                values.put(LauncherSettings.Favorites.SCREEN, item.screen);
                values.put(LauncherSettings.Favorites.CELLX, item.cellX);
                values.put(LauncherSettings.Favorites.CELLY, item.cellY);
                /* YUNOS BEGIN */
                // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
                // Modified to support pad orientation
                values.put(LauncherSettings.Favorites.CELLXPORT, item.cellXPort);
                values.put(LauncherSettings.Favorites.CELLYPORT, item.cellYPort);
                values.put(LauncherSettings.Favorites.CELLXLAND, item.cellXLand);
                values.put(LauncherSettings.Favorites.CELLYLAND, item.cellYLand);
                /* YUNOS END */
                updateItemInDatabaseHelper(finalContext, values, item, "updateItemInDatabase");
            }
        };
        runOnWorkerThread(r);
    }

    static boolean isIntentDataSame(Intent intentA, Intent intentB) {
        if ((intentA == null) || (intentB == null)) {
            return false;
        }
        if ((intentA.getData() == null) && (intentB.getData() == null)) {
            Log.d(TAG, "data all null, return true");
            return true;
        }
        if ((intentA.getData() != null) &&
            (intentB.getData() != null) &&
            (intentA.getData().toString().equals(intentB.getData().toString()))) {
            Log.d(TAG, "data to string same, return true");
            return true;
        }
        Log.d(TAG, "data not same, return false");
        return false;
    }

    static boolean isIntentExtraSame (Intent intentA, Intent intentB) {
        if ((intentA == null) || (intentB == null)) {
            return false;
        }
        if ((intentA.getExtras() == null) && (intentB.getExtras() == null)) {
            Log.d(TAG, "extra all null, return true");
            return true;
        }
        if (intentA.toUri(0) != null) {
            Log.d(TAG, "intent A toUri is " + intentA.toUri(0).toString());
        }
        if (intentB.toUri(0) != null) {
            Log.d(TAG, "intent B toUri is " + intentB.toUri(0).toString());
        }
        if (intentA.getExtras() != null) {
            Log.d(TAG, "intentA extra is " + intentA.getExtras().toString());
        } else {
             Log.d(TAG, "intentA extra is null");
        }
        if (intentB.getExtras() != null) {
            Log.d(TAG, "intentB extra is " + intentB.getExtras().toString());
        } else {
             Log.d(TAG, "intentB extra is null");
        }

        if ((intentA.getExtras() != null) &&
            (intentB.getExtras() != null) &&
            (intentA.getExtras().toString().equals(intentB.getExtras().toString()))) {
            Log.d(TAG, "extra to string same, return true");
            return true;
        }

        Log.d(TAG, "extra not same return false");
        return false;
    }

    /**
     * Returns true if the shortcuts already exists in the database.
     * we identify a shortcut by its title and intent.
     */
    static long shortcutExists(Context context, String title, Intent intent, int userId) {
        long result = -1;
        Log.d(TAG, "shortcutExists in");
        if (intent == null) {
            return result;
        }
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for(ItemInfo info: allApps) {
            //BugID:144486:shortcut verify error.
            if((info instanceof ShortcutInfo) &&
                (info.itemType == Favorites.ITEM_TYPE_APPLICATION ||
                info.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION ||
                info.itemType == Favorites.ITEM_TYPE_BOOKMARK ||
                info.itemType == Favorites.ITEM_TYPE_SHORTCUT)){
                Intent itemIntent = ((ShortcutInfo)info).intent;
                if (((ShortcutInfo)info).userId != userId) {
                    continue;
                }
                if ((itemIntent != null) &&
                   (itemIntent.getComponent() != null)) {
                   if ((intent.getComponent() != null)) {
                       //BugID:137833:add support for com.android.launcher.action.INSTALL_SHORTCUT
                       //if (intent.getComponent().getPackageName().equals(((ShortcutInfo)info).intent.getComponent().getPackageName())) {
                        if (intent.getComponent().toString().equals(itemIntent.getComponent().toString())) {
                            Log.d(TAG, "same component, item id is " + info.id);
                            if ((isIntentDataSame(intent, itemIntent) == true) &&
                                (isIntentExtraSame(intent, itemIntent) == true)) {
                                result = info.id;
                                break;
                            }
                            //BugID:5609708:filter out the same title shortcut
                            if (info.itemType == Favorites.ITEM_TYPE_APPLICATION ||
                                info.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION){
                                if ((info.title != null) && (title != null) &&
                                    (info.title.equals(title))) {
                                    result = info.id;
                                    break;
                                }
                            }
                        }
                        //to filter out the shortcut that has same package name and title as app's
                        else if (info.itemType == Favorites.ITEM_TYPE_APPLICATION ||
                                info.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                            String itemPkgName = itemIntent.getComponent().getPackageName();
                            String shortcutPkgName = intent.getComponent().getPackageName();
                            if ((itemPkgName != null) && (shortcutPkgName != null) &&
                                (itemPkgName.equals(shortcutPkgName))) {
                                if ((info.title != null) && (title != null) &&
                                    (info.title.equals(title))) {
                                    result = info.id;
                                    break;
                                }
                            }
                        }
                   } else {
                       if (itemIntent.toUri(0).equals(intent.toUri(0))) {
                           result = info.id;
                           break;
                       }
                   }
               } else if (itemIntent != null) {
                   if (itemIntent.toUri(0).equals(intent.toUri(0))) {
                       Log.d(TAG, "find same intent");
                       result = info.id;
                       break;
                   }
               } else {
                   Log.d(TAG, "info intent is null");
               }
           }
        }
        Log.d(TAG, "shortcutExists out");
        return result;
    }

    /**
     * Returns an ItemInfo array containing all the items in the LauncherModel.
     * The ItemInfo.id is not set through this function.
     */
    public static ArrayList<ItemInfo> getItemsInLocalCoordinates(Context context) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[] {
                LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.CONTAINER,
                LauncherSettings.Favorites.SCREEN, LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY,
                LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY }, null, null, null);

        final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
        final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);

        try {
            while (c.moveToNext()) {
                ItemInfo item = new ItemInfo();
                item.cellX = c.getInt(cellXIndex);
                item.cellY = c.getInt(cellYIndex);
                item.spanX = c.getInt(spanXIndex);
                item.spanY = c.getInt(spanYIndex);
                item.container = c.getInt(containerIndex);
                item.itemType = c.getInt(itemTypeIndex);
                item.screen = c.getInt(screenIndex);

                items.add(item);
            }
        } catch (Exception e) {
            items.clear();
        } finally {
            c.close();
        }

        return items;
    }

    /**
     * Find a folder in the db, creating the FolderInfo if necessary, and adding it to folderList.
     */
    public FolderInfo getFolderById(Context context,
            HashMap<Long, FolderInfo> folderList, long id) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
                "_id=? and (itemType=? or itemType=?)",
                new String[] { String.valueOf(id),
                        String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)}, null);

        try {
            if (c.moveToFirst()) {
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        folderInfo = findOrMakeFolder(folderList, id);
                        break;
                }
                /* YUNOS BEGIN */
                // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
                if (folderInfo != null) {
                    folderInfo.title = c.getString(titleIndex);
                    folderInfo.id = id;
                    folderInfo.container = c.getInt(containerIndex);
                    folderInfo.screen = c.getInt(screenIndex);
                    folderInfo.cellX = c.getInt(cellXIndex);
                    folderInfo.cellY = c.getInt(cellYIndex);
                }
                /* YUNOS END */
                return folderInfo;
            }
        } finally {
            c.close();
        }

        return null;
    }

    public static void addFolderToDatabase(Context context, final ItemInfo item, final long container,
            final int screen, final int cellX, final int cellY, final boolean notify, ShortcutInfo destInfo) {
        if (ConfigManager.isLandOrienSupport()) {
            boolean isLand = LauncherApplication.isInLandOrientation();
            if (destInfo != null) {
                item.cellX = cellX;
                item.cellY = cellY;
                if (isLand) {
                    item.cellXPort = destInfo.cellXPort;
                    item.cellYPort = destInfo.cellYPort;
                    item.cellXLand = cellX;
                    item.cellYLand = cellY;
                } else {
                    item.cellXLand = destInfo.cellXLand;
                    item.cellYLand = destInfo.cellYLand;
                    item.cellXPort = cellX;
                    item.cellYPort = cellY;
                }
                LauncherModel.addItemToDatabase(context, item, container, screen,
                        item.cellXPort, item.cellYPort, item.cellXLand, item.cellYLand, false);
            } else {
                ScreenPosition sp = findEmptyCellAndOccupy(screen, item.spanX, item.spanY, !isLand);
                if (sp != null) {
                    Log.d(TAG, "sxsexe_pad  addFolderToDatabase sp  " + sp);
                    if (isLand) {
                        item.cellXPort = sp.xPort;
                        item.cellYPort = sp.yPort;
                    } else {
                        item.cellXLand = sp.xLand;
                        item.cellYLand = sp.yLand;
                    }
                    addItemToDatabase(context, item, container, screen, cellX, cellY, false);
                } else {
                    Log.e(TAG, "sxsexe_pad  addFolderToDatabase failed to find empty cell in screen " + item.screen);
                }
            }
        } else {
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        }
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    //for pad land mode. temp function
    public static void addItemToDatabase(Context context, final ItemInfo item, final long container,
                                                                   final int screen, final int cellX, final int cellY, final boolean notify) {
        item.cellX = cellX;
        item.cellY = cellY;
        item.setCurrentOrientationXY(cellX, cellY);
        Log.d(TAG, "sxsexe_test   addItemToDatabase item  " + item);
        addItemToDatabaseDownload(context, item, container, screen, item.cellXPort, item.cellYPort, item.cellXLand, item.cellYLand, notify, null);
    }

    public static void addItemToDatabase(Context context, final ItemInfo item, final long container, final int screen, final int cellXPort,
            final int cellYPort, final int cellXLand, final int cellYLand, final boolean notify) {
        addItemToDatabaseDownload(context, item, container, screen, item.cellXPort, item.cellYPort, item.cellXLand, item.cellYLand, notify, null);
    }

    private static boolean isItemExist(final ItemInfo item) {
        String pkgName = null;
        Collection<ItemInfo> apps = getAllAppItems();
        // ---- need all items include folders ----
        if (item instanceof ShortcutInfo) {
            Intent it = ((ShortcutInfo) item).intent;
            if (it != null) {
                // for downloading type, get package name then
                // create component name with
                // predifend class name
                if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                    pkgName = it.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                } else {
                    ComponentName component = it.getComponent();
                    if (component == null) {
                        Log.d(TAG, "isItemExist : component == null");
                        return false;
                    }
                    pkgName = component.getPackageName();
                }
            }
        }
        for (ItemInfo itemExist : apps) {
            if (itemExist instanceof ShortcutInfo) {
                Intent itExist = ((ShortcutInfo) itemExist).intent;
                if (itExist != null) {
                    // for downloading type, get package name then
                    // create component name with
                    // predifend class name
                    String pkgNameExist = null;
                    if (itemExist.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                        pkgNameExist = itExist.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                    } else {
                        ComponentName component = itExist.getComponent();
                        pkgNameExist = component == null ? null : component.getPackageName();
                    }
                    Log.d(TAG, "pkgNameExist = " + ((pkgNameExist == null) ? "null" : pkgNameExist) + ", pkgName = " + pkgName +", userId:"+((ShortcutInfo) itemExist).userId);
                    if (pkgNameExist != null
                            && pkgNameExist.equals(pkgName)
                            && ((ShortcutInfo) itemExist).userId == ((ShortcutInfo) item).userId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static void addItemToDatabaseDownload(Context context, final ItemInfo item,
            final long container, final int screen, final int cellXPort, final int cellYPort, final int cellXLand, final int cellYLand,
            final boolean notify, final Runnable runnableUI) {
        Log.d(TAG, "sxsexe_pad    addItemToDatabaseDownload item " + item + " cellXPort:" + cellXPort + " cellYPort:" + cellYPort
                + "cellXLand  " + cellXLand + " cellYLand " + cellYLand + " item " + item);

        item.container = container;
        //for pad land mode
        //item.cellX = cellX;     //cellX and cellY should be already modified
        //item.cellY = cellY;
        item.cellXPort = cellXPort;
        item.cellYPort = cellYPort;
        item.cellXLand = cellXLand;
        item.cellYLand = cellYLand;
        // We store hotseat items in canonical form which is this orientation invariant position
        // in the hotseat
        if (context instanceof Launcher && screen < 0 &&
                container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellXPort, cellYPort);
        }
        /* YUNOS BEGIN */
        // ##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        // add hide icon container
        else if (context instanceof Launcher && screen < 0
                && container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
            // TODO: liuhao need mHideseat
            // item.screen = ((Launcher)
            // context).getHotseat().getOrderInHotseat(cellX, cellY);
        }
        /* YUNOS END */
        else {
            item.screen = screen;
        }

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(values);

        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        item.id = app.getLauncherProvider().generateNewId();
        values.put(LauncherSettings.Favorites._ID, item.id);

        /* YUNOS BEGIN */
        // ##date:2014/01/06 ##author:hao.liuhaolh ##BugID: 82849
        // the app icon store in database should be original icon
        /* save the original icon to database if it is a sd app */
        if ((item instanceof ShortcutInfo)
                && (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)) {
            if (((ShortcutInfo) item).isSDApp == 1) {
                // BugID:122827:sdcard app icon wrong on theme change when
                // sdcard unmount
                Drawable origIcon = IconUtils.getAppOriginalIcon(context, (ShortcutInfo) item);
                if (origIcon != null) {
                    ItemInfo.writeBitmap(values, origIcon);
                } else {
                    if (values.containsKey(LauncherSettings.Favorites.ICON)) {
                        values.remove(LauncherSettings.Favorites.ICON);
                    }
                }
            } else {
                values.put(LauncherSettings.Favorites.ICON, new byte[0]);
            }
            values.put(LauncherSettings.Favorites.USER_ID, ((ShortcutInfo) item).userId);
        }
        /* YUNOS END */
        /* YUNOS BEGIN */
        // ##gadget
        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com ##BugID:96378
        if (item instanceof GadgetItemInfo) {
            values.put(LauncherSettings.Favorites.TITLE, ((GadgetItemInfo) item).title.toString());
        }
        /* YUNOS END */

        Runnable r = new Runnable() {
            public void run() {
                if (runnableUI != null && isItemExist(item)) {
                    return;
                }
                String transaction = "DbDebug    Add item (" + item.title + ") to db, id: "
                        + item.id + " (" + container + ", " + screen + ", " + cellXPort + ", "
                        + cellYPort + ")";
                //Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI
                        : LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    checkItemInfoLocked(item.id, item, null);
                    sBgItemsIdMap.put(item.id, item);
                    sBgNoSpaceItems.remove(item);
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.put(item.id, (FolderInfo) item);
                            // Fall through
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                        case LauncherSettings.Favorites.ITEM_TYPE_ALIAPPWIDGET:
                            /* YUNOS BEGIN */
                            // ##date:2014/02/19 ##author:hao.liuhaolh
                            // ##BugID:92481
                            // vp install
// remove vp install
//                        case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                            /* YUNOS END */
                            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                                    || item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                                    ||
                                    /* YUNOS BEGIN */
                                    // ##date:2014/01/17 ##author:hao.liuhaolh
                                    // ##BugID:
                                    // add hide icon container
                                    item.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                                /* YUNOS END */
                                if (sBgWorkspaceItems.contains(item) == false) {
                                    sBgWorkspaceItems.add(item);
                                }
                            } else {
                                if (!sBgFolders.containsKey(item.container)) {
                                    // Adding an item to a folder that doesn't
                                    // exist.
                                    String msg = "adding item: " + item + " to a folder that "
                                            + " doesn't exist";
                                    Log.e(TAG, msg);
                                    Launcher.dumpDebugLogsToConsole();
                                } else {
                                    /* YUNOS BEGIN */
                                    // ##date:2013/12/06 ##author:hao.liuhaolh
                                    // ##BugID:70889
                                    // The items in folder position error after
                                    // restore
                                    Log.d(TAG, "add to folder, item id is " + item.id);
                                    final FolderInfo finalFolder = sBgFolders.get(item.container);
                                    // 'Folder add' has UI operation, I have to
                                    // run belong code in UI
                                    Runnable UIrun = new Runnable() {
                                        public void run() {
                                            finalFolder.add((ShortcutInfo) item);
                                        }
                                    };
                                    /* YUNOS BEGIN */
                                    // ##date:2014/02/19 ##author:hao.liuhaolh
                                    // ##BugID:92481
                                    // vp install
                                    // To avoid same item is add in folder view,
                                    // check whether the the item is already in
                                    // folder
                                    if ((finalFolder != null) && (finalFolder.contents != null)
                                            && (finalFolder.contents.contains(item) == false)) {
                                        runOnMainThread(UIrun);
                                    }
                                    /* YUNOS END */
                                    /* YUNOS END */
                                }
                            }
                            break;
                        /* YUNOS BEGIN */
                        // ##gadget
                        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com
                        // ##BugID:96378
                        case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                            sBgWorkspaceItems.add(item);
                            sBgItemsIdMap.put(item.id, item);
                            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ADD_GADGET, item.title.toString());
                            updateScreenAllXYOnWidgetAdd(item);
                            break;
                        /* YUNOS END */
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            updateScreenAllXYOnWidgetAdd(item);
                            break;
                        /* YUNOS BEGIN LH handle installed app no space case */
                        case LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION:
                            sBgNoSpaceItems.add(item);
                            break;
                    /* YUNOS END */
                    }
                }

                if (runnableUI != null) {
                    runOnMainThread(runnableUI);
                }
                 sWorker.removeCallbacks(mCheckInvalidPosItemsRunnable);
                 sWorker.postDelayed(mCheckInvalidPosItemsRunnable, 3000);
            }
        };
        runOnWorkerThread(r);
    }
    /**
     * Creates a new unique child id, for a given cell span across all layouts.
     */
    public static int getCellLayoutChildId(
            long container, int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return (((int) container & 0xFF) << 24)
                | (screen & 0xFF) << 16 | (localCellX & 0xFF) << 8 | (localCellY & 0xFF);
    }

    public static int getCellCountX() {
        return sCellCountX;
    }

    public static int getCellCountY() {
        return sCellCountY;
    }

    public void resetDataOnConfigurationChanged() {
        sCellCountX = ConfigManager.getCellCountX();
        sCellCountY = ConfigManager.getCellCountY();
        sWorkspaceOccupiedCurrent = LauncherApplication.isInLandOrientation() ? sWorkspaceOccupiedLand : sWorkspaceOccupiedPort;
        sWorkspaceOccupiedNonCurrent = LauncherApplication.isInLandOrientation() ? sWorkspaceOccupiedPort : sWorkspaceOccupiedLand;
    }

    public static int getCellMaxCountX() {
        return ConfigManager.getCellMaxCountX();
    }

    public static int getCellMaxCountY() {
/*       if (((LauncherApplication)LauncherApplication.getContext()).getIconManager().supprtCardIcon()) {
           return ConfigManager.DEFAULT_CELL_MAX_COUNT_Y - 1;
       }*/

        return ConfigManager.getCellMaxCountY();
    }

    /**
     * Updates the model orientation helper to take into account the current layout dimensions
     * when performing local/canonical coordinate transformations.
     */
    public static void updateWorkspaceLayoutCells(int shortAxisCellCount,
            int longAxisCellCount) {
        sCellCountX = shortAxisCellCount;
        sCellCountY = longAxisCellCount;
    }
    //bugid: 5234133: remove big card resource when deleting the icon
    //if we don't delete the icon, the drawbale will not be released
    private static void removeCardBackground(Context context, final ItemInfo item) {
        LauncherApplication app = (LauncherApplication)context.getApplicationContext();
        if ((app == null) ||!(app.getIconManager().supprtCardIcon()) ||
                                (context == null) || (item == null)) {
            return;
        }

        // get component name firstly
        ComponentName component = null;
        String pkgName = null;
        if (item instanceof ShortcutInfo) {
            Intent it = ((ShortcutInfo)item).intent;
            if (it != null) {
                // for downloading type, get package name then create component name with
                // predifend class name
                if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
                   pkgName = it.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                   component = new ComponentName(pkgName, IconManager.DOWNLOAD_CLASS);
                } else {
                   component = it.getComponent() ;
                }
            }
        }
        if (component == null) {
            return ;
        }
        int count = 0;
        // if there is other shortcut sharing the same component, don't remove big icon
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for(ItemInfo info: allApps) {
            if(info instanceof ShortcutInfo) {
                Intent itemIntent = ((ShortcutInfo)info).intent;
                if (itemIntent != null && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
                   pkgName = itemIntent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                   ComponentName cmpnt = new ComponentName(pkgName, IconManager.DOWNLOAD_CLASS);
                   if ((cmpnt != null) && (component.toString().equals(cmpnt.toString()))) {
                       count ++;
                   }
                } else if ((itemIntent != null) && (itemIntent.getComponent() != null)) {
                    if (component.toString().equals(itemIntent.getComponent().toString())) {
                       count ++;
                    }
                }
                if (count > 0) {
                    break;
                }
            }
        }
        if (count < 1) {
            app.getIconManager().clearCardBackgroud(((ShortcutInfo) item).intent);
        }
    }
    /**
     * Removes the specified item from the database
     * @param context
     * @param item
     */
    public static void deleteItemFromDatabase(Context context,
            final ItemInfo item) {
        Log.d(TAG, "deleteItemFromDatabase");
//        final int screen = item.screen;
        final long container = item.container;
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(item.id, false);
        final Context contextFinal = context;

        synchronized (sBgLock) {
            sBgWorkspaceItems.remove(item);
        }

        Runnable r = new Runnable() {
            public void run() {
                String transaction = "DbDebug    Delete item (" + item.title + ") from db, id: "
                        + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX +
                        ", " + item.cellY + ")";
                //Launcher.sDumpLogs.add(transaction);
                Log.d(TAG, transaction);

                cr.delete(uriToDelete, null, null);

                /*YUNOS BEGIN*/
                //##module(HomeShell)
                //##date:2014/04/02 ##author:hao.liuhaolh@alibaba-inc.com##BugID:106212
                //download item delete in restore mode
                if ((BackupManager.getInstance().isInRestore()) &&
                    (item.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING)) {
                    if (((ShortcutInfo)item).intent != null) {
                        Log.d(TAG, "remove downloading package from restore list");
                        String packageName = ((ShortcutInfo)item).intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                        BackupManager.getInstance().addRestoreDoneApp(contextFinal, packageName);
                    }
                }
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##date:2014/03/20 ##author:hao.liuhaolh ##BugID:103304
                //user track in vp install
// remove vp install
//                if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
//                    String pkgName = ((ShortcutInfo)item).intent.getStringExtra(VPUtils.TYPE_PACKAGENAME);
//                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_VP_ITEM_DELETE , pkgName);
//                }
                /*YUNOS END*/

                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    switch (item.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            sBgFolders.remove(item.id);
                            for (ItemInfo info: sBgItemsIdMap.values()) {
                                if (info.container == item.id) {
                                    // We are deleting a folder which still contains items that
                                    // think they are contained by that folder.
                                    String msg = "deleting a folder (" + item + ") which still " +
                                            "contains items (" + info + ")";
                                    Log.e(TAG, msg);
                                    Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                        case LauncherSettings.Favorites.ITEM_TYPE_ALIAPPWIDGET:
// remove vp instal
//                        case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                        /* YUNOS BEGIN */
                        // ##gadget
                        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com
                        // ##BugID:96378
                        case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                        /* YUNOS END */
                            if (item != null && item.title != null)
                                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_REMOVE_GADGET, item.title.toString());
                            sBgWorkspaceItems.remove(item);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            sBgAppWidgets.remove((LauncherAppWidgetInfo) item);
                            break;
                        /*YUNOS BEGIN LH handle installed app no space case*/
                        case LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION:
                            sBgNoSpaceItems.remove(item);
                            break;
                        /*YUNOS END*/
                    }
                    sBgItemsIdMap.remove(item.id);
                    //bugid: 5234133: remove big card resource when deleting the icon
                    try {
                        removeCardBackground(contextFinal, item);
                    } catch (Exception e) {
                    }
                    /*YUNOS BEGIN*/
                    //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
                    // client yun icon lost after restore
                    //sBgDbIconCache.remove(item);
                    /*YUNOS END*/
                    /*YUNOS BEGIN*/

                    //BugID:5183000:items overlay after folder dismiss
                    //only workspace call checkEmptyScreen
                    /*
                    if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                        checkEmptyScreen(contextFinal, screen);
                    } else */
                    if (container >= 0) {
                        FolderInfo folder = sBgFolders.get(item.container);
                        if ((folder != null) && (folder.contents != null)){
                            folder.contents.remove(item);
                        }
                    }
                    /*YUNOS END*/
                    /*YUNOS BEGIN LH handle installed app no space case*/
                    //run checkNoSpaceList after delete complete
                    //to avoid item overlap
                    boolean canCheck = true;
                    if (item.container == Favorites.CONTAINER_DESKTOP) {
                        canCheck = !isScreenEmpty(item.screen);
                    }
                    if (canCheck == true) {
                        //BugID:5628070:ANR during mode change
                        postCheckNoSpaceList();
                    }
                    /*YUNOS END*/
                    /* YUNOS BEGIN */

                    // ##date:2015/9/6 ##author:zhanggong.zg ##BugID:6348948,6394268
                    // Smart app recommendation (to record app launch times and counts)
                    if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                        AppLaunchManager.getInstance().deleteItem(item);
                        NewInstallAppHelper.notifyAppUninstalled(contextFinal, item);
                    }
                    /* YUNOS END */
                }

                sWorker.removeCallbacks(mCheckInvalidPosItemsRunnable);
                sWorker.postDelayed(mCheckInvalidPosItemsRunnable, 3000);
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Removes the specified item from the another table in database . when
     * uninstall app in one mode,also need delete the app record from table of
     * another mode
     *
     * @param context
     * @param item
     */
    public static void deleteItemFromAnotherTable(Context context, final ItemInfo item) {
        if (!(item instanceof ShortcutInfo)) {
            return;
        }
        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            public void run() {
                long id = -1;
                final Cursor c = cr
                        .query(
                                LauncherProvider.getDbAgedModeState() ? LauncherSettings.Favorites.CONTENT_URI_NORMAL_MODE
                                        : LauncherSettings.Favorites.CONTENT_URI_AGED_MODE,
                                null, "intent=?", new String[] {
                                    ((ShortcutInfo) item).intent.toUri(0)
                                }, null);
                try {
                    if (c.moveToNext()) {
                        final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                        id = c.getLong(idIndex);
                    }
                } finally {
                    c.close();
                }
                if (id < 0) {
                    return;
                } else {
                    Uri uriToDelete = LauncherSettings.Favorites.getContentUriForAnotherTable(id,
                            false);
                    cr.delete(uriToDelete, null, null);
                }
            }
        };
        runOnWorkerThread(r);
    }

    /* YUNOS BEGIN */
    // ## date: 2016/07/06 ## author: yongxing.lyx
    // ## BugID:8448699:can't remove shortcut in agemode which subscribed in
    // normal mode.
    public static void deleteShortcutFromAnotherTableByTitle(Context context, final String title) {

        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            public void run() {
                long id = -1;
                Uri table = LauncherProvider.getDbAgedModeState() ? LauncherSettings.Favorites.CONTENT_URI_NORMAL_MODE
                        : LauncherSettings.Favorites.CONTENT_URI_AGED_MODE;
                int count = cr.delete(table, "title=? AND itemType=?", new String[] {
                        title, Integer.toString(Favorites.ITEM_TYPE_SHORTCUT)
                });
                Log.i(TAG, "deleteShortcutFromAnotherTableByTitle : " + title + " count:" + count);
            }
        };
        runOnWorkerThread(r);
    }
    /* YUNOS END */

    /*YUNOS BEGIN LH handle installed app no space case*/
    static void checkNoSpaceList() {
        Log.d(TAG, "sBgNoSpaceItems size is " + sBgNoSpaceItems.size());
        if (sBgNoSpaceItems.size() == 0) {
            return;
        }
        /* YUNOS BEGIN */
        // ##date:2015/3/24 ##author:sunchen.sc ##BugID:5735130
        // Move find empty place to UI thread
        Runnable runOnUI1 = new Runnable() {
            @Override
            public void run() {
                final
                List<ScreenPosition> postions = findEmptyCellsAndOccupy(sBgNoSpaceItems.size());
                // while (postions.size() != 0 && sBgNoSpaceItems.size() != 0) {
                if (postions == null || (postions != null && postions.size() == 0)) {
                    return;
                }
                Runnable runOnWork2 = new Runnable() {

                    @Override
                    public void run() {
                        while (sBgNoSpaceItems.size() != 0 && postions.size() != 0) {
                            ScreenPosition p = postions.remove(0);// findEmptyCell();
                            // if (p == null) {
                            // break;
                            // }
                            ItemInfo item = null;
                            synchronized (sBgLock) {
                                if (!sBgNoSpaceItems.isEmpty()) {
                                    item = sBgNoSpaceItems.get(0);
                                } else {
                                    return;
                                }
                                sBgNoSpaceItems.remove(item);
                                if (!sBgWorkspaceItems.contains(item)) {
                                    sBgWorkspaceItems.add(item);
                                }
                            }
                            item.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                            item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                            if (LauncherApplication.isInLandOrientation()) {
                                item.cellX = p.xLand;
                                item.cellY = p.yLand;
                            } else {
                                item.cellX = p.xPort;
                                item.cellY = p.yPort;
                            }
                            modifyItemInDatabase(LauncherApplication.getContext(), item,
                                    LauncherSettings.Favorites.CONTAINER_DESKTOP,
                                    p.s, p.xPort, p.yPort, p.xLand, p.yLand,
                                    item.spanX, item.spanY, false);

                            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                            final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
                            infos.add(item);
                            final Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    if (callbacks != null) {
                                        callbacks.bindItems(infos, 0, infos.size());
                                    }
                                }
                            };
                            mHandler.post(r);
                        }
                        if (postions.size() == 0) {
                            return;
                        }
                        /* YUNOS BEGIN */
                        // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                        // Release not occupied places
                        Runnable runOnUI2 = new Runnable() {

                            @Override
                            public void run() {
                                for (int i = 0; i < postions.size(); i++) {
                                    ScreenPosition pos = postions.get(i);
                                    releaseWorkspacePlace(pos.s, pos.x, pos.y);
                                }
                            }
                        };
                        runOnMainThread(runOnUI2);
                        /* YUNOS END */
                    }
                };
                runOnWorkerThread(runOnWork2);
            }
        };
        runOnMainThread(runOnUI1);
        /* YUNOS END */
    }
    /*YUNOS END*/

    /* YUNOS BEGIN */
    // ##date:2015/1/19 ##author:zhanggong.zg ##BugID:5716493
    public static void postCheckNoSpaceList() {
        sWorker.removeCallbacks(checkNoSpaceListR);
        sWorker.post(checkNoSpaceListR);
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5705812
    public static boolean addItemToNoSpaceList(final ShortcutInfo item, int container) {
        synchronized (sBgLock) {
            if (!sBgNoSpaceItems.contains(item)) {
                sBgWorkspaceItems.remove(item);
                sBgNoSpaceItems.add(item);
            } else {
                return false;
            }
        }
        item.itemType = LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
        item.container = container;
        modifyItemInDatabase(LauncherApplication.getContext(), item, item.container,
                             -1, -1, -1, item.spanX, item.spanY, false);
        return true;
    }

    public static boolean removeItemFromNoSpaceList(final ShortcutInfo item, int container, int s, int x, int y) {
        synchronized (sBgLock) {
            if (sBgNoSpaceItems.contains(item)) {
                sBgNoSpaceItems.remove(item);
                if (!sBgWorkspaceItems.contains(item)) {
                    sBgWorkspaceItems.add(item);
                }
            } else {
                return false;
            }
        }
        item.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        item.container = container;
        modifyItemInDatabase(LauncherApplication.getContext(), item, item.container,
                             s, x, y, item.spanX, item.spanY, false);
        return true;
    }

    public static Map<String, Collection<ShortcutInfo>> getAllNoSpaceItemsWithPackages(Collection<String> pkgNames) {
        if (pkgNames == null || pkgNames.isEmpty()) return Collections.emptyMap();
        Map<String, Collection<ShortcutInfo>> result = new HashMap<String, Collection<ShortcutInfo>>();
        for (String pkgName : pkgNames) {
            if (!TextUtils.isEmpty(pkgName)) {
                result.put(pkgName, new ArrayList<ShortcutInfo>());
            }
        }
        if (result.isEmpty()) return result;

        ArrayList<ItemInfo> noSpaceItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            noSpaceItems.addAll(sBgNoSpaceItems);
        }
        for (ItemInfo item : noSpaceItems) {
            if (item instanceof ShortcutInfo) {
                Intent intent = ((ShortcutInfo) item).intent;
                ComponentName cmpt = intent != null ? intent.getComponent() : null;
                if (cmpt != null && result.containsKey(cmpt.getPackageName())) {
                    result.get(cmpt.getPackageName()).add((ShortcutInfo) item);
                }
            }
        }
        return result;
    }
    /* YUNOS END */

    /**
     * Remove the contents of the specified folder from the database
     */
    static void deleteFolderContentsFromDatabase(Context context, final FolderInfo info) {
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    sBgItemsIdMap.remove(info.id);
                    sBgFolders.remove(info.id);
                    /*YUNOS BEGIN*/
                    //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
                    // client yun icon lost after restore
                    //sBgDbIconCache.remove(info);
                    /*YUNOS END*/
                    sBgWorkspaceItems.remove(info);
                }

                cr.delete(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION,
                        LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
                // Lock on mBgLock *after* the db operation
                synchronized (sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        sBgItemsIdMap.remove(childInfo.id);
                        /*YUNOS BEGIN*/
                        //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
                        // client yun icon lost after restore
                        //sBgDbIconCache.remove(childInfo);
                        /*YUNOS END*/
                    }
                }
                sWorker.removeCallbacks(mCheckInvalidPosItemsRunnable);
                sWorker.postDelayed(mCheckInvalidPosItemsRunnable, 3000);
            }
        };
        runOnWorkerThread(r);
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    /*YUNOS BEGIN*/
    //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
    // fato sd app icon error
    private void onExternalAppavailable(String[] packages) {
        Log.d(TAG, "onExternalAppavailable begin");
        if (mWorkspaceLoaded == false) {
            Log.d(TAG, "workspace no loaded");
            return;
        }
        for(String packagename: packages) {
            Log.d(TAG, "avaliable package name is " + packagename);
            long itemid = -1;
            ArrayList<ItemInfo> allApps = getAllAppItems();
            for(ItemInfo info: allApps) {
                if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) ||
                    (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)) {
                    if (((ShortcutInfo)info).intent.getComponent().getPackageName().equals(packagename)) {
                        itemid = info.id;
                        break;
                    }
                }
            }
            if (itemid == -1) {
                //create a item
                runOnWorkerThread(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, new String[] { packagename }));
            } else {
                Log.d(TAG, "find the item");
                ShortcutInfo info = (ShortcutInfo)sBgItemsIdMap.get(itemid);
                if (info != null) {
                    Drawable icon = mApp.getIconManager().getAppUnifiedIcon(info,null);
                    info.setIcon(icon);
                    /*YUNOS BEGIN*/
                    //##date:2013/12/15 ##author:hao.liuhaolh ##BugID: 73737
                    // SD card app icon lost
                    try {
                        if ((info.intent != null) && (info.intent.getComponent() != null)) {
                            int appFlags = mApp.getPackageManager().getApplicationInfo(info.intent.getComponent().getPackageName(), 0).flags;
                            ((ShortcutInfo)info).isSDApp = Utils.isSdcardApp(appFlags)?1:0;
                            ((ShortcutInfo)info).customIcon = (((ShortcutInfo)info).isSDApp==0)?false:true;

                            boolean isInAppList = false;
                            for(ApplicationInfo appinfo: mBgAllAppsList.data) {
                                if (appinfo.componentName.equals(info.intent.getComponent())) {
                                    Log.d(TAG, "it is in all app list");
                                    isInAppList = true;
                                    break;
                                }
                            }
                            if (isInAppList == false) {
                                Log.d(TAG, "lit isn't in all app list");
                                mBgAllAppsList.addPackage(mApp, info.intent.getComponent().getPackageName());
                                mBgAllAppsList.added.clear();
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "PackageManager.getApplicationInfo failed");
                    }
                    /*YUNOS END*/
                    final ItemInfo finalinfo = info;
                    updateItemInDatabase(mApp, finalinfo);
                    final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                    final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
                    infos.add(info);
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (callbacks != null) {
                                Log.d(TAG, "onExternalAppavailable call bindItemsUpdated");
                                callbacks.bindItemsUpdated(infos);
                            }
                        }
                    };
                    runOnMainThread(r);
                }
            }
        }
        Log.d(TAG, "onExternalAppavailable end");
    }
    /*YUNOS END*/

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG_LOADERS) Log.d(TAG, "onReceive intent=" + intent);

        final String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdatedTask.OP_NONE;

            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                op = PackageUpdatedTask.OP_UPDATE;
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_REMOVE;
                }
                // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                // later, we will update the package at this time
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_ADD;
                } else {
                    op = PackageUpdatedTask.OP_UPDATE;
                }
            }

            if (op != PackageUpdatedTask.OP_NONE) {
                PackageUpdatedTask task = new PackageUpdatedTask(op, new String[] { packageName });
                /* YUNOS BEGIN */
                //## modules(Home Shell): [Category]
                //## date: 2015/07/30 ## author: wangye.wy
                //## BugID: 6221911: category on desk top
                if (action.equals(Intent.ACTION_PACKAGE_ADDED) && !replacing) {
                    Launcher launcher = LauncherApplication.getLauncher();
                    if (launcher != null && launcher.isInLauncherCategoryMode()) {
                        mPackageUpdatedTasks.add(task);
                        recoverAllIcons(false);
                    } else {
                        mPackageUpdateTaskQueue.enqueue(task);
                    }
                /* YUNOS END */
                } else {
                    mPackageUpdateTaskQueue.enqueue(task);
                }
            }
        }else if( AppDownloadManager.ACTION_APP_DWONLOAD_TASK.equals(action) ||
                  AppDownloadManager.ACTION_HS_DOWNLOAD_TASK.equals(action) ){
            String type = intent.getStringExtra(AppDownloadManager.TYPE_ACTION);
            if(TextUtils.isEmpty(type)){
                Log.w(TAG, "LaunchMode : nReceiver, ACTION_APP_DWONLOAD_TASK, type == null");
                return;
            }

            Log.d(TAG,"AppDownLoad action:" + action + " type:" + type);
            //start app download
            int op = AppDownloadManager.OP_NULL;
            if(AppDownloadManager.ACTION_APP_DOWNLOAD_START.equals(type)){
                op = AppDownloadManager.OP_APPSTORE_START;
            } else if(AppDownloadManager.ACTION_APP_DOWNLOAD_RUNNING.equals(type) ){
                op = AppDownloadManager.OP_APPSTORE_RUNING;
            } else if(AppDownloadManager.ACTION_APP_DOWNLOAD_FAIL.equals(type) ){
                op = AppDownloadManager.OP_APPSTORE_FAIL;
                //checkRestoreDoneIfInRestore(intent);
            } else if(AppDownloadManager.ACTION_APP_DOWNLOAD_CANCEL.equals(type) ){
                op = AppDownloadManager.OP_APPSTORE_CANCEL;
                //checkRestoreDoneIfInRestore(intent);
            }
            Runnable r = mAppDownloadMgr.getAppDownloadTask(op, intent);
            if(r != null) {
                post(r);
            }
        }else if(ACTION_REMOVE_APP_VIEWS.equals(action)){
            int op = PackageUpdatedTask.OP_REMOVE;
            final String packageName = intent.getStringExtra(TYPE_PACKAGENAME);
            Log.d(TAG, "ACTION_REMOVE_APP_VIEWS:" + packageName);
            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                return;
            }
            PackageUpdatedTask task = new PackageUpdatedTask(op, new String[] { packageName });
            mPackageUpdateTaskQueue.enqueue(task);
        }
        /*YUNOS BEGIN*/
        //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
        // fato sd app icon error
        else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            // First, schedule to add these apps back in.
            final String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            Runnable r = new Runnable() {
                public void run() {
                    onExternalAppavailable(packages);
                }
            };
            sWorker.post(r);
            //enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, packages));
            // Then, rebind everything.
            //startLoaderFromBackground();
        }
        /*YUNOS END*/

        //for layout change
        else if (ACTION_HOMESHELL_LAYOUT_CHANGE.equals(action)) {
            Log.d(TAG, "receive action homeshell layout change");
            /* YUNOS BEGIN */
            // ##date:2014/06/05 ##author:hongchao.ghc ##BugID:126343
            // for settings show icon mark
            final int countX = intent.getIntExtra(EXTRA_COUNTX, sCellCountX);
            final int countY = intent.getIntExtra(EXTRA_COUNTY, sCellCountY);
            /* YUNOS END */
            if (LauncherApplication.getLauncher() == null) {
                return;
            }

            //BugID: 5204254:mLauncher null pointer exception in LauncherModel
            if (checkGridSize(countX, countY)) {
                /* YUNOS BEGIN */
                // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
                Utils.showLoadingDialog(LauncherApplication.getLauncher());
                sWorker.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        changeLayout(countX, countY, false, null);
                        Utils.dismissLoadingDialog();
                    }
                },200);
                /* YUNOS END */
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:hao.liuhaolh
        //ignore APPLICATIONS AVAILABLE and UNAVAILABLE
        /*
        else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            enqueuePackageUpdated(new PackageUpdatedTask(
                        PackageUpdatedTask.OP_UNAVAILABLE, packages));
        }
        */
        /*YUNOS END*/
        else if (HomeShellSetting.ACTION_HOTSEAT_LAYOUT_CHANGE.equals(action)) {
            rebindHotseat(mApp);
            return;
        }
        else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If we have changed locale we need to clear out the labels in all apps/workspace.
            /*YUNOS BEGIN*/
            //##module(HomeShell)
            //##date:2014/03/27 ##author:hao.liuhaolh@alibaba-inc.com##BugID:105074
            //restore the deleted icon code in launcher model
            FancyIconsHelper.clearCache();
            mApp.getIconManager().clearFancyIconCache();
            /*YUNOS END*/

            /* clear gadgets on language changed (BugID:5212185:wenliang.dwl)*/
            GadgetCardHelper.onLocaleChanged();
            LauncherGadgetHelper.cleanUp();

            //BugID:5739072 only zh_CN support PushToTalk
            CheckVoiceCommandPressHelper.checkEnvironment();

            forceReload();

            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
        } else if (ACTION_AGED_MODE_CHANGED.equals(action)) {
            Launcher launcher = LauncherApplication.getLauncher();
            if (launcher != null) {
                if (launcher.isInLauncherEditMode()) {
                    launcher.exitLauncherEditMode(false);
                } else if (launcher.isInLauncherCategoryMode()) {
                    recoverAllIcons(true);
                } else if (launcher.isInEditScreenMode()) {
                    launcher.exitScreenEditModeWithoutSave();
                }
            }
            int agedMode = intent.getIntExtra("aged_mode", 0);
            boolean isAgedMode = (agedMode == AgedModeUtil.AGED_MODE_FLAG_IN_MSG);
            AgedModeUtil.setAgedMode(isAgedMode);
            Log.d(AgedModeUtil.TAG, "Receive the agedmode change message in launcherModel"
                    + isAgedMode);
            stopAllDownloadItems();
            HomeShellSetting.setFreezeValue(LauncherApplication.getContext(), isAgedMode);
            if (launcher != null) {
                if (launcher.isStarted()) {
                    Launcher.mIsAgedMode = isAgedMode;
                    Log.d(AgedModeUtil.TAG, "Launcher has started, so call onAgedModeChanged in onReceive method of LauncherModel"
                            + isAgedMode);
                    mApp.onAgedModeChanged(isAgedMode, true, true);
                }
                //else if (!launcher.isFinishing()) {
                    //launcher.finish();
                //}
            }

            /*YUNOS END*/

            /* YUNOS BEGIN */
            // ##date:2014/06/05 ##author:hongchao.ghc ##BugID:126343
            // for settings show icon mark
        } else if (ACTION_UPDATE_LAYOUT.equals(action)) {
            /* YUNOS BEGIN */
            // ##date:2014/06/19 ##author:hongchao.ghc ##BugID:130963
            // optimize the icon mark new feature
            /* YUNOS BEGIN */
            // ##date:2015-1-23 ##author:zhanggong.zg ##BugID:5732116
            // update card indicator when notification mark preference changed
            String[] pkgNameArr = intent.getStringArrayExtra(HomeShellSetting.EXTRA_NOTIFICATION_PACKAGE_NAMES);
            final Set<String> notifPckgs = pkgNameArr != null ? new HashSet<String>(Arrays.asList(pkgNameArr)) : null;
            /* YUNOS END */
            sWorker.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "liuhao handle ACTION_UPDATE_LAYOUT");
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    ArrayList<ItemInfo> apps = new ArrayList<ItemInfo>();
                    for (ItemInfo info : allApps) {
                        if (info.isNewItem()) {
                            apps.add(info);
                        } else if (notifPckgs != null &&
                                   info instanceof ShortcutInfo) {
                            /* YUNOS BEGIN */
                            // ##date:2015-1-23 ##author:zhanggong.zg ##BugID:5732116
                            // update card indicator when notification mark preference changed
                            Intent intent = ((ShortcutInfo) info).intent;
                            if (intent != null) {
                                ComponentName cmpt = intent.getComponent();
                                if (cmpt != null && notifPckgs.contains(cmpt.getPackageName())) {
                                    apps.add(info);
                                }
                            }
                            /* YUNOS END */
                        } else if (info.messageNum > 0) {
                            apps.add(info);
                        }
                    }
                    notifyUIUpdateIcon(apps);
                }
            });
            /* YUNOS END */
        //bugid: 5232096: receive ACTION_APPLICATION_NOTIFICATION in launcherModel to speedup
        //icon mark update
        } else if (ACTION_UPDATE_SLIDEUP.equals(action)) {
                sWorker.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "liuhao handle ACTION_UPDATE_SLIDEUP");
                        ArrayList<ItemInfo> allApps = getAllAppItems();
                        ArrayList<ItemInfo> apps = new ArrayList<ItemInfo>();
                        for (ItemInfo info : allApps) {
                            //BugID:6360112:yunos sync flingup indicator not update
                            if (info instanceof ShortcutInfo && ((ShortcutInfo)info).intent != null) {
                                if (GadgetCardHelper.getInstance(mApp).hasCardView(
                                    ((ShortcutInfo)info).intent.getComponent(), null, info.messageNum > 0)) {
                                apps.add(info);
                                } else {
                                    ComponentName cn = ((ShortcutInfo)info).intent.getComponent();
                                    if (LauncherApplication.getLauncher() != null &&
                                            LauncherApplication.getLauncher().hasWidgetView(cn)) {
                                        apps.add(info);
                                    }
                                }
                            }
                        }
                        notifyUIUpdateIcon(apps);
                    }
                });
        } else if (ACTION_UPDATE_CLONABLE.equals(action)) {
            sWorker.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "-- handle ACTION_UPDATE_CLONABLE");
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    ArrayList<ItemInfo> apps = new ArrayList<ItemInfo>();
                    for (ItemInfo info : allApps) {
                        if (info instanceof ShortcutInfo && "com.tencent.mm".equals(((ShortcutInfo)info).getPackageName())) {
                            Log.d(TAG, "-- get!");
                        }
                        if (AppCloneManager.getInstance().showClonableMark(info)) {
                            apps.add(info);
                        }
                    }
                    notifyUIUpdateIcon(apps);
                }
            });
        } else if (IconDigitalMarkHandler.ACTION_APPLICATION_NOTIFICATION.equals(action)) {
            IconDigitalMarkHandler.getInstance().handleNotificationIntent(context, intent);
        } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
            IconDigitalMarkHandler.getInstance().handleNotificationIntent(context, intent);
        } else if(ACTION_APP_LAUNCHED.equals(action)) {
            final String comp = intent.getExtras().getString("comp");
            handleAppLaunched(comp);
        } else if (HomeShellSetting.ACTION_ON_MARK_TYPE_CHANGE.equals(action)) {
            sWorker.post(new Runnable() {
                @Override
                public void run() {
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    ArrayList<ItemInfo> apps = new ArrayList<ItemInfo>();
                    GadgetCardHelper helper = GadgetCardHelper.getInstance(LauncherApplication.getContext());
                    for (ItemInfo info : allApps) {
                        if (info.messageNum > 0) {
                            apps.add(info);
                        } else if (helper.containsKey(info)) {
                            apps.add(info);
                        }
                    }
                    notifyUIUpdateIcon(apps);
                }
            });
            updateNotificationMarkType();
            return;
        } else if (HomeShellSetting.ACTION_ON_SHOW_NEW_MARK_CHANGE.equals(action)) {
            updateShowNewMark();
            return;
        } else if (HomeShellSetting.ACTION_ON_SHOW_CLONABLE_MARK_CHANGED.equals(action)) {
            updateShowClonableMark();
            return;
        } else if (HomeShellSetting.ACTION_ON_SHOW_SLIDE_UP_MARK_CHANGE.equals(action)) {
            updateShowSlideUpMark();
        }
        /* YUNOS END */
        /*YUNOS BEGIN*/
        //##date:2013/11/26 ##author:hao.liuhaolh
        //ignore ACTION_CONFIGURATION_CHANGED
        /*
        else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
             // Check if configuration change was an mcc/mnc change which would affect app resources
             // and we would need to clear out the labels in all apps/workspace. Same handling as
             // above for ACTION_LOCALE_CHANGED
             Configuration currentConfig = context.getResources().getConfiguration();
             if (mPreviousConfigMcc != currentConfig.mcc) {
                   Log.d(TAG, "Reload apps on config change. curr_mcc:"
                       + currentConfig.mcc + " prevmcc:" + mPreviousConfigMcc);
                   forceReload();
             }
             // Update previousConfig
             mPreviousConfigMcc = currentConfig.mcc;
        }
        */
        /*YUNOS END*/
    }

    private void handleAppLaunched(String comp){
        if(!TextUtils.isEmpty(comp)) {
            final String[] strs = comp.split("/");
            sWorker.post(new Runnable() {

                @Override
                public void run() {
                    Collection<ItemInfo> apps = getAllAppItems();
                    String pkg = strs[0];
                    Intent intent2 = null;
                    String pkgName = null;
                    ContentValues values =  new ContentValues();
                    values.put(LauncherSettings.Favorites.IS_NEW, 0);

                    for (ItemInfo info : apps) {
                        if (info instanceof ShortcutInfo) {
                            intent2 = ((ShortcutInfo) info).intent;
                            if (intent2 == null || intent2.getComponent() == null || !info.isNewItem()) {
                                continue;
                            }
                            pkgName = intent2.getComponent().getPackageName();
                            if (pkg.equals(pkgName)) {
                                info.setIsNewItem(false);
                                updateItemById(LauncherApplication.getContext(), info.id, values, true);
                            }
                        }
                    }
                }
            });
        }
    }

    private void forceReload() {
        resetLoadedState(true, true);

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    /* YUNOS BEGIN */
    //##date:2013/12/08 ##author:hongxing.whx ##bugid: 72248:
    public void setThemeChanged(boolean flag) {
        mThemeChanged = flag;
    }
    /* YUNOS END */
    /**
     * When the launcher is in the background, it's possible for it to miss paired
     * configuration changes.  So whenever we trigger the loader from the background
     * tell the launcher that it needs to re-run the loader when it comes back instead
     * of doing it now.
     */
    public void startLoaderFromBackground() {
        boolean runLoader = false;
        if (mCallbacks != null) {
            Callbacks callbacks = mCallbacks.get();
            if (callbacks != null) {
                // Only actually run the loader if they're not paused.
                if (!callbacks.setLoadOnResume()) {
                    runLoader = true;
                }
            }
        }
        if (runLoader) {
            startLoader(false, -1);
        }
    }

    // If there is already a loader task running, tell it to stop.
    // returns true if isLaunching() was true on the old task
    private boolean stopLoaderLocked() {
        boolean isLaunching = false;
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        return isLaunching;
    }

    public void startLoader(boolean isLaunching, int synchronousBindPage) {
        synchronized (mLock) {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "startLoader isLaunching=" + isLaunching
                        + " mCallbacks " + mCallbacks
                        + " mCallbacks.get() " + (mCallbacks == null ? null : mCallbacks.get())
                        + " sWorkerThread.isAlive " + (sWorkerThread == null ? null : sWorkerThread.isAlive())
                        + " sWorkerThread.isInterrupted " + (sWorkerThread == null ? null : sWorkerThread.isInterrupted())
                        );
            }

            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            mDeferredBindRunnables.clear();

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                // also, don't downgrade isLaunching if we're already running
                isLaunching = isLaunching || stopLoaderLocked();
                mLoaderTask = new LoaderTask(mApp, isLaunching);
                if (synchronousBindPage > -1 && mAllAppsLoaded && mWorkspaceLoaded) {
                    Log.d(TAG, "runBindSynchronousPage");
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    Log.d(TAG, "startLoader sWorker.post LoaderTask");
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);
                }
            }
        }
    }

    class SwitchDB implements Runnable {
        boolean agedMode;

        public SwitchDB(boolean agedMode) {
            this.agedMode = agedMode;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.d("AgedModeUtil.TAG", "switch database,database now agedMode state:" + agedMode);
            mApp.getLauncherProvider().switchDB(agedMode);
        }

    };

    public void bindRemainingSynchronousPages() {
        // Post the remaining side pages to be loaded
        if (!mDeferredBindRunnables.isEmpty()) {
            for (final Runnable r : mDeferredBindRunnables) {
                mHandler.post(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
            mDeferredBindRunnables.clear();
        }
    }

    public void stopLoader() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                mLoaderTask.stopLocked();
            }
        }
    }

    public boolean isAllAppsLoaded() {
        return mAllAppsLoaded;
    }

    public boolean isLoadingWorkspace() {
        /*
        synchronized (mLock) {
            if (mLoaderTask != null) {
                return mLoaderTask.isLoadingWorkspace();
            }
        }
        return false;
        */
        return mIsLoadingAndBindingWorkspace;
    }

    /**
     * Runnable for the thread that loads the contents of the launcher:
     *   - workspace icons
     *   - widgets
     *   - all apps icons
     */
    private class LoaderTask implements Runnable {
        private static final String ACTION_HOMESHELL_BACKUP =  "com.aliyun.homeshell.systembackup.RestoreAllApp";
        private Context mContext;
        private boolean mIsLaunching;
        //BugID:5218961:shouldn't check empty cell in loading
        //private boolean mIsLoadingAndBindingWorkspace;
        private boolean mStopped;
        private boolean mLoadAndBindStepFinished;
        private boolean mIsInStartBinding = false;
        private boolean mIsCurrentScreenBinded = false;
        private boolean mIsCurrentScreenUpdated = false;

        private HashMap<Object, CharSequence> mLabelCache;

        LoaderTask(Context context, boolean isLaunching) {
            mContext = context;
            mIsLaunching = isLaunching;
            mLabelCache = new HashMap<Object, CharSequence>();
        }

        boolean isLaunching() {
            return mIsLaunching;
        }

        //BugID:5218961:shouldn't check empty cell in loading
        /*
        boolean isLoadingWorkspace() {
            return mIsLoadingAndBindingWorkspace;
        }
        */

        /*YUNOS BEGIN*/
        //##module(HomeShell)
        //##date:2014/04/17##author:hao.liuhaolh@alibaba-inc.com##BugID:111614
        //load and display current screen first during homeshell start
        private void loadCurrentScreenWorkspace(int screenID, List<Long> itemsToRemove,
                                            List<ResolveInfo> itemsAllApps, List<ShortcutInfo> itemsInDBForApp,
                                            List<FolderInfo> folderToRemove) {
            Log.d(TAG, "loadCurrentScreenWorkspace in load screen " + screenID);
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();
            final String searchOldPackage = "com.yunos.alimobilesearch";
            final String searchOldClass = "com.yunos.alimobilesearch.widget.SearchWidgetProvider";
            final String searchNewPackage = "com.yunos.lifecard";
            final String searchNewClass = "com.yunos.alimobilesearch.widget.SearchWidgetProvider";
//            boolean isWorkspaceContainAliWidget = false;

            final Map<String, GadgetInfo> gadgets = ThemeUtils.listGadgets(mContext);

            //##date:2014/05/05##author:hao.liuhaolh@alibaba-inc.com##BugID:116612
            //##date:2014/08/15##author:zhanggong.zg##BugID:5186578
            synchronized (sBgLock) {
                final Cursor c = contentResolver.query(
                        LauncherSettings.Favorites.CONTENT_URI,
                        null,
                        "container=? or (screen=? and container<>?) or container>?",
                        new String[] {
                            String.valueOf(-101),
                            String.valueOf(screenID),
                            String.valueOf(Favorites.CONTAINER_HIDESEAT),
                            String.valueOf(-1)
                        },
                        null);
                // +1 for the hotseat (it can be larger than the workspace)
                // Load workspace in reverse order to ensure that latest items are loaded first (and
                // before any earlier duplicates)

                //##date:2014/4/05 ##author:hongxing.whx ##BugID:116588
                try {
                    waitThemeService();
                } catch (Exception e) {
                    Log.d(TAG,"waitThemeService met exception: " + e );
                }

                // +1 for hotseat , +6 for hideseat
                /* HIDESEAT_SCREEN_NUM_MARKER: see ConfigManager.java */
                final ItemInfo occupied[][][] =
                        new ItemInfo[mMaxIconScreenCount+ 1 + ConfigManager.getHideseatScreenMaxCount()][sCellCountX + 1][sCellCountY + 1];
                try {
                    final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.INTENT);
                    final int titleIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.TITLE);
                    final int iconTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_TYPE);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    final int iconPackageIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_PACKAGE);
                    final int iconResourceIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_RESOURCE);
                    final int containerIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ITEM_TYPE);
                    final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.APPWIDGET_ID);
                    final int screenIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SCREEN);
                    //for pad land mode
                    final int cellXLandIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLXLAND);
                    final int cellYLandIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLYLAND);
                    final int cellXPortIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                    final int cellYPortIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

                    final int cellXIndex;
                    final int cellYIndex;
                    if (!mOrienLandedOnLoadStart) {
                        cellXIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                        cellYIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                    } else {
                        cellXIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLXLAND);
                        cellYIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLYLAND);
                    }

                    final int spanXIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SPANY);
                    final int msgNumIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.MESSAGE_NUM);
                    final int isNewIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.IS_NEW);
//                    final int canDeleteIndex = c.getColumnIndexOrThrow(
//                            LauncherSettings.Favorites.CAN_DELEDE);
                    final int isSDAppIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.IS_SDAPP);
                    final int userIdIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.USER_ID);

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    int container;
                    long id;
                    Intent intent;
                    int hotseatitemcount = 0;

                    while (!mStopped && c.moveToNext()) {
                        try {
                            int itemType = c.getInt(itemTypeIndex);
                            id = c.getLong(idIndex);

                            if (c.getInt(containerIndex) == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                if ((c.getInt(cellXIndex) >= sCellCountX) ||
                                     (c.getInt(cellYIndex) >= sCellCountY) ||
                                     (c.getInt(spanXIndex) + c.getInt(cellXIndex) > sCellCountX) ||
                                     (c.getInt(spanYIndex) + c.getInt(cellYIndex) > sCellCountY) ||
                                     (c.getInt(screenIndex) >= mMaxIconScreenCount)) {
                                    itemsToRemove.add(id);
                                    Log.d(TAG, "item position error, id is " + id);
                                    continue;
                                }
                            }

                            switch (itemType) {
                            case LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
// remove vp install
//                            case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (URISyntaxException e) {
                                    itemsToRemove.add(id);
                                    continue;
                                }
                                //remove all download item when age mode change
                                if ((itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) && (mClearAllDownload == true)) {
                                    itemsToRemove.add(id);
                                    continue;
                                }
                                //remove fifth or more item in hotseat
                                int parent = c.getInt(containerIndex);
                                if(parent == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
                                    int hotseatMaxCount =
                                            LauncherApplication.isInLandOrientation() ?
                                                    ConfigManager.getHotseatMaxCountY() :
                                                        ConfigManager.getHotseatMaxCountX();
                                    if(++hotseatitemcount > hotseatMaxCount){
                                        itemsToRemove.add(id);
                                        continue;
                                    }
                                }

// remove vp install
//                                if (itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
//                                    Log.d(TAG, "start check vp install item");
//                                    PackageInfo packageInfoInstall = null;
//                                    try {
//                                        packageInfoInstall = manager.getPackageInfo(intent.getStringExtra("packagename"), 0);
//                                    } catch (NameNotFoundException e) {
//                                        Log.d(TAG, "the package is not installed");
//                                    }
//                                    if (packageInfoInstall != null) {
//                                        Log.d(TAG, packageInfoInstall.packageName + " is installed");
//                                        itemsToRemove.add(id);
//                                        continue;
//                                    }
//                                    if (intent == null) {
//                                        itemsToRemove.add(id);
//                                        continue;
//                                    }
//                                    if ((intent.getComponent() == null) || (intent.getComponent().equals(""))){
//                                        Log.d(TAG, "component is null");
//                                        ComponentName compName = new ComponentName(intent.getStringExtra(VPUtils.TYPE_PACKAGENAME), "vpinstall");
//                                        intent.setComponent(compName);
//                                    }
//                                }

                                if ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) ||
                                    (itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION)) {
                                    ResolveInfo appInfo = AllAppsList.findActivityInfo(itemsAllApps, intent.getComponent());
                                    if (appInfo == null) {
                                        if (c.getInt(isSDAppIndex) == 0) {
                                            itemsToRemove.add(id);
                                            continue;
                                        }else if (BackupManager.getInstance().isInRestore()) {
                                            if ((intent == null) || (intent.getComponent() == null) ||
                                                 (BackupUitil.isInRestoreList(context, intent.getComponent().getPackageName()) == true)) {
                                                itemsToRemove.add(id);
                                                continue;
                                            }
                                        }
                                    }
                                    int userId = c.getInt(userIdIndex);
                                    /*YUNOS BEGIN*/
                                    //##module(HomeShell)
                                    //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
                                    //check restore data to avoid no item in dock
                                    if (userId == 0) {
                                        if ((intent == null) || (allComponentList.contains(intent.toString()))) {
                                            itemsToRemove.add(id);
                                            continue;
                                        } else {
                                            allComponentList.add(intent.toString());
                                        }
                                    } else if (userId > 0) {
                                        if (!AppCloneManager.isSupportAppClone()) {
                                            continue;
                                        } else if (intent != null) {
                                            CloneAppKey key = new CloneAppKey(userId, intent.getComponent().getPackageName());
                                            if (mAllCloneAppList.contains(key)) {
                                                mAllCloneAppList.remove(key);
                                            } else {
                                                itemsToRemove.add(id);
                                                continue;
                                            }
                                        } else {
                                            itemsToRemove.add(id);
                                            continue;
                                        }
                                    }
                                    /*YUNOS END*/
                                    info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                            titleIndex, mLabelCache);
                                    info.itemType = itemType;
                                    info.userId = userId;
                                } else {
                                    info = getShortcutInfo(c, context, iconTypeIndex,
                                            iconPackageIndex, iconResourceIndex, iconIndex,
                                            titleIndex);

                                    // Update the title of VP install apps.
// remove vp install
//                                    String appPackageFilePath = intent.getStringExtra(VPUtils.TYPE_PACKAGEPATH);
//                                    if (!TextUtils.isEmpty(appPackageFilePath)) {
//                                        CharSequence vpLabel = VPUtils.getVpinstallLabel(appPackageFilePath);
//                                        if (!TextUtils.isEmpty(vpLabel)) {
//                                            info.title = vpLabel;
//                                        }
//                                    }

                                    // App shortcuts that used to be automatically added to Launcher
                                    // didn't always have the correct intent flags set, so do that
                                    // here
                                    if (intent.getAction() != null &&
                                        intent.getCategories() != null &&
                                        intent.getAction().equals(Intent.ACTION_MAIN) &&
                                        intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                        intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                    }
                                }

                                if (info != null) {
                                    info.intent = intent;
                                    info.id = c.getLong(idIndex);
                                    container = c.getInt(containerIndex);
                                    info.container = container;
                                    info.screen = c.getInt(screenIndex);
//                                    if(ConfigManager.isLandOrienSupport()) {
                                        //for pad land mode
                                        info.cellXLand = c.getInt(cellXLandIndex);
                                        info.cellYLand = c.getInt(cellYLandIndex);
                                        info.cellXPort = c.getInt(cellXPortIndex);
                                        info.cellYPort = c.getInt(cellYPortIndex);
                                        //info.initCellXYOnLoad((int) info.container, info.cellXPort, info.cellYPort, info.cellXLand, info.cellYLand);
                                        info.cellX = c.getInt(cellXIndex);
                                        info.cellY = c.getInt(cellYIndex);
                                        info.dumpXY(" loadWorkspace ");
//                                    } else {
//                                        info.cellX = c.getInt(cellXIndex);
//                                        info.cellY = c.getInt(cellYIndex);
//                                    }
                                    info.isNew = c.getInt(isNewIndex);
                                    info.messageNum = c.getInt(msgNumIndex);
                                    /* YUNOS BEGIN */
                                    // ## date: 2016/06/29 ## author: yongxing.lyx
                                    // ## BugID:8471126:show paused after changed language when downloaded app is installing.
                                    updateDownloadStatus(info);
                                    /* YUNOS BEGIN */
                                    // check & update map of what's occupied
                                    if (itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                        if ((info.screen < 0) || (info.cellX < 0) || (info.cellY < 0) ||
                                            (!checkItemPlacement(occupied, info))) {
                                            //if the item place is occupied by other item,
                                            //or it's position invalid,
                                            //remove it from db, if the item is an app,
                                            //it can be added in below operation,
                                            //other types will not be recovered
                                            itemsToRemove.add(info.id);
                                            break;
                                        }
                                    } else {
                                        Log.d(TAG, "it is a no space item");
                                    }

                                    switch (container) {
                                    case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                    case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                    //add hide icon container
                                    case LauncherSettings.Favorites.CONTAINER_HIDESEAT:
                                        if (itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                            sBgNoSpaceItems.add(info);
                                        } else {
                                            sBgWorkspaceItems.add(info);
                                        }
                                        break;
                                    default:
                                        //if an item in a folder, it's container is folder's id
                                        //so the item's container must not less than 0
                                        if (container >= 0) {
                                            // Item is in a user folder
                                            FolderInfo folderInfo =
                                                    findOrMakeFolder(sBgFolders, container);
                                            folderInfo.add(info);
                                        }
                                        break;
                                    }
                                    sBgItemsIdMap.put(info.id, info);

                                    // if the shortcut is for applicaiton, add it to itemsInDBForApp
                                    if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) ||
                                        (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION)){
                                        itemsInDBForApp.add(info);
                                    }

                                    //BugID:5614377:create card background in worker thread
                                    if (info.itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                        if (mIconManager.supprtCardIcon() == true) {
                                            mIconManager.getAppCardBackgroud(info);
                                        }
                                    }
                                } else {
                                    // Failed to load the shortcut, probably because the
                                    // activity manager couldn't resolve it (maybe the app
                                    // was uninstalled), or the db row was somehow screwed up.
                                    // Delete it.
                                    id = c.getLong(idIndex);
                                    Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                                    contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                                id, false), null, null);
                                }
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                id = c.getLong(idIndex);
                                FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                folderInfo.title = c.getString(titleIndex);
                                    // updateFolderTitle(folderInfo);
                                folderInfo.id = id;
                                container = c.getInt(containerIndex);
                                folderInfo.container = container;
                                folderInfo.screen = c.getInt(screenIndex);
//                                if(ConfigManager.isLandOrienSupport()) {
                                    //for pad land mode
                                    folderInfo.cellXLand = c.getInt(cellXLandIndex);
                                    folderInfo.cellYLand = c.getInt(cellYLandIndex);
                                    folderInfo.cellXPort = c.getInt(cellXPortIndex);
                                    folderInfo.cellYPort = c.getInt(cellYPortIndex);
                                    //folderInfo.initCellXYOnLoad((int) folderInfo.container, folderInfo.cellXPort, folderInfo.cellYPort, folderInfo.cellXLand, folderInfo.cellYLand);
                                    folderInfo.cellX = c.getInt(cellXIndex);
                                    folderInfo.cellY = c.getInt(cellYIndex);
                                    folderInfo.dumpXY(" loadWorkspace folderInfo");
//                                } else {
//                                    folderInfo.cellX = c.getInt(cellXIndex);
//                                    folderInfo.cellY = c.getInt(cellYIndex);
//                                }

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, folderInfo)) {
                                    itemsToRemove.add(folderInfo.id);
                                    if (sBgFolders.containsKey(folderInfo.id)) {
                                        sBgFolders.remove(folderInfo.id);
                                    }
                                    if (sBgItemsIdMap.containsKey(folderInfo.id)) {
                                        sBgItemsIdMap.remove(folderInfo.id);
                                    }
                                    break;
                                }
                                //remove fifth or more item in hotseat
                                if(container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
                                    if(++hotseatitemcount > ConfigManager.getHotseatMaxCountX()){
                                        itemsToRemove.add(folderInfo.id);
                                        if (sBgFolders.containsKey(folderInfo.id)) {
                                            sBgFolders.remove(folderInfo.id);
                                        }
                                        if (sBgItemsIdMap.containsKey(folderInfo.id)) {
                                            sBgItemsIdMap.remove(folderInfo.id);
                                        }
                                        continue;
                                    }
                                }
                                switch (container) {
                                    case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                    case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                    case LauncherSettings.Favorites.CONTAINER_HIDESEAT:
                                        sBgWorkspaceItems.add(folderInfo);
                                        break;
                                }

                                sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                sBgFolders.put(folderInfo.id, folderInfo);
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                // Read all Launcher-specific widget details
                                int appWidgetId = c.getInt(appWidgetIdIndex);
                                // to restore widget which is bound to the system application
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (URISyntaxException e) {
                                    itemsToRemove.add(id);
                                    continue;
                                }

                                AppWidgetProviderInfo provider =
                                        widgets.getAppWidgetInfo(appWidgetId);
                                boolean needUpdateWidget = false;
                                if (BackupManager.getInstance().isInRestore()) {
                                    try {
                                        Log.d(TAG, "hongixng, widget loading in restore mode");
                                        AppWidgetHost host = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);
                                        int tmpID = host.allocateAppWidgetId();
                                        Log.d(TAG, String.format("widget id changes from %d to %d", appWidgetId, tmpID));
                                        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                                        ACA.AppWidgetManager.bindAppWidgetId(appWidgetManager, tmpID, intent.getComponent());
                                        appWidgetId = tmpID;
                                        provider = widgets.getAppWidgetInfo(appWidgetId);
                                        needUpdateWidget = true;
                                    } catch (Exception e) {
                                        Log.d(TAG, "restore system's widget met:" + e);
                                    }
                                }

                                /* special code for alimobilesearch */
                                /* YUNOS BEGIN */
                                // ##date:2016/03/29 ##author:xiangnan.xn ##BugID:8067003
                                // special code for alimobilesearch widget. replace alimobilesearch widget with lifecard widget if possible
                                // it need recreate a new appWidgetId, rebind widget, refresh database and regenerate a AppWidgetHostView
                                final boolean providerIsInvalid = provider == null || provider.provider == null || provider.provider.getPackageName() == null;
                                final boolean widgetReplaceable = intent.getComponent().getPackageName().equals(searchOldPackage) && Utils.isPackageExist(context, searchNewPackage);
                                Log.d(TAG, "providerIsInvalid=" + (providerIsInvalid ? "true" : "false"));
                                Log.d(TAG, "current widget package: " + intent.getComponent().getPackageName());
                                Log.d(TAG, "widget: " + searchOldPackage + " is " + (widgetReplaceable ? "replaceable":"not replaceable"));

                                if (isSafeMode) {
                                    String log = "Deleting widget since system is in safe mode: id = "
                                        + id + " appWidgetId = " + appWidgetId;
                                    Log.e(TAG, log);
                                    itemsToRemove.add(id);
                                } else if (providerIsInvalid && !widgetReplaceable) {
                                    String log = "Deleting widget that isn't installed anymore: id = "
                                        + id + " appWidgetId = " + appWidgetId;
                                    Log.e(TAG, log);
                                    itemsToRemove.add(id);
                                } else {
                                    if (widgetReplaceable) {
                                        try {
                                            Log.d(TAG, "replace widget");
                                            AppWidgetHost host = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);
                                            int tmpID = host.allocateAppWidgetId();
                                            Log.d(TAG, String.format("widget id changes from %d to %d", appWidgetId, tmpID));
                                            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                                            final ComponentName newComponent = new ComponentName(searchNewPackage, searchNewClass);
                                            ACA.AppWidgetManager.bindAppWidgetId(appWidgetManager, tmpID, newComponent);
                                            appWidgetId = tmpID;
                                            provider = widgets.getAppWidgetInfo(appWidgetId);
                                            needUpdateWidget = true;
                                        } catch (Exception e) {
                                            Log.d(TAG, "replace widget met:" + e);
                                        }
                                    }
                                /* YUNOS END */

                                    appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId,
                                            provider.provider);
                                    appWidgetInfo.id = id;
                                    appWidgetInfo.screen = c.getInt(screenIndex);
//                                    if(ConfigManager.isLandOrienSupport()) {
                                        //for pad land mode
                                        appWidgetInfo.cellXLand = c.getInt(cellXLandIndex);
                                        appWidgetInfo.cellYLand = c.getInt(cellYLandIndex);
                                        appWidgetInfo.cellXPort = c.getInt(cellXPortIndex);
                                        appWidgetInfo.cellYPort = c.getInt(cellYPortIndex);
                                        appWidgetInfo.setCellXY();
//                                    } else {
//                                        appWidgetInfo.cellX = c.getInt(cellXIndex);
//                                        appWidgetInfo.cellY = c.getInt(cellYIndex);
//                                    }
                                    appWidgetInfo.spanX = c.getInt(spanXIndex);
                                    appWidgetInfo.spanY = c.getInt(spanYIndex);
                                    int[] minSpan = Launcher.getMinSpanForWidget(context, provider);
                                    appWidgetInfo.minSpanX = minSpan[0];
                                    appWidgetInfo.minSpanY = minSpan[1];

                                    container = c.getInt(containerIndex);
                                    if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                                        container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                                        container != LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                                        Log.e(TAG, "Widget found where container != " +
                                            "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT nor CONTAINER_HIDESEAT- ignoring!");
                                        continue;
                                    }
                                    appWidgetInfo.container = c.getInt(containerIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, appWidgetInfo)) {
                                        break;
                                    }
                                    sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                    sBgAppWidgets.add(appWidgetInfo);

                                    /* YUNOS BEGIN */
                                    // ##date:2015/6/11 ##author:zhanggong.zg ##BugID:6050280
                                    // Widget id is reallocated after restore.
                                    if (needUpdateWidget) {
                                        updateItemInDatabase(context, appWidgetInfo);
                                    }
                                    /* YUNOS END */
                                }
                                break;
                            case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                                String strType = c.getString(titleIndex);
                                    if (!TextUtils.isEmpty(strType)) {
                                        GadgetInfo ginfo = gadgets.get(strType);
                                        if (ginfo != null) {
                                            GadgetItemInfo gi = new GadgetItemInfo(ginfo);
                                            gi.id = c.getLong(idIndex);
                                            gi.screen = c.getInt(screenIndex);
//                                            if(ConfigManager.isLandOrienSupport()) {
                                                //for pad land mode
                                                gi.cellXLand = c.getInt(cellXLandIndex);
                                                gi.cellYLand = c.getInt(cellYLandIndex);
                                                gi.cellXPort = c.getInt(cellXPortIndex);
                                                gi.cellYPort = c.getInt(cellYPortIndex);
                                                gi.setCellXY();
//                                            } else {
//                                                gi.cellX = c.getInt(cellXIndex);
//                                                gi.cellY = c.getInt(cellYIndex);
//                                            }
                                            gi.spanX = c.getInt(spanXIndex);
                                            gi.spanY = c.getInt(spanYIndex);
                                            sBgWorkspaceItems.add(gi);
                                            sBgItemsIdMap.put(gi.id, gi);
                                        }
                                    }
                                break;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Desktop items loading interrupted:", e);
                        }
                    }
                } finally {
                    c.close();
                }

                if (mStopped != true) {
                    //BugID:142161:check folder should be done when mStopped isn't true
                    //Sometimes Launcher activity is started two times when phone power up
                    //and loadworkspace will be teminated during first start.
                    //BugID: 118887:empty folder in first screen not removed in restore
                    Log.d(TAG, "check empty folder");
                    for (FolderInfo info: sBgFolders.values()) {
                        if ((info != null) && (info.contents != null)) {
                            Log.d(TAG, "folder " + info.title + " size is " + info.contents.size());
                            if (info.contents.size() == 0) {
                                if(folderToRemove.contains(info) == false) {
                                    folderToRemove.add(info);
                                }
                            } else if ((info.contents.size() == 1) &&
                                       (BackupManager.getInstance().isInRestore() == false)){
                                Log.d(TAG, "folder id is " + info.id);
                                ShortcutInfo itemInFolder = info.contents.get(0);
                                if (itemInFolder != null) {
                                    //replace the folder with the item in it
                                    itemInFolder.cellX = info.cellX;
                                    itemInFolder.cellY = info.cellY;
                                    //for pad land mode
                                    itemInFolder.cellXLand = info.cellXLand;
                                    itemInFolder.cellYLand = info.cellYLand;
                                    itemInFolder.cellXPort = info.cellXPort;
                                    itemInFolder.cellYPort = info.cellYPort;
                                    itemInFolder.container = info.container;
                                    itemInFolder.screen = info.screen;
                                    updateItemInDatabase(mContext, itemInFolder);
                                }
                                if(folderToRemove.contains(info) == false) {
                                    folderToRemove.add(info);
                                }
                            }
                        }
                    }
                    /*YUNOS END*/
                    //since all items in folders is loaded, remove empty folders to
                    //avoid they are displayed in workspace
                    for (FolderInfo info: folderToRemove) {
                        final ItemInfo iteminfo = (ItemInfo) info;
                        Log.d(TAG, "remove folder " + info.title);
                        deleteItemFromDatabase(mApp, iteminfo);
                    }
                }

                folderToRemove.clear();

                if (DEBUG_LOADERS) {
                    Log.d(TAG, "loaded one screen " + screenID + " workspace in " + (SystemClock.uptimeMillis()-t) + "ms");
                }
            }
            Log.d(TAG, "loadOneScreenWorkspace out");
        }

        private void bindCurrentScreenWorkspace(int synchronizeBindPage) {
            final long t = SystemClock.uptimeMillis();
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            final boolean isLoadingSynchronously = (synchronizeBindPage > -1);
            //##date:2014/04/30##author:hao.liuhaolh@alibaba-inc.com##BugID:115612
            final int currentScreen = 0;//isLoadingSynchronously ? synchronizeBindPage :
                //oldCallbacks.getCurrentWorkspaceScreen();

            // Load all the items that are on the current page first (and in the process, unbind
            // all the existing workspace items before we call startBinding() below.
            if (mIsInStartBinding == false) {
                unbindWorkspaceItemsOnMainThread();
            }
            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                folders.putAll(sBgFolders);
                itemsIdMap.putAll(sBgItemsIdMap);
            }

            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<Long, FolderInfo>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<Long, FolderInfo>();

            // Separate the items that are on the current screen, and all the other remaining items
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems,
                    otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets,
                    otherAppWidgets);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders,
                    otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };
            if (mIsInStartBinding == false) {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                mIsInStartBinding = true;
            }

            // Load items on the current page
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets,
                    currentFolders, null);

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                    // If we're profiling, ensure this is the last thing in the queue.
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound one screen " + currentScreen + " workspace in "
                            + (SystemClock.uptimeMillis()-t) + "ms");
                    }
                }
            };

            runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);

            mIsCurrentScreenBinded = true;
        }
        /*YUNOS END*/

        private void loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;
            mOrienLandedOnLoadStart = LauncherApplication.isInLandOrientation();
            // Load the workspace
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindWorkspace mWorkspaceLoaded=" + mWorkspaceLoaded);
            }
            if (!mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }

            boolean isMaxYSupported = true;
            if (sCellCountY == ConfigManager.getCellMaxCountY()) {
                //BugID: 5204254:mLauncher null pointer exception in LauncherModel
                isMaxYSupported = checkGridSize(sCellCountX, sCellCountY);
            }
            if (isMaxYSupported == true) {

                // init workspaceOccuiped array in another orientation
                if (ConfigManager.isLandOrienSupport()) {
                    initWorkspaceOccupiedNonCurrent();
                }

                // Bind the workspace
                bindWorkspace(-1);
            } else {
                changeLayout(sCellCountX, sCellCountY - 1, false, this);
//                bindWorkspace(-1);
                if (homeshellSetting != null) {
                    homeshellSetting.updateLayoutPreference();
                }
            }

            /* YUNOS BEGIN */
            //##date:2013/11/28 ##author:hongxing.whx
            // Here is backup&restore
            if (DEBUG_LOADERS) Log.d(TAG,"isInRestore="+
                    BackupManager.getInstance().isInRestore());
            /* YUNOS BEGIN */
            //##date:2013/12/08 ##author:hongxing.whx ##bugid: 72248
            // if loadWorkspace is triggered by theme changed event, we don't need inform
            // applicaiton store to download application
            if (DEBUG_LOADERS) Log.d(TAG,"mThemeChanged="+ mThemeChanged);
            if (BackupManager.getInstance().isInRestore() && !mThemeChanged) {
                if (DEBUG_LOADERS) Log.d(TAG, "In restore mode," +
                    "send a broadcast to homeshell self");
                // set it to false here, if it's will be used by others, we need redesign
                mThemeChanged = false;
            /*YUNOS END*/
                if (BackupManager.getIsRestoreAppFlag(mContext)) {
                    if (DEBUG_LOADERS) Log.d(TAG, "sendBroadcast(ACTION_HOMESHELL_BACKUP)");
                    mContext.sendBroadcast(new Intent(ACTION_HOMESHELL_BACKUP));
                    /* YUNOS BEGIN */
                    //##date:2014/03/20 ##author:nater.wg ##bugid: 102614
                    // Disable VPInstall flag when apps restore starts.
//                    if (DEBUG_LOADERS) Log.d(TAG, "Disable VPInstall flag");
// remove vp install
//                    ConfigManager.setVPInstallEnable(false);
                    /*YUNOS END*/
                }
                //BugID:6068807:need to cancel restore state if no list
                else {
                    BackupManager.setRestoreFlag(mContext, false);
                    BackupManager.getInstance().setIsInRestore(false);
                }
            }
            /* YUNSO END */

            if (sCellCountX == 4 && sCellCountY == 5) {
                changeLayout(sCellCountX, sCellCountY-1, false, null);
            }
        }

        private void waitForIdle() {
            // Wait until the either we're stopped or the other threads are done.
            // This way we don't start loading all apps until the workspace has settled
            // down.
            synchronized (LoaderTask.this) {
                final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                mHandler.postIdle(new Runnable() {
                        public void run() {
                            synchronized (LoaderTask.this) {
                                mLoadAndBindStepFinished = true;
                                if (DEBUG_LOADERS) {
                                    Log.d(TAG, "done with previous binding step");
                                }
                                LoaderTask.this.notify();
                            }
                        }
                    });

                while (!mStopped && !mLoadAndBindStepFinished && !mFlushingWorkerThread) {
                    try {
                        // Just in case mFlushingWorkerThread changes but we aren't woken up,
                        // wait no longer than 1sec at a time
                        this.wait(1000);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "waited "
                            + (SystemClock.uptimeMillis()-workspaceWaitTime)
                            + "ms for previous step to finish binding");
                }
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage < 0) {
                // Ensure that we have a valid page index to load synchronously
                throw new RuntimeException("Should not call runBindSynchronousPage() without " +
                        "valid page index");
            }
            if (!mAllAppsLoaded || !mWorkspaceLoaded) {
                // Ensure that we don't try and bind a specified page when the pages have not been
                // loaded already (we should load everything asynchronously in that case)
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (mLock) {
                if (mIsLoaderTaskRunning) {
                    // Ensure that we are never running the background loading at this point since
                    // we also touch the background collections
                    // throw new
                    // RuntimeException("Error! Background loading is already running");
                    Log.e(TAG, "runBindSynchronousPage() : Error! Background loading is already running");
                    return;
                }
            }

            // XXX: Throw an exception if we are already loading (since we touch the worker thread
            //      data structures, we can't allow any other thread to touch that data, but because
            //      this call is synchronous, we can get away with not locking).

            // The LauncherModel is static in the LauncherApplication and mHandler may have queued
            // operations from the previous activity.  We need to ensure that all queued operations
            // are executed before any synchronous binding work is done.
            mHandler.flush();

            // Divide the set of loaded items into those that we are binding synchronously, and
            // everything else that is to be bound normally (asynchronously).
            bindWorkspace(synchronousBindPage);
            // XXX: For now, continue posting the binding of AllApps as there are other issues that
            //      arise from that.
            onlyBindAllApps();
        }

        public void run() {
            synchronized (mLock) {
                mIsLoaderTaskRunning = true;
            }
            Log.d(TAG, "run loadertask");
            // Optimize for end-user experience: if the Launcher is up and // running with the
            // All Apps interface in the foreground, load All Apps first. Otherwise, load the
            // workspace first (default).
            final Callbacks cbk = mCallbacks.get();
            final boolean loadWorkspaceFirst = cbk != null ? (!cbk.isAllAppsVisible()) : true;

            keep_running: {
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to " +
                            (mIsLaunching ? "DEFAULT" : "BACKGROUND"));
                    android.os.Process.setThreadPriority(mIsLaunching
                            ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }
                if (loadWorkspaceFirst) {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 1: loading workspace");
                    loadAndBindWorkspace();
                } else {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 1: special: loading all apps");
                    loadAndBindAllApps();
                }

                if (mStopped) {
                    break keep_running;
                }

                // Whew! Hard work done.  Slow us down, and wait until the UI thread has
                // settled down.
                synchronized (mLock) {
                    if (mIsLaunching) {
                        if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to BACKGROUND");
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                waitForIdle();

                // second step
                if (loadWorkspaceFirst) {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 2: loading all apps");
                    loadAndBindAllApps();
                } else {
                    if (DEBUG_LOADERS) Log.d(TAG, "step 2: special: loading workspace");
                    loadAndBindWorkspace();
                }

                /* YUNOS BEGIN */
                // ##date:2015/4/1 ##author:sunchen.sc ##BugID:5735130
                // Comment and test
//                try {
//                    findAndFixInvalidItems();
//                } catch (Exception ex) {
//                    Log.e(TAG, "findAndFixInvalidItems exception");
//                }
                /* YUNOS END */

                //After update, set mThemeChanged to false, means the theme change finish
                setThemeChanged(false);

                // Restore the default thread priority after we are done loading items
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }

            /*YUNOS BEGIN*/
            //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
            // client yun icon lost after restore
            // Update the saved icons if necessary
            //if (DEBUG_LOADERS) Log.d(TAG, "Comparing loaded icons to database icons");
            //synchronized (sBgLock) {
            //    for (Object key : sBgDbIconCache.keySet()) {
            //        updateSavedIcon(mContext, (ShortcutInfo) key, sBgDbIconCache.get(key));
            //    }
            //    sBgDbIconCache.clear();
            //}
            /*YUNOS END*/

            /* YUNOS BEGIN */
            //##date:2014/04/11 ##author:nater.wg ##BugID: 109352
            // Send broadcast to app store.
            mContext.sendBroadcast(new Intent(ACTION_RELOAD_DOWNLOADING));
            if (DEBUG_LOADERS) Log.d(TAG, "Send reload downloading broadcast.");
            /* YUNOS END */

            // Clear out this reference, otherwise we end up holding it until all of the
            // callback runnables are done.
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                mIsLoaderTaskRunning = false;
            }
        }

        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
            }
        }

        // check & update map of what's occupied; used to discard overlapping/invalid items
        private boolean checkItemPlacement(ItemInfo occupied[][][], ItemInfo item) {
            checkRunOnWorkerThread();
            Log.d(TAG, "checkItemPlacement start");
            int containerIndex = item.screen;
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                // Return early if we detect that an item is under the hotseat button
                /* YUNOS BEGIN */
                // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
                if (mCallbacks == null) {
                    return false;
                }
                Callbacks cb = mCallbacks.get();
                if (cb == null || cb.isAllAppsButtonRank(item.screen)) {
                    return false;
                }
                /* YUNOS END */
                // We use the last index to refer to the hotseat and the screen as the rank, so
                // test and update the occupied state accordingly
                if (occupied[mMaxIconScreenCount][item.screen][0] != null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY
                        + ") occupied by " + occupied[mMaxIconScreenCount][item.screen][0]);
                    return false;
                } else {
                    occupied[mMaxIconScreenCount][item.screen][0] = item;
                    return true;
                }
            }
            /*YUNOS BEGIN*/
            //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
            //add hide icon container
            if (item.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                // Return early if we detect that an item is under the hotseat button
                /* YUNOS BEGIN */
                // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
                if (mCallbacks == null) {
                    return false;
                }
                Callbacks cb = mCallbacks.get();
                if (cb == null || cb.isAllAppsButtonRank(item.screen)) {
                    return false;
                }
                /* YUNOS END */
                /* YUNOS BEGIN */
                // ##date:2014/11/7 ##author:zhanggong.zg ##BugID:5567095
                // Hide-seat items will be fully checked in method reorderHideseatItemsInDB()
                return true;
                /*
                int screenIndex = mMaxScreenCount + 1 + item.screen;
                // We use the last index to refer to the hotseat and the screen as the rank, so
                // test and update the occupied state accordingly
                if (occupied[screenIndex][item.cellX][item.cellY] != null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY
                        + ") occupied by " + occupied[screenIndex][item.cellX][item.cellY]);
                    return false;
                } else {
                    occupied[screenIndex][item.cellX][item.cellY] = item;
                    return true;
                }
                */
                /* YUNOS END */
            }

            /*YUNOS END*/
            else if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                // Skip further checking if it is not the hotseat or workspace container
                return true;
            }

            // Check if any workspace icons overlap with each other
            for (int x = item.cellX; x < (item.cellX+item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY+item.spanY); y++) {
                    if (occupied[containerIndex][x][y] != null) {
                        Log.e(TAG, "Error loading shortcut " + item
                            + " into cell (" + containerIndex + "-" + item.screen + ":"
                            + x + "," + y
                            + ") occupied by "
                            + occupied[containerIndex][x][y]);
                        return false;
                    }
                }
            }
            for (int x = item.cellX; x < (item.cellX+item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY+item.spanY); y++) {
                    occupied[containerIndex][x][y] = item;
                }
            }

            return true;
        }

        /*YUNOS BEGIN*/
        //##date:2013/12/09 ##author:hao.liuhaolh ##BugID:72771
        //download item error
        private boolean findAndUpdateDownloadItem(ResolveInfo resolveInfo) {
            Log.d(TAG, "findAndUpdateDownloadItem in");
            ApplicationInfo app = new ApplicationInfo(mContext.getPackageManager(), resolveInfo,null);
            ArrayList<ItemInfo> allItems = getAllAppItems();
            for (ItemInfo info: allItems) {
                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
                    if(app.componentName.getPackageName().equals(((ShortcutInfo)info).intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME))) {
                        Log.d(TAG, "it is a downloading item");
                        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                        info.setIsNewItem(true);
                        info.title = app.title;

                        ((ShortcutInfo)info).intent = app.intent;
                        ((ShortcutInfo)info).setAppDownloadStatus(AppDownloadStatus.STATUS_INSTALLED);

                        LauncherApplication application = (LauncherApplication)mContext.getApplicationContext();
                        //add by dongjun for IconManager
                        Drawable icon = mApp.getIconManager().getAppUnifiedIcon(app,null);
                        ((ShortcutInfo) info).setIcon(icon);

                        /*YUNOS BEGIN*/
                        //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
                        // fato sd app icon error
                        ((ShortcutInfo)info).isSDApp = Utils.isSdcardApp(resolveInfo.activityInfo.applicationInfo.flags)?1:0;
                        /*YUNOS END*/
                        ((ShortcutInfo)info).customIcon = (((ShortcutInfo)info).isSDApp==0)?false:true;
                        final ContentValues values = new ContentValues();
                        final ContentResolver cr = mContext.getContentResolver();
                        info.onAddToDatabase(values);

                        //BugID:122827:sdcard app icon wrong on theme change when sdcard unmount
                        if (((ShortcutInfo)info).isSDApp == 1) {
                            Drawable origIcon = IconUtils.getAppOriginalIcon(mContext, (ShortcutInfo)info);
                            if (origIcon != null) {
                                ItemInfo.writeBitmap(values, origIcon);
                            } else {
                                if(values.containsKey(LauncherSettings.Favorites.ICON)) {
                                    values.remove(LauncherSettings.Favorites.ICON);
                                }
                            }
                        } else {
                            values.put(LauncherSettings.Favorites.ICON, new byte[0]);
                        }

                        cr.update(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values,
                                "_id =?", new String[] { String.valueOf(info.id) });
                        return true;
                    }
                }
            }
            Log.d(TAG, "findAndUpdateDownloadItem out");
            return false;
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/4/05 ##author:hongxing.whx ##BugID:116588
        //wait at most 2 seconds to make sure that theme service starts
        private void waitThemeService() {
            final PackageManager pm = mContext.getPackageManager();
            boolean supportTheme = false;
            int count = 0;
            if (pm != null) {
                try {
                    pm.getPackageInfo("com.yunos.theme.themeservice", PackageManager.GET_SERVICES);
                    supportTheme = true;
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (supportTheme) {
                Object srv = ACA.ServiceManager.getService("assetredirectionex");
                Log.d(TAG, "theme service existed:" + srv);
                while (count < 10) {
                    if (srv == null) {
                        try {
                            Thread.sleep(200);
                        } catch (Exception e) {
                        }
                        srv = ACA.ServiceManager.getService("assetredirectionex");
                        count ++;
                        Log.d(TAG, "after sleep "+ (200*count)+"ms, theme service existed:" + srv);
                    } else {
                        break;
                    }
                }
            }
        }
        /* YUNOS END */
        private void loadWorkspace() {
            Log.d(TAG, "loadWorkspace in");
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();
            boolean isWorkspaceContainAliWidget = false;

            // Make sure the default workspace is loaded, if needed
            mApp.getLauncherProvider().loadDefaultFavoritesIfNecessary(0);

            /* YUNOS BEGIN */
            // ##gadget
            // ##date:2014/03/07 ##author:zhangqiang.zq
            final Map<String, GadgetInfo> gadgets = ThemeUtils
                    .listGadgets(mContext);
            /* YUNOS END */

            synchronized (sBgLock) {
                sBgWorkspaceItems.clear();
                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                /*YUNOS BEGIN*/
                //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
                // client yun icon lost after restore
                //sBgDbIconCache.clear();
                /*YUNOS END*/
                /*YUNOS BEGIN LH handle installed app no space case*/
                sBgNoSpaceItems.clear();
                /*YUNOS END*/
                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();
                /*YUNOS BEGIN*/
                //##date:2013/12/15 ##author:hao.liuhaolh ##BugID: 73737
                // SD card app icon lost
                Log.d(TAG, "before get all app list");
                if(appsOnBoot!=null){
                    appsOnBoot.clear();
                    appsOnBoot = null;
                }
                appsOnBoot = AllAppsList.getAllActivity(context);
                AppCloneManager.getInstance().addAllCloneResolveInfo(appsOnBoot);
                final List<ResolveInfo> itemsAllApps = new ArrayList<ResolveInfo>(appsOnBoot);
                mAllCloneAppList = AppCloneManager.getInstance().getAllCloneApp();
                /*YUNOS END*/
                Log.d(TAG, "after get all app list");
                final List<ShortcutInfo> itemsInDBForApp = new ArrayList<ShortcutInfo>();
                final List<ResolveInfo>  itemsNotInDB = new ArrayList<ResolveInfo>();

                /*YUNOS BEGIN*/
                //##module(HomeShell)
                //##date:2014/04/17##author:hao.liuhaolh@alibaba-inc.com##BugID:111614
                //load and display current screen first during homeshell start
                final Callbacks oldCallbacks = mCallbacks.get();
                if (oldCallbacks == null) {
                    // This launcher has exited and nobody bothered to tell us.  Just bail.
                    Log.w(TAG, "LoaderTask running with no launcher");
                    return;
                }

                final int currentScreen = 0;//oldCallbacks.getCurrentWorkspaceScreen();

                //BugID: 118887:empty folder in first screen not removed in restore
                final List<FolderInfo> folderToRemove = new ArrayList<FolderInfo>();
                loadCurrentScreenWorkspace(currentScreen, itemsToRemove, itemsAllApps,
                                                         itemsInDBForApp, folderToRemove);
                boolean isMaxYSupported = true;
                if (sCellCountY == ConfigManager.getCellMaxCountY()) {
                    //BugID: 5204254:mLauncher null pointer exception in LauncherModel
                    isMaxYSupported = checkGridSize(sCellCountX, sCellCountY);
                }
                if (isMaxYSupported == true) {
                    bindCurrentScreenWorkspace(currentScreen);
                }
                /*YUNOS END*/

                //##date:2014/04/30##author:hao.liuhaolh@alibaba-inc.com##BugID:115612
                //##date:2014/05/05##author:hao.liuhaolh@alibaba-inc.com##BugID:116612
                //##date:2014/08/15##author:zhanggong.zg##BugID:5186578
                final Cursor c = contentResolver.query(
                                        LauncherSettings.Favorites.CONTENT_URI,
                                        null,
                                        "container<>? and (screen<>? or container=?) and container<?",
                                        new String[] {
                                            String.valueOf(-101),
                                            String.valueOf(currentScreen),
                                            String.valueOf(Favorites.CONTAINER_HIDESEAT),
                                            String.valueOf(0)},
                                        null);

                // +1 for the hotseat (it can be larger than the workspace)
                // Load workspace in reverse order to ensure that latest items are loaded first (and
                // before any earlier duplicates)

                /*YUNOS BEGIN*/
                //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                //add hide icon container
                // +1 for hotseat , +6 for hideseat
                /* HIDESEAT_SCREEN_NUM_MARKER: see ConfigManager.java */
                final ItemInfo occupied[][][] =
                        new ItemInfo[mMaxIconScreenCount+ 1 + ConfigManager.getHideseatScreenMaxCount()][sCellCountX + 1][sCellCountY + 1];
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##date:2014/08/15 ##author:zhanggong.zg ##BugID:5186578
                // used to reorder the items in hide-seat later
                final List<ShortcutInfo> hideseatItems = new ArrayList<ShortcutInfo>();
                /*YUNOS END*/
                try {
                    final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.INTENT);
                    final int titleIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.TITLE);
                    final int iconTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_TYPE);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    final int iconPackageIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_PACKAGE);
                    final int iconResourceIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_RESOURCE);
                    final int containerIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ITEM_TYPE);
                    final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.APPWIDGET_ID);
                    final int screenIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SCREEN);
                  //for pad land mode
                    final int cellXLandIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLXLAND);
                    final int cellYLandIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLYLAND);
                    final int cellXPortIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                    final int cellYPortIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

                    final int cellXIndex;
                    final int cellYIndex;
                    if (!mOrienLandedOnLoadStart) {
                        cellXIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                        cellYIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                    } else {
                        cellXIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLXLAND);
                        cellYIndex = c
                                .getColumnIndexOrThrow(LauncherSettings.Favorites.CELLYLAND);
                    }
                    final int spanXIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SPANY);
                    final int msgNumIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.MESSAGE_NUM);
                    final int isNewIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.IS_NEW);
                    final int canDeleteIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.CAN_DELEDE);
                    /*YUNOS BEGIN*/
                    //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
                    // fato sd app icon error
                    final int isSDAppIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.IS_SDAPP);
                    /*YUNOS END*/
                    //final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                    //final int displayModeIndex = c.getColumnIndexOrThrow(
                    //        LauncherSettings.Favorites.DISPLAY_MODE);
                    final int userIdIndex = c
                            .getColumnIndexOrThrow(LauncherSettings.Favorites.USER_ID);

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    int container;
                    long id;
                    Intent intent;

                    while (!mStopped && c.moveToNext()) {
                        try {
                            int itemType = c.getInt(itemTypeIndex);
                            id = c.getLong(idIndex);
                            /*YUNOS BEGIN*/
                            //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 78360
                            //check item's position is reasonable
                            if (c.getInt(containerIndex) == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                if ((c.getInt(cellXIndex) >= sCellCountX) ||
                                     (c.getInt(cellYIndex) >= sCellCountY) ||
                                     (c.getInt(spanXIndex) + c.getInt(cellXIndex) > sCellCountX) ||
                                     (c.getInt(spanYIndex) + c.getInt(cellYIndex) > sCellCountY) ||
                                     (c.getInt(screenIndex) >= mMaxIconScreenCount)) {
                                    itemsToRemove.add(id);
                                    Log.d(TAG, "item position error, id is " + id);
                                    continue;
                                }
                            }
                            /*YUNOS END*/

                            switch (itemType) {
                            /*YUNOS BEGIN LH handle installed app no space case*/
                            case LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION:
                            /*YUNOS END*/
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                            /*YUNOS BEGIN*/
                            //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
                            //vp install
// remove vp install
//                            case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
                            /*YUNOS END*/
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (URISyntaxException e) {
                                    itemsToRemove.add(id);
                                    continue;
                                }
                                //remove all download item when age mode change
                                if ((itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) && (mClearAllDownload == true)) {
                                    itemsToRemove.add(id);
                                    continue;
                                }
                                /*YUNOS BEGIN*/
                                //##date:2014/03/18 ##author:hao.liuhaolh ##BugID:101711
                                //vp install item icon is default icon after restore
// remove vp install
//                                if (itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
//                                    Log.d(TAG, "start check vp install item");
//                                    PackageInfo packageInfoInstall = null;
//                                    try {
//                                        packageInfoInstall = manager.getPackageInfo(intent.getStringExtra("packagename"), 0);
//                                    } catch (NameNotFoundException e) {
//                                        Log.d(TAG, "the package is not installed");
//                                    }
//                                    if (packageInfoInstall != null) {
//                                        Log.d(TAG, packageInfoInstall.packageName + " is installed");
//                                        itemsToRemove.add(id);
//                                        continue;
//                                    }
//                                    /*YUNOS BEGIN*/
//                                    //##module(HomeShell)
//                                    //##date:2014/04/14 ##author:hao.liuhaolh@alibaba-inc.com##BugID:110491
//                                    //add component name in vp item's intent for iconmanager
//                                    if (intent == null) {
//                                        itemsToRemove.add(id);
//                                        continue;
//                                    }
//                                    if ((intent.getComponent() == null) || (intent.getComponent().equals(""))){
//                                        Log.d(TAG, "component is null");
//                                        ComponentName compName = new ComponentName(intent.getStringExtra(VPUtils.TYPE_PACKAGENAME), "vpinstall");
//                                        intent.setComponent(compName);
//                                    }
//                                    /*YUNOS END*/
//                                }
                                /*YUNOS END*/

                                /*YUNOS BEGIN LH handle installed app no space case*/
                                if ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) ||
                                    (itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION)) {
                                /*YUNOS END*/

                                    /* YUNOS BEGIN */
                                    //##date:2013/12/4 ##author:hongxing.whx ##bugid:  69775
                                    // To check if application is installed now in restore mode
                                    /*YUNOS BEGIN*/
                                    //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
                                    // fato sd app icon error
                                    ResolveInfo appInfo = AllAppsList.findActivityInfo(itemsAllApps, intent.getComponent());
                                    if (appInfo == null) {
                                        /* YUNOS BEGIN */
                                        // ##date:2014/07/21 ##author:zhanggong.zg ##BugID:5244146
                                        // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
                                        if (Hideseat.isHideseatEnabled() &&
                                            intent != null && intent.getComponent() != null &&
                                            AppFreezeUtil.isPackageFrozen(mContext, intent.getComponent().getPackageName())) {
                                            // ignore frozen apps
                                        } else {
                                            /*YUNOS BEGIN*/
                                            //##date:2014/01/10 ##author:hao.liuhaolh ##BugID: 84736
                                            // SD app icon position change in restore
                                            if (c.getInt(isSDAppIndex) == 0) {
                                                itemsToRemove.add(id);
                                                continue;
                                            } else if (BackupManager.getInstance().isInRestore()) {
                                                // TODO need to check if it's an app installed on sdcard
                                                // if not installed on sdcard, remove item from db
                                                if ((intent == null) || (intent.getComponent() == null) ||
                                                     (BackupUitil.isInRestoreList(context, intent.getComponent().getPackageName()) == true)) {
                                                    Log.d(TAG, "sd app and in restore list");
                                                    itemsToRemove.add(id);
                                                    continue;
                                                }
                                            }
                                            /*YUNOS END*/
                                        }
                                        /*YUNOS END*/
                                    }
                                    /*YUNOS END*/
                                    /* YUNOS END */
                                    int userId = c.getInt(userIdIndex);
                                    /*YUNOS BEGIN*/
                                    //##module(HomeShell)
                                    //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
                                    //check restore data to avoid no item in dock
                                    if (userId == 0) {
                                        if ((intent == null) || (allComponentList.contains(intent.toString()))) {
                                            itemsToRemove.add(id);
                                            continue;
                                        } else {
                                            allComponentList.add(intent.toString());
                                        }
                                    } else if (userId > 0) {
                                        if (!AppCloneManager.isSupportAppClone()) {
                                            continue;
                                        } else if (intent != null) {
                                            CloneAppKey key = new CloneAppKey(userId, intent.getComponent().getPackageName());
                                            if (mAllCloneAppList.contains(key)) {
                                                mAllCloneAppList.remove(key);
                                            } else {
                                                itemsToRemove.add(id);
                                                continue;
                                            }
                                        } else {
                                            itemsToRemove.add(id);
                                            continue;
                                        }
                                    }
                                     /*YUNOS END*/
                                    info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                            titleIndex, mLabelCache);
                                    info.itemType = itemType;
                                    info.userId = userId;
                                } else {
                                    info = getShortcutInfo(c, context, iconTypeIndex,
                                            iconPackageIndex, iconResourceIndex, iconIndex,
                                            titleIndex);

                                    /* YUNOS BEGIN */
                                    //##date:2014/04/04 ##author:nater.wg ##BugID: 107837
                                    // Update the title of VP install apps.
// remove vp install
//                                    String appPackageFilePath = intent.getStringExtra(VPUtils.TYPE_PACKAGEPATH);
//                                    if (!TextUtils.isEmpty(appPackageFilePath)) {
//                                        CharSequence vpLabel = VPUtils.getVpinstallLabel(appPackageFilePath);
//                                        if (!TextUtils.isEmpty(vpLabel)) {
//                                            info.title = vpLabel;
//                                        }
//                                    }
                                    /* YUNOS END */

                                    // App shortcuts that used to be automatically added to Launcher
                                    // didn't always have the correct intent flags set, so do that
                                    // here
                                    if (intent.getAction() != null &&
                                        intent.getCategories() != null &&
                                        intent.getAction().equals(Intent.ACTION_MAIN) &&
                                        intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                        intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                    }
                                }

                                if (info != null) {
                                    info.intent = intent;
                                    info.id = c.getLong(idIndex);
                                    container = c.getInt(containerIndex);
                                    info.container = container;
                                    info.screen = c.getInt(screenIndex);
//                                    if(ConfigManager.isLandOrienSupport()) {
                                        //for pad land mode
                                        info.cellXLand = c.getInt(cellXLandIndex);
                                        info.cellYLand = c.getInt(cellYLandIndex);
                                        info.cellXPort = c.getInt(cellXPortIndex);
                                        info.cellYPort = c.getInt(cellYPortIndex);
                                        //info.initCellXYOnLoad((int) info.container, info.cellXPort, info.cellYPort, info.cellXLand, info.cellYLand);
                                        info.cellX = c.getInt(cellXIndex);
                                        info.cellY = c.getInt(cellYIndex);
                                        info.dumpXY(" loadWorkspace ");
//                                    } else {
//                                        info.cellX = c.getInt(cellXIndex);
//                                        info.cellY = c.getInt(cellYIndex);
//                                    }
                                    info.isNew = c.getInt(isNewIndex);
                                    info.messageNum = c.getInt(msgNumIndex);
                                    /* YUNOS BEGIN */
                                    // ## date: 2016/06/29 ## author: yongxing.lyx
                                    // ## BugID:8471126:show paused after changed language when downloaded app is installing.
                                    updateDownloadStatus(info);
                                    /* YUNOS END */
                                    // check & update map of what's occupied
                                    /*YUNOS BEGIN LH handle installed app no space case*/
                                    if (itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                        if ((info.screen < 0) || (info.cellX < 0) || (info.cellY < 0) ||
                                            (!checkItemPlacement(occupied, info))) {
                                            //if the item place is occupied by other item,
                                            //or it's position invalid,
                                            //remove it from db, if the item is an app,
                                            //it can be added in below operation,
                                            //other types will not be recovered
                                            itemsToRemove.add(info.id);
                                            break;
                                        }
                                    } else {
                                        Log.d(TAG, "it is a no space item");
                                    }
                                    /*YUNOS END*/

                                    /*YUNOS BEGIN*/
                                    // ##date:2014/11/3 ##author:zhanggong.zg ##BugID:5427552
                                    // Check whether the item is frozen or not. frozen item will
                                    // be added to hideseat in method reorderHideseatItemsInDB().
                                    if (container == Favorites.CONTAINER_HIDESEAT ||
                                        (AppFreezeUtil.isPackageFrozen(mContext, info) &&
                                                (itemType == Favorites.ITEM_TYPE_APPLICATION ||
                                                 itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION ||
                                                 itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING))) {
                                        sBgWorkspaceItems.add(info);
                                        hideseatItems.add(info);
                                    }
                                    /*YUNOS END*/
                                    else {
                                        switch (container) {
                                        case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                        case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                        /*YUNOS BEGIN*/
                                        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                                        //add hide icon container
                                        case LauncherSettings.Favorites.CONTAINER_HIDESEAT:
                                        /*YUNOS END*/
                                            /*YUNOS BEGIN LH handle installed app no space case*/
                                            if (itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                                sBgNoSpaceItems.add(info);
                                            } else {
                                                sBgWorkspaceItems.add(info);
                                            }
                                            /*YUNOS END*/
                                            break;
                                        default:
                                            //if an item in a folder, it's container is folder's id
                                            //so the item's container must not less than 0
                                            if (container >= 0) {
                                                // Item is in a user folder
                                                FolderInfo folderInfo =
                                                        findOrMakeFolder(sBgFolders, container);
                                                folderInfo.add(info);
                                            }
                                            break;
                                        }
                                    }
                                    sBgItemsIdMap.put(info.id, info);

                                    // if the shortcut is for applicaiton, add it to itemsInDBForApp
                                    /*YUNOS BEGIN LH handle installed app no space case*/
                                    if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) ||
                                        (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION)){
                                    /*YUNOS END*/
                                        itemsInDBForApp.add(info);
                                    }

                                    //BugID:5614377:create card background in worker thread
                                    if (info.itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                        if (mIconManager.supprtCardIcon() == true) {
                                            mIconManager.getAppCardBackgroud(info);
                                        }
                                    }
                                    // now that we've loaded everthing re-save it with the
                                    // icon in case it disappears somehow.
                                    /*YUNOS BEGIN*/
                                    //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
                                    // client yun icon lost after restore
                                    //queueIconToBeChecked(sBgDbIconCache, info, c, iconIndex);
                                    /*YUNOS END*/
                                } else {
                                    // Failed to load the shortcut, probably because the
                                    // activity manager couldn't resolve it (maybe the app
                                    // was uninstalled), or the db row was somehow screwed up.
                                    // Delete it.
                                    id = c.getLong(idIndex);
                                    Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                                    contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                                id, false), null, null);
                                }
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                id = c.getLong(idIndex);
                                FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                folderInfo.title = c.getString(titleIndex);
                                /* YUNOS BEGIN */
                                //##date:2013/11/25 ##author:hongxing.whx ##bugid: 66169
                                updateFolderTitle(folderInfo);
                                /* YUNOS END */
                                folderInfo.id = id;
                                container = c.getInt(containerIndex);
                                folderInfo.container = container;
                                folderInfo.screen = c.getInt(screenIndex);

//                                if(ConfigManager.isLandOrienSupport()) {
                                    //for pad land mode
                                    folderInfo.cellXLand = c.getInt(cellXLandIndex);
                                    folderInfo.cellYLand = c.getInt(cellYLandIndex);
                                    folderInfo.cellXPort = c.getInt(cellXPortIndex);
                                    folderInfo.cellYPort = c.getInt(cellYPortIndex);
                                    //folderInfo.initCellXYOnLoad((int) folderInfo.container, folderInfo.cellXPort, folderInfo.cellYPort, folderInfo.cellXLand, folderInfo.cellYLand);
                                    folderInfo.cellX = c.getInt(cellXIndex);
                                    folderInfo.cellY = c.getInt(cellYIndex);
                                    folderInfo.dumpXY("loadWorspace Folder ");
//                                } else {
//                                    folderInfo.cellX = c.getInt(cellXIndex);
//                                    folderInfo.cellY = c.getInt(cellYIndex);
//                                }

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, folderInfo)) {
                                    itemsToRemove.add(folderInfo.id);
                                    if (sBgFolders.containsKey(folderInfo.id)) {
                                        sBgFolders.remove(folderInfo.id);
                                    }
                                    if (sBgItemsIdMap.containsKey(folderInfo.id)) {
                                        sBgItemsIdMap.remove(folderInfo.id);
                                    }
                                    break;
                                }
                                switch (container) {
                                    case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                    case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                    /*YUNOS BEGIN*/
                                    //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                                    //add hide icon container
                                    case LauncherSettings.Favorites.CONTAINER_HIDESEAT:
                                    /*YUNOS END*/
                                        sBgWorkspaceItems.add(folderInfo);
                                        break;
                                }

                                sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                sBgFolders.put(folderInfo.id, folderInfo);
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                // Read all Launcher-specific widget details
                                int appWidgetId = c.getInt(appWidgetIdIndex);
                                /* YUNOS BEGIN */
                                // ##date:2013/12/03 ##author:hongxing.whx ##bugid: 69481
                                // to restore widget which is bound to the system application
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (URISyntaxException e) {
                                    itemsToRemove.add(id);
                                    continue;
                                }

                                AppWidgetProviderInfo provider =
                                        widgets.getAppWidgetInfo(appWidgetId);
                                boolean needUpdateWidget = false;
                                if (BackupManager.getInstance().isInRestore()) {
                                    try {
                                        Log.d(TAG, "hongixng, widget loading in restore mode");
                                        AppWidgetHost host = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);
                                        int tmpID = host.allocateAppWidgetId();
                                        Log.d(TAG, String.format("widget id changes from %d to %d", appWidgetId, tmpID));
                                        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
                                        ACA.AppWidgetManager.bindAppWidgetId(appWidgetManager, tmpID, intent.getComponent());
                                        appWidgetId = tmpID;
                                        provider = widgets.getAppWidgetInfo(appWidgetId);
                                        needUpdateWidget = true;
                                    } catch (Exception e) {
                                        Log.d(TAG, "restore system's widget met:" + e);
                                    }
                                }
                                /* YUNOS END */

                                if (!isSafeMode && (provider == null || provider.provider == null ||
                                        provider.provider.getPackageName() == null)) {
                                    String log = "Deleting widget that isn't installed anymore: id="
                                        + id + " appWidgetId=" + appWidgetId;
                                    Log.e(TAG, log);
                                    //Launcher.sDumpLogs.add(log);
                                    itemsToRemove.add(id);
                                } else {
                                    appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId,
                                            provider.provider);
                                    appWidgetInfo.id = id;
                                    appWidgetInfo.screen = c.getInt(screenIndex);
                                    //for pad land mode
//                                    if(ConfigManager.isLandOrienSupport()) {
                                        appWidgetInfo.cellXLand = c.getInt(cellXLandIndex);
                                        appWidgetInfo.cellYLand = c.getInt(cellYLandIndex);
                                        appWidgetInfo.cellXPort = c.getInt(cellXPortIndex);
                                        appWidgetInfo.cellYPort = c.getInt(cellYPortIndex);
                                        appWidgetInfo.setCellXY();
//                                    } else {
//                                        appWidgetInfo.cellX = c.getInt(cellXIndex);
//                                        appWidgetInfo.cellY = c.getInt(cellYIndex);
//                                    }
                                    appWidgetInfo.spanX = c.getInt(spanXIndex);
                                    appWidgetInfo.spanY = c.getInt(spanYIndex);
                                    int[] minSpan = Launcher.getMinSpanForWidget(context, provider);
                                    appWidgetInfo.minSpanX = minSpan[0];
                                    appWidgetInfo.minSpanY = minSpan[1];

                                    container = c.getInt(containerIndex);
                                    if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                                        container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                                        /*YUNOS BEGIN*/
                                        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
                                        //add hide icon container
                                        container != LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                                        /*YUNOS END*/
                                        Log.e(TAG, "Widget found where container != " +
                                            "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT nor CONTAINER_HIDESEAT- ignoring!");
                                        continue;
                                    }
                                    appWidgetInfo.container = c.getInt(containerIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, appWidgetInfo)) {
                                        break;
                                    }
                                    sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                    sBgAppWidgets.add(appWidgetInfo);

                                    /* YUNOS BEGIN */
                                    // ##date:2015/6/11 ##author:zhanggong.zg ##BugID:6050280
                                    // Widget id is reallocated after restore.
                                    if (needUpdateWidget) {
                                        updateItemInDatabase(context, appWidgetInfo);
                                    }
                                    /* YUNOS END */
                                }
                                break;
                            /* YUNOS BEGIN */
                            // ##gadget
                            // ##date:2014/02/27
                            // ##author:kerong.skr@alibaba-inc.com ##BugID:96378
                            case LauncherSettings.Favorites.ITEM_TYPE_GADGET:
                                String strType = c.getString(titleIndex);
                                    if (!TextUtils.isEmpty(strType)) {
                                        GadgetInfo ginfo = gadgets.get(strType);
                                        if (ginfo != null) {
                                            GadgetItemInfo gi = new GadgetItemInfo(ginfo);
                                            gi.id = c.getLong(idIndex);
                                            gi.screen = c.getInt(screenIndex);
//                                            if(ConfigManager.isLandOrienSupport()) {
                                                //for pad land mode
                                                gi.cellXLand = c.getInt(cellXLandIndex);
                                                gi.cellYLand = c.getInt(cellYLandIndex);
                                                gi.cellXPort = c.getInt(cellXPortIndex);
                                                gi.cellYPort = c.getInt(cellYPortIndex);
                                                gi.setCellXY();
//                                            } else {
//                                                gi.cellX = c.getInt(cellXIndex);
//                                                gi.cellY = c.getInt(cellYIndex);
//                                            }
                                            gi.spanX = c.getInt(spanXIndex);
                                            gi.spanY = c.getInt(spanYIndex);
                                            sBgWorkspaceItems.add(gi);
                                            sBgItemsIdMap.put(gi.id, gi);
                                        }
                                    }
                                break;
                            /* YUNOS END */
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Desktop items loading interrupted:", e);
                        }
                    }
                } finally {
                    c.close();
                }

                if (mStopped != true) {
                    /*YUNOS BEGIN*/
                    //##date:2014/08/15 ##author:zhanggong.zg ##BugID:5186578
                    // reorders the items in hide-seat.
                    try {
                        // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
                        if (Hideseat.isHideseatEnabled()) {
                            List<ShortcutInfo> out_nospaceItems = new ArrayList<ShortcutInfo>(0);
                            reorderHideseatItemsInDB(hideseatItems, out_nospaceItems);
                            // deal with no-space items. (very rare situation)
                            for (ShortcutInfo item : out_nospaceItems) {
                                if (item.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                                    item.itemType = Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                                    item.screen = item.cellX = item.cellY = -1;
                                    item.cellXLand = item.cellYLand = item.cellXPort = item.cellYPort = -1;
                                    if (sBgWorkspaceItems.remove(item)) {
                                        sBgNoSpaceItems.add(item);
                                    }
                                    updateItemInDatabase(mContext, item);
                                } else {
                                    deleteItemFromDatabase(mContext, item);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Hide-seat items reordering interrupted:", e);
                    } finally {
                        hideseatItems.clear();
                    }
                    /*YUNOS END*/

                    if (itemsToRemove.size() > 0) {
                        ContentProviderClient client = contentResolver.acquireContentProviderClient(
                                        LauncherSettings.Favorites.CONTENT_URI);
                        // Remove dead items
                        for (long id : itemsToRemove) {
                            if (DEBUG_LOADERS) {
                                Log.d(TAG, "Removed id = " + id);
                            }
                            // Don't notify content observers
                            try {
                                client.delete(LauncherSettings.Favorites.getContentUri(id, false),
                                        null, null);
                            } catch (RemoteException e) {
                                Log.w(TAG, "Could not remove id = " + id);
                            }
                        }
                        if (client != null) {
                            client.release();
                        }
                        mClearAllDownload = false;
                    }

                    // check if an application in itemsAllApps exists in itemsInDBForApp
                    if (DEBUG_LOADERS) Log.d(TAG,"hongxing: to check if any apps are not in db");
                    for(ResolveInfo app : itemsAllApps){
                        boolean find = false;
                        ComponentName appComponent = new ComponentName(app.activityInfo.packageName,
                            app.activityInfo.name);

                        for(ShortcutInfo appInDB : itemsInDBForApp) {
                            ComponentName componentName = appInDB.intent.getComponent();
                            if (appComponent != null
                                    && appComponent.equals(componentName)
                                    && (!(app instanceof CloneResolveInfo) || ((CloneResolveInfo) app).userId == appInDB.userId)) {
                                find = true;
                                break;
                            }
                        }
                        if(!find) {
                            itemsNotInDB.add(app);
                            if (DEBUG_LOADERS) Log.d(TAG, "title : " + app.loadLabel(manager)
                                    + " pkgName : " + app.activityInfo.packageName
                                    + " className : " + app.activityInfo.name);
                        }
                    }

                    /*YUNOS BEGIN*/
                    //##module(HomeShell)
                    //##date:2014/04/17##author:hao.liuhaolh@alibaba-inc.com##BugID:111614
                    //load and display current screen first during homeshell start
                    if (itemsNotInDB.size() > 0) {
                        Collections.sort(itemsNotInDB, new ResolveInfo.DisplayNameComparator(manager));
                    }
                    /*YUNOS END*/
                    boolean agedState = LauncherProvider.getDbAgedModeState();
                    boolean loadCompleted = true;
                    for(ResolveInfo app : itemsNotInDB) {
                        LauncherApplication launcherApp = (LauncherApplication) mContext.getApplicationContext();
                        /*YUNOS BEGIN*/
                        //##date:2013/12/09 ##author:hao.liuhaolh ##BugID:72771
                        //download item error
                        if (mStopped == true) {
                            loadCompleted = false;
                            break;
                        }
                        if (findAndUpdateDownloadItem(app) == false) {
                        /*YUNOS END*/

                            /*YUNOS BEGIN*/
                            //##date:2014/7/8 ##author:zhangqiang.zq
                            // aged mode
                            if (agedState) {
                                PackageManager pm = mContext
                                        .getPackageManager();
                                ApplicationInfo appInfo = new ApplicationInfo(
                                        pm, app, mLabelCache);
                                ShortcutInfo info = getShortcutInfo(pm,
                                        appInfo, context);
                                if (info == null) {
                                    continue;
                                }
                                /* YUNOS BEGIN */
                                // ## date: 2016/06/20 ## author: yongxing.lyx
                                // ## BugID:8411951:add new icon to other-app folder when swith to aged mode.
                                if (LauncherProvider.agedDefaultFolderId == -1) {
                                    FolderInfo defaultFolderInfo = getAgedModelDefaultFolder(mContext);
                                    if (defaultFolderInfo != null) {
                                        LauncherProvider.agedDefaultFolderId = defaultFolderInfo.id;
                                    }
                                }
                                /* YUNOS END */

                                // add all the other app beyond to the default
                                // app to default folder
                                if (LauncherProvider.agedDefaultFolderId != -1
                                        && sBgFolders.get(LauncherProvider.agedDefaultFolderId) != null) {
                                    FolderInfo folderInfo = sBgFolders.get(LauncherProvider.agedDefaultFolderId);
                                    moveToFolderNextPos(LauncherProvider.sMaxPosAfterLoadFav);
                                    info.container = folderInfo.id;
                                    addItemToDatabase(mContext, info, info.container,
                                            LauncherProvider.sMaxPosAfterLoadFav.s,
                                            LauncherProvider.sMaxPosAfterLoadFav.x,
                                            LauncherProvider.sMaxPosAfterLoadFav.y, false);
                                } else {
                                    ScreenPosition p = LauncherModel.findEmptyCellAndOccupy(false);
                                    if (p == null) {
                                        p = new ScreenPosition(-1, -1, -1);
                                        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                                    }
                                    info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                                    /* YUNOS BEGIN */
                                    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
                                    // Modified to support pad orientation
                                    info.cellX = p.x;
                                    info.cellY = p.y;
                                    info.screen = p.s;
                                    info.setCurrentOrientationXY(info.cellX, info.cellY);
                                    addItemToDatabase(mContext, info,
                                            info.container, p.s, p.xPort, p.yPort, p.xLand, p.yLand,
                                            false);
                                    /* YUNOS BEGIN */
                                    // ## date: 2016/05/09 ## author: yongxing.lyx
                                    // ## BugID: 8198787: some icon disappear after pull out icons
                                    // from hideseat and turn aged mode on.
                                    sBgItemsIdMap.put(info.id, info);
                                    if (currentScreen == info.screen) {
                                        mIsCurrentScreenUpdated = true;
                                    }
                                    /* YUNOS END */
                                    /* YUNOS END */
                                }
                            } else {
                            /*YUNOS END*/

                                ScreenPosition p = findEmptyCellAndOccupy(false);

                            if(p == null){
                                /*YUNOS BEGIN LH handle installed app no space case*/
                                p = new ScreenPosition(-1, -1, -1);
                                // no space now, we shall add it to nospace list
                                /*YUNOS END*/
                            }
                            PackageManager pm =mContext.getPackageManager();
                            ApplicationInfo appInfo = new ApplicationInfo(pm, app, mLabelCache);
                            ShortcutInfo info = getShortcutInfo(pm, appInfo, context);
                            if (info == null) {
                                continue;
                            }

                            info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                            /*YUNOS BEGIN LH handle installed app no space case*/
                            if (p.s == -1) {
                                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                            } else {
                                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                            }
                            /*YUNOS END*/
                            info.setPosition(p);
                            info.spanX = 1;
                            info.spanY = 1;

                            //We don't set new flag if the app is found in loadworkspace
                            info.isNew = 0;

                            /* YUNOS BEGIN */
                            // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
                            // Modified to support pad orientation
                            addItemToDatabase(mContext, info, info.container, p.s,
                                    info.cellXPort, info.cellYPort, info.cellXLand, info.cellYLand, false);
                            /* YUNOS END */
                            }
                        }
                    }
                    /* YUNOS BEGIN */
                    // ##date:2014/7/23 ##author:yangshan.ys##140049
                    // for 3*3 layout
                    // reset the defaultFolderId
                    if (agedState && loadCompleted) {
                        LauncherProvider.agedDefaultFolderId = -1;
                    }
                    /* YUNOS END */
                    /* YUNOS BEGIN */
                    // ##date:2014/07/23 ##author:zhanggong.zg ##BugID:5244146
                    // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
                    // check all frozen apps that are not found in database, and put them
                    // into hide-seat.
                    List<ApplicationInfo> allFrozenApps = null;
                    if (Hideseat.isHideseatEnabled()) {
                        allFrozenApps = AppFreezeUtil.getAllFrozenApps(context);
                    } else {
                        allFrozenApps = Collections.emptyList();
                    }
                    for (ApplicationInfo info : allFrozenApps) {
                        boolean foundInDB = false;
                        if (info.componentName == null) continue;
                        String pkgName = info.componentName.getPackageName();
                        String clsName = info.componentName.getClassName();
                        for(ShortcutInfo appInDB : itemsInDBForApp) {
                            ComponentName componentName = appInDB.intent.getComponent();
                            if (componentName == null) continue;
                            if (componentName.equals(info.componentName) ||
                                (TextUtils.isEmpty(clsName) && componentName.getPackageName().equals(pkgName))) {
                                foundInDB = true;
                                break;
                            }
                        }
                        if (!foundInDB) {
                            // the app is not found in database
                            /* YUNOS BEGIN */
                            // ## date: 2016/05/07 ## author: yongxing.lyx
                            // ## BugID: 8185562: some icons in hideseat will
                            // disappear after clear HomeShell data.
                            List<ResolveInfo> resolveInfos = AllAppsList
                                    .findDisabledActivitiesForPackage(context, pkgName);
                            for (ResolveInfo rInfo : resolveInfos) {
                                Intent intent = new Intent(Intent.ACTION_MAIN);
                                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                                ComponentName cn = new ComponentName(
                                        rInfo.activityInfo.packageName,
                                        rInfo.activityInfo.name);
                                intent.setComponent(cn);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                ShortcutInfo si = getShortcutInfo(context.getPackageManager(),
                                        intent, context);
                                if (si == null) {
                                    continue;
                                }
                                si.container = LauncherSettings.Favorites.CONTAINER_HIDESEAT;
                                ScreenPosition p = LauncherModel.findEmptyCellInHideSeat(false);
                                if (p == null) {
                                    // MARK
                                    // no more sapce in hide-seat now,
                                    // completely ignore the app
                                    // p = new ScreenPosition(-1, -1, -1);
                                    continue;
                                }
                                si.screen = p.s;
                                si.cellX = p.x;
                                si.cellY = p.y;
                                si.spanX = 1;
                                si.spanY = 1;
                                si.isNew = 0;
                                addItemToDatabase(mContext, si, si.container, p.s, p.x, p.y, false);
                            }
                            /* YUNOS END */
                        }
                    }
                    /* YUNOS BEGIN */

                    //vp install
                    //init vpinstall after load and bind finished
// remove vp install
//                    sWorker.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (ConfigManager.isVPInstallEnable() == true) {
//                                ConfigManager.setVPInstallEnable(false);
//                                vpInstallInit();
//                            }
//                        }
//                    });

//                    sWorker.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            vpInstallItemsCheck();
//                        }
//                    });

                    /*YUNOS BEGIN*/
                    //##date:2013/12/16 ##author:hao.liuhaolh ##BugID: 75596
                    // find and remove empty folder
                    /*YUNOS BEGIN*/
                    //##date:2014/03/01 ##author:hao.liuhaolh ##BugID: 75596
                    // empty folder in restoring
                    //in restore state, remove the empty folder too, and recreate it when it is needed
                    //if (BackupManager.getInstance().isInRestore() == false) {
                    /*YUNOS END*/
                        Log.d(TAG, "check empty folder");
                        for (FolderInfo info: sBgFolders.values()) {
                            if ((info != null) && (info.contents != null)) {
                                Log.d(TAG, "folder " + info.title + " size is " + info.contents.size());
                                if (info.contents.size() == 0) {
                                    if(folderToRemove.contains(info) == false) {
                                        folderToRemove.add(info);
                                    }
                                }

                                /*YUNOS BEGIN*/
                                //##date:2014/02/28 ##author:hao.liuhaolh ##BugID:95951
                                //folder that only one item in it
                                else if((info.contents.size() == 1) &&
                                           //BugID:112962:don't delete one item folder in restore state
                                           (BackupManager.getInstance().isInRestore() == false)){
                                    ShortcutInfo itemInFolder = info.contents.get(0);
                                    if (itemInFolder != null) {
                                        //replace the folder with the item in it
                                        itemInFolder.cellX = info.cellX;
                                        itemInFolder.cellY = info.cellY;
                                        //for pad land mode
                                        itemInFolder.cellXLand = info.cellXLand;
                                        itemInFolder.cellYLand = info.cellYLand;
                                        itemInFolder.cellXPort = info.cellXPort;
                                        itemInFolder.cellYPort = info.cellYPort;
                                        itemInFolder.container = info.container;
                                        itemInFolder.screen = info.screen;
                                        updateItemInDatabase(mContext, itemInFolder);
                                    }
                                    if(folderToRemove.contains(info) == false) {
                                        folderToRemove.add(info);
                                    }
                                }
                                /*YUNOS END*/
                            }
                        }

                        for (FolderInfo info: folderToRemove) {
                            final ItemInfo iteminfo = (ItemInfo) info;
                            Log.d(TAG, "remove folder " + info.title);
                            deleteItemFromDatabase(mApp, iteminfo);
                        }
                    //}
                    /*YUNOS END*/
                }

                // to clear
                folderToRemove.clear();
                itemsToRemove.clear();
                itemsAllApps.clear();
                itemsInDBForApp.clear();
                itemsNotInDB.clear();
                allComponentList.clear();
                mAllCloneAppList.clear();
                /* YUNOS BEGIN */
                // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6348948
                // Smart app recommendation (to record app launch times and counts)
                AppLaunchManager appLaunch = AppLaunchManager.getInstance();
                appLaunch.initialize(Collections.unmodifiableMap(sBgItemsIdMap));
                /* YUNOS END */
                Log.d(TAG, "loadWorkspace change layout to 4x4");


                if (DEBUG_LOADERS) {
                    Log.d(TAG, "loaded workspace in " + (SystemClock.uptimeMillis()-t) + "ms");
                    Log.d(TAG, "workspace layout: ");
                    for (int y = 0; y < sCellCountY; y++) {
                        String line = "";
                        for (int s = 0; s < mMaxIconScreenCount; s++) {
                            if (s > 0) {
                                line += " | ";
                            }
                            for (int x = 0; x < sCellCountX; x++) {
                                line += ((occupied[s][x][y] != null) ? "#" : ".");
                            }
                        }
                        Log.d(TAG, "[ " + line + " ]");
                    }
                }
            }
            Log.d(TAG, "loadWorkspace out");
        }

        /*YUNOS BEGIN*/
        //##date:2014/08/15 ##author:zhanggong.zg ##BugID:5186578
        private void reorderHideseatItemsInDB(List<ShortcutInfo> hideseatItems, List<ShortcutInfo> out_nospaceItems) {
            // sort items by their current positions
            Collections.sort(hideseatItems, Hideseat.sItemOrderComparator);
            Iterator<ShortcutInfo> itr = hideseatItems.listIterator();
            // check positions one by one
            for (ScreenPosition pos : Hideseat.getScreenPosGenerator()) {
                if (itr.hasNext()) {
                    ShortcutInfo item = itr.next();
                    if (item.screen != pos.s ||
                        item.cellX != pos.x || item.cellY != pos.y) {
                        // position mismatch, need to update database
                        Log.w("Hideseat", String.format("reorderHideseatItemsInDB item %s: (%d,%d,%d) should be (%d,%d,%d)",
                                item.title, item.screen, item.cellX, item.cellY, pos.s, pos.x, pos.y));
                        if (item.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                            item.itemType = Favorites.ITEM_TYPE_APPLICATION;
                        }
                        item.container = Favorites.CONTAINER_HIDESEAT;
                        item.screen = pos.s;
                        item.cellX = pos.x;
                        item.cellY = pos.y;
                        updateItemInDatabase(mContext, item);
                    }
                } else {
                    // all items in hide-seat are processed
                    return;
                }
            }
            // hide-seat space is not enough
            while (itr.hasNext()) {
                out_nospaceItems.add(itr.next());
            }
        }
        /*YUNOS END*/

        /* YUNOS BEGIN */
        //##date:2013/11/25 ##author:hongxing.whx ##bugid: 66169
        /*
         * update the name of the following default folders:
         * tools
         * recommendation
         * games
         */
        private void updateFolderTitle(FolderInfo folder) {
            try {
                String title = (String) folder.title;
                if (DEBUG_LOADERS) Log.d(TAG, "entering updateFolderTitle: title="+title);
                Resources res = mContext.getResources();
                final String GAME_CN = res.getString(R.string.games_cn);
                final String GAME_EN = res.getString(R.string.games_en);
                final String GAME_TW = res.getString(R.string.games_tw);
                final String TOOLS_CN = res.getString(R.string.tools_cn);
                final String TOOLS_EN = res.getString(R.string.tools_en);
                final String TOOLS_TW = res.getString(R.string.tools_tw);
                final String RECOMMEND_APP_CN = res.getString(R.string.recommend_app_cn);
                final String RECOMMEND_APP_EN = res.getString(R.string.recommend_app_en);
                final String RECOMMEND_APP_TW = res.getString(R.string.recommend_app_tw);
                int resId = -1;
                if (TOOLS_CN.equals(title) || TOOLS_EN.equals(title) || TOOLS_TW.equals(title)) {
                    resId = R.string.tools;
                } else if (GAME_CN.equals(title) || GAME_EN.equals(title) || GAME_TW.equals(title)) {
                    resId = R.string.games;
                } else if (RECOMMEND_APP_CN.equals(title) || RECOMMEND_APP_EN.equals(title) || RECOMMEND_APP_TW.equals(title)) {
                    resId = R.string.recommend_app;
                }

                if (resId != -1) {
                    folder.title = res.getString(resId);
                    if (DEBUG_LOADERS) Log.d(TAG, "hongxing: title="+title+" folder.title="+folder.title);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        /* YUNOS END */

        /**
         * Binds all loaded data to actual views on the main thread.
         */
        private void bindWorkspace(int synchronizeBindPage) {
            final long t = SystemClock.uptimeMillis();
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            final boolean isLoadingSynchronously = (synchronizeBindPage > -1);
            int loadingPage = 0;// isLoadingSynchronously ?
                                        // synchronizeBindPage :
            // oldCallbacks.getCurrentWorkspaceScreen();
            if (ConfigManager.isLandOrienSupport()) {
                loadingPage = isLoadingSynchronously ? synchronizeBindPage : oldCallbacks.getCurrentWorkspaceScreen();
            }

            final int currentScreen = loadingPage;
            // Load all the items that are on the current page first (and in the process, unbind
            // all the existing workspace items before we call startBinding() below.
            //BugID:114047:don't unbindworksapce if current screen is binded
            if (mIsInStartBinding == false) {
                unbindWorkspaceItemsOnMainThread();
            }
            /*YUNOS END*/
            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                folders.putAll(sBgFolders);
                itemsIdMap.putAll(sBgItemsIdMap);
            }

            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<Long, FolderInfo>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<Long, FolderInfo>();

            // Separate the items that are on the current screen, and all the other remaining items
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems,
                    otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets,
                    otherAppWidgets);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders,
                    otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };

            /*YUNOS BEGIN*/
            //##module(HomeShell)
            //##date:2014/04/17##author:hao.liuhaolh@alibaba-inc.com##BugID:111614
            //load and display current screen first during homeshell start
            if (mIsInStartBinding == false) {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                mIsInStartBinding = true;
            }
            /* YUNOS BEGIN */
            // ## date: 2016/05/09 ## author: yongxing.lyx
            // ## BugID: 8198787: some icon disappear after pull out icons
            // from hideseat and turn aged mode on.
            if (mIsCurrentScreenBinded == false || mIsCurrentScreenUpdated) {
                mIsCurrentScreenUpdated = false;
            /* YUNOS END */
                // Load items on the current page
                bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets,
                        currentFolders, null);
                if (isLoadingSynchronously) {
                    r = new Runnable() {
                        public void run() {
                            Log.d(TAG, "isLoadingSynchronously");
                            Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                            if (callbacks != null) {
                                callbacks.onPageBoundSynchronously(currentScreen);
                            }
                        }
                    };
                    runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                }
            }
            /*YUNOS END*/

            // Load all the remaining pages (if we are loading synchronously, we want to defer this
            // work until after the first render)
            mDeferredBindRunnables.clear();
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders,
                    (isLoadingSynchronously ? mDeferredBindRunnables : null));

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                        if (FeatureUtility.hasFullScreenWidget()) {
                            callbacks.removeWidgetPages();
                            callbacks.makesureWidgetPages();
                        }
                    }

                    // If we're profiling, ensure this is the last thing in the queue.
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound workspace in "
                            + (SystemClock.uptimeMillis()-t) + "ms");
                    }

                    mIsLoadingAndBindingWorkspace = false;
                    if (homeshellSetting != null) {
                        homeshellSetting.checkNeedLayoutChange(sCellCountX, sCellCountY);
                    }
                }
            };
            if (isLoadingSynchronously) {
                mDeferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            //BugID:111614: load and display current screen first during homeshell start
            mIsInStartBinding = false;
            mIsCurrentScreenBinded= false;
        }

        private void loadAndBindAllApps() {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindAllApps mAllAppsLoaded=" + mAllAppsLoaded);
            }
            if (!mAllAppsLoaded) {
                loadAllAppsByBatch();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            } else {
                onlyBindAllApps();
            }
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }

            // shallow copy
            @SuppressWarnings("unchecked")
            final ArrayList<ApplicationInfo> list
                    = (ArrayList<ApplicationInfo>) mBgAllAppsList.data.clone();
            Runnable r = new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                    }
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound all " + list.size() + " apps from cache in "
                                + (SystemClock.uptimeMillis()-t) + "ms");
                    }
                }
            };
            boolean isRunningOnMainThread = !(sWorkerThread.getThreadId() == Process.myTid());
            if (oldCallbacks.isAllAppsVisible() && isRunningOnMainThread) {
                r.run();
            } else {
                mHandler.post(r);
            }
        }

        private void loadAllAppsByBatch() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager packageManager = mContext.getPackageManager();
            List<Object> apps = null; // contains ResolveInfo and ApplicationInfo objects

            int N = Integer.MAX_VALUE;

            int startIndex;
            int i=0;
            int batchSize = -1;
            while (i < N && !mStopped) {
                if (i == 0) {
                    mBgAllAppsList.clear();
                    final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                    /*YUNOS BEGIN*/
                    //##date:2013/12/15 ##author:hao.liuhaolh ##BugID: 73737
                    // SD card app icon lost
                    if (appsOnBoot == null) {
                        appsOnBoot = packageManager.queryIntentActivities(mainIntent, 0);
                        AppCloneManager.getInstance().addAllCloneResolveInfo(appsOnBoot);
                    }
                    /*YUNOS END*/
                    apps = new ArrayList<Object>(appsOnBoot);
                    /* YUNOS BEGIN */
                    // ##date:2014/07/21 ##author:zhanggong ##BugID:5244146
                    // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
                    // retrieve all frozen apps
                    if (Hideseat.isHideseatEnabled()) {
                        List<ShortcutInfo> hideseatItems = new ArrayList<ShortcutInfo>();
                        /* YUNOS BEGIN */
                        // ##date:2015/03/16 ##author:sunchen.sc
                        // Copy bg member to solve concurrent modification issue
                        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
                        synchronized (sBgLock) {
                            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
                        }
                        /* YUNOS END */
                        for (ItemInfo info : tmpWorkspaceItems) {
                            if (info instanceof ShortcutInfo && info.container == Favorites.CONTAINER_HIDESEAT) {
                                hideseatItems.add((ShortcutInfo) info);
                            }
                        }
                        apps.addAll(AppFreezeUtil.getAllFrozenApps(mContext, hideseatItems.toArray(new ShortcutInfo[0])));
                    }
                    /*YUNOS END*/
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "queryIntentActivities took "
                                + (SystemClock.uptimeMillis()-qiaTime) + "ms");
                    }
                    N = apps.size();
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "queryIntentActivities got " + N + " apps");
                    }
                    if (N == 0) {
                        // There are no apps?!?
                        return;
                    }
                    if (mBatchSize == 0) {
                        batchSize = N;
                    } else {
                        batchSize = mBatchSize;
                    }
                    /*YUNOS BEGIN*/
                    //##date:2013/12/04 ##author:hao.liuhaolh ##BugID:70162:compare error
                    //check empty screen and move items behind empty screen
                    /*
                    final long sortTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                    Collections.sort(apps,
                            new LauncherModel.ShortcutNameComparator(packageManager, mLabelCache));
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "sort took "
                                + (SystemClock.uptimeMillis()-sortTime) + "ms");
                    }
                    */
                    /*YUNOS END*/
                }

                final long t2 = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                startIndex = i;
                for (int j=0; i<N && j<batchSize; j++) {
                    // This builds the icon bitmaps.
                    Object obj = apps.get(i);
                    if (obj instanceof ResolveInfo) {
                        mBgAllAppsList.add(new ApplicationInfo(packageManager, (ResolveInfo) obj, mLabelCache));
                    } else if (obj instanceof ApplicationInfo) {
                        /* YUNOS BEGIN */
                        // ##date:2014/07/21 ##author:zhanggong ##BugID:5244146
                        // frozen apps
                        mBgAllAppsList.add((ApplicationInfo) obj);
                        /* YUNOS END */
                    }
                    i++;
                }

                final boolean first = i <= batchSize;
                final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                final ArrayList<ApplicationInfo> added = mBgAllAppsList.added;
                mBgAllAppsList.added = new ArrayList<ApplicationInfo>();

                mHandler.post(new Runnable() {
                    public void run() {
                        final long t = SystemClock.uptimeMillis();
                        if (callbacks != null) {
                            if (first) {
                                callbacks.bindAllApplications(added);
                            } else {
                                callbacks.bindAppsAdded(added);
                            }
                            if (DEBUG_LOADERS) {
                                Log.d(TAG, "bound " + added.size() + " apps in "
                                    + (SystemClock.uptimeMillis() - t) + "ms");
                            }
                        } else {
                            Log.i(TAG, "not binding apps: no Launcher activity");
                        }
                    }
                });

                if (DEBUG_LOADERS) {
                    Log.d(TAG, "batch of " + (i-startIndex) + " icons processed in "
                            + (SystemClock.uptimeMillis()-t2) + "ms");
                }

                if (mAllAppsLoadDelay > 0 && i < N) {
                    try {
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "sleeping for " + mAllAppsLoadDelay + "ms");
                        }
                        Thread.sleep(mAllAppsLoadDelay);
                    } catch (InterruptedException exc) { }
                }
            }

            if (DEBUG_LOADERS) {
                Log.d(TAG, "cached all " + N + " apps in "
                        + (SystemClock.uptimeMillis()-t) + "ms"
                        + (mAllAppsLoadDelay > 0 ? " (including delay)" : ""));
            }
        }

        public void dumpState() {
            synchronized (sBgLock) {
                Log.d(TAG, "mLoaderTask.mContext=" + mContext);
                Log.d(TAG, "mLoaderTask.mIsLaunching=" + mIsLaunching);
                Log.d(TAG, "mLoaderTask.mStopped=" + mStopped);
                Log.d(TAG, "mLoaderTask.mLoadAndBindStepFinished=" + mLoadAndBindStepFinished);
                Log.d(TAG, "mItems size=" + sBgWorkspaceItems.size());
            }
        }
    }

    public void post(Runnable r){
        sWorker.post(r);
    }

    /* YUNOS BEGIN */
    // ##date:2015-1-21 ##author:zhanggong.zg ##BugID:5684630
    private interface PackageUpdatedTaskListener {
        /**
         * This method will be called in UI thread when the target
         * <code>PackageUpdatedTask</code> is finish.
         */
        void onPackageUpdatedTaskFinished(PackageUpdatedTask task);
    }
    /* YUNOS END */

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted

        private PackageUpdatedTaskListener mListener;

        public PackageUpdatedTask(int op, String[] packages) {
            mOp = op;
            mPackages = packages;
            mListener = null;
        }

        @Override
        public String toString() {
            return String.format("PackageUpdatedTask[op=%d,pkgs=%s]", mOp,
                                 mPackages != null ? Arrays.asList(mPackages) : "null");
        }

        /* YUNOS BEGIN */
        // ##date:2015-1-21 ##author:zhanggong.zg ##BugID:5684630
        public void setListener(PackageUpdatedTaskListener listener) {
            this.mListener = listener;
        }
        /* YUNOS END */

        public void run() {
            final Context context = mApp;

            final String[] packages = mPackages;
            final int N = packages.length;

            /*YUNOS BEGIN*/
            //##date:2014/01/14 ##author:hao.liuhaolh ##BugID: 86054
            // SD app reinstall in SD card usb mode
            //run this part of code in worker thread to avoid
            //sBgItemsIdMap ConcurrentModificationException
            Log.d(TAG, "op is " + mOp);
            if (mOp == PackageUpdatedTask.OP_ADD) {
                //actually there is only one item in packages
                //so we just handle the only item
                if (N >0) {
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    for(ItemInfo info: allApps) {
                        if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) ||
                            (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION)) {
                            if ((((ShortcutInfo)info).intent != null) &&
                                 (((ShortcutInfo)info).intent.getComponent() != null) &&
                                 (((ShortcutInfo)info).intent.getComponent().getPackageName() != null) &&
                                 (((ShortcutInfo)info).intent.getComponent().getPackageName().equals(packages[0]))) {
                                Log.d(TAG, "the installing app in app list");
                                if (((ShortcutInfo)info).mIsUninstalling) {
                                    Log.w(TAG, "WARNING!  item Uninstalling, info = "+info);
                                    ((ShortcutInfo)info).mIsUninstalling = false;
                                }
                                mOp = PackageUpdatedTask.OP_UPDATE;
                                break;
                            }
                        }
                    }
                }
            }
            /*YUNOS END*/

            switch (mOp) {
                case OP_ADD:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.addPackage " + packages[i]);
                        //modified by dongjun for BugID:69147 begin
                        resetAppInstalling(packages[i]);
                        boolean added = mBgAllAppsList.addPackage(context, packages[i]);
                        if(!added){
                            Log.e(TAG,"addPackage failed");
                            if(mAppDownloadMgr!=null){
                                mAppDownloadMgr.appDownloadRemove(packages[i]);
                            }
                        } else {
                            final int index = i;
                            mAppGroupMgr.handleSingleApp(packages[index]);
                        }
                        //modified by dongjun for BugID:69147 end
                    }
                    break;
                case OP_UPDATE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.updatePackage " + packages[i]);
                        resetAppInstalling(packages[i]);
                        mBgAllAppsList.updatePackage(context, packages[i]);
                        LauncherApplication app =
                                (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(
                                app.getWidgetPreviewCacheDb(), packages[i]);
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        resetAppInstalling(packages[i]);
                        mBgAllAppsList.removePackage(packages[i]);
                        LauncherApplication app =
                                (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(
                                app.getWidgetPreviewCacheDb(), packages[i]);
                    }
                    break;
            }

            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            final ArrayList<ApplicationInfo> removedApps = new ArrayList<ApplicationInfo>();

            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<ApplicationInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<ApplicationInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }

            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            if (callbacks == null) {
                Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }

            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                if (DEBUG_LOADERS) Log.d(TAG, "added isn't null");
                parseAddedApps(context, added);
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsAdded(addedFinal);
                        }
                    }
                });
            }

            if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                if (DEBUG_LOADERS) Log.d(TAG, "modified isn't null");
                parseUpdatedApps(context, modified);
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }

            // If a package has been removed, or an app has been removed as a result of
            // an update (for example), make the removed callback.
            if (mOp == OP_REMOVE || !removedApps.isEmpty()) {
                if (DEBUG_LOADERS) Log.d(TAG, "removed isn't null");
                final boolean permanent = (mOp == OP_REMOVE);
                final ArrayList<String> removedPackageNames =
                        new ArrayList<String>(Arrays.asList(packages));
                /*YUNOS BEGIN*/
                //##date:2014/01/09 ##author:hao.liuhaolh ##BugID: 84441
                // Can't enter edit mode after app update.
                if(mAppDownloadMgr!=null) {
                    if ((mOp == OP_REMOVE) && (removedPackageNames.size() > 0)) {
                        for (String packagename: removedPackageNames) {
                            Log.d(TAG, "remove by package name:" + packagename);
                            mAppDownloadMgr.updateDownloadCount(packagename, false);
                        }
                    } else if (!removedApps.isEmpty()) {
                        for (ApplicationInfo appinfo: removedApps) {
                            if ((appinfo != null) &&
                                (appinfo.componentName != null) &&
                                (appinfo.componentName.getPackageName() != null)) {
                                Log.d(TAG, "remove by appinfo:" + appinfo.componentName.getPackageName());
                                mAppDownloadMgr.updateDownloadCount(appinfo.componentName.getPackageName(), false);
                            }
                        }
                    }
                }
                /*YUNOS END*/
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindComponentsRemoved(removedPackageNames,
                                    removedApps, permanent);
                        }
                    }
                });
            }
            // YUNOS BEGIN
            // ##date:2014/08/29 ##author:hongchao.ghc ##bugid: 5214563
            // remote code ,because there will be corresponding in
            // LauncherAppWidgetHost callback method to deal
            // YUNOS END

            /* YUNOS BEGIN */
            // ##date:2015-1-21 ##author:zhanggong.zg ##BugID:5684630
            final PackageUpdatedTaskListener listener = mListener;
            if (listener != null) {
                mHandler.post(new Runnable() {
                    public void run() {
                        listener.onPackageUpdatedTaskFinished(PackageUpdatedTask.this);
                    }
                });
            }
            /* YUNOS END */
        }

       /* YUNOS END */
        /*YUNOS BEGIN*/
        //##date:2013/11/18 ##author:hao.liuhaolh
        //add installed app's shortcut to workspace
        private void parseAddedApps(final Context context, ArrayList<ApplicationInfo> added) {
            if (DEBUG_LOADERS) Log.d(TAG, "parseAddedApps in");
            if (added.size() > 0) {
                int itemSize = added.size();
                boolean isDownloadingItem = false;

                for(int i = 0; i <  itemSize; i++){
                    ApplicationInfo app = (ApplicationInfo)added.get(i);
                    /* YUNOS BEGIN */
                    // ##date:2014/12/22 ##author:zhanggong.zg ##BugID:5654420
                    isDownloadingItem = false;
                    /* YUNOS END */

                    /* YUNOS BEGIN */
                    // ##date:2013/12/03 ##author:hongxing.whx ##bugid: 69481
                    // to restore widget which is bound to the installing applicaitons
                    // commenting out right now because restoreWiget fails at bindAppWidgetId
                    // codebase 2.5 copied the logic of 2.3, butcodebase 2.3 has the same issue
                    /*
                    try {
                        if (BackupManager.getInstance().isInRestore()) {
                            restoreWidget(context,app);
                        }
                    } catch (Exception e) {

                    }
                    */
                    /* YUNOS END */
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    for (ItemInfo info: allApps) {
                        //find the app in download list first
                        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
                            if(app.componentName.getPackageName().equals(((ShortcutInfo)info).intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME))) {
                                Log.d(TAG, "it is a downloading item");
                                isDownloadingItem = true;
                                /* YUNOS BEGIN */
                                //##date:2014/03/24 ##author:nater.wg ##BugID:107102
                                // If the item is restore app, do not show new tag.
                                info.setIsNewItem(!Utils.isRestoreApp(app.componentName.getPackageName()));
                                /* YUNOS END */
                                info.title = app.title;

                                ((ShortcutInfo)info).intent = app.intent;
                                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
                                ((ShortcutInfo)info).setAppDownloadStatus(AppDownloadStatus.STATUS_INSTALLED);
                                //added by dongjun for 69273 begin
                                mAppDownloadMgr.updateDownloadCount(app.componentName.getPackageName(),false);
                                //added by dongjun for 69273 end

                                //add by dongjun for IconManager
                                Drawable icon = mApp.getIconManager().getAppUnifiedIcon(app,null);
                                ((ShortcutInfo) info).setIcon(icon);
                                mApp.getIconManager().clearCardBackgroud(((ShortcutInfo)info).intent);
                                /*YUNOS BEGIN*/
                                //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
                                // fato sd app icon error
                                try {
                                    int appFlags = context.getPackageManager().getApplicationInfo(app.componentName.getPackageName(), 0).flags;
                                    ((ShortcutInfo)info).isSDApp = Utils.isSdcardApp(appFlags)?1:0;
                                    ((ShortcutInfo)info).customIcon = (((ShortcutInfo)info).isSDApp==0)?false:true;
                                } catch (NameNotFoundException e) {
                                    Log.d(TAG, "PackageManager.getApplicationInfo failed");
                                }
                                /*YUNOS END*/

                                final ContentValues values = new ContentValues();
                                final ContentResolver cr = context.getContentResolver();
                                ((ShortcutInfo) info).onAddToDatabase(values);

                                /*YUNOS BEGIN*/
                                //##date:2014/01/06 ##author:hao.liuhaolh ##BugID: 82849
                                // the app icon store in database should be original icon
                                /*save the original icon to database if it is a sd app*/
                                if (((ShortcutInfo) info).isSDApp == 1) {
                                    //BugID:122827:sdcard app icon wrong on theme change when sdcard unmount
                                    Drawable origIcon = IconUtils.getAppOriginalIcon(context, (ShortcutInfo)info);
                                    if (origIcon != null) {
                                        ItemInfo.writeBitmap(values, origIcon);
                                    } else {
                                        if(values.containsKey(LauncherSettings.Favorites.ICON)) {
                                            values.remove(LauncherSettings.Favorites.ICON);
                                        }
                                    }
                                } else {
                                    values.put(LauncherSettings.Favorites.ICON, new byte[0]);
                                }
                                /*YUNOS END*/

                                /* YUNOS BEGIN */
                                //##date:2014/08/08 ##author:zhanggong.zg ##BugID:5244146
                                // froze the app in hideseat when finish download
                                if (info.container == Favorites.CONTAINER_HIDESEAT) {
                                    Hideseat.freezeApp((ShortcutInfo) info);
                                } else {
                                    //xiaodong.lxd #5631917 : unfreeze app after installed
                                    if (AppFreezeUtil.isPackageFrozen(LauncherApplication.getContext(), (ShortcutInfo) info)) {
                                        ShortcutInfo shortcutInfo = (ShortcutInfo)info;
                                        Intent intent = shortcutInfo.intent;
                                        ComponentName cmpt = intent != null ? intent.getComponent() : null;
                                        String pkgName = cmpt != null ? cmpt.getPackageName() : null;
                                        AppFreezeUtil.asyncUnfreezePackage(LauncherApplication.getContext(), pkgName);
                                    }
                                }
                                /* YUNOS END */

                                cr.update(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values,
                                        "_id =?", new String[] { String.valueOf(info.id) });
                                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                                final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
                                infos.add(info);
                                final Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (callbacks != null) {
                                            callbacks.bindItemsUpdated(infos);
                                        }
                                    }
                                };
                                runOnMainThread(r);
                                break;
                            }
                        }
                        /*YUNOS BEGIN*/
                        //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
                        //vp install
                        //if the installed app is a vp install item, replace the vp item info with the installed info
// remove vp install
//                        else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
//                            if(app.componentName.getPackageName().equals(((ShortcutInfo)info).intent.getStringExtra(VPUtils.TYPE_PACKAGENAME))) {
//                                Log.d(TAG, "it is a vpinstall item");
//                                /*YUNOS BEGIN*/
//                                //##date:2014/03/20 ##author:hao.liuhaolh ##BugID:103304
//                                //user track in vp install
//                                String pkgName = ((ShortcutInfo)info).intent.getStringExtra(VPUtils.TYPE_PACKAGENAME);
//                                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_VP_ITEM_INSTALL_SUCCESS , pkgName);
//                                /*YUNOS END*/
//                                isDownloadingItem = true;
//                                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
//                                // Do not show new icon on VPInstall app.
//                                info.setIsNewItem(false);
//                                info.title = app.title;
//                                ((ShortcutInfo)info).intent = app.intent;
//                                ((ShortcutInfo)info).setVPInstallStatus(VPInstallStatus.STATUS_NORMAL);
//
//                                LauncherApplication application = (LauncherApplication)context.getApplicationContext();
//
//                                //add by dongjun for IconManager
//                                Drawable icon = mApp.getIconManager().getAppUnifiedIcon(app,null);
//                                ((ShortcutInfo) info).setIcon(icon);
//
//                                /*YUNOS BEGIN*/
//                                //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
//                                // fato sd app icon error
//                                try {
//                                    int appFlags = context.getPackageManager().getApplicationInfo(app.componentName.getPackageName(), 0).flags;
//                                    ((ShortcutInfo)info).isSDApp = Utils.isSdcardApp(appFlags)?1:0;
//                                    ((ShortcutInfo)info).customIcon = (((ShortcutInfo)info).isSDApp==0)?false:true;
//                                } catch (NameNotFoundException e) {
//                                    Log.d(TAG, "PackageManager.getApplicationInfo failed");
//                                }
//                                /*YUNOS END*/
//
//                                final ContentValues values = new ContentValues();
//                                final ContentResolver cr = context.getContentResolver();
//                                ((ShortcutInfo) info).onAddToDatabase(values);
//
//                                /*YUNOS BEGIN*/
//                                //##date:2014/01/06 ##author:hao.liuhaolh ##BugID: 82849
//                                // the app icon store in database should be original icon
//                                /*save the original icon to database if it is a sd app*/
//                                if (((ShortcutInfo) info).isSDApp == 1) {
//                                    //BugID:122827:sdcard app icon wrong on theme change when sdcard unmount
//                                    Drawable origIcon = IconUtils.getAppOriginalIcon(context, (ShortcutInfo)info);
//                                    if (origIcon != null) {
//                                        ItemInfo.writeBitmap(values, origIcon);
//                                    } else {
//                                        if(values.containsKey(LauncherSettings.Favorites.ICON)) {
//                                            values.remove(LauncherSettings.Favorites.ICON);
//                                        }
//                                    }
//                                } else {
//                                    values.put(LauncherSettings.Favorites.ICON, new byte[0]);
//                                }
//                                /*YUNOS END*/
//                                /*YUNOS BEGIN*/
//                                //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
//                                //vp install item icon display
//                                VPInstallDrawingList.remove(info);
//                                /*YUNOS END*/
//
//                                /* YUNOS BEGIN */
//                                // ##date:2014/08/08 ##author:zhanggong.zg ##BugID:5244146
//                                // froze the app in hideseat when finish download
//                                if (info.container == Favorites.CONTAINER_HIDESEAT) {
//                                    Hideseat.freezeApp((ShortcutInfo) info);
//                                }
//                                /* YUNOS END */
//
//                                cr.update(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values,
//                                        "_id =?", new String[] { String.valueOf(info.id) });
//                                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
//                                final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
//                                infos.add(info);
//                                final ItemInfo finalvpinfo = info;
//                                final Runnable r = new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        if (callbacks != null) {
//                                            callbacks.bindItemsUpdated(infos);
//                                            /*YUNOS BEGIN*/
//                                            //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
//                                            //vp install item icon display
//                                            Intent intent = new Intent(((ShortcutInfo)finalvpinfo).intent);
//                                            callbacks.startVPInstallActivity(intent, (ShortcutInfo)finalvpinfo);
//                                            /*YUNOS END*/
//                                        }
//                                    }
//                                };
//                                runOnMainThread(r);
//                                break;
//                            }
//                        }
                        /*YUNOS END*/
                    }

                    final boolean isDownloadingItemFinal = isDownloadingItem;
                    final ApplicationInfo appFinal = app;
                    //create a new shortcut info if the app no in download list

                    /* YUNOS BEGIN */
                    // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                    // When user occupy the UI thread, not find empty space in the background.
                    // Only find the empty space in the UI thread, this will forbid icon overlap
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (isDownloadingItemFinal == false) {
                                Log.d(TAG, "findEmptyCell");
                                int startScreen = obtainStartScreen();
                                ScreenPosition p = findEmptyCellAndOccupy(startScreen);
                                if (p == null) {
                                    p = new ScreenPosition(-1, -1, -1);
                                    Toast.makeText(context, context.getString(R.string.application_not_show_no_space),
                                                           Toast.LENGTH_SHORT).show();
                                }
                                final ScreenPosition p2 = p;
                                final Runnable r2 = new Runnable() {
                                    @Override
                                    public void run() {
                                        getNewItemInfoByApplicationInfo(context, p2, appFinal);
                                    }
                                };
                                runOnWorkerThread(r2);
                            }
                        }
                    };
                    runOnMainThread(r);
                    /* YUNOS END */
                }
            }
        }

        private ShortcutInfo getNewItemInfoByApplicationInfo(final Context context,
                ScreenPosition p, ApplicationInfo app) {
            ShortcutInfo info = getShortcutInfo(context.getPackageManager(), app, context);

            if (info == null) {
                return null;
            }

            int screen = p.s;
            int x = p.x;
            int y = p.y;
            long container = LauncherSettings.Favorites.CONTAINER_DESKTOP;

            info.container = container;
            info.setPosition(p);
            info.spanX = 1;
            info.spanY = 1;

            /*YUNOS BEGIN*/
            //##module(HomeShell)
            //##date:2014/03/27 ##author:hao.liuhaolh@alibaba-inc.com##BugID:105341
            // system app icon display new mark after disable and re enable
            if (info.isSystemApp == false) {
                info.setIsNewItem(true);
            } else {
                info.setIsNewItem(false);
            }
            /*YUNOS END*/

            /*YUNOS BEGIN LH handle installed app no space case*/
            if (screen == -1) {
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                //remove item, which can't be added in screen,from RestoreList
                final String pkgname = info.getPackageName();
                Runnable r = new Runnable(){
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        BackupManager.getInstance().addRestoreDoneApp(context, pkgname);
                    }
                };
                runOnMainThread(r);
            }
            /*YUNOS END*/

            /*YUNOS BEGIN*/
            //##date:2014/03/25 ##author:nater.wg ##BugID:104547
            //check duplicate app item.
            if (isDuplicateItem(info)) {
                return null;
            }
            /*YUNOS END*/

            //addItemToDatabase will set Id
            addItemToDatabase(context, info, container,
                    screen, p.xPort, p.yPort, p.xLand, p.yLand, false);
            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
            infos.add(info);
            /*YUNOS BEGIN LH handle installed app no space case*/
            if (info.itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
            /*YUNOS END*/
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (callbacks != null) {
                            callbacks.bindItemsAdded(infos);
                        }
                    }
                };
                runOnMainThread(r);
            }

            /* YUNOS BEGIN */
            // ##date:2015/5/29 ##author:zhanggong.zg ##BugID:6026848
            // Ensure the new app is NOT frozen after installation.
            if (AppFreezeUtil.isPackageFrozen(LauncherApplication.getContext(), info)) {
                Log.d(TAG, "getNewItemInfoByApplicationInfo: new app is frozen: " + info.title);
                ComponentName cmpt = info.intent != null ? info.intent.getComponent() : null;
                String pkgName = cmpt != null ? cmpt.getPackageName() : null;
                AppFreezeUtil.asyncUnfreezePackage(LauncherApplication.getContext(), pkgName);
            }
            /* YUNOS END */

            return info;
        }

        private void parseUpdatedApps(final Context context, ArrayList<ApplicationInfo> added) {
            if (DEBUG_LOADERS) Log.d(TAG, "parseUpdatedApps in");
            if (added.size() > 0) {
                int itemSize = added.size();

                for(int i = 0; i <  itemSize; i++){
                    ApplicationInfo app = (ApplicationInfo)added.get(i);
                    //find the app in download list first
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    for (ItemInfo info: allApps) {
                        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            Log.d(TAG, "app.componentName.getPackageName() is " + app.componentName.getPackageName());
                            Log.d(TAG, "item component name is " + ((ShortcutInfo)info).intent.getComponent().getPackageName());
                            /*YUNOS BEGIN*/
                            //##module(homeshell)
                            //##date:2013/12/06 ##author:jun.dongj@alibaba-inc.com##BugID:71500
                            //a package with multi launcher activity udpate, compare with component name not package name
                            // if(app.componentName.getPackageName().equals(((ShortcutInfo)info).intent.getComponent().getPackageName())) {
                            if((app.componentName.compareTo(((ShortcutInfo)info).intent.getComponent())==0) ||
                              ((AppFreezeUtil.isPackageFrozen(context, (ShortcutInfo)info) == true)&&
                              TextUtils.isEmpty(app.componentName.getClassName()) &&
                              app.componentName.getPackageName().equals(((ShortcutInfo)info).intent.getComponent().getPackageName()))) {
                            /*YUNOS END*/
                                final ShortcutInfo shortcutInfo = (ShortcutInfo) info;
                                //for update item, don't set it new
                                //info.setIsNewItem(true);
                                //BugID:5592994:icon type isn't theme type
                                if (AppFreezeUtil.isPackageFrozen(context, shortcutInfo) == false) {
                                    /* YUNOS BEGIN */
                                    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5690670
                                    if (app.needsUpdate()) {
                                        // intent, component name and title of the app-info
                                        // will be updated
                                        app.update(context.getPackageManager());
                                    }
                                    /* YUNOS END */
                                    shortcutInfo.title = app.title;
                                    shortcutInfo.intent = app.intent;
                                } else {
                                    app.intent = shortcutInfo.intent;
                                }
                                shortcutInfo.setAppDownloadStatus(AppDownloadStatus.STATUS_INSTALLED);
                                //added by dongjun for 69273 begin
                                mAppDownloadMgr.updateDownloadCount(app.componentName.getPackageName(),false);
                                //added by dongjun for 69273 end
                                LauncherApplication application = (LauncherApplication)context.getApplicationContext();

                                //add by dongjun for IconManager
                                final IconManager iconManager = mApp.getIconManager();
                                Drawable icon = iconManager.getAppUnifiedIcon(app,null);
                                shortcutInfo.setIcon(icon);
                                /* YUNOS BEGIN */
                                // ##date:2015/2/4 ##author:zhanggong.zg ##BugID:5751112
                                // Do not clear card background in worker thread. Because icon
                                // might flicker when it redraws in main thread before the new
                                // backgroud is generated and set.
                                //mApp.getIconManager().clearCardBackgroud(((ShortcutInfo)info).intent);
                                /* YUNOS END */

                                final ContentValues values = new ContentValues();
                                final ContentResolver cr = context.getContentResolver();
                                info.onAddToDatabase(values);

                                /*YUNOS BEGIN*/
                                //##date:2014/01/06 ##author:hao.liuhaolh ##BugID: 82849
                                // the app icon store in database should be original icon
                                /*save the original icon to database if it is a sd app*/
                                if (shortcutInfo.isSDApp == 1) {
                                    //BugID:122827:sdcard app icon wrong on theme change when sdcard unmount
                                    Drawable origIcon = IconUtils.getAppOriginalIcon(context, shortcutInfo);
                                    if (origIcon != null) {
                                        ItemInfo.writeBitmap(values, origIcon);
                                    } else {
                                        if(values.containsKey(LauncherSettings.Favorites.ICON)) {
                                            values.remove(LauncherSettings.Favorites.ICON);
                                        }
                                    }
                                } else {
                                    values.put(LauncherSettings.Favorites.ICON, new byte[0]);
                                }
                                /*YUNOS END*/
                                /* YUNOS BEGIN */
                                //##date:2014/08/08 ##author:zhanggong.zg ##BugID:5244146
                                // froze the app in hideseat when finish download
                                if (shortcutInfo.container == Favorites.CONTAINER_HIDESEAT) {
                                    if (!AppFreezeUtil.isPackageFrozen(context, shortcutInfo)) {
                                        Hideseat.freezeApp(shortcutInfo);
                                    }
                                } else {
                                    //xiaodong.lxd #5631917 : unfreeze app after installed
                                    boolean isFrozen = AppFreezeUtil.isPackageFrozen(LauncherApplication.getContext(), (ShortcutInfo) info);
                                    boolean isInHideseat = Hideseat.containsSamePackageOf(info);
                                    if (isFrozen && !isInHideseat) {
                                        Intent intent = shortcutInfo.intent;
                                        ComponentName cmpt = intent != null ? intent.getComponent() : null;
                                        String pkgName = cmpt != null ? cmpt.getPackageName() : null;
                                        AppFreezeUtil.asyncUnfreezePackage(LauncherApplication.getContext(), pkgName);
                                    }
                                }
                                /* YUNOS END */

                                cr.update(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values,
                                        "_id =?", new String[] { String.valueOf(info.id) });
                                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                                final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
                                infos.add(info);
                                final Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (callbacks != null) {
                                            /* YUNOS BEGIN */
                                            // ##date:2015/2/4 ##author:zhanggong.zg ##BugID:5751112
                                            // Clear card background in main thread instead of worker
                                            // thread to avoid icon flicker.
                                            Intent intent = shortcutInfo.intent;
                                            if (intent != null) {
                                                // ##date:2015/8/5 ##author:zhanggong.zg ##BugID:6284676
                                                // clear big/small icons when app updated
                                                iconManager.clearCardBackgroud(shortcutInfo.intent);
                                                iconManager.clearIconCache(shortcutInfo.intent);
                                            }
                                            /* YUNOS END */
                                            callbacks.bindItemsUpdated(infos);
                                        }
                                    }
                                };
                                runOnMainThread(r);
                            }
                        }
                    }
                    /* YUNOS BEGIN */
                    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5690670
                    if (app.needsUpdate()) {
                        app.update(context.getPackageManager());
                    }
                    /* YUNOS END */
                }
            }
        }
        /*YUNOS END*/
    }

    // Returns a list of ResolveInfos/AppWindowInfos in sorted order
    public static ArrayList<Object> getSortedWidgetsAndShortcuts(Context context) {
        PackageManager packageManager = context.getPackageManager();
        final ArrayList<Object> widgetsAndShortcuts = new ArrayList<Object>();
        List<ResolveInfo> appShortcuts = filterAppShortcut(packageManager);
        Collections.sort(appShortcuts, new ShortcutWidgetComparator(packageManager));
        widgetsAndShortcuts.addAll(appShortcuts);
        widgetsAndShortcuts.addAll(AppWidgetManager.getInstance(context).getInstalledProviders());
        /* YUNOS BEGIN */
        // ##gadget
        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com##BugID:96378
        widgetsAndShortcuts.addAll(0, ThemeUtils.listGadgets(context).values());
        /* YUNOS END */
        return widgetsAndShortcuts;
    }

    private static List<ResolveInfo> filterAppShortcut(PackageManager pm) {
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();
        Intent shortcutsIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        List<ResolveInfo> list = pm.queryIntentActivities(shortcutsIntent, 0);
        for (ResolveInfo ri : list) {
            if (checkResolveInfo(ri)) {
                result.add(ri);
            }
        }
        return result;
    }

    private static boolean checkResolveInfo(ResolveInfo ri) {
        return ri != null && ri.activityInfo != null && ("alias.DialShortcut".equals(ri.activityInfo.name)
                && "com.yunos.alicontacts".equals(ri.activityInfo.packageName) ||
                "com.aliyun.homeshell".equals(ri.activityInfo.packageName));
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/18 ##author:hao.liuhaolh
    //add installed app's shortcut to workspace
    public ShortcutInfo getShortcutInfo(PackageManager packageManager,
            ApplicationInfo app, Context context) {
        ComponentName componentName = app.componentName;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ShortcutInfo info = getShortcutInfo(packageManager, intent, context);
        if (info != null) {
            info.intent = intent;
            /* YUNOS BEGIN */
            // ##date:2014/7/23 ##author:zhanggong.zg ##BugID:5244146
            // the title & icon of a frozen app cannot be retrieved by getShortcutInfo()
            if (info.title == null || info.title.length() == 0) {
                info.title = app.title;
            }
            /* YUNOS END */
            info.userId = app.userId;
        }
        return info;
    }
    /*YUNOS END*/

    /**
     * This is called from the code that adds shortcuts from the intent receiver.  This
     * doesn't have a Cursor, but
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context) {
        return getShortcutInfo(manager, intent, context, null, -1, -1, null);
    }

    /**
     * Make an ShortcutInfo object for a shortcut that is an application.
     *
     * If c is not null, then it will be used to fill in missing data like the title and icon.
     */
    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, Context context,
            Cursor c, int iconIndex, int titleIndex, HashMap<Object, CharSequence> labelCache) {
        Drawable icon = null;
        final ShortcutInfo info = new ShortcutInfo();

        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            Log.d(TAG, "componentName is null");
            return null;
        }

        try {
            PackageInfo pi = manager.getPackageInfo(componentName.getPackageName(), 0);
            if (!pi.applicationInfo.enabled) {
                // If we return null here, the corresponding item will be removed from the launcher
                // db and will not appear in the workspace.
                Log.d(TAG, "pi.applicationInfo.enabled is false");
                /* YUNOS BEGIN */
                // ##date:2014/07/21 ##author:zhanggong.zg ##BugID:5244146
                // HomeShell displays frozen apps in hide-seat, so instead of null, a ShortcutInfo
                // object should be returned.
                // the container property should be set here. so its icon can be retrieved later.
                info.container = LauncherSettings.Favorites.CONTAINER_HIDESEAT;
                // return null;
                /* YUNOS END */
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "getPackInfo failed for package " + componentName.getPackageName());
        }

        // TODO: See if the PackageManager knows about this case.  If it doesn't
        // then return null & delete this.

        // the resource -- This may implicitly give us back the fallback icon,
        // but don't worry about that.  All we're doing with usingFallbackIcon is
        // to avoid saving lots of copies of that in the database, and most apps
        // have icons anyway.

        // Attempt to use queryIntentActivities to get the ResolveInfo (with IntentFilter info) and
        // if that fails, or is ambiguious, fallback to the standard way of getting the resolve info
        // via resolveActivity().
        ResolveInfo resolveInfo = null;
        ComponentName oldComponent = intent.getComponent();
        Intent newIntent = new Intent(intent.getAction(), null);
        newIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        newIntent.setPackage(oldComponent.getPackageName());
        List<ResolveInfo> infos = manager.queryIntentActivities(newIntent, 0);
        /* YUNOS BEGIN */
        // ## date: 2016/05/07 ## author: yongxing.lyx
        // ## BugID: 8185562: some icons in hideseat will disappear after clear
        // HomeShell data.
        if (infos.size() == 0) {
            infos = manager
                    .queryIntentActivities(newIntent, PackageManager.GET_DISABLED_COMPONENTS);
        }
        /* YUNOS END */
        for (ResolveInfo i : infos) {
            ComponentName cn = new ComponentName(i.activityInfo.packageName,
                    i.activityInfo.name);
            if (cn.equals(oldComponent)) {
                resolveInfo = i;
            }
        }
        if (resolveInfo != null) {
            newIntent.setComponent(oldComponent);
        }

        // from the resource
        if (resolveInfo != null) {
            ComponentName key = LauncherModel.getComponentNameFromResolveInfo(resolveInfo);
            if (labelCache != null && labelCache.containsKey(key)) {
                info.title = labelCache.get(key);
            } else {
                info.title = resolveInfo.activityInfo.loadLabel(manager);
                if (labelCache != null) {
                    labelCache.put(key, info.title);
                }
            }
        }
        // from the db
        if (info.title == null) {
            if (c != null) {
                info.title =  c.getString(titleIndex);
            }
        }
        // fall back to the class name of the activity
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

        /*YUNOS BEGIN*/
        //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
        // fato sd app icon error
        if (c != null) {
            try {
                int index = c.getColumnIndexOrThrow(LauncherSettings.Favorites.IS_SDAPP);
                info.isSDApp = c.getInt(index);
            } catch (Exception ex) {
            }
        }
        if (resolveInfo != null) {
            info.isSDApp = Utils.isSdcardApp(resolveInfo.activityInfo.applicationInfo.flags)?1:0;
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/03/07 ##author:jun.dongj ##BugID:97050
        //icon ,for the sd card app, in the hidseat show wrong
        info.intent = intent;
        IconCursorInfo cursorinfo = new IconCursorInfo(c,iconIndex);
        icon = mApp.getIconManager().getAppUnifiedIcon(info,cursorinfo);
        info.setIcon(icon);
        /*YUNOS END*/
        info.customIcon = (info.isSDApp==0)?false:true;

        /*YUNOS BEGIN added by xiaodong.lxd for #99779*/
        info.setSystemAppFlag(resolveInfo);
        /*YUNOS END*/
        /* YUNOS BEGIN */
        // ## date: 2016/06/22 ## author: yongxing.lyx
        // ## BugID:8411092:don't show updating status after changed language.
        int dlStatus = mAppDownloadMgr.getStatus(componentName.getPackageName(), false);
        if (!mClearAllDownload
                && (dlStatus == AppDownloadStatus.STATUS_PAUSED || dlStatus == AppDownloadStatus.STATUS_DOWNLOADING)) {
            info.mDownloadType = AppDownloadManager.DOWNLOAD_TYPE_UPDATE;
            info.setAppDownloadStatus(dlStatus);
            int progress = mAppDownloadMgr.getProgress(componentName.getPackageName(), false);
            if (progress >= 0) {
                info.setProgress(progress);
            }
        }
        /* YUNOS END */
        return info;
    }

    /**
     * Returns the set of workspace ShortcutInfos with the specified intent.
     */
    public static ArrayList<ItemInfo> getWorkspaceShortcutItemInfosWithIntent(
            Intent intent) {
        ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgWorkspaceItems) {
                if (info instanceof ShortcutInfo) {
                    ShortcutInfo shortcut = (ShortcutInfo) info;
                    if (shortcut.intent.toUri(0).equals(intent.toUri(0))) {
                        items.add(shortcut);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Make an ShortcutInfo object for a shortcut that isn't an application.
     */
    private ShortcutInfo getShortcutInfo(Cursor c, Context context,
            int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex,
            int titleIndex) {
        Drawable icon = null;
        final ShortcutInfo info = new ShortcutInfo();
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        if (c == null) {
            return info;
        }
        /* YUNOS END */
        int type = c.getInt(c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE));

        if(type==LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING){
            info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING;
            //add by dongjun for init the pasued item status
            info.setAppDownloadStatus(AppDownloadStatus.STATUS_PAUSED);
        }

// remove vp install
//        else if (type==LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
//            info.itemType = LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL;
//        }

        // TODO: If there's an explicit component and we can't install that, delete it.

        info.title = c.getString(titleIndex);

        int iconType = c.getInt(iconTypeIndex);
        switch (iconType) {
        case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
            String packageName = c.getString(iconPackageIndex);
            String resourceName = c.getString(iconResourceIndex);
            PackageManager packageManager = context.getPackageManager();
            info.customIcon = false;
            // the resource
            try {
                Resources resources = packageManager.getResourcesForApplication(packageName);
                if (resources != null) {
                    //removed by dongjun, for the resource in alway null current;
                    //final int id = resources.getIdentifier(resourceName, null, null);
                    //icon = Utilities.createIconDrawable(mIconManager.getFullResIcon(resources, id), context);
                }
            } catch (Exception e) {
                // drop this.  we have other places to look for icons
            }
            // the db
            Log.d(TAG,"getshortinfo step 1");
            IconCursorInfo cursorinfo = new IconCursorInfo(c,iconIndex,true);
            icon = mIconManager.getAppUnifiedIcon(info, cursorinfo);
            break;
        case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
            Log.d(TAG,"getshortinfo step 2");
            IconCursorInfo cursor_info = new IconCursorInfo(c,iconIndex,true);
            icon = mApp.getIconManager().getAppUnifiedIcon(info,cursor_info);
            if(mApp.getIconManager().isDefaultIcon(icon)){
                info.customIcon = false;
                info.usingFallbackIcon = true;
            }else{
                info.customIcon = true;
            }
            break;
        default:
            Log.d(TAG,"getshortinfo step 3");
            icon = getFallbackIcon();
            info.usingFallbackIcon = true;
            info.customIcon = false;
            break;
        }

//        Drawable newIcon = mIconManager.buildUnifiedIcon(icon);
//        if (newIcon == null) {
//            newIcon = icon;
//        } else if (!newIcon.equals(icon)) {
//          //@@@@@@ need confirm
//          //((FastBitmapDrawable)icon).getBitmap().recycle();
//            icon = null;
//        }
        Log.d(TAG,"getshortinfo step 4");
        info.setIcon(icon);


        /*YUNOS BEGIN*/
        //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
        // fato sd app icon error
        if (c != null) {
            try {
                int index = c.getColumnIndexOrThrow(LauncherSettings.Favorites.IS_SDAPP);
                info.isSDApp = c.getInt(index);
            } catch (Exception ex) {
            }
        }
        /*YUNOS END*/

        return info;
    }

    /*YUNOS BEGIN*/
    //##date:2014/05/07##author:hao.liuhaolh@alibaba-inc.com##BugID:117986
    //if vpinstall apk name changed in fota, it can't be installed
// remove vp install
//    public void vpInstallItemsCheck() {
//        if (mIsLoaderTaskRunning == true) {
//            Log.d(TAG, "load task is running, wait");
//            sWorker.post(new Runnable() {
//                @Override
//                public void run() {
//                    vpInstallItemsCheck();
//                }
//            });
//            return;
//        }
//        Log.d(TAG, "vp install item check start");
//        //item list to be removed
//        final ArrayList<ItemInfo> removeinfos = new ArrayList<ItemInfo>();
//        List<PackageInfo> pkginfolist = null;
//        ArrayList<ItemInfo> allApps = getAllAppItems();
//        for(ItemInfo item: allApps) {
//            if (item.itemType == Favorites.ITEM_TYPE_VPINSTALL) {
//                Intent vpintent = ((ShortcutInfo)item).intent;
//                if (vpintent == null) {
//                    removeinfos.add(item);
//                    continue;
//                }
//                String path = vpintent.getStringExtra(VPUtils.TYPE_PACKAGEPATH);
//                if ((path ==null) || (path.equals(""))) {
//                    removeinfos.add(item);
//                    continue;
//                }
//                File vpfile = new File(path);
//                if ((vpfile != null) && (vpfile.exists() == true)) {
//                    continue;
//                }
//                Log.d(TAG, "vp item's apk not find " + path);
//                //if the apk can't find by path, find the same component name apk in /system/etc/property/vp-app
//                //and update vp item's path if same component name apk is found, or remove the vp item
//                String compname = vpintent.getStringExtra(VPUtils.TYPE_PACKAGENAME);
//                boolean componentfound = false;
//                //create pkginfolist only when it is needed
//                if (pkginfolist == null) {
//                    pkginfolist = mVPUtils.ScanVPInstallDir();
//                }
//                if (pkginfolist == null) {
//                    Log.d(TAG, "scan vp install dir failed");
//                    removeinfos.add(item);
//                    //creat a empty list
//                    pkginfolist = new ArrayList<PackageInfo>();
//                    continue;
//                }
//                for(PackageInfo pkgInfo: pkginfolist) {
//                    if ((pkgInfo.packageName != null) && (pkgInfo.packageName.equals(compname) == true)) {
//                        Log.d(TAG, "vp item's new path is " + pkgInfo.applicationInfo.sourceDir);
//                        vpintent.putExtra(VPUtils.TYPE_PACKAGEPATH, pkgInfo.applicationInfo.sourceDir);
//                        componentfound = true;
//                        final ItemInfo finalitem = item;
//                        final Intent intent = new Intent(vpintent);
//                        //run in another runnable to avoid block worker thread
//                        Runnable r = new Runnable() {
//                            @Override
//                            public void run() {
//                                final ContentValues values = new ContentValues();
//                                values.put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0));
//                                updateItemInDatabaseHelper(mApp, values, finalitem, "vpInstallItemsCheck");
//                            }
//                        };
//                        runOnWorkerThread(r);
//                        continue;
//                    }
//                }
//                if (componentfound == false) {
//                    removeinfos.add(item);
//                }
//            }
//        }
//
//        //remove all invalide vp items
//        if (removeinfos.size() > 0) {
//            notifyUIRemoveIcon(removeinfos, true, false);
//        }
//
//        if (pkginfolist != null) {
//            pkginfolist.clear();
//        }
//        Log.d(TAG, "vp install item check finish");
//    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
    //vp install
// remove vp install
//    public void vpInstallInit() {
//        if (mIsLoaderTaskRunning == true) {
//            Log.d(TAG, "load task is running, wait");
//            sWorker.post(new Runnable() {
//                @Override
//                public void run() {
//                    vpInstallInit();
//                }
//            });
//            return;
//        }
//        //get all vp package info from special dir
//        final ArrayList<ItemInfo> infos = mVPUtils.createVPInstallShortcutInfos();
//        if (infos == null) {
//            return;
//        }
//        Log.d(TAG, "infos list size is " + infos.size());
//        final ArrayList<ItemInfo> folderinfos = new ArrayList<ItemInfo>();
//        final ArrayList<ItemInfo> apkinfos = new ArrayList<ItemInfo>();
//        final HashMap<Long, FolderInfo> folderinfoshash = new HashMap<Long, FolderInfo>();
//        for (ItemInfo info: infos) {
//            if (info.itemType == Favorites.ITEM_TYPE_FOLDER) {
//                folderinfos.add(info);
//                folderinfoshash.put(info.id, (FolderInfo) info);
//            } else if (info.itemType == Favorites.ITEM_TYPE_VPINSTALL) {
//                if (info.container < 0) {
//                    apkinfos.add(info);
//                }
//            }
//        }
//        final Callbacks callbacks = LauncherModel.mCallbacks != null ? LauncherModel.mCallbacks.get() : null;
//        final Runnable r = new Runnable() {
//            @Override
//            public void run() {
//                if (callbacks != null) {
//                    Log.d(TAG, "bind folders first");
//                    callbacks.bindItemsAdded(folderinfos);
//                    callbacks.bindFolders(folderinfoshash);
//                    Log.d(TAG, "then bind vp items");
//                    if (apkinfos.size() > 0) {
//                        callbacks.bindItemsAdded(apkinfos);
//                    }
//                    folderinfos.clear();
//                    folderinfoshash.clear();
//                    apkinfos.clear();
//                    infos.clear();
//                }
//            }
//        };
//        if ((infos != null) && (infos.size() > 0)) {
//            LauncherModel.runOnMainThread(r);
//        }
//    }

    /*YUNOS BEGIN*/
    //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
    //vp install item icon display
// remove vp install
//    ArrayList<ItemInfo> VPInstallDrawingList = new ArrayList<ItemInfo>();
//    Runnable mVPInstallDrawLoading = new Runnable() {
//        public void run() {
//            final ArrayList<ItemInfo> drawingList = new ArrayList<ItemInfo>();
//            for (ItemInfo item: VPInstallDrawingList) {
//               int progress = ((ShortcutInfo)item).getProgress();
//               ((ShortcutInfo)item).setProgress(progress%100 + 5);
//               drawingList.add(item);
//            }
//            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
//            final Runnable r = new Runnable() {
//                @Override
//                public void run() {
//                    if (callbacks != null) {
//                        callbacks.bindItemsUpdated(drawingList);
//                    }
//                }
//            };
//            if (VPInstallDrawingList.size() > 0) {
//                runOnMainThread(r);
//                sWorker.postDelayed(mVPInstallDrawLoading, 500);
//            }
//        }
//    };
    /*YUNOS END*/
// remove vp install
//    public void startVPSilentInstall(ShortcutInfo shortcutinfo) {
//        final ShortcutInfo finalInfo = shortcutinfo;
//        Runnable r = new Runnable() {
//            public void run() {
//                Log.d(TAG, "start startVPSilentInstall in worker thread");
//                if (mVPUtils.installSilently(finalInfo) == true) {
//                    final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
//                    final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
//                    infos.add(finalInfo);
//                    final Runnable r = new Runnable() {
//                        @Override
//                        public void run() {
//                            if (callbacks != null) {
//                                callbacks.bindItemsUpdated(infos);
//                            }
//                        }
//                    };
//                    runOnMainThread(r);
//                    /*YUNOS BEGIN*/
//                    //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
//                    //vp install item icon display
//                    VPInstallDrawingList.add(finalInfo);
//                    sWorker.removeCallbacks(mVPInstallDrawLoading);
//                    sWorker.post(mVPInstallDrawLoading);
//                    /*YUNOS END*/
//                }
//            }
//        };
//        runOnWorkerThread(r);
//    }

    /*YUNOS BEGIN*/
    //##module(HomeShell)
    //##date:2014/04/11 ##author:hao.liuhaolh@alibaba-inc.com##BugID:109600
    //vp item still display loading after install finish
//    Object vplock  = new Object();

//    @Override
//    public void onInstallStateChange(AppKey appKey, int state) {
//        Log.d(TAG, "onInstallStateChange is called");
//        Log.d(TAG, "appKey is " + appKey.packageName + " state is " + state);
//        synchronized(vplock) {
//        /*YUNOS END*/
//            final AppKey finalappKey = appKey;
//            final int finalstate = state;
//            if ((state != VPInstaller.INSTALL_FAILED) && (state != VPInstaller.INSTALLED)) {
//                return;
//            }
//            sWorker.post(new Runnable(){
//                @Override
//                public void run() {
//                    Log.d(TAG, "appKey is " + finalappKey.packageName + " state is " + finalstate);
//                    ArrayList<ItemInfo> allApps = getAllAppItems();
//                    for (ItemInfo info: allApps) {
//                        if (info.itemType == Favorites.ITEM_TYPE_VPINSTALL) {
//                            if(finalappKey.packageName.equals(((ShortcutInfo)info).intent.getStringExtra(VPUtils.TYPE_PACKAGENAME))) {
//                                ((ShortcutInfo) info).setVPInstallStatus(VPInstallStatus.STATUS_NORMAL);
//                                /*YUNOS BEGIN*/
//                                //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
//                                //vp install item icon display
//                                boolean ret = VPInstallDrawingList.remove(info);
//                                /*YUNOS END*/
//                                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
//                                final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>();
//                                infos.add(info);
//                                final Runnable r = new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        if (callbacks != null) {
//                                            Log.d(TAG, "onInstallStateChange bindItemsUpdated");
//                                            /*
//                                            if (finalstate == VPInstaller.INSTALLED) {
//                                                ToastManager.makeToast(ToastManager.APP_EXIST);
//                                            } else
//                                            */
//                                            if (finalstate == VPInstaller.INSTALL_FAILED){
//                                                if (Utils.isInUsbMode()) {
//                                                    ToastManager.makeToast(ToastManager.APP_UNAVAILABLE_IN_USB);
//                                                } else {
//                                                    ToastManager.makeToast(ToastManager.APP_NOT_FOUND);
//                                                }
//                                            }
//                                            callbacks.bindItemsUpdated(infos);
//                                        }
//                                    }
//                                };
//                                runOnMainThread(r);
//                            }
//                        }
//                    }
//                }
//            });
//        }
//    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##module(HomeShell)
    //##date:2014/03/28 ##author:hao.liuhaolh@alibaba-inc.com##BugID: 104963
    //homeshell can't enter edit mode if package add intent is missed
    //BugID:5193043:download status error after app installed
    public void checkInstallingState() {
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                sWorker.removeCallbacks(mInstallingItemsCheck);
                sWorker.postDelayed(mInstallingItemsCheck, 5000);
            }
        };
        runOnWorkerThread(r);
    }

    //BugID:5193043:download status error after app installed
    public void stopCheckInstallingState() {
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                sWorker.removeCallbacks(mInstallingItemsCheck);
            }
        };
        runOnWorkerThread(r);
    }

    //this runnable is used to check all installing items install state
    //if the item is installed, change the item's statue from download to normal app
    //to avoid launcher model miss package add intent from system
    //if package add intent is missed, launcher model can't enter edit mode.
    Runnable mInstallingItemsCheck = new Runnable() {
        public void run() {
            ArrayList<ShortcutInfo> downloadItemsList = new ArrayList<ShortcutInfo>();
            Log.d(TAG, "mInstallingItemsCheck in");
            ArrayList<ItemInfo> allApps = getAllAppItems();
            for(ItemInfo item: allApps) {
                if (item.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                    downloadItemsList.add((ShortcutInfo)item);
                }
            }
            ArrayList<ShortcutInfo> rmList = new ArrayList<ShortcutInfo>();

            for (ShortcutInfo instInfo: downloadItemsList) {
                if (instInfo == null) {
                    continue;
                }
                //check app is installed
                Context context = LauncherApplication.getContext();
                PackageManager pm = context.getPackageManager();
                Intent intent = instInfo.intent;
                if ((intent == null) || (intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME) == null)) {
                    rmList.add(instInfo);
                    continue;
                }
                String pkgname = intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                Log.d(TAG, "check " + pkgname + " is installed");
                PackageInfo pkgInfo = null;
                boolean isInstalled = false;
                try {
                    pkgInfo = pm.getPackageInfo(pkgname, 0);
                    Log.d(TAG, "apk is installed");
                    isInstalled = true;
                    if (AppFreezeUtil.isPackageFrozen(context, pkgname)) {
                        Log.d(TAG, "apk is frozen");
                    }
                } catch (NameNotFoundException e) {
                    Log.d(TAG, "PackageManager.getPackageInfo failed for " + pkgname);
                    isInstalled = false;
                }
                if (isInstalled == true) {
                    //change shortcut info's state
                    Log.d(TAG, "call app add");
                    PackageUpdatedTask task = new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, new String[] { pkgname });
                    mPackageUpdateTaskQueue.enqueue(task);
                    rmList.add(instInfo);
                }
            }

            //remove not installing item from downloadItemsList
            for(ShortcutInfo rmInfo: rmList) {
                downloadItemsList.remove(rmInfo);
            }
            rmList.clear();

            if (isDownloadItemsListAvaliable(downloadItemsList) == true){
                sWorker.removeCallbacks(mInstallingItemsCheck);
                sWorker.postDelayed(mInstallingItemsCheck, 20000);
            } else {
                if (downloadItemsList.size() > 0) {
                    downloadItemsList.clear();
                }
            }
            Log.d(TAG, "mInstallingItemsCheck out");
        }
    };

    //BugID:5193043:download status error after app installed
    private boolean isDownloadItemsListAvaliable(ArrayList<ShortcutInfo> downloadItemsList) {
        boolean ret = false;
        if (downloadItemsList.size() <= 0) {
            return ret;
        }
        for (ShortcutInfo instInfo: downloadItemsList) {
            if (instInfo != null) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    /*YUNOS END*/

    /*YUNOS BEGIN LH*/
    private static void addToStringSet(SharedPreferences sharedPrefs,
            SharedPreferences.Editor editor, String key, String value) {
        Set<String> strings = sharedPrefs.getStringSet(key, null);
        if (strings == null) {
            strings = new HashSet<String>(0);
        } else {
            strings = new HashSet<String>(strings);
        }
        strings.add(value);
        editor.putStringSet(key, strings);
    }

    public void installShortcutInWorkerThread(Context contextin, Intent datain,
            String namein, final Intent intent, final SharedPreferences sharedPrefs) {
        //lxd #5582298 2014-11-18
        if(namein == null || TextUtils.isEmpty(namein.trim())) {
            Toast.makeText(contextin, contextin.getString(R.string.shortcut_name_empty), Toast.LENGTH_LONG).show();
            return;
        }
        //end #5582298
        Log.d(TAG, "installShortcutInWorkerThread in");
        final Context context = contextin;
        final Intent data = datain;
        final String name  = namein;
        final int userId = datain.getIntExtra(InstallShortcutReceiver.SHORTCUT_EXTRA_USERID, 0);

        //run in worker thread for all operations about bg list and db, to avoid async issue
        Runnable r = new Runnable() {
            public void run() {
                /*YUNOS BEGIN*/
                //##date:2013/12/10 ##author:hao.liuhaolh ##BugId: 73014
                //CloudApp isn't displayed
                long shortcutid = shortcutExists(context, name, intent, userId);
                Log.d(TAG, "shortcutid is " + shortcutid);
                boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
                if ((shortcutid != -1) && (duplicate == false)) {
                    //BugID:5233554:same intent but different title shortcut can't be updated
                    ItemInfo existItem = sBgItemsIdMap.get(shortcutid);
                    boolean isSame = true;
                    if ((existItem != null) && (existItem.itemType == Favorites.ITEM_TYPE_SHORTCUT)) {
                        String newName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                        if ((newName != null) && (existItem.title != null) &&
                                !(existItem.title.toString().equals(newName.toString()))) {
                            //same shortcut url but different title
                            //need to update title
                            isSame = false;
                            existItem.title = newName;
                            updateItemInDatabase(context, existItem);
                            //update the title in UI
                            final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
                            final ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
                            items.add(existItem);
                            final Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                                    if (callbacks != null) {
                                        callbacks.bindItemsUpdated(items);
                                    }
                                }
                            };
                            runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                            Toast.makeText(context, context.getString(R.string.shortcut_updated, name),
                                        Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (isSame == true) {
                        //BugID:144486:shortcut verify error.
                        Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name),
                                                   Toast.LENGTH_SHORT).show();
                    }
                } else {
                    /* YUNOS BEGIN */
                    // ##date:2015/3/24 ##author:sunchen.sc ##BugID:5735130
                    // Move find empty space to UI thread
                    Runnable runInUI1 = new Runnable() {
                        @Override
                        public void run() {
                            ScreenPosition pos = findEmptyCellAndOccupy(true);
                            if (pos != null) {
                                final ScreenPosition posFinal1 = pos;
                                Runnable runInWork1 = new Runnable() {

                                    @Override
                                    public void run() {
                                        final int screen = posFinal1.s;

                                        if (intent != null) {
                                            if (intent.getAction() == null) {
                                                intent.setAction(Intent.ACTION_VIEW);
                                            } else if (intent.getAction().equals(Intent.ACTION_MAIN) &&
                                                    intent.getCategories() != null &&
                                                    intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                                intent.addFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                            }

                /*YUNOS BEGIN*/
                //##date:2013/12/17 ##author:hao.liuhaolh ##BugID: 75210
                // icon override
                /*
                                            new Thread("setNewAppsThread") {
                                                public void run() {
                                                    // If the new app is going to fall into the same page as before,
                                                    // then just continue adding to the current page
                                                    final int newAppsScreen = sharedPrefs.getInt(
                                                            NEW_APPS_PAGE_KEY, screen);
                                                    SharedPreferences.Editor editor = sharedPrefs.edit();
                                                    if (newAppsScreen == -1 || newAppsScreen == screen) {
                                                        addToStringSet(sharedPrefs,
                                                            editor, NEW_APPS_LIST_KEY, intent.toUri(0));
                                                    }
                                                    editor.putInt(NEW_APPS_PAGE_KEY, screen);
                                                    editor.commit();
                                                }
                                            }.start();
                */
                /*YUNOS END*/
                                            ShortcutInfo info = addShortcut(context, data,
                                                    LauncherSettings.Favorites.CONTAINER_DESKTOP, posFinal1, false);
                                            info.userId = userId;
                                            if (info == null) {
                                                Toast.makeText(context, "Create info error",
                                                                   Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.shortcut_added, name),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                };
                                runOnWorkerThread(runInWork1);
                            } else {
                                Toast.makeText(context, context.getString(R.string.completely_out_of_space),
                                                       Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    runOnMainThread(runInUI1);
                    /* YUNOS END */
                }
                /*YUNOS END*/
            }
        };
        runOnWorkerThread(r);
    }
    /*YUNOS END*/

    public ShortcutInfo addShortcut(Context context, Intent data,
            long container, ScreenPosition pos, boolean notify) {
        final ShortcutInfo info = infoFromShortcutIntent(context, data, null);
        if (info == null) {
            return null;
        }
        /*YUNOS BEGIN*/
        //##date:2013/11/25 ##author:hongxing.whx ##BugId: 66163
        info.isNew = 1;
        /*YUNOS END*/
        /* YUNOS BEGIN */
        // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
        // Modified to support pad orientation
        if (LauncherApplication.isInLandOrientation()) {
            info.cellX = pos.xLand;
            info.cellY = pos.yLand;
        } else {
            info.cellX = pos.xPort;
            info.cellY = pos.yPort;
        }
        info.cellXPort = pos.xPort;
        info.cellYPort = pos.yPort;
        info.cellXLand = pos.xLand;
        info.cellYLand = pos.yLand;

        addItemToDatabase(context, info, container, pos.s, pos.xPort, pos.yPort, pos.xLand, pos.yLand, notify);
        info.initCellXYOnLoad((int) container, info.cellXPort, info.cellYPort, info.cellXLand, info.cellYLand);
        /* YUNOS END */
        //BugID: 117502: download item's icon doesn't change when theme change
        //save original icon in db and then render the icon by theme
        Drawable orgIcon = info.mIcon;
        info.setIcon(mApp.getIconManager().buildUnifiedIcon(orgIcon));

        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
        infos.add(info);
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindItems(infos, 0, infos.size());
                }
            }
        };
        mHandler.post(r);

        return info;
    }

    public void unInstallShortcutInWorkerThread(Context contextin,
            Intent datain,
                final SharedPreferences sharedPrefs) {
        Log.d(TAG, "unInstallShortcutInWorkerThread in");
        final Context context = contextin;
        final Intent data = datain;

        //run in worker thread for all operations about bg list and db, to avoid async issue
        Runnable r = new Runnable() {
            public void run() {
                String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                if (name == null) {
                    name = data.getStringExtra("label");
                }
                final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>();
                if(name != null) {
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    for(ItemInfo info: allApps) {
                        if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                             (info.title.equals(name))) {
                            Log.d(TAG, "find the delete item: id " + info.id);
                            infos.add(info);
                            break;
                        }
                    }
                }

                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (callbacks != null) {
                            callbacks.bindItemsRemoved(infos);
                        }
                    }
                };
                mHandler.post(r);
            }
        };
        runOnWorkerThread(r);
    }


    /**
     * Attempts to find an AppWidgetProviderInfo that matches the given component.
     */
    AppWidgetProviderInfo findAppWidgetProviderInfoWithComponent(Context context,
            ComponentName component) {
        List<AppWidgetProviderInfo> widgets =
            AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(component)) {
                return info;
            }
        }
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Added to support pad orientation
    private static boolean[][][] newOccupiedArray(int orientation) {
        int xCount = ConfigManager.getCellCountX(orientation);
        int yCount = ConfigManager.getCellCountY(orientation);
        int scrnCount = mMaxIconScreenCount;
        return new boolean[scrnCount][xCount][yCount];
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2013/11/21 ##author:hao.liuhaolh
    // cell and screen check
    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Modified to support pad orientation
    private static void initOccupied(boolean[][][] occupiedPort, boolean[][][] occupiedLand, ItemInfo... excludedInfos) {
        final int cellCountXPort = ConfigManager.getCellCountX(Configuration.ORIENTATION_PORTRAIT);
        final int cellCountYPort = ConfigManager.getCellCountY(Configuration.ORIENTATION_PORTRAIT);
        final int cellCountXLand = ConfigManager.getCellCountX(Configuration.ORIENTATION_LANDSCAPE);
        final int cellCountYLand = ConfigManager.getCellCountY(Configuration.ORIENTATION_LANDSCAPE);
        int screen;
        int x;
        int y;
        int spanX;
        int spanY;

        /* YUNOS BEGIN */
        // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5681074
        // fix concurrent modification exception
        List<ItemInfo> workspaceItems, appWidgets;
        synchronized (sBgLock) {
            workspaceItems = new ArrayList<ItemInfo>(sBgWorkspaceItems);
            appWidgets = new ArrayList<ItemInfo>(sBgAppWidgets);
            /* YUNOS BEGIN */
            // ##date:2015/02/12 ##author:zhanggong.zg ##BugID:5613700
            // Exclude specified items
            if (excludedInfos.length > 0) {
                List<ItemInfo> excluded = Arrays.asList(excludedInfos);
                workspaceItems.removeAll(excluded);
                appWidgets.removeAll(excluded);
            }
            /* YUNOS END */
        }
        /* YUNOS END */

        for (ItemInfo info : workspaceItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screen = info.screen;
                /* YUNOS BEGIN */
                // ##date:2013/12/17 ##author:wei.sun ##bugid: 75485
                // can not avoid invalid screen absolutely by now.
                if (!(screen > -1 && screen < mMaxIconScreenCount)) {
                    continue;
                }
                /* YUNOS END */

                spanX = info.spanX;
                spanY = info.spanY;

                if (occupiedPort != null) {
                    if (ConfigManager.isLandOrienSupport()) {
                        x = info.cellXPort;
                        y = info.cellYPort;
                    } else {
                        x = info.cellX;
                        y = info.cellY;
                    }
                    /* YUNOS BEGIN */
                    // ##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 78360
                    // check item's position is reasonable
                    if ((x >= cellCountXPort) || (y >= cellCountYPort) || (x + spanX > cellCountXPort) || (y + spanY > cellCountYPort)
                            || (x < 0) || (y < 0)) {
                        Log.d(TAG, "initOccupied item position error " + info.id);
                        continue;
                    }
                    /* YUNOS END */
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            occupiedPort[screen][x + i][y + j] = true;
                        }
                    }
                }

                if (occupiedLand != null) {
                    x = info.cellXLand;
                    y = info.cellYLand;
                    if ((x >= cellCountXLand) || (y >= cellCountYLand) || (x + spanX > cellCountXLand) || (y + spanY > cellCountYLand)
                            || (x < 0) || (y < 0)) {
                        Log.d(TAG, "initOccupied item position error " + info.id);
                        continue;
                    }
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            occupiedLand[screen][x + i][y + j] = true;
                        }
                    }
                }
            }
        }
        workspaceItems.clear();

        for (ItemInfo info : appWidgets) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screen = info.screen;
                spanX = info.spanX;
                spanY = info.spanY;

                if (occupiedPort != null) {
                    x = info.cellXPort;
                    y = info.cellYPort;
                    if ((x >= cellCountXPort) || (y >= cellCountYPort) || (x + spanX > cellCountXPort) || (y + spanY > cellCountYPort)
                            || (x < 0) || (y < 0) || (screen < 0) || (screen >= mMaxIconScreenCount)) {
                        Log.d(TAG, "initOccupied item position error " + info.id);
                        continue;
                    }
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            occupiedPort[screen][x + i][y + j] = true;
                        }
                    }
                }

                if (occupiedLand != null) {
                    x = info.cellXLand;
                    y = info.cellYLand;
                    if ((x >= cellCountXLand) || (y >= cellCountYLand) || (x + spanX > cellCountXLand) || (y + spanY > cellCountYLand)
                            || (x < 0) || (y < 0) || (screen < 0) || (screen >= mMaxIconScreenCount)) {
                        Log.d(TAG, "initOccupied item position error " + info.id);
                        continue;
                    }
                    for (int i = 0; i < spanX; i++) {
                        for (int j = 0; j < spanY; j++) {
                            occupiedLand[screen][x + i][y + j] = true;
                        }
                    }
                }
            }
        }
        appWidgets.clear();

        /*
        if(occupiedPort != null) {
            Log.d(TAG, "sxsexe_test   dump occupiedPort start  ");
            for(int i = 0; i < occupiedPort.length; i++) {
                for(int m = 0; m < cellCountXPort; m++) {
                    for(int n = 0; n < cellCountYPort; n++) {
                        Log.d(TAG, "sxsexe_test occupiedPort  screen " + i + " x " + m + " y " + n + " : " + occupiedPort[i][m][n]);
                    }
                }
            }
            Log.d(TAG, "sxsexe_test   dump occupiedPort end");
        }
        if(occupiedLand != null) {
            Log.d(TAG, "sxsexe_test   dump occupiedLand start");
            for(int i = 0; i < occupiedLand.length; i++) {
                for(int m = 0; m < cellCountXLand; m++) {
                    for(int n = 0; n < cellCountYLand; n++) {
                        Log.d(TAG, "sxsexe_test occupiedLand  screen " + i + " x " + m + " y " + n + " : " + occupiedLand[i][m][n]);
                    }
                }
            }
            Log.d(TAG, "sxsexe_test   dump occupiedLand end");
        }
        */
    }

    private static boolean checkPositionAvailable(boolean[][] bs, int x, int y, int spanX,
            int spanY) {
        int rowCount = bs.length;
        for (int i = 0; i < spanY; i++) {
            for (int j = 0; j < spanX; j++) {
                if (x + j >= rowCount || y + i >= bs[x + j].length) {
                    break;
                }
                if (bs[x + j][y + i]) {
                    return false;
                }
            }
        }
        return true;
    }

    /* YUNOS BEGIN */
    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Modified to support pad orientation
    private static ScreenPosition getAvailablePositionInScreen(int screenId, boolean[][] bs, int spanX, int spanY, int orientation) {
        int checkEndX = ConfigManager.getCellCountX(orientation) - spanX;
        int checkEndY = ConfigManager.getCellCountY(orientation) - spanY;
        for (int i = 0; i <= checkEndY; i++) {
            for (int j = 0; j <= checkEndX; j++) {
                if (checkPositionAvailable(bs, j, i, spanX, spanY)) {
                    ScreenPosition res = new ScreenPosition(screenId, j, i);
                    if (DEBUG_OCCUPY)
                        Log.d(TAG, "getAvailableInScreen() res screenId = " + screenId + ", j = " + j + ", i = " + i);
                    return res;
                }
            }
        }
        return null;
    }
    /* YUNOS END */

    private static ScreenPosition getScreenPositionLandSupport(int screen, int spanX, int spanY) {
        ScreenPosition resPort = getAvailablePositionInScreen(screen, sWorkspaceOccupiedPort.get(screen), spanX, spanY,
                Configuration.ORIENTATION_PORTRAIT);
        ScreenPosition resLand = getAvailablePositionInScreen(screen, sWorkspaceOccupiedLand.get(screen), spanX, spanY,
                Configuration.ORIENTATION_LANDSCAPE);
        ScreenPosition resCurrent = LauncherApplication.isInLandOrientation() ? resLand : resPort;
        if (resPort != null && resLand != null) {
            boolean occupyDragCell = isDragInfoOccupied(resCurrent.s, resCurrent.x, resCurrent.y);
            if (!occupyDragCell) {
                Log.d(TAG, "findEmptyCell out screen:" + resCurrent.s + " cellx:" + resCurrent.x + " celly:" + resCurrent.y);
                /* YUNOS BEGIN */
                // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                // Lock occupy the empty space, if the empty place has been
                // occupied, continue find empty space
                ScreenPosition result = new ScreenPosition(screen, resPort.x, resPort.y, resLand.x, resLand.y);
                (sWorkspaceOccupiedPort.get(screen))[resPort.x][resPort.y] = true;
                (sWorkspaceOccupiedLand.get(screen))[resLand.x][resLand.y] = true;
                return result;
                /* YUNOS END */
            }
        }
        return null;
    }

    private static ScreenPosition getScreenPositionNoLandSupport(int screen, int spanX, int spanY) {
        ScreenPosition res = getAvailablePositionInScreen(screen, sWorkspaceOccupiedCurrent.get(screen), spanX, spanX,
                Configuration.ORIENTATION_PORTRAIT);
        if (res != null) {
            boolean occupyDragCell = isDragInfoOccupied(res.s, res.x, res.y);
            if (!occupyDragCell) {
                Log.d(TAG, "findEmptyCell out screen:" + res.s + " cellx:" + res.x + " celly:" + res.y);
                /* YUNOS BEGIN */
                // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                // Lock occupy the empty space, if the empty place has
                // been
                // occupied, continue find empty space
                (sWorkspaceOccupiedCurrent.get(screen))[res.x][res.y] = true;
                return res;
                /* YUNOS END */
            }
        }

        return null;
    }

    /**
     * Return a screen position in on screen and one orientation
     *
     * @param screen
     * @param spanX
     * @param spanY
     * @param land
     * @return
     */
    public static ScreenPosition findEmptyCellAndOccupy(int screen, int spanX, int spanY, boolean land) {
        int xCount = getCellCountX();
        int yCount = getCellCountY();

        if (spanX > xCount || spanY > yCount) {
            Log.e(TAG, "checkAndFindEmptyCell: input is wrong:spanX=" + spanX + ",spanY=" + spanY);
            return null;
        }

        int orientation = land ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        ArrayList<boolean[][]> occupiedArray = land ? sWorkspaceOccupiedLand : sWorkspaceOccupiedPort;
        boolean[][] bs = occupiedArray.get(screen);
        ScreenPosition pos = getAvailablePositionInScreen(screen, bs, spanX, spanY, orientation);
        if (pos != null) {
            if (land) {
                for (int i = pos.xLand; i < (pos.xLand + spanX); i++) {
                    for (int j = pos.yLand; j < (pos.yLand + spanY); j++) {
                        bs[i][j] = true;
                    }
                }
            } else {
                for (int i = pos.xPort; i < (pos.xPort + spanX); i++) {
                    for (int j = pos.yPort; j < (pos.yPort + spanY); j++) {
                        bs[i][j] = true;
                    }
                }
            }
        }
        return pos;
    }

    public static ScreenPosition findEmptyCellAndOccupy(int startScreen, int endScreen, int spanX, int spanY) {
        Log.d(TAG, "sxsexe_pad   findEmptyCellAndOccupy  in startScreen startScreen " + startScreen + " endScreen " + endScreen);
        dumpUIOccupied();
        checkRunOnUIThread();
        int xCount = LauncherModel.getCellCountX();
        int yCount = LauncherModel.getCellCountY();

        if (spanX>xCount || spanY>yCount) {
            Log.d(TAG, "findEmptyCell,input is wrong:spanX="+spanX+",spanY="+spanY);
            return null;
        }

         //##date:2013/11/25 ##author:hongxing.whx
         // the main screen (index = 0) is special case, installed application and shotcuts will not be on it.
        if (startScreen >= sWorkspaceOccupiedCurrent.size() || startScreen < 0) {
            return null ;
        }

        boolean directionRight = true;
        if (startScreen <= endScreen) {
            directionRight = true;
        } else {
            directionRight = false;
        }
        Log.d(TAG, "findEmptyCellAndOccupy() startScreen = " + startScreen + " endScreen " + endScreen + ", spanX = " + spanX
                + ", spanY = " + spanY);
        ScreenPosition screenPosition = null;
        if (directionRight) {
            for (int i = startScreen; i <= endScreen; i++) {
                dumpCellOccupied(i);
                if (ConfigManager.isLandOrienSupport()) {
                    screenPosition = getScreenPositionLandSupport(i, spanX, spanY);
                } else {
                    screenPosition = getScreenPositionNoLandSupport(i, spanX, spanY);
                }
                if (screenPosition != null) {
                    break;
                }
            }
        } else {
            for (int i = startScreen; i >= endScreen; i--) {
                dumpCellOccupied(i);
                if (ConfigManager.isLandOrienSupport()) {
                    screenPosition = getScreenPositionLandSupport(i, spanX, spanY);
                } else {
                    screenPosition = getScreenPositionNoLandSupport(i, spanX, spanY);
                }
                if (screenPosition != null) {
                    break;
                }
            }
        }
        Log.d(TAG, "sxsexe_pad    findEmptyCell out screenPosition " + screenPosition);
        return screenPosition;
    }
    static HashMap<ItemInfo, ScreenPosition> findEmptyCellsAndOccupy(int startScreen, ArrayList<ItemInfo> invalidItems){
        if (invalidItems == null) {
            return null;
        }
        dumpUIOccupied();
        checkRunOnUIThread();
        int xCount = LauncherModel.getCellCountX();
        int yCount = LauncherModel.getCellCountY();

         //##date:2013/11/25 ##author:hongxing.whx
         // the main screen (index = 0) is special case, installed application and shotcuts will not be on it.
        if (startScreen >= sWorkspaceOccupiedCurrent.size()) {
            return null ;
        }

        HashMap<ItemInfo, ScreenPosition> posMap = new HashMap<ItemInfo, ScreenPosition>();
        int index = 0;
        while (index < invalidItems.size()) {
            ItemInfo item = invalidItems.get(index);
            if (item == null || (item != null && posMap.containsKey(item))) {
                continue;
            }
            if (item.spanX > xCount || item.spanY > yCount) {
                continue;
            }
            ScreenPosition position = findEmptyCellAndOccupy(startScreen, ConfigManager.getIconScreenMaxCount() - 1, item.spanX, item.spanY);
            if (position == null) {
                break;
            }

            posMap.put(item, position);
            index++;
        }

        Log.d(TAG, "findEmptyCell out posMap posMap.size " + posMap.size());
        return posMap;
    }
    static ScreenPosition findEmptyCellAndOccupyInBg(int startScreen, int spanX, int spanY){
        checkRunOnWorkerThread();
        int xCount = LauncherModel.getCellCountX();
        int yCount = LauncherModel.getCellCountY();
        Log.d(TAG, "findEmptyCellAndOccupyInBg in startScreen " + startScreen + " xCount " + xCount + " yCount " + yCount);
        int scrnCount = mMaxIconScreenCount;

        if (spanX>xCount || spanY>yCount) {
            Log.d(TAG, "findEmptyCell,input is wrong:spanX="+spanX+",spanY="+spanY);
            return null;
        }
        if (startScreen >= sWorkspaceOccupiedCurrent.size()) {
            return null;
        }
        ScreenPosition posResult = null;

        if (ConfigManager.isLandOrienSupport()) {
            boolean[][][] occupiedPort = newOccupiedArray(Configuration.ORIENTATION_PORTRAIT);
            boolean[][][] occupiedLand = newOccupiedArray(Configuration.ORIENTATION_LANDSCAPE);
            initOccupied(occupiedPort, occupiedLand);

            for (int i = startScreen; i < scrnCount; i++) {
                ScreenPosition posPort = getAvailablePositionInScreen(i, occupiedPort[i], spanX, spanX, Configuration.ORIENTATION_PORTRAIT);
                if (posPort != null) {
                    ScreenPosition posLand = getAvailablePositionInScreen(i, occupiedLand[i], spanX, spanX,
                            Configuration.ORIENTATION_LANDSCAPE);
                    if (posLand != null) {
                        posResult = new ScreenPosition(i, posPort.xPort, posPort.yPort, posLand.xLand, posLand.yLand);
                    }
                    if (posResult != null) {
                        break;
                    }
                }
            }
        } else {
            boolean[][][] occupiedPort = newOccupiedArray(Configuration.ORIENTATION_PORTRAIT);
            initOccupied(occupiedPort, null);
            for (int i = startScreen; i < scrnCount; i++) {
                posResult = getAvailablePositionInScreen(i, occupiedPort[i], spanX, spanX, Configuration.ORIENTATION_PORTRAIT);
                if (posResult != null) {
                    posResult.xPort = posResult.x;
                    posResult.yPort = posResult.y;
                    posResult.xLand = -1;
                    posResult.yLand = -1;
                    break;
                }
            }
        }

        Log.d(TAG, "findEmptyCell out posResult " + posResult);
        return posResult;
    }

    /* YUNOS BEGIN */
    // ##date:2014/7/23 ##author:zhanggong.zg ##BugID:5244146
    public static ScreenPosition findEmptyCellInHideSeat(boolean isInUiThread) {
        ScreenPosition[] pos = null;
        if (isInUiThread) {
            pos = findEmptyCellsInHideSeatAndOccupy(1);
        } else {
            pos = findEmptyCellsInHideSeatAndOccupyInBg(1);
        }
        if (pos == null) return null;
        else return pos[0];
    }

    public static ScreenPosition[] findEmptyCellsInHideSeatAndOccupy(int count) {
        if(DEBUG_OCCUPY)
            Log.d(TAG, "findEmptyCellsInHideSeatAndOccupy in");
        checkRunOnUIThread();
        if (count <= 0) return new ScreenPosition[0];

        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        final int xCount = ConfigManager.getHideseatMaxCountX();
        /* YUNOS END */
        final int yCount = ConfigManager.getHideseatMaxCountY();
        final int scrnCount = ConfigManager.getHideseatItemsMaxCount() / (xCount * yCount);

        List<ScreenPosition> rst = new ArrayList<ScreenPosition>(count);
        for (int screenId = 0; screenId < scrnCount; screenId++) {
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    if (!(sHideseatOccupied.get(screenId))[x][y]) {
                        /* YUNOS BEGIN */
                        // ##date:2015/3/18 ##author:sunchen.sc ##BugID:5735130
                        // Lock occupy the empty space, if the empty place has been occupied, continue find empty space
                        sHideseatOccupied.get(screenId)[x][y] = true;
                        rst.add(new ScreenPosition(screenId, x, y));
                        if (rst.size() == count) {
                            dumpUIOccupied();
                            if (DEBUG_OCCUPY)
                                Log.d(TAG, "findEmptyCellsInHideSeatAndOccupy out");
                            return rst.toArray(new ScreenPosition[count]);
                        }
                        /* YUNOS END */
                    }
                }
            }
        }
        return null;
    }
    /* YUNOS END */

    public static ScreenPosition[] findEmptyCellsInHideSeatAndOccupyInBg(int count) {
        Log.d(TAG, "findEmptyCellsInHideSeatAndOccupy in");
        checkRunOnWorkerThread();
        if (count <= 0) return new ScreenPosition[0];

        /*YUNOS BEGIN*/
        //##date:2014/10/29 ##author:zhangqiang.zq
        // aged mode
        final int xCount = ConfigManager.getHideseatMaxCountX();
        /* YUNOS END */
        final int yCount = ConfigManager.getHideseatMaxCountY();
        final int scrnCount = ConfigManager.getHideseatItemsMaxCount() / (xCount * yCount);

        boolean[][][] occupied = new boolean[scrnCount][xCount][yCount];

        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
        }
        /* YUNOS END */

        // No need init occupied
        for (ItemInfo info : tmpWorkspaceItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                if (info.screen < 0 || info.screen >= scrnCount) continue;
                if ((info.cellX >= xCount) || (info.cellY >= yCount) ||
                    (info.cellX < 0) || (info.cellY < 0)) {
                    continue;
                }
                occupied[info.screen][info.cellX][info.cellY] = true;
            }
        }

        List<ScreenPosition> rst = new ArrayList<ScreenPosition>(count);
        for (int screenId = 0; screenId < scrnCount; screenId++) {
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    if (!occupied[screenId][x][y]) {
                        rst.add(new ScreenPosition(screenId, x, y));
                        if (rst.size() == count) {
                            return rst.toArray(new ScreenPosition[count]);
                        }
                    }
                }
            }
        }
        return null;
    }
    /* YUNOS END */
    private static int obtainStartScreen() {
        // check if only one empty celllayout in workspace
        int startScreen = ConfigManager.DEFAULT_FIND_EMPTY_SCREEN_START;
        List<ItemInfo> workspaceItems;
        synchronized (sBgLock) {
            workspaceItems = new ArrayList<ItemInfo>(sBgWorkspaceItems);
        }
        for (ItemInfo info : workspaceItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && info.screen != ConfigManager.DEFAULT_HOME_SCREEN_INDEX) {
                startScreen = ConfigManager.DEFAULT_FIND_EMPTY_SCREEN_START;
                break;
            }
            startScreen = ConfigManager.DEFAULT_HOME_SCREEN_INDEX;
        }
        return startScreen;
    }

    public static ScreenPosition findEmptyCellAndOccupy(boolean isInUiThread) {
        if (isInUiThread) {
            return findEmptyCellAndOccupy(obtainStartScreen(), ConfigManager.getIconScreenMaxCount() - 1, 1, 1);
        } else {
            return findEmptyCellAndOccupyInBg(obtainStartScreen(), 1, 1);
        }
    }

    public static ScreenPosition findEmptyCellAndOccupy(int startScreen) {
        return findEmptyCellAndOccupy(startScreen, ConfigManager.getIconScreenMaxCount() - 1, 1, 1);
    }
    /**
     *
     * @param startScreen
     * @param direction 0:left, 1:right
     * @return
     */
    public static ScreenPosition findEmptyCellForFlingAndOccupy(int currentScr, int direction) {
        Log.d(TAG, "sxsexe-----------> ++++findEmptyCellForFling   currentScr " + currentScr + " direction " + direction);
        checkRunOnUIThread();
        if(direction == 0 && currentScr == 0) {
            return null;
        }
        if(direction == 1 && currentScr == (mMaxIconScreenCount - 1)) {
            return null;
        }
        int scrnCount = mMaxIconScreenCount;

        if (currentScr >= sWorkspaceOccupiedCurrent.size()) {
            return null ;
        }
        ScreenPosition screenPosition = null;
        if(direction == 1) {
            for (int i = currentScr + 1; i < scrnCount; i++) {
                if (DEBUG_OCCUPY)
                    Log.d(TAG, "findEmptyCellForFlingAndOccupy() screen = " + i + "direction = " + direction);
                dumpCellOccupied(i);
                dumpCellOccupied(sWorkspaceOccupiedCurrent.get(i));
                if (ConfigManager.isLandOrienSupport()) {
                    screenPosition = getScreenPositionLandSupport(i, 1, 1);
                } else {
                    screenPosition = getScreenPositionNoLandSupport(i, 1, 1);
                }
                if (screenPosition != null) {
                    return screenPosition;
                }
            }
        } else if(direction == 0) {
            for (int i = currentScr - 1; i >= 0; i--) {
                Log.d(TAG, "findEmptyCellAndOccupy() screen = " + i + "direction = " + direction);
                dumpCellOccupied(i);
                if (ConfigManager.isLandOrienSupport()) {
                    screenPosition = getScreenPositionLandSupport(i, 1, 1);
                } else {
                    screenPosition = getScreenPositionNoLandSupport(i, 1, 1);
                }
                if (screenPosition != null) {
                    return screenPosition;
                }
            }
        }

        Log.d(TAG, "sxsexe-----------> findEmptyCellForFling out screenPosition " + screenPosition);
        return screenPosition;
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2013/12/06 ##author:hao.liuhaolh ##BugID:69423
    //The items in hotseat restore
    public static int getHotSeatPosition(int oldposition) {
        int count = 0;
        int newposition = 0;
        int maxHotseatCount = ConfigManager.getHotseatMaxCount();
        ItemInfo[] infos = new ItemInfo[maxHotseatCount];
        ArrayList<ItemInfo> allItems = getAllAppItems();
        for (ItemInfo info: allItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                infos[info.screen] = info;
                count++;
                if (count >= maxHotseatCount) {
                    break;
                }
            }
        }

        if (count >= ConfigManager.getHotseatMaxCount()) {
            newposition = count;
        } else {
            if (oldposition <= count) {
                if (infos[oldposition] == null) {
                    newposition = oldposition;
                } else {
                    newposition = count;
                }
            } else {
                newposition = count;
            }
        }
        return newposition;
    }

    public static void rebindHotseat(Context context) {
        //get all hotseat items
        final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>();
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for (ItemInfo info: allApps) {
            if ((info != null) &&
                 (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT)) {
                 infos.add(info);
            }
        }

        Collections.sort(infos, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                int cellCountX = LauncherModel.getCellCountX();
                int cellCountY = LauncherModel.getCellCountY();
                int screenOffset = cellCountX * cellCountY;
                int containerOffset = screenOffset * (mMaxIconScreenCount + 1); // +1 hotseat
                long lr = (lhs.container * containerOffset + lhs.screen * screenOffset +
                        lhs.cellY * cellCountX + lhs.cellX);
                long rr = (rhs.container * containerOffset + rhs.screen * screenOffset +
                        rhs.cellY * cellCountX + rhs.cellX);
                return (int) (lr - rr);
            }
        });

        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindRebuildHotseat(infos);
                }
            }
        };
        mHandler.post(r);
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/03/20 ##author:hao.liuhaolh ##BugID:102492
    //items in hide seat backup restore issue
    public static ScreenPosition getHideSeatPositionAndOccupy(int screen, int x, int y) {
        Log.d(TAG, "getHideSeatPosition in screen: " + screen + " x: " + x + " y: " + y);
        checkRunOnUIThread();
        ScreenPosition p = null;

        final int mMaxHideSeatScrCount = ConfigManager.getHideseatScreenMaxCount();
        final int mMaxHideSeatXCount = ConfigManager.getHideseatMaxCountX();
        final int mMaxHideSeatYCount = ConfigManager.getHideseatMaxCountY();

        int xCount = mMaxHideSeatXCount;
        int yCount = mMaxHideSeatYCount;
        int scrnCount = mMaxHideSeatScrCount;
        int maxHideSeatCount = scrnCount * xCount * yCount;

        if ((x < xCount) && (y < yCount) && (screen < scrnCount)) {
            if ((sHideseatOccupied.get(screen))[x][y]) {
                Log.d(TAG, "the original position is occupied");
                for (int i = 0; i < mMaxHideSeatScrCount; i++) {
                    for (int j = 0; j < mMaxHideSeatYCount; j++) {
                        for (int k = 0; k < mMaxHideSeatXCount; k++) {
                            if ((sHideseatOccupied.get(i))[k][j] == false) {
                                (sHideseatOccupied.get(i))[k][j] = true;
                                    p = new ScreenPosition(i, k, j);
                                    break;
                            }
                        }
                        if (p != null) {
                            break;
                        }
                    }
                    if (p != null) {
                        break;
                    }
                }
            } else {
                p = new ScreenPosition(screen, x, y);
            }
        }
        dumpUIOccupied();
        Log.d(TAG, "getHideSeatPosition end");
        return p;
    }
    /*YUNOS END*/

    //BugID:5214634:widget and item icon overlap after homeshell start
    private static boolean isScreenEmpty(int screen) {
        boolean isempty = true;
        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems );
            tmpAppWidgets.addAll( sBgAppWidgets );
        }
        /* YUNOS END */
        for (ItemInfo item: tmpWorkspaceItems) {
            if ((item.screen == screen) &&
                (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)) {
                isempty = false;
                break;
            }
        }
        if (isempty == true) {
            for (ItemInfo item: tmpAppWidgets) {
                if (item.screen == screen) {
                    isempty = false;
                    break;
                }
            }
        }
        return isempty;
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/22 ##author:hao.liuhaolh
    //check empty screen and move items behind empty screen
    public static void checkEmptyScreen(Context context, int screen) {
        if ((screen <0) || (screen >= mMaxIconScreenCount)) {
            return;
        }
        final int srn = screen;
        final Context finalContext = context;
        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            public void run() {
                Log.d(TAG, "check empty screen start");
                boolean isempty = true;
                /* YUNOS BEGIN */
                // ##date:2015/03/16 ##author:sunchen.sc
                // Copy bg member to solve concurrent modification issue
                final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
                final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
                synchronized (sBgLock) {
                    tmpWorkspaceItems.addAll(sBgWorkspaceItems);
                    tmpAppWidgets.addAll(sBgAppWidgets);
                }
                /* YUNOS END */
                for (ItemInfo item: tmpWorkspaceItems) {
                    if ((item.screen == srn) &&
                        (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)) {
                        Log.d(TAG, "item screen is " + item.screen);
                        Log.d(TAG, "item id is " + item.id);
                        isempty = false;
                        break;
                    }
                }
                if (isempty == true) {
                    for (ItemInfo item: tmpAppWidgets) {
                        if (item.screen == srn) {
                            isempty = false;
                            break;
                        }
                    }
                }
                //update lists screen number first
                if (isempty == true) {
                    for (ItemInfo item: tmpWorkspaceItems) {
                        if ((item.screen > srn) &&
                            (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)) {
                                item.screen -= 1;
                        }
                    }
                    for (ItemInfo item: tmpAppWidgets) {
                        if (item.screen > srn) {
                            item.screen -= 1;
                        }
                    }
                }
                if (isempty == true) {
                    Log.d(TAG, "the screen is empty");
                    forScreenRemoveUpdate(cr, srn);
                    //BugID:5214634:widget and item icon overlap after homeshell start
                    checkNoSpaceList();
                }
                Log.d(TAG, "check empty screen finish");
            }
        };
        runOnWorkerThread(r);
    }

    static void forScreenRemoveUpdate(ContentResolver cr, int screenid) {
        Log.d(TAG, "forScreenRemoveUpdate in");
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
                new String[] { "_id", "screen"}, "screen>? and container=?",
                new String[] { String.valueOf(screenid), String.valueOf(LauncherSettings.Favorites.CONTAINER_DESKTOP)},
                null);
        if (c == null) {
            return;
        }
        int screenIndex = c.getColumnIndexOrThrow(
                LauncherSettings.Favorites.SCREEN);
        int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);

        try {
            c.moveToFirst();
            Log.d(TAG, "c count is " + c.getCount());
            if (c.getCount() > 0) {
                /*YUNOS BEGIN*/
                //##date:2014/03/11 ##author:hao.liuhaolh ##BugID: 99159
                //re-array screen database update issue during power off
                ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
                for (int i = 0; i < c.getCount(); i++) {
                    ContentValues values = new ContentValues();
                    values.put("screen", c.getInt(screenIndex) - 1);
                    int itemId = c.getInt(idIndex);
                    Uri uri = LauncherSettings.Favorites.getContentUri(itemId, false);
                    updateArgs args = new updateArgs(uri, values, null, null);
                    updatelist.add(args);
                    //cr.update(uri, values, null, null);
                    c.moveToNext();
                }
                LauncherApplication app = (LauncherApplication) LauncherApplication.getContext().getApplicationContext();
                if ((app != null) && (app.getLauncherProvider() != null)) {
                    Log.d(TAG, "before call bulkupdate");
                    app.getLauncherProvider().bulkUpdate(updatelist);
                }
                updatelist.clear();
                /*YUNOS END*/
            }

        } finally {
            c.close();
        }
        final int screen = screenid;
        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindRemoveScreen(screen);
                }
            }
        };
        runOnMainThread(r);
        Log.d(TAG, "forScreenRemoveUpdate out");
    }
    /*YUNOS END*/

    /*YUNOS BEGIN LH*/
    /**
     * modify item's "isNew" in db
     */
    public static void modifyItemNewStatusInDatabase(Context context,
            final ItemInfo item, boolean isNew) {
        Log.d(TAG, "modify item in db");
        String transaction = "DbDebug    Modify item new status (" + item.title + ") in db, id: " + item.id +
                " (" + item.isNewItem()+ ") --> " + "(" + isNew+ ")";
        //Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.setIsNewItem(isNew);

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.BaseLauncherColumns.IS_NEW, item.isNewItem()?1:0);

        updateItemInDatabaseHelper(context, values, item, "modifyItemNewStatusInDatabase");
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2013/11/23 ##author:hongxing.whx
    // update itemInfo in database and itemList, and may update view on workspace
    /**
     * This method is used to update itemInfo in database and itemList, and may update view
     * on workspace
     *
     * @param context
     * @param itemId: use it to get itemInfo from sBgItemsIdMap
     * @param values: contains the values which are changed
     * @param updateUI: true, after updating itemInfo in database and ItemList, will update
     *                  corresponding view on workspace
     */
    public static void updateItemById(Context context, long itemId, ContentValues values, boolean updateUI) {
        updateItemById(context, itemId, values, updateUI, false);
    }

    public static void updateItemById(Context context, long itemId, ContentValues values, boolean updateUI, boolean postIdle) {
        Log.d(TAG, "updateItemById: itemId="+itemId+",values="+values);
        if(values == null) {
            Log.e(TAG, "updateItemById: values = null");
            return;
        }

        ItemInfo modelItem = null;
        synchronized (sBgLock) {
            modelItem = sBgItemsIdMap.get(itemId);
            if (modelItem == null) {
                Log.e(TAG, "updateItemById: fail to find item which id is "+itemId);
                return;
            }

            if (values.containsKey(LauncherSettings.Favorites.MESSAGE_NUM)) {
                Long numObj = values.getAsLong(LauncherSettings.Favorites.MESSAGE_NUM);
                if (numObj!=null) {
                    Log.d(TAG, "old number = "+ modelItem.messageNum+ ", new number = "+numObj.intValue());
                    modelItem.messageNum = numObj.intValue();
                }
            }
        }

        //Update database
        updateItemInDatabaseHelper(context, values, modelItem, "updateItemById");

        if (updateUI) {
            Log.d(TAG, "need to update corresponding view on workspace");
            final ItemInfo finalitem = modelItem;
            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    ArrayList<ItemInfo> items = new ArrayList<ItemInfo>(1);
                    items.add(finalitem);
                    if (callbacks != null) {
                        callbacks.bindItemsUpdated(items);
                    }
                }
            };
            if (postIdle) {
                postRunnableIdle(r);
            } else {
                mHandler.post(r);
            }
          //runOnMainThread(r);
        }
    }
    /*YUNOS END*/

    public static void postRunnableIdle(Runnable r) {
        mHandler.postIdle(r);
    }

    public ShortcutInfo infoFromShortcutIntent(Context context, Intent data,
            Drawable fallbackIcon) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }

        Drawable icon = null;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;

        if (bitmap != null && bitmap instanceof Bitmap) {
            icon = Utilities.createIconDrawable(new FastBitmapDrawable((Bitmap)bitmap), context);
            customIcon = true;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(
                            iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = Utilities.createIconDrawable(
                            mIconManager.getFullResIcon(resources, id), context);
                } catch (Exception e) {
                    Log.w(TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }

        final ShortcutInfo info = new ShortcutInfo();

        if (icon == null) {
            if (fallbackIcon != null) {
                icon = fallbackIcon;
            } else {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
        }

        info.setIcon(icon);

        info.title = name;
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    /**
     * Return an existing FolderInfo object if we have encountered this ID previously,
     * or make a new one.
     */
    private static FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
            // No placeholder -- create a new instance
            folderInfo = new FolderInfo();
            folders.put(id, folderInfo);
        }
        return folderInfo;
    }

    public static final Comparator<ApplicationInfo> getAppNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<ApplicationInfo>() {
            public final int compare(ApplicationInfo a, ApplicationInfo b) {
                int result = collator.compare(a.title.toString(), b.title.toString());
                if (result == 0) {
                    result = a.componentName.compareTo(b.componentName);
                }
                return result;
            }
        };
    }
    public static final Comparator<ApplicationInfo> APP_INSTALL_TIME_COMPARATOR
            = new Comparator<ApplicationInfo>() {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            if (a.firstInstallTime < b.firstInstallTime) return 1;
            if (a.firstInstallTime > b.firstInstallTime) return -1;
            return 0;
        }
    };
    public static final Comparator<AppWidgetProviderInfo> getWidgetNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<AppWidgetProviderInfo>() {
            public final int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                return collator.compare(a.label.toString(), b.label.toString());
            }
        };
    }

    public static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }
    public static class ShortcutNameComparator implements Comparator<ResolveInfo> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, CharSequence> mLabelCache;
        ShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, CharSequence>();
            mCollator = Collator.getInstance();
        }
        ShortcutNameComparator(PackageManager pm, HashMap<Object, CharSequence> labelCache) {
            mPackageManager = pm;
            mLabelCache = labelCache;
            mCollator = Collator.getInstance();
        }
        public final int compare(ResolveInfo a, ResolveInfo b) {
            CharSequence labelA, labelB;
            ComponentName keyA = LauncherModel.getComponentNameFromResolveInfo(a);
            ComponentName keyB = LauncherModel.getComponentNameFromResolveInfo(b);
            if (mLabelCache.containsKey(keyA)) {
                labelA = mLabelCache.get(keyA);
            } else {
                labelA = a.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyA, labelA);
            }
            if (mLabelCache.containsKey(keyB)) {
                labelB = mLabelCache.get(keyB);
            } else {
                labelB = b.loadLabel(mPackageManager).toString();

                mLabelCache.put(keyB, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };
    public static class WidgetAndShortcutNameComparator implements Comparator<Object> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<Object, String> mLabelCache;
        WidgetAndShortcutNameComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<Object, String>();
            mCollator = Collator.getInstance();
        }
        public final int compare(Object a, Object b) {
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = (a instanceof AppWidgetProviderInfo) ?
                    ((AppWidgetProviderInfo) a).label :
                    ((ResolveInfo) a).loadLabel(mPackageManager).toString();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = (b instanceof AppWidgetProviderInfo) ?
                    ((AppWidgetProviderInfo) b).label :
                    ((ResolveInfo) b).loadLabel(mPackageManager).toString();
                mLabelCache.put(b, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };
    /* YUNOS BEGIN */
    // ##date:2015/8/12 ##author:zhanggong.zg ##BugID:6312246
    // shortcuts provided by same app will be put together
    public static class ShortcutWidgetComparator implements Comparator<ResolveInfo> {
        private Collator mCollator;
        private PackageManager mPackageManager;
        private HashMap<ResolveInfo, String> mLabelCache;
        ShortcutWidgetComparator(PackageManager pm) {
            mPackageManager = pm;
            mLabelCache = new HashMap<ResolveInfo, String>();
            mCollator = Collator.getInstance();
        }
        public final int compare(ResolveInfo a, ResolveInfo b) {
            // shortcuts provided by same app will be put together
            String appName1 = a.activityInfo.packageName;
            String appName2 = b.activityInfo.packageName;
            int rst = appName1.compareTo(appName2);
            if (rst != 0) return rst;

            // compare shortcut label
            String labelA, labelB;
            if (mLabelCache.containsKey(a)) {
                labelA = mLabelCache.get(a);
            } else {
                labelA = a.loadLabel(mPackageManager).toString();
                mLabelCache.put(a, labelA);
            }
            if (mLabelCache.containsKey(b)) {
                labelB = mLabelCache.get(b);
            } else {
                labelB = b.loadLabel(mPackageManager).toString();
                mLabelCache.put(b, labelB);
            }
            return mCollator.compare(labelA, labelB);
        }
    };
    /* YUNOS END */
    public void dumpState() {
        Log.d(TAG, "mCallbacks=" + mCallbacks);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", mBgAllAppsList.data);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", mBgAllAppsList.added);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", mBgAllAppsList.removed);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", mBgAllAppsList.modified);
        if (mLoaderTask != null) {
            mLoaderTask.dumpState();
        } else {
            Log.d(TAG, "mLoaderTask=null");
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/9/26 ##author:zhanggong.zg ##BugID:5244146
    // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5705812
    /**
     * Retrieves groups of <code>ShortcutInfo</code> which are corresponding to given package names.
     * <p><strong>NOTE THAT</strong> this method only returns items whose type are
     * <code>ITEM_TYPE_APPLICATION</code> or <code>ITEM_TYPE_NOSPACE_APPLICATION</code>.
     * @param pkgNames a group of package names
     * @return a map from package name to collection of <code>ShortcutInfo</code>
     */
    public static Map<String, Collection<ShortcutInfo>> getAllShortcutInfoByPackageNames(Collection<String> pkgNames) {
        if (pkgNames == null || pkgNames.isEmpty()) return Collections.emptyMap();

        List<ItemInfo> allitems = null;
        synchronized (sBgLock) {
            allitems = new ArrayList<ItemInfo>(sBgItemsIdMap.values());
            // ##date:2015/1/14 ##author:zhanggong.zg ##BugID:5705812
            // includes no-space items
            allitems.addAll(sBgNoSpaceItems);
        }

        Map<String, Collection<ShortcutInfo>> rst = new HashMap<String, Collection<ShortcutInfo>>(pkgNames.size());
        for (String pkgName : pkgNames) {
            rst.put(pkgName, new HashSet<ShortcutInfo>(1));
        }
        Set<String> pkgSet = rst.keySet();

        for (ItemInfo item : allitems) {
            if (item instanceof ShortcutInfo) {
                if (item.itemType != Favorites.ITEM_TYPE_APPLICATION &&
                    item.itemType != Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                    continue;
                }
                Intent intent = ((ShortcutInfo) item).intent;
                if (intent == null || intent.getComponent() == null) continue;
                String pkgName = intent.getComponent().getPackageName();
                if (pkgSet.contains(pkgName)) {
                    rst.get(pkgName).add((ShortcutInfo) item);
                }
            }
        }
        return rst;
    }
    /* YUNOS END */

    public void notifyUIAddIcon(final ArrayList<ItemInfo> apps){
        Log.d(TAG,"LauncherMode : notifyUIAddIcon begin");
        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;

        if (callbacks != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callbacks == mCallbacks.get()) {
                        callbacks.bindItemsAdded(apps);
                    }
                }
            });
        }
        Log.d(TAG,"LauncherMode : notifyUIAddIcon end");
    }

    /* YUNOS BEGIN */
    // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
    // homeshell boot anr
    public void notifyUIUpdateIcon(final ArrayList<ItemInfo> apps) {
        notifyUIUpdateIcon(apps,false);
    }

    private void notifyUIUpdateIcon(final ArrayList<ItemInfo> apps,final boolean isThemeChange){
        Log.d(TAG,"LauncherMode : notifyUIUpdateIcon begin");
        final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
        //BugID:5211661:anr in theme change
        int N = apps.size();
        int updateItemsChunk = 50;
        if (oldCallbacks == null) {
            /* YUNOS BEGIN */
            // ## date: 2016/08/16 ## author: yongxing.lyx
            // ## BugID:8410785:sometimes loading dailog would be dismissed after changed theme
            Utils.dismissLoadingDialog();
            Log.w(TAG, "notifyUIUpdateIcon() failed! oldCallbacks == null");
            /* YUNOS END */
            return;
        }
        for (int i = 0; i < N; i += updateItemsChunk) {
            final int start = i;
            final int chunkSize = (i+updateItemsChunk <= N) ? updateItemsChunk : (N-i);
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindItemsChunkUpdated(apps, start, start + chunkSize,
                                isThemeChange);
                    } else {
                        /* YUNOS BEGIN */
                        // ## date: 2016/08/16 ## author: yongxing.lyx
                        // ## BugID:8410785:sometimes loading dailog would be dismissed after changed theme
                        Utils.dismissLoadingDialog();
                        Log.w(TAG, "notifyUIUpdateIcon() failed! callbacks == null");
                        /* YUNOS END */
                    }
                }
            };
            /* YUNOS BEGIN */
            // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
            // homeshell boot anr
            if(isThemeChange) {
                runOnMainThread(r, MAIN_THREAD_THEME_CHANGE_RUNNABLE);
            } else {
                /* YUNOS BEGIN */
                // ##date:2014/09/287 ##author:xindong.zxd ##BugId:5241941
                // execute the Runnable that update or download application icon when UI thread idle
                ShortcutInfo info = (ShortcutInfo) apps.get(0);
                if (info != null
                        && info.getAppDownloadStatus() != AppDownloadStatus.STATUS_NO_DOWNLOAD) {
                    mHandler.postIdle(r, MAIN_THREAD_BINDING_RUNNABLE);
                } else {
                    runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
                }
                /* YUNOS END */
            }
            /* YUNOS END */
        }
        Log.d(TAG,"LauncherMode : notifyUIUpdateIcon end");
    }
    /* YUNOS END */

    public void notifyUIUpdateDownloadIcon(ShortcutInfo item){
        Log.d(TAG,"LauncherMode : notifyUIUpdateDownloadIcon begin");
        final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
        if (oldCallbacks == null) {
            return;
        }
        final ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>(1);
        infos.add(item);
        final IconManager iconManager = mIconManager;
        final ShortcutInfo finalitem = item;
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Intent intent = finalitem.intent;
                if (intent != null) {
                    iconManager.clearCardBackgroud(finalitem.intent);
                }
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.bindItemsUpdated(infos);
                }
            }
        };
        runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
        Log.d(TAG,"LauncherMode : notifyUIUpdateDownloadIcon end");
    }
    /* YUNOS END */


    public void notifyUIRemoveIcon(final ArrayList<ItemInfo> apps, final boolean permanent,
            final boolean isFromAppInstall){
        Log.d(TAG,"LauncherMode : notifyUIRemoveIcon begin");
        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;

        mHandler.post(new Runnable(){
            @Override
            public void run() {
                if(callbacks != null && callbacks == mCallbacks.get()) {
                    callbacks.bindDownloadItemsRemoved(apps, permanent);
                }
            }
        });
        Log.d(TAG,"LauncherMode : notifyUIRemoveIcon end");
    }

    public boolean isDownloadStatus(){
        if(mAppDownloadMgr!=null&&mAppDownloadMgr.isDownloadStatus()){
            return true;
        }
        return false;
    }

    /*YUNOS BEGIN*/
    //##date:2013/12/27 ##author:hao.liuhaolh ##BugID: 79444
    //widget and icons override after restore
    //in restore status, empty cell can't be removed befor download start
    public boolean isEmptyCellCanBeRemoved(){
        if (BackupManager.getInstance().isInRestore()){
            return false;
        }
        return true;
    }
    /*YUNOS END*/

    //BugID: 5204254:mLauncher null pointer exception in LauncherModel
    public static boolean checkGridSize(int countX, int countY) {
        int maxCountX = LauncherModel.getCellMaxCountX();
        if (countX > maxCountX) {
            return false;
        }

        int maxCountY = LauncherModel.getCellMaxCountY();
        if (countY > maxCountY) {
            return false;
        }

        return true;
    }

    public void onFontChanged() {
        bindItemOnThemeChange(getSbgWorkspaceItems());
    }

    public void onThemeChange() {
        post(new Runnable() {
            @Override
            public void run() {
                /* YUNOS BEGIN */
                // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
                // remove the remaining Runnable in the queue
                mHandler.cancelAllRunnablesOfType(MAIN_THREAD_THEME_CHANGE_RUNNABLE);
                /* YUNOS END */
                //first IconManager reset cache;
                mIconManager.notifyThemeChanged();
                Launcher.sReloadingForThemeChangeg = true;
                //bugid: 5258002: HomeShellSetting used to know theme changed event by
                //theme changed listener which called IconManager.notifyThemeChanged,
                //but LauncherModel called IconManager.notifyThemeChanged here. This caused
                //resource accessing conflict. Now notify HomeShellSetting by broadcast intent
                mApp.sendBroadcast(new Intent(HomeShellSetting.THEME_CHANGED_ACTION));
                reloadWorkspace();
                /* YUNOS BEGIN */
                // ##date:2014/4/3 ##author:zhangqiang.zq
                // gadget support theme changed
                final ArrayList<Object> widgetsAndShortcuts = getSortedWidgetsAndShortcuts(mApp);
                // BugID:5187767:ConcurrentModificationException about widget
                // list
                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindPackagesUpdated(widgetsAndShortcuts);
                        }
                    }
                });
                /* YUNOS END */
            }
        });
    }

    private void reloadWorkspace(){
        //BugID:5212981:no space items theme change error
        //BugID:5629686:load all items icon and bgcard in worker thread, no only in sBgWorkspaceItem
        loadItemOnThemeChange(getAllAppItems());
        FolderIcon.onThemeChanged();
        bindItemOnThemeChange(getSbgWorkspaceItems());
    }

    private void loadItemOnThemeChange(List<ItemInfo> list) {
        FancyIconsHelper.clearCache();
        ThemeResources.refresh();
        Context context = mApp;
        //mIconManager.notifyThemeChanged();
        boolean hasBanner = false;
        boolean isUsbMode = Utils.isInUsbMode();
        HashMap<Long, Bitmap> iconsFromDB = getPrimaryIconFromDB(context);

        /* YUNOS BEGIN */
        // ##date:2014/4/3 ##author:zhangqiang.zq
        // gadget support theme changed
        Map<String, GadgetInfo> gadgets = ThemeUtils.listGadgets(context);
        /* YUNOS END */
        for (ItemInfo info : list) {
            if (info instanceof ShortcutInfo) {
                Bitmap iconDB = iconsFromDB.get(info.id);
                Drawable icon = mIconManager.getAppUnifiedIcon(info,null);
                if(mIconManager.isDefaultIcon(icon) && iconDB!=null){
                    icon = new FastBitmapDrawable(iconDB);
                    /*YUNOS BEGIN*/
                    //##module(IconManager)
                    //##date:2014/03/36 ##author:jun.dongj@alibaba-inc.com##BugID:104255
                    //the icons of sdcard apps are the default icon after fota or reboot
                    mIconManager.addAppIconToCache(((ShortcutInfo) info).intent, icon);
                    /*YUNOS END*/
                }
                ((ShortcutInfo) info).setIcon(icon);
                //create card bg in worker thread
                //to avoid card create in UI thread
                if (mIconManager.supprtCardIcon() == true) {
                    mIconManager.getAppCardBackgroud(info);
                }
            } else if (info instanceof FolderInfo) {
                for (Iterator it = ((FolderInfo)info).contents.iterator(); it.hasNext();) {
                    ShortcutInfo si = (ShortcutInfo)it.next();
//                for (ShortcutInfo si : ((FolderInfo)info).contents) {
                    /*YUNOS BEGIN*/
                    //##date:2014/06/27 ##author:yangshan.ys
                    //open a folder when theme is changing,prevent the icon change for the editfoldershortcut
                     if(si.isEditFolderShortcut()) {
                         continue;
                     }
                     /*YUNOS END*/
                     Bitmap iconDB = iconsFromDB.get(si.id);
                     Drawable icon = mIconManager.getAppUnifiedIcon(si,null);
                     if(mIconManager.isDefaultIcon(icon) && iconDB!=null){
                        icon = new FastBitmapDrawable(iconDB);
                     }
                     si.setIcon(icon);
                }
            } else if (info instanceof GadgetItemInfo) {
                /* YUNOS BEGIN */
                // ##date:2014/4/3 ##author:zhangqiang.zq
                // gadget support theme changed
                GadgetItemInfo gadgetItemInfo = (GadgetItemInfo) info;
                GadgetInfo gadgetInfo = gadgets
                        .get(gadgetItemInfo.gadgetInfo.label);
                gadgetItemInfo.gadgetInfo = gadgetInfo;
            }
            /* YUNOS END */
        }

        Log.d(TAG, "loadItemOnThemeChange hasBanner : " + hasBanner + " isUsbMode : " + isUsbMode);
//        if (!hasBanner) {
//            AliAppWidgetInfo info = AliAppWidgetInfo.getInfo("banner");
//            info.screen = 0;
//            info.cellX = 0;
//            info.cellY = 0;
//            info.spanX = 4;
//            info.spanY = 1;
//            if (isCellEmtpy(info.screen, info.cellX, info.cellY, info.spanX, info.spanY) != null) {
//                addItemToDatabase(mApp, info, info.container,
//                        info.screen, info.cellX, info.cellY, false);
//                ArrayList<ItemInfo> banner = new ArrayList<ItemInfo>();
//                banner.add(info);
//                notifyUIAddIcon(banner);
//            }
//        }
    }

    /**
     * Find the item's id and icon in db that ICON_TYPE is ICON_TYPE_BITMAP
     * @param context
     * @return hashmap\uff0ckey is id\uff0cvalue is icon(Bitmap)
     */
    private HashMap<Long, Bitmap> getPrimaryIconFromDB(Context context) {
        HashMap<Long, Bitmap> idIconMap = new HashMap<Long, Bitmap>();
        String key = LauncherSettings.Favorites.ICON_TYPE;
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(Favorites.CONTENT_URI,
                new String[] {Favorites._ID, Favorites.ICON,Favorites.TITLE },
                key + "=?",
                new String[] { String.valueOf(Favorites.ICON_TYPE_BITMAP) },
                null);

        if (cursor != null) {
            int idIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);

            while (cursor.moveToNext()) {
                Long id = cursor.getLong(idIndex);
                IconCursorInfo cursorInfo = new IconCursorInfo(cursor,iconIndex);
                Drawable cursorIcon = (FastBitmapDrawable)mApp.getIconManager().getIconFromCursor(cursorInfo);
                if (cursorIcon == null) {
                    cursorIcon = (FastBitmapDrawable)mApp.getIconManager().getDefaultIcon();
                }
                //BugID:122827:sdcard app icon wrong on theme change when sdcard unmount
                if (cursorIcon != null) {
                    cursorIcon = ((LauncherApplication)context.getApplicationContext()).getIconManager().buildUnifiedIcon(cursorIcon, ThemeUtils.ICON_TYPE_APP_TEMPORARY);
                }
                if (cursorIcon != null) {
                    Bitmap bitmap = ((FastBitmapDrawable) cursorIcon).getBitmap();
                    idIconMap.put(id, bitmap);
                }
                /*YUNOS END*/
            }
            cursor.close();
        }

        return idIconMap;
    }

    private void bindItemOnThemeChange(ArrayList<ItemInfo> list) {
        /* YUNOS BEGIN */
        // ##date:2014/09/17 ##author:xindong.zxd ##BugId:5233315
        // homeshell boot anr
        notifyUIUpdateIcon(list,true);
        /*YUNOS END*/
    }

    /*YUNOS BEGIN*/
    //##date:2014/1/9 ##author:zhangqiang.zq
    //screen edit
    public static void reArrageScreen(Context context, final List<Integer> newIndexs) {
        // TODO Auto-generated method stub
        Log.d(TAG, "forScreenRemoveUpdate in");
        final Map<Integer, Integer> screenExchangeMap = getScreenExchangeMap(
                getOldScreenIndexs(), newIndexs);
        if (screenExchangeMap == null) {
            return;
        }

        Map<ItemInfo, ItemInfo> map = new HashMap<ItemInfo, ItemInfo>();
        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        /* YUNOS END */
        for (int i = 0; i < tmpWorkspaceItems.size(); i++) {
            ItemInfo item = tmpWorkspaceItems.get(i);
            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                    || map.containsKey(item)) {
                continue;
            }
            map.put(item, item);
            item.screen = screenExchangeMap.get(item.screen);
        }

        for (int i = 0; i < tmpAppWidgets.size(); i++) {
            ItemInfo item = tmpAppWidgets.get(i);
            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                    || map.containsKey(item)) {
                continue;
            }
            map.put(item, item);
            item.screen = screenExchangeMap.get(item.screen);
        }
        map.clear();

        final ContentResolver cr = context.getContentResolver();

        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Cursor c = cr
                        .query(LauncherSettings.Favorites.CONTENT_URI,
                        new String[] { "_id", "screen" },
                        "container=?",
                        new String[] {
                                String.valueOf(LauncherSettings.Favorites.CONTAINER_DESKTOP) },
                        null);
                if (c == null) {
                    return;
                }

                int idIndex = c
                        .getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                int screenIndex = c
                        .getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);

                try {
                    c.moveToFirst();
                    if (c.getCount() > 0) {
                        /*YUNOS BEGIN*/
                        //##date:2014/03/11 ##author:hao.liuhaolh ##BugID: 99159
                        //re-array screen database update issue during power off
                        ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
                        for (int i = 0; i < c.getCount(); i++) {
                            int oldScreen = c.getInt(screenIndex);
                            if (oldScreen < 0) {
                                continue;
                            }

                            int newScreen = screenExchangeMap.get(oldScreen);
                            if (oldScreen == newScreen) {
                                c.moveToNext();
                                continue;
                            }
                            ContentValues values = new ContentValues();
                            values.put("screen", newScreen);
                            int itemId = c.getInt(idIndex);
                            Uri uri = LauncherSettings.Favorites.getContentUri(
                                    itemId, false);
                            //cr.update(uri, values, null, null);
                            updateArgs args = new updateArgs(uri, values, null, null);
                            updatelist.add(args);
                            c.moveToNext();
                        }
                        if (updatelist.size() > 0) {
                            LauncherApplication app = (LauncherApplication) LauncherApplication.getContext().getApplicationContext();
                            if ((app != null) && (app.getLauncherProvider() != null)) {
                                Log.d(TAG, "before call bulkupdate");
                                app.getLauncherProvider().bulkUpdate(updatelist);
                            }
                            updatelist.clear();
                        }
                        /*YUNOS END*/
                    }
                } finally {
                    c.close();
                }
            }
        });
    }

    public static void exchangeScreen(Context context, final int prev, final int next) {
        Log.d(TAG, "exchangeScreen in");
        Map<ItemInfo, ItemInfo> map = new HashMap<ItemInfo, ItemInfo>();
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        for (int i = 0; i < tmpWorkspaceItems.size(); i++) {
            ItemInfo item = tmpWorkspaceItems.get(i);
            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                    || map.containsKey(item)) {
                continue;
            }
            map.put(item, item);
            if (item.screen == prev || item.screen == next) {
                item.screen = (item.screen == prev) ? next : prev;
            }
        }
        for (int i = 0; i < tmpAppWidgets.size(); i++) {
            ItemInfo item = tmpAppWidgets.get(i);
            if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                    || map.containsKey(item)) {
                continue;
            }
            map.put(item, item);
            if (item.screen == prev || item.screen == next) {
                item.screen = (item.screen == prev) ? next : prev;
            }
        }
        map.clear();

        final ContentResolver cr = context.getContentResolver();

        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Cursor c = cr
                        .query(LauncherSettings.Favorites.CONTENT_URI,
                        new String[] { "_id", "screen" },
                        "container=?",
                        new String[] { String.valueOf(LauncherSettings.Favorites.CONTAINER_DESKTOP) },
                        null);
                if (c == null) {
                    return;
                }

                int idIndex = c
                        .getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                int screenIndex = c
                        .getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);

                try {
                    c.moveToFirst();
                    if (c.getCount() > 0) {
                        ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
                        for (int i = 0; i < c.getCount(); i++) {
                            int screen = c.getInt(screenIndex);
                            if (screen < 0) {
                                continue;
                            }
                            if (screen == prev || screen == next) {
                                ContentValues values = new ContentValues();
                                values.put("screen", screen == prev ? next : prev);
                                int itemId = c.getInt(idIndex);
                                Uri uri = LauncherSettings.Favorites.getContentUri(itemId, false);
                                updateArgs args = new updateArgs(uri, values, null, null);
                                updatelist.add(args);
                            }
                            c.moveToNext();
                        }
                        if (updatelist.size() > 0) {
                            LauncherApplication app = (LauncherApplication) LauncherApplication.getContext().getApplicationContext();
                            if ((app != null) && (app.getLauncherProvider() != null)) {
                                Log.d(TAG, "before call bulkupdate");
                                app.getLauncherProvider().bulkUpdate(updatelist);
                            }
                            updatelist.clear();
                        }
                    }
                } finally {
                    c.close();
                }
            }
        });
    }

    private static Map<Integer, Integer> getScreenExchangeMap(
            List<Integer> oldIndexs,
            List<Integer> newIndexs) {
        Log.d(TAG, "getScreenExchangeMap" + oldIndexs + "," + newIndexs);
        if (newIndexs == null || oldIndexs == null
                || oldIndexs.size() != newIndexs.size()) {
            return null;
        }

        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        int count = newIndexs.size();
        int[] l2pMap = new int[count];
        for (int i = 0; i < count; i++) {
            int j = newIndexs.get(i);
            l2pMap[j] = i;
        }

        for (int i = 0; i < count; i++) {
            map.put(oldIndexs.get(i), l2pMap[i]);
        }

        Log.d(TAG, "getScreenExchangeMap, result map" + map);
        return map;
    }

    private static List<Integer> getOldScreenIndexs() {
        List<Integer> screens = new ArrayList<Integer>();
        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        /* YUNOS END */
        for (int i = 0; i < tmpWorkspaceItems.size(); i++) {
            ItemInfo item = tmpWorkspaceItems.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    && !screens.contains(item.screen)) {
                Log.d(TAG, "" + item.screen + ":" + item);
                screens.add(item.screen);
            }
        }

        for (int i = 0; i < tmpAppWidgets.size(); i++) {
            ItemInfo item = tmpAppWidgets.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    && !screens.contains(item.screen)) {
                screens.add(item.screen);
            }
        }

        Collections.sort(screens);
        return screens;
    }
    /*YUNOS END*/

    public static void sendStatus() {
        // TODO Auto-generated method stub
        UserTrackerHelper.iconStatus(sBgWorkspaceItems);
        List<FolderInfo> list = new ArrayList<FolderInfo>();

        synchronized (sBgLock) {
            for (Long key : sBgFolders.keySet()) {
                list.add(sBgFolders.get(key));
            }
        }
        UserTrackerHelper.folderStatus(list);

        //BugID:5717551:userTrack for widget and shortcut count
        int wcount = 0;
        int scount = 0;
        boolean hasAccelerate = false, hasLockScreen = false;
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for (ItemInfo item : allApps) {
            if ((item.itemType == Favorites.ITEM_TYPE_GADGET) || (item.itemType == Favorites.ITEM_TYPE_APPWIDGET)) {
                wcount++;
            }
            else if (item.itemType == Favorites.ITEM_TYPE_SHORTCUT) {
                scount++;
                if (!hasAccelerate && HomeShellGadgetsRender.isOneKeyAccerateShortCut(item)) {
                    hasAccelerate = true;
                } else if (!hasLockScreen && HomeShellGadgetsRender.isOneKeyLockShortCut(item)) {
                    hasLockScreen = true;
                }
            }
        }
        allApps.clear();
        UserTrackerHelper.wigdetStatus(wcount, hasAccelerate, hasLockScreen);
        UserTrackerHelper.shortcutStatus(scount);
    }

    /*YUNOS BEGIN*/
    //##date:2014/04/26##author:hao.liuhaolh@alibaba-inc.com##BugID:114988
    //find and remove one item folder after all restore app handled by appstore
    public static void checkFolderAndUpdate() {
        Log.d(TAG, "checkFolderAndUpdate in");
        final ArrayList<FolderInfo> folderToRemove = new ArrayList<FolderInfo>();
        final ArrayList<ItemInfo> itemsToUpdate = new ArrayList<ItemInfo>();
        ArrayList<FolderInfo> allFolders = getAllFolderItems();
        for (FolderInfo info: allFolders) {
            if ((info != null) && (info.contents != null)) {
                Log.d(TAG, "folder " + info.title + " size is " + info.contents.size());
                if (info.contents.size() == 1){
                    ShortcutInfo itemInFolder = info.contents.get(0);
                    if (itemInFolder != null) {
                        //replace the folder with the item in it
                        itemInFolder.cellX = info.cellX;
                        itemInFolder.cellY = info.cellY;
                        itemInFolder.container = info.container;
                        itemInFolder.screen = info.screen;
                        updateItemInDatabase(LauncherApplication.getContext(), itemInFolder);
                        itemsToUpdate.add(itemInFolder);
                        //BugID:5631883:folder dismiss issue
                        info.contents.remove(itemInFolder);
                        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                        final Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (callbacks != null) {
                                    callbacks.closeFolderWithoutExpandAnimation();
                                }
                            }
                        };
                        mHandler.post(r);
                    }
                    folderToRemove.add(info);
                } else if (info.contents.size() == 0) {
                    folderToRemove.add(info);
                // [[ YunOS BEGIN PB
                // module:(HomeShell)  author:wb-lz260651
                // BugID:(9987269)  data:2017/02/14 10:22
                } else if (info.contents.size() == 2 && info.isEditFolderInContents()) {
                    final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            if (callbacks != null) {
                                callbacks.closeFolderWithoutExpandAnimation();
                            }
                        }
                    };
                    mHandler.post(r);
                }
                // YunOS END PB ]]
            }
        }

        final ArrayList<ItemInfo> FoldersToViewRemoved = new ArrayList<ItemInfo>();
        for (FolderInfo info: folderToRemove) {
            final ItemInfo iteminfo = (ItemInfo) info;
            FoldersToViewRemoved.add(iteminfo);
            Log.d(TAG, "remove folder id " + info.id);
            deleteItemFromDatabase(LauncherApplication.getContext(), iteminfo);
        }

        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        final Runnable rFolderRemove = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindItemsViewRemoved(FoldersToViewRemoved);
                }
            }
        };

        final Runnable rItemRemove = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindItemsViewRemoved(itemsToUpdate);
                }
            }
        };

        final Runnable rItemCreate = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.bindItemsViewAdded(itemsToUpdate);
                }
            }
        };

        final Runnable checkEmptyScreen = new Runnable() {
            @Override
            public void run() {
                if (callbacks != null) {
                    callbacks.checkAndRemoveEmptyCell();
                }
            }
        };
        if (folderToRemove.size() > 0) {
            runOnMainThread(rItemRemove);
            runOnMainThread(rFolderRemove);
            runOnMainThread(rItemCreate);
            runOnMainThread(checkEmptyScreen);
        }
        folderToRemove.clear();
    }

    //BugID:5191015:liuhao:the last item in folder overlay
    public static void checkFolderAndUpdateByUI() {
        Runnable r = new Runnable() {
            public void run() {
                checkFolderAndUpdate();
            }
        };
        runOnWorkerThread(r);
    }

    public static void restoreDownloadHandled() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                checkFolderAndUpdate();
            }
        };
        sWorker.post(r);
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/03/25 ##author:nater.wg ##BugID:104547
    //check duplicate app item.
    private static boolean isDuplicateItem(ItemInfo item) {
        if (item instanceof ShortcutInfo &&
                (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                        item.itemType == LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION)) {
            ShortcutInfo shortcutInfo = (ShortcutInfo) item;
            checkComponentBlock: {
                if (shortcutInfo.intent == null || shortcutInfo.intent.getComponent() == null) {
                    break checkComponentBlock;
                }
                ComponentName componentName = shortcutInfo.intent.getComponent();
                Log.d(TAG, "Checking duplicate item: componentName = " + componentName);
                ArrayList<ItemInfo> allApps = getAllAppItems();
                for (ItemInfo itemInfo : allApps) {
                    if (itemInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                                    && itemInfo.itemType != LauncherSettings.Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                        continue;
                    }
                    if (itemInfo instanceof ShortcutInfo) {
                        ShortcutInfo compareShortcutInfo = (ShortcutInfo) itemInfo;
                        if (compareShortcutInfo.intent != null
                                        && componentName.equals(compareShortcutInfo.intent.getComponent())) {
                            Log.w(TAG, "Ignore a duplicate item. componentName = " + componentName);
                            return true;
                        }
                    }
                }
            }
        }
        Log.d(TAG, "No duplicate item.");
        return false;
    }
    /*YUNOS END*/

    /* YUNOS BEGIN */
    // ##date:2014/7/23 ##author:yangshan.ys##140049
    // for 3*3 layout
    //for aged mode icon layout change
    public void changeLayoutForAgedModeChanged(boolean isAgedMode, final int countx,
            final int county) {
        //save new countx and county to sharedpreference
        sCellCountX = countx;
        sCellCountY = county;
        Launcher launcher = LauncherApplication.getLauncher();
        if (launcher != null) {
            Log.d(AgedModeUtil.TAG, "Change the UI because of the agedModeState change to:"
                    + isAgedMode + ",the des layout is:" + countx + "," + county);
            ConfigManager.setCellCountX(sCellCountX);
            ConfigManager.setCellCountY(sCellCountY);
            if (isAgedMode) {
                ConfigManager.adjustToThreeLayout();
                launcher.adjustToThreeLayout();
            } else {
                ConfigManager.adjustFromThreeLayout();
                launcher.adjustFromThreeLayout();
            }
            BubbleResources.setNeedsReload();
            /* YUNOS BEGIN */
            // ##date:2015/3/26 ##author:sunchen.sc##5735130
            // adjustToThreeLayout() will change the hideseat max screen. So reCreateUiOccupied() after reCreateUiOccupied
            // Recreate UI occupied when old mode launched, because of layout or workspace and hideseat will change.
            reCreateUiOccupied(sCellCountX, sCellCountY);
            /* YUNOS END */
            launcher.resetWorkspaceGridSize(sCellCountX, sCellCountY);
        } else {
            Log.d(AgedModeUtil.TAG,
                    "the launcher is null when changeLayoutForAgedModeChanged,so do not change the layout,the desAgedState:"
                            + isAgedMode + "des layout:" + countx + "," + county);
        }
    }
    /* YUNSO END */

    private void stopAllDownloadItems() {
        final Context tmpcontext = LauncherApplication.getContext();
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for (ItemInfo item: allApps) {
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                String pkgName = ((ShortcutInfo)item).intent.
                                     getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                Intent intent = new Intent(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
                intent.putExtra(AppDownloadManager.TYPE_ACTION,
                                    AppDownloadManager.ACTION_HS_DOWNLOAD_CANCEL);
                intent.putExtra(AppDownloadManager.TYPE_PACKAGENAME, pkgName);
                tmpcontext.sendBroadcast(intent);
            }
        }
    }

    public void clearDownloadItems() {
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                mClearAllDownload = true;
            }
        };
        runOnWorkerThread(r);
    }

  //for icon layout change
    public void changeLayout(final int countx, final int county, final boolean isThemeChanged, final LoaderTask task) {
        Log.d(TAG, "new countx is " + countx + " new county is " + county);
        Runnable runOnUiThread = new Runnable() {
            @Override
            public void run() {

                // create current layout data
                // screen + 1 for hotseat, + 6 for hideseat
                /* HIDESEAT_SCREEN_NUM_MARKER: see ConfigManager.java */
                final ItemInfo occupied[][][] = new ItemInfo[mMaxIconScreenCount + 1
                        + ConfigManager.getHideseatScreenMaxCount()][sCellCountX + 1][sCellCountY + 1];
                createCurrentLayoutData(occupied);
                /* YUNOS BEGIN */
                // ##date:2015/3/26 ##author:sunchen.sc##5735130
                // Recreate UI occupied when layout changed, eg: 4*4->4*5, theme
                // change, relayout icon
                reCreateUiOccupied(countx, county);
                /* YUNOS END */
                // calculate new position
                if (sCellCountX == countx) {
                    // calcNewLayoutDataSameX(occupied, countx, county);
                    calcNewLayoutData(occupied, countx, county);
                } else {
                    Log.d(TAG, "unsupport layout at present");
                    calcNewLayoutData(occupied, countx, county);
                }
                // save new countx and county to sharedpreference
                sCellCountX = countx;
                sCellCountY = county;
                updateLayoutData();
                // Stop routine no space check because calcNewLayoutData() has check no space items
                sWorker.removeCallbacks(checkNoSpaceListR);
                final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
                // Runnable r = new Runnable() {
                // @Override
                // public void run() {
                // BugID:5204915:mLauncher is null in LauncherApplication
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    oldCallbacks.collectCurrentViews();
                    oldCallbacks.resetWorkspaceGridSize(sCellCountX, sCellCountY);
                    oldCallbacks.reLayoutCurrentViews();
                    callbacks.checkAndRemoveEmptyCell();
                    Utils.dismissLoadingDialog();
                }
                // }
                // };
                // runOnMainThread(r);
                /* YUNOS END */
                if (isThemeChanged) {
                    Runnable run2 = new Runnable() {
                        public void run() {
                            reloadWorkspace();
                        }
                    };
                    runOnWorkerThread(run2);
                }
                if (task != null) {
                    Runnable run2 = new Runnable() {
                        public void run() {
                            task.bindWorkspace(-1);
                        }
                    };
                    runOnWorkerThread(run2);
                }
            }
        };
        runOnMainThread(runOnUiThread);
    }

    public void updateLayoutData() {
        Log.d(TAG, "updateLayoutData in");

        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
                ArrayList<ItemInfo> allApps = getAllAppItems();
                for (ItemInfo item: allApps) {
                    if ((item.container == Favorites.CONTAINER_DESKTOP) ||
                            (item.container > 0)){
                        ContentValues values = new ContentValues();
                        values.put("screen", item.screen);
                        values.put("cellY", item.cellY);
                        values.put("cellX", item.cellX);
                        values.put("spanX", item.spanX);
                        values.put("spanY", item.spanY);
                        values.put("container", item.container);
                        Uri uri = LauncherSettings.Favorites.getContentUri(item.id, false);
                        updateArgs args = new updateArgs(uri, values, null, null);
                        updatelist.add(args);
                    }
                }
                Log.d(TAG, "update db start");
                LauncherApplication app = (LauncherApplication) LauncherApplication.getContext();
                if ((app != null) && (app.getLauncherProvider() != null)) {
                    Log.d(TAG, "before call bulkupdate");
                    app.getLauncherProvider().bulkUpdate(updatelist);
                }
                updatelist.clear();
            }
        });
        ConfigManager.setCellCountX(sCellCountX);
        ConfigManager.setCellCountY(sCellCountY);
        Log.d(TAG, "updateLayoutData out");
    }

    /** Filters the set of items who are directly or indirectly (via another container) on the
     * specified screen. */
    public void filterCurrentWorkspaceItems(int currentScreen,
            ArrayList<ItemInfo> allWorkspaceItems,
            ArrayList<ItemInfo> currentScreenItems,
            ArrayList<ItemInfo> otherScreenItems) {
        // Purge any null ItemInfos
        Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
        while (iter.hasNext()) {
            ItemInfo i = iter.next();
            if (i == null) {
                iter.remove();
            }
        }

        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // items given.
        if (currentScreen < 0) {
            currentScreenItems.addAll(allWorkspaceItems);
        }

        // Order the set of items by their containers first, this allows use to walk through the
        // list sequentially, build up a list of containers that are in the specified screen,
        // as well as all items in those containers.
        Set<Long> itemsOnScreen = new HashSet<Long>();
        Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                return (int) (lhs.container - rhs.container);
            }
        });
        for (ItemInfo info : allWorkspaceItems) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (info.screen == currentScreen) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            } else if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                currentScreenItems.add(info);
                itemsOnScreen.add(info.id);
            } else if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                // ##date:2014/08/15##author:zhanggong.zg##BugID:5186578
                otherScreenItems.add(info);
            } else {
                if (itemsOnScreen.contains(info.container)) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(info.id);
                } else {
                    otherScreenItems.add(info);
                }
            }
        }
    }

    /** Filters the set of widgets which are on the specified screen. */
    private void filterCurrentAppWidgets(int currentScreen,
            ArrayList<LauncherAppWidgetInfo> appWidgets,
            ArrayList<LauncherAppWidgetInfo> currentScreenWidgets,
            ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {
        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // widgets given.
        if (currentScreen < 0) {
            currentScreenWidgets.addAll(appWidgets);
        }

        for (LauncherAppWidgetInfo widget : appWidgets) {
            if (widget == null) continue;
            if (widget.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    widget.screen == currentScreen) {
                currentScreenWidgets.add(widget);
            } else {
                otherScreenWidgets.add(widget);
            }
        }
    }

    /** Filters the set of folders which are on the specified screen. */
    private void filterCurrentFolders(int currentScreen,
            HashMap<Long, ItemInfo> itemsIdMap,
            HashMap<Long, FolderInfo> folders,
            HashMap<Long, FolderInfo> currentScreenFolders,
            HashMap<Long, FolderInfo> otherScreenFolders) {
        // If we aren't filtering on a screen, then the set of items to load is the full set of
        // widgets given.
        if (currentScreen < 0) {
            currentScreenFolders.putAll(folders);
        }

        for (long id : folders.keySet()) {
            ItemInfo info = itemsIdMap.get(id);
            FolderInfo folder = folders.get(id);
            if (info == null || folder == null) continue;
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                    info.screen == currentScreen) {
                currentScreenFolders.put(id, folder);
            } else {
                otherScreenFolders.put(id, folder);
            }
        }
    }

    /** Sorts the set of items by hotseat, workspace (spatially from top to bottom, left to
     * right) */
    private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
        // XXX: review this
        Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                int cellCountX = LauncherModel.getCellCountX();
                int cellCountY = LauncherModel.getCellCountY();
                int screenOffset = cellCountX * cellCountY;
                int containerOffset = screenOffset * (mMaxIconScreenCount + 1); // +1 hotseat
                long lr = (lhs.container * containerOffset + lhs.screen * screenOffset +
                        lhs.cellY * cellCountX + lhs.cellX);
                long rr = (rhs.container * containerOffset + rhs.screen * screenOffset +
                        rhs.cellY * cellCountX + rhs.cellX);
                return (int) (lr - rr);
            }
        });
    }

    private void bindWorkspaceItems(final Callbacks oldCallbacks,
            final ArrayList<ItemInfo> workspaceItems,
            final ArrayList<LauncherAppWidgetInfo> appWidgets,
            final HashMap<Long, FolderInfo> folders,
            ArrayList<Runnable> deferredBindRunnables) {

        final boolean postOnMainThread = (deferredBindRunnables != null);

        // Bind the workspace items
        int N = workspaceItems.size();
        for (int i = 0; i < N; i += ITEMS_CHUNK) {
            final int start = i;
            final int chunkSize = (i+ITEMS_CHUNK <= N) ? ITEMS_CHUNK : (N-i);
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindItems(workspaceItems, start, start+chunkSize);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        // Bind the folders
        if (!folders.isEmpty()) {
            final Runnable r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindFolders(folders);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }

        // Bind the widgets, one at a time
        N = appWidgets.size();
        for (int i = 0; i < N; i++) {
            final LauncherAppWidgetInfo widget = appWidgets.get(i);
            final Runnable r = new Runnable() {
                public void run() {
                    Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAppWidget(widget);
                    }
                }
            };
            if (postOnMainThread) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
        }
    }

    /**
     * Gets the callbacks object.  If we've been stopped, or if the launcher object
     * has somehow been garbage collected, return null instead.  Pass in the Callbacks
     * object that was around when the deferred message was scheduled, and if there's
     * a new Callbacks object around then also return null.  This will save us from
     * calling onto it with data that will be ignored.
     */
    Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
        synchronized (mLock) {
            if (mCallbacks == null) {
                return null;
            }
            final Callbacks callbacks = mCallbacks.get();
            if (callbacks != oldCallbacks) {
                return null;
            }
            if (callbacks == null) {
                Log.w(TAG, "no mCallbacks");
                return null;
            }
            return callbacks;
        }
    }

    public void calcNewLayoutData(ItemInfo[][][] occupied, int countX, int countY) {
        Log.d(TAG, "calcNewLayoutData in");
        ItemInfo[][][] newoccupied = new ItemInfo[mMaxIconScreenCount + 4][countX + 1][countY + 1];
        int padding = 0;
        int currentPosCount = 0;
        int newPosCount = 0;
        int currentMaxScreen = 0;
        //ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
        ArrayList<ItemInfo> handledItems = new ArrayList<ItemInfo>();
        int emptycount = 0;    //for remove empty line
        for (int lscreen = 0; lscreen < mMaxIconScreenCount; lscreen++) {
            for (int lcelly = 0; lcelly < sCellCountY; lcelly++) {
                for (int lcellx = 0; lcellx < sCellCountX; lcellx++) {
                    ItemInfo item = occupied[lscreen][lcellx][lcelly];
                    if ((item == null) || (item.container != Favorites.CONTAINER_DESKTOP)) {
                        //don't remove the empty in first screen
                        if ((lscreen > 0) && (item == null)){
                            emptycount++;
                            Log.d(TAG, "emptycount after inc is " + emptycount);
                            if ((emptycount >= countX) && (lcellx == countX - 1)){
                                emptycount -= countX;
                                padding -= countX;
                            }
                        }
                        //only support desktop now. no support hotseat and hideseat
                        continue;
                    }
                    emptycount = 0;
                    if (handledItems.contains(item) == true) {
                        Log.d(TAG, "the item is handled " + item.id);
                        continue;
                    }
                    handledItems.add(item);
                    Log.d(TAG, "calc item id:" + item.id + " new postion");
                    Log.d(TAG, "current position is screen:" + item.screen +
                                                    " cellY:" + item.cellY +
                                                    " cellX:" + item.cellX);

                    if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET) {
                        LauncherAppWidgetInfo appWidgetInfo = (LauncherAppWidgetInfo) item;
                        // check if it is larger than new layout (countX, countY)
                        int[] spanXY = Launcher.getSpanForWidgetCustomCountXY(mApp, appWidgetInfo, countX, countY);
                        item.spanX = Math.min(spanXY[0], countX);
                        item.spanY = Math.min(spanXY[1], countY);
                    }

                    currentPosCount = item.screen * sCellCountY * sCellCountX
                                           + item.cellY * sCellCountX
                                           + item.cellX + padding;
                    Log.d(TAG, "padding is " + padding);
                    int leftLine = countY - (currentPosCount % (countX * countY)) /countX;
                    int leftPosInLine = countX - (currentPosCount % (countX * countY)) % countX;

                    if (leftLine < item.spanY) {
                        //no enough space for this block
                        Log.d(TAG, "leftLine is " + leftLine);
                        padding = padding + leftLine * countX - (currentPosCount % (countX * countY)) % countX;
                        //start in a new screen
                        Log.d(TAG, "before calc new newPosCount is " + newPosCount);
                        newPosCount = currentPosCount + leftLine * countX - (currentPosCount % (countX * countY)) % countX;
                        Log.d(TAG, "new newPosCount is " + newPosCount);
                    } else if (leftPosInLine < item.spanX) {
                        //no enough space in current line, find new position in next line
                        newPosCount = currentPosCount + leftPosInLine;
                        padding = padding + leftPosInLine;
                    } else {
                        newPosCount = currentPosCount;
                    }
                    //find a new block to place the item
                    int newscreen = newPosCount / (countX * countY);
                    int newx = (newPosCount % (countX * countY)) % countX;
                    int newy = (newPosCount % (countX * countY)) / countX;
                    //check whether new block position occupied by other items
                    //and find a new block position if it is occupied
                    while(true) {
                        boolean occupy = false;
                        for (int x = 0; x < item.spanX; x++) {
                            for (int y = 0; y < item.spanY; y++) {
                                if (((newx + x) < countX) &&
                                    ((newy + y) < countY) &&
                                    (newscreen < mMaxIconScreenCount) &&
                                    (newoccupied[newscreen][newx + x][newy + y] == null)) {
                                    occupy = false;
                                } else {
                                    occupy = true;
                                    break;
                                }
                            }
                            if (occupy == true) {
                                if ((countX - newx) > item.spanX) {
                                    newx++;
                                } else {
                                    newx = countX;
                                }
                                newy = newy + newx /countX;
                                newx = newx % countX;

                                newscreen = newscreen + newy /countY;
                                newy = newy % countY;
                                break;
                            }
                        }

                        if (occupy == false) {
                            break;
                        }

                        if (newscreen >= mMaxIconScreenCount) {
                            Log.d(TAG, "out of the max screen count");
                            //for items out of max screen, leave them handled in handleNoSpaceItems
                            newscreen = -1;
                            newx = -1;
                            newy = -1;
                            break;
                        }
                    }
                    item.screen = newscreen;
                    item.cellY = newy;
                    item.cellX = newx;
                    if ((newscreen > -1) && (newscreen < mMaxIconScreenCount)) {
                        for (int x = 0; x < item.spanX; x++) {
                            for (int y = 0; y < item.spanY; y++) {
                                if (((x + newx) < countX) &&
                                    ((y + newy) < countY)) {
                                        newoccupied[newscreen][newx + x][newy + y] = item;
                                        if (item.screen > currentMaxScreen) {
                                            currentMaxScreen = item.screen;
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
        handledItems.clear();

        handleNoSpaceItems(newoccupied, currentMaxScreen + 1, countX, countY);

        Log.d(TAG, "calcNewLayoutData out");
    }

    public void handleNoSpaceItems(ItemInfo[][][] newoccupied, int screen, int countX, int countY) {
        ArrayList<ItemInfo> noSpaceItemsList = new ArrayList<ItemInfo>();
        ArrayList<ItemInfo> removeList = new ArrayList<ItemInfo>();
        ArrayList<ItemInfo> noSpaceFoldersList = new ArrayList<ItemInfo>();
        ArrayList<ItemInfo> allApps = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> iconAdded = new ArrayList<ItemInfo>(1);
        final ArrayList<ItemInfo> iconRemove = new ArrayList<ItemInfo>(1);
        final HashMap<Long,FolderInfo> folders = new HashMap<Long,FolderInfo>();
        synchronized (sBgLock) {
            allApps.addAll(sBgItemsIdMap.values());
        }
        for (ItemInfo item: allApps) {
            if (item.screen == -1) {
                //delete no space widget and gadget
                if ((item.itemType == Favorites.ITEM_TYPE_APPWIDGET) ||
                   (item.itemType == Favorites.ITEM_TYPE_GADGET)) {
                    removeList.add(item);
                } else {
                    if (item.itemType == Favorites.ITEM_TYPE_FOLDER) {
                        noSpaceFoldersList.add(item);
                    } else {
                        noSpaceItemsList.add(item);
                    }
                }
            }
        }
        Log.d(TAG, "no space item count is " + noSpaceItemsList.size());
        if (removeList.size() > 0) {
            //remove no space widget
            for (ItemInfo wItem: removeList) {
                deleteItemFromDatabase(mApp, wItem);
            }
            removeList.clear();
        }
        if ((noSpaceItemsList.size() == 0) &&
           (noSpaceFoldersList.size() == 0)){
            return;
        }
        //for no space folder, find an empty position
        //if no empty postion, delete the folder and add items in it to no space item list
        for (ItemInfo noSpaceFolderItem: noSpaceFoldersList) {
            ScreenPosition position = findEmptyCellInOccupied(newoccupied, screen, countX, countY);
            if (position != null) {
                noSpaceFolderItem.screen = position.s;
                noSpaceFolderItem.cellX = position.x;
                noSpaceFolderItem.cellY = position.y;
                newoccupied[noSpaceFolderItem.screen][noSpaceFolderItem.cellX][noSpaceFolderItem.cellY] = noSpaceFolderItem;
                // cache then add to worksapce
                folders.put(noSpaceFolderItem.id, (FolderInfo)noSpaceFolderItem);
            } else {
                for(ItemInfo itemInFolder: ((FolderInfo)noSpaceFolderItem).contents) {
                    itemInFolder.container = Favorites.CONTAINER_DESKTOP;
                    itemInFolder.screen = -1;
                    itemInFolder.cellX = -1;
                    itemInFolder.cellY = -1;
                    noSpaceItemsList.add(itemInFolder);
                }
                ((FolderInfo)noSpaceFolderItem).contents.clear();
                deleteItemFromDatabase(mApp, noSpaceFolderItem);
            }
        }
        noSpaceFoldersList.clear();

        //Put all noSpaceItems in a new folder.
        //Find an empty position for the new folder,
        //if no empty position, find a single item in workspace
        //put no space items and the single item in a new folder
        //in the single item's position.
        //If no empty position or single item found, I have to leave
        //these nospace item in -1 state.
        FolderInfo newfolder = null;
        int listcount = noSpaceItemsList.size();
        for (int i = 0; i < listcount; i++) {
            ItemInfo noSpaceItem = noSpaceItemsList.get(i);
            synchronized (sBgLock) {
                if (sBgWorkspaceItems.contains(noSpaceItem)) {
                    sBgWorkspaceItems.remove(noSpaceItem);
                }
            }
            if ((newfolder == null) || (newfolder.contents.size() >= ConfigManager.getFolderMaxItemsCount())) {
                ScreenPosition position = findEmptyCellInOccupied(newoccupied, screen, countX, countY);
                if (position != null) {
                    if ((listcount - i) == 1) {
                        //only one item left
                        noSpaceItem.screen = position.s;
                        noSpaceItem.cellX = position.x;
                        noSpaceItem.cellY = position.y;
                        newoccupied[noSpaceItem.screen][noSpaceItem.cellX][noSpaceItem.cellY] = noSpaceItem;
                        // cache then add to worksapce
                        iconAdded.add(noSpaceItem);
                    } else {
                        //create a new folder
                        newfolder = new FolderInfo();
                        newfolder.container = Favorites.CONTAINER_DESKTOP;
                        newfolder.screen = position.s;
                        newfolder.cellX = position.x;
                        newfolder.cellY = position.y;
                        newfolder.title = noSpaceItem.title + mApp.getResources().getString(R.string.folder_name_etc);
                        addItemToDatabase(mApp, newfolder, newfolder.container, newfolder.screen,
                                                     newfolder.cellX, newfolder.cellY, false);
                        Log.d(TAG, "the newfolder id is " + newfolder.id);
                        synchronized (sBgLock) {
                            sBgFolders.put(newfolder.id, newfolder);
                            sBgItemsIdMap.put(newfolder.id, newfolder);
                            if (sBgWorkspaceItems.contains(newfolder) == false) {
                                sBgWorkspaceItems.add(newfolder);
                            }
                        }

                        //put the no space item in newfolder
                        newfolder.contents.add((ShortcutInfo)noSpaceItem);
                        noSpaceItem.container = newfolder.id;
                        noSpaceItem.screen = 0;
                        //the no space item is the first item in newfolder
                        //so x and y is 0
                        noSpaceItem.cellX = 0;
                        noSpaceItem.cellY = 0;
                        //BugID:5212981:remove the item from sBgNoSpaceItems if it is added into a folder
                        synchronized (sBgLock) {
                            if (sBgNoSpaceItems.contains(noSpaceItem) == true) {
                                sBgNoSpaceItems.remove(noSpaceItem);
                            }
                        }
                        if (noSpaceItem.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                            noSpaceItem.itemType = Favorites.ITEM_TYPE_APPLICATION;
                        }
                        newoccupied[newfolder.screen][newfolder.cellX][newfolder.cellY] = newfolder;
                        // cache then add to worksapce
                        folders.put(newfolder.id, (FolderInfo)newfolder);
                    }
                } else {
                    //no empty for newfolder, find a item in workspace and create a new folder at the item's position
                    ItemInfo firstSingleItem = null;
                    for (int s = screen - 1; s > 0; s--) {
                        for (int y = countY -1; y >= 0; y--) {
                            for (int x = countX -1; x >= 0; x--) {
                                ItemInfo item = newoccupied[s][x][y];
                                if (item != null) {
                                    if ((item.itemType == Favorites.ITEM_TYPE_APPLICATION) ||
// remove vp install
//                                        (item.itemType == Favorites.ITEM_TYPE_VPINSTALL) ||
                                        (item.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) ||
                                        (item.itemType == Favorites.ITEM_TYPE_SHORTCUT)) {
                                        firstSingleItem = item;
                                        break;
                                    }
                                }
                            }
                            if (firstSingleItem != null) {
                                break;
                            }
                        }
                        if (firstSingleItem != null) {
                            break;
                        }
                    }
                    if (firstSingleItem != null) {
                        newfolder = new FolderInfo();
                        newfolder.container = Favorites.CONTAINER_DESKTOP;
                        newfolder.screen = firstSingleItem.screen;
                        newfolder.cellX = firstSingleItem.cellX;
                        newfolder.cellY = firstSingleItem.cellY;
                        newfolder.title = firstSingleItem.title + mApp.getResources().getString(R.string.folder_name_etc);
                        addItemToDatabase(mApp, newfolder, newfolder.container, newfolder.screen,
                                                     newfolder.cellX, newfolder.cellY, false);
                        Log.d(TAG, "the newfolder id is " + newfolder.id);
                        synchronized (sBgLock) {
                            sBgFolders.put(newfolder.id, newfolder);
                            sBgItemsIdMap.put(newfolder.id, newfolder);
                            if (sBgWorkspaceItems.contains(newfolder) == false) {
                                sBgWorkspaceItems.add(newfolder);
                            }
                        }
                        // Remove the icon that will change to folder
                        ItemInfo iconChangeTofolder = new ItemInfo();
                        iconChangeTofolder.container = firstSingleItem.container;
                        iconChangeTofolder.screen = firstSingleItem.screen;
                        iconChangeTofolder.cellX = firstSingleItem.cellX;
                        iconChangeTofolder.cellY = firstSingleItem.cellY;
                        iconRemove.add(iconChangeTofolder);

                        //put the firstSingleItem in newfolder;
                        newfolder.contents.add((ShortcutInfo)firstSingleItem);
                        firstSingleItem.container = newfolder.id;
                        firstSingleItem.screen = 0;
                        firstSingleItem.cellX = 0;
                        firstSingleItem.cellY = 0;
                        synchronized (sBgLock) {
                            if (sBgWorkspaceItems.contains(firstSingleItem)) {
                                sBgWorkspaceItems.remove(firstSingleItem);
                            }
                        }
                        //put the no space item in newfolder
                        newfolder.contents.add((ShortcutInfo)noSpaceItem);
                        noSpaceItem.screen = 0;
                        //the no space item is the first item in newfolder
                        //so x and y is 0
                        noSpaceItem.cellX = 1;
                        noSpaceItem.cellY = 0;
                        // Don't forget assign new id
                        noSpaceItem.container = newfolder.id;
                        newoccupied[newfolder.screen][newfolder.cellX][newfolder.cellY] = newfolder;
                        // cache then add to worksapce
                        folders.put(newfolder.id, (FolderInfo)newfolder);
                    }
                }
            } else {
                //put the no space item in newfolder
                newfolder.contents.add((ShortcutInfo)noSpaceItem);
                noSpaceItem.container = newfolder.id;
                noSpaceItem.screen = 0;
                noSpaceItem.cellX = newfolder.contents.size() % ConfigManager.getFolderMaxCountY();
                noSpaceItem.cellY = newfolder.contents.size() / ConfigManager.getFolderMaxCountY();
                //BugID:5212981:remove the item from sBgNoSpaceItems if it is added into a folder
                synchronized (sBgLock) {
                    if (sBgNoSpaceItems.contains(noSpaceItem)) {
                        sBgNoSpaceItems.remove(noSpaceItem);
                    }
                }
                if (noSpaceItem.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                    noSpaceItem.itemType = Favorites.ITEM_TYPE_APPLICATION;
                }
                if (newfolder.contents.size() == 2) {
                    Log.d(TAG, "call get folder name");
                    String newFolderName = mAppGroupMgr.getFolderNameByPkgNames(
                            newfolder.contents.get(0).getPackageName(),
                            newfolder.contents.get(1).getPackageName());
                    if (newFolderName != null && !newFolderName.isEmpty()) {
                        newfolder.title = newFolderName;
                        updateItemInDatabase(mApp, newfolder);
                    }
                }
            }
        }
        noSpaceItemsList.clear();
        // Need refresh UI about no space items because there is no all item bind when change layout, only
        // reLayoutCurrentViews()
        Runnable runOnUI = new Runnable() {

            @Override
            public void run() {
                final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                if (callbacks != null) {
                    // remove icon from workspace that change to folder
                    if (iconRemove.size() > 0) {
                        callbacks.bindItemsViewRemoved(iconRemove);
                    }
                    // bind no space items when 4*5->4*4
                    if (iconAdded.size() > 0) {
                        callbacks.bindItemsAdded(iconAdded);
                    }
                    // bind folder contains no space items when 4*5->4*4
                    if (folders.size() > 0) {
                        Iterator it = folders.entrySet().iterator();
                        ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>();
                        while (it.hasNext()) {
                            Map.Entry entry = (Map.Entry)it.next();
                            ItemInfo info = (FolderInfo)entry.getValue();
                            infos.add(info);
                        }
                        callbacks.bindItemsAdded(infos);
                        callbacks.bindFolders(folders);
                    }
                }
            }
        };
        runOnMainThread(runOnUI);
    }

    public ScreenPosition findEmptyCellInOccupied(ItemInfo[][][] newoccupied, int screen, int countX, int countY) {
        Log.d(TAG, "findEmptyCellInOccupied() start");
        checkRunOnUIThread();
        ScreenPosition position = null;
        for (int s = screen - 1; s > ConfigManager.DEFAULT_HOME_SCREEN_INDEX; s--) {
            for (int y = countY -1; y >= 0; y--) {
                for (int x = countX -1; x >= 0; x--) {
                    if (newoccupied[s][x][y] == null && !isDragInfoOccupied(s, x, y)) {
                        position = new ScreenPosition(s, x, y);
                        break;
                    }
                }
                if (position != null) {
                    break;
                }
            }
            if (position != null) {
                break;
            }
        }

        dumpUIOccupied();
        return position;
    }

    public static void createCurrentLayoutData(ItemInfo[][][] occupied) {
        ArrayList<ItemInfo> allItems = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            allItems.addAll(sBgItemsIdMap.values());
        }
        for(ItemInfo item: allItems) {
            int containerIndex = item.screen;
            //BugID:135271:ArrayIndexOutOfBoundsException if item's position error
            if ((containerIndex < 0) || (containerIndex >= mMaxIconScreenCount + 4) ||
                (item.cellX >= sCellCountX + 1) || (item.cellY >= sCellCountY + 1) ||
                (item.cellX < 0) || (item.cellY < 0)) {
                Log.d(TAG, "item position error, mCellCountX " + sCellCountX);
                Log.d(TAG, "mCellCountY " + sCellCountY);
                Log.d(TAG, "item.cellX is " + item.cellX);
                Log.d(TAG, "item.cellY is " + item.cellY);
                continue;
            }
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                //BugID:135271:ArrayIndexOutOfBoundsException if item's position error
                if (item.screen >= sCellCountX + 1) {
                    Log.d(TAG, "hotseat position error, item.screen is " + item.screen);
                    Log.d(TAG, "mCellCountX " + sCellCountX);
                    continue;
                }
                if (occupied[mMaxIconScreenCount][item.screen][0] != null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY
                        + ") occupied by " + occupied[mMaxIconScreenCount][item.screen][0]);
                } else {
                    occupied[mMaxIconScreenCount][item.screen][0] = item;
                }
            } else if (item.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                int screenIndex = mMaxIconScreenCount + 1 + item.screen;
                if (occupied[screenIndex][item.cellX][item.cellY] != null) {
                    Log.e(TAG, "Error loading shortcut into hotseat " + item
                        + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY
                        + ") occupied by " + occupied[screenIndex][item.cellX][item.cellY]);
                } else {
                    occupied[screenIndex][item.cellX][item.cellY] = item;
                }
            } else if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                // Check if any workspace icons overlap with each other
                boolean isOccupied = false;
                for (int x = item.cellX; x < (item.cellX+item.spanX); x++) {
                    for (int y = item.cellY; y < (item.cellY+item.spanY); y++) {
                        if (occupied[containerIndex][x][y] != null) {
                            Log.e(TAG, "Error loading shortcut " + item
                                + " into cell (" + containerIndex + "-" + item.screen + ":"
                                + x + "," + y
                                + ") occupied by "
                                + occupied[containerIndex][x][y]);
                            isOccupied = true;
                        }
                    }
                }
                if (isOccupied == false) {
                    for (int x = item.cellX; x < (item.cellX+item.spanX); x++) {
                        for (int y = item.cellY; y < (item.cellY+item.spanY); y++) {
                           occupied[containerIndex][x][y] = item;
                        }
                    }
                }
            }
        }

        //TODO: find new postion for lostItem
    }

    /* YUNOS BEGIN */
    //## modules(Home Shell): [Category]
    //## date: 2015/08/31 ## author: wangye.wy
    //## BugID: 6221911: category on desk top
    private static boolean mExitEditMode = false;
    private static boolean mIsInProcess = false;
    private AlertDialog mAlertDialog = null;
    private final List<String> mTools = new ArrayList<String>();
    private final List<PackageUpdatedTask> mPackageUpdatedTasks = new ArrayList<PackageUpdatedTask>();
    private final Map<Long, ScreenPosition> mPositions = new HashMap<Long, ScreenPosition>();
    private final List<ItemInfo> mRemovedItems = new ArrayList<ItemInfo>();
    private final List<FolderInfo> mAddedFolders = new ArrayList<FolderInfo>();

    public void loadToolsFromXml() {
        Log.d(TAG, "getToolsFromXml()");
        sWorker.post(new Runnable() {
            @Override
            public void run() {
                mTools.clear();
                loadTools();
            }
        });
    }

    public void reCategoryAllIcons() {
        Log.d(TAG, "reCategoryAllIcons()");
        final Launcher launcher = LauncherApplication.getLauncher();
        if (launcher == null) {
            return;
        }
        if (launcher.isInLauncherCategoryMode()) {
            return;
        }
        if (mIsInProcess) {
            return;
        }
        mIsInProcess = true;
        sWorker.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (launcher.isPaused()) {
                    mIsInProcess = false;
                    return;
                }
                /* YUNOS BEGIN */
                //20160722 by chusheng.xcs
                if(launcher.isIsActivateGuide()){
                    reCategoryAndSort();
                    return;
                }
                /* YUNOS END */
                if (mAlertDialog == null) {
                    View view = LayoutInflater.from(launcher)
                            .inflate(R.layout.alert_dialog_progress_view, null, false);
                    TextView text = (TextView)view
                            .findViewById(R.id.dialog_progress_message_text);
                    text.setText(launcher.getString(R.string.str_load));
                    mAlertDialog = new AlertDialog.Builder(launcher)
                            .setView(view)
                            .setCancelable(false)
                            .create();
                }
                mAlertDialog.show();
                reCategoryAndSort();
            }
        }, 200);
    }

    public void coverAllIcons() {
        Log.d(TAG, "coverAllIcons()");
        Launcher launcher = LauncherApplication.getLauncher();
        if (launcher == null) {
            return;
        }
        if (!launcher.isInLauncherCategoryMode()) {
            return;
        }
        if (mIsInProcess) {
            return;
        }
        mIsInProcess = true;
        sWorker.postDelayed(new Runnable() {
            @Override
            public void run() {
                coverAll();
            }
        }, 200);
    }

    public void recoverAllIcons(boolean exitEditMode) {
        Log.d(TAG, "recoverAllIcons()");
        Launcher launcher = LauncherApplication.getLauncher();
        if (launcher == null) {
            return;
        }
        if (!launcher.isInLauncherCategoryMode()) {
            return;
        }
        if (mIsInProcess) {
            return;
        }
        mExitEditMode = exitEditMode;
        mIsInProcess = true;
        sWorker.postDelayed(new Runnable() {
            @Override
            public void run() {
                recoverAll();
            }
        }, 200);
    }

    private void reCategoryAndSort() {
        Log.d(TAG, "reCategoryAndSort()");
        Runnable runOnUiThread = new Runnable() {
            @Override
            public void run() {
                final List<ItemInfo> allItems = new ArrayList<ItemInfo>();
                categoryAndSort(allItems);
                final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.setLauncherCategoryMode(true);
                    callbacks.reLayoutCurrentViews(allItems);
                    if (FeatureUtility.hasFullScreenWidget()) {
                        callbacks.removeWidgetPages();
                    }
                    callbacks.enterLauncherCategoryMode();
                }
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }
                allItems.clear();
                mIsInProcess = false;
            }
        };
        runOnMainThread(runOnUiThread);
    }

    public void coverAll() {
        Log.d(TAG, "coverAll()");
        Runnable runOnUiThread = new Runnable() {
            @Override
            public void run() {
                cover();
                final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    if (FeatureUtility.hasFullScreenWidget()) {
                        callbacks.makesureWidgetPages();
                    }
                    callbacks.exitLauncherCategoryMode(true);
                    callbacks.setLauncherCategoryMode(false);
                }
                mPositions.clear();
                mRemovedItems.clear();
                mAddedFolders.clear();
                mIsInProcess = false;
                Launcher launcher = LauncherApplication.getLauncher();
                if (launcher != null) {
                    launcher.exitLauncherEditMode(true);
                }
            }
        };
        runOnMainThread(runOnUiThread);
    }

    public void recoverAll() {
        Log.d(TAG, "recoverAll()");
        Runnable runOnUiThread = new Runnable() {
            @Override
            public void run() {
                final List<ItemInfo> allItems = new ArrayList<ItemInfo>();
                recover(allItems);
                final Callbacks oldCallbacks = mCallbacks != null ? mCallbacks.get() : null;
                Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                if (callbacks != null) {
                    callbacks.reLayoutCurrentViews(allItems);
                    if (FeatureUtility.hasFullScreenWidget()) {
                        callbacks.makesureWidgetPages();
                    }
                    callbacks.exitLauncherCategoryMode(false);
                    callbacks.setLauncherCategoryMode(false);
                }
                if (mPackageUpdatedTasks.size() > 0) {
                    for (PackageUpdatedTask task : mPackageUpdatedTasks) {
                        mPackageUpdateTaskQueue.enqueue(task);
                    }
                    mPackageUpdatedTasks.clear();
                    Toast.makeText(mApp, mApp.getString(R.string.application_installed),
                            Toast.LENGTH_SHORT).show();
                }
                allItems.clear();
                mPositions.clear();
                mRemovedItems.clear();
                mAddedFolders.clear();
                mIsInProcess = false;
                Launcher launcher = LauncherApplication.getLauncher();
                if (mExitEditMode && launcher != null) {
                    launcher.exitLauncherEditMode(false);
                }
            }
        };
        runOnMainThread(runOnUiThread);
    }

    private void cover() {
        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
        for (ItemInfo item : mRemovedItems) {
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                ((FolderInfo)item).contents.clear();
                if (callbacks != null) {
                    callbacks.removeFolder((FolderInfo)item);
                }
            }
            deleteItemFromDatabase(mApp, item);
        }
        synchronized (sBgLock) {
            for (FolderInfo folder : mAddedFolders) {
                addItemToDatabase(mApp, folder, folder.container, folder.screen,
                        folder.cellX, folder.cellY, false);
                Log.d(TAG, "cover(), new folder ID: " + folder.id);
                sBgFolders.put(folder.id, folder);
                sBgItemsIdMap.put(folder.id, folder);
                if (!sBgWorkspaceItems.contains(folder)) {
                    sBgWorkspaceItems.add(folder);
                }
                for (ShortcutInfo content : folder.contents) {
                    Log.d(TAG, "cover(), content ID: " + content.id
                            + ", title: " + content.title);
                    content.container = folder.id;
                }
            }
        }
        if (callbacks != null) {
            if (mAddedFolders.size() > 0) {
                HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
                for (FolderInfo folder : mAddedFolders) {
                    folders.put(folder.id, folder);
                }
                callbacks.bindFolders(folders);
            }
        }

        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<updateArgs> updatelist = new ArrayList<updateArgs>();
                List<ItemInfo> items = new ArrayList<ItemInfo>();
                synchronized (sBgLock) {
                    items.addAll(sBgItemsIdMap.values());
                }
                for (ItemInfo item : items) {
                    if (item.container == Favorites.CONTAINER_DESKTOP ||
                            item.container > 0) {
                        Log.d(TAG, "cover(), ID: " + item.id + ", title: " + item.title
                                + ", container: " + item.container);
                        ContentValues values = new ContentValues();
                        values.put("container", item.container);
                        values.put("screen", item.screen);
                        values.put("cellY", item.cellY);
                        values.put("cellX", item.cellX);
                        values.put("spanX", item.spanX);
                        values.put("spanY", item.spanY);
                        Uri uri = LauncherSettings.Favorites.getContentUri(item.id, false);
                        updateArgs args = new updateArgs(uri, values, null, null);
                        updatelist.add(args);
                    }
                }
                Log.d(TAG, "update db start");
                LauncherProvider provider = mApp.getLauncherProvider();
                if (provider != null) {
                    Log.d(TAG, "before call bulkupdate");
                    provider.bulkUpdate(updatelist);
                }
                updatelist.clear();
            }
        });
    }

    private void recover(final List<ItemInfo> allItems) {
        synchronized (sBgLock) {
            allItems.addAll(sBgItemsIdMap.values());
        }
        for (ItemInfo item : allItems) {
            Log.d(TAG, "recover(), title: " + item.title + ", " + item.container);
            if (item.screen < 0 || item.screen > mMaxIconScreenCount ||
                item.cellX > sCellCountX || item.cellY > sCellCountY ||
                item.cellX < 0 || item.cellY < 0) {
                Log.d(TAG, "item position error, mCellCountX: " + sCellCountX + ", mCellCountY: " + sCellCountY
                        + ", cellX: " + item.cellX + ", cellY: " + item.cellY);
                continue;
            }
            ScreenPosition position = mPositions.get(item.id);
            if (position != null) {
                item.container = position.c;
                item.screen = position.s;
                item.cellX = position.x;
                item.cellY = position.y;
            }
            if (item.container >= 0) {
                FolderInfo folder = (FolderInfo) sBgItemsIdMap.get(item.container);
                if (folder != null) {
                    if (!folder.contents.contains(item)) {
                        folder.add((ShortcutInfo)item);
                    }
                }
            }
        }
    }

    private void categoryAndSort(final List<ItemInfo> allItems) {
        List<ItemInfo> items = new ArrayList<ItemInfo>();

        List<ShortcutInfo> tools = new ArrayList<ShortcutInfo>();
        List<ItemInfo> systemItems = new ArrayList<ItemInfo>();
        List<ItemInfo> deletableItems = new ArrayList<ItemInfo>();

        Map<String, List<ShortcutInfo>> categoryFolders = new HashMap<String, List<ShortcutInfo>>();
        List<ShortcutInfo> othersFolder = new ArrayList<ShortcutInfo>();
        List<ItemInfo> tempItems = new ArrayList<ItemInfo>();
        ArrayList<ShortcutInfo> needRmFromFolderList = new ArrayList<ShortcutInfo>();

        synchronized (sBgLock) {
            items.addAll(sBgItemsIdMap.values());
        }

        int position = sCellCountX * sCellCountY * mMaxIconScreenCount;
        String toolsTitle = mApp.getString(R.string.tools);

        final String TOOLS_EN = mApp.getString(R.string.tools_en);
        final String TOOLS_CN = mApp.getString(R.string.tools_cn);
        final String TOOLS_TW = mApp.getString(R.string.tools_tw);

        for (ItemInfo item : items) {
            if (item.screen < 0 || item.screen > mMaxIconScreenCount ||
                item.cellX > sCellCountX || item.cellY > sCellCountY ||
                item.cellX < 0 || item.cellY < 0) {
                Log.d(TAG, "item position error, mCellCountX: " + sCellCountX + ", mCellCountY: " + sCellCountY
                        + ", cellX: " + item.cellX + ", cellY: " + item.cellY);
                continue;
            }
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                int itemPosition = sCellCountX * sCellCountY * item.screen
                        + sCellCountX * item.cellY + item.cellX;
                mPositions.put(item.id, new ScreenPosition(
                        item.container, item.screen, item.cellX, item.cellY));
                switch (item.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                        if (itemPosition < position) {
                            position = itemPosition;
                        }
                        ShortcutInfo shortcut = (ShortcutInfo)item;
                        String packageName = shortcut.getPackageName();
                        String title = shortcut.title.toString();
                        Log.d(TAG, "categoryAndSort(), shortcut on desk top, system: " + shortcut.isSystemApp
                                + ", name: " + packageName + ", title: " + title);
                        if (mTools.contains(packageName)) {
                            if (!tools.contains(shortcut)) {
                                tools.add(shortcut);
                            }
                        } else if (shortcut.isSystemApp) {
                            if (!systemItems.contains(shortcut)) {
                                systemItems.add(shortcut);
                            }
                        } else {
                            addCategory(shortcut, othersFolder, categoryFolders);
                        }
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                        if (itemPosition < position) {
                            position = itemPosition;
                        }
                        FolderInfo folder = (FolderInfo)item;
                        if (!mRemovedItems.contains(folder)) {
                            mRemovedItems.add(folder);
                        }
                        title = folder.title.toString();
                        if (title.equals(TOOLS_EN) || title.equals(TOOLS_CN) || title.equals(TOOLS_TW)) {
                            toolsTitle = title;
                        }
                        int systemApp = 0;
                        shortcut = null;
                        for (ShortcutInfo content : folder.contents) {
                            packageName = content.getPackageName();
                            Log.d(TAG, "categoryAndSort(), shortcut in folder, system: " + content.isSystemApp
                                    + ", name: " + packageName + ", title: " + content.title);
                            mPositions.put(content.id, new ScreenPosition(
                                    content.container, content.screen, content.cellX, content.cellY));
                            if (mTools.contains(packageName)) {
                                if (!tools.contains(content)) {
                                    needRmFromFolderList.add(content);
                                    tools.add(content);
                                }
                            } else if (content.isSystemApp) {
                                if (title.equals(toolsTitle)) {
                                    if (!tools.contains(content)) {
                                        tools.add(content);
                                        needRmFromFolderList.add(content);
                                    }
                                } else {
                                    systemApp++;
                                    shortcut = content;
                                }
                            } else {
                                addCategory(content, othersFolder, categoryFolders);
                                needRmFromFolderList.add(content);
                            }
                        }
                        for (ShortcutInfo info:needRmFromFolderList) {
                            folder.remove(info);
                            info.container = -1;
                        }
                        needRmFromFolderList.clear();
                        if (systemApp > 0) {
                            if (systemApp > 1) {
                                if (!systemItems.contains(folder)) {
                                    systemItems.add(folder);
                                }
                            } else {
                                if (!systemItems.contains(shortcut)) {
                                    systemItems.add(shortcut);
                                }
                            }
                        }
                        break;
                    default:
                        if (!deletableItems.contains(item)) {
                            deletableItems.add(item);
                        }
                        break;
                }
            }
        }

        int toolsSize = tools.size();
        if (toolsSize == 1) {
            ShortcutInfo shortcut = tools.get(0);
            if (shortcut.isSystemApp) {
                if (!systemItems.contains(shortcut)) {
                    systemItems.add(shortcut);
                }
            } else {
                addCategory(shortcut, othersFolder, categoryFolders);
            }
        }

        items.clear();

        int screen = 0;
        int cellY = 0;
        int cellX = 0;

        int deletableIndex = 0;
        int deletableSize = deletableItems.size();
        if (deletableSize > 0) {
            Collections.sort(deletableItems, new Comparator() {
                public int compare(Object object1, Object object2) {
                    ItemInfo item1 = (ItemInfo)object1;
                    ItemInfo item2 = (ItemInfo)object2;
                    int position1 = sCellCountX * sCellCountY * item1.screen
                            + sCellCountX * item1.cellY + item1.cellX;
                    int position2 = sCellCountX * sCellCountY * item2.screen
                            + sCellCountX * item2.cellY + item2.cellX;
                    return (position1 - position2);
                }
            });
            for (; deletableIndex < deletableSize; deletableIndex++) {
                ItemInfo item = deletableItems.get(deletableIndex);
                int itemPosition = sCellCountX * sCellCountY * item.screen
                        + sCellCountX * item.cellY + item.cellX;
                if (itemPosition < position) {
                    while ((cellX + item.spanX > sCellCountX) ||
                            (cellX > 0 && item.spanY > 1) ||
                            (cellX == 0 && (cellY + item.spanY > sCellCountY))) {
                        int y = cellY + 1;
                        cellX = 0;
                        cellY = y % sCellCountY;
                        screen += (y / sCellCountY);
                    }
                    item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    item.screen = screen;
                    item.cellX = cellX;
                    item.cellY = cellY;
                    if (!allItems.contains(item)) {
                        allItems.add(item);
                    }
                    int x = cellX + item.spanX;
                    int y = cellY;
                    if (item.spanY > 1) {
                        cellX = 0;
                        y += item.spanY;
                    } else {
                        cellX = x % sCellCountX;
                        y += (x / sCellCountX);
                    }
                    cellY = y % sCellCountY;
                    screen += (y / sCellCountY);
                } else {
                    break;
                }
            }
        }

        int currentPosition = sCellCountX * sCellCountY * screen + sCellCountX * cellY + cellX;
        if (currentPosition < sCellCountX * 2) {
            screen = 0;
            cellY = 2;
            cellX = 0;
        }

        int folderMaxCountY = ConfigManager.getFolderMaxCountY();
        int folderMaxCountX = ConfigManager.getFolderMaxCountX();

        Collections.sort(systemItems, new Comparator() {
            public int compare(Object object1, Object object2) {
                ItemInfo item1 = (ItemInfo)object1;
                ItemInfo item2 = (ItemInfo)object2;
                if (item1.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER &&
                        item2.itemType != LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                    return 1;
                } else if (item1.itemType != LauncherSettings.Favorites.ITEM_TYPE_FOLDER &&
                        item2.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
                    return -1;
                } else {
                    return LauncherModel.this.compare(
                            (ItemInfo)object1, (ItemInfo)object2);
                }
            }
        });

        int systemSize = systemItems.size();
        int systemIndex = 0;
        for (; systemIndex < systemSize; systemIndex++) {
            ItemInfo system = systemItems.get(systemIndex);
            if (system.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                    system.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT ||
                    system.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
                ShortcutInfo shortcut = (ShortcutInfo)system;
                shortcut.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                shortcut.screen = screen;
                shortcut.cellX = cellX;
                shortcut.cellY = cellY;
                if (!allItems.contains(shortcut)) {
                    allItems.add(shortcut);
                }
                int x = cellX + 1;
                int y = cellY + (x / sCellCountX);
                cellX = x % sCellCountX;
                cellY = y % sCellCountY;
                screen += (y / sCellCountY);
            } else {
                break;
            }
        }

        Log.d(TAG, "categoryAndSort(), toolsSize: " + toolsSize);
        if (toolsSize > 1) {
            FolderInfo toolsFolder = new FolderInfo();
            toolsFolder.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
            toolsFolder.screen = screen;
            toolsFolder.cellX = cellX;
            toolsFolder.cellY = cellY;
            toolsFolder.spanX = 1;
            toolsFolder.spanY = 1;
            toolsFolder.title = toolsTitle;
            Collections.sort(tools, new Comparator() {
                public int compare(Object object1, Object object2) {
                    return LauncherModel.this.compare(
                            (ItemInfo)object1, (ItemInfo)object2);
                }
            });
            int folderScreen = 0;
            int folderCellX = 0;
            int folderCellY = 0;
            for (ShortcutInfo shortcut : tools) {
                shortcut.screen = folderScreen;
                shortcut.cellX = folderCellX;
                shortcut.cellY = folderCellY;
                toolsFolder.contents.add(shortcut);
                int x = folderCellX + 1;
                int y = folderCellY + (x / folderMaxCountX);
                folderCellX = x % folderMaxCountX;
                folderCellY = y % folderMaxCountY;
                folderScreen += (y / folderMaxCountY);
            }
            if (!mAddedFolders.contains(toolsFolder)) {
                mAddedFolders.add(toolsFolder);
            }
            if (!allItems.contains(toolsFolder)) {
                allItems.add(toolsFolder);
            }
            int x = cellX + 1;
            int y = cellY + (x / sCellCountX);
            cellX = x % sCellCountX;
            cellY = y % sCellCountY;
            screen += (y / sCellCountY);
        }

        for (; systemIndex < systemSize; systemIndex++) {
            FolderInfo system = (FolderInfo)systemItems.get(systemIndex);
            FolderInfo folder = new FolderInfo();
            folder.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
            folder.screen = screen;
            folder.cellX = cellX;
            folder.cellY = cellY;
            folder.spanX = 1;
            folder.spanY = 1;
            folder.title = system.title;
            for (ShortcutInfo content : system.contents) {
                if (content.isSystemApp) {
                    folder.contents.add(content);
                }
            }
            Collections.sort(folder.contents, new Comparator() {
                public int compare(Object object1, Object object2) {
                    return LauncherModel.this.compare(
                            (ItemInfo)object1, (ItemInfo)object2);
                }
            });
            int folderScreen = 0;
            int folderCellX = 0;
            int folderCellY = 0;
            for (ShortcutInfo content : folder.contents) {
                content.screen = folderScreen;
                content.cellX = folderCellX;
                content.cellY = folderCellY;
                int x = folderCellX + 1;
                int y = folderCellY + (x / folderMaxCountX);
                folderCellX = x % folderMaxCountX;
                folderCellY = y % folderMaxCountY;
                folderScreen += (y / folderMaxCountY);
            }
            if (!mAddedFolders.contains(folder)) {
                mAddedFolders.add(folder);
            }
            if (!allItems.contains(folder)) {
                allItems.add(folder);
            }
            int x = cellX + 1;
            int y = cellY + (x / sCellCountX);
            cellX = x % sCellCountX;
            cellY = y % sCellCountY;
            screen += (y / sCellCountY);
        }

        tools.clear();
        systemItems.clear();

        Log.d(TAG, "categoryAndSort(), category size > 1: " + screen + ", " + cellY + ", " + cellX);

        List<Entry<String, List<ShortcutInfo>>> category =
                new ArrayList<Entry<String, List<ShortcutInfo>>>(categoryFolders.entrySet());
        Collections.sort(category, new Comparator<Entry<String, List<ShortcutInfo>>>() {
            public int compare(Entry<String, List<ShortcutInfo>> object1,
                    Entry<String, List<ShortcutInfo>> object2) {
                List<ShortcutInfo> category1 = (List<ShortcutInfo>)object1.getValue();
                List<ShortcutInfo> category2 = (List<ShortcutInfo>)object2.getValue();
                return (category2.size() - category1.size());
            }
        });

        int categoryIndex = 0;
        int categorySize = category.size();
        int othersSize = othersFolder.size();

        for (; categoryIndex < categorySize; categoryIndex++) {
            Entry<String, List<ShortcutInfo>> categoryEntry = category.get(categoryIndex);
            List<ShortcutInfo> shortcutsFolder = (List<ShortcutInfo>)categoryEntry.getValue();
            if (shortcutsFolder.size() > 1) {
                if ((screen + 1 == mMaxIconScreenCount) &&
                        (cellY + 1 == sCellCountY) &&
                        (cellX + 1 == sCellCountX) &&
                        (categoryIndex < categorySize - 1 ||
                        othersSize > 0)) {
                    othersFolder.addAll(shortcutsFolder);
                    othersSize = othersFolder.size();
                } else {
                    String categoryId = (String)categoryEntry.getKey();
                    CategoryInfo info = mAppGroupMgr.getCatInfoByCatID(categoryId);
                    FolderInfo folder = new FolderInfo();
                    folder.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    folder.screen = screen;
                    folder.cellX = cellX;
                    folder.cellY = cellY;
                    folder.spanX = 1;
                    folder.spanY = 1;
                    /* YUNOS BEGIN */
                    //author:yi.ruany
                    //BugID:(8930072) ##date:2016.9.22
                    folder.title = info != null ? info.catName : mApp.getString(R.string.others);
                    /* YUNOS END */
                    Collections.sort(shortcutsFolder, new Comparator() {
                        public int compare(Object object1, Object object2) {
                            return LauncherModel.this.compare(
                                    (ItemInfo)object1, (ItemInfo)object2);
                        }
                    });
                    int folderScreen = 0;
                    int folderCellX = 0;
                    int folderCellY = 0;
                    for (ShortcutInfo shortcut : shortcutsFolder) {
                        shortcut.screen = folderScreen;
                        shortcut.cellX = folderCellX;
                        shortcut.cellY = folderCellY;
                        folder.contents.add(shortcut);
                        int x = folderCellX + 1;
                        int y = folderCellY + (x / folderMaxCountX);
                        folderCellX = x % folderMaxCountX;
                        folderCellY = y % folderMaxCountY;
                        folderScreen += (y / folderMaxCountY);
                    }
                    if (!mAddedFolders.contains(folder)) {
                        mAddedFolders.add(folder);
                    }
                    if (!allItems.contains(folder)) {
                        allItems.add(folder);
                    }
                    int x = cellX + 1;
                    int y = cellY + (x / sCellCountX);
                    cellX = x % sCellCountX;
                    cellY = y % sCellCountY;
                    screen += (y / sCellCountY);
                }
            } else {
                break;
            }
        }

        Log.d(TAG, "categoryAndSort(), others: " + screen + ", " + cellY + ", " + cellX);

        int othersScreen = -1;
        int othersCellY = -1;
        int othersCellX = -1;
        if (othersSize > 0) {
            othersScreen = screen;
            othersCellY = cellY;
            othersCellX = cellX;
            if (!((screen + 1 == mMaxIconScreenCount) &&
                    (cellY + 1 == sCellCountY) &&
                    (cellX + 1 == sCellCountX))) {
                int x = cellX + 1;
                int y = cellY + (x / sCellCountX);
                cellX = x % sCellCountX;
                cellY = y % sCellCountY;
                screen += (y / sCellCountY);
            }
        }

        Log.d(TAG, "categoryAndSort(), category size == 1: " + screen + ", " + cellY + ", " + cellX);

        for (; categoryIndex < categorySize; categoryIndex++) {
            Entry<String, List<ShortcutInfo>> categoryEntry = category.get(categoryIndex);
            List<ShortcutInfo> shortcutsFolder = (List<ShortcutInfo>)categoryEntry.getValue();
            if ((screen + 1 == mMaxIconScreenCount) &&
                    (cellY + 1 == sCellCountY) &&
                    (cellX + 1 == sCellCountX) &&
                    (categoryIndex < categorySize - 1)) {
                othersFolder.addAll(shortcutsFolder);
                othersSize = othersFolder.size();
            } else {
                ShortcutInfo shortcut = shortcutsFolder.get(0);
                if (!tempItems.contains(shortcut)) {
                    tempItems.add(shortcut);
                }
            }
        }

        if (othersSize > 0) {
            if (othersSize > 1) {
                FolderInfo folder = new FolderInfo();
                folder.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                folder.screen = othersScreen;
                folder.cellX = othersCellX;
                folder.cellY = othersCellY;
                folder.spanX = 1;
                folder.spanY = 1;
                folder.title = mApp.getString(R.string.others);
                Collections.sort(othersFolder, new Comparator() {
                    public int compare(Object object1, Object object2) {
                        return LauncherModel.this.compare(
                                (ItemInfo)object1, (ItemInfo)object2);
                    }
                });
                int folderScreen = 0;
                int folderCellX = 0;
                int folderCellY = 0;
                for (ShortcutInfo shortcut : othersFolder) {
                    shortcut.screen = folderScreen;
                    shortcut.cellX = folderCellX;
                    shortcut.cellY = folderCellY;
                    folder.contents.add(shortcut);
                    int x = folderCellX + 1;
                    int y = folderCellY + (x / folderMaxCountX);
                    folderCellX = x % folderMaxCountX;
                    folderCellY = y % folderMaxCountY;
                    folderScreen += (y / folderMaxCountY);
                }
                if (!mAddedFolders.contains(folder)) {
                    mAddedFolders.add(folder);
                }
                if (!allItems.contains(folder)) {
                    allItems.add(folder);
                }
            } else {
                ShortcutInfo shortcut = othersFolder.get(0);
                if (!tempItems.contains(shortcut)) {
                    tempItems.add(shortcut);
                }
                screen = othersScreen;
                cellX = othersCellX;
                cellY = othersCellY;
            }
        }

        othersFolder.clear();
        categoryFolders.clear();

        Log.d(TAG, "categoryAndSort(), temp: " + screen + ", " + cellY + ", " + cellX);

        if (tempItems.size() > 0) {
            Collections.sort(tempItems, new Comparator() {
                public int compare(Object object1, Object object2) {
                    return LauncherModel.this.compare(
                            (ItemInfo)object1, (ItemInfo)object2);
                }
            });
            for (ItemInfo item : tempItems) {
                item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                item.screen = screen;
                item.cellX = cellX;
                item.cellY = cellY;
                if (!allItems.contains(item)) {
                    allItems.add(item);
                }
                int x = cellX + 1;
                int y = cellY + (x / sCellCountX);
                cellX = x % sCellCountX;
                cellY = y % sCellCountY;
                screen += (y / sCellCountY);
            }
        }

        tempItems.clear();

        Log.d(TAG, "categoryAndSort(), deletable: " + screen + ", " + cellY + ", " + cellX);

        if (screen < mMaxIconScreenCount && deletableSize > 0) {
            for (; screen < mMaxIconScreenCount &&
                    deletableIndex < deletableSize; deletableIndex++) {
                ItemInfo item = deletableItems.get(deletableIndex);
                while ((cellX + item.spanX > sCellCountX) ||
                        (cellX > 0 && item.spanY > 1) ||
                        (cellX == 0 && (cellY + item.spanY > sCellCountY))) {
                    int y = cellY + 1;
                    cellX = 0;
                    cellY = y % sCellCountY;
                    screen += (y / sCellCountY);
                }
                if (screen == mMaxIconScreenCount) {
                    break;
                }
                item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                item.screen = screen;
                item.cellX = cellX;
                item.cellY = cellY;
                if (!allItems.contains(item)) {
                    allItems.add(item);
                }
                int x = cellX + item.spanX;
                int y = cellY;
                if (item.spanY > 1) {
                    cellX = 0;
                    y += item.spanY;
                } else {
                    cellX = x % sCellCountX;
                    y += (x / sCellCountX);
                }
                cellY = y % sCellCountY;
                screen += (y / sCellCountY);
            }
        }

        if (screen == mMaxIconScreenCount && deletableIndex < deletableSize) {
            for (; deletableIndex < deletableSize; deletableIndex++) {
                ItemInfo item = deletableItems.get(deletableIndex);
                if (!mRemovedItems.contains(item)) {
                    mRemovedItems.add(item);
                }
            }
        }

        deletableItems.clear();
    }

    private void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            ;
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                    ", expected " + firstElementName);
        }
    }

    private void loadTools() {
        try {
            XmlResourceParser parser = null;
            if (Utils.isForLightUpdate()) {
                parser = mApp.getResources().getXml(R.xml.default_workspace_lightupdate);
            } else if (Utils.isForCMCC()) {
                /* YUNOS BEGIN CMCC */
                //author:xianqiu.zbb
                //BugID:(8716966) ##date:2016.8.15
                //descrpition: 修改移动入库新的应用预置需求
                if (SystemProperties.getBoolean("ro.yunos.cmcc.newreq", false)) {
                    parser = mApp.getResources().getXml(R.xml.default_workspace_for_cmcc_new);
                } else {
                    parser = mApp.getResources().getXml(R.xml.default_workspace_for_cmcc);
                }
                /* YUNOS END CMCC */
            } else {
                parser = mApp.getResources().getXml(R.xml.default_workspace);
            }
            AttributeSet attrs = Xml.asAttributeSet(parser);
            beginDocument(parser, "favorites");
            final int depth = parser.getDepth();
            int type;
            while (((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                TypedArray array = mApp.obtainStyledAttributes(attrs, R.styleable.Favorite);
                long container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                if (array.hasValue(R.styleable.Favorite_container)) {
                    container = Long.valueOf(array.getString(R.styleable.Favorite_container));
                }
                if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    final String name = parser.getName();
                    if (name.equals("folder")) {
                        final int folderDepth = parser.getDepth();
                        while ((type = parser.next()) != XmlPullParser.END_TAG ||
                                parser.getDepth() > folderDepth) {
                            if (type != XmlPullParser.START_TAG) {
                                continue;
                            }
                            TypedArray itemArray = mApp.obtainStyledAttributes(attrs, R.styleable.Favorite);
                            final String itemName = parser.getName();
                            if (itemName.equals("favorite")) {
                                String packageName = itemArray.getString(R.styleable.Favorite_packageName);
                                Log.d(TAG, "loadTools(), shortcut in folder: " + packageName);
                                mTools.add(packageName);
                            }
                            itemArray.recycle();
                        }
                    }
                }
                array.recycle();
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Got exception parsing defaults.", e);
        } catch (IOException e) {
            Log.w(TAG, "Got exception parsing defaults.", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Got exception parsing defaults.", e);
        }
    }

    private void addCategory(ShortcutInfo shortcut, List<ShortcutInfo> othersFolder,
            Map<String, List<ShortcutInfo>> categoryFolders) {
        String packageName = shortcut.getPackageName();
        Log.d(TAG, "addCategory(), title: " + shortcut.title);
        if (TextUtils.isEmpty(packageName)) {
            if (!othersFolder.contains(shortcut)) {
                othersFolder.add(shortcut);
            }
        } else {
            String categoryId = mAppGroupMgr.getCatIdByPkg(packageName);
            Log.d(TAG, "addCategory(), category ID: " + categoryId);
            if (TextUtils.isEmpty(categoryId)) {
                if (!othersFolder.contains(shortcut)) {
                    othersFolder.add(shortcut);
                }
            } else {
                List<ShortcutInfo> categoryFolder = categoryFolders.get(categoryId);
                if (categoryFolder == null) {
                    categoryFolder = new ArrayList<ShortcutInfo>();
                }
                if (!categoryFolder.contains(shortcut)) {
                    categoryFolder.add(shortcut);
                }
                categoryFolders.put(categoryId, categoryFolder);
            }
        }
    }

    private int compare(ItemInfo item1, ItemInfo item2) {
        int position1 = 0;
        int position2 = 0;
        int folderMaxCountY = ConfigManager.getFolderMaxCountY();
        int folderMaxCountX = ConfigManager.getFolderMaxCountX();
        if (item1.container > 0 && item2.container > 0 &&
                item1.container == item2.container) {
            position1 = folderMaxCountX * folderMaxCountY * item1.screen
                    + folderMaxCountX * item1.cellY + item1.cellX;
            position2 = folderMaxCountX * folderMaxCountY * item2.screen
                    + folderMaxCountX * item2.cellY + item2.cellX;
        } else {
            position1 = sCellCountX * sCellCountY * item1.screen
                    + sCellCountX * item1.cellY + item1.cellX;
            if (item1.container > 0) {
                Log.d(TAG, "compare(), item1 title: " + item1.title
                        + ", container: " + item1.container);
                synchronized (sBgLock) {
                    FolderInfo folder =
                            (FolderInfo)sBgItemsIdMap.get(item1.container);
                    if (folder != null) {
                        Log.d(TAG, "compare(), folder title: " + folder.title);
                        position1 = sCellCountX * sCellCountY * folder.screen
                                + sCellCountX * folder.cellY + folder.cellX;
                    }
                }
            }
            position2 = sCellCountX * sCellCountY * item2.screen
                    + sCellCountX * item2.cellY + item2.cellX;
            if (item2.container > 0) {
                Log.d(TAG, "compare(), item2 title: " + item2.title
                        + ", container: " + item2.container);
                synchronized (sBgLock) {
                    FolderInfo folder =
                            (FolderInfo)sBgItemsIdMap.get(item2.container);
                    if (folder != null) {
                        Log.d(TAG, "compare(), folder title: " + folder.title);
                        position2 = sCellCountX * sCellCountY * folder.screen
                                + sCellCountX * folder.cellY + folder.cellX;
                    }
                }
            }
        }
        return (position1 - position2);
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/06/05 ##author:hongchao.ghc ##BugID:126343
    /**
     * check if showing the new mark icon or not
     *
     * @return
     */
    public static boolean isShowNewMarkIcon() {
        if (mShowNewMarkInit) {
            return mShowNewMark;
        }
        return updateShowNewMark();
    }

    private static boolean updateShowNewMark() {
        mShowNewMarkInit = true;
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                 Context.MODE_PRIVATE);
        return mShowNewMark = sp.getBoolean(HomeShellSetting.DB_SHOW_NEW_MARK_ICON, true);
    }
    /* YUNOS END */

    public static boolean isShowClonableMarkIcon() {
        if (mShowClonableMarkInit) {
            return mShowClonableMark;
        }
        return updateShowClonableMark();
    }

    private static boolean updateShowClonableMark() {
        mShowClonableMarkInit = true;
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                 Context.MODE_PRIVATE);
        return mShowClonableMark = sp.getBoolean(HomeShellSetting.DB_SHOW_CLONABLE_MARK_ICON, true);
    }

    public static boolean isShowSlideUpMarkIcon() {
        if (mShowSlideUpMarkInit) {
            return mShowSlideUpMark;
        }
        return updateShowSlideUpMark();
    }

    private static boolean updateShowSlideUpMark() {
        mShowSlideUpMarkInit = true;
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                 Context.MODE_PRIVATE);
        return mShowSlideUpMark = sp.getBoolean(HomeShellSetting.DB_SHOW_SLIDE_UP_MARK_ICON, true);
    }

    /** check if it's needed to show notification mark */
    public static boolean showNotificationMark() {
        if (FeatureUtility.isYunOS2_9System())
            return false;
        return getNotificationMarkType() != HomeShellSetting.NO_NOTIFICATION;
    }

    /**
     * if user upgrades from an old version, checkout it's earlier value
     * @return return the type how we show notifications
     */
    public static int getNotificationMarkType(){
        if (mMarkType != INVALID_MARK_TYPE) {
            return mMarkType;
        }
        return updateNotificationMarkType();
    }

    private static int updateNotificationMarkType(){
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        mMarkType = sp.getInt(HomeShellSetting.KEY_NOTIFICATION_MARK_PREF_NEW, INVALID_MARK_TYPE);
        if( mMarkType != INVALID_MARK_TYPE ){ // if it doesn't exist
            /* YUNOS BEGIN */
            // ##date:2015/9/14 ##author:zhanggong.zg ##BugID:6419517
            if (mMarkType == HomeShellSetting.PART_NOTIFICATION) {
                // "only display important notification" is not available,
                // automatically change to display all.
                mMarkType = HomeShellSetting.ALL_NOTIFICATION;
                sp.edit().putInt(HomeShellSetting.KEY_NOTIFICATION_MARK_PREF_NEW, mMarkType).commit();
            }
            /* YUNOS END */
            return mMarkType;
        }
        boolean showNotifcationOld = sp.getBoolean(HomeShellSetting.KEY_NOTIFICATION_MARK_PREF_OLD, true);
        return mMarkType = showNotifcationOld ? HomeShellSetting.ALL_NOTIFICATION : HomeShellSetting.NO_NOTIFICATION;
    }

    //BugID:139616:uninstall an app in folder, the info about this app not removed from db
    public void deleteItemsInDatabaseByPackageName(ArrayList<String> packages) {
        if ((packages == null) || (packages.size() <= 0)) {
            return;
        }
        final ArrayList<String> finalpackages = packages;
        Runnable r = new Runnable() {
            public void run() {
                Log.d(TAG, "deleteItemsInDatabaseByPackageName runnable in");
                ArrayList<ItemInfo> delItems = new ArrayList<ItemInfo>();
                for (String pkgname: finalpackages) {
                    if (pkgname == null) {
                        continue;
                    }
                    Hideseat.removePackageFromFrozenList(pkgname);
                    ArrayList<ItemInfo> allApps = getAllAppItems();
                    for (ItemInfo item: allApps) {
                        if ((item.itemType == Favorites.ITEM_TYPE_APPLICATION) ||
                           (item.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION)) {
                            Intent intent = ((ShortcutInfo)item).intent;
                            if ((intent == null) || (intent.getComponent() == null) ||
                                (intent.getComponent().getPackageName() == null)) {
                                continue;
                            }

                            if (pkgname.equals(intent.getComponent().getPackageName())) {
                                delItems.add(item);
                            }
                        }
                    }
                }

                for (ItemInfo delitem: delItems) {
                    deleteItemFromDatabase(mApp, delitem);
                    deleteItemFromAnotherTable(mApp, delitem);
                }
            }
        };
        runOnWorkerThread(r);
    }
    /*YUNOS END*/

    //BugID:144025:find and recover invalid position items
    //SCTODO:need remove
    private static void findAndFixInvalidItems() {
        Log.d(TAG, "findAndFixInvalidItems in ");
        if (mIsInProcess || (LauncherApplication.mLauncher != null && LauncherApplication.mLauncher.isInLauncherCategoryMode())) {
            return;
        }
        ArrayList<ItemInfo> invalidItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> needBindItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> needUpdateItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> needRemoveViewItems = new ArrayList<ItemInfo>();
        final HashMap<Long, FolderInfo> needBindFolderItems = new HashMap<Long, FolderInfo>();

        scanInvalidPositionItems(invalidItems);
        if (invalidItems.size() > 0 && !AgedModeUtil.isAgedMode()) {
            Log.d(TAG, "invalidItems.size is " + invalidItems.size());
            recoverInvalidPostionItems(invalidItems, needBindItems, needBindFolderItems, needUpdateItems, needRemoveViewItems);

        }
        Log.d(TAG, "findAndFixInvalidItems out");
    }

    //This function is used to check sBgItemsIdMap,
    //and find the invalid position items,
    //such as items in folder but the folder isn't exist,
    //items that position is out of range of screen
    private static void scanInvalidPositionItems(ArrayList<ItemInfo> invalidItems) {
        if (invalidItems == null) {
            return;
        }

        List<FolderInfo> invalidFolders = new ArrayList<FolderInfo>();
        ArrayList<ItemInfo> allApps = getAllAppItems();
        for (ItemInfo item: allApps) {
            if (item == null) {
                continue;
            }
            /* YUNOS BEGIN */
            // ## date: 2016/08/01 ## author: yongxing.lyx
            // ## BugID:8630568:there are some clones after uninstalled wechat.
            Intent intent = (item instanceof ShortcutInfo) ? ((ShortcutInfo) item).intent : null;
            PackageManager pm = LauncherApplication.getContext().getPackageManager();
            /* YUNOS END */
            if (isInvalidPosition(item) == true) {
                Log.d(TAG, "invalid position item: " + item.id + ", cloning item:"
                        + AppCloneManager.getInstance().getCloningItem());
                /* YUNOS BEGIN */
                // ## date: 2016/08/01 ## author: yongxing.lyx
                // ## BugID:8630568:there are some clones after uninstalled
                // wechat.
                // ++
                // ## date: 2016/09/11 ## author: yongxing.lyx
                // ## BugID:8851868:duplicated icons when cloning.
                if (intent != null && pm.resolveActivity(intent, 0) == null) {
                    Log.d(TAG, "invalid position item, resolveActivity failed, skip.." + item);
                } else if (AppCloneManager.getInstance().getCloningItem() == item) {
                    Log.d(TAG, "invalid position item, cloning item, skip.." + item);
                } else {
                    invalidItems.add(item);
                }
                /* YUNOS END */
                if (item instanceof FolderInfo) {
                    invalidFolders.add((FolderInfo) item);
                }
                continue;
            }

            if (isInDeletedFolder(item) == true) {
                Log.d(TAG, "in deleted folder item: " + item.id);
                /* YUNOS BEGIN */
                // ## date: 2016/08/01 ## author: yongxing.lyx
                // ## BugID:8630568:there are some clones after uninstalled wechat.
                if (intent != null && pm.resolveActivity(intent, 0) == null) {
                    Log.d(TAG, "invalid position item, resolveActivity failed, skip.." + item);
                } else {
                    invalidItems.add(item);
                }
                /* YUNOS END */
                continue;
            }
        }

        /* YUNOS BEGIN */
        // ##date:2015/6/16 ##author:zhanggong.zg ##BugID:6015804
        for (FolderInfo folderInfo : invalidFolders) {
            Iterator<ItemInfo> itr = invalidItems.iterator();
            while (itr.hasNext()) {
                ItemInfo item = itr.next();
                if (item.itemType != Favorites.ITEM_TYPE_FOLDER &&
                    item.container == folderInfo.id) {
                    // The item is invalid because the parent folder is invalid.
                    // Try to fix the folder and this item will be fine.
                    itr.remove();
                }
            }
        }
        /* YUNOS END */
    }

    //if the item is in folder and the folder isn't in sBgFolders
    //it means the item is lost.
    //this function can be invoked only after sBgFolders build complete.
    private static boolean isInDeletedFolder(ItemInfo item) {
        if (item.container < 0 ) {
            return false;
        }

        synchronized (sBgLock) {
            if (sBgFolders.containsKey(item.container)) {
                ItemInfo folderinfo = sBgFolders.get(item.container);
                if (isInvalidPosition(folderinfo) == false) {
                    return false;
                } else {
                    sBgFolders.remove(folderinfo.id);
                    if (sBgItemsIdMap.containsKey(folderinfo.id)) {
                        sBgItemsIdMap.remove(folderinfo.id);
                    }
                }
            }
        }
        return true;
    }

    //If new container type added, the new type must be added to this function
    private static boolean isInvalidPosition(ItemInfo item) {
        if (ConfigManager.isLandOrienSupport()) {
            //for LandOrien support, don't check invalid temply
            //to avoid position error during orien change
            return false;
        }
        // ##date:2015/3/10 ##author:zhanggong.zg ##BugID:5744988
        // valid cellX/Y lower bounds change to -1
        if (item.container == Favorites.CONTAINER_DESKTOP) {
            if (item.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                //no space app's screen, cellX and cellY all should be -1
                if ((item.screen != -1) || (item.cellX != -1) || (item.cellY != -1)) {
                    return true;
                }
            } else if ((item.screen >= ConfigManager.getIconScreenMaxCount()) ||
                (item.screen < 0) ||
                (item.cellX >= ConfigManager.getCellCountX()) ||
                (item.cellX < -1) ||
                (item.cellY >= ConfigManager.getCellCountY()) ||
                (item.cellY < -1)) {
                return true;
            }

            if (ConfigManager.isLandOrienSupport()) {
                if (LauncherApplication.isInLandOrientation() == true) {
                    if ((item.cellXPort < -1) ||
                        (item.cellXPort >= ConfigManager.getCellCountX(Configuration.ORIENTATION_PORTRAIT)) ||
                        (item.cellYPort < -1) ||
                        (item.cellYPort >= ConfigManager.getCellCountY(Configuration.ORIENTATION_PORTRAIT))){
                        return true;
                    }
                } else {
                    if ((item.cellXLand < -1) ||
                        (item.cellXLand >= ConfigManager.getCellCountX(Configuration.ORIENTATION_LANDSCAPE)) ||
                        (item.cellYLand < -1) ||
                        (item.cellYLand >= ConfigManager.getCellCountY(Configuration.ORIENTATION_LANDSCAPE))){
                        return true;
                    }
                }
            }
        }
        else if (item.container == Favorites.CONTAINER_HOTSEAT) {
            if (ConfigManager.isLandOrienSupport()) {
                if (LauncherApplication.isInLandOrientation() == true) {
                    //in hotseat, item's screen and cellx are same
                    if ((item.screen >= ConfigManager.getHotseatMaxCountY()) ||
                        (item.screen < -1)) {
                        return true;
                    }
                    if ((item.cellYLand >= ConfigManager.getHotseatMaxCountY()) ||
                        (item.cellYLand < -1)) {
                        return true;
                    }
                } else {
                    //in hotseat, item's screen and cellx are same
                    if ((item.screen >= ConfigManager.getHotseatMaxCountX()) ||
                        (item.screen < -1)) {
                        return true;
                    }
                    if ((item.cellXPort >= ConfigManager.getHotseatMaxCountX()) ||
                        (item.cellXPort < -1)) {
                        return true;
                    }
                }
            } else {
                //in hotseat, item's screen and cellx are same
                if ((item.screen >= ConfigManager.getHotseatMaxCountX()) ||
                    (item.screen < -1)) {
                    return true;
                }
                if ((item.cellX >= ConfigManager.getHotseatMaxCountX()) ||
                    (item.cellX < -1)) {
                    return true;
                }
            }
        }
        else if (item.container == Favorites.CONTAINER_HIDESEAT) {
            if ((item.screen >= ConfigManager.getHideseatScreenMaxCount()) ||
                (item.screen < 0) ||
                (item.cellX >= ConfigManager.getHideseatMaxCountX()) ||
                (item.cellX < -1) ||
                (item.cellY >= ConfigManager.getHideseatMaxCountY()) ||
                (item.cellY < -1)) {
                return true;
            }
        }
        else if (item.container >= -1) {
            if ((item.cellX >= ConfigManager.getFolderMaxCountX()) ||
                (item.cellX < -1) ||
                (item.cellY >= ConfigManager.getFolderMaxCountY()) ||
                (item.cellY < -1)) {
                return true;
            }
        }
        else {
            return true;
        }

        return false;
    }

    private static void recoverInvalidPostionItems(final ArrayList<ItemInfo> invalidItems,
                                            final ArrayList<ItemInfo> needBindItems,
                                            final HashMap<Long, FolderInfo> needBindFolderItems,
                                            final ArrayList<ItemInfo> needUpdateItems,
                                            final ArrayList<ItemInfo> needRemoveViewItems) {


        Runnable runOnUI = new Runnable() {

            @Override
            public void run() {
                HashMap<ItemInfo, ScreenPosition> tmpMap = null;
                //BugID:6050112:ArrayIndexOutOfBoundsException
                boolean isCorrect = true;
                try {
                    tmpMap = findEmptyCellsAndOccupy(ConfigManager.DEFAULT_FIND_EMPTY_SCREEN_START, invalidItems);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    isCorrect = false;
                }
                final HashMap<ItemInfo, ScreenPosition> posMap = tmpMap;
                Runnable runOnWork = new Runnable() {

                    @Override
                    public void run() {
                        ArrayList<ItemInfo> noSpaceItemsList = new ArrayList<ItemInfo>();
                        ArrayList<ItemInfo> removeList = new ArrayList<ItemInfo>();
                        for (ItemInfo invalidItem : invalidItems) {
                            if (invalidItem == null) {
                                continue;
                            }
                            Log.d(TAG, "invalidItem id is " + invalidItem.id);
                            ScreenPosition newPos = null;
                            if (posMap != null) {
                                newPos = posMap.get(invalidItem);
                            }
                            if (newPos != null) {
                                Log.d(TAG, "find a new position in workspace");
                                invalidItem.container = Favorites.CONTAINER_DESKTOP;
                                invalidItem.screen = newPos.s;
                                invalidItem.cellX = newPos.x;
                                invalidItem.cellY = newPos.y;
                                needBindItems.add(invalidItem);
                                //find new position, the item is a workspace items
                                synchronized (sBgLock) {
                                    if (sBgWorkspaceItems.contains(invalidItem) == false) {
                                        sBgWorkspaceItems.add(invalidItem);
                                    }
                                    /* YUNOS BEGIN */
                                    // ##date:2015/6/16 ##author:zhanggong.zg ##BugID:6015804
                                    // recover invalid folder
                                    if (sBgItemsIdMap.containsKey(invalidItem.id) == false) {
                                        sBgItemsIdMap.put(invalidItem.id, invalidItem);
                                    }
                                    if (invalidItem instanceof FolderInfo &&
                                        sBgFolders.containsKey(invalidItem.id) == false) {
                                        sBgFolders.put(invalidItem.id, (FolderInfo) invalidItem);
                                    }
                                    /* YUNOS END */
                                }
                            } else {
                                Log.d(TAG, "no space for the invalid item " + invalidItem.id);
                                if ((invalidItem.itemType == Favorites.ITEM_TYPE_GADGET) ||
                                    (invalidItem.itemType == Favorites.ITEM_TYPE_APPWIDGET) ||
                                    (invalidItem.itemType == Favorites.ITEM_TYPE_ALIAPPWIDGET)) {
                                    //no space for these items, just remove them
                                    removeList.add(invalidItem);
                                } else if (invalidItem.itemType == Favorites.ITEM_TYPE_FOLDER) {
                                    //since no space for folder, there has no space for items in the folder too
                                    //delet the folder and find new folder for items in it
                                    for(ItemInfo itemInFolder: ((FolderInfo)invalidItem).contents) {
                                        noSpaceItemsList.add(itemInFolder);
                                    }
                                    removeList.add(invalidItem);
                                } else {
                                    //find or create a folder for these nospace items
                                    noSpaceItemsList.add(invalidItem);
                                }
                            }
                        }

                        if (removeList.size() > 0) {
                            //remove no space widget
                            for (ItemInfo wItem: removeList) {
                                deleteItemFromDatabase(LauncherApplication.getContext(), wItem);
                            }
                            removeList.clear();
                        }

                        if (noSpaceItemsList.size() > 0) {

                            //create current layout data
                            //screen + 1 for hotseat, + 6 for hideseat
                            /* HIDESEAT_SCREEN_NUM_MARKER: see ConfigManager.java */
                            final ItemInfo occupied[][][] = new ItemInfo[mMaxIconScreenCount + 1
                                    + ConfigManager.getHideseatScreenMaxCount()][sCellCountX + 1][sCellCountY + 1];
                            createCurrentLayoutData(occupied);

                            //Put all noSpaceItems in a new folder.
                            //find a single item in workspace
                            //put no space items and the single item in a new folder
                            //in the single item's position.
                            //If no empty position or single item found, I have to leave
                            //these nospace item in -1 state.
                            FolderInfo newfolder = null;

                            for (ItemInfo noSpaceItem: noSpaceItemsList) {
                                if ((newfolder == null) || (newfolder.contents.size() >= ConfigManager.getFolderMaxItemsCount())) {
                                    //no empty for newfolder, find a item in workspace and create a new folder at the item's position
                                    ItemInfo firstSingleItem = null;
                                    for (int s = ConfigManager.getIconScreenMaxCount() - 1; s > 0; s--) {
                                        for (int y = ConfigManager.getCellMaxCountY() -1; y >= 0; y--) {
                                            for (int x = ConfigManager.getCellMaxCountX() -1; x >= 0; x--) {
                                                ItemInfo item = occupied[s][x][y];
                                                if (item != null) {
                                                    if ((item.itemType == Favorites.ITEM_TYPE_APPLICATION) ||
// remove vp install
//                                                        (item.itemType == Favorites.ITEM_TYPE_VPINSTALL) ||
                                                        (item.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) ||
                                                        (item.itemType == Favorites.ITEM_TYPE_SHORTCUT)) {
                                                        firstSingleItem = item;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (firstSingleItem != null) {
                                                break;
                                            }
                                        }
                                        if (firstSingleItem != null) {
                                            break;
                                        }
                                    }
                                    if (firstSingleItem != null) {
                                        newfolder = new FolderInfo();
                                        newfolder.container = Favorites.CONTAINER_DESKTOP;
                                        newfolder.screen = firstSingleItem.screen;
                                        newfolder.cellX = firstSingleItem.cellX;
                                        newfolder.cellY = firstSingleItem.cellY;
                                        newfolder.title = firstSingleItem.title + LauncherApplication.getContext().getResources().getString(R.string.folder_name_etc);

                                        addItemToDatabase(LauncherApplication.getContext(), newfolder, newfolder.container, newfolder.screen,
                                                                     newfolder.cellX, newfolder.cellY, false);
                                        Log.d(TAG, "the newfolder id is " + newfolder.id);
                                        synchronized (sBgLock) {
                                            sBgFolders.put(newfolder.id, newfolder);
                                            sBgItemsIdMap.put(newfolder.id, newfolder);
                                            if (!sBgWorkspaceItems.contains(newfolder)) {
                                                sBgWorkspaceItems.add(newfolder);
                                            }
                                        }

                                        //put the firstSingleItem in newfolder;
                                        newfolder.contents.add((ShortcutInfo)firstSingleItem);
                                        firstSingleItem.container = newfolder.id;
                                        firstSingleItem.screen = 0;
                                        firstSingleItem.cellX = 0;
                                        firstSingleItem.cellY = 0;
                                        synchronized (sBgLock) {
                                            if (sBgWorkspaceItems.contains(firstSingleItem)) {
                                                sBgWorkspaceItems.remove(firstSingleItem);
                                            }
                                        }
                                        //put the no space item in newfolder
                                        newfolder.contents.add((ShortcutInfo)noSpaceItem);
                                        //BugID:5697156:fix homeshell crash
                                        noSpaceItem.container = newfolder.id;
                                        noSpaceItem.screen = 0;
                                        //the no space item is the second item in newfolder
                                        //so x and y is 0
                                        noSpaceItem.cellX = 1;
                                        noSpaceItem.cellY = 0;
                                        //BugID:5697085:app can't be frozen
                                        if (noSpaceItem.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                            noSpaceItem.itemType = Favorites.ITEM_TYPE_APPLICATION;
                                        }
                                        occupied[newfolder.screen][newfolder.cellX][newfolder.cellY] = newfolder;

                                        needBindItems.add(newfolder);
                                        needBindFolderItems.put(newfolder.id, newfolder);
                                        needRemoveViewItems.add(firstSingleItem);
                                        needUpdateItems.add(noSpaceItem);
                                        needUpdateItems.add(firstSingleItem);
                                    } else {
                                        //no single item in workspace, my god
                                        //remove no app items and set app item as no space item
                                        if ((noSpaceItem.itemType == Favorites.ITEM_TYPE_APPLICATION) ||
                                            (noSpaceItem.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION)){
                                            noSpaceItem.itemType = Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                                            noSpaceItem.container = Favorites.CONTAINER_DESKTOP;
                                            noSpaceItem.screen = -1;
                                            noSpaceItem.cellX = -1;
                                            noSpaceItem.cellY = -1;
                                            updateItemInDatabase(LauncherApplication.getContext(), noSpaceItem);
                                        } else {
                                            deleteItemFromDatabase(LauncherApplication.getContext(), noSpaceItem);
                                        }
                                    }
                                } else {
                                    //put the no space item in newfolder
                                    newfolder.contents.add((ShortcutInfo)noSpaceItem);
                                    noSpaceItem.container = newfolder.id;
                                    noSpaceItem.screen = 0;
                                    noSpaceItem.cellX = newfolder.contents.size() % ConfigManager.getFolderMaxCountY();
                                    noSpaceItem.cellY = newfolder.contents.size() / ConfigManager.getFolderMaxCountY();
                                    //BugID:5697085:app can't be frozen
                                    if (noSpaceItem.itemType == Favorites.ITEM_TYPE_NOSPACE_APPLICATION) {
                                        noSpaceItem.itemType = Favorites.ITEM_TYPE_APPLICATION;
                                    }
                                    needUpdateItems.add(noSpaceItem);
                                }
                            }
                        }
                        //update in db
                        for (ItemInfo item: needUpdateItems) {
                            updateItemInDatabase(LauncherApplication.getContext(), item);
                        }

                        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks != null) {
                            final Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    callbacks.bindItemsViewRemoved(needRemoveViewItems);
                                    callbacks.bindItems(needBindItems,0,needBindItems.size());
                                    callbacks.bindFolders(needBindFolderItems);
                                }
                            };
                            runOnMainThread(r);
                        }
                        invalidItems.clear();
                        posMap.clear();
                    }
                };
                if (isCorrect == true) {
                    runOnWorkerThread(runOnWork);
                }
            }
        };
        runOnMainThread(runOnUI);
    }

    static Runnable mCheckInvalidPosItemsRunnable = new Runnable() {
        public void run() {
            try {
                findAndFixInvalidItems();
//                checkAndFixOverlapItems();
            } catch (Exception ex) {
                Log.e(TAG, "findAndFixInvalidItems exception");
            }
        }
    };
    //YUNSO END

    public static int calcEmptyCell(int startScreen) {
        Log.d(TAG, "calcEmptyCell in");
        checkRunOnUIThread();
        int orientation = LauncherApplication.isInLandOrientation() ?
                Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        int xCount = ConfigManager.getCellCountX(orientation);
        int yCount = ConfigManager.getCellCountY(orientation);
        int ecCount = 0;

        ArrayList<boolean[][]> occupied = getWorkspaceOccupied();
         //##date:2013/11/25 ##author:hongxing.whx
         // the main screen (index = 0) is special case, installed application and shotcuts will not be on it.
        if (startScreen >= occupied.size()) {
            return 0;
        }
        for (int scr = startScreen; scr < occupied.size(); scr++) {
            for (int x = 0; x < xCount; x++) {
                for (int y = 0; y < yCount; y++) {
                    boolean[][] pos = occupied.get(scr);
                    if (pos[x][y] == false && !isDragInfoOccupied(scr, x, y)) {
                        ecCount++;
                    }
                }
            }
        }
        Log.d(TAG, "calcEmptyCell out");
        return ecCount;
    }

    public static void getEmptyPosListAndOccupy(ArrayList<ScreenPosition> posList, int startScreen, int endScreen, int reqCount) {
        Log.d(TAG, "getEmptyPosList in startScreen " + startScreen + " endScreen " + endScreen + " reqCount " + reqCount);
        checkRunOnUIThread();
        if (posList == null) {
            posList = new ArrayList<ScreenPosition>(reqCount);
        }
        if (startScreen < 0) {
            startScreen = 0;
        } else if (startScreen >= mMaxIconScreenCount) {
            startScreen = mMaxIconScreenCount - 1;
        }
        if (endScreen < 0) {
            endScreen = 0;
        } else if (endScreen >= mMaxIconScreenCount) {
            endScreen = mMaxIconScreenCount - 1;
        }

        int index = 0;
        ScreenPosition screenPosition = null;
        while (index < reqCount) {
            screenPosition = findEmptyCellAndOccupy(startScreen, endScreen, 1, 1);
            if (screenPosition != null) {
                posList.add(screenPosition);
            }
            index++;
        }

        dumpUIOccupied();
        Log.d(TAG, "getEmptyPosList out posList.size " + posList.size());
    }

    public AppDownloadManager getAppDownloadManager(){
        return mAppDownloadMgr;
    }

    //BugID:5183000:items overlay after folder dismiss
    //SCTODO:need remove
    public static void checkAndFixOverlapItems(){
        Log.d(TAG, "checkAndFixOverlapItems in");
        int screen;
        int x;
        int y;
        int spanX;
        int spanY;
        int xCount = sCellCountX;
        int yCount = sCellCountY;
        int scrnCount = mMaxIconScreenCount;
        boolean isOccupied = false;
        boolean[][][] occupied = new boolean[scrnCount][xCount][yCount];

        final ArrayList<ItemInfo> overlapItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> updateViewItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> removeItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> removeViewItems = new ArrayList<ItemInfo>();

        /* YUNOS BEGIN */
        // ##date:2015/03/16 ##author:sunchen.sc
        // Copy bg member to solve concurrent modification issue
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<ItemInfo>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<ItemInfo>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        /* YUNOS END */
        //find out the overlap items in sBgWorkspaceItems
        for (ItemInfo info : tmpWorkspaceItems) {
            isOccupied = false;
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screen = info.screen;
                if (!(screen > -1 && screen < scrnCount)) {
                    continue;
                }

                x = info.cellX;
                y = info.cellY;
                spanX = info.spanX;
                spanY = info.spanY;
                if ((x >= sCellCountX) ||
                    (y >= sCellCountY) ||
                    (x + spanX > sCellCountX) ||
                    (y + spanY > sCellCountY) ||
                    (x < 0) || (y < 0)) {
                    Log.d(TAG, "initOccupied item position error " + info.id);
                    continue;
                }

                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[screen][x + i][y + j] == true) {
                            Log.d(TAG, "item is overlap " +info.id);
                            overlapItems.add(info);
                            isOccupied = true;
                            break;
                        } else {
                            occupied[screen][x + i][y + j] = true;
                        }
                    }
                    if (isOccupied == true) {
                        break;
                    }
                }
            }
        }

        //find out the overlap items in sBgAppWidgets
        for (ItemInfo info : tmpAppWidgets) {
            isOccupied = false;
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                screen = info.screen;
                x = info.cellX;
                y = info.cellY;
                spanX = info.spanX;
                spanY = info.spanY;
                if ((x >= sCellCountX) ||
                    (y >= sCellCountY) ||
                    (x + spanX > sCellCountX) ||
                    (y + spanY > sCellCountY) ||
                    (x < 0) || (y < 0) ||
                    (screen < 0) || (screen >= mMaxIconScreenCount)) {
                    Log.d(TAG, "initOccupied item position error " + info.id);
                    continue;
                }
                for (int i = 0; i < spanX; i++) {
                    for (int j = 0; j < spanY; j++) {
                        if (occupied[screen][x + i][y + j] == true) {
                            Log.d(TAG, "item is overlap " +info.id);
                            overlapItems.add(info);
                            isOccupied = true;
                            break;
                        } else {
                            occupied[screen][x + i][y + j] = true;
                        }
                    }
                    if (isOccupied == true) {
                        break;
                    }
                }
            }
        }

        if (overlapItems.size() == 0) {
            return;
        }

        Runnable runOnUI = new Runnable() {

            @Override
            public void run() {
                final HashMap<ItemInfo, ScreenPosition> posMap = findEmptyCellsAndOccupy(1, overlapItems);
                Runnable runOnWork = new Runnable() {

                    @Override
                    public void run() {
                        for (ItemInfo info : overlapItems) {
                            ScreenPosition pos = null;
                            if (posMap != null) {
                                pos = posMap.get(info);
                            }
                            if (pos == null) {
                                //if no position, change app type to no-space app
                                //and delete other type items
                                if (info.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                                    info.container = Favorites.CONTAINER_DESKTOP;
                                    info.screen = -1;
                                    info.cellX = -1;
                                    info.cellY = -1;
                                    info.itemType = Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                                    updateItemInDatabase(LauncherApplication.getContext(), info);
                                    removeViewItems.add(info);
                                } else {
                                    if (info.itemType == Favorites.ITEM_TYPE_FOLDER) {
                                        //change app type to no-space app
                                        //and delete other type items in the folder
                                        FolderInfo folder = (FolderInfo)info;
                                        if (folder.contents != null) {
                                            for (ItemInfo iteminfolder: folder.contents) {
                                                if (iteminfolder.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                                                    iteminfolder.container = Favorites.CONTAINER_DESKTOP;
                                                    iteminfolder.screen = -1;
                                                    iteminfolder.cellX = -1;
                                                    iteminfolder.cellY = -1;
                                                    iteminfolder.itemType = Favorites.ITEM_TYPE_NOSPACE_APPLICATION;
                                                    updateItemInDatabase(LauncherApplication.getContext(), iteminfolder);
                                                    removeViewItems.add(iteminfolder);
                                                } else {
                                                    removeItems.add(iteminfolder);
                                                }
                                            }
                                        }
                                    }
                                    removeItems.add(info);
                                }
                            } else {
                                info.container = Favorites.CONTAINER_DESKTOP;
                                info.screen = pos.s;
                                /* YUNOS BEGIN */
                                // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
                                // Modified to support pad orientation
                                if (LauncherApplication.isInLandOrientation()) {
                                    info.cellX = pos.xLand;
                                    info.cellY = pos.yLand;
                                    info.cellXLand = pos.xLand;
                                    info.cellYLand = pos.yLand;
                                } else {
                                    info.cellX = pos.xPort;
                                    info.cellY = pos.yPort;
                                    info.cellXPort = pos.xPort;
                                    info.cellYPort = pos.yPort;
                                }
                                /* YUNOS END */
                                info.cellX = pos.x;
                                info.cellY = pos.y;
                                updateItemInDatabase(LauncherApplication.getContext(), info);
                                updateViewItems.add(info);
                            }
                        }

                        final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks != null) {
                            final Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    callbacks.bindItemsRemoved(removeItems);
                                    callbacks.bindItemsViewRemoved(removeViewItems);
                                    callbacks.bindWorkspaceItemsViewMoved(updateViewItems);
                                }
                            };
                            runOnMainThread(r);
                        }

                        overlapItems.clear();
                    }
                };
                runOnWorkerThread(runOnWork);
            }
        };
        runOnMainThread(runOnUI);
        Log.d(TAG, "checkAndFixOverlapItems out");
    }
    public boolean isWorkspaceLoaded() {
        return mWorkspaceLoaded;
    }

    //BugID:5241055:delete sd card app icon no toast during sd card unmounted
    public void removeComponentFormAllAppList(String pkgName) {
        final String packageName = pkgName;
        final Runnable r = new Runnable(){
            @Override
            public void run() {
                mBgAllAppsList.removePackageFromData(packageName);
            }
        };
        runOnWorkerThread(r);
    }


    public static void startLoadAppGroupInfo(long delayTime) {
        final AppGroupManager manager = AppGroupManager.getInstance();
        manager.setStatus(true);
        sWorker.postDelayed(new Runnable() {

            @Override
            public void run() {
                manager.loadAppGroupInfosFromServer();
            }
        }, delayTime);
    }

    /* YUNSO END */
    private void moveToFolderNextPos(ScreenPosition pos) {
        int maxX = ConfigManager.getFolderMaxCountX();
        int maxY = ConfigManager.getFolderMaxCountY();
        if (pos.x < maxX - 1) {
            pos.x = pos.x + 1;
        } else if (pos.x == maxX - 1) {
            if (pos.y < maxY - 1) {
                pos.y = pos.y + 1;
                pos.x = 0;
            } else if (pos.y == maxY - 1) {
                pos.s = pos.s + 1;
                pos.x = pos.y = 0;
            } else {
                Log.d(AgedModeUtil.TAG, "error favorites xml");
            }
        } else {
            Log.d(AgedModeUtil.TAG, "error favorites xml");
        }
    }

    public void switchDbForAgedMode(boolean agedMode) {
        SwitchDB sw = new SwitchDB(agedMode);
        sWorker.post(sw);
    }

    /* YUNOS BEGIN */
    // ##date:2014/12/25 ##author:zhanggong.zg ##BugId:5641141
    // suspend package add/remove tasks in special situation,
    // e.g., screen-editing mode or dragging icons.

    public PackageUpdateTaskQueue getPackageUpdateTaskQueue() {
        return mPackageUpdateTaskQueue;
    }

    public static final class PackageUpdateTaskQueue {

        private static final String TAG = "PackageUpdateTaskQueue";

        // the fields below need to be synchronized to "this"
        private final List<PackageUpdatedTask> mQueue = new ArrayList<PackageUpdatedTask>();
        private int mLock = 0; // suspend tasks when mLock > 0

        private PackageUpdateTaskQueue() {
        }

        public synchronized void reset() {
            mQueue.clear();
            mLock = 0;
        }

        /**
         * When this method get called, <code>LauncherModel</code> will suspend
         * incoming package add/remove tasks, until the last {@link #releaseLock()}
         * get called. Method {@link #retainLock(String)} and {@link #releaseLock()}
         * must be called in pairs.<p/>This method is thread-safe.
         * @param tag used to print log
         */
        public synchronized void retainLock(String tag) {
            mLock++;
            Log.d(TAG, String.format("retainLock: count=%d tag=%s", mLock, tag));
        }

        /**
         * See {@link #retainLock(String)}.<p/>
         * This method is thread-safe.
         */
        public void releaseLock() {
            final List<PackageUpdatedTask> copiedTasks = new ArrayList<PackageUpdatedTask>(0);
            synchronized (this) {
                mLock--;
                Log.d(TAG, String.format("releaseLock: count=%d", mLock));
                if (mLock < 0) {
                    Log.e(TAG, "releaseLock: count is negative");
                    mLock = 0;
                }
                // if mLock is down to zero, execute all tasks in mQueue
                if (mLock == 0 && !mQueue.isEmpty()) {
                    copiedTasks.addAll(mQueue);
                    mQueue.clear();
                }
            }

            /* YUNOS BEGIN */
            // ##date:2015-1-21 ##author:zhanggong.zg ##BugID:5684630
            if (!copiedTasks.isEmpty()) {
                Log.d(TAG, String.format("releaseLock: prepare to run %d tasks", copiedTasks.size()));
                final int[] index = new int[] { 0 }; // Use an array to wrap the index so it
                                                     // can be modified in a runnable.
                final PackageUpdatedTaskListener callback = new PackageUpdatedTaskListener() {
                    @Override
                    public void onPackageUpdatedTaskFinished(PackageUpdatedTask task) {
                        // when the current task is done, schedule the next.
                        task.setListener(null);
                        if (index[0] < copiedTasks.size() - 1) {
                            index[0]++;
                            Log.d(TAG, String.format("releaseLock: run task #" + index[0]));
                            PackageUpdatedTask nextTask = copiedTasks.get(index[0]);
                            nextTask.setListener(this);
                            sWorker.post(nextTask);
                        } else {
                            copiedTasks.clear();
                            Log.d(TAG, String.format("releaseLock: all tasks done"));
                        }
                    }
                };
                // start the first task
                Log.d(TAG, String.format("releaseLock: run task #" + index[0]));
                PackageUpdatedTask firstTask = copiedTasks.get(index[0]);
                firstTask.setListener(callback);
                sWorker.post(firstTask);
            }
            /* YUNOS END */
        }

        private synchronized void enqueue(PackageUpdatedTask task) {
            if (mLock > 0 &&
                    (task.mOp == PackageUpdatedTask.OP_ADD ||
                     task.mOp == PackageUpdatedTask.OP_REMOVE)) {
                // task needs to wait in queue until releaseLock() is called
                Log.d(TAG, "enqueue: task=" + task);
                mQueue.add(task);
            } else {
                // normal situation, schedule to worker thread immediately
                sWorker.post(task);
            }
        }
    }

    /* YUNOS END */
    public static void checkRunOnWorkerThread() {
        if (sEnableThreadCheck && sWorkerThread.getThreadId() != Process.myTid()) {
            throw new RuntimeException("Should be called in lauchermodel worker thread");
        }
    }
    public static void checkRunOnUIThread() {
        if (sEnableThreadCheck && sUIThreadid != Process.myTid()) {
            throw new RuntimeException("Should be called in ui thread");
        }
    }

    private String[] mLayoutTitle = null;
    private String[] mLayoutValue = null;
    private SharedPreferences preference = null;
    public void updateLayoutPref() {
        if (mLayoutTitle == null || mLayoutValue == null || preference == null) {
            mLayoutTitle = mApp.getResources().getStringArray(R.array.entries_layout_preference);
            mLayoutValue = mApp.getResources()
                    .getStringArray(R.array.entryvalues_layout_preference);
            preference = PreferenceManager.getDefaultSharedPreferences(mApp);
        }
        String layoutValue = "0";
        String layoutTitle = "" + sCellCountX + "x" + sCellCountY;
        for (int i = 0; i < mLayoutTitle.length; i++) {
            if (layoutTitle.equals(mLayoutTitle[i])) {
                layoutValue = mLayoutValue[i];
            }
        }
        preference.edit().putString(HomeShellSetting.KEY_PRE_LAYOUT_STYLE, layoutValue).commit();
    }
    public static class DragInfo {
        // public View cell;
        public int cellX = -1;
        public int cellY = -1;
        public int spanX;
        public int spanY;
        public int screen;
        public long container;

        @Override
        public String toString() {
            return "Cell[screen = " + screen + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }

    private static HashMap<View, DragInfo> sDragInfoList = new HashMap<View, DragInfo>();

    public static void addDragInfo(View cell, DragInfo dragInfo) {
        sDragInfoList.put(cell, dragInfo);
    }

    public static void removeDragInfo(View cell) {
        sDragInfoList.remove(cell);
    }
    public static void clearDragInfo() {
        sDragInfoList.clear();
    }

    private static boolean isDragInfoOccupied(int screen, int cellX, int cellY) {
        Iterator iter = sDragInfoList.entrySet().iterator();
        boolean isOccupied = false;

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            DragInfo dragInfo = (DragInfo) entry.getValue();

            //BugID:6345137:the dragInfo's screen include lifecenter
            Launcher launcher = LauncherApplication.getLauncher();
            int offSet = 0;
            if (launcher != null && launcher.getWorkspace() != null) {
                offSet = launcher.getWorkspace().getIconScreenHomeIndex();
            }
            if (dragInfo.screen == screen + offSet) {
                if (cellX >= dragInfo.cellX && cellX < (dragInfo.cellX + dragInfo.spanX) &&
                   cellY >= dragInfo.cellY && cellY < (dragInfo.cellY + dragInfo.spanY)) {
                    isOccupied = true;
                }
            }
        }
        return isOccupied;
    }

    public static boolean hasWidgetInScreen(String packageName, String className, int screen){
        final ComponentName providerName = new ComponentName(packageName, className);
        boolean has = false;
        final ArrayList<LauncherAppWidgetInfo> widgetlist = sBgAppWidgets;
        for (LauncherAppWidgetInfo info : widgetlist) {
            if (info.screen == screen && info.providerName.equals(providerName)) {
                has = true;
                break;
            }
        }
        return has;
    }

    public static boolean hasSearchWidget(int screen){
        return hasWidgetInScreen("com.yunos.alimobilesearch","com.yunos.alimobilesearch.widget.SearchWidgetProvider", screen);
    }

    public static boolean hasSearchNowCardWidget(int screen){
        return hasWidgetInScreen("com.yunos.lifecard","com.yunos.alimobilesearch.widget.SearchWidgetProvider", screen) ||
               hasWidgetInScreen("com.yunos.lifecard","com.yunos.alimobilesearch.widget.SearchNowCardWidgetProvider", screen)||
               hasWidgetInScreen("com.yunos.lifecard","com.yunos.alimobilesearch.widget.NowCardWeatherProvider4X1", screen);
    }

    public void transferCoordinateForOrienChanged() {
        for (ItemInfo info : sBgWorkspaceItems) {
            info.setCellXY();
        }
        for(LauncherAppWidgetInfo info: sBgAppWidgets) {
            info.setCellXY();
        }
        Log.d(TAG,"sxsexe_pad transferCoordinate for orien chagne has completed");

    }

    private static void updateScreenAllXYOnWidgetAdd(ItemInfo info) {
      //notify UI Screen to update XY
        if(ConfigManager.isLandOrienSupport()) {
            Launcher launcher = LauncherApplication.getLauncher();
            if(launcher != null && launcher.getWorkspace() != null) {
                final CellLayout cellLayout = (CellLayout) launcher.getWorkspace().getChildAt(info.screen);
                Runnable updateCellLayoutXYRunnable = new Runnable() {
                    public void run() {
                        if(cellLayout != null) {
                            cellLayout.transferAllXYsOnDataChanged();
                        }
                    }
                };
                runOnMainThread(updateCellLayoutXYRunnable);
            }
        }
    }
    public void addWaitingBindForOrienChanged() {
        mWaitBindForOrienChangedCount ++;
    }
    public void clearWaitingCount() {
        mWaitBindForOrienChangedCount = 0;
    }
    public int getWaitingBindForOrienChanged() {
        return mWaitBindForOrienChangedCount;
    }

    public static boolean isInProcess() {
        return mIsInProcess;
    }

    public boolean UpdateItemIconByComponentName(ComponentName cmpName) {
        boolean result = false;
        ArrayList<ItemInfo> itemList = getAllAppItems();
        for (ItemInfo item : itemList) {
            if (item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                if (((ShortcutInfo) item).intent.getComponent().equals(cmpName) == true) {
                    Log.d(TAG, "liuhao find the item " + item.title);
                    ShortcutInfo scInfo = (ShortcutInfo) item;
                    mIconManager.clearIconCache(scInfo.intent);
                    if(mIconManager.supprtCardIcon()) {
                        mIconManager.clearCardBackgroud(scInfo.intent);
                    }
                    scInfo.setIcon(mIconManager.getAppUnifiedIcon(scInfo.intent));
                    ArrayList<ItemInfo> list = new ArrayList<ItemInfo>(1);
                    list.add(item);
                    notifyUIUpdateIcon(list);
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public void findAndRemoveExpiredUpdateIcon() {
        sWorker.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIconUpdateMgr.findAndRemoveExpireForCurrentItems();
            }
        }, 2000);
    }
   
    /* YUNOS BEGIN */
    // ## date: 2016/06/29 ## author: yongxing.lyx
    // ## BugID:8471126:show paused after changed language when downloaded app is installing.
    private void updateDownloadStatus(ShortcutInfo info) {
        if (info.itemType == Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING) {
            if (mAppDownloadMgr.getStatus(info.getPackageName(), true) == AppDownloadStatus.STATUS_INSTALLING) {
                info.setAppDownloadStatus(AppDownloadStatus.STATUS_INSTALLING);
                info.setProgress(100);
            }
        }
    }
    /* YUNOS BEGIN */

    /* YUNOS BEGIN */
    // ## date: 2016/06/20 ## author: yongxing.lyx
    // ## BugID:8411951:add new icon to other-app folder when swith to aged mode.
    private FolderInfo getAgedModelDefaultFolder(Context context) {
        for (FolderInfo info : sBgFolders.values()) {
            if (info.title != null && info.title.equals("#string/other_apps")) {
                return info;
            }
        }
        return null;
    }
    /* YUNOS END */

    static class InstallingRecord {
        String packageName;
        long startTime;

        InstallingRecord(String packageName, long startTime) {
            this.packageName = packageName;
            this.startTime = startTime;
        }
    }

    public static boolean IsInstallingApp() {
        if (sInstallingApps == null) {
            return false;
        }
        synchronized (sInstallingApps) {
            long currentTime = System.currentTimeMillis();
            dumpInstallingRecord("before");
            for (int i = sInstallingApps.size() - 1; i >= 0; i--) {
                InstallingRecord rec = sInstallingApps.get(i);
                if (currentTime - rec.startTime > APP_INSTALL_TIME_MAX) {
                    sInstallingApps.remove(i);
                }
            }
            dumpInstallingRecord("after");
            return sInstallingApps.size() > 0;
        }
    }

    public static void setAppInstalling(String packageName) {

        synchronized (sInstallingApps) {
            if (packageName == null) {
                return;
            }

            long currTime = System.currentTimeMillis();
            boolean updated = false;
            for (InstallingRecord rec : sInstallingApps) {
                if (packageName.equals(rec.packageName)) {
                    rec.startTime = currTime;
                    updated = true;
                }
            }
            if (!updated) {
                InstallingRecord rec = new InstallingRecord(packageName, currTime);
                sInstallingApps.add(rec);
            }
        }
    }

    public static void resetAppInstalling(String packageName) {
        synchronized (sInstallingApps) {
            if (packageName == null || sInstallingApps == null) {
                return;
            }
            long currTime = System.currentTimeMillis();
            for (int i = sInstallingApps.size() - 1; i >= 0; i--) {
                InstallingRecord rec = sInstallingApps.get(i);
                if (packageName.equals(rec.packageName)) {
                    sInstallingApps.remove(i);
                }
            }
        }
    }

    private static void dumpInstallingRecord(String tag) {
        Log.d(TAG, "dumpInstallingRecord() " + tag + " size:" + sInstallingApps.size());
        long currTime = System.currentTimeMillis();
        for (InstallingRecord rec : sInstallingApps) {
            Log.d(TAG, "dumpInstallingRecord() " + tag + " packageName:" + rec.packageName
                    + " invalid:" + (currTime - rec.startTime > APP_INSTALL_TIME_MAX));
        }
    }

}
