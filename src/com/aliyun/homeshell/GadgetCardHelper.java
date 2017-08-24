
package com.aliyun.homeshell;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import app.aliyun.v3.gadget.GadgetView;

import android.appwidget.AppWidgetHostView;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.appclone.AppCloneManager.CloneAppKey;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.BubbleController;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.HomeShellSetting;
import com.aliyun.homeshell.utils.PrivacySpaceHelper;
import com.aliyun.utility.FeatureUtility;
import yunos.ui.util.DynColorSetting;
import yunos.ui.util.ColorUtils;
import app.aliyun.aml.FancyDrawable;
import android.os.Build;

final public class GadgetCardHelper {
    private static final HashSet<String> sConstGadgetCard = new HashSet<String>();
    private static final String TAG = "GadgetCardHelper";
    private static final String EXTRA_USERID = "userId";
    private static GadgetCardHelper sInscance;
    private Context mContext;
    private View mPrivacyCard;
    private View mNotificationPanel;
    private HashMap<ComponentName, WeakReference<View>> mGadgetCache = new HashMap<ComponentName, WeakReference<View>>();
    private HashMap<CloneAppKey, WeakReference<View>> mBubbleTextViewCache = new HashMap<CloneAppKey, WeakReference<View>>();
    private HashSet<ComponentName> mEmptyGadgetCard = new HashSet<ComponentName>();
    private DynColorSetting mDynColorSetting;
    static {
        sConstGadgetCard.add("fm.xiami.yunos");
        sConstGadgetCard.add("com.android.calendar");
        sConstGadgetCard.add("com.android.settings");
        sConstGadgetCard.add("com.yunos.weatherservice");
        sConstGadgetCard.add("com.moji.aliyun");
        sConstGadgetCard.add("com.android.mms");
        sConstGadgetCard.add("com.xiami.walkman");
        sConstGadgetCard.add("sina.mobile.tianqitongyunos");
        sConstGadgetCard.add("com.aliyun.wireless.vos.appstore");
    }
    private HashMap<CloneAppKey, Integer> mAppNotiCounts = new HashMap<CloneAppKey, Integer>();
    private static ArrayList<Object> mWidgetInfos;
    private AppWidgetHostView mNotificationHostView;
    private int mNotificationWidgetId = -1;
    private GadgetCardHelper(Context context) {
        mContext = context;
        mWidgetInfos = new ArrayList<Object>();
        mDynColorSetting = new DynColorSetting(context.getResources().getConfiguration());
        final IntentFilter filter = new IntentFilter();
        filter.addAction("com.yunos.systemui.update_noti_count");
        mContext.registerReceiver(mReceiver, filter);
    }

    public View getPrivacyCard(ComponentName cn, final View v) {
        if (mPrivacyCard == null) {
            mPrivacyCard = LauncherGadgetHelper.getGadget(mContext, new ComponentName("com.yunos.privacy", ""));
        } else {
            // maybe the GadgetView has been clearUp(), so we be-bind it.
            ((GadgetView) mPrivacyCard).rebind();
        }
        if (mPrivacyCard != null)
            mPrivacyCard.setOnClickListener(new View.OnClickListener() {
                public void onClick(View gadget) {
                    try {
                        ShortcutInfo info = (ShortcutInfo) v.getTag();
                        LauncherApplication.getLauncher().startActivitySafely(v, info.intent, info);
                    } catch (Exception e) {
                    }
                }
            });
        return mPrivacyCard;
    }

    public static GadgetCardHelper getInstance(Context context) {
        if (sInscance == null)
            sInscance = new GadgetCardHelper(context.getApplicationContext());
        return sInscance;
    }

    private void clearViewCache() {
        mBubbleTextViewCache.clear();
        mEmptyGadgetCard.clear();
    }

    private void clearGadgetCache() {
        mGadgetCache.clear();
        if (mPrivacyCard != null) {
            if (mPrivacyCard instanceof GadgetView)
                ((GadgetView) mPrivacyCard).cleanUp();
            mPrivacyCard = null;
        }
    }

    public static void onThemeChanged() {
        if (sInscance != null) {
            sInscance.clearViewCache();
            sInscance.clearGadgetCache();
        }
    }

    public static void onLocaleChanged() {
        if (sInscance != null)
            sInscance.clearGadgetCache();
    }

    /* YUNOS_BEGIN */
    // ##modules(HomeShell): add for dynamic color of Card
    // ##date: 2015-11.10 author: ruijie.lrj@alibaba-inc.com
    public static void onColorChanged(Context context, DynColorSetting dynColorSetting) {
        if(sInscance != null){
            sInscance.changeGadgetColor(context, dynColorSetting);
        }
    }

    private void  changeGadgetColor(Context context, DynColorSetting dynColorSetting) {
        mDynColorSetting = dynColorSetting;
        int bgColor = mDynColorSetting.getColorValue(DynColorSetting.HEADER_COLOR, 0);
        if (bgColor == context.getResources().getColor(R.color.setting_header_color)){
            bgColor = 0;
        }
        int textColor = mDynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR, 0);
        if(textColor == context.getResources().getColor(R.color.setting_title_color)){
            textColor = 0;
        }
        Set<ComponentName> cnSet = mGadgetCache.keySet();
        for (ComponentName cn : cnSet){
            String pkgName = cn.getPackageName();
            if (shouldChangeColor(pkgName)) {
               View gadget = null;
               WeakReference<View> ref = mGadgetCache.get(cn);
               if (ref != null)
                   gadget = ref.get();
                if(gadget != null && gadget instanceof GadgetView){
                    ((GadgetView)gadget).setHeaderColor(bgColor, textColor);
               }
            }
        }
    }
    private boolean shouldChangeColor(String pkgName){
        boolean should = false;
        if (pkgName != null && !"fm.xiami.yunos".equalsIgnoreCase(pkgName)
                && !"com.android.calendar".equalsIgnoreCase(pkgName)
                && !"com.yunos.weatherservice".equalsIgnoreCase(pkgName)
                && !"com.moji.aliyun".equalsIgnoreCase(pkgName)
                && !"com.xiami.walkman".equalsIgnoreCase(pkgName)
                && !"sina.mobile.tianqitongyunos".equalsIgnoreCase(pkgName)){
                should = true;
        }
        return should;
    }
    /*YUNOS_END*/

    public View getCardView(ComponentName cn) {
        return getCardView(cn, null, null, null, false);
    }

    public View getCardView(ComponentName cn, View iconView, Drawable drawable, CharSequence appName, boolean hasYunOSMsg) {
        // Note: this method should keep consistency with method hasCardView().
        if (cn == null)
            return null;

        if (isPrivacy(cn)) {
            return getPrivacyCard(cn, iconView);
        }

        if (isNotificationWidgetCardAvaliable(cn, iconView)) {
            View v = getNotificationWidgetCard(cn, iconView, drawable, appName);
            if (v != null)
                return v;
        }

        if (isWidgetCardAvaliable(cn)) {
            View v = getWidgetCard(cn);
            if (v != null) {
                return v;
            }
        }

        if (isCardFromAssetsAvaliable(cn)) {
            View v = getGadgetCard(cn, true);
            if (v != null)
                return v;
        }

        if (isCardAvaliable(cn, hasYunOSMsg)) {
            View v = getGadgetCard(cn, false);
            if (v != null) {
                return v;
            }
        }

        return null;
    }

    private View getWidgetCard(ComponentName cn) {
        View widgetGadget = LauncherApplication.mLauncher.initWidgetView(cn);
        return widgetGadget;
    }

    private Bitmap createBitmapFromDrawable(Drawable drawable) {
        return Utilities.createIconBitmap(drawable, mContext);
    }

    private View getNotificationWidgetCard(ComponentName cn, View iconView, Drawable drawable, CharSequence appName) {
        boolean fetchAppName = TextUtils.isEmpty(appName);
        if (drawable == null || fetchAppName) {
            Intent intent = new Intent();
            intent.setComponent(cn);
            PackageManager pm = mContext.getPackageManager();
            ResolveInfo r = pm.resolveActivity(intent, 0);
            ActivityInfo info = null;
            if (r != null && (info = r.activityInfo) != null) {
                if (drawable == null)
                    drawable = info.loadIcon(pm);
                if (fetchAppName)
                    appName = info.loadLabel(pm);
            }
        }

        if (FeatureUtility.hasNotificationFeature()) {
            Bitmap bitmap = null;
            if (drawable instanceof FancyDrawable) {
                bitmap = createBitmapFromDrawable(drawable);
            }
            int userId = 0;
            if (iconView.getTag() instanceof ShortcutInfo) {
                ShortcutInfo info = (ShortcutInfo)iconView.getTag();
                userId = info.userId;
            }
            return getNotificationWidgetView(mContext, cn, userId, bitmap, appName);
        } else {
            mNotificationPanel = getNotificationView(cn.getPackageName(), drawable, appName);
            return mNotificationPanel;
        }
    }

    private View getGadgetCard(ComponentName cn, boolean isYunCard) {
        View v = getGadget(cn, isYunCard);
        if (v instanceof GadgetView) {
            ((GadgetView) v).rebind();
            /* YUNOS_BEGIN */
            // ##modules(HomeShell): add for dynamic color of Card
            // ##date: 2015-11.10 author: ruijie.lrj@alibaba-inc.com
            if (shouldChangeColor(cn.getPackageName())) {
                int bgcolor = mDynColorSetting.getColorValue(DynColorSetting.HEADER_COLOR, 0);
                int textcolor = mDynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR, 0);
                boolean restore = false; //BugID:8222514
                if (bgcolor == ColorUtils.WHITE) {
                    restore = true;
                }
                ((GadgetView) v).setHeaderColor(bgcolor, textcolor, restore);
            }
            /*YUNOS_END*/
        }
        if (v != null)
            return v;
        // we cache ComponentName which can not pair it's own gadget card to
        // boost process of get card
        mEmptyGadgetCard.add(cn);
        return null;
    }

    /* YUNOS BEGIN */
    // ##date:2015-2-5 ##author:zhanggong.zg ##BugID:5746131
    /**
     * This method is used to determine whether an icon has corresponding
     * card view or not.
     */
    public boolean hasCardView(ComponentName cn, View iconView, boolean hasYunOSMsg) {
        if (cn == null)
            return false;

        if(!FeatureUtility.hasBigCardFeature())
            return false;

        if (isNotificationWidgetCardAvaliable(cn, iconView)) {
            return true;
        }

        if (isWidgetCardAvaliable(cn)) {
            return true;
        }

        if (isCardFromAssetsAvaliable(cn)) {
            return true;
        }

        if (isCardAvaliable(cn, hasYunOSMsg)) {
            return true;
        }

        return false;
    }
    /* YUNOS END */

    private boolean isWidgetCardAvaliable(ComponentName cn) {
        if (LauncherApplication.mLauncher != null) {
            return LauncherApplication.mLauncher.hasWidgetView(cn);
        }
        return false;
    }

    private boolean isNotificationWidgetCardAvaliable(ComponentName cn, View iconView){
        int userId = 0;
        if (iconView != null && iconView.getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo)iconView.getTag();
            userId = info.userId;
        }
        if ("com.android.calendar".equals(cn.getPackageName())
                || "com.android.calendar.AllInOneActivity".equals(cn.getClassName())) {
            return false;
        }

        int notifications = getNotificationCount(cn, userId, iconView);
        if (notifications > 0) {
            return true;
        }
        return false;
    }

    private boolean isCardAvaliable(ComponentName cn, boolean hasYunOSMsg) {
        if (cn != null && !mEmptyGadgetCard.contains(cn)) {
            String pkgName = cn.getPackageName();
            if ("com.yunos.alicontacts".equals(pkgName)) {
                return "com.yunos.alicontacts.activities.DialtactsContactsActivity".equals(cn.getClassName());
            }
            return sConstGadgetCard.contains(pkgName) || hasYunOSMsg;
        }
        return false;
    }

    /* YUNOS BEGIN */
    // ##date:2015-12-09
    // ##author:ruijie.lrj@alibaba-inc.com
    // ##BugID:6800546
    // ##Desc:to find out whether this app has yun card files inside of it's directory of "assets/"
    private boolean isCardFromAssetsAvaliable(ComponentName cn) {
        boolean hasYunCard = false;
        if (cn != null) {
            ActivityInfo aInfo = null;
            try {
                aInfo = LauncherApplication.mLauncher.getPackageManager().getActivityInfo(cn, PackageManager.GET_META_DATA);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return hasYunCard;
            }
            if (aInfo != null) {
                Bundle meta = aInfo.metaData;
                if (meta != null) {
                    hasYunCard = meta.getBoolean("yun_card");
                }
            }
        }
        return hasYunCard;
    }
    /*YUNOS_END*/

    private boolean shouldIgnoreNotification(ComponentName cn) {
        String pkgName = cn.getPackageName();
        String clazzName = cn.getClassName();
        if ("com.yunos.alicontacts".equals(pkgName)
                && "com.yunos.alicontacts.activities.DialtactsContactsActivity".equals(clazzName))
            return true;
        return false;
    }

    public boolean isPrivacy(ComponentName cn) {
        return PrivacySpaceHelper.getInstance(mContext).isPackageLocked(cn.getPackageName());
    }

    private View getGadget(ComponentName cn) {
        return getGadget(cn, false);
    }

    private View getGadget(ComponentName cn, boolean isYunCard) {
        View gadget = null;
        WeakReference<View> ref = mGadgetCache.get(cn);
        if (ref != null)
            gadget = ref.get();
        if (gadget == null) {
            Log.d(TAG, "get gadget card: " + cn);
            gadget = LauncherGadgetHelper.getGadget(mContext, cn, isYunCard);
            if (gadget != null)
                mGadgetCache.put(cn, new WeakReference<View>(gadget));
        }
        return gadget;
    }

    public int getNotificationCount(ComponentName cn, int userId, View view) {
        if (cn == null)
            return 0;
        String pkg = cn.getPackageName();
        if (pkg == null)
            return 0;
        View refView = null;
        // add listener
        CloneAppKey key = new CloneAppKey(userId, pkg);
        WeakReference<View> reference = mBubbleTextViewCache.get(key);
        if (reference != null) {
            refView = reference.get();
            if (refView != null && refView.getParent() == null)
                refView = null;
        }
        if (refView == null && view != null) {
            ShortcutInfo info = (ShortcutInfo) view.getTag();
            // ignore shortcut
            if (info == null || info.itemType != Favorites.ITEM_TYPE_APPLICATION)
                return 0;
            mBubbleTextViewCache.put(key, new WeakReference<View>(view));
        } else if (refView != null && view != null && refView != view) {
            ShortcutInfo cnRef = (ShortcutInfo) refView.getTag();
            if (cnRef != null && cnRef.intent != null) {
                if (cn.equals(cnRef.intent.getComponent()) && userId == cnRef.userId) {
                    mBubbleTextViewCache.put(key, new WeakReference<View>(view));
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }

        if (shouldIgnoreNotification(cn))
            return 0;

        if (FeatureUtility.hasNotificationFeature()) {
            return getNotificationCount(pkg, userId);
        } else {
            return 0;
        }
    }

    @Deprecated
    public CardNotificationPanelView getNotificationView(String pkg, Drawable draw, CharSequence appName) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        CardNotificationPanelView notificationView = (CardNotificationPanelView) inflater.inflate(
                R.layout.card_show_notification, null);
        notificationView.setPackage(pkg);
        notificationView.setAppName(appName);
        notificationView.setIcon(draw);
        notificationView.setHelper(sInscance);
//      notificationView.initStatusBarNotificationList(mSNL.getStatusBarNotificationList(pkg));
        notificationView.initStatusBarNotificationList(new ArrayList<StatusBarNotification>());
        return notificationView;
    }

    @Deprecated
    public void cancelNotification(StatusBarNotification sbn) {
//      mSNL.cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
    }

    private void refreshIconView(CloneAppKey key) {
        View view = null;

        WeakReference<View> reference = mBubbleTextViewCache.get(key);
        if (reference != null) {
            view = reference.get();
            /* YUNOS BEGIN */
            // ##date:2015-1-17 ##author:zhanggong.zg ##BugID:5681346
            // show or hide card indicator on BubbleTextView
            // ##date:2015-1-29 ##author:xiaodong.lxd ##BugID:5751028
            if (view instanceof BubbleTextView) {
                final BubbleTextView target = (BubbleTextView) view;
                Launcher launcher = LauncherApplication.getLauncher();
                if (launcher != null) {
                    launcher.postRunnableToMainThread(new Runnable() {
                        @Override
                        public void run() {
                            BubbleController.updateView(target);
                            target.postInvalidate();
                        }
                    }, 0);
                }
            }
            /* YUNOS END */
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if ("com.yunos.systemui.update_noti_count".equals(action)) {
                final Bundle bundle = (Bundle) intent.getBundleExtra("all_exist_pkgs");
                if (bundle != null) {
                    if (bundle.isEmpty()) {
                        return;
                    }
                    new Handler().post(new Runnable() {
                        public void run() {
                            Set<String> keySet = bundle.keySet();
                            Iterator iter = keySet.iterator();
                            while (iter.hasNext()) {
                                String pkg = (String) iter.next();
                                int count = (Integer) bundle.get(pkg);
                                CloneAppKey key = new CloneAppKey(0, pkg);
                                mAppNotiCounts.put(key, count);
                                refreshIconView(key);
                            }
                        }
                    });
                } else {
                    int count = intent.getExtras().getInt("count");
                    String pkgName = intent.getExtras().getString("pkg");
                    int uid = intent.getIntExtra(EXTRA_USERID, 0);
                    CloneAppKey key = new CloneAppKey(uid, pkgName);
                    if (mAppNotiCounts.containsKey(key)) {
                        mAppNotiCounts.remove(key);
                    }
                    mAppNotiCounts.put(key, count);
                    refreshIconView(key);
                }
            }
        }
    };
    public View getNotificationWidgetView(Context context, ComponentName name, int userId, Bitmap bitmap, CharSequence appName) {
        View view = mNotificationHostView;
        Intent intent = new Intent("com.yunos.systemui.widget.UPDATE");
        intent.putExtra("opt", "open");
        intent.putExtra("pkgName", name.getPackageName());
        intent.putExtra("icon", bitmap);
        intent.putExtra("appName", appName);
        intent.putExtra("widgetId", mNotificationWidgetId);
        // Show notification or not has nothing to do with notification mark type.
        // It should be shown all notification.
        intent.putExtra("showType", HomeShellSetting.ALL_NOTIFICATION);
        intent.putExtra(EXTRA_USERID, userId);
        context.sendBroadcast(intent);
        return view;
    }

    synchronized public void setWidgetInfoList(ArrayList<Object> array) {
        mWidgetInfos.clear();
        mWidgetInfos.addAll(array);
    }
    public void setNotificationWidgetView(AppWidgetHostView view, int widgetId) {
        mNotificationHostView = view;
        mNotificationWidgetId = widgetId;
    }
    synchronized public ArrayList<Object> getWidgetInfos() {
        ArrayList<Object> array = new ArrayList<Object>(mWidgetInfos.size());
        array.addAll(mWidgetInfos);
        return array;
    }

    public int getNotificationCount(String pkgName) {
        return getNotificationCount(pkgName, 0);
    }

    public int getNotificationCount(String pkgName, int userId) {
        CloneAppKey key  = new CloneAppKey(userId, pkgName);
        if (mAppNotiCounts.containsKey(key)) {
            return mAppNotiCounts.get(key);
        }
        return 0;
    }

    public boolean containsKey(ItemInfo info) {
        if (info instanceof ShortcutInfo) {
            ShortcutInfo shortcut = (ShortcutInfo) info;
            return mAppNotiCounts.containsKey(new CloneAppKey(shortcut.userId, shortcut.getPackageName()));
        } else {
            return false;
        }
    }

}
