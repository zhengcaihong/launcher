
package com.aliyun.homeshell.widgetpage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.aliyun.homeshell.DragLayer;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.Launcher;

public class WidgetPageManager implements IAliWidgetPage {
    public static final String TAG = "WidgetPageManager";
    public static final String PAGE_LAYOUT_NAME = "page_main";
    public static final String HOTSEAT_LAYOUT_NAME = "hotseat";
    private List<WidgetPageInfo> mWigetPageList;
    private Context mContext;

    public class WidgetPageInfo {
        private String mPackageName;
        private View mRootView;
        private Context mRemoteContext;
        private View mHotseatView;
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(5735133) ##date:2015/3/3
        // ##description: add indicator icon for widget page.
        private Drawable mIndicatorIcon;
        private Drawable mIndicatorIconFocus;

        /* YUNOS END PB */

        public String getPackageName() {
            return mPackageName;
        }

        public View getRootView() {
            return mRootView;
        }

        public View getHotseatView() {
            return mHotseatView;
        }

        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(5735133) ##date:2015/3/3
        // ##description: add indicator icon for widget page.
        public Drawable getIndicatorIcon() {
            return mIndicatorIcon;
        }

        public Drawable getIndicatorIconFocus() {
            return mIndicatorIconFocus;
        }
        /* YUNOS END PB */
    }

    public WidgetPageManager(Context context) {
        mContext = context;
        Resources r = context.getResources();
        String[] packageNames = r.getStringArray(R.array.widgetpage_array);

        mWigetPageList = new ArrayList<WidgetPageInfo>();
        String packagename;
        int hotseatH = (int) context.getResources().getDimension(
                R.dimen.button_bar_height_plus_padding);
        for (int i = 0; i < packageNames.length; i++) {
            packagename = packageNames[i];
            if (packagename == null || packagename.length() == 0) {
                continue;
            }
            WidgetPageInfo info = new WidgetPageInfo();
            info.mPackageName = packagename;
            info.mRemoteContext = newWidgetContext(context, packagename);
            if (info.mRemoteContext == null) {
                continue;
            }
            info.mRootView = createView(info.mRemoteContext, packagename, PAGE_LAYOUT_NAME);
            if (info.mRootView == null) {
                continue;
            }
            info.mHotseatView = createView(info.mRemoteContext, packagename, HOTSEAT_LAYOUT_NAME);
            if (info.mHotseatView != null) {
                // ((Launcher)context).getDragLayer().addView(info.mHotseatView);
                // FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                // FrameLayout.LayoutParams.MATCH_PARENT, hotseatH);
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, hotseatH);
                lp.gravity = Gravity.BOTTOM;
                ((Launcher) context).getDragLayer().addView(info.mHotseatView, lp);
                //YUNOS BEGIN PB
                //## modules(HomeShell):
                //## date:2015/07/31 ##author:shuoxing.wsx
                //## BugID:6230696:camera widget page's hotseat overlaps with music widget page's.
                info.mHotseatView.setVisibility(View.INVISIBLE);
                //YUNOS END PB
            }
            /* YUNOS BEGIN PB */
            // ##modules(HomeShell): ##author:yongxing.lyx
            // ##BugID:(5735133) ##date:2015/3/3
            // ##description: add indicator icon for widget page.
            Drawable[] indicatorIcons = getIndicatorIcons(info);
            if (indicatorIcons != null && indicatorIcons.length >= 2) {
                info.mIndicatorIcon = indicatorIcons[ICON_INDEX_NORMAL];
                info.mIndicatorIconFocus = indicatorIcons[ICON_INDEX_FOCUS];
            }
            /* YUNOS END PB */
            mWigetPageList.add(info);
        }
        Log.e(TAG, "WidgetPageManager init size:" + mWigetPageList.size());

    }

    public int getWigetPageCount() {
        return mWigetPageList.size();
    }

    public View getWidgetPageRootView(int index) {
        WidgetPageInfo info = mWigetPageList.get(index);
        if (info != null) {
            return info.mRootView;
        }
        return null;

    }

    public View getWidgetPageRootView(String packagename) {
        WidgetPageInfo info = getWidgetPageInfo(packagename);
        if (info != null) {
            return info.mRootView;
        }
        return null;
    }

    public View getHotseatView(String packagename) {

        WidgetPageInfo info = getWidgetPageInfo(packagename);
        if (info != null) {
            return info.mHotseatView;
        }
        return null;
    }

    public WidgetPageInfo getWidgetPageInfo(String packagename) {
        for (int i = 0; i < mWigetPageList.size(); i++) {
            WidgetPageInfo info = mWigetPageList.get(i);
            if (info.mPackageName.equals(packagename)) {
                return info;
            }
        }
        return null;
    }

    public WidgetPageInfo getWidgetPageInfo(int index) {
        WidgetPageInfo info = mWigetPageList.get(index);

        return info;
    }

    public static Context newWidgetContext(Context context, String packageName) {

        int contextPermission = Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY;

        Context theirContext = null;
        try {
            theirContext = context.createPackageContext(packageName, contextPermission);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return theirContext;
    }

    public static View createView(Context remoteContext, String packagename, String resource) {
        Context theirContext = remoteContext;

        if (theirContext == null) {
            return null;
        }
        LayoutInflater theirInflater = (LayoutInflater) theirContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        theirInflater = theirInflater.cloneInContext(theirContext);
        Resources r = theirContext.getResources();

        int id = 0;

        id = r.getIdentifier(resource, "layout", packagename);

        if (id == 0) {
            Log.e(TAG, "ERROR! can't get root layout id.");
            return null;
        }
        View v = null;

        try {
            v = theirInflater.inflate(id, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##author:yongxing.lyx
        // ##BugID:(5602005) ##date:2014/11/26
        // ##description: add fake ItemInfo to tag avoid null point exception
        if (v != null) {
            ItemInfo info = new ItemInfo();
            v.setTag(info);
        }
        /* YUNOS BEGIN PB */
        return v;
    }

    @Override
    public void onPause() {

        try {

            Method m = null;
            for (int i = 0; i < mWigetPageList.size(); i++) {
                WidgetPageInfo info = mWigetPageList.get(i);
                if (info != null) {
                    m = info.mRootView.getClass().getDeclaredMethod("onPause");
                    if (m != null) {
                        m.invoke(info.mRootView);
                    }
                }

            }

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    @Override
    public void onResume() {
        try {

            Method m = null;
            for (int i = 0; i < mWigetPageList.size(); i++) {
                WidgetPageInfo info = mWigetPageList.get(i);
                if (info != null) {
                    m = info.mRootView.getClass().getDeclaredMethod("onResume");
                    if (m != null) {
                        m.invoke(info.mRootView);
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    @Override
    public void onPageBeginMoving() {
        try {

            Method m = null;
            for (int i = 0; i < mWigetPageList.size(); i++) {
                WidgetPageInfo info = mWigetPageList.get(i);
                if (info != null) {
                    m = info.mRootView.getClass().getDeclaredMethod("onPageBeginMoving");
                    if (m != null) {
                        m.invoke(info.mRootView);
                    }
                }

            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void enterWidgetPage(int page) {
        try {
            Method m = null;
            WidgetPageInfo info = ((Launcher) mContext).getWorkspace().getWidgetPageInfoAt(page);
            if (info != null) {
                m = info.mRootView.getClass().getDeclaredMethod("enterWidgetPage", Integer.TYPE);
                if (m != null) {
                    m.invoke(info.mRootView, page);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void leaveWidgetPage(int page) {
        try {
            Method m = null;
            WidgetPageInfo info = ((Launcher) mContext).getWorkspace().getWidgetPageInfoAt(page);
            if (info != null) {
                m = info.mRootView.getClass().getDeclaredMethod("leaveWidgetPage", Integer.TYPE);
                if (m != null) {
                    m.invoke(info.mRootView, page);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##author:yongxing.lyx
    // ##BugID:(5735133) ##date:2015/3/3
    // ##description: add indicator icon for widget page.
    @Override
    public int getConsumedFlingDirection(int page) {
        try {
            Method m = null;
            WidgetPageInfo info = ((Launcher) mContext).getWorkspace().getWidgetPageInfoAt(page);
            if (info != null) {
                m = info.mRootView.getClass().getDeclaredMethod("getConsumedFlingDirection",
                        Integer.TYPE);
                if (m != null) {
                    return (Integer) m.invoke(info.mRootView, page);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Drawable[] getIndicatorIcons(int page) {
        try {
            Method m = null;
            WidgetPageInfo info = ((Launcher) mContext).getWorkspace().getWidgetPageInfoAt(page);
            if (info != null) {
                m = info.mRootView.getClass().getDeclaredMethod("getIndicatorIcons", Integer.TYPE);
                if (m != null) {
                    return (Drawable[]) m.invoke(info.mRootView, page);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    public Drawable[] getIndicatorIcons(WidgetPageInfo info) {
        try {
            Method m = null;
            if (info != null) {
                m = info.mRootView.getClass().getDeclaredMethod("getIndicatorIcons", Integer.TYPE);
                if (m != null) {
                    return (Drawable[]) m.invoke(info.mRootView, 0);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }
    /* YUNOS END PB */
}
