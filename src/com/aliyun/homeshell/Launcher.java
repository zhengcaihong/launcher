
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

package com.aliyun.homeshell;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;

import app.aliyun.aml.FancyDrawable;
import app.aliyun.v3.gadget.GadgetInfo;
import app.aliyun.v3.gadget.GadgetView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.StatusBarManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.AliFingerprintCallBack;
import android.hardware.fingerprint.AliFingerprintManager;
import android.hardware.fingerprint.RegisterInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Advanceable;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.homeshell.AppDownloadManager.AppDownloadStatus;
import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.FolderIcon.FolderRingAnimator;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.activateapp.ActivateGuideFragment;
import com.aliyun.homeshell.activateapp.FootprintBase;
import com.aliyun.homeshell.animation.FlipAnimation;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.appclone.HintDialogUitils;
import com.aliyun.homeshell.appclone.AppCloneManager.AppCloneCallback;
import com.aliyun.homeshell.appgroup.AppGroupManager;
import com.aliyun.homeshell.backuprestore.BackupManager;
import com.aliyun.homeshell.editmode.EditModeHelper;
import com.aliyun.homeshell.editmode.PreviewContainer;
import com.aliyun.homeshell.editmode.PreviewContainer.PreviewContentType;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.globalsearch.LauncherContainer;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;
import com.aliyun.homeshell.hideseat.CustomHideseat;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.icon.BubbleResources;
import com.aliyun.homeshell.icon.IconUtils;
import com.aliyun.homeshell.icon.TitleColorManager;
import com.aliyun.homeshell.lifecenter.CardBridge;
import com.aliyun.homeshell.lifecenter.LifeCenterReceiver;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.screenmanager.ScreenManager;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.homeshell.smartlocate.AppLaunchManager;
import com.aliyun.homeshell.smartlocate.AppLaunchUpdater;
import com.aliyun.homeshell.themeutils.ThemeUtils;
import com.aliyun.homeshell.utils.NotificationPriorityHelper;
import com.aliyun.homeshell.utils.PrivacySpaceHelper;
import com.aliyun.homeshell.utils.ToastManager;
import com.aliyun.homeshell.utils.Utils;
import com.aliyun.homeshell.views.DropDownDialog;
import com.aliyun.homeshell.widgetpage.WidgetPageManager;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.utility.utils.ACA;
import android.hardware.fingerprint.AliFingerprintCallBack;

import com.aliyun.homeshell.searchui.SearchBridge;

import storeaui.widget.ActionSheet;

import com.aliyun.profilemanager.AliUserManager;


/*YUNOS END PB*/
/* YUNOS BEGIN PB */
//##module [Smart Gesture] ##BugID:168585
//##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
//##description: Support Single & Dual Orient Smart Gesture based on proximity
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
/* YUNOS END PB */


import android.os.Build;
/* YUNOS BEGIN */
//##date:2014/02/21  ##author: chao.jiangchao ##BugId: 93596
/* YUNOS END */


/*****************  YUNOS FP import start  ************************/
import com.aliyun.homeshell.utils.fingerPrintCommunication;
import com.aliyun.homeshell.utils.fingerPrintCommunication.fpListener;
/*****************  YUNOS FP import end  ************************/
///ZDP - Added on 2016-08-31 for EasyLauncher@{
import com.mediatek.common.featureoption.XunhuOption;
///@}
///ZDP - Added on 2016-08-10 for anti-remove and anti-update for the pre-installed apps @{
import android.util.PreInstallProtectionUtil;
///@}
/**
 * Default launcher application.
 */
 import android.util.Base64;
public final class Launcher extends Activity
        implements View.OnClickListener, OnLongClickListener, LauncherModel.Callbacks,
                   View.OnTouchListener {
    private Set<View> currentViews = new HashSet<View>();
    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:yangshan.ys##5157204
    // for 3*3 layout
    public static boolean mIsAgedMode = false;
    /*YUNOS END*/
	/* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2016/12/23 ##author:wb-sl248920##9598014
	public static boolean mHasReplaceFolder = false;
	/*YUNOS END*/
	
    static final String TAG = "Launcher";
    static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    static final boolean LOGD = false;
    public static boolean sReloadingForThemeChangeg = false;
    static final boolean PROFILE_STARTUP = false;
    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPLICATION = 6;
    private static final int REQUEST_PICK_SHORTCUT = 7;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;

    private static final float UNINSTALL_ICON_ALPHA = 0.4f;
    /* YUNOS BEGIN */
    // ##date:2015/2/6 ##author:zhanggong.zg ##BugID:5776265
    public static final int REQUEST_HOMESHELL_SETTING = 12;
    private boolean mBackFromHomeShellSetting = false;
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/05/28  ##author: chao.jiangchao ##BugId: 5063796
    private static final int REQUEST_PICK_CLOUDLET = 1000;
    /* YUNOS END */

    public static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

    /*YUNOS BEGIN LH*/
    public static final int SCREEN_COUNT = 12;
    /*YUNOS END*/
    static int DEFAULT_SCREEN = 0;

    private static final String PREFERENCES = "launcher.preferences";
    /* YUNOS BEGIN PB */
    //##module [Smart Gesture] ##BugID:168585
    //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
    //##description: Support Single & Dual Orient Smart Gesture based on proximity

    private Object mSmartGestureObj = null;
    private Class mSmartGestureClass = null;
    private boolean mIsSmartGestureOn = false;
    /* YUNOS END PB */
    // To turn on these properties, type
    // adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String FORCE_ENABLE_ROTATION_PROPERTY = "launcher_force_rotate";
    static final String DUMP_STATE_PROPERTY = "launcher_dump_state";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.aliyun.homeshell.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6277395
    static final String INTENT_ACTION_FIRST_LAUNCH_APP = "com.android.systemui.first.notification.window";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: boolean
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    // Type: long
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";

    /** The different states that Launcher can be in. */
    private enum State {
        NONE, WORKSPACE, APPS_CUSTOMIZE
    };
    private State mState = State.WORKSPACE;
    private TextView mEditModeTips;
    private RelativeLayout mCategoryMode;
    private Button mCategoryModeOk;
    private Button mCategoryModeCancel;
    public static final int APPWIDGET_HOST_ID = 1024;

    private static final Object sLock = new Object();
    private static int sScreen = DEFAULT_SCREEN;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;

    private static int TA_DELAYED = 1500;

    private final BroadcastReceiver mCloseSystemDialogsReceiver
            = new CloseSystemDialogsIntentReceiver();
    /* YUNOS BEGIN */
    // ##date:2013/12/09  ##author: hongxing.whx ##BugId: 60497
    // For wallpaper changed
    private final BroadcastReceiver mWallpaperChangedReceiver
            = new WallpaperChangedIntentReceiver();
    /* YUNOS END */

    private static final String ACTION_ENABLE_CLOUDCARD = "com.yunos.lifecard.action.enableCloudcard";
    private final BroadcastReceiver mEnableCloudCardReceiver = new EnableCloudCardReceiver();

    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

    private LayoutInflater mInflater;
    FolderUtils mFolderUtils = new FolderUtils();
    private UnlockAnimation mUnlockAnimation;
    LauncherMotionHelper mMotionHelper;
    private FlipAnimation mFlipAnim;

    private Workspace mWorkspace;
    private View mLauncherView;
    private DragLayer mDragLayer;
    private DragController mDragController;
    private Folder mFolder;
    private GestureLayer mGestureLayer;

    // divider between hotseat and workspace, should check if empty when using.
    private View mLandscapeDivider;

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private ItemInfo mPendingAddInfo = new ItemInfo();
    private AppWidgetProviderInfo mPendingAddWidgetInfo;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    private FolderInfo mFolderInfo;

    private int mCurrentOrientation;

    private Hotseat mHotseat;
    private Hideseat mHideseat;
    private CustomHideseat mCustomHideseat;
    private Runnable mHideseatOpenCompleteRunnable;
    private View mAllAppsButton;

    private SearchDropTargetBar mSearchDropTargetBar;
    private boolean mAutoAdvanceRunning = false;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    private boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;

    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();

    // Keep track of whether the user has left launcher
    private static boolean sPausedFromUserAction = false;

    private Bundle mSavedInstanceState;

    private LauncherModel mModel;
    private boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mAttached = false;

    private static LocaleConfiguration sLocaleConfiguration = null;

    private static HashMap<Long, FolderInfo> sFolders = new HashMap<Long, FolderInfo>();

    /* avoid frequently pressing downloading icon by wenliang.dwl, BugID:5196859 */
    private HashMap<String, Long> mLastPressTimeOfDownloadingIcon = new HashMap<String, Long>();

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance =
        new HashMap<View, AppWidgetProviderInfo>();

    // Determines how long to wait after a rotation before restoring the screen orientation to
    // match the sensor state.
    private final int mRestoreScreenOrientationDelay = 500;

    private Drawable mWorkspaceBackgroundDrawable;

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();

    public static final ArrayList<String> sDumpLogs = new ArrayList<String>();

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    /* YUNOS BEGIN PB */
    // Desc:BugID:6428097:hide or show nav bar by user
    // ##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc
    // disable the hide button of nav bar in Launcher
    private StatusBarManager mStatusBarManager;
    /* YUNOS END PB */

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<View>();
    static final boolean DEBUG_SHOW_FPS = false;

    private BubbleTextView mWaitingForResume;

    /* YUNOS BEGIN */
    // ##date:2013/11/26 ##author:jun.dong@aliyun-inc.com
    // update menu
    private View mMenu = null;
    /* YUNOS END */
    private View mSortMenu = null;
    /* YUNOS BEGIN */
    // ##date:2013/12/23 ##author:yaodi.yd
    // optimize the uninstalling process
    private DropDownDialog mDeleteDialog = null;
    /* YUNOS END */

    private int mMenuKeyDownCount = 0;

    /*YUNOS BEGIN*/
    //##date:2014/5/22 ##author:zhangqiang.zq
    // smart search
    //##date:2015/2/2 ##author:zhanggong.zg ##BugID:5719824
    // smart search is disabled to reduce initial memory usage
    //private T9DialpadView mT9DialpadView;
    private final boolean mAppSearchMode = false;
    /* YUNOS END */

    /*YUNOS BEGIN*/
    //##date:2014/1/9 ##author:zhangqiang.zq
    //screen edit
    private boolean mScreenEditMode;
    private boolean mOnClickValid = true;
    /*YUNOS END*/

    //added by xiaodong.lxd 2014/11/03
    private int mStatusBarHeight;

    private static ArrayList<PendingAddArguments> sPendingAddList
            = new ArrayList<PendingAddArguments>();

    private static boolean sForceEnableRotation = isPropertyEnabled(FORCE_ENABLE_ROTATION_PROPERTY);

    private PageIndicatorView mIndicatorView = null;
    /* YUNOS BEGIN */
    // ##date:2014/2/18 ##author:yaodi.yd ##BugID:90913
    //private boolean mShouldPlayAnimation;
    //private boolean mIsTopWhenScreenOff;
    private boolean mIsResumed;
    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2014/2/18 ##author:yaodi.yd ##BugID:91808
    //private boolean mIsWakeUpByThreeFingerMode;
    //private boolean mIsWakeUpFromOtherApp;
    /* YUNOS END */

    private Toast mLauncherToast = null;

    //private HashMap<View, PointF> mWorkspaceItemsEndPoints;
    //private int mFolderXOnAnimation;
    //private int mFolderYOnAnimation;

    //BugID:5717551:userTrack for card and and launcher stay time. hao.liuhaolh
    private long mResumeTime;
    private long mFlipStartTime;
    private String mFlipCardPkgName = null;
    private String mFlipCardType = null;

    private LifeCenterReceiver mLifeCenterReceiver;
    private SearchBridge mSearchBridge = null;

    private ArrayList<WeakReference<GadgetView>> gadgetViewList = new ArrayList<WeakReference<GadgetView> >();
    private static final String HOMESHELL_ACTION_CANCEL_BIGCARD = "com.aliyun.homeshell.action.cancel_bigcard";
    int mScreenWidth;
    int mScreenHeight;
    public AppCloneManager mAppCloneManager;
    //YUNOS BEGIN PB
    //## modules(fingerprint):
    //## date:2016/05/11 ##author:xiaolu.txl
    //## BugID:8236970
    private final String FTAG = "AliFingerprintService";
    private AliFingerprintManager mAliFingerprintManager = null;
    private final int FINGERPRINT_OPEN_HIDESEAT = 1000;
    private final int FINGERPRINT_CLOSE_HIDESEAT = 1001;
    private boolean mIsActivateGuide = false;
    private FootprintBase mFootprintBase;
    private ActivateGuideFragment mActivateFragment;
    private String mShipSig = null;
    /*****************     YUNOS FP code start  ************************/
    private AliFingerprintCallBack mAliFingerprintCallBack = new AliFingerprintCallBack(){

        @Override
        public void onAuthenticationAcquired(int acquiredInfo) throws RemoteException {
            // TODO Auto-generated method stub
            super.onAuthenticationAcquired(acquiredInfo);
            Log.d(FTAG, "onAuthenticationAcquired acquiredInfo="+acquiredInfo);
        }

        @Override
        public void onAuthenticationError(int errorCode) throws RemoteException {
            // TODO Auto-generated method stub
            super.onAuthenticationError(errorCode);
            Log.d(FTAG, "onAuthenticationError errorCode="+errorCode);
        }

        @Override
        public void onAuthenticationFailed() throws RemoteException {
            // TODO Auto-generated method stub
            super.onAuthenticationFailed();
            Log.d(FTAG, "onAuthenticationFailed");
        }

        @Override
        public void onAuthenticationHelp() throws RemoteException {
            // TODO Auto-generated method stub
            super.onAuthenticationHelp();
            Log.d(FTAG, "onAuthenticationHelp");
        }

        @Override
        public void onAuthenticationSucceeded() throws RemoteException {
            // TODO Auto-generated method stub
            super.onAuthenticationSucceeded();
            Log.d(FTAG, "onAuthenticationSucceeded");
            mHandler.sendEmptyMessage(FINGERPRINT_OPEN_HIDESEAT);

        }
        @Override
        public void onFingerprintDown() throws RemoteException {
            // TODO Auto-generated method stub
            super.onFingerprintDown();
            mHandler.sendEmptyMessage(FINGERPRINT_CLOSE_HIDESEAT);

        }
    } ;
  //YUNOS END PB
    public fingerPrintCommunication mFpComm;
    private boolean mFingerCloseHideseat;
    private long mFingerCloseHideseatTime;
    private long mClickSortMenuTime = 0;
	///ZDP - Added on 2016-08-31 for EasyLauncher@{
    private boolean mEasyLuancherStarted = false;
	///@}
    fingerPrintCommunication.fpListener mFpListener = new fpListener() {

        @Override
        public void onCatchedEvent(int event, int result) {
            // TODO Auto-generated method stub
            Log.e(TAG, "Launcher fp------------------------onCatchedEvent, event:" + event + ", result:" + result);
            if(event == fingerPrintCommunication.FP_EVENT_VERIFY && result == 0) {
                /*open hideseat if seat is close,  verify success and not finger close since 2s ago*/
                //YUNOS BEGIN PB
                //## modules(Homeshell):
                //## date:2015/09/16 ##author:xiaolu.txl
                //## BugID:6401280
                Log.d(TAG,"isHideseatShowing : "+isHideseatShowing());
                Log.d(TAG,"mWorkspace.getOpenFolder() : "+(mWorkspace.getOpenFolder()==null));
                Log.d(TAG,"isWorkspaceLocked "+isWorkspaceLocked());
                Log.d(TAG,"isInLauncherEditMode "+isInLauncherEditMode());
                Log.d(TAG,"mDragController.isDragging(): "+mDragController.isDragging());
                Log.d(TAG,"isWidgetScreen : "+isWidgetScreen(getCurrentScreen()));
                Log.d(TAG,"isInLeftScreen : "+isInLeftScreen());
                Log.d(TAG,"isGadgetCardShowing:"+isGadgetCardShowing());
                Log.d(TAG,"isInLauncherCategoryMode:"+isInLauncherCategoryMode());
                Log.d(TAG,"freezeValue is : "+HomeShellSetting.getFreezeValue(Launcher.this));
                if (!isHideseatShowing() && (mWorkspace.getOpenFolder() == null) && !isInLauncherEditMode()
                    && !isWorkspaceLocked() && !mDragController.isDragging() && !isWidgetScreen(getCurrentScreen())
                    && !isInLeftScreen() && !isGadgetCardShowing() && !isInLauncherCategoryMode()
                    && !HomeShellSetting.getFreezeValue(Launcher.this)) {//BugID:6442134,6441685
                //YUNOS END PB
                        Log.i(TAG, "open hide seat before");
                    if(!mFingerCloseHideseat || (SystemClock.uptimeMillis() - mFingerCloseHideseatTime > 2000)) {
                        Log.i(TAG, "open hide seat");
                        getGestureLayer().hideseatSwitch(true);
                    }
                    mFingerCloseHideseat = false;
                    Log.i(TAG, "open hide seat end");
                }
            } else if(event == fingerPrintCommunication.FP_EVENT_DETECTED) {
                /*close hideseat if seat is open,  and finger detected*/
                if(isHideseatShowing() && !isWidgetScreen(getCurrentScreen()) && !isInLeftScreen()) {//BugID:6448244
                    Log.i(TAG, "close hide seat");
                    getGestureLayer().hideseatSwitch(false);
                    mFingerCloseHideseat = true;
                    mFingerCloseHideseatTime = SystemClock.uptimeMillis();
                }
            }
        }
    };
    //YUNOS BEGIN PB
    //## modules(fingerprint):
    //## date:2016/05/11 ##author:xiaolu.txl
    //## BugID:8236970
    private void initFingerprint(){
        mAliFingerprintManager = AliFingerprintManager.get();
        if ((null != mAliFingerprintManager) && isHomeShellSeatOpened()) {
            RegisterInfo registerInfo = new RegisterInfo("com.aliyun.homeshell", SystemClock.uptimeMillis(), false, false);
            mAliFingerprintManager.registeredFingerprintCallback(registerInfo, mAliFingerprintCallBack);
        }
    }
    private boolean isHomeShellSeatOpened(){
        return (Settings.Global.getInt(getApplicationContext().getContentResolver(), "fingerprint_homeshell_seat", 0) == 1);
    }
    //YUNOS END PB
    /*****************     YUNOS FP code end  ************************/

    private static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        int screen;
        int cellX;
        int cellY;
    }

    private static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }

    private boolean mSupportLeftScreen = false;
    private CardBridge mCardBridge = null;
    private boolean mSupportLifeCenter = false;
    private boolean mSupportSearchBridge=false;
    private boolean mIsStarted;

 // YUNOS BEGIN PB
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(163418) ##date:2014/08/15
    // ##description: Added support for widget page
    public  WidgetPageManager mWidgetPageManager;
    // YUNOS END PB
    private TitleColorManager mTitleColorManager;

    public boolean isStarted() {
        return mIsStarted;
    }
    public boolean isSupportLifeCenter(){
        return mSupportLifeCenter;
    }

    public boolean isSupportLeftScreen() {
        return mSupportLeftScreen;
    }

    public void setIsSupportLifeCenter(boolean isLifeCenterSupport) {
        mSupportLifeCenter = isLifeCenterSupport;
    }

    @Override
    public View findViewById(int id) {
        if (mLauncherView != null) {
            return mLauncherView.findViewById(id);
        }
        return super.findViewById(id);
    }

    public void moveToDefaultScreen(boolean animate){
        mWorkspace.moveToDefaultScreen(animate);
        stopFlipWithoutAnimation();
    }
    /* YUNOS BEGIN */
    // ##date:2015/01/14 ##author: jun.dongj
    //##suport global search
    public boolean isLifeCenterEnableSearch() {
        boolean enable = true;
        if (mSupportLifeCenter && mCardBridge != null && getCurrentScreen() == 0
                && !mCardBridge.isEnableGlobalPullDown()) {
            enable = false;
        }
        if(mIsActivateGuide){
            enable = false;
        }
        return enable;
    }

    public boolean blockTouchDown() {
        boolean block = false;
        if (mSupportLifeCenter && mCardBridge != null && getCurrentScreen() == 0
                && mCardBridge.isEnableGlobalPullDown()) {
            block = true;
        }
        return block;
    }
    /* YUNOS END */

    ///ZDP - Added on 2016-08-31 for EasyLauncher@{
    private boolean isEasyLauncherEnabled(){
        return XunhuOption.XUNHU_ZDP_EASYLAUNCHER_SUPPORT && android.provider.Settings.System.getInt(getContentResolver(), "is_easyluancher_mode", 0) == 1;
    }
    
	private boolean startEasyLauncher() {
        if (isEasyLauncherEnabled()) {
            try {
                //Thread.sleep(10000);//xiongchao: delay to avoid boot_complete being blocked
                Intent easyintent = new Intent(
                        "com.aliyun.easylauncher.starteasylauncher");
                //easyintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(easyintent);
                /*YunOS BEGIN PB*/
                //##module:HomeShell##author:bingshui.xbs@alibaba-inc.com
                //##BugID:(165989, 167635) ##date:2014-10-31 13:56:53
                //##description:easylauncher isn't showed correctly when back from other apps.
                //finish();
                mEasyLuancherStarted = true;
                /*YUNOS END PB*/
                return true;
            } catch (Exception e) {

            }
        }
        /*YunOS BEGIN PB*/
        //##module:HomeShell##author:bingshui.xbs@alibaba-inc.com
        //##BugID:(165989, 167635) ##date:2014-10-31 13:56:53
        //##description:easylauncher isn't showed correctly when back from other apps.
        mEasyLuancherStarted = false;
        /*YUNOS END PB*/
        return false;
    }
	///@}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        ///ZDP - Added on 2016-08-10 for anti-remove and anti-update for the pre-installed apps @{
        if(!Build.IS_CARRIER_BUILD && XunhuOption.XUNHU_ZDP_PREINSTALL_APP_PROTECT){
            PreInstallProtectionUtil.init(this);
        }
        ///@}
        UserTrackerHelper.init(getApplicationContext());

        super.onCreate(savedInstanceState);
        LauncherApplication app = ((LauncherApplication)getApplication());
        mCurrentOrientation = app.getResources().getConfiguration().orientation;
        mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(),
                Context.MODE_PRIVATE);

        /* YUNOS BEGIN PB */
        // Desc:BugID:6428097:hide or show nav bar by user
        // ##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc
        // disable the hide button of nav bar in Launcher
        mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        /* YUNOS END PB */
        mTitleColorManager = new TitleColorManager(this);

        //open HARDWARE_ACCELERATED
        final Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        /* YUNOS BEGIN PB */
        // Desc:soft key feature
        // BugID:6428097
        // ##Date: Sep 16, 2015 4:44:00 PM ##Author:chao.lc
        //win.setFlags(~WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
        //                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        /* YUNOS END PB */
        win.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                      WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        /*YUNOS BEGIN*/
         //##date:2013/12/8 ##author:xindong.zxd

        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2014/03/19 ##author:jun.dongj@alibaba-inc.com##BugID:102748
        // set statusBar and navigationBar transparent
        final WindowManager.LayoutParams wlp = getWindow().getAttributes();
        @SuppressLint("InlinedApi")
        int flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        wlp.flags |= flags;
        getWindow().setAttributes(wlp);
        /*YUNOS END*/

        //set BubbleTextView's parameters
        BubbleResources.setNeedsReload();
        /*YUNOS END*/
        mModel = app.setLauncher(this);
        mModel.getPackageUpdateTaskQueue().reset();
        mDragController = new DragController(this);
        mInflater = getLayoutInflater();

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing(
                    Environment.getExternalStorageDirectory() + "/launcher");
        }

        checkForLocaleChange();
        checkLifeCenter();
        if (mSupportLifeCenter) {
            DEFAULT_SCREEN = 1;
        }
        mAppCloneManager = AppCloneManager.getInstance();
        mAppCloneManager.updateMaxCloneCount(this);
        setContentView(R.layout.launcher);
        setupViews();

        mLifeCenterReceiver = new LifeCenterReceiver(this);
        mLifeCenterReceiver.register();
        if (mSupportLifeCenter || mSupportLeftScreen) {
            mCardBridge.onCreate();
            mLifeCenterReceiver.setListener(mCardBridge);
            if (mSupportLifeCenter) {

                View blurBackground = mCardBridge.getBlurBackground();
                if (blurBackground != null) {
                    /* YUNOS BEGIN PB */
                    // Desc:soft key feature
                    // BugID:6428097
                    // ##Date: Sep 16, 2015 4:44:00 PM ##Author:chao.lc
                    DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    lp.ignoreInsets = true;
                    mGestureLayer.addView(blurBackground, 0, lp);
                    /* YUNOS END PB */
                }
            }
        }

        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        boolean dataInvaild = ConfigManager.checkAgedModeDataValid(getApplicationContext());
        mIsAgedMode = AgedModeUtil.isAgedMode();
        if(mIsAgedMode) {
            ((FrameLayout.LayoutParams) mIndicatorView.getLayoutParams()).bottomMargin = getResources()
                    .getDimensionPixelSize(R.dimen.page_indicator_bottom_margin_3_3);
            mIndicatorView.getLayoutParams().height = getResources()
                    .getDimensionPixelSize(R.dimen.page_indicator_height_3_3);
        }
        if(dataInvaild) {
            if(mIsAgedMode) {
                app.onAgedModeChanged(true, true, false);
            } else {
                app.onAgedModeChanged(false, true, false);
            }
        }
        /* YUNOS END */
        /* YUNOS END */

        registerContentObservers();

        lockAllApps();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        if (!mRestoring) {
            if (sPausedFromUserAction) {
                // If the user leaves launcher, then we should just load items asynchronously when
                // they return.
                mModel.resetLoadedState(true, true);
                mModel.startLoader(true, -1);
            } else {
                // We only load the page synchronously if the user rotates (or triggers a
                // configuration change) while launcher is in the foreground
                mModel.resetLoadedState(true, true);
                mModel.startLoader(true, mWorkspace.getCurrentPage());
            }
        }

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);
        /* YUNOS BEGIN */
        // ##date:2013/12/09  ##author: hongxing.whx ##BugId: 60497
        // For wallpaper changed
        IntentFilter filter1 = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(mWallpaperChangedReceiver, filter1);
        /* YUNOS END */

        IntentFilter filter2 = new IntentFilter(ACTION_ENABLE_CLOUDCARD);
        registerReceiver(mEnableCloudCardReceiver, filter2);

        // On large interfaces, we want the screen to auto-rotate based on the current orientation
        unlockScreenOrientation(true);
        if (!ConfigManager.isLandOrienSupport()) {
            setRequestedOrientation(Configuration.ORIENTATION_PORTRAIT);
        }

        if (DEBUG_SHOW_FPS) {
            View fpsView = DisplayFrameRate.generateFpsView(this);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, 0, Gravity.LEFT | Gravity.TOP);
            lp.topMargin =  50 * getResources().getDisplayMetrics().densityDpi / 240;
            lp.leftMargin = 50 * getResources().getDisplayMetrics().densityDpi / 240;

            Log.d("testpage", " lp.topMargin " + lp.topMargin);
            Log.d("testpage", " lp.leftMargin " + lp.leftMargin);

            mDragLayer.addView(fpsView, lp);
        }
        Intent intent = new Intent("com.aliyun.systemui.action.CHANGE_STATUSBAR_BACKGROUND");
        sendBroadcast(intent);
        mUnlockAnimation = new UnlockAnimation(this);
        mMotionHelper = new LauncherMotionHelper(this);
        mFlipAnim = new FlipAnimation(this);
        if (fingerPrintCommunication.isAliFingerSupported(this)) {
            mFpComm = new fingerPrintCommunication(this, mFpListener);
        }

        // Update customization drawer _after_ restoring the states
        if (mEditModePreviewContainer != null) {
            /* YUNOS BEGIN */
            // ##date:2014/09/1 ##author:xindong.zxd ##BugId:5215868
            // application not response,execution time consuming operation in
            // asyncTask doInBackground
            new AsyncTask<Void, Void, ArrayList<Object>>() {
                protected ArrayList<Object> doInBackground(Void... params) {
                    ArrayList<Object> list = LauncherModel.getSortedWidgetsAndShortcuts(Launcher.this);
                    return list;
                }
                protected void onPostExecute(ArrayList<Object> list) {
                    mEditModePreviewContainer.onPackagesUpdated(list);
                    if (FeatureUtility.hasNotificationFeature()) {
                        GadgetCardHelper.getInstance(Launcher.this);
                        mEditModePreviewContainer.initNotificationWidgetView(mAppWidgetManager, mAppWidgetHost);
                    }
                };
            }.execute();
            /* YUNOS END */
        }

     // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if (FeatureUtility.hasFullScreenWidget()) {
            mWidgetPageManager = new WidgetPageManager(this);
        }
        // YUNOS END PB
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        getIsSmartGestureOn();
        if (mIsSmartGestureOn)
        {
            try {
                mSmartGestureClass = Class.forName("com.aliyunos.smartgesture.SmartGestureDetector");
                Class<?> listener = Class.forName("com.aliyunos.smartgesture.SmartGestureDetector$OnSmartGestureListener");
                Constructor<?> constructor = mSmartGestureClass.getConstructor(new Class[] {Context.class, listener });
                smartGestureHandler mHandler = new smartGestureHandler();
                Object obj = Proxy.newProxyInstance(this.getClassLoader(), new Class[] { listener }, mHandler);
                mSmartGestureObj = constructor.newInstance(this,obj);
            }catch (Exception e){
                Log.e(TAG, " e = " + e.toString());
                e.printStackTrace();
            }
        }
        /* YUNOS END PB */

        /* YUNOS BEGIN */
        // ## modules(Home Shell): [Category]
        // ## date: 2015/08/31 ## author: wangye.wy
        // ## BugID: 6221911: category on desk top
        mModel.loadToolsFromXml();
        /* YUNOS END */

        //BugID:6452545:LockScreenAnimator error after change language
        if (this != LockScreenAnimator.getInstance(this).getLauncher()) {
            LockScreenAnimator.getInstance(this).reset();
        }
        mScreenWidth = this.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = this.getResources().getDisplayMetrics().heightPixels;
        mFootprintBase = FootprintBase.getInstance();
    }
    /// Xunhu: Add by mjs <<<During use of SmartGesture, let the speed of the desktop switching as same as the speed of the gesture>>> at 2016-09-01 {{{@@@
    private void nextPage(PagedView pageView, int duration) {
        if (pageView == null) {
            return;
        }
        int current = pageView.getCurrentPage();
        int pageCount = pageView.getPageCount();
        if(PagedView.sContinuousHomeShellFeature){
            pageView.snapToPage((current + 1) % pageCount, duration);
        }else{
            if(current + 1 < pageCount){
                pageView.snapToPage(current + 1, duration);
            }
        }
    }
    
    private void prevPage(PagedView pageView, int duration) {
        if (pageView == null) {
            return;
        }
        int current = pageView.getCurrentPage();
        int pageCount = pageView.getPageCount();
        pageView.snapToPage((current - 1 + pageCount) % pageCount , duration);
    }
    ///@@@}}}

    /* YUNOS BEGIN PB */
    //##module [Smart Gesture] ##BugID:168585
    //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
    //##description: Support Single & Dual Orient Smart Gesture based on proximity
    private void getIsSmartGestureOn(){
        String smartOn = "yes";
        if(smartOn.equals(SystemProperties.get("ro.yunos.singlegesture"))
                || smartOn.equals(SystemProperties.get("ro.yunos.dualgesture"))){
            mIsSmartGestureOn = true;
        }
        Log.e(TAG,"getIsSmartGestureOn mIsSmartGestureOn " + mIsSmartGestureOn);
    }
    public class smartGestureHandler implements InvocationHandler{

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String method_name = method.getName();
            if("onPrev".equals(method_name)){
                Log.e(TAG,"InvocationHandler onPrev Rushon -- > Prev page !");
                if (mState == State.WORKSPACE) {
                    UserTrackerHelper.reportSmartSensorEvent();
                    prevPage(mWorkspace);
                }
            }else if("onNext".equals(method_name)){
                Log.e(TAG,"InvocationHandler onNext Rushon -- > Next page !");
                if (mState == State.WORKSPACE) {
                    UserTrackerHelper.reportSmartSensorEvent();
                    nextPage(mWorkspace);
                }
            }
            Log.e(TAG,"InvocationHandler doing callback...");
            return null;
        }

    }
    private void nextPage(PagedView pageView) {
        if (pageView == null) {
            Log.e(TAG, "nextPage: pageView is null");
            return;
        }

        int current = pageView.getCurrentPage();
        int pageCount = pageView.getPageCount();
        //YUNOS BEGIN PB
        //## modules(HomeShell):
        //## date:2015/09/08 ##author:shuoxing.wsx
        //## BugID:6385386:don't scroll to the first scren by smart gesture when desktop is not continuous.
        if(PagedView.sContinuousHomeShellFeature){
            pageView.snapToPage((current + 1) % pageCount);
        }else{
            //YUNOS BEGIN PB
            //## modules(HomeShell):
            //## date:2015/09/24 ##author:shuoxing.wsx
            //## BugID:6457121: the page index must be not larger than page count.
            if(current + 1 < pageCount){
                pageView.snapToPage(current + 1);
            }
            //YUNOS END PB
        }
        //YUNOS END PB
    }

    private void prevPage(PagedView pageView) {
        if (pageView == null) {
            Log.e(TAG, "prevPage: pageView is null");
            return;
        }

        int current = pageView.getCurrentPage();
        int pageCount = pageView.getPageCount();
        pageView.snapToPage((current - 1 + pageCount) % pageCount);
    }
    /* YUNOS END PB */

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int currentOrientation = newConfig.orientation;
        Log.d(TAG, "  sxsexe_test    onConfigurationChanged  currentPage " + mWorkspace.getCurrentPage() + " supportLifeCenter "
                + mSupportLifeCenter + " mSupportLeftScreen " + mSupportLeftScreen + "new orien is:" + currentOrientation
                + " mCurrentOrientation " + mCurrentOrientation);

        if (mCurrentOrientation != currentOrientation) {
            mCurrentOrientation = currentOrientation;
            if (mModel.isLoadingWorkspace()) {
                return;
            }
            new Handler().post(new Runnable() {
                public void run() {
                    runOrienChangedSync();
                }
            });
        } else {
            final View v = getWindow().peekDecorView();
            try {
                if (v != null && v.getWindowToken() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            } catch (RuntimeException e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    private void runOrienChangedSync() {

        Log.d(TAG,
                "sxsexe_pad   runOrienChangedSync  " + ConfigManager.toLogString() + " isLand " + LauncherApplication.isInLandOrientation()
                        + " LauncherModel.getCellCountX() " + LauncherModel.getCellCountX() + " LauncherModel.getCellCountY() "
                        + LauncherModel.getCellCountY());
        if (isHideDeleteDialog()) {
            mDeleteDialog.dismiss();
            reVisibileDraggedItem((ItemInfo) mWorkspace.getDragInfo().cell.getTag());
        }
        exitScreenEditModeWithoutSave();
        if (ConfigManager.isLandOrienSupport()) {
            mModel.transferCoordinateForOrienChanged();
        }

        int childCount = mWorkspace.getChildCount();
        int currentPage = mWorkspace.getCurrentPage();
        if (isInLauncherEditMode() || isInLauncherCategoryMode()) {
            CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(currentPage);
            if (cellLayout != null && (!cellLayout.hasChild() || cellLayout.isFakeChild())) {
                currentPage = 0;
            }
            if (isInLauncherEditMode()) {
                exitLauncherEditMode(false);
            }
            if (isInLauncherCategoryMode()) {
                exitLauncherCategoryMode(false);
            }
            mWorkspace.removeEmptyCellLayoutSync();
            childCount = mWorkspace.getChildCount();
        }

        mDragController.clearDropTarget();
        mDragController.clearDragListener();
        ((LauncherContainer) findViewById(R.id.launcher_container)).unregisterReceiver();
        unregisterReceiver(mFlipAnim);
        mWorkspace.cleanUpAllGadgets();
        LauncherGadgetHelper.cleanUp();
        LauncherAnimUtils.onDestroyActivity();
        LockScreenAnimator.getInstance(this).reset();
        mWidgetsToAdvance.clear();
        mWorkspace.removeAllViews();
        if (mHideseat != null) {
            mHideseat.removeAllViews();
        }
        mHotseat.removeAllViews();
        mDragLayer.removeAllViews();
        mGestureLayer.removeAllViews();

        mMenu = null;
        mDragLayer = null;
        mWorkspace = null;
        mGestureLayer = null;
        mLauncherView = null;
        mWorkspaceLoading = true;
        mIndicatorView = null;
        mSearchDropTargetBar = null;
        mFlipAnim = null;
        mUnlockAnimation = null;
        mScreenManager = null;
        mLandscapeDivider = null;
        mDeleteDialog = null;

        mUnlockAnimation = new UnlockAnimation(this);
        mFlipAnim = new FlipAnimation(this);
        setContentView(R.layout.launcher);
        setupViews();
        mDragLayer.requestFitSystemWindows();// !important need to ask a new
                                             // dispatch of
                                             // fitSystemWindows(Rect)
        mWorkspace.makesureAddScreenIndex(childCount - 1);
        // mWorkspace.snapToPage(currentPage, 0);
        mWorkspace.setCurrentPage(currentPage);
        mModel.startLoader(true, currentPage);
        final View v = getWindow().peekDecorView();
        try {
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
    }

    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    private void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    sLocaleConfiguration = result;
                    checkForLocaleChange(); // recursive, but now with a locale
                                            // configuration
                }
            }.execute();
            return;
        }

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = sLocaleConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = sLocaleConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = sLocaleConfiguration.mnc;
        final int mnc = configuration.mnc;

        boolean localeChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;

            // @@@@@@
            // need notify iconmanager reset

            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() {
                    writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

    private static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }
    /* YUNOS BEGIN */
    // module(HomeShell)
    // ##date:2014/03/27 ##author:yaodi.yd ##BugID:105225
    // it can not enter Screen edit Mode,
    // When the dialog is displayed
    public GestureLayer getGestureLayer() {
        return mGestureLayer;
    }
    /* YUNOS END */

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is
        // possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return (!mModel.isLoadingWorkspace()) && mWorkspace.getFolderBatchOping() == null
        /* YUNOS BEGIN */
        // ## modules(Home Shell): [Category]
        // ## date: 2015/07/30 ## author: wangye.wy
        // ## BugID: 6221911: category on desk top
                && !mLauncherCategoryMode;
        /* YUNOS END */
    }

    static int getScreen() {
        synchronized (sLock) {
            return sScreen;
        }
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and
     * widgets that have a configuration step, this allows the proper animations
     * to run after other transitions.
     */
    private boolean completeAdd(PendingAddArguments args) {

        if (args.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            mWorkspace.makesureAddScreenIndex(args.screen);
        }

        boolean result = false;
        switch (args.requestCode) {
            case REQUEST_PICK_APPLICATION :
                completeAddApplication(args.intent, args.container, args.screen, args.cellX, args.cellY);
                break;
            case REQUEST_PICK_SHORTCUT :
                processShortcut(args.intent);
                break;
            case REQUEST_CREATE_SHORTCUT :
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX, args.cellY);
                result = true;
                break;
            case REQUEST_CREATE_APPWIDGET :
                int appWidgetId = args.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case REQUEST_PICK_WALLPAPER :
                // We just wanted the activity result here so we can clear
                // mWaitingForResult
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a
        // workspace screen,
        // if you turned the screen off and then back while in All Apps,
        // Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this
        // issue
        resetAddInfo();
        return result;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        /* YUNOS BEGIN */
        // ##date:2014/08/12 ##author: xinzheng.lxz
        // move dispatchActivityResult to mLifeCenterHostView ,make launcher
        // don't care native or h5 version.
        if (mSupportLifeCenter && (requestCode > REQUEST_PICK_CLOUDLET)) {
            if (mCardBridge != null)
                mCardBridge.dispatchActivityResult(requestCode, resultCode, data);
            return;
        }
        /* YUNOS END */
        /* YUNOS BEGIN */
        // ##date:2015/2/6 ##author:zhanggong.zg ##BugID:5776265
        // ##date:2015/2/11 ##author:zhanggong.zg ##BugID:5788061
        if (requestCode == REQUEST_HOMESHELL_SETTING) {
            mBackFromHomeShellSetting = true;
        } else {
            mBackFromHomeShellSetting = false;
        }
        /* YUNOS END */
        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null, mPendingAddWidgetInfo);
            }
            return;
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET);
        mWaitingForResult = false;

        // We have special handling for widgets
        if (isWidgetDrop) {
            int appWidgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (appWidgetId < 0) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\" + "widget configuration activity.");
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
            }
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual
        // target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and
        // we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = mPendingAddInfo.container;
            args.screen = mPendingAddInfo.screen;
            args.cellX = mPendingAddInfo.cellX;
            args.cellY = mPendingAddInfo.cellY;
            // BugID:5567652:don't need to check download status
            // if (isWorkspaceLocked()) {
            if (mWorkspaceLoading || mWaitingForResult) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        mDragLayer.clearAnimatedView();
        if (mLauncherEditMode && mEditModePreviewContainer != null) {
            mEditModePreviewContainer.clearAnimationView();
            mDragLayer.invalidate();
        }
    }

    private void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mPendingAddInfo.screen);

        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId, mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container, mPendingAddInfo.screen, layout, null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }

        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout, (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else {
            // The animated view may be null in the case of a rotation during
            // widget configuration
            if (onCompleteRunnable != null) {
                onCompleteRunnable.run();
            }
        }
    }

    public boolean isGadgetCardShowing() {
        return mFlipAnim.isShowing() || mFlipAnim.isWaiting();
    }

    @Override
    protected void onStop() {
        if(mIsActivateGuide){
            super.onStop();
            return;
        }
        mIsStarted = false;
        super.onStop();
		///ZDP - Added on 2016-08-31 for EasyLauncher@{
        if(isEasyLauncherEnabled() && mEasyLuancherStarted){
            return;
        }
		///@}
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/17 ## author: wangye.wy
        // ## BugID: 7930322: stop waiting for onStop()
        mHandler.removeCallbacks(mOnStopWaitingRunnable);
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ## modules(Home Shell): [Category]
        // ## date: 2015/07/30 ## author: wangye.wy
        // ## BugID: 6221911: category on desk top
        if (mLauncherCategoryMode) {
            /* YUNOS BEGIN */
            // ## date: 2016/06/15 ## author: yongxing.lyx
            // ## BugID:8208980:cell layout overlap after power off and on when
            // holding category page.
            if (mWorkspace.isPageMoving()) {
                mWorkspace.runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        mModel.recoverAllIcons(true);
                    }
                });
            } else {
                mModel.recoverAllIcons(true);
            }
            /* YUNOS END */
        } else {
            mEditModeHelper.resetScreenParamsOnExitEditMode();
        }
        /* YUNOS END */

        mMotionHelper.unregister();
        FirstFrameAnimatorHelper.setIsVisible(false);
        /* YUNOS BEGIN */
        // ##date:2014/02/21 ##author:yaodi.yd
        // it's should hide delete dialog on Stop
        // YUNOS BEGIN
        // ##date:2014/10/11 ##author:hongchao.ghc ##BugID:5324440
        if (isHideDeleteDialog()) {
            getSearchBar().hideDropTargetBar(false);
            if (mDeleteDialog != null && mDeleteDialog.getNegetiveButton() != null) {
                mDeleteDialog.getNegetiveButton().performClick();
            }
        }
        // source code to onPause
        // YUNOS END
        mWorkspace.cleanDragItemList();

        if (mFlipAnim.isShowing()) {
            stopFlipWithoutAnimation();
        }

        /* YUNOS BEGIN */
        // ##date:2014/03/03 ##author:zhangqiang.zq
        // BugID:100647 crash when theme changing
        if (mScreenEditMode) {
            exitScreenEditModeWithoutSave();
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##module(homeshell)
        // ##date:2014/03/13 ##author:jun.dongj@alibaba-inc.com##BugID:98875
        // onStop shoud close all open folder
        if (mWorkspace.getOpenFolder() != null && !mWorkspace.getOpenFolder().isEditingName()) {
            closeOpenFolders(false);
        }
        /* YUNOS END */

        if (mIsResumed)
            mUnlockAnimation.finish();
    }

    @Override
    protected void onStart() {
        /* YUNOS BEGIN */
        // ##date:2014/3/12 ##author:yaodi.yd ##BugID:99171 and 98737
        // if the screen is locked when Launcher starting, That suggests it's
        // wake up from an application.
        // then we set mIsWakeUpFromOtherApp to true, so it's will not play
        // animation after this application stoped.
        /*
         * if (isScreenLocked()) { mIsWakeUpFromOtherApp = true; }
         */
        /* YUNOS END */
        super.onStart();

		///ZDP - Added on 2016-08-31 for EasyLauncher@{
        if(isEasyLauncherEnabled()){  
			//startEasyLauncher() will be moved to onResume()
            return;
        }
		///@}
        if (!mLauncherCategoryMode) {
            if (!mWorkspace.isTouchActive() && mWorkspace.isPageMoving()) {
                mWorkspace.runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        exitLauncherEditMode(true);
                    }
                });
            } else if (!mWorkspace.isTouchActive() && !mWorkspace.isPageMoving()) {
                exitLauncherEditMode(false);
            }
        }
        mUnlockAnimation.standby();
        mMotionHelper.register();
        int countX = LauncherModel.getCellCountX();
        int countY = LauncherModel.getCellCountY();
        if (AgedModeUtil.isAgedMode() != mIsAgedMode || !HomeShellSetting.isValidLayout(countX, countY, mIsAgedMode)) { // BugID:6398960
            final Workspace workspace = mWorkspace;
            if (mWorkspace != null) {
                mWorkspace.clearReference();
                int count = workspace.getIconScreenCount();
                int offSet = workspace.getIconScreenHomeIndex();
                for (int i = 0; i < count; i++) {
                    final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i + offSet);
                    layoutParent.removeAllViewsInLayout();
                }
                // clear all celllayouts on entering ageMode
                while (mWorkspace.getIconScreenCount() > 1) {
                    mWorkspace.removeViewAt(1 + offSet);
                }
            }
            if (mWidgetsToAdvance != null) {
                mWidgetsToAdvance.clear();
            }
            if (mHotseat != null) {
                mHotseat.resetLayout();
            }
            if (mHideseat != null) {
                mHideseat.resetLayout();
            }
            Log.d(AgedModeUtil.TAG, "onStart,mIsAgedMode in Launcher is :" + mIsAgedMode
                    + ",different with the state in AgedModeUtil,call onAgedModeChanged:" + !mIsAgedMode);
            mIsAgedMode = AgedModeUtil.isAgedMode();
            ((LauncherApplication) getApplication()).onAgedModeChanged(mIsAgedMode, true, true);
        }
        // FirstFrameAnimatorHelper.setIsVisible(true);
        /* YUNOS BEGIN */
        // ##date:2014/2/17 ##author:yaodi.yd ##BugID:90913
        // set all items invisible
        /*
         * if (mState == State.WORKSPACE && !isInEditScreenMode() // Added for
         * BugID:98018 && !mIsWakeUpFromOtherApp // Added for BugID:99171 and
         * 98737 && (mIsWakeUpByThreeFingerMode || mShouldPlayAnimation)) {
         * Log.d(TAG, "mIsWakeUpByThreeFingerMode="+mIsWakeUpByThreeFingerMode+
         * ",mShouldPlayAnimation=" +mShouldPlayAnimation); Log.d(TAG,
         * "onstart to play unlock animation");
         * getWorkspace().setAllItemsOfCurrentPageVisibility(View.INVISIBLE);
         * getAnimationPlayer().play(new ScreenUnlockedAnimation(this));
         * mShouldPlayAnimation = mIsTopWhenScreenOff = false; }
         */
        /* YUNOS END */
        mIsStarted = true;
        mModel.findAndRemoveExpiredUpdateIcon();
    }

//    Runnable mResumeTracherRunnable = new Runnable() {
//        public void run() {
//            UserTrackerHelper.pageEnter(Launcher.this);
//            UserTrackerHelper.entryPageBegin(UserTrackerMessage.LABEL_LAUNCHER);
//        }
//    };
    @Override
    protected void onResume() {
		///ZDP - Added on 2016-08-31 for EasyLauncher@{
        if(XunhuOption.XUNHU_ZDP_EASYLAUNCHER_SUPPORT && startEasyLauncher()){
            super.onResume();
            return;
        }
		///@}  
        if(mIsActivateGuide){
            super.onResume();
            return;
        }
        // BugID:5695121:userTrack for card and and launcher stay time.
        // hao.liuhaolh
        mResumeTime = SystemClock.uptimeMillis();

//        BugID:6626281:postDelayed cause track data lost sometimes
//        mHandler.postDelayed(mResumeTracherRunnable, TA_DELAYED);
        UserTrackerHelper.pageEnter(Launcher.this);
        UserTrackerHelper.entryPageBegin(UserTrackerMessage.LABEL_LAUNCHER);
        /* YUNOS BEGIN PB */
        // Desc:BugID:6428097:hide or show nav bar by user
        // ##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc
        // disable the hide button of nav bar in Launcher
        mHandler.removeCallbacks(mShowNavbarButtonsRunnable);
        if (SystemProperties.getBoolean("ro.yunos.navbar.hidebutton", false)
                && getResources().getBoolean(R.bool.navbar_disable_hidebutton)) {
            mStatusBarManager.disable(0x04000000);
        }
        /* YUNOS END PB */
        /* YUNOS BEGIN */
        // ##date:2014/3/12 ##author:yaodi.yd ##BugID:99171 and 98737
        // if the screen is locked when Launcher resuming, That suggests it's
        // wake up from an application.
        // then we set mIsWakeUpFromOtherApp to true, so it's will not play
        // animation after this application stoped.
        /*
         * if (isScreenLocked()) { mIsWakeUpFromOtherApp = true; }
         */
        /* YUNOS END */
        /* YUNOS BEGIN */
        // ##date:2014/2/17 ##author:yaodi.yd
        // set all items invisible and play animation
        /*
         * if (mState == State.WORKSPACE && !isInEditScreenMode() // Added for
         * BugID:98018 && !mIsWakeUpFromOtherApp // Added for BugID:99171 and
         * 98737 && (mIsWakeUpByThreeFingerMode || mShouldPlayAnimation)) {
         * Log.d(TAG, "mIsWakeUpByThreeFingerMode="+mIsWakeUpByThreeFingerMode+
         * ",mShouldPlayAnimation=" +mShouldPlayAnimation); Log.d(TAG,
         * "onresume to play unlock animation"); getAnimationPlayer().play(new
         * ScreenUnlockedAnimation(this)); mShouldPlayAnimation =
         * mIsTopWhenScreenOff = false; }
         */
        /* YUNOS END */
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onResume();

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/17 ## author: wangye.wy
        // ## BugID: 7930322: reset flag of waiting for onStop()
        mOnStopWaiting = false;
        /* YUNOS END */

        FancyDrawable.resumeAll();
        GadgetView.resumeAll();
        // BugID:111118:life center need do resume when homeshell resume if in
        // life center page.
        if (mSupportLifeCenter || mSupportLeftScreen) {
            mCardBridge.onResume();
        }

        HomeShellGadgetsRender.getRender().onResume(this);
        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS_CUSTOMIZE) {
            showAllApps(false);
        }
        mOnResumeState = State.NONE;

        // Background was set to gradient in onPause(), restore to black if in
        // all apps.
        setWorkspaceBackground(mState == State.WORKSPACE);

        // Process any items that were added while Launcher was away
        InstallShortcutReceiver.flushInstallQueue(this);

        mPaused = false;
        sPausedFromUserAction = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            mWorkspaceLoading = true;
            mModel.startLoader(true, -1);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }
        if (mOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see
            // waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            mOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                Log.d(TAG, "Time spent processing callbacks in onResume: " + (System.currentTimeMillis() - startTimeCallbacks));
            }
        }

        // Reset the pressed state of icons that were locked in the press state
        // while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            /*
             * mWaitingForResume.setStayPressed(false);
             */
        }
        // It is possible that widgets can receive updates while launcher is not
        // in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the
        // foreground activity
        // (framework issue). On resuming, we ensure that any widgets are
        // inflated for the current
        // orientation.
        getWorkspace().reinflateWidgetsIfNecessary();

        // Again, as with the above scenario, it's possible that one or more of
        // the global icons
        // were updated in the wrong orientation.
        // updateGlobalIcons();
        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        /* YUNOS BEGIN */
        // ##date:2013/11/25 ##author:xiaodong.lxd
        // re-visible the drag item and check empty screen
        // reVisibileWorkspaceItem();
        // reVisibileFolderItem();
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##date:2013/11/25 ##author:zhangqiang.zq
        // for ic
        final Context context = getApplicationContext();
        final SharedPreferences sp = context.getSharedPreferences(DataCollector.PREFERENCES_CONFIG, Context.MODE_PRIVATE);
        DataCollector.getInstance(context).ensureICFileIsExist();
        final long lastCollected = sp.getLong(DataCollector.LAST_DOIC_TIME, 0);
        final long currentTime = System.currentTimeMillis();
        if (0 == lastCollected) {
            DataCollector.getInstance(context).updateSendFlag();
            new Thread() {
                public void run() {
                    try {
                        // delay 10s waiting for the IC service to start when
                        // booted
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (mWorkspace != null) {// YUNOS added by xiaodong.lxd
                                             // #137734
                        UserTrackerHelper.screenStatus(mWorkspace.getIconScreenCount());
                        LauncherModel.sendStatus();
                        UserTrackerHelper.continueStatus(HomeShellSetting.getLoopDesktopMode(getApplicationContext()));
                    }
                }
            }.start();
        } else if (currentTime - lastCollected > DataCollector.SEVEN_DAY_MILLISECONDS) {
            sp.edit().putLong(DataCollector.LAST_DOIC_TIME, currentTime).commit();
            new Thread() {
                public void run() {
                    try {
                        // delay 10s waiting for the IC service to start when
                        // booted
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    final int days = (int) ((currentTime - lastCollected) / (24 * 60 * 60 * 1000));
                    DataCollector.getInstance(context).doIC(context, days);
                    /* YUNOS BEGIN */
                    // ##date:2014/07/24 ##author:hongchao.ghc ##BugID:140373
                    if (mWorkspace != null) {
                        UserTrackerHelper.screenStatus(mWorkspace.getIconScreenCount());
                    }
                    /* YUNOS END */
                    LauncherModel.sendStatus();
                }
            }.start();
        }
        /* YUNOS END */

        ((LauncherApplication) getApplication()).sendDayAppsData();
        ((LauncherApplication) getApplication()).sendConfigurationData();

        /* YUNOS BEGIN */
        // ##date:2014/02/18 ##author:yaodi.yd ##BugID:90913
        mIsResumed = true;
        /* YUNOS END */

        mOnClickValid = true;
        mUnlockAnimation.standby();
        if (FeatureUtility.hasFullScreenWidget()) {
            mWidgetPageManager.onResume();
        }
        //BugID:5193043:download status error after app installed
        if (mModel != null) {
            mModel.checkInstallingState();
        }
        // YUNOS BEGIN
        // ##date:2014/10/11 ##author:hongchao.ghc ##BugID:5324440
        // solve the text input box does not automatically hidden when the
        // applications delete box is showing
        // if you want to keep the input box of operation please be filtered
        final View v = getWindow().peekDecorView();
        try {
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
        // YUNOS END

        if(!Build.YUNOS_CTA_SUPPORT) {
            AppGroupManager.getInstance().reloadAppGroupInfosFromServer();
        }

        meausreStatusBarHeight();

        /* YUNOS BEGIN */
        // ##date:2015/2/6 ##author:zhanggong.zg ##BugID:5776265
        mBackFromHomeShellSetting = false;
        /* YUNOS END */

        if (mFpComm != null && Settings.Global.getInt(getApplicationContext().getContentResolver(), "fingerprint_homeshell_seat", 0) == 1) {
            mFpComm.registerFpListener(fingerPrintCommunication.FP_EVENT_DETECTED | fingerPrintCommunication.FP_EVENT_VERIFY, true);
            mFingerCloseHideseat = false;
            mFingerCloseHideseatTime = 0;
        }


        //YUNOS BEGIN PB
        //## modules(fingerprint):
        //## date:2016/05/11 ##author:xiaolu.txl
        //## BugID:8236970
        initFingerprint();
        //YUNOS END PB
        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
        if (mIsSmartGestureOn)
        {
           if (SystemProperties.getBoolean("persist.sys.ng_launcher", false) && null != mSmartGestureClass) {
       //Log.d(TAG, "Rushon : NextGesture  detect start !!!");
              try {
                  Method start = mSmartGestureClass.getDeclaredMethod("start");
                  start.invoke(mSmartGestureObj);
              }catch (Exception e){
                  Log.e(TAG," Exception " + e.toString());
                  e.printStackTrace();
              }
              // mSmartGestureDetector.start();
           }
        }
        /* YUNOS END PB */

        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(7815078) ##date:2016/1/18
        // ##description: don't resume Atom when flipAnim is showing.
        if (mHotseat != null && !isInLauncherEditMode() && !mFlipAnim.isShowing()) {
            mHotseat.afterShowHotseat();
        }
        // YUNOS END PB
        if(sIsDeferInstallAppEnabled && FootprintBase.ACTIVATE_STATUS_NEED_ACTIVATE == mFootprintBase.getActivateStatus()){
            IPackageManager mPm = IPackageManager.Stub.asInterface(android.os.ServiceManager.getService("package"));
            String[] pkgs = null;
            try{
                pkgs = mPm.getAllDeferredPreloadApps();
            }catch(RemoteException e){
                Log.e(TAG, "getAllDeferredPreloadApps exception:" + e.getMessage());
            }
            Log.d(TAG, "startActivateYunOSService pkgs:" + pkgs);
            if(pkgs != null && pkgs.length != 0){
                Log.d(TAG, "startActivateYunOSService pkgs.length:" + pkgs.length);
                startActivateYunOSService();
            }
        }
    }

    /* YUNOS BEGIN */
    // ##date:2013/11/27 ##author:xiaodong.lxd
    // re-visible the drag item
    protected void reVisibileDraggedItem(ItemInfo info, float alpha) {
        reVisibileDraggedItemEx(info, alpha, isDragToClone());
    }

    protected void reVisibileDraggedItemEx(ItemInfo info, float alpha, boolean isDragToClone) {
        if (mWorkspace == null || info == null) {
            return;
        }
        View cell = mWorkspace.getDragItemFromList(info, true);
        /* YUNOS BEGIN */
        // ## date: 2016/06/15 ## author: yongxing.lyx
        // ## BugID:8490865:view disappear after power off when dropped to clone target.
        if (cell != null && alpha < 1) {
            cell.setAlpha(alpha);
        }
        Log.d(TAG, "sxsexe------------>reVisibileWorkspaceItem info " + info + " cell " + cell);

        if (isContainerFolder(info.container)) {
            FolderInfo folderInfo = sFolders.get(info.container);
            if (folderInfo != null) {
                /* YUNOS BEGIN */
                // ## date: 2016/06/15 ## author: yongxing.lyx
                // ## BugID:8405601:null point exception protection
                // ...
                // ## date: 2016/08/05 ## author: yongxing.lyx
                // ## BugID:8593934:duplicated icons on folder after clone.
                if (info instanceof ShortcutInfo) {
                    folderInfo.add((ShortcutInfo) info);
                } else if (mFolder != null && mFolder.mShortcutInfoCache != null) {
                    folderInfo.add(mFolder.mShortcutInfoCache);
                }
                /* YUNOS END */
            }
        } else if (isContainerHideseat(info.container) && mHideseat != null && cell != null) {
            mHideseat.addInScreen(cell, info.container, info.screen, info.cellX, info.cellY,
                    info.spanX, info.spanY, true);
            /* YUNOS BEGIN */
            // ##date:2014/10/14 ##author:zhanggong.zg ##BugID:5252746
            getHideseat().onDropCompleted(cell, null, false, true);
            /* YUNOS END */
        } else if (isContainerHotseat(info.container) && cell != null) {
            mWorkspace.addInHotseat(cell, info.container, info.screen, info.cellX, info.cellY,
                    info.spanX, info.spanY, info.screen);
            getHotseat().onDrop(false, 0, null, cell, true);
        } else if (isContainerWorkspace(info.container) && cell != null) {
            if (mLauncherEditMode && !isDragToDelete() && !isDragToClone) {
            } else {
                cell.setVisibility(View.VISIBLE);
            }
            if (cell.getParent() != null) {
                CellLayout layout = (CellLayout) cell.getParent().getParent();
                if (layout != null)
                    layout.markCellsAsOccupiedForView(cell);
            }
        }
        /* YUNOS END */
        mWorkspace.mDropTargetView = null;
    }
    /* YUNOS END */

    protected void reVisibileDraggedItem(ItemInfo info) {
        reVisibileDraggedItem(info, 1);
    }

    public static FolderInfo findFolderInfo(long container) {
        return sFolders.get(container);
    }

    public boolean isPaused() {
        return mPaused;
    }

    @Override
    protected void onPause() {
        /* YUNOS BEGIN */
        // ##date:2014/06/03 ##author:yangshan.ys
        // batch operations to the icons in folder
        // close the folder because the onPause will be called when press home
        // button
        /* YUNOS END */
		///ZDP - Added on 2016-08-31 for EasyLauncher@{
        if(isEasyLauncherEnabled() && mEasyLuancherStarted){
            super.onPause();
            return;
        }
		///@}     
        if(mIsActivateGuide){
            super.onPause();
            return;
        }
        if (mFpComm != null) {
            mFpComm.unregisterFpListener();
        }
        //YUNOS BEGIN PB
        //## modules(fingerprint):
        //## date:2016/05/11 ##author:xiaolu.txl
        //## BugID:8236970
        if (null != mAliFingerprintManager && mAliFingerprintManager.isHardwareDetected()) {
            RegisterInfo registerInfo = new RegisterInfo("com.aliyun.homeshell", SystemClock.uptimeMillis(), false, false);
            mAliFingerprintManager.unRegisteredFingerprintCallback(registerInfo);
        }
       //YUNOS END PB
        // by wenliang.dwl, retore icons to workspace because
        // onWindowFocusChanged() is called after onPause()
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/28 ## author: wangye.wy
        // ## BugID: 8035785: restore delayed for waiting for lock screen enter
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LockScreenAnimator.getInstance(Launcher.this).restoreIfNeeded();
            }
        }, 300);
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ##date:2014/3/12 ##author:yaodi.yd ##BugID:99171 and 98737
        // if the screen is locked when Launcher pausing, That suggests it's
        // wake up from an application.
        // then we set mIsWakeUpFromOtherApp to true, so it's will not play
        // animation after this application stoped.
        /*
         * if (isScreenLocked()) { mIsWakeUpFromOtherApp = true;
         * getWorkspace().setAllItemsOfCurrentPageVisibility(View.VISIBLE); }
         */
        /* YUNOS END */
        // NOTE: We want all transitions from launcher to act as if the
        // wallpaper were enabled
        // to be consistent. So re-enable the flag here, and we will re-disable
        // it as necessary
        // when Launcher resumes and we are still in AllApps.

//BugID:6626281:postDelayed cause track data lost sometimes
//        mHandler.postDelayed(new Runnable() {
//            public void run() {
        UserTrackerHelper.pageLeave(Launcher.this);
        UserTrackerHelper.entryPageEnd(UserTrackerMessage.LABEL_LAUNCHER);
//            }
//        }, TA_DELAYED);
        updateWallpaperVisibility(true);

        /* YUNOS BEGIN PB */
        //##module [Smart Gesture] ##BugID:168585
        //##date:2014-11-04  ##author:xiongchao.lxc@alibaba-inc.com
        //##description: Support Single & Dual Orient Smart Gesture based on proximity
       if (mIsSmartGestureOn)
       {
          if(SystemProperties.getBoolean("persist.sys.ng_launcher", false) && null != mSmartGestureClass){
              /*if(mSmartGestureDetector!=null){
                   mSmartGestureDetector.stop();
               }*/
              try {
                  Method stop = mSmartGestureClass.getDeclaredMethod("stop");
                  stop.invoke(mSmartGestureObj);
              }catch (Exception e){
                  Log.e(TAG," Exception " + e.toString());
                  e.printStackTrace();
              }
           }
       }
       /* YUNOS END PB */
        super.onPause();
        FancyDrawable.pauseAll();
        GadgetView.pauseAll();
        // BugID:111118:life center need do pause when homeshell pause if in
        // life center page.
        if (mSupportLifeCenter || mSupportLeftScreen) {
            if(mCardBridge != null){
                mCardBridge.onPause();
            }
        }
        if (isInLauncherCategoryMode()) {
            exitLauncherCategoryMode(false);
            mModel.recoverAllIcons(false);
        }
        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
        if (!isWorkspaceLocked() && !isDragToDelete()) {
            getWorkspace().checkAndRemoveEmptyCell();
        }
        mWorkspace.cancelFlingDropDownAnimation();

        /* YUNOS BEGIN */
        // ##date:2014/02/18 ##author:yaodi.yd ##BugID:90913
        // mIsWakeUpByThreeFingerMode = mIsResumed = false;
        mIsResumed = false;
        /* YUNOS END */

        /* YUNOS BEGIN added by xiaodong.lxd for push to talk */
        if (CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
            CheckVoiceCommandPressHelper.getInstance().forceDismissVoiceCommand();
        }
        /* YUNOS END */
        /* YUNOS BEGIN added by xiaodong.lxd #99812 */
        getHotseat().onPause();
        /* YUNOS END */

        HomeShellGadgetsRender.getRender().onPause(this);
        /* YUNOS BEGIN */
        // ##module(Hideseat)
        // ##date:2014/3/27 ##author:wenliang.dwl@aliyun-inc.com ##BugId:105240
        // close hideseat when lock screen
        if( isHideseatShowing() ){
            hideHideseat(false);
        }
        /* YUNOS END */

        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if (FeatureUtility.hasFullScreenWidget()) {
            mWidgetPageManager.onPause();
        }
        // YUNOS END PB

        //BugID:5193043:download status error after app installed
        if (mModel != null) {
            mModel.stopCheckInstallingState();
        }

        // YUNOS BEGIN
        // ##date:2014/10/11 ##author:hongchao.ghc ##BugID:5324440
        //delete code,move to onStop
        // YUNOS END
        /* YUNOS BEGIN PB */
        // Desc:BugID:6428097:hide or show nav bar by user
        // ##Date: Oct 21, 2015 3:18:54 PM ##Author:chao.lc
        // disable the hide button of nav bar in Launcher
        mHandler.postDelayed(mShowNavbarButtonsRunnable, 200);
        /* YUNOS END PB */

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/17 ## author: wangye.wy
        // ## BugID: 7930322: start waiting for onStop()
        mOnStopWaiting = false;
        mHandler.postDelayed(mOnStopWaitingRunnable, 500);
        /* YUNOS END */
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        mModel.stopLoader();
        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open. So don't bother
    /*
     * @Override public void onWindowFocusChanged(boolean hasFocus) {
     * super.onWindowFocusChanged(hasFocus);
     *
     * if (hasFocus) { final InputMethodManager inputManager =
     * (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
     * WindowManager.LayoutParams lp = getWindow().getAttributes();
     * inputManager.hideSoftInputFromWindow(lp.token, 0, new
     * android.os.ResultReceiver(new android.os.Handler()) { protected void
     * onReceiveResult(int resultCode, Bundle resultData) { Log.d(TAG,
     * "ResultReceiver got resultCode=" + resultCode); } }); Log.d(TAG,
     * "called hideSoftInputFromWindow from onWindowFocusChanged"); } }
     */

    // commented by chenjian temporarily, you can open it if needed,2015/07/06
    /*
     * private boolean acceptFilter() { final InputMethodManager inputManager =
     * (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
     * return !inputManager.isFullscreenMode(); }
     */

    private void closeOpenFolders(){
        closeOpenFolders(true);
    }
    private void closeOpenFolders(boolean anim){
        if(mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            /*YUNOS BEGIN*/
            //##module(homeshell)
            //##date:2014/03/13 ##author:jun.dongj@alibaba-inc.com##BugID:98875
            //soft keyboard don't disappear
            if (openFolder!=null) {
                openFolder.dismissEditingName();
            /* YUNOS END */
                /* YUNOS BEGIN */
                // ##date:2014/3/10 ##author:yaodi.yd
//                closeFolder();
                closeFolderWithoutExpandAnimation(anim);
                /* YUNOS END */
            }
        }
    }
    @Override
    public boolean onKeyUp(int keyCode,KeyEvent event){
        if(mIsActivateGuide){
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mModel.isInProcess()) {
                return true;
            }

            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncherCategoryMode) {
                return true;
            }
            /* YUNOS END */

            /* YUNOS BEGIN */
            //## modules(Home Shell)
            //## date: 2016/01/14 ## author: wangye.wy
            //## BugID: 7803026: return during animation of lock screen
            if (LockScreenAnimator.getInstance(this).isRuning()) {
                return true;
            }
            /* YUNOS END */

            /* YUNOS BEGIN */
            // ##date:2014/02/21  ##author: chao.jiangchao ##BugId: 93596
            try {
                if (mSupportLifeCenter) {
                    if (getCurrentScreen() == 0) {
                        Log.d(TAG, "Menu, in lifecenter");
                        /* YUNOS BEGIN */
                        // ##module(homeshell)
                        // ##date:2014/03/17 ##author:
                        // hongxing.whx##BugID:101179
                        // long press menu key ,dont show menu
                        mMenuKeyDownCount--;
                        if(mMenuKeyDownCount>0){
                            Log.d(TAG, "in lifecenter, long press menu key");
                            mMenuKeyDownCount = 0;
                            return super.onKeyUp(keyCode, event);
                        }
                        /*YUNOS END*/
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "life center error", e);
            }
            /* YUNOS END */

            /*YUNOS BEGIN*/
            //##module(homeshell)
            //##date:2013/12/05 ##author:jun.dongj@alibaba-inc.com##BugID:69159
            //long press menu key ,show menu
            Log.d(TAG, "Menu,mMenuKeyDownCount = " + mMenuKeyDownCount);
            mMenuKeyDownCount--;
            if(mMenuKeyDownCount>0){
                mMenuKeyDownCount = 0;
                return super.onKeyUp(keyCode, event);
            }
            /*YUNOS END*/
            if (isSearchMode()) {
                return true;
            }
            // added by xiaodong.lxd #5189063 : exit screen edit mode when open
            // menu
            if (mScreenEditMode) {
                exitScreenEditMode(false);
            }
            if(isHideseatShowing()) {
                hideHideseat(false);
            }
            //BugID:5614018  xiaodong.lxd  2014-12-02
            if(mState == State.APPS_CUSTOMIZE) {
                showWorkspace(true);
            }
            if( mFlipAnim.isShowing() ){
                stopFlipWithoutAnimation();
            }

            if (CheckVoiceCommandPressHelper.isPushTalkCanUse() && CheckVoiceCommandPressHelper.getInstance().isVoiceUIShown()) {
                CheckVoiceCommandPressHelper.getInstance().forceDismissVoiceCommand();
            }

            if(mLauncherEditMode) {
                // do nothing
            } else {
                CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
                if (cellLayout.isWidgetPage()) {
                    return true;
                }
                Folder openFolder = mWorkspace.getOpenFolder();
                // BugID : 8336097
                if (!mDragController.isDragging()) {
                    getGestureLayer().setTouchEnabled(false);
                }
                int timeout = 0;
                if (openFolder != null) {
                    timeout = FolderUtils.getFolderCloseDuration();
                    closeOpenFolders();
                }
                if (HomeShellSetting.getFreezeValue(Launcher.this)) {
                    if (mLauncherToast == null) {
                        mLauncherToast = Toast.makeText(Launcher.this, R.string.aged_freeze_homeshell_toast, Toast.LENGTH_SHORT);
                    } else {
                        mLauncherToast.setText(R.string.aged_freeze_homeshell_toast);
                    }
                    mLauncherToast.show();
                    getGestureLayer().setTouchEnabled(true);
                } else {
                    postRunnableToMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isLeftScreen(mWorkspace.getCurrentPage())) {
                                getGestureLayer().setTouchEnabled(true);
                                return;
                            }
                            enterLauncherEditMode();
                            getGestureLayer().setTouchEnabled(true);
                            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU);
                        }
                    }, timeout + 50);
                }
            }

            return true;
            /*YUNOS BEGIN*/
            //##date:2014/1/9 ##author:zhangqiang.zq
            //screen edit
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mScreenEditMode) {
                exitScreenEditMode(true);
                return true;
            }

            /*YUNOS END*/

            /*YUNOS BEGIN*/
            //##date:2014/6/4 ##author:zhangqiang.zq
            // smart search
            if (mAppSearchMode) {
                exitSearchMode();
                return true;
            }
            /*YUNOS END*/

            if (mModel.isInProcess()) {
                return true;
            }

            /* YUNOS BEGIN */
            //## modules(Home Shell): [Category]
            //## date: 2015/07/30 ## author: wangye.wy
            //## BugID: 6221911: category on desk top
            if (mLauncherCategoryMode) {
                if(mWorkspace.isPageMoving()) {
                    mWorkspace.runOnPageStopMoving(new Runnable() {
                        @Override
                        public void run() {
                            mModel.recoverAllIcons(false);
                        }
                    });
                } else {
                    mModel.recoverAllIcons(false);
                }
                return true;
            }
            /* YUNOS END */
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mIsActivateGuide){
            return true;
        }
        if ((isLeftScreenOpened() || isInLeftScreen()) && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (mCardBridge.dispatchKeyEvent(event)) {
                return true;
            }
        }

        boolean handled = super.onKeyDown(keyCode, event);
        /*YUNOS BEGIN*/
        //##module(homeshell)
        //##date:2013/12/05 ##author:jun.dongj@alibaba-inc.com##BugID:69159
        //long press menu key ,show menu
        if(event.getKeyCode() == KeyEvent.KEYCODE_MENU){
            mMenuKeyDownCount++;
        }
        /*YUNOS END*/
        return handled;
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS_CUSTOMIZE) {
            mOnResumeState = State.APPS_CUSTOMIZE;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentPage(currentScreen);
        }

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final int pendingAddScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screen = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddWidgetInfo = savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mWaitingForResult = true;
            mRestoring = true;
        }


        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = mModel.getFolderById(this, sFolders, id);
            mRestoring = true;
        }

        /*
         * commented by xiaodong.lxd // Restore the AppsCustomize tab if
         * (mAppsCustomizeTabHost != null) { String curTab =
         * savedState.getString("apps_customize_currentTab"); if (curTab !=
         * null) { mAppsCustomizeTabHost.setContentTypeImmediate(
         * mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
         * mAppsCustomizeContent.loadAssociatedPages(
         * mAppsCustomizeContent.getCurrentPage()); }
         *
         * int currentIndex = savedState.getInt("apps_customize_currentIndex");
         * mAppsCustomizeContent.restorePageForIndex(currentIndex); }
         */
    }

    /* YUNOS BEGIN lxd#134902 calculate the hideseat position */
    private void positionHideseat() {
        if(mCustomHideseat == null){
            return;
        }
        android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) mCustomHideseat.getLayoutParams();
        lp.height = getCustomHideseat().getCustomeHideseatHeight();
        // ##date:2014/11/12 ##author:zhanggong.zg ##BugID:5444810
        lp.bottomMargin = calcHideseatBottomMargin();
        Log.d(TAG, "positionHideseat: bottomMargin=" + lp.bottomMargin);
    }
    /* YUNOS END */

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;

        if (mLauncherView == null) {
            mLauncherView = findViewById(R.id.launcher);
        }

        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        mIndicatorView = (PageIndicatorView) findViewById(R.id.pageindicator_view);
        mGestureLayer = (GestureLayer) findViewById(R.id.gesture_layer);
        mEditModeTips = (TextView) findViewById(R.id.edit_mode_tips);
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
            Resources resource = getResources();
            int emTipsMarginTop = resource.getDimensionPixelSize(R.dimen.em_tips_globalSearch_margin_top);
            /* YUNOS BEGIN */
            // ##date: 2016/08/11 ## author: yongxing.lyx
            // ##BugID:8699757:search bar overlap with icons in phone with navbar.
            if (hasNavigationBar()) {
                emTipsMarginTop -= getNavigationBarHeight() / 2;
            }
            /* YUNOS BEGIN */
            mEditModeTips.setTranslationY(emTipsMarginTop);
        }
        mCategoryMode = (RelativeLayout) findViewById(R.id.category_mode);
        mCategoryModeOk = (Button) findViewById(R.id.category_mode_ok);
        mCategoryModeCancel = (Button) findViewById(R.id.category_mode_cancel);
        /* YUNOS BEGIN PB */
        // Desc:soft key feature
        // BugID:6428097
        // ##Date: Sep 16, 2015 4:44:00 PM ##Author:chao.lc
        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        /* YUNOS END PB */
        mWorkspaceBackgroundDrawable = getResources().getDrawable(R.drawable.workspace_bg);

        // Setup the drag layer
        mDragLayer.setup(this, dragController);

        // Setup the gesture layer
        mGestureLayer.setup(this,dragController);
        /* YUNOS BEGIN */
        // ##date:2014/01/27 ##author:yaodi.yd
        // for fling icon
        mGestureLayer.initFlingParams(this);
        /* YUNOS END */

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setup(this);
        }

        // setup hideseat
        if (Hideseat.isHideseatEnabled()) {
            ViewStub stub = (ViewStub) findViewById(R.id.hideseat_stub);
            mCustomHideseat = (CustomHideseat) stub.inflate();
            mHideseat = (Hideseat) findViewById(R.id.hideseat);
            mHideseat.setup(this);
        }

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);

        dragController.addDragListener(mWorkspace);
        if(mHideseat != null){
            dragController.addDragListener(mHideseat);
        }

        // Get the search/delete bar
        mSearchDropTargetBar = (SearchDropTargetBar) mDragLayer.findViewById(R.id.qsb_bar);

        // Setup the drag controller (drop targets have to be added in reverse
        // order in priority)
        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
        }
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);

        /* YUNOS BEGIN added by xiaodong.lxd for push to talk */
        CheckVoiceCommandPressHelper.getInstance().setup(this);
        /* YUNOS END */

        if (mDeleteDialog == null) {
            mDeleteDialog = new DropDownDialog(this);
        }
        if (mSupportSearchBridge) {
            if(mSupportLifeCenter){
            mCardBridge = new CardBridge(this);
            mLifeCenterCellLayout = (CellLayout) mWorkspace.getChildAt(CardBridge.LEFT_SCREEN_INDEX);
            mLifeCenterCellLayout.setIsLeftPage(true);
            FrameLayout root = new FrameLayout(this);
            if (mCardBridge.getRootView() != null) {
                root.addView(mCardBridge.getRootView());
                mCardBridge.setHostRootView(root);
                mLifeCenterCellLayout.addView(root);
            }}
           // final Context context = getApplicationContext();
            if (SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
                if(mSearchBridge == null){
                     mSearchBridge = new SearchBridge(this);
                }
                View v = mSearchBridge.getRootView();
                if (v != null) {
                   FrameLayout globalSearchBox = (FrameLayout)findViewById(R.id.GlobalSearch);
                   if(v.getParent()!=null) {
                        ((ViewGroup)v.getParent()).removeView(v);
                   }
                   if (globalSearchBox != null ) {
                       globalSearchBox.addView(v);
                    }
                    else{
                        Log.w(TAG, "not find global searchBox");
                    }
                   if(isInLauncherEditMode() || AgedModeUtil.isAgedMode()){
                        setGlobalSearchVisibility(View.GONE);
                   }
                 }
                mDragLayer.invalidate();

            }
        } else {
            if (mSupportLeftScreen) {
                mCardBridge = new CardBridge(this);
                FrameLayout root = new FrameLayout(this);
                if (mCardBridge.getRootView() != null) {
                    int W = mCardBridge.getCardContainerWidth();
                    root.addView(mCardBridge.getRootView(), W, LayoutParams.MATCH_PARENT);
                    mCardBridge.setHostRootView(root);
                    root.setVisibility(View.GONE);
                    root.setTranslationX(-W);

                    ViewGroup laucherContainer = (ViewGroup) mLauncherView.findViewById(R.id.launcher_container);
                    laucherContainer.addView(root, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                }
            }
        }
        mEditModePreviewContainer = (PreviewContainer) findViewById(R.id.editmode_container);
        mEditModeHelper.setup(this, mEditModePreviewContainer);
        if (mEditModePreviewContainer != null) {
            mEditModePreviewContainer.setup(this, dragController);
        }

        if (ConfigManager.isLandOrienSupport()) {
            boolean isPort = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            mIndicatorView.setNeedLine(isPort);
            if (isPort) {
                mLandscapeDivider = null;
            } else {
                mLandscapeDivider = new TextView(this);
                LayoutParams lp = new LayoutParams(1, LayoutParams.MATCH_PARENT);
                mLandscapeDivider.setBackgroundColor(0x10ffffff);
                lp.leftMargin = this.getResources().getDimensionPixelSize(R.dimen.land_divider_margin_left);
                lp.bottomMargin = this.getResources().getDimensionPixelSize(R.dimen.land_divider_margin_bottom);
                lp.topMargin = this.getResources().getDimensionPixelSize(R.dimen.land_divider_margin_top);
                mDragLayer.addView(mLandscapeDivider, lp);
            }
        }
        /* YUNOS BEGIN */
        // ##date:2016/07/19 ##author:yongxing.lyx
        // BugID:8576038:charge statusbar color when changed wallpaper.
        updateDisplayStyle(false);
        /* YUNOS END */
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info
     *            The data structure describing the shortcut.
     *
     * @return A View inflated from R.layout.application.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified
     * resource.
     *
     * @param layoutResId
     *            The id of the XML layout used to create the shortcut.
     * @param parent
     *            The group the shortcut belongs to.
     * @param info
     *            The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(layoutResId, parent, false);
        favorite.setup(this);
        BubbleController.applyToView(info, favorite);
        favorite.setOnClickListener(this);
        BubbleController.updateView(favorite);

        return favorite;
    }

    /**
     * Add an application shortcut to the workspace.
     *
     * @param data
     *            The intent describing the application.
     * @param cellInfo
     *            The position on screen where to create the shortcut.
     */
    void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        final int[] cellXY = mTmpAddItemCellCoordinates;
        final CellLayout layout = getCellLayout(container, screen);

        // First we check if we already know the exact location where we want to
        // add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, 1, 1)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        final ShortcutInfo info = mModel.getShortcutInfo(getPackageManager(), data, this);

        if (info != null) {
            info.setActivity(data.getComponent(), Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.container = ItemInfo.NO_ID;
            mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1], isWorkspaceLocked(), cellX, cellY);
        } else {
            Log.e(TAG, "Couldn't find ActivityInfo for selected application: " + data);
        }
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data
     *            The intent describing the shortcut.
     * @param cellInfo
     *            The position on screen where to create the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, int screen, int cellX, int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);

        boolean foundCellSpan = false;

        ShortcutInfo info = mModel.infoFromShortcutIntent(this, data, null);
        if (info == null) {
            return;
        }
        /* YUNOS BEGIN */
        // ##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (!HomeShellGadgetsRender.isHomeShellGadgets(info)) {
            info.isNew = 1;
        } else {
            info.isNew = 0;
        }
        Drawable orgIcon = info.mIcon;
        Drawable themeIcon = getIconManager().buildUnifiedIcon(orgIcon);
        info.setIcon(themeIcon);
        /* YUNOS END */
        final View view = createShortcut(info);
        if (mEditModePreviewContainer != null) {
            mEditModePreviewContainer.setDragSrcView(view);
        }

        // First we check if we already know the exact location where we want to
        // add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing
            // folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0, true, null, null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject, true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (ConfigManager.isLandOrienSupport() && foundCellSpan) {
            boolean isLand = LauncherApplication.isInLandOrientation();
            int acellX = LauncherApplication.isInLandOrientation() ? info.cellXPort : info.cellXLand;
            int acellY = LauncherApplication.isInLandOrientation() ? info.cellYPort : info.cellYLand;
            ScreenPosition pos = null;
            if (acellX == -1 || acellY == -1) {
                info.cellX = cellXY[0];
                info.cellY = cellXY[1];
                // find pos in another orientation
                pos = LauncherModel.findEmptyCellAndOccupy(screen, 1, 1, !isLand);
                if (pos == null) {
                    foundCellSpan = false;
                } else {
                    if (isLand) {
                        info.cellXLand = info.cellX;
                        info.cellYLand = info.cellY;
                        info.cellXPort = pos.xPort;
                        info.cellYPort = pos.yPort;
                    } else {
                        info.cellXPort = info.cellX;
                        info.cellYPort = info.cellY;
                        info.cellXLand = pos.xLand;
                        info.cellYLand = pos.yLand;
                    }
                }
            }
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        /* YUNOS BEGIN */
        // ##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        info.setIcon(orgIcon);
        /* YUNOS END */
        LauncherModel.addItemToDatabase(this, info, container, screen, cellXY[0], cellXY[1], false);

        info.setIcon(themeIcon);
        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, isWorkspaceLocked());
            Map<String, String> param = new HashMap<String, String>();
            param.put("widget", info.title == null ? "" : info.title.toString());
            param.put("container", String.valueOf(container));
            param.put("screen", String.valueOf(screen));
            param.put("cellX", String.valueOf(cellXY[0]));
            param.put("cellY", String.valueOf(cellXY[1]));
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WIDGET_SELECT, param);
        }
    }

    static int[] getSpanForWidget(Context context, ComponentName component, int minWidth, int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
        // We want to account for the extra amount of padding that we are adding
        // to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null, LauncherModel.getCellCountX(),
                LauncherModel.getCellCountY());
    }

    public static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    public static int[] getSpanForWidgetCustomCountXY(Context context, LauncherAppWidgetInfo appWidgetInfo, int countX, int countY) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, appWidgetInfo.providerName, null);
        // We want to account for the extra amount of padding that we are adding
        // to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = appWidgetInfo.minWidth + padding.left + padding.right;
        int requiredHeight = appWidgetInfo.minHeight + padding.top + padding.bottom;
        return CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null, countX, countY);
    }

    public static int[] getMinSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    // static int[] getSpanForWidget(Context context, PendingAddWidgetInfo info)
    // {
    // return getSpanForWidget(context, info.componentName, info.minWidth,
    // info.minHeight);
    // }
    //
    // static int[] getMinSpanForWidget(Context context, PendingAddWidgetInfo
    // info) {
    // return getSpanForWidget(context, info.componentName, info.minResizeWidth,
    // info.minResizeHeight);
    // }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId
     *            The app widget id
     * @param cellInfo
     *            The position on screen where to create the widget.
     */
    private void completeAddAppWidget(final int appWidgetId, long container, int screen, AppWidgetHostView hostView,
            AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            /* YUNOS BEGIN */
            // ##date:2014/1/3 ##author:xindong.zxd
            // analysis code of getAppWidgetInfo() function, maybe return null
            if (appWidgetInfo == null) {
                return;
            }
            /* YUNOS END */
        }

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(container, screen);

        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this
        // really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {
            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null && layout != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0], spanXY[1], cellXY,
                    finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = (result != null);
        } else if (layout != null) {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        }

        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                // Deleting an app widget ID is a void call but writes to disk
                // before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        getWorkspace().checkAndRemoveEmptyCell();

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = mPendingAddInfo.minSpanY;
        launcherInfo.cellX = cellXY[0];
        launcherInfo.cellY = cellXY[1];
        launcherInfo.minWidth = appWidgetInfo.minWidth;
        launcherInfo.minHeight = appWidgetInfo.minHeight;

        if (ConfigManager.isLandOrienSupport()) {
            boolean isLand = LauncherApplication.isInLandOrientation();
            ScreenPosition pos = LauncherModel.findEmptyCellAndOccupy(screen, launcherInfo.spanX, launcherInfo.spanY, !isLand);
            if (pos == null) {
                foundCellSpan = false;
                showOutOfSpaceMessage(isHotseatLayout(layout));
            } else {
                if (isLand) {
                    launcherInfo.cellXLand = launcherInfo.cellX;
                    launcherInfo.cellYLand = launcherInfo.cellY;
                    launcherInfo.cellXPort = pos.xPort;
                    launcherInfo.cellYPort = pos.yPort;
                } else {
                    launcherInfo.cellXPort = launcherInfo.cellX;
                    launcherInfo.cellYPort = launcherInfo.cellY;
                    launcherInfo.cellXLand = pos.xLand;
                    launcherInfo.cellYLand = pos.yLand;
                }
            }
        }

        LauncherModel.addItemToDatabase(this, launcherInfo, container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and
                // instantiated
                launcherInfo.hostView = hostView;
            }
            if (mEditModePreviewContainer != null) {
                mEditModePreviewContainer.setDragSrcView(launcherInfo.hostView);
            }
            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.notifyWidgetSizeChanged(this);

            mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1], launcherInfo.spanX, launcherInfo.spanY,
                    isWorkspaceLocked());
            Map<String, String> param = new HashMap<String, String>();
            param.put("widget", appWidgetInfo.label);
            param.put("container", String.valueOf(container));
            param.put("screen", String.valueOf(screen));
            param.put("cellX", String.valueOf(cellXY[0]));
            param.put("cellY", String.valueOf(cellXY[1]));
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WIDGET_SELECT, param);

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (getCurrentScreen() == 0 && mSupportLifeCenter) // lifecard does
                                                               // not need
                                                               // animation;
                return;
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if(mIsActivateGuide){
                    return;
                }
                /* YUNOS BEGIN */
                // ## modules(Home Shell)
                // ## date: 2016/03/17 ## author: wangye.wy
                // ## BugID: 7930322: no unlock animation when waiting for onStop()
                if (mOnStopWaiting) {
                    mOnStopWaiting = false;
                } else {
                    mUnlockAnimation.screenOff();
                }
                /* YUNOS END */
                // mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateRunning();

                // (by wenliang.dwl) close big card when lock screen
                if (mFlipAnim.isShowing()) {
                    stopFlipWithoutAnimation();
                }

                // Reset AllApps to its initial state only if we are not in the
                // middle of
                // processing a multi-step drop
                /* YUNOS BEGIN */
                // ##date:2013/11/25 ##author:xiaodong.lxd
                // when screen off, no need to show workspace again
                /*
                 * if (mAppsCustomizeTabHost != null &&
                 * mPendingAddInfo.container == ItemInfo.NO_ID) {
                 * mAppsCustomizeTabHost.reset(); showWorkspace(false); }
                 */
                /* YUNOS END */

                /* YUNOS BEGIN */
                // ##date:2014/2/18 ##author:yaodi.yd ##BugID:90913
                // mIsTopWhenScreenOff = isTopActivity();
                /* YUNOS BEGIN */
                // ##date:2014/3/12 ##author:yaodi.yd ##BugID:99171 and 98737
                // we set mIsWakeUpFromOtherApp to false when screen off.
                // mIsWakeUpFromOtherApp = false;
                /* YUNOS END */

                /* YUNOS BEGIN */
                // ##date:2014/04/21 ##author:yangshan.ys
                // BugID:110900 return to the screenedit after lock screen and
                // unlock screen operation from screenedit
                if (mScreenEditMode) {
                    exitScreenEditModeWithoutSave();
                }
                /* YUNOS END */
                // YUNOS BEGIN PB
                // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                // ##BugID:(163418) ##date:2014/08/15
                // ##description: Added support for widget page
                if (FeatureUtility.hasFullScreenWidget()) {
                    mWidgetPageManager.onPause();
                }
                // YUNOS END PB
            } else if ("aliyun.intent.action.KEYGUARD_UNLOCK_INTENT_DONE".equals(action)) {
                mUnlockAnimation.finish();
            }else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if(mIsActivateGuide){
                    return;
                }
                closeOpenFolders(false);
                mUnlockAnimation.screenOn();

                // (by wenliang.dwl) close big card when lock screen
                if (mFlipAnim.isShowing()) {
                    stopFlipWithoutAnimation();
                }
                /*
                 * mShouldPlayAnimation = mIsTopWhenScreenOff; if (mState ==
                 * State.WORKSPACE && ((mIsResumed && mShouldPlayAnimation) ||
                 * mIsWakeUpByThreeFingerMode)) {
                 * getWorkspace().cancelUnlockScreenAnimation(); //
                 * ##BugID:93036 if (isScreenLocked()){
                 * getWorkspace().setAllItemsOfCurrentPageVisibility
                 * (View.INVISIBLE);
                 * getAnimationPlayer().postVisibleRunnableDelayed(); } }
                 */
            /* YUNOS END */
                // YUNOS BEGIN PB
                // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                // ##BugID:(163418) ##date:2014/08/15
                // ##description: Added support for widget page
                if (FeatureUtility.hasFullScreenWidget()) {
                    mWidgetPageManager.onResume();
                }
                // YUNOS END PB
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUnlockAnimation.standby();
                // mUserPresent = true;
                // updateRunning();
                /* YUNOS BEGIN */
                // ##date:2014/2/18 ##author:yaodi.yd ##BugID:90913
                /*
                 * LockScreenAnimator lsa =
                 * LockScreenAnimator.getInstance(Launcher.this); if (mState ==
                 * State.WORKSPACE && ( lsa.getNeedBackHomeAnim() ||(mIsResumed
                 * && mShouldPlayAnimation) || mIsWakeUpByThreeFingerMode)) {
                 * Log.d(TAG,
                 * "mIsWakeUpByThreeFingerMode="+mIsWakeUpByThreeFingerMode
                 * +",mShouldPlayAnimation="
                 * +mShouldPlayAnimation+",mIsResumed="
                 * +mIsResumed+",lsa.getNeedBackHomeAnim="
                 * +lsa.getNeedBackHomeAnim()); Log.d(TAG,
                 * "ACTION_USER_PRESENT to play unlock animation");
                 * getAnimationPlayer().play(new
                 * ScreenUnlockedAnimation(Launcher.this)); mIsResumed = false;
                 * }
                 */
                /* YUNOS END */
            } else if (ALARM_ALERT_ACTION.equals(action)) {
                // (by wenliang.dwl) close big card when lock screen
                if (mFlipAnim.isShowing()) {
                    stopFlipWithoutAnimation();
                }
            } else if ("com.yunos.systemui.startup".equals(action)) {
                if (FeatureUtility.hasNotificationFeature()) {
                    mEditModePreviewContainer.initNotificationWidgetView(mAppWidgetManager, mAppWidgetHost);
                }
            } else if ("com.yunos.systemui.action.cacel_pkg_notification".equals(action) || HOMESHELL_ACTION_CANCEL_BIGCARD.equals(action)) {
                stopFlipAnimation();
            }
        }
    };

    public boolean shouldPlayUnlockAnimation() {
        return mState == State.WORKSPACE && !isInEditScreenMode();
    }
    public int getCurrentScreen(){
            return getCurrentWorkspaceScreen();
    }
    public IconManager getIconManager(){
        return ((LauncherApplication) getApplicationContext()).getIconManager();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        /* YUNOS BEGIN */
        // ##date:2014/02/18 ##author:yaodi.yd ##BugID:90913
        filter.addAction(Intent.ACTION_SCREEN_ON);
        /* YUNOS END */
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(ALARM_ALERT_ACTION);
        filter.addAction("aliyun.intent.action.KEYGUARD_UNLOCK_INTENT_DONE");
        filter.addAction("aliyun.intent.action.KEYGUARD_UNLOCK_DONE");
        filter.addAction("com.yunos.systemui.startup");
        filter.addAction("com.yunos.systemui.action.cacel_pkg_notification");
        filter.addAction(HOMESHELL_ACTION_CANCEL_BIGCARD);
        registerReceiver(mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        /* YUNOS BEGIN PB */
        // Desc:soft key feature
        // BugID:6428097
        // ##Date: Sep 16, 2015 4:44:00 PM ##Author:chao.lc
        if (hasNavigationBar()) {
            setupTransparentSystemBarsForLmp();
        }
        /* YUNOS END PB */
        mAttached = true;
        mVisible = true;
        this.sendBroadcast(new Intent("com.yunos.launcher_start").putExtra("showType", LauncherModel.getNotificationMarkType()));
    }

    /* YUNOS BEGIN PB */
    // Desc:soft key feature
    // BugID:6428097
    // ##Date: Sep 16, 2015 4:44:00 PM ##Author:chao.lc@alibaba-inc.com
    /**
     * Sets up transparent navigation and status bars in LMP. This method is a
     * no-op for other platform versions.
     */
    @TargetApi(19)
    private void setupTransparentSystemBarsForLmp() {
        // TODO(sansid): use the APIs directly when compiling against L sdk.
        // Currently we use reflection to access the flags and the API to set
        // the transparency
        // on the System bars.
        if (Utilities.isLmpOrAbove()) {
            try {
                getWindow().getAttributes().systemUiVisibility |=
                        (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                Field drawsSysBackgroundsField = WindowManager.LayoutParams.class.getField(
                        "FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS");
                getWindow().addFlags(drawsSysBackgroundsField.getInt(null));
                Method setStatusBarColorMethod =
                        Window.class.getDeclaredMethod("setStatusBarColor", int.class);
                Method setNavigationBarColorMethod =
                        Window.class.getDeclaredMethod("setNavigationBarColor", int.class);
                setStatusBarColorMethod.invoke(getWindow(), Color.TRANSPARENT);
                setNavigationBarColorMethod.invoke(getWindow(), Color.TRANSPARENT);

                Log.i(TAG, "setting up transparent bars");
            } catch (NoSuchFieldException e) {
                Log.w(TAG, "NoSuchFieldException while setting up transparent bars");
            } catch (NoSuchMethodException ex) {
                Log.w(TAG, "NoSuchMethodException while setting up transparent bars");
            } catch (IllegalAccessException e) {
                Log.w(TAG, "IllegalAccessException while setting up transparent bars");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "IllegalArgumentException while setting up transparent bars");
            } catch (InvocationTargetException e) {
                Log.w(TAG, "InvocationTargetException while setting up transparent bars");
            } finally {
            }
        }
    }
    /* YUNOS END PB */

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateRunning();
    }

    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateRunning();
        // The following code used to be in onResume, but it turns out onResume
        // is called when
        // you're in All Apps and click home to go to the workspace.
        // onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we
                // force it to build
                // layers on all the workspace pages, so that transitioning to
                // Launcher from other
                // apps is nice and speedy.
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;
                    public void onDraw() {
                        if (mStarted)
                            return;
                        mStarted = true;
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run. In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        final ViewTreeObserver.OnDrawListener listener = this;
                        mWorkspace.post(new Runnable() {
                            public void run() {
                                if (mWorkspace != null && mWorkspace.getViewTreeObserver() != null) {
                                    mWorkspace.getViewTreeObserver().removeOnDrawListener(listener);
                                }
                            }
                        });
                        return;
                    }
                });
            }
            clearTypedText();
        }
    }

    private void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    private void updateRunning() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval - (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using
                                            // postDelayed()
            }
        }
    }
    //YUNOS BEGIN PB
    //## modules(fingerprint):
    //## date:2016/05/11 ##author:xiaolu.txl
    //## BugID:8236970
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                       postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
            switch (msg.what) {
                case FINGERPRINT_OPEN_HIDESEAT :
                    /*Log.d(FTAG, "isHideseatShowing : " + isHideseatShowing());
                    Log.d(FTAG, "mWorkspace.getOpenFolder() : " + (mWorkspace.getOpenFolder() == null));
                    Log.d(FTAG, "isWorkspaceLocked " + isWorkspaceLocked());
                    Log.d(FTAG, "isInLauncherEditMode " + isInLauncherEditMode());
                    Log.d(FTAG, "mDragController.isDragging(): " + mDragController.isDragging());
                    Log.d(FTAG, "isWidgetScreen : " + isWidgetScreen(getCurrentScreen()));
                    Log.d(FTAG, "isInLeftScreen : " + isInLeftScreen());
                    Log.d(FTAG, "isGadgetCardShowing:" + isGadgetCardShowing());
                    Log.d(FTAG, "isInLauncherCategoryMode:" + isInLauncherCategoryMode());
                    Log.d(FTAG, "freezeValue is : " + HomeShellSetting.getFreezeValue(Launcher.this));*/
                    if (!isHideseatShowing() && (mWorkspace.getOpenFolder() == null) && !isInLauncherEditMode() && !isWorkspaceLocked() && !mDragController.isDragging() && !isWidgetScreen(getCurrentScreen())
                            && !isInLeftScreen() && !isGadgetCardShowing() && !isInLauncherCategoryMode() && !HomeShellSetting.getFreezeValue(Launcher.this)) {
                        Log.i(FTAG, "open hide seat before");
                        if (!mFingerCloseHideseat || (SystemClock.uptimeMillis() - mFingerCloseHideseatTime > 2000)) {
                            Log.i(FTAG, "open hide seat");
                            getGestureLayer().hideseatSwitch(true);
                        }
                        mFingerCloseHideseat = false;
                        Log.i(FTAG, "open hide seat end");
                    }
                    break;
                case FINGERPRINT_CLOSE_HIDESEAT:
                    if(isHideseatShowing() && !isWidgetScreen(getCurrentScreen()) && !isInLeftScreen()) {//BugID:6448244
                        Log.i(FTAG, "close hide seat");
                        getGestureLayer().hideseatSwitch(false);
                        mFingerCloseHideseat = true;
                        mFingerCloseHideseatTime = SystemClock.uptimeMillis();
                    }
                    break;
                default :
                    break;
            }
        }
    };
  //YUNOS END PB
    private boolean mIsAllAppShowed;
    public boolean isAllAppShowed(){
        return mIsAllAppShowed;
    }

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1)
            return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        /* YUNOS BEGIN */
        // ##date:2014/4/21 ##author:hongchao.ghc ##BugID:111144
        if (v != null && v instanceof Advanceable) {
        /* YUNOS END */
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateRunning();
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_REMOVE_WIDGET,
                ((launcherInfo == null) ? "" : launcherInfo.providerName.toString()));
        /* YUNOS BEGIN */
        // ##date:2014/4/21 ##author:hongchao.ghc ##BugID:111144
        if (launcherInfo != null) {
            removeWidgetToAutoAdvance(launcherInfo.hostView);
            launcherInfo.hostView = null;
        }
        /* YUNOS END */
    }

    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    void showToastMessage(int strId) {
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        mWaitingForResult = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent == null || mWorkspace == null){
            return;
        }
        final Intent lifeIntent = intent;
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }

        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        if (cellLayout == null) {
            return;
        }
        ActionSheet actionSheet = cellLayout.getCellActionSheet();
        if (actionSheet != null) {
            actionSheet.dismiss();
        }

        Log.d(TAG,"onNewIntent");
        super.onNewIntent(intent);
        /* YUNOS BEGIN */
        // ##date:2014/2/18 ##author:yaodi.yd ##BugID:91808 & BugID:91825
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey("wakeUp")) {
            //mIsWakeUpByThreeFingerMode = true;
            return;
        } else {
            //mIsWakeUpByThreeFingerMode = false;
            //mShouldPlayAnimation = false;
        }

        if (HintDialogUitils.dismissAppCloneHintDialogIfNeed()) {
            return;
        }

        /* YUNOS END */
        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();
            final boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            Log.d(TAG, "onNewIntent, alreadyonhome=" + alreadyOnHome);
            /* YUNOS BEGIN */
            // ##date:2014/3/11 ##author:zhangqiang.zq
            // bugID:97562 screen edit result saved after power off
            final boolean needExitScreenEditMode = ((intent.getFlags() & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)) == (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            /* YUNOS END */

            final String url = intent.getStringExtra("url");
            if ((isLeftScreenOpened() || isInLeftScreen()) && url == null) {
                mCardBridge.dispatchHome(alreadyOnHome);
                return;
            }

            /* YUNOS END */

            /* YUNOS BEGIN */
            // ##date:2015/2/6 ##author:zhanggong.zg ##BugID:5776265
            final boolean backFromHomeShellSetting = mBackFromHomeShellSetting;
            mBackFromHomeShellSetting = false;
            /* YUNOS END */

            Runnable processIntent = new Runnable() {
                public void run() {
                    if (mWorkspace == null) {
                        // Can be cases where mWorkspace is null, this prevents
                        // a NPE
                        return;
                    }

                    boolean moveToMainPage = true;
                    /* YUNOS BEGIN */
                    // ## date: 2016/06/20 ## author: yongxing.lyx
                    // ## BugID:8424225: icons disappear after press home when
                    // enter searching from folder name editor.
                    mWorkspace.resetChildrenVisibility();
                    /* YUNOS END */
                    if (mSupportLifeCenter) {
                        if (null != url) {
                            if (getCurrentScreen() != 0) {
                                mCardBridge.enterShowDetailCard(url, lifeIntent);
                                /* YUNOS BEGIN */
                                // ## date: 2016/06/02 ## author: yongxing.lyx
                                // ## BugID:8280545: blink when press home button at O+C page.
                                mWorkspace.setCurrentPage(0);
                                mWorkspace.onPageEndMoving();
                                /* YUNOS END */
                            } else {
                                mCardBridge.showCardWithIntent(url, lifeIntent);
                            }
                            moveToMainPage = false;
                        }
                    } else if (mSupportLeftScreen) {
                        if (url != null) {
                            if (!isLeftScreenOpened) {
                                mCardBridge.enterShowDetailCard(url, lifeIntent);
                                openLeftScreen(false);
                            } else {
                                mCardBridge.showCardWithIntent(url, lifeIntent);
                            }
                            moveToMainPage = false;
                        }
                    }

                    if (mModel.isInProcess()) {
                        return;
                    }

                    /* YUNOS BEGIN */
                    // ## modules(Home Shell): [Category]
                    // ## date: 2015/07/30 ## author: wangye.wy
                    // ## BugID: 6221911: category on desk top
                    if (mLauncherCategoryMode) {
                        if(mWorkspace.isPageMoving()) {
                            mWorkspace.runOnPageStopMoving(new Runnable() {
                                @Override
                                public void run() {
                                    mModel.recoverAllIcons(true);
                                }
                            });
                        } else {
                            mModel.recoverAllIcons(true);
                        }
                        return;
                    }
                    /* YUNOS END */

                    // Exit edit mode
                    if (mLauncherEditMode) {
                        if (!mWorkspace.isAnimateScrollEffectOver()) {
                            Log.d(TAG_EDITMODE, "sxsexe   onNewIntent AnimateScrollEffect is not over yet!!!");
                            return;
                        }
                        if (mWorkspace.isPageMoving()) {
                            // ##date:2015/8/4 ##author:zhanggong.zg
                            // ##BugID:6275448
                            // wait until page scrolling animation is over
                            mWorkspace.runOnPageStopMoving(new Runnable() {
                                @Override
                                public void run() {
                                    exitLauncherEditMode(true);
                                }
                            });
                        } else {
                            exitLauncherEditMode(true);
                        }
                        return;
                    }

                    // Go to default screen after edit mode animation
                    AnimatorSet animatorSet = mEditModeHelper.getRunningEditModeAnimations();
                    if (animatorSet != null) {
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mWorkspace.moveToDefaultScreen(true);
                            }
                        });
                        return;
                    }

                    if( mFlipAnim.isShowing() && !mFlipAnim.isAnimating() ){
                        stopFlipAnimation(false);
                        closeFolder();
                        return;
                    }else if( mFlipAnim.isShowing() ){
                        return;
                    }
                    /*YUNOS BEGIN*/
                    //##date:2014/1/9 ##author:zhangqiang.zq
                    //screen edit
                    if (mScreenEditMode && needExitScreenEditMode) {
                        exitScreenEditMode(true);
                        return;
                    }
                    /*YUNOS END*/

                  /*YUNOS BEGIN*/
                  //##date:2014/6/4 ##author:zhangqiang.zq
                  // smart search
                    if (mAppSearchMode) {
                        exitSearchMode();
                        return;
                    }
                    /*YUNOS END*/

                    /* YUNOS BEGIN */
                    // ##date:2013/12/23 ##author:yaodi.yd
                    // optimize the uninstalling process

                    // (##BugId:103767) Dismiss delete dialog and recovery icon
                    // to original position.
                    if (mDeleteDialog != null && mDeleteDialog.isShowing()) {
                        mDeleteDialog.getNegetiveButton().performClick();
                        return;
                    }
                    /* YUNOS END */

                    // In all these cases, only animate if we're already on home
                    mWorkspace.exitWidgetResizeMode();
                    Folder openFolder = mWorkspace.getOpenFolder();
                    boolean openHideseat = isHideseatShowing();
                    if (moveToMainPage && alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() && openFolder == null
                            && !openHideseat && !backFromHomeShellSetting) {
                        mWorkspace.moveToDefaultScreen(true);
                    }

                    if (openHideseat) {
                        hideHideseat(true);
                    }

                    closeFolder();

                    // If we are already on home, then just animate back to the
                    // workspace,
                    // otherwise, just wait until onResume to set the state back
                    // to Workspace
                    if (alreadyOnHome) {
                        showWorkspace(true);
                    } else {
                        mOnResumeState = State.WORKSPACE;
                    }

                    final View v = getWindow().peekDecorView();

                    try {
                        if (v != null && v.getWindowToken() != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    } catch (RuntimeException e) {
                        // add try catch block due to InputMethod throws
                        // RuntimeException;
                        // BugID:101639
                        Log.e(TAG, e.toString(), e);
                    }
                }
            };

            if (alreadyOnHome && !mWorkspace.hasWindowFocus()) {
                // Delay processing of the intent to allow the status bar
                // animation to finish
                // first in order to avoid janky animations.
                mWorkspace.postDelayed(processIntent, 350);
            } else {
                // Process the intent immediately.
                processIntent.run();
            }

        }
        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page : mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
        mWorkspace.moveToDefaultScreen(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need
        // to make sure
        // this state is reflected.
        /* YUNOS BEGIN */
        // ##date:2014/3/10 ##author:yaodi.yd
        // closeFolder();
        if (mWorkspace.getOpenFolder() != null && !mWorkspace.getOpenFolder().isEditingName()) {
            closeFolderWithoutExpandAnimation();
        }
        /* YUNOS END */

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screen > -1 && mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
        }

        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // CheckVoiceCommandPressHelper.getInstance().deInitVoiceService();
        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.cleanUpAllGadgets();
        mHandler.removeCallbacks(mRefeshViewTextColor);
        mAppCloneManager.destroy();
        // Stop callbacks from LauncherModel
        LauncherApplication app = ((LauncherApplication) getApplication());
        /* YUNOS BEGIN */
        // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
        if (mModel != null) {
            mModel.stopLoader();
        }
        /* YUNOS END */
        app.setLauncher(null);

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();

        // Disconnect any of the callbacks and drawables associated with
        // ItemInfos on the workspace
        // to prevent leaking Launcher activities on orientation change.
        if (mModel != null) {
            mModel.unbindItemInfosAndClearQueuedBindRunnables();
        }

        getContentResolver().unregisterContentObserver(mWidgetObserver);
        unregisterReceiver(mCloseSystemDialogsReceiver);
        /* YUNOS BEGIN */
        // ##date:2013/12/09 ##author: hongxing.whx ##BugId: 60497
        // For wallpaper changed
        unregisterReceiver(mWallpaperChangedReceiver);
        /* YUNOS END */
        unregisterReceiver(mEnableCloudCardReceiver);
        // (BugID:5216654 by wenliang.dwl) unregister receiver relevant to close
        // Card
        ((LauncherContainer) findViewById(R.id.launcher_container)).unregisterReceiver();
        unregisterReceiver(mFlipAnim);

        // unregister content observer of privacy space and notification
        // importance
        // these are used in GadgetCardHelper. (by wenliang.dwl)
        PrivacySpaceHelper.destroy();
        NotificationPriorityHelper.destroy();

        mDragLayer.clearAllResizeFrames();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllViews();
        mAppCloneManager = null;
        mWorkspace = null;
        mDragController = null;
        mTitleColorManager = null;


        LauncherAnimUtils.onDestroyActivity();

        if (mMenu != null) {
            mMenu = null;
        }

        UserTrackerHelper.deinit();

        /* YUNOS BEGIN */
        // ##date:2014/03/19 ##author: chao.jiangchao ##BugId: 102610
        if (mSupportLifeCenter || mSupportLeftScreen) {
            if(mCardBridge != null){
                mCardBridge.onDestroy();
            }
        }
        mLifeCenterReceiver.unRegister();
        /* YUNOS END */
        LauncherApplication.homeshellSetting = null;

        if(isHideDeleteDialog()) {
            mDeleteDialog.dismiss();
        }
        mDeleteDialog = null;

        gadgetViewList.clear();
        mAppCloneManager = null;
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
            if(mSearchBridge != null){
                mSearchBridge.destoryBridge();
            }
        }
    }

    public DragController getDragController() {
        return mDragController;
    }

    public LifeCenterReceiver getLifeCenterReceiver() {
        return mLifeCenterReceiver;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0)
            mWaitingForResult = true;
        super.startActivityForResult(intent, requestCode);
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult || mModel.isDownloadStatus();
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screen = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = -1;
        mPendingAddInfo.dropPos = null;
    }

    public boolean isEmptyCellCanBeRemoved() {
        return (mScreenManager != null && mScreenManager.lockWorkspace())
                || (mWorkspace != null && mWorkspace.getChildAt(mWorkspace.getCurrentPage()) == LockScreenAnimator.getInstance(this)
                        .getWorkingOnCellLayout());
    }

    void addAppWidgetImpl(final int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;

            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
            /*YUNOS BEGIN*/
            //##date:2013/1/8 ##author:jun.dongj:BugID:84114
            //can't add yahoo weather widget to desktop
            //if (appWidgetId != -1) {
            //   getAppWidgetHost().deleteAppWidgetId(appWidgetId);
            //}
            /*YUNOS END*/
        } else {
            // Otherwise just add it
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget, appWidgetInfo);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName
     *            The name of the component
     * @param screen
     *            The screen where it should be added
     * @param cell
     *            The cell it should be added to, optional
     * @param position
     *            The location on the screen where it was dropped, optional
     */
    void processShortcutFromDrop(ComponentName componentName, long container, int screen, int[] cell, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    /**
     * Process a widget drop.
     *
     * @param info
     *            The PendingAppWidgetInfo of the widget being added.
     * @param screen
     *            The screen where it should be added
     * @param cell
     *            The cell it should be added to, optional
     * @param position
     *            The location on the screen where it was dropped, optional
     */
    void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen, int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = loc;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);
        } else {
            // In this case, we either need to start an activity to get
            // permission to bind
            // the widget, or we need to start an activity to configure the
            // widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = false;
            // binding widget will failed, while uninstalled widgetApp before
            // drop widget.
            try {
                if (options != null) {
                    success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName, options);
                } else {
                    success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in addAppWidgetFromDrop : " + e.getMessage());
            }

            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options
                // bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
                // options);
                /* YUNOS BEGIN modified by xiaodong.lxd #111373 */
                try {
                    startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
                } catch (Exception e) {
                    Log.e(TAG, "sxsexe---->addAppWidgetFromDrop Error " + e.getMessage());
                }
                /* YUNOS END */
            }
        }
    }

    /* YUNOS BEGIN */
    // ##gadget
    // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com##BugID:96378
    void addGadgetFromDrop(GadgetItemInfo info, long container, int screen, int[] cell, int[] span, int[] loc) {
        if (info == null || info.gadgetInfo == null) {
            Log.e(TAG, "add gadget failed, info N/A");
        } else {
            View v = LauncherGadgetHelper.getGadget(this, info.gadgetInfo);
            if (v == null) {
                Log.e(TAG, "add gadget failed " + info.gadgetInfo);
            } else {
                if (mEditModePreviewContainer != null) {
                    mEditModePreviewContainer.setDragSrcView(v);
                }
                info.cellX = cell[0];
                info.cellY = cell[1];
                info.spanX = span[0];
                info.spanY = span[1];
                if (ConfigManager.isLandOrienSupport()) {
                    boolean isLand = LauncherApplication.isInLandOrientation();
                    ScreenPosition pos = LauncherModel.findEmptyCellAndOccupy(screen, info.spanX, info.spanY, !isLand);
                    if (pos == null) {
                        showOutOfSpaceMessage(false);
                        return;
                    } else {
                        if (isLand) {
                            info.cellXLand = info.cellX;
                            info.cellYLand = info.cellY;
                            info.cellXPort = pos.xPort;
                            info.cellYPort = pos.yPort;
                        } else {
                            info.cellXPort = info.cellX;
                            info.cellYPort = info.cellY;
                            info.cellXLand = pos.xLand;
                            info.cellYLand = pos.yLand;
                        }
                    }
                }
                v.setTag(info);
                mWorkspace.addInScreen(v, container, screen, cell[0], cell[1], span[0], span[1], isWorkspaceLocked());
                LauncherModel.addItemToDatabase(this, info, container, screen, cell[0], cell[1], false);
                Map<String, String> param = new HashMap<String, String>();
                param.put("widget", ThemeUtils.getGadgetName(this, info.gadgetInfo));
                param.put("container", String.valueOf(container));
                param.put("screen", String.valueOf(screen));
                param.put("cellX", String.valueOf(cell[0]));
                param.put("cellY", String.valueOf(cell[1]));
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WIDGET_SELECT, param);
            }
        }
    }
    /* YUNOS END */

    void processShortcut(Intent intent) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.title_select_application));
            startActivityForResultSafely(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            startActivityForResultSafely(intent, REQUEST_CREATE_SHORTCUT);
        }

        /* YUNOS BEGIN */
        // ##date:2013/11/23 ##author:xiaodong.lxd
        // checkAndRemoveEmptyCell
        getWorkspace().checkAndRemoveEmptyCell();
        /* YUNOS END */
    }

    void processWallpaper(Intent intent) {
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }

    FolderIcon addFolder(CellLayout layout, long container, final int screen, int cellX, int cellY, ShortcutInfo destInfo) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addFolderToDatabase(Launcher.this, folderInfo, container, screen, cellX, cellY, false, destInfo);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
        mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY, 1, 1, isWorkspaceLocked());
        if (ConfigManager.isLandOrienSupport()) {
            LauncherModel.markCellsOccupiedInNonCurrent(folderInfo, true);
        }
        return newFolder;
    }

    public void removeFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    // private void startWallpaper() {
    // showWorkspace(true);
    // final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
    // Intent chooser = Intent.createChooser(pickWallpaper,
    // getText(R.string.chooser_wallpaper));
    // // NOTE: Adds a configure option to the chooser if the wallpaper supports
    // it
    // // Removed in Eclair MR1
    // // WallpaperManager wm = (WallpaperManager)
    // // getSystemService(Context.WALLPAPER_SERVICE);
    // // WallpaperInfo wi = wm.getWallpaperInfo();
    // // if (wi != null && wi.getSettingsActivity() != null) {
    // // LabeledIntent li = new LabeledIntent(getPackageName(),
    // // R.string.configure_wallpaper, 0);
    // // li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
    // // chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
    // // }
    // startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    // }

    /**
     * Registers various content observers. The current implementation registers
     * only a favorites observer to keep track of the favorites applications.
     */
    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, mWidgetObserver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME :
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN :
                    if (isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME :
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (isAllAppsVisible()) {
            showWorkspace(true);
        } else if (mFlipAnim != null && mFlipAnim.isShowing()) {
            if (!mFlipAnim.isAnimating())
                stopFlipAnimation();
        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
        } else if (mLauncherEditMode) {
            if (!mWorkspace.isAnimateScrollEffectOver()) {
                Log.d(TAG_EDITMODE, "sxsexe   onBackPressed AnimateScrollEffect is not over yet!!!");
                return;
            }
            if (mEditModePreviewContainer.isShowing() && mWorkspace.getFolderBatchOping() == null) {
                mEditModeHelper.switchFromEmContainerToMenu();
                mEditModePreviewContainer.setContentType(PreviewContentType.None);
                mEditModeHelper.updateEditModeTips(PreviewContentType.CellLayouts);
                mWorkspace.clearSelectFlag();
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: item of icon sort
            } else if (isSortMenuShowing()) {
                mEditModeHelper.switchFromSortMenuToMenu();
                mEditModePreviewContainer.setContentType(PreviewContentType.CellLayouts);
                mEditModeHelper.updateEditModeTips(PreviewContentType.CellLayouts);
            /* YUNOS END */
            } else if (mWorkspace.isPageMoving()) {
                // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6275448
                // wait until page scrolling animation is over
                mWorkspace.runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        exitLauncherEditMode(true);
                    }
                });
            } else {
                exitLauncherEditMode(true);
            }
        } else if (mEditModeHelper.getRunningEditModeAnimations() != null) {
            AnimatorSet set = mEditModeHelper.getRunningEditModeAnimations();
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    gotoMainScreen();
                }
            });
        } else if (mWorkspaceLoading) {//added by LXD ##bug82147
            return;
        } else if (isHideseatShowing()) {
            CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
            if (cellLayout.isWidgetPage()) {
                hideHideseat(false);
                gotoMainScreen();
            } else {
                hideHideseat(true);
            }
        } else if (CheckVoiceCommandPressHelper.isPushTalkCanUse() && CheckVoiceCommandPressHelper.getInstance().isVoiceUIShown()) {
            CheckVoiceCommandPressHelper.getInstance().forceDismissVoiceCommand();
        } else {
            mWorkspace.exitWidgetResizeMode();

            // Back button is a no-op here, but give at least some feedback for
            // the button press
            // mWorkspace.showOutlinesTemporarily();
            gotoMainScreen();
        }
    }

    /* YUNOS BEGIN */
    // ##module(component name)
    // ##date:2013/11/25 ##jun.dongj@alibaba-inc.com##BugId:66151
    // go to default screen while pressing back key
    /**
     * go to default screen.
     */
    public void gotoMainScreen() {
        if (hasWindowFocus()) {
            if (mWorkspace.getCurrentPage() != DEFAULT_SCREEN) {
                mWorkspace.moveToDefaultScreen(true);
            }
        }
    }

    /* YUNOS END */

    /**
     * Re-listen when widgets are reset.
     */
    private void onAppWidgetReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v
     *            The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is
        // launching, or after the
        // view has detached (it's possible for this to happen if the view is
        // removed mid touch).

        if (mModel.isInProcess()) {
            return;
        }

        /* YUNOS BEGIN */
        // ## modules(Home Shell): [Category]
        // ## date: 2015/07/30 ## author: wangye.wy
        // ## BugID: 6221911: category on desk top
        if (mLauncherCategoryMode) {
            return;
        }
        /* YUNOS END */

        /* YUNOS BEGIN added by xiaodong.lxd for editmode */
        if (mLauncherEditMode) {
            mEditModeHelper.handleClickEventInEditMode(v);
            return;
        }
        /* YUNOS END */

        if (v.getWindowToken() == null) {
            Log.d(TAG, "sxsexe-----------------> onClick return by v.getWindowToken() is null");
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            Log.d(TAG,
                    "sxsexe-----------------> onClick return by mWorkspace.isFinishedSwitchingState() "
                            + mWorkspace.isFinishedSwitchingState());
            return;
        }

        /* YUNOS BEGIN */
        // ##date:2014/04/15 ##author:zhangqiang.zq ##BugID:110307
        // should dismiss menu when click happend
        if (mMenu != null && isMenuShowing()) {
            dismissMenu();
        }
        /* YUNOS ENG */

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: item of icon sort
        if (mSortMenu != null && isSortMenuShowing()) {
            dismissSortMenu();
        }
        /* YUNOS END */

        // added by wenliang.dwl, avoid onclick when doing animation
        if (LockScreenAnimator.getInstance(this).shouldPreventGesture()) {
            Log.d(TAG, "sxsexe-----------------> onClick return by shouldPreventGesture ");
            return;
        }

        /* YUNOS BEGIN added by xiaodong.lxd for push to talk */
        if (!mOnClickValid) {
            Log.d(TAG, "sxsexe-----------------> onClick return by mOnClickValid ");
            mOnClickValid = true;
            return;
        }
        /* YUNOS END */

        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/02/03 ## author: wangye.wy
        // ## BugID: 7880734: return on double click
        CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        if (cellLayout == null) {
            return;
        }
        ActionSheet actionSheet = cellLayout.getCellActionSheet();
        if (actionSheet != null && actionSheet.isShowing()) {
            return;
        }
        /* YUNOS END */

        Object tag = v.getTag();

        Log.d(TAG, "sxsexe-----------------> onClick tag " + tag);
        if (tag instanceof ShortcutInfo) {
            // Open shortcut
            ShortcutInfo info = ((ShortcutInfo) tag);
            if (HomeShellGadgetsRender.isOneKeyAccerateShortCut(info)) {
                // YUNOS BEGIN
                // ##modules(HomeShell): ##yongxing.lyx
                // ##BugID:(8113446) ##date:2016/04/13
                // ##description: emerge black screen when press OneKeyAccerate quickly
                if (HomeShellGadgetsRender.getRender().isAccelarateAnimPlaying(this) || !mIsResumed || !mIsStarted) {
                    return;
                }
                // YUNOS END
                startActivitySafely(v, info.intent, tag);
                info.setIsNewItem(false);
                BubbleController.updateView((BubbleTextView) v);
                LauncherModel.modifyItemNewStatusInDatabase(this, info, false);
                HomeShellGadgetsRender.getRender().playAccelarateAnimation(getApplicationContext());
                return;
            }

            if (v instanceof BubbleTextView && !info.isDownloading()) {
                BubbleTextView view = (BubbleTextView) v;
                view.setShowClickEffect(true);
                view.invalidate();
            }

            /* YUNOS BEGIN xiaodong.lxd#139377 */
            if (((ShortcutInfo) tag).intent == null) {
                Log.d(TAG, "sxsexe-----------------> onClick return by intent == null ");
                return;
            }
            /* YUNOS END */

            // BugID:5193088:same shortcut item issue
            // create an new intent, not use the intent in shortcutinfo
            // to avoid shortcutinfo's intent change.
            final Intent intent = new Intent(((ShortcutInfo) tag).intent);

            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
            // add by dongjun for appstore begin
            boolean success = false;

            /* YUNOS BEGIN */
            // ##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
            // vp install
            // item type vpinstall need to be checked before isDownloading
            // remove vp install
            // if (info.itemType ==
            // LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL) {
            // Log.d(TAG, "sxsexe----->vpinstall slience install");
            // mModel.startVPSilentInstall(info);
            // }
            /* YUNOS END */
            // else
            if (info.isDownloading()) {
                success = onDownloadingClick(v);
            } else if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                // the app that is frozen in hide-seat cannot run
                if (mLauncherToast == null) {
                    mLauncherToast = Toast.makeText(this, R.string.application_unavailable_due_to_frozen, Toast.LENGTH_SHORT);
                } else {
                    mLauncherToast.setText(R.string.application_unavailable_due_to_frozen);
                }
                mLauncherToast.show();

                // BugID:5717551:userTrack for hide out
                Map<String, String> param = new HashMap<String, String>();
                param.put("type", "app");
                Intent itemIntent = info.intent;
                if ((itemIntent != null) && (itemIntent.getComponent() != null)){
                    param.put("PkgName", itemIntent.getComponent().getPackageName());
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_HIDESEAT_CLICK, param);
                }
            } else if (HintDialogUitils.needShowAppCloneHintDialog(this, tag)) {
                HintDialogUitils.showAppCloneHintDialog(this, v);
                return;
            }else{
                if (isContactShortCut(info)) {
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_DIRECT_DIAL_CLICK);
                }
                success = startActivitySafely(v, intent, tag);
                /*YUNOS BEGIN*/
                //##date:2014/8/1 ##author:zhangqiang.zq
                // favorite app
                if (info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION
                        && info.container != LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                    ((LauncherApplication)getApplication()).collectUsageData(info.id);
                }
                /*YUNOS END*/
                //added by dongjun for adding user tracker
                if(info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT ){
                    StringBuilder sb = new StringBuilder();
                    if(intent.getComponent() == null) {
                        sb.append(intent.getAction());//for bookmark icon
                    } else {
                        sb.append(intent.getComponent().getPackageName());
                    }
                    sb.append(":").append(mWorkspace.getIconScreenIndex(getCurrentWorkspaceScreen()) + 1).append(":")
                            .append(info.cellX + 1);
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_START_APPLICATION_DOCK,sb.toString());
                }
            }
            //add by dongjun for appstore end

            if (success && v instanceof BubbleTextView) {
                mWaitingForResume = (BubbleTextView) v;
                if (info.isNewItem()) {
                    // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6277395
                    asyncSendFirstLaunchBroadcast(info);
                }
                info.setIsNewItem(false);
                BubbleController.updateView(mWaitingForResume);
                LauncherModel.modifyItemNewStatusInDatabase(this, info, false);
                asyncIncreaseAppLaunchCount(info);
                /*
                 * mWaitingForResume.setStayPressed(true);
                 */
            }
            /* YUNOS BEGIN added by xiaodong.lxd #97820 */
            v.postInvalidateDelayed(300);
            /* YUNOS ENG */

        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                handleFolderClick(fi);
            }
        } else if (v == mAllAppsButton) {
            if (isAllAppsVisible()) {
                showWorkspace(true);
            } else {
                onClickAllAppsButton(v);
            }
        } else if (v instanceof CellLayout) {
            CellLayout layout = (CellLayout) v;
            if (layout.isHideseatOpen) {
               hideHideseat(true);
            }
        }
    }

    /* YUNOS BEGIN */
    // ##date:2015/8/4 ##author:zhanggong.zg ##BugID:6277395
    public static void asyncSendFirstLaunchBroadcast(final ShortcutInfo info) {
        // resolve package name
        Intent intent = info.intent;
        ComponentName cmpt = intent != null ? intent.getComponent() : null;
        final String pkgName = cmpt != null ? cmpt.getPackageName() : null;
        if (pkgName == null || pkgName.isEmpty()) {
            // failed to resolve packaged name, can be a shortcut
            return;
        }
        // determine first launch or not
        LauncherModel.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Map<String, Collection<ShortcutInfo>> map = LauncherModel.getAllShortcutInfoByPackageNames(Arrays.asList(pkgName));
                for (ShortcutInfo otherInfo : map.get(pkgName)) {
                    if (otherInfo != info && otherInfo.isNewItem() == false) {
                        // already launched before by other icon
                        return;
                    }
                }
                // first launch, send broadcast
                Context context = LauncherApplication.getContext();
                Intent intent = new Intent(INTENT_ACTION_FIRST_LAUNCH_APP);
                intent.putExtra("pkg", pkgName);
                context.sendBroadcast(intent);
            }
        });
    }
    /* YUNOS END */

    public static void asyncIncreaseAppLaunchCount(final ShortcutInfo info) {
        LauncherModel.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                AppLaunchManager manager = AppLaunchManager.getInstance();
                manager.increaseCount(info);
            }
        });
    }

    private static boolean isContactShortCut(ItemInfo info) {
        return info instanceof ShortcutInfo && info.itemType == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT
                && ((ShortcutInfo) info).intent != null && Intent.ACTION_CALL.equals(((ShortcutInfo) info).intent.getAction());
    }

    public boolean onTouch(View v, MotionEvent event) {
        // this is an intercepted event being forwarded from mWorkspace;
        // clicking anywhere on the workspace causes the customization drawer to
        // slide down
        showWorkspace(true);
        return false;
    }

    /**
     * Event handler for the "grid" button that appears on the home screen,
     * which enters all apps mode.
     *
     * @param v
     *            The view that was clicked.
     */
    public void onClickAllAppsButton(View v) {
        showAllApps(true);
    }

    public void onClickAppWidgetMenu() {
        mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
        UserTrackerHelper.entryPageBegin(UserTrackerMessage.LABEL_WIDGET_LOADER);
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_WIDGET_LOADER);
        showAllApps(true);
    }
    public void onTouchDownAllAppsButton(View v) {
        // Provide the same haptic feedback that the system offers for virtual
        // keys.
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void onIconFlingUp(BubbleTextView view) {
        Log.d(FlipAnimation.TAG, "onFlingUp");

        ShortcutInfo info = (ShortcutInfo) view.getTag();
        if (info == null || info.intent == null) {
            return;
        }
        if (info.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
            return;
        }
        if (isInLauncherEditMode()) {
            return;
        }
        // ##date:2015-3-24 ##author:zhanggong.zg ##BugID:5850679
        if (info.itemType != Favorites.ITEM_TYPE_APPLICATION) {
            view.shake();
            return;
        }

        if (mWorkspace.isCatActShtStart()) {
            view.shake();
            return;
        }
        ComponentName cn = info.intent.getComponent();
        GadgetCardHelper helper = GadgetCardHelper.getInstance(view.getContext());

        View gadget = null;
        gadget = helper.getCardView(cn, view, view.getIconDrawable(), view.getText(), info.messageNum > 0);

        if (gadget == null) {
            Log.d(FlipAnimation.TAG, "onFlingUp: no card view for " + info.title);
            view.shake();
        } else {
            startFlipAnimation(view, gadget);
            // add user track by wenliang.dwl
            Map<String, String> msg = new HashMap<String, String>();
            msg.put("PkgName", cn.getPackageName());
            msg.put("Type", gadget instanceof GadgetView ? "Special" : "Normal");
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CARD_SLIDE_UP, msg);
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivitySafely(null, intent, "startApplicationDetailsActivity");
    }

    void startApplicationUninstallActivity(final ApplicationInfo appInfo, final ShortcutInfo shortcutInfo, final Bitmap dragBitmap) {
        Log.d(TAG, "sxsexe---->startApplicationUninstallActivity app " + appInfo);
        boolean isSystemApp = (appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) == 0;
        boolean isUsbModeApp = (appInfo.flags & ApplicationInfo.SDCARD_FLAG) != 0 && Utils.isInUsbMode();
        final Folder folder = mFolder;
        if (isSystemApp || isUsbModeApp) {
            // SystemApp and sdcard-app(in usb mode) can't be uninstalled,show
            // toast
            // We may give them the option of disabling apps this way.
            if (isUsbModeApp) {
                Toast.makeText(this, R.string.application_not_deleted_in_usb, Toast.LENGTH_SHORT).show();
            }
            reVisibileDraggedItem(appInfo);
            mWorkspace.checkAndRemoveEmptyCell();
            getSearchBar().getTrashAnimatorSet(false, false).start();
        } else {
            /* YUNOS BEGIN */
            // ##date:2013/12/23 ##author:yaodi.yd
            // optimize the uninstalling process
            // String packageName = appInfo.componentName.getPackageName();
            // String className = appInfo.componentName.getClassName();
            // Intent intent = new Intent(Intent.ACTION_DELETE,
            // Uri.fromParts("package", packageName, className));
            // intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            // startActivity(intent);
            final String packageName = appInfo.componentName.getPackageName();
            final ShortcutInfo info = shortcutInfo;
            final View cell = mWorkspace.getDragItemFromList(info, false);
            final IPackageDeleteObserver deleteObserver = new IPackageDeleteObserver.Stub() {
                @Override
                public void packageDeleted(String arg0, int arg1) throws RemoteException {

                    /* YUNO BEGIN added by xiaodong.lxd */
                    // if failed to delete, revisible this app and toast
                    // 1 means delete_succeed
                    if (arg1 == 1) {
                        // bugid: 5316363: remove item after uninstalling
                        // successfully
                        // originally, the following section of code was called
                        // in
                        // positivelistener.onClick(). But this will cause icon
                        // is
                        // removed from UI but applicaiton is not deleted
                        // successfully
                        getWorkspace().removeDragItemFromList(appInfo);
                        checkAndReplaceFolderIfNecessary(appInfo, folder);
                        Map<String, String> param = new HashMap<String, String>();
                        param.put("item_name", appInfo == null ? "" : appInfo.componentName.toString());
                        param.put("operate_area", isInLauncherEditMode() ? "menu_arrage" : "launcher");
                        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_REMOVE_APP, param);
                        // end of 5316363
                        /* YUNOS BEGIN */
                        // ##date:2014/9/19 ##author:zhanggong.zg
                        // ##BugID:5244146
                        // For hide-seat item, the database record should be
                        // removed when
                        // the uninstall succeed.
                        if (shortcutInfo.container == Favorites.CONTAINER_HIDESEAT) {
                            LauncherModel.deleteItemFromDatabase(Launcher.this, shortcutInfo);
                        }
                        /* YUNOS END */
                        /* YUNOS BEGIN */
                        // ## date: 2016/06/03 ## author: yongxing.lyx
                        // ## BugID:8357388:can't get correct Map by
                        // getAllProfileApps() at this moment.
                        mAppCloneManager.onAppUninstalled(packageName);
                        /* YUNOS END */
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                //BugID:6673553:icon alpha not restore if uninstall failed
                                //belong code will not be performed because
                                //the item in mDragItems is removed by last operation
                                //reVisibileDraggedItem(appInfo);
                                if (cell != null) {
                                    cell.setAlpha(1);
                                }
                                shortcutInfo.mIsUninstalling = false;
                                Toast.makeText(getApplicationContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    /* YUNOS END */

                    getWorkspace().checkAndRemoveEmptyCell();
                }
            };

            final AppCloneCallback appCloneCallback = new AppCloneCallback() {

                @Override
                public void onDeleted(int userId, String packageName, int returnCode) {
                    super.onDeleted(userId, packageName, returnCode);
                    if (returnCode == AppCloneManager.EXEC_SUCCESS) {
                        checkAndReplaceFolderIfNecessary(appInfo, folder);
                        LauncherModel.deleteItemFromDatabase(getApplicationContext(), shortcutInfo);
                        /* YUNOS BEGIN */
                        // ## date: 2016/06/02 ## author: yongxing.lyx
                        // ## BugID:8358496:hotseat will add a removed clone app
                        // when drop a clone app to hotseat.
                        ArrayList<ItemInfo> removeList = new ArrayList<ItemInfo>();
                        removeList.add(shortcutInfo);
                        bindItemsRemoved(removeList);
                        /* YUNOS END */
                        Map<String, String> param = new HashMap<String, String>();
                        param.put("PkgName", packageName);
                        int count = mAppCloneManager.getCloneCount(packageName);
                        param.put("delate_count", "1");
                        param.put("remain_count", Integer.toString(count));
                        UserTrackerHelper.sendUserReport(
                                UserTrackerMessage.MSG_APP_DUPLICATE_DELATE, param);
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (cell != null) {
                                    cell.setAlpha(1);
                                }
                                shortcutInfo.mIsUninstalling = false;
                                Toast.makeText(getApplicationContext(), R.string.delete_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    getWorkspace().checkAndRemoveEmptyCell();
                }

            };
            View.OnClickListener positivelistener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LauncherModel.clearDragInfo();
                    // if homeshell is system app, delete app, else start delete
                    // activity in system
                    if (AppCloneManager.isCloneShortcutInfo(shortcutInfo)) {
                        if (mAppCloneManager.delete(shortcutInfo.userId, shortcutInfo.getPackageName(), appCloneCallback)) {
                            if (info.container < 0) {
                                reVisibileDraggedItem(info, UNINSTALL_ICON_ALPHA);
                            }
                            if (isHideDeleteDialog()) {
                                mDeleteDialog.dismiss();
                            }
                            info.mIsUninstalling = true;
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (cell != null) {
                                        cell.setAlpha(1);
                                    }
                                    shortcutInfo.mIsUninstalling = false;
                                    Toast.makeText(getApplicationContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                            reVisibileDraggedItem(info, UNINSTALL_ICON_ALPHA);
                            mDeleteDialog.dismiss();
                        }
                    } else if (AppUtil.isSystemApp(LauncherApplication.getContext(), LauncherApplication.getContext().getPackageName())) {
                        if (info.container < 0) {
                            reVisibileDraggedItem(info, UNINSTALL_ICON_ALPHA);
                        }
                        if (mAppCloneManager.hasCloneBody(packageName)) {
                            Map<String, String> param = new HashMap<String, String>();
                            param.put("PkgName", packageName);
                            int count = mAppCloneManager.getCloneCount(packageName);
                            param.put("delate_count", Integer.toString(count));
                            param.put("remain_count", "0");
                            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_APP_DUPLICATE_DELATE, param);
                        }
                        PackageManager pm = getPackageManager();
                        ACA.PackageManager.deletePackage(pm, packageName, deleteObserver, 0);
                        if (isHideDeleteDialog()) {
                            mDeleteDialog.dismiss();
                        }
                        info.mIsUninstalling = true;
                    }else{
                        if (isHideDeleteDialog()) {
                            mDeleteDialog.dismiss();
                        }
                        Uri packageURI = Uri.parse("package:"+packageName);
                        Intent intent = new Intent(Intent.ACTION_DELETE,packageURI);
                        startActivity(intent);
                    }
                    /* YUNOS BEGIN */
                    // ##date:2014/09/7 ##author:xindong.zxd ##BugId:5225776
                    // send download cancel broadcast to the AppStore.
                    sendDownLoadCancelBroadcastToAppStore(shortcutInfo);
                    /*YUNOS END*/
                }
            };
            View.OnClickListener negativelistener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isHideDeleteDialog()) {
                        boolean animated = mIsStarted;
                        reVisibileDraggedItem(appInfo);
                        mDeleteDialog.dismiss(animated);
                    }
                }
            };

            Resources res = getResources();
            /* YUNOS BEGIN */
            // ##module(homeshell)
            // ##date 2013/12/31 ##auther yaodi.yd ##BugID:80857
            // To prevent a memory leak
            if (mDeleteDialog == null)
                mDeleteDialog = new DropDownDialog(this);
            PackageManager pm = getPackageManager();
            Bitmap iconSrc = dragBitmap;
            String title = "";
            try {
                title = pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString();
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            String hint;
            if (AppCloneManager.isCloneShortcutInfo(shortcutInfo)) {
                hint = getResources().getString(R.string.delete_profile_title);
            } else if (shortcutInfo != null
                    && mAppCloneManager.hasCloneBody(shortcutInfo.getPackageName())) {
                hint = getResources().getString(R.string.delete_and_uninstall_title);
            } else {
                hint = getResources().getString(R.string.uninstall_title);
            }
            mDeleteDialog.setTitle(hint, title);
            if(iconSrc != null) {
                // BugID:5239064:add original view parameter
                if (shortcutInfo.container == Favorites.CONTAINER_HIDESEAT) {
                    /* YUNOS BEGIN */
                    // ##date:2014/9/19 ##author:zhanggong.zg ##BugID:5244146
                    mDeleteDialog.setIcon(iconSrc, getHideseat().getDragView());
                    /* YUNOS END */
                } else {
                    if (getWorkspace().getDragInfo() != null) {
                        mDeleteDialog.setIcon(iconSrc, getWorkspace().getDragInfo().cell);
                    }
                }
            }
            /* YUNOS END */
            mDeleteDialog.setPositiveButton(res.getString(R.string.uninstall_app_confirm), positivelistener);
            mDeleteDialog.setNegativeButton(res.getString(R.string.uninstall_app_cancel), negativelistener);
            mDeleteDialog.setCanceledOnTouchOutside(false);
            mDeleteDialog.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    getSearchBar().hideDropTargetBar(false);
                    getSearchBar().getTrashAnimatorSet(false, true).start();
                    // ##date:2015/1/20 ##author:zhanggong.zg ##BugID:5706226
                    mModel.getPackageUpdateTaskQueue().releaseLock();
                }
            });
            mDeleteDialog.show();

            // ##date:2015/1/20 ##author:zhanggong.zg ##BugID:5706226
            mModel.getPackageUpdateTaskQueue().retainLock("DeleteDialog");
            /* YUNOS END */
        }
    }

    public void sendDownLoadCancelBroadcastToAppStore(final ShortcutInfo info) {
        if (info != null && info.getAppDownloadStatus() != AppDownloadStatus.STATUS_NO_DOWNLOAD) {
            String pkgName = info.intent.getComponent().getPackageName();
            Intent intent = new Intent(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
            intent.putExtra(AppDownloadManager.TYPE_ACTION, AppDownloadManager.ACTION_HS_DOWNLOAD_CANCEL);
            intent.putExtra(AppDownloadManager.TYPE_PACKAGENAME, pkgName);
            sendBroadcast(intent);
            LauncherApplication app = (LauncherApplication) getApplicationContext();
            app.getModel().getAppDownloadManager().updatepPckageDownloadCancelTimeByHS(pkgName);
        }
    }

    void cancelFolderDismiss(final FolderInfo folderInfo) {
        if (isHideDeleteDialog()) {
            mDeleteDialog.dismiss();
        }
        reVisibileDraggedItem(folderInfo);
    }

    void dismissFolder(final FolderInfo folderInfo, Bitmap src) {

        // BugID:5305301,wenliang.dwl
        // save FolderIcon before Workspace.mDragInfoDelete && Workspace.mDragInfo && CellLayout.mDragInfo
        // were changed in CellLayout.onInterceptTouchEvent
//        final FolderIcon folderIcon = (FolderIcon)getWorkspace().getDragInfo().cell;
        final FolderIcon folderIcon = (FolderIcon)getWorkspace().getDragItemFromList(folderInfo, false);

        View.OnClickListener positivelistener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LauncherModel.clearDragInfo();
                //check empty cell count first
                int emptyCellCount = 0;
                //BugID:5583238:icon lost during dismiss folder
                if (((folderInfo.screen == 0) && (folderInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP))
                   ||((folderInfo.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) && (mWorkspace.getCurrentPage() == 0))) {
                    emptyCellCount = LauncherModel.calcEmptyCell(0);
                } else {
                    emptyCellCount = LauncherModel.calcEmptyCell(1);
                }
                Log.d(TAG, "empty cell count is " + emptyCellCount);
                // BugID:5958197:Use UI occupy array find space, the icon place will be released when drag the foler.
                // So no use +1 any more.
                //BugID:5583388:one item lost when dismiss a folder in hotseat
//                if (folderInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
//                    emptyCellCount = emptyCellCount + 1;
//                }
                if (folderInfo.contents.size() > emptyCellCount) {
                    //not enough empty cell to take contents in folder
                    //don't dismiss the folder and recover the display
                    cancelFolderDismiss(folderInfo);
                    Toast.makeText(getApplicationContext(), R.string.folder_dismiss_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isHideDeleteDialog()) {
                    mDeleteDialog.dismiss();
                }
                mWorkspace.dismissFolder(folderIcon);
                mWorkspace.removeDragItemFromList(folderInfo);
                if(isContainerHotseat(folderInfo.container)){
                    getHotseat().onDrop(false, 0, null, null, true);
                }
                Map<String, String> param = new HashMap<String, String>();
                param.put("FolderName", folderInfo.title.toString());
                param.put("count", String.valueOf(folderInfo.contents.size()));
                param.put("operate_area", isInLauncherEditMode() ? "menu_arrage" : "launcher");
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_REMOVE_FOLDER, param);
            }
        };
        View.OnClickListener negativelistener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelFolderDismiss(folderInfo);
            }
        };

        if (mDeleteDialog == null)
            mDeleteDialog = new DropDownDialog(this);
        /* YUNOS BEGIN */
        // ## date: 2016/07/01 ## author: yongxing.lyx
        // ## BugID:8486706:default folder auto change title when language changed.
        String title = (String) folderInfo.getDisplayTitle(this);
        /* YUNOS END */
        String hint = getResources().getString(R.string.dismiss_folder_hint);
        mDeleteDialog.setTitle(hint, title);
        // BugID:5239064:add original view parameter
        mDeleteDialog.setIcon(src,getWorkspace().getDragInfo().cell);

        mDeleteDialog.setPositiveButton(getResources().getString(R.string.dismiss_folder_confirm), positivelistener);
        mDeleteDialog.setNegativeButton(getResources().getString(R.string.dismiss_folder_cancel), negativelistener);
        mDeleteDialog.setCanceledOnTouchOutside(false);
        mDeleteDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getSearchBar().hideDropTargetBar(false);
                getSearchBar().getTrashAnimatorSet(false, true).start();
            }
        });
        mDeleteDialog.show();
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/19 ##author:xiaodong.lxd
    //uninstall app from ShortcutInfo
    void startApplicationUninstallActivity(ShortcutInfo shortcutInfo, Bitmap dragBitmap) {

        boolean storgeMount = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        boolean appNotMounted = !storgeMount && shortcutInfo.isSDApp != 0;
        if(appNotMounted) {
            reVisibileDraggedItem(shortcutInfo);
            Toast.makeText(this, R.string.application_not_deleted_in_usb, Toast.LENGTH_SHORT).show();
            getSearchBar().getTrashAnimatorSet(false, false).start();
            return;
        }

        //convert shortCutInfo to ApplicationInfo
        if(shortcutInfo == null || shortcutInfo.intent == null || shortcutInfo.intent.getComponent() == null) {
            /* YUNOS BEGIN */
            // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
            //make sure delete it from db
            if (shortcutInfo != null) {
                Log.e(TAG, "sxsexe startApplicationUninstallActivity This shortcut has no intent??? " + shortcutInfo);
                LauncherModel.deleteItemFromDatabase(getApplicationContext(), shortcutInfo);
            }
            /* YUNOS END */
            return;
        }
        String packageName = shortcutInfo.intent.getComponent().getPackageName();
        final PackageManager packageManager = getApplicationContext().getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        ApplicationInfo appInfo = null;
        if(apps == null || apps.size() == 0) {
            // ##date:2014/10/15 ##author:zhanggong.zg ##BugID:5363183
            // make sure delete it from db if it's not a frozen app
            final boolean isFrozen = AppFreezeUtil.isPackageFrozen(getApplicationContext(), packageName);
            Log.d(TAG, "startApplicationUninstallActivity: failed to query appInfo." + " isFrozen=" + isFrozen + " shortcutInfo="
                    + shortcutInfo);

            /* YUNOS BEGIN */
            if (isFrozen) {
                // ##date:2015/1/12 ##author:zhanggong.zg
                // ##BugID:5244146,5698387
                // uninstall a frozen app
                appInfo = new ApplicationInfo(packageManager, shortcutInfo.intent.getComponent(), shortcutInfo.title.toString(), null);
            } else if (shortcutInfo.container >= 0 || shortcutInfo.container == Favorites.CONTAINER_HIDESEAT) {
                // ##date:2014/01/18 ##author:hao.liuhaolh ##BugID:83464
                // ##date:2014/08/19 ##author:zhanggong.zg ##BugID:5186578
                Log.d(TAG, "startApplicationUninstallActivity: delete from db directly");
                LauncherModel.deleteItemFromDatabase(getApplicationContext(), shortcutInfo);
            } else {
                HashSet<ComponentName> sets = new HashSet<ComponentName>();
                sets.add(shortcutInfo.intent.getComponent());
                mWorkspace.removeItemsByComponentName(sets);
            }
            /* YUNOS END */
            if (!isFrozen) {
                // BugID:5241055:delete sd card app icon no toast during sd card
                // unmounted
                mModel.removeComponentFormAllAppList(packageName);
                checkAndReplaceFolderIfNecessary(shortcutInfo);
                Toast.makeText(this, R.string.sd_app_icon_del, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "sxsexe startApplicationUninstallActivity why ResolveInfo is null??? pack " + packageName);
            }
            if (appInfo == null)
                return;
        }

        if (appInfo == null) {
            appInfo = new ApplicationInfo(packageManager, apps.get(0), null);
        }
        appInfo.container = shortcutInfo.container;
        appInfo.itemType = shortcutInfo.itemType;
        appInfo.id = shortcutInfo.id;
        appInfo.screen = shortcutInfo.screen;
        appInfo.cellX = shortcutInfo.cellX;
        appInfo.cellY = shortcutInfo.cellY;
        appInfo.spanX = shortcutInfo.spanX;
        appInfo.spanY = shortcutInfo.spanY;
        startApplicationUninstallActivity(appInfo, shortcutInfo, dragBitmap);
        DataCollector.getInstance(getApplicationContext()).collectDeleteAppData(shortcutInfo);
    }
    /* YUNOS END */

    boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Only launch using the new animation if the shortcut has not opted
            // out (this is a
            // private contract between launcher and may be ignored in the
            // future).
            boolean useLaunchAnimation = (v != null) && !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            if (useLaunchAnimation) {
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                int startX = -location[0]/2;
                int startY = -location[1]/2;
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, startX, startY, mScreenWidth/2 , mScreenHeight/2);
                //ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());

                startActivity(intent, opts.toBundle());
            } else {
                startActivity(intent);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent
                    + ". Make sure to create a MAIN intent-filter for the corresponding activity "
                    + "or use the exported attribute for this activity. " + "tag=" + tag + " intent=" + intent, e);
        }
        return false;
    }

    public boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        try {
            /* YUNOS BEGIN */
            // ## date: 2016/07/12 ## author: yongxing.lyx
            // ## BugID:8534609:don't start activity when the app is uninstalling.
            ShortcutInfo info = null;
            if (tag instanceof ShortcutInfo) {
                info = (ShortcutInfo) tag;
            }
            if (info != null && info.mIsUninstalling) {
                Toast.makeText(this, R.string.app_uninstalling, Toast.LENGTH_SHORT).show();
            /* YUNOS END */
            } else if (info != null && AppCloneManager.isCloneShortcutInfo(tag)) {
                success = mAppCloneManager.launch(info.userId, info.getPackageName());
            } else {
                success = startActivity(v, intent, tag);
            }
            /* YUNOS BEGIN */
            // ##date:2015/7/28 ##author:zhanggong.zg ##BugID:6222137
            // GadgetCardHelper.getInstance(this).onLaunchActivity(intent.getComponent());
            /* YUNOS END */
            // BugID:5695121:userTrack for card and and launcher stay time.
            // hao.liuhaolh
            sendLauncherStayTimeMsg(v, intent, tag);
        } catch (ActivityNotFoundException e) {
            ShortcutInfo info = null;
            int id = ToastManager.APP_NOT_FOUND;
            if (tag instanceof ShortcutInfo) {
                info = (ShortcutInfo) tag;
            }
            if (info != null) {
                if (info.isDownloading()) {
                    id = ToastManager.APP_IN_UPDATING;
                } else if (info.isSDApp > 0 && Utils.isInUsbMode()) {
                    id = ToastManager.APP_UNAVAILABLE_IN_USB;
                    /* YUNOS BEGIN */
                    // ##date:2014/9/16 ##author:zhanggong.zg ##BugID:5244146
                    // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
                    // ##date:2014/12/3 ##author:zhanggong.zg ##BugID:5602414
                } else if (Hideseat.isHideseatEnabled() && AppFreezeUtil.isPackageFrozen(getApplicationContext(), info)) {
                    if (info.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                        // The app has been moved out from hide-seat, but still
                        // in frozen state.
                        id = ToastManager.APP_UNAVAILABLE_BEING_UNFROZEN;
                        // In case of the unfreeze-broadcast is not sent
                        // correctly, send again here.
                        String pkgName = info.intent.getComponent().getPackageName();
                        AppFreezeUtil.asyncUnfreezePackage(getApplicationContext(), pkgName);
                    } else if (info.itemType == Favorites.ITEM_TYPE_SHORTCUT) {
                        // The original app is frozen
                        id = ToastManager.SHORTCUT_UNAVAILABLE_DUE_TO_FROZEN;
                    } else {
                        id = ToastManager.APP_NOT_FOUND;
                    }
                    /* YUNOS END */
                } else {
                    id = ToastManager.APP_NOT_FOUND;
                }
            }
            ToastManager.makeToast(id);
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent
                    + ". Make sure to create a MAIN intent-filter for the corresponding activity "
                    + "or use the exported attribute for this activity.", e);
        }
    }

    /* YUNOS BEGIN author xiaodong.lxd */
    // for bug 69380 2012-12-02
    public boolean isContainerFolder(long container) {
        return container != LauncherSettings.Favorites.CONTAINER_DESKTOP && container != LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
        /* YUNOS BEGIN */
        // ##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        // add hide icon container
                container != LauncherSettings.Favorites.CONTAINER_HIDESEAT;
        /* YUNOS END */
    }
    /* YUNOS END */

    private boolean isContainerHideseat(long container) {
        return container == LauncherSettings.Favorites.CONTAINER_HIDESEAT;
    }

    public boolean isContainerHotseat(long container) {
        return container == LauncherSettings.Favorites.CONTAINER_HOTSEAT;
    }

    public boolean isContainerWorkspace(long container) {
        return container == LauncherSettings.Favorites.CONTAINER_DESKTOP;
    }

    private void handleFolderClick(FolderIcon folderIcon) {
        // fullscreen folder do not support dual-opening folder
        if (mFolderUtils.isFolderOpened())
            return;
        /* YUNOS BEGIN */
        // ##author:xiangnan.xn@alibaba-inc.com
        // ##BugID:7937582 ##date:2015/03/01
        // stop Flip process before open a folder
        if (mFlipAnim.isShowing()) {
            stopFlipWithoutAnimation();
        }
        /* YUNOS END */
        final FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);

        Log.d(TAG, "sxsexe-----> handleFolderClick openFolder " + openFolder + " mFolder " + mFolder + " info.opened " + info.opened);

        // If the folder info reports that the associated folder is open, then
        // verify that
        // it is actually opened. There have been a few instances where this
        // gets out of sync.
        if (info.opened && openFolder == null) {
            Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: " + info.screen + " (" + info.cellX + ", "
                    + info.cellY + ")");
            info.opened = false;
        }
        Log.d(TAG, "opened:" + info.opened + "  destroyed:" + folderIcon.getFolder().isDestroyed());
        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            Log.d(TAG, "1:");
            // Close any open folder
            /* YUNOS BEGIN */
            // ##date:2014/3/10 ##author:yaodi.yd
            // closeFolder();
            closeFolderWithoutExpandAnimation();
            /* YUNOS END */
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                Log.d(TAG, "2:" + openFolder.toString());
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    /* YUNOS BEGIN */
                    // ##date:2014/3/10 ##author:yaodi.yd
                    // closeFolder();
                    closeFolderWithoutExpandAnimation();
                    /* YUNOS END */
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            } else if (mFolder != null) {
                /* YUNOS BEGIN author xiaodong.lxd */
                Log.d(TAG, "3:" + mFolder.toString());
                /* YUNOS BEGIN */
                // ##date:2014/3/10 ##author:yaodi.yd
                // closeFolder();
                closeFolderWithoutExpandAnimation();
                /* YUNOS END */
                openFolder(folderIcon);
                // openFolder(mFolder.getmFolderIcon());
                /* YUNOS END */
            }
        }
    }

    public void openFolder(final FolderIcon folderIcon) {
        openFolder(folderIcon, null);
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the
     * folder is animated relative to the specified View. If the View is null,
     * no animation is played.
     *
     * @param folderInfo
     *            The FolderInfo describing the folder to open.
     * @param finish
     *            An optional callback that will be invoked when the animation
     *            is done.
     */
    public void openFolder(final FolderIcon folderIcon, final Runnable finish) {

        //if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
        //  FrameLayout  globalSearchBox = (FrameLayout)findViewById(R.id.GlobalSearch);
        //  globalSearchBox.setVisibility(View.GONE);
        //}
        setGlobalSearchVisibility(View.GONE);
        /* YUNOS BEGIN */
        // ##module(FolderAnimation)
        // ##date:2014/03/30 ##author:yaodi.yd ##BugID:105911
        // when page moving,it can not open and play folder's animation
        if (getWorkspace().isPageMoving())
            return;
        /* YUNOS END */
        /* YUNOS BEGIN */
        // ##date:2014/06/03 ##author:yangshan.ys
        // batch operations to the icons in folder
        FolderUtils.addEditFolderShortcut(this, folderIcon);
        /* YUNOS END */
        Runnable openRun = new Runnable() {
            @Override
            public void run() {
                Folder folder = folderIcon.getFolder();
                FolderInfo info = folder.mInfo;

                info.opened = true;

                // Just verify that the folder hasn't already been added to the
                // DragLayer.
                // There was a one-off crash where the folder had a parent
                // already.
                if (folder.getParent() == null) {
                    mDragLayer.addView(folder);
                    mDragController.addDropTarget((DropTarget) folder);
                    // add by dongjun for folder Gaussian blur begin
                    /* YUNOS BEGIN */
                    // ##date:2014/2/14 ##author:yaodi.yd
                    // delete this code for new folder animation
                    // ForstedGlassUtils.setForstedGlassBackground(Launcher.this);
                    /* YUNOS END */
                    // mWorkspace.setVisibility(View.GONE);
                    // mHotseat.setVisibility(View.GONE);
                    // mIndicatorView.setVisibility(View.GONE);
                    // add by dongjun for folder Gaussian blur end
                } else {
                    Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" + folder.getParent() + ").");
                }
                /* YUNOS BEGIN */
                // ##date:2014/2/14 ##author:yaodi.yd
                // added new animation for folder
                // folder.animateOpen();
                mFolderUtils.animateOpen(folder, finish);
                /* YUNOS END */
                /* YUNOS BEGIN */
                // ##date:2013/11/27 ##author:zhangqiang.zq
                // modify folder display

                // growAndFadeOutFolderIcon(folderIcon);

                /* YUNOS END */

                // Notify the accessibility manager that this folder "window"
                // has appeared and occluded
                // the workspace items
                folder.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
            }
        };

        if (isHideseatShowing()) {
            hideHideseat(false);
            mHandler.post(openRun);
        } else {
            openRun.run();
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/1/9 ##author:zhangqiang.zq
    // screen edit
    public boolean isInEditScreenMode() {
        return mScreenEditMode;
    }

    public void exitScreenEditMode(boolean animate) {
        if (mScreenEditMode) {
            UserTrackerHelper.entryPageEnd(UserTrackerMessage.LABEL_SCREEN_MANAGER);

            if (mScreenManager != null) {
                mScreenManager.stop(animate, Boolean.valueOf(animate));
            }
        }
    }

    public void exitScreenEditModeWithoutSave() {
        exitScreenEditMode(false);
    }

    public void enterScreenEditMode() {
        /* YUNOS BEGIN */
        // module(HomeShell)
        // ##date:2014/03/27 ##author:yaodi.yd ##BugID:105225
        // it can not enter Screen edit Mode,
        // When the dialog is displayed
        if (mDeleteDialog != null && mDeleteDialog.isShowing()) {
            return;
        }
        /* YUNOS END */

        if (mWorkspaceLoading || mWaitingForResult) {
            // BugID: 105599: we should prevent entering
            // screen editor when workspace is locked
            return;
        }

        /* YUNOS BEGIN */
        // ##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (AgedModeUtil.isAgedMode()) {
            return;
        }
        /* YUNOS END */

        // BugID:6416230
        if (mWorkspace.getIconScreenCount() <= 1) {
            return;
        }

        if (!mScreenEditMode) {
            if (FeatureUtility.hasFullScreenWidget()) {
                mWorkspace.removeWidgetPages();
            }
            int currentScreenIndex = mWorkspace.getCurrentPage();
            CellLayout currentlayout = (CellLayout) mWorkspace.getChildAt(currentScreenIndex);

            // BugID:100157
            if (currentlayout == null) {
                Log.d(TAG, "ERROR:current page is null!!!");
                return;
            }

            mScreenEditMode = true;
            mWorkspace.setScrollingEnable(false);

            mModel.getPackageUpdateTaskQueue().retainLock("ScreenEditMode");

            UserTrackerHelper.entryPageBegin(UserTrackerMessage.LABEL_SCREEN_MANAGER);
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_SCREEN_MANAGER);

            if (isHideseatShowing()) {
                hideHideseat(false);
            }

            if (mCardBridge != null) {
                mCardBridge.hideLeftScreenGuide();
            }

            for (int i = 0; i < mWorkspace.getIconScreenCount(); i++) {
                CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getRealScreenIndex(i));
                if (cellLayout != null) {
                    cellLayout.cancelFlingDropDownAnimation();
                }
            }

            mIndicatorView.setVisibility(View.GONE);
            mHotseat.setVisibility(View.GONE);
            if (mLandscapeDivider != null) {
                mLandscapeDivider.setVisibility(View.GONE);
            }
            if (mScreenManager == null) {
                mScreenManager = new ScreenManager(this);

                getDragLayer().addView(mScreenManager.getRootView(),
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                mScreenManager.setScreenManagerListener(new ScreenManager.ScreenManagerListener() {

                    @Override
                    public void onExit(Object stopTag) {
                        mScreenEditMode = false;
                        if ((Boolean) stopTag) {
                            saveScreenChange();
                        }
                        if (FeatureUtility.hasFullScreenWidget()) {
                            mWorkspace.makeSureWidgetPages();
                        }
                        mModel.getPackageUpdateTaskQueue().releaseLock();
                        mWorkspace.setVisibility(View.VISIBLE);
                        mHotseat.setVisibility(View.VISIBLE);
                        mIndicatorView.setVisibility(View.VISIBLE);
                        mWorkspace.setScrollingEnable(true);
                        if (mLandscapeDivider != null) {
                            mLandscapeDivider.setVisibility(View.VISIBLE);
                        }

                        if (mCardBridge != null) {
                            mCardBridge.showLeftScreenGuide();
                        }
                    }

                    @Override
                    public void onItemClick(int index) {
                        if (mScreenEditMode) {
                            UserTrackerHelper.entryPageEnd(UserTrackerMessage.LABEL_SCREEN_MANAGER);

                            if (mScreenManager != null) {
                                mScreenManager.stop(Boolean.TRUE, index);
                            }
                        }
                    }
                });
            }

            if (!mScreenManager.start()) {
                mWorkspace.setScrollingEnable(true);
                mScreenEditMode = false;
                return;
            }
            mScreenManager.getRootView().setVisibility(View.VISIBLE);
        }
    }

    private ScreenManager mScreenManager;

    private void saveScreenChange() {
        if (mScreenManager != null) {
            List<Integer> newIndexs = mScreenManager.getNewIndexs();
            int currentPage = mScreenManager.getCurrentPage();
            screenExchange(newIndexs, currentPage);
            LauncherModel.reArrageScreen(Launcher.this, newIndexs);
            Map<String, String> param = new HashMap<String, String>();
            param.put(UserTrackerMessage.Key.SCREEN, newIndexs.toString());
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_DRAG_SCREEN_RESULT, param);
        }
    }

    private void screenExchange(final List<Integer> newIndexs, int currentScreen) {
        if (newIndexs == null) {
            return;
        }

        int count = newIndexs.size();
        CellLayout[] layouts = new CellLayout[count];

        for (int i = count - 1; i >= 0; i--) {
            layouts[i] = (CellLayout) mWorkspace.getChildAt(mWorkspace.getRealScreenIndex(i));
            mWorkspace.removeViewAt(mWorkspace.getRealScreenIndex(i));
        }

        for (int i = 0; i < count; i++) {
            int j = newIndexs.get(i);
            mWorkspace.addView(layouts[j]);
        }

        mWorkspace.setCurrentPage(mWorkspace.getRealScreenIndex(currentScreen));
        mIndicatorView.setCurrentPos(mWorkspace.getRealScreenIndex(currentScreen));
    }

    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2014/3/10 ##author:yaodi.yd
    public void closeFolderWithoutExpandAnimation() {
        closeFolderWithoutExpandAnimation(true);
    }
    public void closeFolderWithoutExpandAnimation(boolean anim) {
        //if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
        //  FrameLayout  globalSearchBox = (FrameLayout)findViewById(R.id.GlobalSearch);
        //  globalSearchBox.setVisibility(View.VISIBLE);
        //}
        if(!AgedModeUtil.isAgedMode() && !mDragController.isDragging()){
             setGlobalSearchVisibility(View.VISIBLE);
        }
        final Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            /* YUNOS BEGIN */
            // ##date:2014/06/03 ##author:yangshan.ys
            // batch operations to the icons in folder
            folder.closeSelectApps();
            FolderUtils.removeEditFolderShortcut(folder.getInfo());
            /* YUNOS END */

            /* YUNOS BEGIN */
            // ##date:2014/1/13 ##author:zhangqiang.zq
            // BugID: 102038
            // YUNOS BEGIN
            // ##date:2014/09/16 ##author:hongchao.ghc ##BugID:5236424
            folder.hideSoftInputMethod(null);
            // YUNOS END
            /* YUNOS END */

            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            folder.getInfo().opened = false;
            mFolderUtils.animateClosed(folder, false, anim);
            // Notify the accessibility manager that this folder "window" has
            // disappeard and no
            // longer occludeds the workspace items
            getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    }
    /* YUNOS END */

    public void closeFolder() {
        closeFolder(0);
    }
    public void closeFolder(int delay) {
        final Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            folder.dismissEditingName();
            // hide IME will cause window animation so we close folder some ms
            // delay
            if (delay > 0) {
                mWorkspace.postDelayed(new Runnable() {
                    public void run() {
                        closeFolder(folder);
                    }
                }, delay);
            } else {
                closeFolder(folder);
            }
        }
    }

    void closeFolder(final Folder folder) {
        //if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)){
        //  FrameLayout  globalSearchBox = (FrameLayout)findViewById(R.id.GlobalSearch);
        //  globalSearchBox.setVisibility(View.VISIBLE);
        //}
        if(!AgedModeUtil.isAgedMode() && !mDragController.isDragging()){
           setGlobalSearchVisibility(View.VISIBLE);
        }
        /* YUNOS BEGIN */
        // ##date:2014/06/03 ##author:yangshan.ys
        // batch operations to the icons in folder
        // YUNOS BEGIN
        // ##date:2014/09/17 ##author:hongchao.ghc ##BugID:5239939
        folder.getEditTextRegion().clearFocus();
        // YUNOS END
        folder.closeSelectApps();
        // FolderUtils.removeEditFolderShortcut(folder.getInfo());
        /* YUNOS END */
        folder.getInfo().opened = false;

        // ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        // if (parent != null) {
        // FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
        // shrinkAndFadeInFolderIcon(fi);
        // }

        /* YUNOS BEGIN */
        // ##date:2014/06/03 ##author:kerong.skr
        mFolderUtils.animateClosed(folder, true, getWindow() != null);
        /* YUNOS END */
        // add by dongjun for folder Gaussian blur begin & removed by yaodi.yd
        // this.mHandler.post(new Runnable(){
        // public void run(){
        // mWorkspace.setVisibility(View.VISIBLE);
        // mHotseat.setVisibility(View.VISIBLE);
        // mIndicatorView.setVisibility(View.VISIBLE);
        // ForstedGlassUtils.clearForstedGlassBackground(Launcher.this);
        // }
        // });
        // add by dongjun for folder Gaussian blur end & removed by yaodi.yd

        // Notify the accessibility manager that this folder "window" has
        // disappeard and no
        // longer occludeds the workspace items
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        // YUNOS BEGIN
        // ##date:2014/09/17 ##author:hongchao.ghc ##BugID:5236424
        mWorkspace.postDelayed(new Runnable() {
            public void run() {
                folder.hideSoftInputMethod(Launcher.this);
            }
        }, 200);
        // YUNOS END
    }

    public boolean onLongClick(View v) {
        if (isInLeftScreen() || isInWidgetPage())
            return false;
        // added by wenliang.dwl, avoid longclick when doing animation
        if (LockScreenAnimator.getInstance(this).shouldPreventGesture())
            return false;
        if (mGestureLayer.getPointerCount() > 1)
            return false;

        if (!isDraggingEnabled())
            return false;
        /* YUNOS BEGIN */
        // ##date:2013/11/29 ##author:hongxing.whx ##bugid: 68388
        // add toast in lock mode
        /*
         * if (mModel.isDownloadStatus()) {
         * ToastManager.makeToast(ToastManager.NOT_ALLOW_EDIT_IN_DOWNING);
         * //return true; }
         */
        /* YUNOS END */

        Log.d(TAG, "onLongClick mWorkspaceLoading || mWaitingForResult || mModel.isDownloadStatus() " + mWorkspaceLoading + " - "
                + mWaitingForResult + " - " + mModel.isDownloadStatus());
        if (mWorkspaceLoading || mWaitingForResult)
            return false;
        Log.d(TAG, "onLongClick mState " + mState);
        if (mState != State.WORKSPACE)
            return false;
        boolean dragWidgetAndGadgetInHideSeatMode = isHideseatShowing() &&
                (v instanceof GadgetView || v instanceof LauncherAppWidgetHostView);
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: header in cell layout
        boolean isHeader = false;
        if (v.getId() == R.id.header) {
            isHeader = true;
        }
        /* YUNOS END */
        //BugID:108338:getParent is not instanceof View
        if (!(v instanceof CellLayout)) {
            do{
                ViewParent p = v.getParent();
                if( p == null || !(p instanceof View)) {
                    Log.d(TAG,"onLongClick getParent : " + p );
                    return true;
                }
                v = (View)p;
            }while( !(v instanceof CellLayout));
        }

        resetAddInfo();
        CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
        // This happens when long clicking an item with the dpad/trackball
        if (longClickCellInfo == null) {
            return true;
        }

        // The hotseat touch handling does not go through Workspace, and we
        // always allow long press
        // on hotseat items.
        final View itemUnderLongClick = longClickCellInfo.cell;
        boolean allowLongPress = dragWidgetAndGadgetInHideSeatMode || (isHotseatLayout(v) || (mWorkspace != null && mWorkspace.allowLongPress()));
        if (allowLongPress && !mDragController.isDragging()) {
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: dragging screen
            if (mLauncherEditMode && isHeader) {
                mWorkspace.startDragScreenInEditMode();
                return true;
            }
            /* YUNOS END */
            if (itemUnderLongClick == null || dragWidgetAndGadgetInHideSeatMode) {
                // User long pressed on empty space
                // /*YUNOS BEGIN*/
                // //##date:2013/11/15 ##author:xiaodong.lxd
                // //just go to widget list
                // UserTrackerHelper
                // .entryPageBegin(UserTrackerMessage.LABEL_WIDGET_LOADER);
                // UserTrackerHelper
                // .sendUserReport(UserTrackerMessage.MSG_ENTRY_WIDGET_LOADER);
                // showAllApps(true);
                //startWallpaper();
                /*YUNOS BEGIN*/
                //##2015-06-19 BugID:6096098 ##author:chenliang.cl
                if (HomeShellSetting.getFreezeValue(this)) {
                    Toast.makeText(this, R.string.aged_freeze_homeshell_toast, Toast.LENGTH_SHORT).show();
                } else {
                    if (!mLauncherEditMode) {
                        if (isHideseatShowing()) {
                            hideHideseat(false);
                        }
                        postRunnableToMainThread(new Runnable() {
                            @Override
                            public void run() {
                                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                                enterLauncherEditMode();
                                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_PRESS_BLANK);
                            }
                        }, 50);
                    }
                }
                /*YUNOS END*/
            } else {
                /*YUNOS BEGIN*/
                //##date:2014/7/8 ##author:zhangqiang.zq
                // aged mode
                if (HomeShellSetting.getFreezeValue(this)) {
                    /*YUNOS BEGIN*/
                    //##2015-03-16 BugID:5568655
                    boolean toastShow = true;
                    if(CheckVoiceCommandPressHelper.isPushTalkCanUse()) {
                        Object object = itemUnderLongClick.getTag();
                        if(object  instanceof ShortcutInfo) {
                            toastShow = !CheckVoiceCommandPressHelper.getInstance().isVoiceApp((ShortcutInfo) object);
                        }
                    }
                    if(toastShow) {
                        Toast.makeText(this, R.string.aged_freeze_homeshell_toast, Toast.LENGTH_SHORT).show();
                    }
                    /*YUNOS END*/
                    return true;
                }
                /*YUNOS END*/

                //edit by dongjun for traffic panel
                if (!(itemUnderLongClick instanceof Folder)) {
                    //BugID:6637832:the app in uninstall can't drag
                    ItemInfo item =  (ItemInfo) itemUnderLongClick.getTag();
                    if (item != null && item.itemType == Favorites.ITEM_TYPE_APPLICATION && ((ShortcutInfo)item).mIsUninstalling ) {
                        Toast.makeText(this, R.string.app_uninstalling, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (isInLauncherEditMode()) {
                        /* YUNOS BEGIN */
                        // ## modules(Home Shell)
                        // ## date: 2016/03/10 ## author: wangye.wy
                        // ## BugID: 7945871: item of icon sort
                        if (mEditModePreviewContainer.getContentType() == PreviewContentType.IconSort) {
                            mEditModeHelper.switchFromSortMenuToEmContainer();
                        /* YUNOS END */
                        } else {
                            mEditModeHelper.switchFromMenuToEmContainer();
                        }
                        mEditModePreviewContainer.setContentType(PreviewContentType.CellLayouts);
                    }
                    Log.d(TAG, "onLongClick itemUnderLongclick not instanceof Folder");
                    // User long pressed on an item
                    Set<View> views = mWorkspace.getSelectedViewsInLayout();
                    if (isInLauncherEditMode() && (views.size() > 1 || (views.size() == 1 && !views.contains(longClickCellInfo.cell)))) {
                        mWorkspace.startDragInEditMode(longClickCellInfo, mWorkspace);
                    } else {
                        mWorkspace.startDrag(longClickCellInfo, mWorkspace);
                    }
                }
            }
        }
        return true;
    }

    public boolean isHotseatLayout(View layout) {
        return mHotseat != null && mHotseat.getVisibility() == View.VISIBLE && layout != null && (layout instanceof CellLayout)
                && (layout == mHotseat.getLayout());
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public TextView getEditmodeTipsView() {
        return mEditModeTips;
    }

    public CellLayout getLifeCenterCellLayout() {
        return mLifeCenterCellLayout;
    }

    public Hideseat getHideseat() {
        return mHideseat;
    }

    public CustomHideseat getCustomHideseat() {
        return mCustomHideseat;
    }

    SearchDropTargetBar getSearchBar() {
        return mSearchDropTargetBar;
    }

    /**
     * Returns the CellLayout of the specified container at the specified
     * screen.
     */
    CellLayout getCellLayout(long container, int screen) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        }
        /*YUNOS BEGIN*/
        //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
        //add hide icon container
        else if (container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
            //TODO: liuhao need mHideseat
            return null;
        }
        /*YUNOS END*/
        else {
            return (CellLayout) mWorkspace.getChildAt(mWorkspace.getRealScreenIndex(screen));
        }
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    // Now a part of LauncherModel.Callbacks. Used to reorder loading steps.
    @Override
    public boolean isAllAppsVisible() {
        return /*
                * (mState == State.APPS_CUSTOMIZE) || (mOnResumeState ==
                * State.APPS_CUSTOMIZE);
                */false;
    }

    @Override
    public boolean isAllAppsButtonRank(int rank) {
        return /*mHotseat.isAllAppsButtonRank(rank)commented by xiaodong.lxd*/ false;
    }

    private void setWorkspaceBackground(boolean workspace) {
        mLauncherView.setBackground(workspace ? mWorkspaceBackgroundDrawable : null);
    }

    void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
        setWorkspaceBackground(visible);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            if (mDeleteDialog == null || !mDeleteDialog.isShowing()) {
                if (isHideseatShowing()) {
                    hideHideseat(false);
                }
            } // When another window occludes launcher (like the notification
              // shade, or recents),
              // ensure that we enable the wallpaper flag so that transitions
              // are done correctly.
            updateWallpaperVisibility(true);
            // if(mMenu!=null&&mMenu.isShowing()){
            // mMenu.dismiss();
            // }
        } /*
           * else { // When launcher has focus again, disable the wallpaper if
           * we are in AllApps mWorkspace.postDelayed(new Runnable() {
           *
           * @Override public void run() { disableWallpaperIfInAllApps(); } },
           * 500); }
           */

        if (!hasFocus) {
            // added by wenliang.dwl, restore icons position when lose window
            // focus
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/28 ## author: wangye.wy
            // ## BugID: 8035785: restore delayed for waiting for lock screen enter
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LockScreenAnimator.getInstance(Launcher.this).restoreIfNeeded();
                }
            }, 300);
            /* YUNOS END */
        }
    }

    public void showWorkspace(boolean animated) {
        showWorkspace(animated, null);
    }

    void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        if (mState != State.WORKSPACE) {
            mWorkspace.setVisibility(View.VISIBLE);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        mWorkspace.flashScrollingIndicator(animated);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        /* YUNOS BEGIN */
        // ##date:2014/02/21  ##author: chao.jiangchao ##BugId: 93596
        mIsAllAppShowed = false;
        /* YUNOS END */
        setOnClickValid(true);
    }

    void showAllApps(boolean animated) {
        if (isHideseatShowing()) {
            hideHideseat(false);
        }
        if (mState != State.WORKSPACE)
            return;

        // Change the state *after* we've called all the transition code
        mState = State.APPS_CUSTOMIZE;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        /* YUNOS BEGIN */
        // ##date:2014/3/10 ##author:yaodi.yd
        // closeFolder();
        closeFolderWithoutExpandAnimation();
        /* YUNOS END */

        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);

        /* YUNOS BEGIN */
        // ##date:2014/02/21 ##author: chao.jiangchao ##BugId: 93596
        mIsAllAppShowed = true;
        /* YUNOS END */
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    /**
     * Shows the hotseat area.
     */
    void showHotseat(boolean animated) {
        View hotseat = (View) getHotseat();
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (hotseat.getAlpha() != 1f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionInDuration();
                    }
                    hotseat.animate().alpha(1f).setDuration(duration);
                }
            } else {
                hotseat.setAlpha(1f);
            }
        }
    }

    /**
     * Hides the hotseat area.
     */
    void hideHotseat(boolean animated) {
        View hotseat = (View)getHotseat();
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (hotseat.getAlpha() != 0f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionOutDuration();
                    }
                    hotseat.animate().alpha(0f).setDuration(duration);
                }
            } else {
                hotseat.setAlpha(0f);
            }
        }
    }

    /**
     * Add an item from all apps or customize onto the given workspace screen.
     * If layout is null, add to the current screen.
     */
    void addExternalItemToScreen(ItemInfo itemInfo, final CellLayout layout) {
        if (!mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
    }

    /**
     * Maps the current orientation to an index for referencing orientation
     * correct global icons
     */
    // private int getCurrentOrientationIndexForGlobalIcons() {
    // // default - 0, landscape - 1
    // switch (getResources().getConfiguration().orientation) {
    // case Configuration.ORIENTATION_LANDSCAPE:
    // return 1;
    // default:
    // return 0;
    // }
    // }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS_CUSTOMIZE) {
            text.add(getString(R.string.all_apps_button_label));
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * Receives notifications when click search widget ten times.
     */
    private class EnableCloudCardReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    /* YUNOS BEGIN */
    // ##date:2013/12/09  ##author: hongxing.whx ##BugId: 60497
    // For wallpaper changed
    /**
     * Receives WALLPAPER_CHANGED intent
     */
    private class WallpaperChangedIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "ACTION_WALLPAPER_CHANGED");
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            // ##date:2014/03/26  ##author: hongxing.whx ##BugId: 104985
            // forcely make height larger than width
            final int wallpaperWidth =  (dm.widthPixels > dm.heightPixels) ? dm.heightPixels : dm.widthPixels;
            final int wallpaperHeight = (dm.widthPixels < dm.heightPixels) ? dm.heightPixels : dm.widthPixels;
            final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            new Thread("setWallpaperDimension") {
                public void run() {
                    wallpaperManager.suggestDesiredDimensions(wallpaperWidth, wallpaperHeight);
                }
            }.start();

            // support card icon, not need to adapt icon title color.
            updateTitleColor();
        }
    }
    /* YUNOS END */
    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }

    /**
     * If the activity is currently paused, signal that we need to run the
     * passed Runnable in onResume.
     *
     * This needs to be called from incoming places where resources might have
     * been loaded while we are paused. That is becaues the Configuration might
     * be wrong when we're not running, and if it comes back to what it was when
     * we were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused. The caller might be able to skip
     *         some work in that case since we will come back again.
     */
    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            Log.i(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mOnResumeCallbacks.remove(run)) {
                }
            }
            mOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        /*YUNOS BEGIN*/
        //##date:2013/12/03 ##author:hao.liuhaolh BugID:69836
        //no waiting for onResume
        return false;
        //return waitUntilResume(run, false);
        /*YUNOS END*/
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the
     * loader in onResume.
     *
     * This needs to be called from incoming places where resources might have
     * been loaded while we are paused. That is becaues the Configuration might
     * be wrong when we're not running, and if it comes back to what it was when
     * we were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused. The caller might be able to skip
     *         some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            Log.i(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        // If we're starting binding all over again, clear any bind calls we'd
        // postponed in
        // the past (see waitUntilResume) -- we don't need them since we're
        // starting binding
        // from scratch again
        mOnResumeCallbacks.clear();

        final Workspace workspace = mWorkspace;
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        mWorkspace.clearReference();
        int count = workspace.getIconScreenCount();
        int offSet = workspace.getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout()
            // and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i + offSet);
            layoutParent.removeAllViewsInLayout();
        }

        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(163418) ##date:2014/08/15
        // ##description: Added support for widget page
        if (FeatureUtility.hasFullScreenWidget()) {
            for (int i = count - 1; i >= 0; i--) {
                final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
                if (layoutParent.isWidgetPage()) {
                    workspace.bindWidgetPage(i);
                }
            }
        }
        // YUNOS END PB

        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
        if (mHideseat != null) {
            mHideseat.resetLayout();
        }
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindItems(shortcuts, start, end);
                }
            })) {
            return;
        }
        // Get the list of added shortcuts and intersect them with the set of
        // shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = mSharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);
        Workspace workspace = mWorkspace;
        /* YUNOS BEGIN */
        // ##date:2014/07/22 ##author:hongchao.ghc ##BugID:138994
        if (workspace == null) {
            return;
        }
        /* YUNOS END */
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);
            // Short circuit if we are loading dock items for a configuration
            // which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT && mHotseat == null) {
                continue;
            }
            /*YUNOS BEGIN*/
            //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
            //add hide icon container
            if (item.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                //TODO: liuhao need mHideseat
                //continue;
            }
            /* YUNOS END */

            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP && item.screen == 0) {
                mWorkspace.invalidatePageIndicator(true);
            }
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    //String uri = info.intent.toUri(0).toString();
                    /* YUNOS BEGIN */
                    // ##date:2015/1/15 ##author:zhanggong.zg ##BugID:5705812
                    if (Hideseat.containsSamePackageOf(info) && mHideseat!= null) {
                        mHideseat.bindItemInHideseat(info);
                    /* YUNOS END */
                    } else {
                        View shortcut = createShortcut(info);
                        workspace.addInScreen(shortcut, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                        boolean animateIconUp = false;
                        // cancel animate. single shortcut can't be displayed if
                        // animate is true
                        /*
                         * synchronized (newApps) { if (newApps.contains(uri)) {
                         * animateIconUp = newApps.remove(uri); } }
                         */
                        if (animateIconUp) {
                            // Prepare the view to be animated up
                            shortcut.setAlpha(0f);
                            shortcut.setScaleX(0f);
                            shortcut.setScaleY(0f);
                            mNewShortcutAnimatePage = item.screen;
                            if (!mNewShortcutAnimateViews.contains(shortcut)) {
                                mNewShortcutAnimateViews.add(shortcut);
                            }
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER :
                    FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (FolderInfo) item);
                    workspace.addInScreen(newFolder, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                    break;
                /* YUNOS BEGIN */
                // ##gadget
                // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com
                // ##BugID:96378
                case LauncherSettings.Favorites.ITEM_TYPE_GADGET :
                    GadgetItemInfo gadgetInfo = (GadgetItemInfo) item;
                    View gadget = LauncherGadgetHelper.getGadget(this, gadgetInfo.gadgetInfo);
                    if (gadget == null) {
                        Log.e(TAG, "failed get gadget " + gadgetInfo.title);
                        break;
                    }
                    gadget.setTag(gadgetInfo);
                    gadget.setOnLongClickListener(this);
                    workspace.addInScreen(gadget, item.container, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
                    break;
            /* YUNOS END */
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                    ShortcutInfo shortcutinfo = (ShortcutInfo) item;
                    View shortcutview = createShortcut(shortcutinfo);
                    workspace.addInScreen(shortcutview, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);

                    break;
            /* YUNOS BEGIN */
            // ##date:2014/02/19 ##author:hao.liuhaolh ##BugID:92481
            // vp install
            // remove vp install
            // case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
            // ShortcutInfo vpinfo = (ShortcutInfo) item;
            // View vpshortcut = createShortcut(vpinfo);
            // workspace.addInScreen(vpshortcut, item.container, item.screen,
            // item.cellX,
            // item.cellY, 1, 1, false);
            // break;
            /* YUNOS END */
            }
        }

        workspace.requestLayout();
    }

    private Handler mbindHandler = new Handler(Looper.getMainLooper());

    // BugID:5211661:anr in theme change
    @Override
    public void bindItemsChunkUpdated(ArrayList<ItemInfo> items, int start, int end, final boolean themeChange) {
        Log.d(TAG, "Launcher : bindItemsChunkUpdated begin");
        // BugID:5254092:icon update failed during screen update
        if (mScreenEditMode) {
            Log.d(TAG, "Animation isplaying");
            if (Utils.isLoadingDialogShowing()) {
               exitScreenEditModeWithoutSave();
            }
            final ArrayList<ItemInfo> finalitems = new ArrayList<ItemInfo>(items);
            final int finalstart = start;
            final int finalend = end;
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    bindItemsChunkUpdated(finalitems, finalstart, finalend, themeChange);
                }
            };
            mbindHandler.postDelayed(r, 500);
            return;
        }
        /* YUNOS BEGIN */
        // ## date: 2016/08/16 ## author: yongxing.lyx
        // ## BugID:8410785:sometimes loading dailog would be dismissed after changed theme
        Utils.dismissLoadingDialog();
        if (items == null || mWorkspace == null) {
            Log.e(TAG, "bindItemsChunkUpdated() failed! items != null ? " + (items != null)
                    + ", mWorkspace != null ? " + (mWorkspace != null));
            return;
        }
        /* YUNOS END */

        EditModeHelper.setChangeThemeFromeHomeShell(false);
        final Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++){
            ItemInfo iteminfo = items.get(i);

            CellLayout layout = null;
            if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                layout = (CellLayout) getHotseat().getLayout();
            }else if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                layout = (CellLayout)getHideseat().getChildAt(iteminfo.screen);
            }else if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                layout = (CellLayout) workspace.getChildAt(mWorkspace.getRealScreenIndex(iteminfo.screen));
            } else {
                try {
                    FolderInfo folder = sFolders.get(iteminfo.container);
                    long container = folder.container;
                    Log.d(TAG, "folder size : " + sFolders.size() + " container : " + container + " screen : " + folder.screen);
                    if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        layout = (CellLayout) getHotseat().getLayout();
                    } else if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                        layout = (CellLayout) workspace.getChildAt(mWorkspace.getRealScreenIndex(folder.screen));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "bindItemsChunkUpdated met exception : " + e);
                    layout = null;
                }
            }
            //BugID:5730318:icon title is installing after the app installed
            boolean isFound = false;
            if(layout == null){
                Log.d(TAG, "bindItemsChunkUpdated layout is null");
                //find the item in all layouts
                isFound = bindItemUpdateInAllLayouts(iteminfo, themeChange);
            } else {
                isFound = bindItemUpdateInOneLayout(layout, iteminfo, themeChange);
                if (isFound == false) {
                    //find the item in all layouts
                    isFound = bindItemUpdateInAllLayouts(iteminfo, themeChange);
                }
            }

            // BugID: 5324557:icon still in installing status after move to
            // hotseat
            // find the item in workspace's mDragItems
            if ((isFound == false) && (iteminfo instanceof ShortcutInfo)) {
                View tmpView = workspace.searchDragItemFromList(iteminfo);
                if ((tmpView != null) && (tmpView instanceof BubbleTextView)) {
                    BubbleTextView bubble = (BubbleTextView) tmpView;
                    Log.d(TAG, "find the item in workspace's mDragItems!");
                    if (themeChange) {
                        bubble.preThemeChange();
                    }
                    BubbleController.applyToView((ShortcutInfo) iteminfo, bubble);
                    BubbleController.updateView(bubble);
                    //BugID:6652967:apply fading effect for hideseat icons when theme change
                    if (iteminfo.container == Favorites.CONTAINER_HIDESEAT && themeChange) {
                        bubble.applyFadingEffectInHideseat();
                    }
                    continue;
                }
            }
        }

        if (end == items.size()) {
            items.clear();
            // if still in edit mode after theme changed, check empty screen, if
            // true show delete button
            mWorkspace.updateChildEditBtnContainer();
        }
        if (themeChange) {
            Launcher.sReloadingForThemeChangeg = false;
        }
        Log.d(TAG,"Launcher : bindItemsChunkUpdated end");
    }

    // BugID:5730318:icon title is installing after the app installed
    private boolean bindItemUpdateInAllLayouts(ItemInfo iteminfo, final boolean themeChange) {
        boolean isFound = false;
        CellLayout layout = (CellLayout) getHotseat().getLayout();
        isFound = bindItemUpdateInOneLayout(layout, iteminfo, themeChange);
        if (isFound == true) {
            return isFound;
        }

        if (getHideseat() != null) {
            layout = (CellLayout) getHideseat().getChildAt(iteminfo.screen);
            isFound = bindItemUpdateInOneLayout(layout, iteminfo, themeChange);
            if (isFound == true) {
                return isFound;
            }
        }

        final Workspace workspace = mWorkspace;
        int count = workspace.getIconScreenCount();
        int offSet = workspace.getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            layout = (CellLayout) workspace.getChildAt(i + offSet);
            isFound = bindItemUpdateInOneLayout(layout, iteminfo, themeChange);
            if (isFound == true) {
                return isFound;
            }
        }
        return isFound;
    }

    // BugID:5730318:icon title is installing after the app installed
    private boolean bindItemUpdateInOneLayout(CellLayout layout, ItemInfo iteminfo, final boolean themeChange) {
        if ((layout == null) || (iteminfo == null)) {
            return false;
        }
        ShortcutAndWidgetContainer container = layout.getShortcutAndWidgetContainer();
        Log.d(TAG, "title : " + iteminfo.title + " screen is " + iteminfo.screen + " id: " + iteminfo.id);
        int childCount = container.getChildCount();
        Log.d(TAG, "childCount is " + childCount);
        boolean isFound = false;
        for (int j = 0; j < childCount; j++) {
            View view = container.getChildAt(j);
            Object tag = view.getTag();
            if (tag instanceof ShortcutInfo) {
                ItemInfo info = (ItemInfo) tag;
                if (iteminfo.equals(info)) {
                    Log.d(TAG, "find the iteminfo : " + info.id);
                    isFound = true;
                    BubbleTextView bubble = (BubbleTextView) view;
                    if (themeChange) {
                        bubble.preThemeChange();
                    }
                    BubbleController.applyToView((ShortcutInfo) info, bubble);
                    BubbleController.updateView(bubble);
                    //BugID:6652967:apply fading effect for hideseat icons when theme change
                    if (iteminfo.container == Favorites.CONTAINER_HIDESEAT && themeChange) {
                        bubble.applyFadingEffectInHideseat();
                    }
                    break;
                }
            }

            if (tag instanceof FolderInfo) {
                ArrayList<View> childviews = (((FolderIcon)view).getFolder()).getItemsInReadingOrder();
                if (!iteminfo.equals(tag)) {
                    //find iteminfo in folder.
                    boolean found = false;
                    for (View bubbleview : childviews) {
                        ShortcutInfo info = (ShortcutInfo)(bubbleview.getTag());
                        Log.d(TAG, "in folder title : " + info.title + (info.userId > 0 ? " userId:" + info.userId : ""));
                        if (iteminfo.equals(info)) {
                            Log.d(TAG, "find the iteminfo in folder : " + info.id);
                            isFound = true;
                            BubbleTextView bubble = (BubbleTextView) bubbleview;
                            if (themeChange) {
                                // BugID:5881366:exception drag a icon in folder
                                // during theme change
                                bubble.preThemeChange();
                            }
                            BubbleController.applyToView((ShortcutInfo) info, bubble);
                            BubbleController.updateView(bubble);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        ((FolderIcon)view).updateView();
                        break;
                    }
                } else {
                    // update folder include child.
                    for (View bubbleview : childviews) {
                        ShortcutInfo info = (ShortcutInfo)(bubbleview.getTag());
                        Log.d(TAG, "find the iteminfo in folder : " + info.id);
                        isFound = true;
                        BubbleTextView bubble = (BubbleTextView) bubbleview;
                        if (themeChange) {
                            bubble.preThemeChange();
                        }
                        BubbleController.applyToView((ShortcutInfo) info, bubble);
                        BubbleController.updateView(bubble);
                    }

                    Log.d(TAG, "find the iteminfo of folder : " + iteminfo.id);
                    ((FolderIcon)view).updateView();
                    /*YUNOS BEGIN*/
                    //##date:2014/9/25 ##author:yangshan.ys BugId:5255762
                    (((FolderIcon) view).getFolder()).onThemeChanged();
                    /*YUNOS END*/
                    break;
                }
            }

            if (tag instanceof GadgetItemInfo && iteminfo.equals(tag)) {
                CellLayout.LayoutParams lp = new CellLayout.LayoutParams((CellLayout.LayoutParams) view.getLayoutParams());
                GadgetItemInfo info = (GadgetItemInfo) tag;
                layout.removeView(view);
                view.setTag(null);
                ((GadgetView) view).cleanUp();

                View newGadgetView = LauncherGadgetHelper.getGadget(this, info.gadgetInfo);
                if (newGadgetView == null) {
                    Log.d(TAG, "ERROR: fetch gadget error:" + info.gadgetInfo.label);
                    return true;
                }
                newGadgetView.setTag(tag);
                newGadgetView.setOnLongClickListener(this);
                layout.addViewToCellLayout(newGadgetView, j, (int) info.id, lp, true);
            }
        }

        return isFound;
    }

    /*YUNOS BEGIN LH*/
    @Override
    public void bindItemsUpdated(ArrayList<ItemInfo> items) {
        Log.d(TAG,"Launcher : bindItemsUpdated begin");

        /*YUNOS BEGIN*/
        //##module(HomeShell)
        //##date:2014/04/11 ##author:hao.liuhaolh@alibaba-inc.com##BugID:109600
        //vp item still display loading after install finish
        //BugID:5241693:installed item icon update failed
        if (mScreenEditMode){
            final ArrayList<ItemInfo> finalitems = new ArrayList<ItemInfo>(items);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    bindItemsUpdated(finalitems);
                }
            };
            mbindHandler.postDelayed(r, 500);
            return;
        }
        /*YUNOS END*/
        /* YUNOS BEGIN */
        // ##date:2013/12/02 ##author:zhangqiang.zq ##bugid: 75062
        if (items == null || mWorkspace == null) {
            return;
        }
        /* YUNOS END */
        final int itemCount = items.size();
        Log.d(TAG, "bindItemsUpdated itemCount is " + itemCount);
        final Workspace workspace = mWorkspace;
        for (int i = 0; i < itemCount; i++){
            ItemInfo iteminfo = items.get(i);

            CellLayout layout = null;
            if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                layout = (CellLayout) getHotseat().getLayout();
            } else if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_HIDESEAT) {
                layout = (CellLayout) getHideseat().getChildAt(iteminfo.screen);
            } else if (iteminfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                layout = (CellLayout) workspace.getChildAt(mWorkspace.getRealScreenIndex(iteminfo.screen));
            } else {
                /* YUNOS BEGIN */
                // ##date:2013/12/02 ##author:hongxing.whx ##bugid: 69225
                try {
                    FolderInfo folder = sFolders.get(iteminfo.container);
                    long container = folder.container;
                    Log.d(TAG, "bindItemsUpdated folder size : " + sFolders.size() + " container : " + container + " screen : "
                            + folder.screen);
                    if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                        layout = (CellLayout) getHotseat().getLayout();
                    } else if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                        layout = (CellLayout) workspace.getChildAt(mWorkspace.getRealScreenIndex(folder.screen));
                    }
                } catch (Exception e) {
                    Log.d(TAG, "bindItemsUpdated met exception : " + e);
                    layout = null;
                }
                /* YUNOS END */
            }
            if(layout == null){
                Log.d(TAG, "bindItemsUpdated layout is null");
                //return;
                continue;
            }

            ShortcutAndWidgetContainer container = layout.getShortcutAndWidgetContainer();
            Log.d(TAG, "title : " + iteminfo.title + " screen is " + iteminfo.screen + " id: " + iteminfo.id);
            int childCount = container.getChildCount();
            Log.d(TAG, "childCount is " + childCount);
            boolean isFound = false;
            for (int j = 0; j < childCount; j++) {
                View view = container.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ItemInfo info = (ItemInfo)tag;
                    if(iteminfo.equals(info)){
                        Log.d(TAG, "find the iteminfo : " + info.id);
                        isFound = true;
                        BubbleTextView bubble = (BubbleTextView) view;
                        BubbleController.applyToView((ShortcutInfo) info, bubble);
                        BubbleController.updateView(bubble);
                        break;
                    }
                }

                if (tag instanceof FolderInfo) {
                    ArrayList<View> childviews = (((FolderIcon)view).getFolder()).getItemsInReadingOrder();
                    if (!iteminfo.equals(tag)) {
                        //find iteminfo in folder.
                        boolean found = false;
                        for (View bubbleview : childviews) {
                            ShortcutInfo info = (ShortcutInfo)(bubbleview.getTag());
                            Log.d(TAG, "in folder title : " + info.title);
                            if (iteminfo.equals(info)) {
                                Log.d(TAG, "find the iteminfo in folder : " + info.id);
                                isFound = true;
                                BubbleTextView bubble = (BubbleTextView) bubbleview;
                                BubbleController.applyToView((ShortcutInfo) info, bubble);
                                BubbleController.updateView(bubble);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            ((FolderIcon)view).updateView();
                            break;
                        }
                    } else {
                        // update folder include child.
                        for (View bubbleview : childviews) {
                            ShortcutInfo info = (ShortcutInfo)(bubbleview.getTag());
                            Log.d(TAG, "find the iteminfo in folder : " + info.id);
                            isFound = true;
                            BubbleTextView bubble = (BubbleTextView) bubbleview;
                            BubbleController.applyToView((ShortcutInfo) info, bubble);
                            BubbleController.updateView(bubble);
                        }

                        Log.d(TAG, "find the iteminfo of folder : " + iteminfo.title);
                        ((FolderIcon)view).updateView();
                        break;
                    }
                }

                /* YUNOS BEGIN */
                // ##date:2014/4/3 ##author:zhangqiang.zq
                // gadget support theme changed
                if (tag instanceof GadgetItemInfo && iteminfo.equals(tag)) {
                    CellLayout.LayoutParams lp = new CellLayout.LayoutParams((CellLayout.LayoutParams) view.getLayoutParams());
                    GadgetItemInfo info = (GadgetItemInfo) tag;
                    layout.removeView(view);
                    view.setTag(null);
                    ((GadgetView) view).cleanUp();

                    View newGadgetView = LauncherGadgetHelper.getGadget(this, info.gadgetInfo);
                    if (newGadgetView == null) {
                        Log.d(TAG, "ERROR: fetch gadget error:" + info.gadgetInfo.label);
                        return;
                    }
                    newGadgetView.setTag(tag);
                    newGadgetView.setOnLongClickListener(this);
                    layout.addViewToCellLayout(newGadgetView, j, (int) info.id, lp, true);
                }
                /* YUNOS END */
            }

            // BugID: 5324557:icon still in installing status after move to
            // hotseat
            // find the item in workspace's mDragItems
            if ((isFound == false) && (iteminfo instanceof ShortcutInfo)) {
                View tmpView = workspace.searchDragItemFromList(iteminfo);
                if ((tmpView != null) && (tmpView instanceof BubbleTextView)) {
                    Log.d(TAG, "find the item in workspace's mDragItems!");
                    BubbleTextView bubble = (BubbleTextView) tmpView;
                    BubbleController.applyToView((ShortcutInfo) iteminfo, bubble);
                    BubbleController.updateView(bubble);
                    continue;
                }
            }
        }
        items.clear();
        Log.d(TAG,"Launcher : bindItemsUpdated end");
    }

    @Override
    public void bindDownloadItemsRemoved(final ArrayList<ItemInfo> items, final boolean permanent) {
        Log.d(TAG,"Launcher : bindDownloadItemsRemoved begin");
         if (waitUntilResume(new Runnable() {
             public void run() {
                 bindDownloadItemsRemoved(items, permanent);
             }
         })) {
             return;
         }
        final ArrayList<String> packageNames = new ArrayList<String>();
        for(ItemInfo info : items){
            if(info instanceof ShortcutInfo){
                if (((ShortcutInfo)info).intent != null) {
                    packageNames.add(((ShortcutInfo)info).intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME));
                }
            }
        }
        if (mWorkspace != null) {
            mWorkspace.removeItemsByPackageName(packageNames);
        }
        //BugID:5235067:cancel downloading item leave one item folder
        items.clear();
        Runnable checkFolderRunnable = new Runnable() {
            @Override
            public void run() {
                LauncherModel.checkFolderAndUpdateByUI();
            }
        };
        mWorkspace.post(checkFolderRunnable);
        Log.d(TAG,"Launcher : bindDownloadItemsRemoved end");
    }

    /*YUNOS BEGIN*/
    //##date:2014/04/26##author:hao.liuhaolh@alibaba-inc.com##BugID:114988
    //find and remove one item folder after all restore app handled by appstore
    @Override
    public void bindItemsViewRemoved(final ArrayList<ItemInfo> items) {
         Log.d(TAG,"Launcher : bindItemsViewRemoved begin");
         if (waitUntilResume(new Runnable() {
             public void run() {
                 bindItemsViewRemoved(items);
             }
         })) {
             return;
         }
        final ArrayList<ItemInfo> itemsViewRemoved = new ArrayList<ItemInfo>(items);
        //BugID:5531731:mWorkspace null pointer exception
        if (mWorkspace == null) {
            return;
        }
        mWorkspace.removeItemsViewByItemInfo(itemsViewRemoved);
        Log.d(TAG,"Launcher : bindItemsViewRemoved end");
    }
    /*YUNOS END*/

    //BugID:5197690:bind remove item null pointer exception
    @Override
    public void bindItemsRemoved(final ArrayList<ItemInfo> items) {
         Log.d(TAG,"Launcher : bindItemsRemoved begin");
         if (waitUntilResume(new Runnable() {
             public void run() {
                 bindItemsRemoved(items);
             }
         })) {
             return;
         }
        final ArrayList<ItemInfo> itemsRemoved = new ArrayList<ItemInfo>(items);

        if (mWorkspace != null) {
            mWorkspace.removeItemsByItemInfo(itemsRemoved);
        }
        Log.d(TAG,"Launcher : bindItemsRemoved end");
    }

    //this function only used to move views into workspace
    //not move into folder, hotseat, or hideseat
    public void bindWorkspaceItemsViewMoved(final ArrayList<ItemInfo> items) {
        Log.d(TAG,"Launcher : bindWorkspaceItemsViewMoved begin");
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindWorkspaceItemsViewMoved(items);
            }
        })) {
            return;
        }
        final ArrayList<ItemInfo> itemsViewMoved = new ArrayList<ItemInfo>(items);

        moveItemsViewByItemInfo(itemsViewMoved);
        Log.d(TAG,"Launcher : bindWorkspaceItemsViewMoved end");

    }

    @Override
    public void bindItemsAdded(ArrayList<ItemInfo> items) {
        Log.d(TAG,"Launcher : bindItemsAdded begin");
        bindItems(items, 0, items.size());
        removeItemsFromRestoreListIfNeed(items);
        Log.d(TAG,"Launcher : bindItemsAdded end");
    }

    private void removeItemsFromRestoreListIfNeed(ArrayList<ItemInfo> items){
        if(items == null){
            return;
        }
        if (BackupManager.getInstance().isInRestore()) {
            final Context context = this;
            String pkgname = null;
            for(ItemInfo info:items){
                if(info instanceof ShortcutInfo){
                    pkgname = ((ShortcutInfo)info).getPackageName();
                    BackupManager.getInstance().addRestoreDoneApp(context, pkgname);
                }else if(info instanceof FolderInfo){
                    for(ShortcutInfo item:((FolderInfo)info).contents){
                        pkgname = item.getPackageName();
                        BackupManager.getInstance().addRestoreDoneApp(context, pkgname);
                    }
                }
            }
        }
    }

    @Override
    public void bindItemsViewAdded(ArrayList<ItemInfo> items) {
        //final ArrayList<ItemInfo> finalitems = items;
        final ArrayList<ItemInfo> itemsToAdd = new ArrayList<ItemInfo>(items);
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"Launcher : bindItemsViewAdded begin");
                bindItems(itemsToAdd, 0, itemsToAdd.size());
                Log.d(TAG,"Launcher : bindItemsViewAdded end");
            }
        });
    }

    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2013/12/06 ##author:hao.liuhaolh ##BugID:69423
    //The items in hotseat restore
    @Override
    public void bindRebuildHotseat(ArrayList<ItemInfo> items) {
        mHotseat.resetLayout();
        bindItems(items, 0, items.size());
    }
    /*YUNOS END*/

    //BugID:5204915:mLauncher is null in LauncherApplication
    @Override
    public void resetWorkspaceGridSize(int countX, int countY) {
        final int screenCount = mWorkspace.getIconScreenCount();
        int offSet = mWorkspace.getIconScreenHomeIndex();
        for (int i = offSet; i < screenCount + offSet; i++) {
            CellLayout layout = (CellLayout) mWorkspace.getChildAt(i);
            layout.resetGridSize(countX, countY);
            mWorkspace.adjustCellLayoutPadding(layout);
            /* YUNOS BEGIN */
            // ##date:2015/3/26 ##author:sunchen.sc ##BugID:5735130
            // When layout change(eg:4*4->4*5), update the workspace space list
            // in launcher model
            layout.addWorkspaceOccupiedToModel(mWorkspace);
            /* YUNOS END */
        }

        CellLayout hotseatLayout = mHotseat.getCellLayout();
        hotseatLayout.setGridSize(hotseatLayout.getCellWidth(), hotseatLayout.getCellHeight(), hotseatLayout.getWidthGap(),
                hotseatLayout.getHeightGap());

        if(mHideseat != null){
            mHideseat.onWorkspaceLayoutChange(countX, countY);
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mWorkspace.setCurrentPage(mWorkspace.getCurrentPage());
            }
        });
    }

    @Override
    public void checkAndRemoveEmptyCell() {
        if (mWorkspace != null) {
            mWorkspace.checkAndRemoveEmptyCell();
        }
    }

    /*YUNOS BEGIN*/
    //##date:2014/01/17 ##author:hao.liuhaolh ##BugID:
    //add hide icon container
    @Override
    public void bindRebuildHideseat(ArrayList<ItemInfo> items) {
        //TODO liuhao it should mHideseat
        //mHotseat.resetLayout();
        bindItems(items, 0, items.size());
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/03/13 ##author:hao.liuhaolh ##BugID:98731
    //vp install item icon display
// remove vp install
//    public void startVPInstallActivity(Intent intent, Object tag) {
//        if (mPaused == false) {
//            startActivitySafely(null, intent, tag);
//        }
//    }
    /*YUNOS END*/

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(final HashMap<Long, FolderInfo> folders) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindFolders(folders);
                }
            })) {
            return;
        }
        //sFolders.clear();
        sFolders.putAll(folders);
    }

    /**
     * Add the views for a widget to the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindAppWidget(item);
                }
            })) {
            return;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }
        final Workspace workspace = mWorkspace;

        final int appWidgetId = item.appWidgetId;
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component " + appWidgetInfo.provider);
        }

        item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        item.minWidth = appWidgetInfo.minWidth;
        item.minHeight = appWidgetInfo.minHeight;
        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        //Log.d(TAG, "getClassName=" + item.providerName.getClassName());
        workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
        workspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id=" + item.appWidgetId + " in " + (SystemClock.uptimeMillis() - start) + "ms");
        }
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    finishBindingItems();
                }
            })) {
            return;
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        /* YUNOS BEGIN */
        // ##date:2013/12/04 ##author:zhangqiang.zq
        // bug id:73069
        mWorkspace.dispatchRestoreInstanceState(null);
        /* YUNOS END */
        mWorkspace.restoreInstanceStateForRemainingPages();

        // If we received the result of any pending adds while the loader was
        // running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 && mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to the new page to show
                // the new-apps animation or just run them if they are to appear on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items in place if they
                // are on another page (or just normally if they are added to the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;
        /*YUNOS BEGIN lxd#134902 calculate the hideseat postion*/
        positionHideseat();
        /*YUNOS END*/

        /* YUNOS BEGIN */
        // ##date:2014/9/11 ##author:zhanggong.zg ##BugID:5244146
        // ##date:2014/9/28 ##author:zhanggong.zg ##BugID:5306090
        // This method ensures the correctness of frozen states of hide-seat items.
        // This is an important process after restore or fota.
        if (Hideseat.isHideseatEnabled()) {
            getHideseat().rearrangeFrozenAppsInHideseat();
        }
        /* YUNOS END */

        getHotseat().initViewCacheList();
        getWorkspace().invalidatePageIndicator(true);

        /* YUNOS BEGIN */
        // ##date:2015/8/24 ##author:zhanggong.zg ##BugID:6356409
        // daily update feature for smart recommendation
        AppLaunchUpdater.updateIfNecessary(getApplicationContext());
        /* YUNOS END */

        if (mModel.getWaitingBindForOrienChanged() % 2 != 0) {
            mModel.clearWaitingCount();
            ConfigManager.reCreateConfigDataOnOrientationChanged();
            mModel.resetDataOnConfigurationChanged();
            runOrienChangedSync();
        }
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher
     * was in the background.
     *
     * @param immediate
     *            whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if
        // requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat("alpha", 1f),
                        PropertyValuesHolder.ofFloat("scaleX", 1f), PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
                mSharedPrefs.edit().putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1)
                        .putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, null).commit();
            }
        }.start();
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<ApplicationInfo> apps) {
    }

    /**
     * A package was installed.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAdded(final ArrayList<ApplicationInfo> apps) {
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(final ArrayList<ApplicationInfo> apps) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindAppsUpdated(apps);
            }
        })) {
            return;
        }

        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }
    }

    /**
     * A package was uninstalled. We take both the super set of packageNames in
     * addition to specific applications to remove, the reason being that this
     * can be called when a package is updated as well. In that scenario, we
     * only remove specific components from the workspace, where as
     * package-removal should clear all items by package name.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindComponentsRemoved(final ArrayList<String> packageNames, final ArrayList<ApplicationInfo> appInfos,
            final boolean matchPackageNamesOnly) {
        Log.d(TAG, "bindComponentsRemoved : begin");
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindComponentsRemoved(packageNames, appInfos, matchPackageNamesOnly);
            }
        })) {
            return;
        }

        if (matchPackageNamesOnly) {
            // BugID:139616:uninstall an app in folder, the info about this app
            // not removed from db
            mWorkspace.removeItemsByPackageNameForAppUninstall(packageNames);
        } else {
            mWorkspace.removeItemsByApplicationInfo(appInfos);
        }
        /* YUNOS BEGIN */
        // ##date:2013/11/25 ##author:xiaodong.lxd
        // clean drag info
        if (mFolder != null) {
            if (mFolder.mHasDirtyData) {
                Log.d(TAG, "sxsexe-------------------->  bindComponentsRemoved mFolder.mShortcutInfoCache " + mFolder.mShortcutInfoCache);
                mFolder.getInfo().remove(mFolder.mShortcutInfoCache);
                LauncherModel.deleteItemFromDatabase(this, mFolder.mShortcutInfoCache);
            }
        }

        getWorkspace().cleanDragInfo();
        getWorkspace().checkAndRemoveEmptyCell();
        /* YUNOS END */

        // Notify the drag controller
        mDragController.onAppsRemoved(appInfos, this);

        // BugID:5191015:liuhao:the last item in folder overlay
        // the reason to use runnable is checkFolderAndUpdateByUI
        // must be run after view remove finished in mWorkspace
        // removeItemsByComponentName
        Runnable checkFolderRunnable = new Runnable() {
            @Override
            public void run() {
                LauncherModel.checkFolderAndUpdateByUI();
            }
        };
        mWorkspace.post(checkFolderRunnable);

        Log.d(TAG,"bindComponentsRemoved : end");
    }

    /**
     * A number of packages were updated.
     */

    private ArrayList<Object> mWidgetsAndShortcuts;
    private Runnable mBindPackagesUpdatedRunnable = new Runnable() {
            public void run() {
                bindPackagesUpdated(mWidgetsAndShortcuts);
                mWidgetsAndShortcuts = null;
            }
        };

    public void bindPackagesUpdated(final ArrayList<Object> widgetsAndShortcuts) {
        if (waitUntilResume(mBindPackagesUpdatedRunnable, true)) {
            mWidgetsAndShortcuts = widgetsAndShortcuts;
            return;
        }

        if(mEditModePreviewContainer != null) {
            mEditModePreviewContainer.onPackagesUpdated(widgetsAndShortcuts);
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
            case Surface.ROTATION_0 :
            case Surface.ROTATION_180 :
                // We are currently in the same basic orientation as the natural
                // orientation
                naturalOri = configOri;
                break;
            case Surface.ROTATION_90 :
            case Surface.ROTATION_270 :
                // We are currently in the other basic orientation to the
                // natural orientation
                naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE)
                        ? Configuration.ORIENTATION_PORTRAIT
                        : Configuration.ORIENTATION_LANDSCAPE;
                break;
        }

        int[] oriMap = {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE};
        // Since the map starts at portrait, we need to offset if this device's
        // natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    public boolean isRotationEnabled() {
        boolean enableRotation = sForceEnableRotation || getResources().getBoolean(R.bool.allow_rotation);
        return enableRotation;
    }
    public void lockScreenOrientation() {
        if (isRotationEnabled()) {
            setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation));
        }
    }
    public void unlockScreenOrientation(boolean immediate) {
        if (isRotationEnabled()) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, mRestoreScreenOrientationDelay);
            }
        }
    }

    /*YUNOS BEGIN lxd #143377*/
    public boolean checkFolderIdValid(long folderId) {
        return sFolders.containsKey(folderId);
    }
    /*YUNOS END*/

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        Log.d(TAG, "BEGIN launcher2 dump state for launcher " + this);
        Log.d(TAG, "mSavedState=" + mSavedState);
        Log.d(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
        Log.d(TAG, "mRestoring=" + mRestoring);
        Log.d(TAG, "mWaitingForResult=" + mWaitingForResult);
        Log.d(TAG, "mSavedInstanceState=" + mSavedInstanceState);
        Log.d(TAG, "sFolders.size=" + sFolders.size());
        mModel.dumpState();

        Log.d(TAG, "END launcher2 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }

    public static void dumpDebugLogsToConsole() {
        Log.d(TAG, "");
        Log.d(TAG, "*********************");
        Log.d(TAG, "Launcher debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            Log.d(TAG, "  " + sDumpLogs.get(i));
        }
        Log.d(TAG, "*********************");
        Log.d(TAG, "");
        sDumpLogs.clear();
    }

    @Override
    public void bindRemoveScreen(int screen) {

    }

    public void setCurrentFolder(Folder folder) {
        mFolder = folder;
    }

    protected boolean isDragToDelete() {
        if((mWorkspace != null && mWorkspace.mDropTargetView instanceof DeleteDropTarget
                && ((DeleteDropTarget) mWorkspace.mDropTargetView).isDeleteTarget())
                ||(mDeleteDialog != null && mDeleteDialog.isShowing())){
            return true;
        }
        return false;
    }

    protected boolean isDragToClone() {
        if(mWorkspace != null && mWorkspace.mDropTargetView instanceof DeleteDropTarget
                && ((DeleteDropTarget) mWorkspace.mDropTargetView).isAppCloneTarget()){
            return true;
        }
        return false;
    }

    /* YUNOS BEGIN */
    // ##date:2014/02/20 ##author:yaodi.yd ##BugID:91131
    private boolean isHideDeleteDialog() {
        if (mDeleteDialog == null || !mDeleteDialog.isShowing())
            return false;
        return true;
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##module(HomeShell)
    // ##date:2014/01/07 ##author:yaodi.yd ##feature:unlockScreen animation
    public Handler getHandler() {
        return mHandler;
    }

    public PageIndicatorView getIndicatorView() {
        return mIndicatorView;
    }

    // private boolean isTopActivity() {
    // ActivityManager am = (ActivityManager)
    // getSystemService(ACTIVITY_SERVICE);
    // List<RunningTaskInfo> runningTaskInfos = am.getRunningTasks(1);
    // if (runningTaskInfos == null || runningTaskInfos.size() == 0)
    // return false;
    // ComponentName componentName = runningTaskInfos.get(0).topActivity;
    // return
    // componentName.getPackageName().equals(Launcher.class.getPackage().getName());
    // }
    /* YUNOS END */

    public void openHideseat(boolean isAnimation) {
        if(mCustomHideseat == null){
            return;
        }
        AnimationListener l = new AnimationListener() {
            CellLayout currentLayout = null;
            @Override
            public void onAnimationEnd(Animation animation) {
                /* YUNOS BEGIN */
                // ##date:2015-1-19 ##author:zhanggong.zg ##BugID:5621070

                // the next and previous celllayout enter hideseat mode immediately
                int nextPage = mWorkspace.getCurrentPage() + 1;
                /* YUNOS BEGIN PB */
                // ##date:2015/1/11    ##author:suhang.sh
                // ##module:Homeshell
                // ##description: Fixed 7768230, do not force widget page enter Hideseat mode
                if (nextPage < mWorkspace.getChildCount() && !((CellLayout) mWorkspace.getChildAt(nextPage)).isWidgetPage()) {
                /* YUNOS END PB*/
                    CellLayout nextCellLayout = (CellLayout) mWorkspace.getChildAt(nextPage);
                    nextCellLayout.enterHideseatMode();
                    nextCellLayout.didEnterHideseatMode();
                }
                int previousPage = mWorkspace.getCurrentPage() - 1;
                if (previousPage > mWorkspace.getIconScreenHomeIndex()) {
                    CellLayout previousCellLayout = (CellLayout) mWorkspace.getChildAt(previousPage);
                    previousCellLayout.enterHideseatMode();
                    previousCellLayout.didEnterHideseatMode();
                }

                if (mHideseatOpenCompleteRunnable == null) {
                    mHideseatOpenCompleteRunnable = new Runnable() {
                        @Override
                        public void run() {
                            final int N = mWorkspace.getIconScreenCount();
                            int offSet = mWorkspace.getIconScreenHomeIndex();
                            for (int i = 0; i < N; i++) {
                                int index = i + offSet;
                                CellLayout layout = (CellLayout) mWorkspace.getChildAt(index);
                                if (!layout.isHideseatOpen) {
                                    layout.enterHideseatMode();
                                    layout.didEnterHideseatMode();
                                }
                            }
                        }
                    };
                }
                mHandler.removeCallbacks(mHideseatOpenCompleteRunnable);
                postRunnableToMainThread(mHideseatOpenCompleteRunnable, 200);
                /* YUNOS END */

                /* YUNOS BEGIN */
                // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
                // clips the contents below hide-seat during animation
                mCustomHideseat.setVerticalClip(0);
                mIndicatorView.setVisibility(View.VISIBLE);
                if (currentLayout != null) {
                    currentLayout.setHideseatAnimationPlaying(false);
                    currentLayout = null;
                }
                /* YUNOS END */
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "enterHideseatMode");

                /* YUNOS BEGIN */
                // ##module(Hideseat)
                // ##date:2014/3/25 ##author:shaoguo.wangsg@alibaba-inc.com
                // ##BugId:105933
                // can not hideseat and dock showing is not correctly.
                final int N = mWorkspace.getIconScreenCount();
                int cur = mWorkspace.getCurrentPage();
                currentLayout = (CellLayout) mWorkspace.getChildAt(cur);
                currentLayout.enterHideseatMode();
                // ##date:2015/1/29 ##author:zhanggong.zg
                // ##BugID:5732475
                currentLayout.requestLayout();
                /* YUNOS END */

                // BugID:106602 add adjustment for avoiding more than one
                // enterHideseatMode.
                if (mCustomHideseat.getVisibility() != View.VISIBLE) {
                    mCustomHideseat.setVisibility(View.VISIBLE);
                    mHotseat.setVisibility(View.GONE);
                    mDragController.addDropTarget(mHideseat);
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)mIndicatorView.getLayoutParams();
                    // YUNOS BEGIN PB
                    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                    // ##BugID:(6737848) ##date:2015/12/10
                    // ##description: hide seat margin error with navbar.
                    lp.bottomMargin = getDynamicNavBarHeight();
                    // YUNOS END PB
                    FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams)mCustomHideseat.getLayoutParams();
                    lp1.bottomMargin = calcHideseatBottomMargin();
                    for (int i = 0; i < N; i++) {
                        CellLayout layout = (CellLayout) mWorkspace.getChildAt(i);
                        if (FeatureUtility.hasFullScreenWidget() && layout.isWidgetPage()) {
                            layout.getWidgetPageInfo().getHotseatView().setVisibility(View.GONE);
                        }
                    }
                    /* YUNOS BEGIN */
                    // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
                    // clips the contents below hide-seat during animation
                    if (mGestureLayer.isLiveWallpaperFlag()) {
                        mCustomHideseat.setVerticalClip(getCustomHideseat().getCustomeHideseatHeight());
                        mIndicatorView.setVisibility(View.GONE);
                    }
                    // ##date:2015-1-19 ##author:zhanggong.zg ##BugID:5621070
                    if (currentLayout != null) {
                        currentLayout.setHideseatAnimationPlaying(true);
                    }
                    /* YUNOS END */
                }
            }
        };

        mGestureLayer.openHideseat(l, isAnimation);
        if (isAnimation) {
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_OPEN_HIDESEAT);
        }
    }

    private int calcHideseatBottomMargin() {
        /* YUNOS BEGIN */
        // ##date:2015/2/3 ##author:zhanggong.zg ##BugID:5763426
        CellLayout layout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        if (layout == null) {
            layout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getIconScreenHomeIndex());
        }
        /* YUNOS END */
        ShortcutAndWidgetContainer container = (ShortcutAndWidgetContainer) layout.getChildAt(0);
        CellLayout.LayoutParams lp = container.buildLayoutParams(0, CellLayout.HIDESEAT_CELLY, 1, 1, true);

        int top = 0;
        top += layout.getTop();
        top += container.getTop();
        top += lp.y;

        top -= layout.getHeightGap() / 2;

        int hideseatH = getCustomHideseat().getCustomeHideseatHeight();
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6737848) ##date:2015/12/10
        // ##description: hide seat margin error with navbar.
        int bottomMargin = mWorkspace.getHeight() - top - hideseatH + getDynamicNavBarHeight();
        // YUNOS BEGIN PB
        Log.d(TAG, "bottomMargin : " + bottomMargin + " workspace H : " + mWorkspace.getHeight() + " top : " + top + " hideseat H : "
                + hideseatH);

        return bottomMargin;
    }

    public void hideHideseat(boolean isAnimation) {
        if(mCustomHideseat == null){
            return;
        }
        AnimationListener l = new AnimationListener(){
            CellLayout currentLayout = null;
            @Override
            public void onAnimationEnd(Animation animation) {
                Log.d(TAG, "exitHideseatMode");

                mHandler.removeCallbacks(mHideseatOpenCompleteRunnable);
                final int N = mWorkspace.getIconScreenCount();
                int offSet = mWorkspace.getIconScreenHomeIndex();
                for (int i = 0; i < N; i++) {
                    CellLayout layout = (CellLayout) mWorkspace.getChildAt(i + offSet);
                    layout.exitHideseatMode();
                }

                // BugID:106602 add adjustment for avoid more than one
                // exitHideseatMode.
                if (mCustomHideseat.getVisibility() != View.GONE) {
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mIndicatorView.getLayoutParams();
                    // YUNOS BEGIN PB
                    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
                    // ##BugID:(6737848) ##date:2015/12/10
                    // ##description: hide seat margin error with navbar.
                    if (!AgedModeUtil.isAgedMode()) {
                        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.button_bar_height_plus_padding)
                                + getDynamicNavBarHeight();
                    } else {
                        lp.bottomMargin = mCustomHideseat.getHeight() + getDynamicNavBarHeight();
                    }
                    // YUNOS BEGIN PB
                    mCustomHideseat.setVisibility(View.GONE);
                    View v = mWorkspace.getPageAt(mWorkspace.getCurrentPage());
                    if (FeatureUtility.hasFullScreenWidget() && v instanceof CellLayout && ((CellLayout) v).isWidgetPage()) {
                        ((CellLayout) v).getWidgetPageInfo().getHotseatView().setTranslationY(0);
                        ((CellLayout) v).getWidgetPageInfo().getHotseatView().setVisibility(View.VISIBLE);
                    } else {
                        /* YUNOS BEGIN */
                        // ## modules(Home Shell)
                        // ## date: 2016/01/05 ## author: wangye.wy
                        // ## BugID: 7756096: set visibility with atom
                        mHotseat.setVisibility(View.VISIBLE, !(mFlipAnim != null && mFlipAnim.isShowing()));
                        /* YUNOS END */
                    }

                    mDragController.removeDropTarget(mHideseat);
                    mCustomHideseat.setVerticalClip(0);
                    mCustomHideseat.setTranslationX(0);
                    mCustomHideseat.setTranslationY(0);
                    mIndicatorView.setVisibility(View.VISIBLE);
                    //BugID:6491987:to private mWallpaperTranslationX isn't 0
                    //when first enter lifecenter
                    mGestureLayer.setWallpaperTranslationX(0);
                }

                /* YUNOS BEGIN */
                // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
                // clips the contents below hide-seat during animation
                if (currentLayout != null) {
                    currentLayout.setHideseatAnimationPlaying(false);
                    currentLayout = null;
                }
                /* YUNOS END */
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {
                /* YUNOS BEGIN */
                // ##date:2015-1-16 ##author:zhanggong.zg ##BugID:5712973
                // clips the contents below hide-seat during animation
                if (mGestureLayer.isLiveWallpaperFlag()) {
                    mIndicatorView.setVisibility(View.GONE);
                }
                // ##date:2015-1-19 ##author:zhanggong.zg ##BugID:5621070
                currentLayout = (CellLayout) mWorkspace.getPageAt(mWorkspace.getCurrentPage());
                if (currentLayout != null)
                    currentLayout.setHideseatAnimationPlaying(true);
                /* YUNOS END */
            }
        };

        mGestureLayer.closeHideseat(l, isAnimation);
        if (isAnimation) {
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CLOSE_HIDESEAT);
        }
    }

    public boolean isHideseatShowing() {
        boolean showing = false;
        if(mCustomHideseat != null){
            showing =  mCustomHideseat.getVisibility() == View.VISIBLE;
        }
        return showing;
    }

    /* YUNOS BEGIN */
    //author:kerong.skr do not invalidate when view is not showing
    public boolean shouldInvalidate(CellLayout layout) {
        if (getDragLayer().getAnimatedView() != null || layout.getParent() != mWorkspace)
            return true;
        if (mWorkspace.isPageMoving())
            return false;
        return layout == mWorkspace.getChildAt(getCurrentWorkspaceScreen());
    }
    /* YUNOS END */

    /*YUNOS BEGIN added by xiaodong.lxd for push to talk*/
    public void setOnClickValid(boolean valid) {
        mOnClickValid = valid;
    }
    /* YUNOS END */

    /*YUNOS BEGIN added by xiaodong.lxd for #99779*/
    public boolean isSystemAppOrFolder(Object itemInfo) {
        if(itemInfo instanceof FolderInfo) {
            return true;
        }
        if(itemInfo instanceof ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)itemInfo;
            return shortcutInfo.isSystemApp;
        }
        return false;
    }

    public boolean isSystemApp(Object itemInfo) {
        if(itemInfo instanceof ShortcutInfo) {
            ShortcutInfo shortcutInfo = (ShortcutInfo)itemInfo;
            return shortcutInfo.isSystemApp;
        }
        return false;
    }

    public boolean isItemUnDeletable(Object itemInfo) {
        // in editmode, do not show trash bin while multi-drag and drag widget
        // from widget list
        if (mWorkspace.isMultiSelectDragging() || itemInfo instanceof PendingAddItemInfo) {
            return true;
        }
        if(itemInfo instanceof ItemInfo) {
            ItemInfo info = (ItemInfo)itemInfo;
            return !info.isDeletable();
        }
        return false;
    }

    /*YUNOS END*/

    /*YUNOS BEGIN added by xiaodong.lxd for #112854*/
    public void postRunnableToMainThread(Runnable r, long delayMillis) {
        if(mHandler != null) {
            if(delayMillis > 0) {
                mHandler.postDelayed(r, delayMillis);
            } else {
                mHandler.post(r);
            }
        }
    }

    protected void cancelRunnableInMainThread(Runnable r) {
        if(mHandler != null) {
            mHandler.removeCallbacks(r);
        }
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/5/22 ##author:zhangqiang.zq
    // smart search
    //##date:2015/2/2 ##author:zhanggong.zg ##BugID:5719824
    // smart search is disabled to reduce initial memory usage

    public void enterSearchMode() {
        /*
         * Log.i(TAG, "enterSearchMode:" + mAppSearchMode); if (mT9DialpadView
         * == null) { mT9DialpadView = (T9DialpadView)
         * findViewById(R.id.app_search);
         * mT9DialpadView.setT9DialpadViewListener(new
         * T9DialpadView.T9DialpadViewListener(){
         *
         * @Override public void onExit() { // TODO Auto-generated method stub
         * exitSearchMode(); }}); }
         *
         * mT9DialpadView.onEnter(); mT9DialpadView.setVisibility(View.VISIBLE);
         * getWindow
         * ().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
         * mAppSearchMode = true;
         */
    }

    public void exitSearchMode() {
        /*
         * Log.i(TAG, "exitSearchMode:" + mAppSearchMode);
         * mT9DialpadView.setVisibility(View.INVISIBLE);
         * mT9DialpadView.onExit();
         * getWindow().addFlags(WindowManager.LayoutParams
         * .FLAG_TRANSLUCENT_STATUS); mAppSearchMode = false;
         */
    }

    public boolean isSearchMode() {
        return mAppSearchMode;
    }

    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2014/7/2 ##author:yangshan.ys##BugID:134407
    // use the remind icon replace the folder which has only one icon in it
    // ++
    // ## date: 2016/10/03 ## author: yongxing.lyx
    // ## BugID:8877680:folder don't dissmiss after uninstalled all item in it.
    public void checkAndReplaceFolderIfNecessary(final ItemInfo item) {
        checkAndReplaceFolderIfNecessary(item, mFolder);
    }

    public void checkAndReplaceFolderIfNecessary(final ItemInfo item, final Folder folder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                /* YUNOS BEGIN */
                // ##date:2016/12/23 ##author:wb-sl248920##9598014
				mHasReplaceFolder = false;
                if (isContainerFolder(item.container) && sFolders.containsKey(item.container)
                        && folder != null) {
                    if (folder.getInfo().id != item.container) {
                        Log.e(TAG, "checkAndReplaceFolderIfNecessary() invalid folder!!! item:"
                                + item + ", folder:"
                                + ((folder != null) ? folder.getInfo() : "null"));
                    }
                    if (folder.getInfo().count() <= 1) {
                        folder.replaceFolderWithFinalItem();
						mHasReplaceFolder = true;
                /* YUNOS END */
                    } else {
                        folder.updateFolderNameWithRemainedApp(item);
                    }
                }
            }
        });
    }
    /* YUNOS END */

    /* YUNOS BEGIN lxd */
    public int getNavigationBarHeight() {
        boolean hasMenukey = ViewConfiguration.get(getApplicationContext()).hasPermanentMenuKey();
        boolean hasBackkey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        if (hasMenukey && hasBackkey) {
            return 0;
        } else {
            Resources resources = getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                return resources.getDimensionPixelSize(resourceId);
            }
            return 0;
        }
    }
    /* YUNOS END */

    // YUNOS BEGIN PB
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(6898647) ##date:2015/12/10
    // ##description: margin error after show or hide navbar in screen manager.
    public int getDynamicNavBarHeight() {
        if (mWorkspace != null) {
            return mWorkspace.mInsets.bottom;
        }
        return 0;
    }

    public static boolean hasNavigationBar() {
        try {
            IWindowManager windowManagerService = IWindowManager.Stub.asInterface(ServiceManager
                    .getService(Context.WINDOW_SERVICE));
            return windowManagerService.hasNavigationBar();
        } catch (android.os.RemoteException re) {
            Log.e(TAG, "ERROR!! WindowManagerService.hasNavigationBar() FAILED. return false.");
        }
        return false;
    }

    private Runnable mShowNavbarButtonsRunnable = new Runnable() {

        @Override
        public void run() {
            if ( !(SystemProperties.getBoolean("ro.yunos.navbar.hidebutton", false)) ) {
                mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
            }
        }
    };
    // YUNOS END PB

    /* YUNOS BEGIN */
    // ## modules(Home Shell)
    // ## date: 2016/03/17 ## author: wangye.wy
    // ## BugID: 7930322: set flag of waiting for onStop()
    private boolean mOnStopWaiting = false;
    private Runnable mOnStopWaitingRunnable = new Runnable() {
        @Override
        public void run() {
            mOnStopWaiting = true;
        }
    };
    /* YUNOS END */

    public int getStatusBarHeight() {
        return mStatusBarHeight;
    }

    /* YUNOS BEGIN lxd : measure statusbar height 2014/11/03 */
    private void meausreStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        } else {
            Rect rectangle = new Rect();
            Window window = getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
            int statusBarHeight = rectangle.top;
            View view = window.findViewById(Window.ID_ANDROID_CONTENT);
            if (view != null) {
                int contentViewTop = view.getTop();
                mStatusBarHeight = statusBarHeight - contentViewTop;
            }
        }
    }
    /* YUNOS END */

    // global search begin
    public boolean isInIdleStatus() {
        boolean idle = true;
        if ((mWorkspace != null) && (mWorkspace.getOpenFolder() != null)) {
            idle = false;
        } else if (isInEditScreenMode()) {
            idle = false;
        } else if (isHideseatShowing()) {
            idle = false;
        } else if (isAllAppsVisible()) {
            idle = false;
        } else if (isGadgetCardShowing()) {
            idle = false;
        } else if (mFolderUtils.isFolderOpened()) {
            idle = false;
        } else if (CheckVoiceCommandPressHelper.isPushTalkCanUse()
                && (CheckVoiceCommandPressHelper.getInstance().isVoiceUIShown() || CheckVoiceCommandPressHelper.getInstance()
                        .isVoiceUIShowMsgSent())) {
            idle = false;
        } else if (!isLifeCenterEnableSearch()) {
            idle = false;
        } else if (mWorkspace != null && mWorkspace.isInCategory()) {
            idle = false;
        }
        return idle;
    }
    // global search end

    /**
     * handle click event on downloading icon
     *
     * @param v
     *            the View was clicked
     * @return if it's handled
     * @author wenliang.dwl
     */
    public boolean onDownloadingClick(View v) {
        ShortcutInfo info = (ShortcutInfo) v.getTag();
        String pkgName = info.intent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
        if (pkgName == null) {
            pkgName = info.intent.getComponent().getPackageName();
        }

        Long lastPressTime = mLastPressTimeOfDownloadingIcon.get(pkgName);
        long now = System.currentTimeMillis();
        if (lastPressTime != null && now - lastPressTime < AppDownloadManager.DOWNLOAD_ICON_PRESS_INTERVAL){
            // if user frequently press downloading icon, ignore it
            return false;
        } else {
            mLastPressTimeOfDownloadingIcon.put(pkgName, now);
        }

        if (Utils.isSupportAppStoreQuickControl(this)) {
            // if clicked when downloading, send "Pause" broadcast
            if (info.getAppDownloadStatus() == AppDownloadStatus.STATUS_DOWNLOADING
                    || info.getAppDownloadStatus() == AppDownloadStatus.STATUS_WAITING) {
                Intent intent = new Intent(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
                intent.putExtra(AppDownloadManager.TYPE_ACTION, AppDownloadManager.ACTION_HS_DOWNLOAD_PAUSE);
                intent.putExtra(AppDownloadManager.TYPE_PACKAGENAME, pkgName);
                sendBroadcast(intent);
                Log.d(TAG,"send download pause broacast : "+pkgName);
            }
            // if clicked when paused, send "Continue" broadcast
            if( info.getAppDownloadStatus() == AppDownloadStatus.STATUS_PAUSED ){
                Intent intent = new Intent(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
                intent.putExtra(AppDownloadManager.TYPE_ACTION, AppDownloadManager.ACTION_HS_DOWNLOAD_RUNNING);
                intent.putExtra(AppDownloadManager.TYPE_PACKAGENAME, pkgName);
                intent.putExtra(AppDownloadManager.TYPE_PROGRESS, info.getProgress());
                sendBroadcast(intent);
                Log.d(TAG,"send download continue broacast : "+pkgName);
            }
            return true;
        }else{
            final Intent downloadintent = info.createDownloadIntent();
            return startActivitySafely(v, downloadintent, v.getTag());
        }
    }

    // BugID:5183000:items overlay after folder dismiss
    // only move views from workspace to workspace. Not from folder, hotseat or
    // hideseat
    // if move a view in folder to workspace and the folder leave only one item
    // may cause some error
    void moveItemsViewByItemInfo(final ArrayList<ItemInfo> items) {
        if (items == null) {
            return;
        }
        Log.d(TAG, "moveItemsViewByItemInfo in");
        int screenCount = mWorkspace.getIconScreenCount();
        int offSet = mWorkspace.getIconScreenHomeIndex();

        for (ItemInfo moveitem : items) {
            boolean isFound = false;
            for (int screen = 0; screen < screenCount; screen++) {
                final ViewGroup layout = ((CellLayout) mWorkspace.getChildAt(screen + offSet)).getShortcutAndWidgetContainer();
                if (layout == null) {
                    continue;
                }
                int childCount = layout.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    final View view = layout.getChildAt(j);
                    Object tag = view.getTag();
                    final long id = ((ItemInfo) tag).id;

                    if (id == moveitem.id) {
                        Log.d(TAG, "find same id " + id);
                        layout.removeViewInLayout(view);
                        // BugID:5222344:widget overlap with other items
                        layout.invalidate();
                        mWorkspace.addInScreen(view, moveitem.container, moveitem.screen, moveitem.cellX, moveitem.cellY, moveitem.spanX,
                                moveitem.spanY);
                        isFound = true;
                        break;
                    }
                }
                if (isFound == true) {
                    break;
                }
            }
        }
    }
    public void startFlipAnimation(final View self, final View gadget) {
        // do not add gadget when is animating
        if (mFlipAnim.isAnimating() || mFlipAnim.isWaiting()) {
            return;
        }

        // BugID:5695121:userTrack for card and and launcher stay time.
        // hao.liuhaolh
        mFlipStartTime = SystemClock.uptimeMillis();
        ShortcutInfo info = (ShortcutInfo) ((BubbleTextView) self).getTag();
        // the info is verified in BubbleTextView onFlingUp.
        mFlipCardPkgName = info.intent.getComponent().getPackageName();
        // mFlipCardType = gadget instanceof GadgetView ? "Special" : "Normal";
        mFlipCardType = gadget instanceof CardNotificationPanelView ? "Normal" : "Special";

        // gesture has been detected and it's waiting for animation on main
        // thread
        mFlipAnim.setIsWaiting(true);

        // for bug 5233174,5237445 gadget already has a parent
        ViewGroup gadgetParent = (ViewGroup)gadget.getParent();
        if( gadgetParent != null ){
            gadgetParent.removeView(gadget);
        }

        // Add Blue Background
        final View bg = new View(this);
        mDragLayer.addView(bg);
        DragLayer.LayoutParams bgLp = new DragLayer.LayoutParams(mDragLayer.getWidth(), mDragLayer.getHeight());
        bg.setLayoutParams(bgLp);

        // Add Big Card View
        int height = getResources().getDimensionPixelSize(R.dimen.big_card_view_height);
        int width  = getResources().getDimensionPixelSize(R.dimen.big_card_view_width);
        gadget.setAlpha(0);
        mDragLayer.addView(gadget,new DragLayer.LayoutParams(width, height));

        // Add Small Card View
        int points[] = new int[2];
        Canvas canvas = new Canvas();
        mDragLayer.getLocationInDragLayer(self, points);
        Bitmap b = getWorkspace().createDragBitmap(self, canvas, 0);
        final ImageView iv = new ImageView(this);
        iv.setImageBitmap(b);
        mDragLayer.addView(iv);
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(b.getWidth(), b.getHeight());
        lp.x = points[0];
        lp.y = points[1];
        lp.customPosition = true;
        iv.setLayoutParams(lp);

        // post the animation to main thread to make sure views get their size
        mDragLayer.post(new Runnable() {
            @Override
            public void run() {
                mFlipAnim.setIsWaiting(false);
                mFlipAnim.setSmallCard(iv);
                mFlipAnim.setBigCard(gadget);
                mFlipAnim.setBackground(bg);
                mFlipAnim.setThatBubble(self);
                mFlipAnim.computeValues();
                mFlipAnim.appear();
            }
        });

        bg.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if( !mFlipAnim.isAnimating() ){
                   stopFlipAnimation();
                }
            }
        });
    }

    public void stopFlipWithoutAnimation() {
        /* YUNOS BEGIN */
        // ##date:2015/7/23 ##author:zhiqiang.zhangzq ##BugID:6219083
        // ##replace GadgetCard with Widget
        View bigCard = mFlipAnim.getBigCard();
        if (bigCard instanceof AppWidgetHostView) {
            removeWidgetView((AppWidgetHostView) bigCard);
        }
        /* YUNOS END */
        mFlipAnim.clear();
        // BugID:5695121:userTrack for card and and launcher stay time.
        // hao.liuhaolh
        sendCardStayTimeMsg();
    }

    public void stopFlipAnimation() {
        stopFlipAnimation(true);
    }

    public void stopFlipAnimation(boolean anim) {
        Log.d(TAG, "stopFlipAnimation anim=" + anim);
        Intent intent = new Intent("com.yunos.systemui.widget.UPDATE");
        intent.putExtra("opt", "close");
        this.sendBroadcast(intent);
        /* YUNOS BEGIN */
        // ##date:2015/7/23 ##author:zhiqiang.zhangzq ##BugID:6219083
        // ##replace GadgetCard with Widget
        View bigCard = mFlipAnim.getBigCard();
        if (bigCard instanceof AppWidgetHostView) {
            removeWidgetView((AppWidgetHostView) bigCard);
        }
        /* YUNOS END */
        if (anim) {
            mFlipAnim.disappear();
        } else {
            mFlipAnim.disappear(false);
        }
        // BugID:5695121:userTrack for card and and launcher stay time.
        // hao.liuhaolh
        sendCardStayTimeMsg();
    }

    /* YUNOS BEGIN */
    // ##modules:HomeShell ##author:xiangnan.xn@alibaba-inc.com
    // ##BugID:7937582 ##date:2015/03/01
    // add a public method for checking flipAnimation showing status
    public boolean isFlipAnimationShowing() {
        return mFlipAnim.isShowing();
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    // ##date:2015/7/23 ##author:zhiqiang.zhangzq ##BugID:6219083
    // ##replace GadgetCard with Widget
    public View initWidgetView(ComponentName cn) {
        return mEditModePreviewContainer.initWidgetView(mAppWidgetManager, mAppWidgetHost, cn);
    }
    public void removeWidgetView(AppWidgetHostView widgetView) {
        mEditModePreviewContainer.removeWidgetView(widgetView);
    }
    // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6349930
    public boolean hasWidgetView(ComponentName cn) {
        return mEditModePreviewContainer.hasWidgetView(mAppWidgetManager, mAppWidgetHost, cn);
    }
    /* YUNOS END */

    // BugID:5695121:userTrack for card and and launcher stay time. hao.liuhaolh
    private void sendCardStayTimeMsg() {
        Map<String, String> msg = new HashMap<String, String>();
        if (mFlipStartTime == 0) {
            return;
        }
        long endtime = SystemClock.uptimeMillis();
        msg.put("Time", String.valueOf(endtime - mFlipStartTime));
        msg.put("PkgName", mFlipCardPkgName);
        msg.put("Type", mFlipCardType);
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CARD_STAY_TIME, msg);
        mFlipStartTime = 0;
    }

    private void sendLauncherStayTimeMsg(View v, Intent intent, Object tag) {
        if (mResumeTime == 0) {
            return;
        }
        if (!(v instanceof BubbleTextView) || !(tag instanceof ShortcutInfo) || (intent == null)) {
            return;
        }
        ShortcutInfo info = (ShortcutInfo) tag;
        if ((info.itemType != Favorites.ITEM_TYPE_APPLICATION) && (info.itemType != Favorites.ITEM_TYPE_SHORTCUT)) {
            return;
        }
        Map<String, String> msg = new HashMap<String, String>();
        long endtime = SystemClock.uptimeMillis();
        msg.put("Time", String.valueOf(endtime - mResumeTime));
        String pkgName;
        if (intent.getComponent() != null) {
            pkgName = intent.getComponent().getPackageName();
        } else if (intent.getPackage() != null) {
            pkgName = intent.getPackage();
        } else {
            Log.d(TAG, "no package name");
            return;
        }
        msg.put("PkgName", pkgName);
        msg.put("Screen", String.valueOf(info.screen));
        msg.put("type", info.itemType == Favorites.ITEM_TYPE_APPLICATION ? "app" : "shortcut");
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_LAUNCHER_STAY_TIME, msg);
        mResumeTime = 0;
    }

    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2014/10/16 ##author:yangshan.ys##5157204
    // for 3*3 layout
    public void adjustToThreeLayout() {
        mWorkspace.adjustToThreeLayout();
        mHotseat.adjustToThreeLayout();
        if (mHideseat != null) {
            mHideseat.adjustFromThreeLayout();
        }
        FolderIcon.onThemeChanged();
        ((FrameLayout.LayoutParams) mIndicatorView.getLayoutParams()).bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.page_indicator_bottom_margin_3_3);
        mIndicatorView.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.page_indicator_height_3_3);
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6473036) ##date:2015/10/9
        // ##description: setFrameLayoutChildInsets after set LayoutParams.height
        getDragLayer().setFrameLayoutChildInsets(mIndicatorView, getGestureLayer().mInsets,
                new Rect());
        // YUNOS END PB
        if (mCustomHideseat != null) {
            mCustomHideseat.getLayoutParams().height = mCustomHideseat.getCustomeHideseatHeight();
        }
        positionHideseat();
        FolderRingAnimator.refreshStaticValues();
    }

    public void adjustFromThreeLayout() {
        mWorkspace.adjustFromThreeLayout();
        mHotseat.adjustFromThreeLayout();
        if (mHideseat != null) {
            mHideseat.adjustFromThreeLayout();
        }
        FolderIcon.onThemeChanged();
        ((FrameLayout.LayoutParams) mIndicatorView.getLayoutParams()).bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.button_bar_height_plus_padding);
        mIndicatorView.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.page_indicator_height);
        // YUNOS BEGIN PB
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(6473036) ##date:2015/10/9
        // ##description: setFrameLayoutChildInsets after set LayoutParams.height
        getDragLayer().setFrameLayoutChildInsets(mIndicatorView, getGestureLayer().mInsets,
                new Rect());
        // YUNOS END PB
        if (mCustomHideseat != null) {
            mCustomHideseat.getLayoutParams().height = mCustomHideseat.getCustomeHideseatHeight();
        }
        positionHideseat();
        FolderRingAnimator.refreshStaticValues();
    }
    /* YUNSO END */
    /* YUNOS BEGIN */
    // ##date:2015/1/27 ##author:yangshan.ys##5734658
    // optimize the layout change
    @Override
    public void collectCurrentViews() {
        currentViews.clear();
        int count = mWorkspace.getIconScreenCount();
        int offSet = mWorkspace.getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            CellLayout layout = (CellLayout) mWorkspace.getChildAt(i + offSet);
            int childCount = layout.getShortcutAndWidgetContainer().getChildCount();
            for (int j = 0; j < childCount; j++) {
                currentViews.add(layout.getShortcutAndWidgetContainer().getChildAt(j));
            }
            layout.removeAllViewsInLayout();
        }
    }

    @Override
    public void reLayoutCurrentViews() {
        // TODO Auto-generated method stub
        for (View view : currentViews) {
            ((CellLayout.LayoutParams) view.getLayoutParams()).useTmpCoords = false;
            ItemInfo itemInfo = (ItemInfo) view.getTag();
            // 4*5->4*4, there may some no space items, they will be add to a
            // folder, and not show on the workspace
            if (itemInfo.container < 0) {
                mWorkspace.addInScreen(view, itemInfo.container, itemInfo.screen, itemInfo.cellX, itemInfo.cellY, itemInfo.spanX,
                        itemInfo.spanY);
            }
        }
        currentViews.clear();
    }
    /* YUNSO END */

    /* YUNOS BEGIN */
    // ## modules(Home Shell): [Category]
    // ## date: 2015/08/31 ## author: wangye.wy
    // ## BugID: 6221911: category on desk top
    @Override
    public void reLayoutCurrentViews(final List<ItemInfo> allItems) {
        mWorkspace.cleanDragItemList();
        int count = mWorkspace.getIconScreenCount();
        int offset = mWorkspace.getIconScreenHomeIndex();
        for (int i = 0; i < count; i++) {
            CellLayout layout = (CellLayout) mWorkspace.getChildAt(i + offset);
            ShortcutAndWidgetContainer container = layout.getShortcutAndWidgetContainer();
            int childCount = container.getChildCount();
            for (int j = childCount; j > 0; j--) {
                View child = container.getChildAt(j - 1);
                if (child instanceof GadgetView) {
                    LauncherGadgetHelper.cleanUp((GadgetView)child);
                }
            }
            layout.removeAllViewsInLayout();
        }
        for (ItemInfo item : allItems) {
            Log.d(TAG, "reLayoutCurrentViews(), title: " + item.title + ", container: " + item.container + ", " + item.screen + ", "
                    + item.cellY + ", " + item.cellX);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                View view = null;
                switch (item.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION :
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT :
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING :
                        view = createShortcut((ShortcutInfo) item);
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_FOLDER :
                        item.unbind();
                        view = FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) mWorkspace.getChildAt(item.screen),
                                (FolderInfo) item);
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET :
                        LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) item;
                        AppWidgetProviderInfo appWidgetInfo = widgetInfo.hostView.getAppWidgetInfo();
                        widgetInfo.hostView = mAppWidgetHost.createView(this, widgetInfo.appWidgetId, appWidgetInfo);
                        widgetInfo.hostView.setAppWidget(widgetInfo.appWidgetId, appWidgetInfo);
                        if (mEditModePreviewContainer != null) {
                            mEditModePreviewContainer.setDragSrcView(widgetInfo.hostView);
                        }
                        widgetInfo.hostView.setTag(widgetInfo);
                        widgetInfo.notifyWidgetSizeChanged(this);
                        addWidgetToAutoAdvanceIfNeeded(widgetInfo.hostView, appWidgetInfo);
                        view = widgetInfo.hostView;
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_GADGET :
                        GadgetItemInfo gadgetInfo = (GadgetItemInfo) item;
                        view = LauncherGadgetHelper.getGadget(this, gadgetInfo.gadgetInfo);
                        if (view == null) {
                            Log.e(TAG, "failed get gadget " + gadgetInfo.title);
                            break;
                        }
                        view.setTag(gadgetInfo);
                        view.setOnLongClickListener(this);
                        break;
                }
                if (view != null) {
                    mWorkspace.addInScreen(view, item.container, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
                }
            }
        }
        // i > 1 means leave at lest one page in workspace
        for (int i = count; i > 1; i--) {
            CellLayout layout = (CellLayout) mWorkspace.getChildAt(i + offset - 1);
            if (!layout.hasChild()) {
                mWorkspace.removeViewAt(i + offset - 1);
                mWorkspace.invalidatePageIndicator(true);
            }
        }
        mWorkspace.requestLayout();
    }

    private boolean mLauncherCategoryMode = false;

    public boolean isInLauncherCategoryMode() {
        return mLauncherCategoryMode;
    }

    @Override
    public void setLauncherCategoryMode(boolean mode) {
        mLauncherCategoryMode = mode;
    }

    @Override
    public void enterLauncherCategoryMode() {
        if (mWorkspaceLoading || mWaitingForResult) {
            return;
        }
        Log.d(TAG, "enterLauncherCategoryMode: " + mLauncherCategoryMode);
        if (isHideseatShowing()) {
            hideHideseat(false);
        }
        mWorkspace.setCurrentPage(mWorkspace.getIconScreenHomeIndex());
        mCategoryMode.setVisibility(View.VISIBLE);
        mCategoryModeOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLauncherCategoryMode) {
                    if (mWorkspace.isPageMoving()) {
                        mWorkspace.runOnPageStopMoving(new Runnable() {
                            @Override
                            public void run() {
                                mModel.coverAllIcons();
                            }
                        });
                    } else {
                        mModel.coverAllIcons();
                    }
                }
            }
        });
        mCategoryModeCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLauncherCategoryMode) {
                    if (mWorkspace.isPageMoving()) {
                        mWorkspace.runOnPageStopMoving(new Runnable() {
                            @Override
                            public void run() {
                                mModel.recoverAllIcons(false);
                            }
                        });
                    } else {
                        mModel.recoverAllIcons(false);
                    }
                }
            }
        });
        mEditModeHelper.updateEditModeTips(PreviewContentType.Preview);
        mWorkspace.enterLauncherCategoryMode();
        mSortMenu.setVisibility(View.INVISIBLE);
        mIsSortMenuShowing = false;
        mWorkspace.invalidatePageIndicator(true);
        updateCategoryPreviewView();
    }

    @Override
    public void exitLauncherCategoryMode(boolean ok) {
        Log.d(TAG, "exitLauncherCategoryMode: " + mLauncherCategoryMode);
        mCategoryMode.setVisibility(View.GONE);
        mCategoryModeOk.setOnClickListener(null);
        mCategoryModeCancel.setOnClickListener(null);
        mEditModeHelper.updateEditModeTips(PreviewContentType.IconSort);
        mWorkspace.exitLauncherCategoryMode();
        mSortMenu.setVisibility(View.VISIBLE);
        mIsSortMenuShowing = true;
        if (!ok) {
            mWorkspace.setCurrentPage(mWorkspace.getIconScreenHomeIndex());
        }
        mWorkspace.invalidatePageIndicator(true);
        mHotseat.setAtomIconClickable(true);
    }
    /* YUNSO END */

    public void onWorkspacePageBeginMoving() {
        for (WeakReference<GadgetView> wrf : gadgetViewList) {
            GadgetView v = wrf.get();
            if (v != null) {
                v.onPause();
            }
        }
        // AppIconsHelper.pauseAll();
    }

    public void onWorkspacePageEndMoving() {
        ArrayList<WeakReference<GadgetView>> tmpList = new ArrayList<WeakReference<GadgetView>>();
        for (WeakReference<GadgetView> wrf : gadgetViewList) {
            GadgetView v = wrf.get();
            if (v != null) {
                v.onResume();
            } else {
                tmpList.add(wrf);
            }
        }
        for (WeakReference<GadgetView> wrf : tmpList) {
            if (gadgetViewList.contains(wrf)) {
                gadgetViewList.remove(wrf);
            }
        }
        // AppIconsHelper.resumeAll();
    }

    void addGadgetView(GadgetView v) {
        gadgetViewList.add(new WeakReference<GadgetView>(v));
    }

    public CardBridge getCardBridge() {
        return mCardBridge;
    }

    public boolean isInLeftScreen() {
        if (mSupportLifeCenter && getCurrentScreen() == CardBridge.LEFT_SCREEN_INDEX) {
            return true;
        }

        return false;
    }

    public boolean isLeftScreen(int index) {
        if (mSupportLifeCenter && index == CardBridge.LEFT_SCREEN_INDEX) {
            return true;
        }

        return false;
    }

    public boolean isWidgetScreen(int index) {
        if (FeatureUtility.hasFullScreenWidget()) {
            CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(index);
            return cellLayout.isWidgetPage();
        }
        return false;
    }

    /* YUNOS BEGIN for LauncherEditMode */
    private CellLayout mLifeCenterCellLayout;
    private boolean mLauncherEditMode;
    private PreviewContainer mEditModePreviewContainer;
    private boolean mIsMenuShowing = false;
    private boolean mIsSortMenuShowing = false;
    private EditModeHelper mEditModeHelper = EditModeHelper.getInstance();
    public static final String TAG_EDITMODE = "Launcher.EditMode";

    public boolean isMenuShowing() {
        return mIsMenuShowing;
    }

    public void setMenuShowing(boolean menuShowing) {
        mIsMenuShowing = menuShowing;
    }

    public boolean isSortMenuShowing() {
        return mIsSortMenuShowing;
    }

    public void setSortMenuShowing(boolean sortMenuShowing) {
        mIsSortMenuShowing = sortMenuShowing;
    }

    public boolean isInLauncherEditMode() {
        return mLauncherEditMode;
    }

    public void setLauncherEditMode(boolean editmode) {
        mLauncherEditMode = editmode;
    }

    public void dismissMenu() {
        if (mIsMenuShowing) {
            mIsMenuShowing = false;
            mEditModeHelper.playDismissMenuAnimation();
        }
    }

    public void dismissSortMenu() {
        if (mIsSortMenuShowing) {
            mIsSortMenuShowing = false;
            mEditModeHelper.playDismissSortMenuAnimation();
        }
    }

    public AdapterView<?> getPreviewList() {
        return mEditModePreviewContainer.getListView();
    }

    public void enterLauncherEditMode() {
        if (mWorkspaceLoading || mWaitingForResult) {
            return;
        }
        if (mLauncherEditMode || AgedModeUtil.isAgedMode()) {
            return;
        }
        if (mWorkspace.isPageMoving() || mDragController.isDragging()) {
            return;
        }
        if (getWorkspace().getTranslationY() != 0) {
            return;
        }
        if (mMenu == null) {
            getMenu();
        }
        setGlobalSearchVisibility(View.GONE);

        /* YUNOS BEGIN */
        // ##modules:HomeShell ##author:xiangnan.xn@alibaba-inc.com
        // ##BugID:7924954 ##date:2015/02/26
        // double check if Launcher is in FlipAnimate Showing status
        // enterLauncherEditMode is being executed delayed after the
        // first check, FlipAnim may started between two checks.
        if (mFlipAnim.isShowing()) {
            stopFlipWithoutAnimation();
        }
        /* YUNOS END */

        mEditModeHelper.enterLauncherEditMode();
    }

    public void exitLauncherEditMode(boolean anim) {
        if (!mLauncherEditMode || AgedModeUtil.isAgedMode() || mWorkspace.isPageMoving()
                || mDragController.isDragging()) {
            return;
        }
        mEditModeHelper.exitLauncherEditMode(anim);
        if(!mLauncherEditMode){
            getSearchBridge().onAnimationUpdate(0);
            setGlobalSearchVisibility(View.VISIBLE);
        }

    }

    public UnlockAnimation getUnlockAnimation() {
        return mUnlockAnimation;
    }

    public View getMenu() {
        if (mMenu == null) {
            mMenu = findViewById(R.id.homshell_menu);
            OnClickListener menuOnClick = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMenuItemClicked(v.getId());
                }
            };
            mMenu.findViewById(R.id.menu_item_wallpaper).setOnClickListener(menuOnClick);
            if (Utils.isSupportTheme(this)) {
                mMenu.findViewById(R.id.menu_item_theme).setOnClickListener(menuOnClick);
            } else {
                mMenu.findViewById(R.id.menu_item_theme).setVisibility(View.GONE);
            }
            mMenu.findViewById(R.id.menu_item_widgets).setOnClickListener(menuOnClick);
            mMenu.findViewById(R.id.menu_item_effects).setOnClickListener(menuOnClick);
            mMenu.findViewById(R.id.menu_item_sort).setOnClickListener(menuOnClick);
            if (Utils.isForLightUpdate()) {
                mMenu.findViewById(R.id.menu_item_homeshell_setting).setOnClickListener(menuOnClick);
            } else {
                mMenu.findViewById(R.id.menu_item_homeshell_setting).setVisibility(View.GONE);
            }
        }
        return mMenu;
    }

    public View getSortMenu() {
        if (mSortMenu == null) {
            mSortMenu = findViewById(R.id.sort_menu);
            OnClickListener sortMenuOnClick = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (SystemClock.elapsedRealtime() - mClickSortMenuTime > 1000) {
                        onSortMenuItemClicked(v.getId());
                        mClickSortMenuTime = SystemClock.elapsedRealtime();
                    }
                }
            };
            mSortMenu.findViewById(R.id.menu_item_sort_page).setOnClickListener(sortMenuOnClick);
            mSortMenu.findViewById(R.id.menu_item_sort_all).setOnClickListener(sortMenuOnClick);
            mSortMenu.findViewById(R.id.menu_item_category).setOnClickListener(sortMenuOnClick);
        }
        return mSortMenu;
    }

    public EditModeHelper getEditModeHelper() {
        return mEditModeHelper;
    }

    public PreviewContainer getEditmodeContainer() {
        return mEditModePreviewContainer;
    }

    public void onMenuItemClicked(int itemId) {
        if (!mLauncherEditMode || mEditModeHelper.isEditModeAniamtionRunning()) {
            return;
        }
        /* YUNOS BEGIN */
        // ## modules(Home Shell)
        // ## date: 2016/03/10 ## author: wangye.wy
        // ## BugID: 7945871: item of icon sort
        if (mDragController.isDragging()) {
            return;
        }
        PreviewContentType type = mEditModePreviewContainer.getContentType();
        if (type == PreviewContentType.IconSort) {
            return;
        }
        /* YUNOS END */
        switch (itemId) {
            case R.id.menu_item_wallpaper :
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WALLPAPER);
                if (Utils.isSupportWallPaper(this)) {
                    mEditModeHelper.switchFromMenuToEmContainer();
                    mEditModePreviewContainer.setContentType(PreviewContentType.Wallpapers);
                    mEditModeHelper.updateEditModeTips(PreviewContentType.Wallpapers);
                } else {
                    startWallPaper();
                }
                break;
            case R.id.menu_item_theme :
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_THEME);
                mEditModeHelper.switchFromMenuToEmContainer();
                mEditModePreviewContainer.setContentType(PreviewContentType.Themes);
                mEditModeHelper.updateEditModeTips(PreviewContentType.Themes);
                break;
            case R.id.menu_item_widgets :
                mEditModeHelper.switchFromMenuToEmContainer();
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_WIDGET);
                mEditModePreviewContainer.setContentType(PreviewContentType.Widgets);
                mEditModeHelper.updateEditModeTips(PreviewContentType.Widgets);
                break;
            case R.id.menu_item_effects :
                mEditModeHelper.switchFromMenuToEmContainer();
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_EFFECTS);
                mEditModePreviewContainer.setContentType(PreviewContentType.Effects);
                mEditModeHelper.updateEditModeTips(PreviewContentType.Effects);
                break;
            /* YUNOS BEGIN */
            // ## modules(Home Shell)
            // ## date: 2016/03/10 ## author: wangye.wy
            // ## BugID: 7945871: item of icon sort
            case R.id.menu_item_sort :
                if (type != PreviewContentType.None && type != PreviewContentType.CellLayouts) {
                    break;
                }
                if (mSortMenu == null) {
                    mSortMenu = getSortMenu();
                }
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE);
                mEditModeHelper.switchFromMenuToSortMenu();
                mEditModePreviewContainer.setContentType(PreviewContentType.IconSort);
                mEditModeHelper.updateEditModeTips(PreviewContentType.IconSort);
                break;
            /* YUNOS END */
            case R.id.menu_item_homeshell_setting :
                Intent intent = new Intent(this, HomeShellSetting.class);
                startActivity(intent);
                break;
        }
    }

    public void onSortMenuItemClicked(int itemId) {
        if (!mLauncherEditMode || mEditModeHelper.isEditModeAniamtionRunning()) {
            return;
        }
        if (mEditModePreviewContainer.getContentType() == PreviewContentType.CellLayouts
                || getModel().isInProcess()) {
            return;
        }
        final CellLayout cl = (CellLayout)mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        switch (itemId) {
            case R.id.menu_item_sort_page:
                final int screen = mWorkspace.getIconScreenIndex(mWorkspace.getCurrentPage());
                final Map<String, String> param = new HashMap<String, String>();
                param.put("screen", String.valueOf(screen + 1));
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_CURRENTSCREEN, param);
                mEditModeHelper.switchFromSortMenuToMenu();
                mEditModePreviewContainer.setContentType(PreviewContentType.CellLayouts);
                mEditModeHelper.updateEditModeTips(PreviewContentType.CellLayouts);
                cl.reOrderIcons();
                break;
            case R.id.menu_item_sort_all:
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_ALLSCREEN);
                mEditModeHelper.switchFromSortMenuToMenu();
                mEditModePreviewContainer.setContentType(PreviewContentType.CellLayouts);
                mEditModeHelper.updateEditModeTips(PreviewContentType.CellLayouts);
                mWorkspace.setReOrderAll(true);
                cl.reOrderAllIcons();
                break;
            case R.id.menu_item_category:
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_ENTRY_MENU_ARRANGE_CLASSIFY);
                mWorkspace.getScreenCount();
                getModel().reCategoryAllIcons();
                break;
        }
    }

    public void onThemeChanged() {
        if (mLauncherEditMode) {
            mDragController.cancelDrag();
            mDragController.resetLastGestureUpTime();
            if (mEditModePreviewContainer != null) {
                mEditModePreviewContainer.onExit(true);
            }
        }
    }

    public void addAppWidget(AppWidgetProviderInfo info, int newScreen) {
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:guoshuai.lgs
        // ##BugID:(158083) ##date:2014/10/28
        // ##decrpition: Do not add widget at fake layout.
        if (newScreen == -1) {
            newScreen = getCurrentWorkspaceScreen();
        }
        CellLayout layout = (CellLayout) mWorkspace.getChildAt(newScreen);
        if ((layout != null)) {
            PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(info, null, null);
            int[] spanXY = Launcher.getSpanForWidget(this, info);
            pendingInfo.spanX = spanXY[0];
            pendingInfo.spanY = spanXY[1];
            int[] minSpanXY = Launcher.getMinSpanForWidget(this, info);
            pendingInfo.minSpanX = minSpanXY[0];
            pendingInfo.minSpanY = minSpanXY[1];
            pendingInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET;
            if (!ConfigManager.isLandOrienSupport()) {
                if (layout.checkSpaceAvailable(pendingInfo, Configuration.ORIENTATION_PORTRAIT)) {
                    addAppWidgetFromDrop(pendingInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, newScreen, null, null, null);
                } else {
                    showOutOfSpaceMessage(isHotseatLayout(layout));
                }
            } else {
                if (layout.checkSpaceAvailable(pendingInfo, Configuration.ORIENTATION_LANDSCAPE)
                    && layout.checkSpaceAvailable(pendingInfo, Configuration.ORIENTATION_PORTRAIT)) {
                    addAppWidgetFromDrop(pendingInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, newScreen, null, null, null);
                } else {
                    String strToast = getResources().getString(R.string.toast_no_space_for_widget);
                    String strOrientation = null;
                    if (LauncherApplication.isInLandOrientation()) {
                        strOrientation = getResources().getString(R.string.orientation_port);
                    } else {
                        strOrientation = getResources().getString(R.string.orientation_land);
                    }
                    Toast.makeText(this, String.format(strToast, TextUtils.htmlEncode(strOrientation)), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
        /* YUNOS END PB */
    }

    public void addGadgetWidget(GadgetInfo info, int newScreen) {
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:guoshuai.lgs
        // ##BugID:(158083) ##date:2014/10/28
        // ##decrpition: Do not add widget at fake layout.
        if(newScreen == -1){
            newScreen = getCurrentWorkspaceScreen();
        }
        CellLayout layout = (CellLayout) mWorkspace.getChildAt(newScreen);
        if ((layout != null)) {
            GadgetItemInfo gadgetInfo = new GadgetItemInfo(info);
            boolean foundCellSpan = false;
            int[] cell = new int[2];
            int[] span = new int[2];
            span[0] = info.spanX;
            span[1] = info.spanY;
            foundCellSpan = layout.findCellForSpan(cell, span[0], span[1]);
            if (foundCellSpan && !ConfigManager.isLandOrienSupport()) {
                addGadgetFromDrop(gadgetInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, newScreen, cell, span, null);
            } else if (foundCellSpan) {
                if (layout.checkSpaceAvailable(gadgetInfo, Configuration.ORIENTATION_LANDSCAPE)
                        && layout.checkSpaceAvailable(gadgetInfo, Configuration.ORIENTATION_PORTRAIT)) {
                    addGadgetFromDrop(gadgetInfo, LauncherSettings.Favorites.CONTAINER_DESKTOP, newScreen, cell, span, null);
                } else {
                    String strToast = getResources().getString(R.string.toast_no_space_for_widget);
                    String strOrientation = null;
                    if (LauncherApplication.isInLandOrientation()) {
                        strOrientation = getResources().getString(R.string.orientation_port);
                    } else {
                        strOrientation = getResources().getString(R.string.orientation_land);
                    }
                    Toast.makeText(this, String.format(strToast, TextUtils.htmlEncode(strOrientation)), Toast.LENGTH_SHORT).show();
                }
            } else {
                showOutOfSpaceMessage(isHotseatLayout(layout));
            }
        } else {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
        /* YUNOS END PB */
    }

    public void addShortcut(ActivityInfo activityInfo, int newScreen) {
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:guoshuai.lgs
        // ##BugID:(158083) ##date:2014/10/28
        // ##decrpition: Do not add widget at fake layout.
        if (newScreen == -1) {
            newScreen = getCurrentWorkspaceScreen();
        }

        ComponentName component = new ComponentName(activityInfo.packageName, activityInfo.name);
        processShortcutFromDrop(component, LauncherSettings.Favorites.CONTAINER_DESKTOP, newScreen, null, null);
        /* YUNOS END PB */
    }

    private void startWallPaper() {
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper, getText(R.string.chooser_wallpaper));
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    }
    /*YUNOS END  for LauncherEditMode*/

    public boolean isInWidgetPage() {
        return FeatureUtility.hasFullScreenWidget() && ((CellLayout) mWorkspace
                .getChildAt(mWorkspace.getCurrentPage())).isWidgetPage();
    }
    @Override
    public void removeWidgetPages() {
        if (mWorkspace != null) {
            mWorkspace.setCurrentPage(DEFAULT_SCREEN);
            mWorkspace.removeWidgetPages();
        }
    }
    @Override
    public void makesureWidgetPages() {
        if (mWorkspace != null) {
            mWorkspace.makeSureWidgetPages();
        }
    }

    public void checkLifeCenter() {
        if (CardBridge.checkLifecenterPackage(this)) {
            if (ConfigManager.isLandOrienSupport()) {
                mSupportLeftScreen = true;
            } else {
                mSupportSearchBridge = true;
                if (CardBridge.isCMCC(this)) {
                    if (CardBridge.getCloudCardEnableValue(this)) {
                        mSupportLifeCenter = true;
                    } else {
                        mSupportLifeCenter = false;
                    }
                } else {
                    mSupportLifeCenter = true;
                }
            }
        }
    }

    boolean isLeftScreenOpened = false;
    public boolean isLeftScreenOpened() {
        return mSupportLeftScreen && isLeftScreenOpened;
    }

    public void openLeftScreen(boolean force) {
        if (!force) {
            if (isLeftScreenOpened) {
                return;
            }
        }

        if (mCardBridge != null) {
            mCardBridge.openLeftScreen();
        }
        isLeftScreenOpened = true;
    }

    public void closeLeftScreen(boolean force) {
        if (!force) {
            if (!isLeftScreenOpened || mCardBridge == null) {
                return;
            }
        }

        if (mCardBridge != null) {
            mCardBridge.closeLeftScreen();
        }
        isLeftScreenOpened = false;
    }
    public void updateSearchAnimationProgress(Bundle params) {
        if (mSearchBridge != null) {
                mSearchBridge.updateSearchAnimationProgress(params);
            }
    }
    public SearchBridge getSearchBridge(){
        if (mSearchBridge == null) {
            mSearchBridge = new SearchBridge(this);
        }
        return mSearchBridge;
    }


    public void setGlobalSearchVisibility(int visibility) {
        if(SearchBridge.isHomeShellSupportGlobalSearchUI(this)) {
            FrameLayout  globalSearchBox = (FrameLayout)findViewById(R.id.GlobalSearch);
            if (globalSearchBox == null ) {
                Log.w(TAG, "not find global searchBox");
                return;
            }
            globalSearchBox.setVisibility(visibility);
        }
    }


    public TitleColorManager getTitleColorManager() {
        return mTitleColorManager;
    }

    private void updateTitleColor() {
        if (getIconManager() == null || mTitleColorManager == null) {
            return;
        }
        mTitleColorManager.supprtCardIcon(getIconManager().supprtCardIcon());
        mTitleColorManager.needUpdateColor();
        if (FeatureUtility.supportDyncColor() && HomeShellSetting.getIconDyncColor(getApplicationContext())) {
            mHandler.removeCallbacks(mRefeshViewTextColor);
            mHandler.post(mRefeshViewTextColor);
        }
    }

    private Runnable mRefeshViewTextColor = new Runnable(){

        @Override
        public void run() {
            //update workspace icon title color
            for (int i = 0; i < mWorkspace.getChildCount(); i++) {
                CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(i);
                if (!isLeftScreen(i)) {
                    ViewGroup group = cellLayout.getShortcutAndWidgetContainer();
                    if (group != null) {
                        int childCount = group.getChildCount();
                        for (int index = 0; index < childCount; index++) {
                            View child = group.getChildAt(index);
                            child.postInvalidate();
                        }
                    }
                }
            }
            // update hotseat icon title color
            CellLayout cellLayout = (CellLayout) mHotseat.getLayout();
            ViewGroup group = cellLayout.getShortcutAndWidgetContainer();
            if (group != null) {
                for (int index = 0; index < group.getChildCount(); index++) {
                    View child = group.getChildAt(index);
                    child.postInvalidate();
                }
            }
        }
    };

    public void updateDisplayStyle(final boolean force) {
        if(force && FootprintBase.ACTIVATE_STATUS_ACTIVATE_CANCEL != mFootprintBase.getActivateStatus()){
            Launcher.this.recreate();
        }
    }

    final private static boolean sIsDeferInstallAppEnabled =
            SystemProperties.getBoolean("ro.yunos.defer.install_app", false);

    private void startActivateYunOSService() {
        if (!sIsDeferInstallAppEnabled || mLauncherView == null || mIsActivateGuide || !isProductionSignature()) {
            Log.d(TAG, "startActivateYunOSService mLauncherView:" + mLauncherView + "  mIsActivateGuide:" + mIsActivateGuide);
            return;
        }
        mIsActivateGuide = true;
        mActivateFragment = new ActivateGuideFragment();
        getFragmentManager().beginTransaction()
                    .add(R.id.gesture_layer, mActivateFragment).commit();
    }

    public void updateCategoryPreviewView(){
        if(mActivateFragment != null){
            new Handler().postDelayed(new Runnable(){
                public void run() {   
                    if(mActivateFragment != null){
                        int cellcount = mWorkspace.getChildCount();
                        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                        for (int cellIdx = 0; cellIdx < cellcount; cellIdx++) {
                            View v = mWorkspace.getChildAt(cellIdx);
                            if(R.id.lifecenter_cell != v.getId() || mLifeCenterCellLayout == null){
                                v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
                                v.buildDrawingCache(true);
                                bitmaps.add(v.getDrawingCache(true));
                            }
                        }
                        mActivateFragment.setCategoryPreview(bitmaps);
                    }
                }
             }, 800);
        }
    }

    public void coverAllIcons(){
        if (mLauncherCategoryMode) {
            if (mWorkspace.isPageMoving()) {
                mWorkspace.runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        mModel.coverAllIcons();
                    }
                });
            } else {
                mModel.coverAllIcons();
            }
        }
    }

    public void recoverAllIcons(){
        if (mLauncherCategoryMode) {
            if (mWorkspace.isPageMoving()) {
                mWorkspace.runOnPageStopMoving(new Runnable() {
                    @Override
                    public void run() {
                        mModel.recoverAllIcons(true);
                    }
                });
            } else {
                mModel.recoverAllIcons(true);
            }
        }
    }

    public void exitActivateGuideMode(){
        mIsActivateGuide = false;
        mActivateFragment = null;
    }

    public void cancelActivate(){
        if(mFootprintBase != null){
            mFootprintBase.setActivateStatus(FootprintBase.ACTIVATE_STATUS_ACTIVATE_CANCEL);
            mFootprintBase.incraseTriggerPeriodic();
            mFootprintBase.setActivateDelay();
        }
    }

    public void completeActivate(){
        if(mFootprintBase != null){
            mFootprintBase.setActivateStatus(FootprintBase.ACTIVATE_STATUS_ACTIVATE_DONE);
            mFootprintBase.cancelActivateNotify();
        }
    }

    public boolean isIsActivateGuide(){
        return mIsActivateGuide;
    }

    public boolean isProductionSignature(){
        if( mFootprintBase != null){
            if(TextUtils.isEmpty(mShipSig)){
                mShipSig = mFootprintBase.getSign(this);
            }
            Log.d(TAG, "isProductionSignature mShipSig:" + mShipSig);
            return mFootprintBase.ship_sig.equals(mShipSig);
        }
        return false;
    }
}

interface LauncherTransitionable {
    View getContent();
    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStep(Launcher l, float t);
    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
}
