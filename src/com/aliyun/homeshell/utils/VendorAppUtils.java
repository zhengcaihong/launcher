package com.aliyun.homeshell.utils;

import java.lang.reflect.Method;

import android.content.pm.ApplicationInfo;

public final class VendorAppUtils {

    public final static boolean isUpdatedVendorAppNoDelete(ApplicationInfo info) {
        if (info == null) return false;
        return isVendorAppNoDelete(info) && isUpdatedVendorApp(info);
    }

    public final static boolean isVendorAppNoDelete(ApplicationInfo info) {
        if (info == null) return false;
        try {
            Method isVendorAppNoDelete = ApplicationInfo.class.getDeclaredMethod("isVendorAppNoDelete");
            if (isVendorAppNoDelete == null) return false;
            return (Boolean)isVendorAppNoDelete.invoke(info);
        } catch (Throwable t) {
            // do nothing
        }
        return false;
    }

    public final static boolean isUpdatedVendorApp(ApplicationInfo info) {
        if (info == null) return false;
        try {
            Method isUpdatedVendorApp = ApplicationInfo.class.getDeclaredMethod("isUpdatedVendorApp");
            if (isUpdatedVendorApp == null) return false;
            return (Boolean)isUpdatedVendorApp.invoke(info);
        } catch (Throwable t) {
            // do nothing
        }
        return false;
    }

}
