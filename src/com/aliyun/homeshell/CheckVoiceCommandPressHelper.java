/**
 * YUNOS added by xiaodong.lxd for push to talk
 */

package com.aliyun.homeshell;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.aliyun.homeshell.DropTarget.DragObject;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.utility.FeatureUtility;

public class CheckVoiceCommandPressHelper {

    public static boolean PUSH_TO_TALK_SUPPORT = true;
    private static boolean mServiceConnected = false;

    private static final String TAG = "CheckVoiceCommandPressHelper";
    private int mTimeOut;;
    private int mOffsetDock;
    private int mStatusBarHeight;

    private static final String BROADCAST_PUSHTALK_STARTED = "com.yunos.vui.pushtalk.viewshow";
    private static final String BROADCAST_PUSHTALK_DISMISSED = "com.yunos.vui.pushtalk.viewhide";
    public static final String BROADCAST_PUSHTALK_SWITCH_CHANGED = "com.yunos.vui.switchChanged";
    public static final String BROADCAST_PUSHTALK_KILLED_SELF = "android.intent.action.ALIVOICE_KILLED";
    //private static final String sSwitchVUI = "switch_yunos_vui";

    private StringBuilder mStringBuilder;

    private View mView;
    private ShortcutInfo mShortcutInfo;
    private boolean mHasPerformedCheckVoiceCommand;
    private boolean mVoiceUIShown;
    private boolean mTouchInHotzone;
    private CheckForVoiceCommand mPendingCheckForVoiceCommand;
    private boolean mMsgShowSent;

    private static final int MSG_PUSHTALK_SHOW = 1;
    private static final int MSG_PUSHTALK_HIDE = 2;
    private static final int MSG_PUSHTALK_FORCE_HIDE = 3;

    private Context mContext = null;
    private Launcher mLauncher = null;
    private Messenger mMessenger;
    private static CheckVoiceCommandPressHelper instance = new CheckVoiceCommandPressHelper();

    private HashMap<String, String> speechPackageMap = new HashMap<String, String>();
    private Rect mRect;

    class CheckForVoiceCommand implements Runnable {

        private String mComponentName = null;

        public CheckForVoiceCommand(String componetName) {
            mComponentName = componetName;
        }

        @Override
        public void run() {
        	boolean isFreezeModeOn = HomeShellSetting.getFreezeValue(mLauncher);
            boolean isDragging = (mLauncher != null && mLauncher.getDragController() != null && mLauncher.getDragController().isDragging());
        	Log.d(TAG, "sxsexe-------------->CheckForVoiceCommand.run "
        			+ " freezemode " + isFreezeModeOn + " isDragging " + isDragging);
            if (!isFreezeModeOn && !isDragging) {
                mHasPerformedCheckVoiceCommand = false;
                mTouchInHotzone = false;
                mView = null;
                return;
            }
            
            if (mHasPerformedCheckVoiceCommand && mTouchInHotzone && mMessenger != null) {
                int location[] = new int[2];
                setTouchViewCoordinate(mView, location);
                int iconX = location[0];
                int iconY = location[1] - mStatusBarHeight;
                
                try {
                    Message msg = Message.obtain(null, MSG_PUSHTALK_SHOW);
                    Bundle data = new Bundle();
                    data.putInt("icon_x", iconX);
                    data.putInt("icon_y", iconY);
                    data.putInt("icon_width", mView.getWidth());
                    data.putInt("icon_height", mView.getHeight());
                    data.putString("scene", mComponentName);
                    msg.setData(data);
                    Log.d(TAG, "sxsexe-------------->mMessenger.send X " + iconX + " Y " + iconY
                            + " string " + mComponentName + " width " + mView.getWidth()
                            + " height " + mView.getHeight());
                    mMessenger.send(msg);
                    mMsgShowSent = true;
                    hideTrashBar();
                    //added by xiaodong.lxd #103558
                    UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_PUSH_TO_TALK);
                } catch (Exception e) {
                    Log.e(TAG, "sxsexe--------------> send message to Voice Error " + e);
                }
            } else {
                mHasPerformedCheckVoiceCommand = false;
                //mView = null;
            }
        }
    }

    private CheckVoiceCommandPressHelper() {
        mStringBuilder = new StringBuilder();
        mRect = new Rect();
        mContext = LauncherApplication.getContext();
    }

    public static CheckVoiceCommandPressHelper getInstance() {
        return instance;
    }
    
    /**
     * Check info is a voice app
     * @param info
     * @return
     */
    public  boolean isVoiceApp(ShortcutInfo info) {
        if(getVoiceCommandComponet(info) == null) {
            return false;
        } else {
            return true;
        }
    }

    private String getVoiceCommandComponet(ShortcutInfo info) {
        String result = null;
        if(mStringBuilder == null) {
            mStringBuilder = new StringBuilder();
        }
        if (mStringBuilder.length() != 0) {
            mStringBuilder.delete(0, mStringBuilder.length());
        }
        try {
            mStringBuilder.append(info.intent.getComponent().getPackageName()).append("/")
                    .append(info.intent.getComponent().getClassName());
            // Log.d(TAG, "sxsexe--------------> mStringBuilder " +
            // mStringBuilder.toString());
            result = speechPackageMap.get(mStringBuilder.toString());
        } catch (Exception e) {
            Log.e(TAG, "sxsexe------> getVoiceCommandComponet error : " + e.toString());
        }
        return result;
    }
    
    private void setTouchViewCoordinate(View view, int[] location) {
        if(view == null) {
            return;
        }
        
        view.getLocationOnScreen(location);
        if(location[0] <= 0 || location[1] <= 0) {
            if(mLauncher != null && mLauncher.getDragController() != null) {
                DragView dragView =   mLauncher.getDragController().getDragView();
                if(dragView != null) {
                    dragView.getLocationOnScreen(location);
                }
            }
        }
    }
    
    private void buildHotZone(View view) {
        ItemInfo info = (ItemInfo) view.getTag();
        if (isViewFromHotSeat()) {
            view.getHitRect(mRect);
            Hotseat hotseat = mLauncher.getHotseat();
            int topOffset = hotseat.getTop();
            mRect.offset(0, topOffset);
            mRect.left -= mOffsetDock;
            mRect.top -= mOffsetDock;
            mRect.right += mOffsetDock;
            mRect.bottom += mOffsetDock;
        } else if (view.getParent() != null && (view.getParent() instanceof ShortcutAndWidgetContainer)) {
            int result[] = new int[2];
            CellLayout cellLayout = (CellLayout) ((ShortcutAndWidgetContainer) view.getParent()).getParent();
            cellLayout.cellToPoint(info.cellX, info.cellY, result);
            mRect.left = (int) (result[0] + cellLayout.getCellWidth() * 0.1);
            mRect.top = (int) (result[1] + cellLayout.getCellHeight() * 0.1);
            mRect.right = (int) (cellLayout.getCellWidth()*0.8 + mRect.left);
            mRect.bottom = (int) (cellLayout.getCellHeight()*0.8 + mRect.top);
        }
    } 
    
    private void setTimeOut() {
        Resources res = mContext.getResources();
        if (AgedModeUtil.isAgedMode()) {
            mTimeOut = LauncherApplication.getLongPressTimeout() + res.getInteger(R.integer.push_to_talk_time_out_offset_agemode);
        } else {
            mTimeOut = res.getInteger(R.integer.push_to_talk_time_out);
        }
        Log.d(TAG, "setTimeOut " + mTimeOut);
    }

    public void postCheckVoiceCommand(final View view) {
        Log.d(TAG, "sxsexe------> postCheckVoiceCommand isVoiceSwitchOn : " + isVoiceSwitchOn() 
                + " mMessenger " + mMessenger
                 + " PUSH_TO_TALK_SUPPORT " + isPushTalkCanUse()
                 + " view.tag " + (view == null ? null : view.getTag()));
        if(!isPushTalkCanUse() || !isVoiceSwitchOn()) {
            return;
        }
        // only item in workspace or hotseat can trigger voice service
        if (view != null && view.getTag() != null) {
            if (!(view.getTag() instanceof ShortcutInfo)) {
                return;
            }
            
            ShortcutInfo info = (ShortcutInfo) view.getTag();
            if (checkContainerInvalid(info)) {
                return;
            }
            String result = getVoiceCommandComponet(info);
            if(TextUtils.isEmpty(result)) {
                return;
            }
        } else {
            return;
        }

        dumpVuiProcessInfo();
        mView = view;
        buildHotZone(view);
        if (mMessenger == null) {
            // added by xiaodong.lxd#5189052 if service is not
            // connected,re-connect it
            if (mLauncher != null) {
                mLauncher.postRunnableToMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "sxsexe----->postCheckVoiceCommand reConnectService mMessenger " + mMessenger + " mHasPerformedCheckVoiceCommand "
                                + mHasPerformedCheckVoiceCommand);
                        if (mMessenger == null) {
                            try {
                                mContext.bindService(new Intent("com.yunos.vui.pushtalk.Service"), mConn, Context.BIND_AUTO_CREATE);
                            } catch (Exception e) {
                                Log.e(TAG, "bind PushTalk Service Error");
                            }
                        }
                    }
                }, 500);
            }
            return;
        }
        
        if (!mHasPerformedCheckVoiceCommand) {
            ItemInfo info = (ItemInfo)view.getTag();
            String vcComponet = getVoiceCommandComponet((ShortcutInfo)info);
            if (vcComponet == null) {
                return;
            }
            
            if(info instanceof ShortcutInfo) {
                mShortcutInfo = (ShortcutInfo)info;
            }
            
            mHasPerformedCheckVoiceCommand = true;
            mTouchInHotzone = true;

            if (mPendingCheckForVoiceCommand == null) {
                mPendingCheckForVoiceCommand = new CheckForVoiceCommand(vcComponet);
            }
            setTimeOut();
            if(mLauncher != null) {
                mLauncher.postRunnableToMainThread(mPendingCheckForVoiceCommand, mTimeOut);
            }
        }
    }

    public void cancelCheckedVoiceCommand() {
        if(!isVoiceSwitchOn()) {
            return;
        }
        
        if (mPendingCheckForVoiceCommand != null && mHasPerformedCheckVoiceCommand) {
            Log.d(TAG, "sxsexe------------>cancelCheckedVoiceCommand ");
            if(mLauncher != null) {
                mLauncher.cancelRunnableInMainThread(mPendingCheckForVoiceCommand);
            }
            mPendingCheckForVoiceCommand = null;
            broadcastQuitVoiceCommand(false);
        }
        mHasPerformedCheckVoiceCommand = false;
        mView = null;
    }

    public boolean isVoiceUIShown() {
        return mVoiceUIShown;
    }
    
    public boolean isVoiceUIShowMsgSent() {
    	return mMsgShowSent;
    }

    public void forceDismissVoiceCommand() {
        if(!isVoiceSwitchOn()) {
            return;
        }
        Log.d(TAG, "sxsexe------------>forceDismissVoiceCommand mPendingCheckForVoiceCommand "
                + mPendingCheckForVoiceCommand + " mVoiceUIShown " + mVoiceUIShown);
         
        if (mPendingCheckForVoiceCommand != null) {
            if(mLauncher != null) {
                mLauncher.cancelRunnableInMainThread(mPendingCheckForVoiceCommand);
            }
            mPendingCheckForVoiceCommand = null;
        }
        if(mLauncher != null) {
            mLauncher.setOnClickValid(false);
        }
        broadcastQuitVoiceCommand(true);
        mHasPerformedCheckVoiceCommand = false;
        mView = null;
    }

    private void broadcastQuitVoiceCommand(final boolean force) {
        if (mVoiceUIShown && mMessenger != null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    
                    Log.d(TAG,
                            "sxsexe------------>broadcastQuitVoiceCommand send broadcast mMessenger "
                                    + mMessenger);
                     
                    try {
                        Message msg = Message.obtain(null, force ? MSG_PUSHTALK_FORCE_HIDE
                                : MSG_PUSHTALK_HIDE);
                        Log.d(TAG, "sxsexe------------>broadcastQuitVoiceCommand hide msg "
                                + msg.what);
                        mMessenger.send(msg);
                    } catch (Exception e) {
                        Log.e(TAG,
                                "sxsexe-------->broadcastQuitVoiceCommand error " + e.getMessage());
                    }
                }
            };
            if(mLauncher != null) {
                mLauncher.postRunnableToMainThread(r, 0);
            }
        }
    }

    public boolean hasPerformedCheckVoiceCommand() {
        return mHasPerformedCheckVoiceCommand;
    }

    public void checkDragRegion(DragObject dragObject, boolean overWorkspace, float[] dragViewVisualCenter) {
        if(!(dragObject.dragInfo instanceof ShortcutInfo)) {
            return;
        }
        ShortcutInfo info = (ShortcutInfo) dragObject.dragInfo;
        if (checkContainerInvalid(info)) {
            return;
        }
        String result = getVoiceCommandComponet(info);
        if(TextUtils.isEmpty(result)) {
            return;
        }

        if (mView != null) {
            if (overWorkspace && dragViewVisualCenter != null) {
                int dragViewCenterX = (int) dragViewVisualCenter[0];
                int dragViewCenterY = (int) dragViewVisualCenter[1];
                mTouchInHotzone = mRect.contains(dragViewCenterX, dragViewCenterY);
            } else {
                mTouchInHotzone = false;
            }

            if (!mTouchInHotzone) {
                Log.d(TAG, "sxsexe11---->Got outside ****mview rect " + mRect.flattenToString() + " d.x " + dragObject.x + " d.y " + dragObject.y);
                if (mHasPerformedCheckVoiceCommand) {
                    forceDismissVoiceCommand();
                    showTrashBar();
                }
            }
        }
    }
    
    private boolean checkContainerInvalid(ItemInfo info) {
        return info.container != Favorites.CONTAINER_DESKTOP && info.container != Favorites.CONTAINER_HOTSEAT;
    }

    private boolean isViewFromHotSeat() {
        ItemInfo info = (ItemInfo) mView.getTag();
        return info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT;
    }
    
    /**
     * de-init the voice service and unregister the receiver
     */
    public void deInitVoiceService() {
        if (mContext != null) {
            if (mReceiver != null) {
                try {
                    mContext.unregisterReceiver(mReceiver);
                    if (mConn != null && mMessenger != null) {
                        mContext.unbindService(mConn);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "deInitVoiceService Error", e);
                }
            }
            if (speechPackageMap != null) {
                speechPackageMap.clear();
            }
        }
    }

    public void initVoiceService() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_PUSHTALK_STARTED);
        filter.addAction(BROADCAST_PUSHTALK_DISMISSED);
        filter.addAction(BROADCAST_PUSHTALK_KILLED_SELF);
        mContext.registerReceiver(mReceiver, filter);
        
        try {
            mContext.bindService(new Intent("com.yunos.vui.pushtalk.Service"), mConn, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "bind PushTalk Service Error");
            mServiceConnected = false;
        }
        
        Resources res = mContext.getResources();
        setTimeOut();
        mOffsetDock = res.getDimensionPixelSize(R.dimen.push_to_talk_offset_dock);
        
        initSpeechList(res);
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        DisplayMetrics metric = new DisplayMetrics();
        mLauncher.getWindowManager().getDefaultDisplay().getMetrics(metric);
        Resources res = mContext.getResources();
        int resId = res.getIdentifier("status_bar_height", "dimen", "android");
        if(resId <= 0) {
            mStatusBarHeight = res.getInteger(R.integer.status_bar_height);
            mStatusBarHeight *= metric.density;
        } else {
            mStatusBarHeight = res.getDimensionPixelSize(resId);
        }
    }
    
    /**
     * receive switch flag in EngineerMode
     */
    protected BroadcastReceiver mSwitchReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "sxsexe------------------------>onReceive action " + action);
            if(BROADCAST_PUSHTALK_SWITCH_CHANGED.equals(action)) {
                if(isVoiceSwitchOn()) {
                    initVoiceService();
                } else {
                    deInitVoiceService();
                }
            }
        }
    };

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "sxsexe------------------------>onReceive action " + action);
            if (action == null || action.trim().length() == 0) {
                return;
            }
            mMsgShowSent = false;
            if (BROADCAST_PUSHTALK_DISMISSED.equals(action)) {
                mVoiceUIShown = false;
                mLauncher.setOnClickValid(true);
            } else if (BROADCAST_PUSHTALK_STARTED.equals(action)) {
                mVoiceUIShown = true;
                if(!mHasPerformedCheckVoiceCommand) {
                    forceDismissVoiceCommand();
                }
            } else if(BROADCAST_PUSHTALK_KILLED_SELF.equals(action)) {
                Log.d(TAG, "sxsexe-------> PushTalk Service Killed self TODO");
            }
        }
    };

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessenger = null;
            mServiceConnected = false;
            //Log.d(TAG, "sxsexe-------> PushTalk Service DisConnected");
            Log.d(TAG, "sxsexe-------> PushTalk Service Disconnected and rebind service");
            //when service disconnected by some unknown reason,reConnect the voice service in 500ms 
            if(mLauncher != null) {
                mLauncher.postRunnableToMainThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mContext.bindService(new Intent("com.yunos.vui.pushtalk.Service"), mConn, Context.BIND_AUTO_CREATE);
                        } catch (Exception e) {
                            mServiceConnected = false;
                            Log.e(TAG, "bind PushTalk Service Error");
                        }
                    }
                }, 500);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceConnected = true;
            mMessenger = new Messenger(service);
            Log.d(TAG, "sxsexe-------> PushTalk Service Connected mHasPerformedCheckVoiceCommand " + mHasPerformedCheckVoiceCommand
                    + " mTouchInHotzone " + mTouchInHotzone
                     + " mView " + mView);
            //added by xiaodong.lxd#5189052
            if(mView != null && !mHasPerformedCheckVoiceCommand && mTouchInHotzone) {
                postCheckVoiceCommand(mView);
            }
        }
    };

    private void initSpeechList(Resources res) {
        if(speechPackageMap != null) {
            speechPackageMap.clear();
        }
        
        TypedArray components = res.obtainTypedArray(R.array.components);
        TypedArray scenes = res.obtainTypedArray(R.array.scene);
        int length = components.length();
        for (int i = 0; i < length; i++) {
            String comp = components.getString(i);
            String scene = scenes.getString(i);
            speechPackageMap.put(comp, scene);
        }
    }

    private void showTrashBar() {
        if(mShortcutInfo.isSystemApp) {
            return;
        }
        SearchDropTargetBar searchDropTargetBar = mLauncher.getSearchBar();
        searchDropTargetBar.showDropTargetBar(true);
    }

    private void hideTrashBar() {
        if(mShortcutInfo.isSystemApp) {
            return;
        }
        SearchDropTargetBar searchDropTargetBar = mLauncher.getSearchBar();
        searchDropTargetBar.hideDropTargetBar(true);
    }
    
    private void dumpVuiProcessInfo() {
        ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager  
                .getRunningAppProcesses(); 
        if (appProcessList == null) {
            Log.d(TAG, "sxsexe--------------------------VUI process not found: appProcessList is null");
            return;
        }
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) { 
            String processName = appProcessInfo.processName; 
            if("com.yunos.vui".equals(processName)) {
                Log.d(TAG, "sxsexe--------------------------VUI has started");
                return;
            }
        }
        Log.d(TAG, "sxsexe--------------------------VUI process not found");
    }
    
    private static void checkLanguage() {
        Locale locale = Locale.getDefault();
        if(locale == null) return;
        String sLang = String.format("%s-%s",  locale.getLanguage(), locale.getCountry());
        if ("zh-CN".equalsIgnoreCase(sLang)) {
            PUSH_TO_TALK_SUPPORT = true;
        }else {
            PUSH_TO_TALK_SUPPORT  = false;
        }
    }

    /**
     * Check the environment if support PustToTalk
     */
    public static void checkEnvironment() {
        if(FeatureUtility.isYunOSInternational() || FeatureUtility.hasPushTalk()) {
            PUSH_TO_TALK_SUPPORT = false;
        } else {
            checkLanguage();
        }
        Log.d(TAG, "checkEnvironment PUSH_TO_TALK_SUPPORT " + PUSH_TO_TALK_SUPPORT);
    }
    /**
     * 1 : on; 0 : off
     * @return
     */
    public boolean isVoiceSwitchOn() {
        /*int flag = Settings.System.getInt(mContext.getContentResolver(),sSwitchVUI,0);
        return flag == 1;*/
        return true;
    }
    public static boolean isPushTalkCanUse() {
        return PUSH_TO_TALK_SUPPORT && mServiceConnected;
    }
}
