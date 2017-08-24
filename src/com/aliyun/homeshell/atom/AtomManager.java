package com.aliyun.homeshell.atom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.widgetpage.WidgetPageManager.WidgetPageInfo;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public final class AtomManager {

    private static final String TAG = "AtomManager";
    private static AtomManager mInsatnce = new AtomManager();
    private static final String ATOM_PACKAGE_NAME = "com.yunos.assistant";
    private static final String ATOM_LAYOUT_RESOURCE = "atom_main_view";
    private View mRootView;
    private Method mPauseMethod;
    private Method mResumeMethod;

    private AtomManager() {}

    public static AtomManager getAtomManager() {
        return mInsatnce;
    }

    public View getRootView(Context context) {
        if (mRootView == null) {
            mRootView = createView(newAtomContext(context, ATOM_PACKAGE_NAME),
                    ATOM_PACKAGE_NAME, ATOM_LAYOUT_RESOURCE);
        }
        return mRootView;
    }

    private static Context newAtomContext(Context context, String packageName) {

        int contextPermission = Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY;

        Context theirContext = null;
        try {
            theirContext = context.createPackageContext(packageName, contextPermission);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return theirContext;
    }
    
    private static View createView(Context remoteContext, String packagename, String resource) {
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
            return null;
        }
        View v = null;

        try {
            v = theirInflater.inflate(id, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (v != null) {
            ItemInfo info = new ItemInfo();
            v.setTag(info);
        }
        return v;
    }

    public void pauseAtomIcon(boolean useStaticImage) {
        if (mPauseMethod == null) {
            try {
                mPauseMethod = mRootView.getClass().getDeclaredMethod("onPause", Boolean.TYPE);
            } catch (Exception e) {
                Log.e(TAG, "can not found onPause Method!!!");
            }
        }
        if (mPauseMethod != null) {
            try {
                mPauseMethod.invoke(mRootView, useStaticImage);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void resumeAtomIcon() {
        if (mResumeMethod == null) {
            try {
                mResumeMethod = mRootView.getClass().getDeclaredMethod("onResume");
            } catch (Exception e) {
                Log.e(TAG, "can not found onResume Method!!!");
            }
        }
        if (mResumeMethod != null) {
            try {
                mResumeMethod.invoke(mRootView);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
