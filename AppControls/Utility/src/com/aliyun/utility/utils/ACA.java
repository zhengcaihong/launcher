package com.aliyun.utility.utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

import com.aliyun.ams.tyid.TYIDManagerCallback;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;


/**
 * AliConstant&MethodAdaper
 *
 * @author Sheng
 *
 */
public class ACA {
    private static ConcurrentHashMap<String, Class<?>> clzMap = new ConcurrentHashMap<String, Class<?>>();
    private static String TAG = "ACA";

    public static int R_styleable_View_fadingEdgeLength = (Integer) getConstant("android.R$styleable", "View_fadingEdgeLength");
    public static int[] R_styleable_HorizontalScrollView = (int[]) getConstant("android.R$styleable", "HorizontalScrollView");
    public static int R_styleable_HorizontalScrollView_fillViewport = (Integer) getConstant("android.R$styleable", "HorizontalScrollView_fillViewport");

    public static final class MethodAdapter {
        public static File getSharedPrefsFile(Context context, String fileName) {
            return (File) executeMethod(context, "android.content.Context", "getSharedPrefsFile", fileName);
        }

        public static void putIBinder(Bundle receiver, String binderName, IBinder binder) {
            executeMethod(receiver, "android.os.Bundle", "putIBinder", binderName, binder);
        }

        public static String getExternalStorage2State() {
            return (String) executeMethod(null, "android.os.Environment", "getExternalStorage2State");
        }

        public static void setClassNameInLauncherApp(android.app.Notification receiver, String value) {
            setVariable(receiver, "android.app.Notification", "classNameInLauncherApp", value);
        }

        public static int getInstallLocation(Object iPackageManager) {
            return (Integer) executeMethod(iPackageManager, "android.content.pm.IPackageManager", "getInstallLocation");
        }

        public static int PackageHelper_APP_INSTALL_EXTERNAL = (Integer) getConstant("com.android.internal.content.PackageHelper", "APP_INSTALL_EXTERNAL");

    }

    public static final class ActivityManager {
        public static final String className = "android.app.ActivityManager";

        public static final int RECENT_INCLUDE_PROFILES = (Integer) getIntConstant(className, "RECENT_INCLUDE_PROFILES", 0x0004);

        public static final class MemoryInfo {
            public static final String className = "android.app.ActivityManager$MemoryInfo";

            public static int secondaryServerThreshold(android.app.ActivityManager.MemoryInfo receiver) {
                return (Integer) getVariable(receiver, className, "secondaryServerThreshold");
            }
        }

        public static final class RunningAppProcessInfo {
            public static final String className = "android.app.ActivityManager$RunningAppProcessInfo";
            public static int FLAG_PERSISTENT = (Integer) getConstant(className, "FLAG_PERSISTENT");
            public static int IMPORTANCE_FOREGROUND = (Integer) getConstant(className, "IMPORTANCE_FOREGROUND");
            public static int IMPORTANCE_CANT_SAVE_STATE = (Integer) getConstant(className, "IMPORTANCE_CANT_SAVE_STATE");
            public static int FLAG_CANT_SAVE_STATE = (Integer) getConstant(className, "FLAG_CANT_SAVE_STATE");

            public static int flags(android.app.ActivityManager.RunningAppProcessInfo receiver) {
                return (Integer) getVariable(receiver, className, "flags");
            }
        }

        public static void forceStopPackage(android.app.ActivityManager receiver, String pkgName) {
            executeMethod(receiver, className, "forceStopPackage", pkgName);
        }

        public static boolean clearApplicationUserData(android.app.ActivityManager receiver, String packageName, IPackageDataObserver clearUserDataObserver) {
            return (Boolean) executeMethod(receiver, className, "clearApplicationUserData", packageName, clearUserDataObserver);
        }
    }

    public static final class Intent {
        public static final String className = "android.content.Intent";
        public static String ACTION_QUERY_PACKAGE_RESTART = (String) getConstant(className, "ACTION_QUERY_PACKAGE_RESTART");
        public static String EXTRA_PACKAGES = (String) getConstant(className, "EXTRA_PACKAGES");
    }

    public static final class PackageManager {
        public static final String className = "android.content.pm.PackageManager";
        public static int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = (Integer) getConstant(className, "INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES");
        public static int MOVE_SUCCEEDED = (Integer) getConstant(className, "MOVE_SUCCEEDED");
        public static int DELETE_SUCCEEDED = (Integer) getConstant(className, "DELETE_SUCCEEDED");
        public static int INSTALL_FAILED_INSUFFICIENT_STORAGE = (Integer) getConstant(className, "INSTALL_FAILED_INSUFFICIENT_STORAGE");
        public static int INSTALL_SUCCEEDED = (Integer) getConstant(className, "INSTALL_SUCCEEDED");
        public static int INSTALL_REPLACE_EXISTING = (Integer) getConstant(className, "INSTALL_REPLACE_EXISTING");
        public static int MOVE_EXTERNAL_MEDIA = (Integer) getConstant(className, "MOVE_EXTERNAL_MEDIA");
        public static int MOVE_INTERNAL = (Integer) getConstant(className, "MOVE_INTERNAL");
        public static int MOVE_FAILED_INSUFFICIENT_STORAGE = (Integer) getConstant(className, "MOVE_FAILED_INSUFFICIENT_STORAGE");
        public static int MOVE_FAILED_DOESNT_EXIST = (Integer) getConstant(className, "MOVE_FAILED_DOESNT_EXIST");
        public static int MOVE_FAILED_FORWARD_LOCKED = (Integer) getConstant(className, "MOVE_FAILED_FORWARD_LOCKED");
        public static int MOVE_FAILED_INVALID_LOCATION = (Integer) getConstant(className, "MOVE_FAILED_INVALID_LOCATION");
        public static int MOVE_FAILED_SYSTEM_PACKAGE = (Integer) getConstant(className, "MOVE_FAILED_SYSTEM_PACKAGE");
        public static int MOVE_FAILED_INTERNAL_ERROR = (Integer) getConstant(className, "MOVE_FAILED_INTERNAL_ERROR");

        public static void getPackageSizeInfo(android.content.pm.PackageManager receiver, String packageName, IPackageStatsObserver sizeObserver) {
            executeMethod(receiver, className, "getPackageSizeInfo", new Object[] { packageName, sizeObserver });
        }

        public static void installPackage(android.content.pm.PackageManager receiver, Uri uri, IPackageInstallObserver obs, int installFlag, String packageName) {
            executeMethod(receiver, className, "installPackage", uri, obs, installFlag, packageName);
        }

        public static void deletePackage(android.content.pm.PackageManager receiver, String packageName, IPackageDeleteObserver obs, int flags) {
            executeMethod(receiver, className, "deletePackage", packageName, obs, flags);
        }

        public static void movePackage(android.content.pm.PackageManager receiver, String packageName, IPackageMoveObserver obs, int flags) {
            executeMethod(receiver, className, "movePackage", packageName, obs, flags);
        }

        public static void deleteApplicationCacheFiles(android.content.pm.PackageManager receiver, String packageName, IPackageDataObserver clearUserDataObserver) {
            executeMethod(receiver, className, "deleteApplicationCacheFiles", packageName, clearUserDataObserver);
        }
    }

    public static final class PackageParser {
        public static final String className = "android.content.pm.PackageParser";

        public static Object getInstance(String apkPath) {
            Object returnObject = null;
            try {
                Class<?> classObject = Class.forName(className);
                returnObject = classObject.getConstructor(String.class).newInstance(apkPath);
            } catch (Exception e) {
                Log.v("error", e.toString());
            }
            return returnObject;
        }

        public static Object getInstance() {
            Object returnObject = null;
            try {
                Class<?> classObject = Class.forName(className);
                returnObject = classObject.getConstructor().newInstance();
            } catch (Exception e) {
                Log.v("error", e.toString());
            }
            return returnObject;
        }

        public static Object parsePackage(Object receiver, File apkFile, String apkPath, DisplayMetrics metrics, int flags) {
            return executeMethod(receiver, className, "parsePackage", apkFile, apkPath, metrics,flags);
        }

        public static Object parsePackage(Object receiver, File apkFile, int flags) {
            return executeMethod(receiver, className, "parsePackage", apkFile, flags);
        }

        public static final class Package {
            public static final String className = "android.content.pm.PackageParser$Package";

            public static android.content.pm.ApplicationInfo applicationInfo(Object receiver) {
                return (android.content.pm.ApplicationInfo) getVariable(receiver, className, "applicationInfo");
            }
        }
    }

    public static final class ActivityInfo {
        public static final String className = "android.content.pm.ActivityInfo";
        public static int CONFIG_THEME_RESOURCE = (Integer) getConstant(className, "CONFIG_THEME_RESOURCE");
    }

    public static final class ApplicationInfo {
        public static final String className = "android.content.pm.ApplicationInfo";
        public static int CONFIG_THEME_RESOURCE = (Integer) getConstant(className, "CONFIG_THEME_RESOURCE");
        public static int FLAG_FORWARD_LOCK = (Integer) getConstant(className, "FLAG_FORWARD_LOCK");

        public static int enabledSetting(android.content.pm.ApplicationInfo receiver) {
            return (Integer) getVariable(receiver, className, "enabledSetting");
        }
    }

//    public static final class PackageInfo {
//        public static final String className = "android.content.pm.PackageInfo";
////        public static int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = (Integer) getConstant(className, "INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES");
//        public static int INSTALL_LOCATION_UNSPECIFIED = (Integer) getConstant(className, "INSTALL_LOCATION_UNSPECIFIED");
//        public static int INSTALL_LOCATION_PREFER_EXTERNAL = (Integer) getConstant(className, "INSTALL_LOCATION_PREFER_EXTERNAL");
//        public static int INSTALL_LOCATION_AUTO = (Integer) getConstant(className, "INSTALL_LOCATION_AUTO");
//
//        public static int installLocation(android.content.pm.PackageInfo receiver) {
//            return (Integer) getVariable(receiver, className, "installLocation");
//        }
//    }



    public static final class SystemProperties {
        public static final String className = "android.os.SystemProperties";

        public static String get(String key) {
            return (String) executeMethod(null, className, "get", key);
        }

        public static String get(String key, String def) {
            return (String) executeMethod(null, className, "get", key, def);
        }

        public static boolean getBoolean(String key, boolean def) {
            return (Boolean) executeMethod(null, className, "getBoolean", key, def);
        }

        public static int getInt(String key, int def) {
            return (Integer) executeMethod(null, className, "getInt", key, def);
        }

        public static void set(String key, String val) {
            executeMethod(null, className, "set", key, val);
        }

    }

    public static final class SurfaceControl {
        public static final String className = "android.view.SurfaceControl";

        public static Bitmap screenshot(int width, int height) {
            return (Bitmap) executeMethod(null, className, "screenshot", width, height);
        }
    }

    public static final class TelephonyIntents {
        public static final String className = "com.android.internal.telephony.TelephonyIntents";

        public static String ACTION_SIM_STATE_CHANGED = (String) getConstant(className, "ACTION_SIM_STATE_CHANGED");
        public static String ACTION_ANY_DATA_CONNECTION_STATE_CHANGED = (String) getConstant(className, "ACTION_ANY_DATA_CONNECTION_STATE_CHANGED");
    }

    public static final class Telephony {
        public static final class Intents {
            public static final String className = "android.provider.Telephony.Intents";

            public static String SPN_STRINGS_UPDATED_ACTION = (String) getConstant(className, "SPN_STRINGS_UPDATED_ACTION");
        }
    }

    public static final class SizeAdaptiveLayout {
        public static final String className = "com.android.internal.widget.SizeAdaptiveLayout";

        public static final class LayoutParams {
            public static final String className = "com.android.internal.widget.SizeAdaptiveLayout$LayoutParams";

            public static Object getInstance(ViewGroup.LayoutParams layoutParams) {
                Object returnObject = null;
                try {
                    Class<?> classLayoutParams = Class.forName(className);
                    returnObject = classLayoutParams.getConstructor(ViewGroup.LayoutParams.class).newInstance(layoutParams);
                } catch (Exception e) {
                    Log.v("error", e.toString());
                }
                return returnObject;
            }

            public static void setMinHeight(Object receiver, int value) {
                setVariable(receiver, className, "minHeight", value);
            }

            public static void setMaxHeight(Object receiver, int value) {
                setVariable(receiver, className, "maxHeight", value);
            }

        }
    }

    public static final class ServiceManager {
        public static final String className = "android.os.ServiceManager";

        public static Object getService(String str) {
            return (Object)executeMethod(null, className, "getService", str);
        }
    }

    public static final class AssetManager {
        public static final String className = "android.content.res.AssetManager";

        public static int addAssetPath(android.content.res.AssetManager receiver,String path) {
            return (Integer)executeMethod(receiver, className, "addAssetPath", path);
        }
    }

    public static final class ConnectivityManager {
        public static final String className = "android.net.ConnectivityManager";

        public static void setMobileDataEnabled(android.net.ConnectivityManager receiver, boolean enabled) {
            executeMethod(receiver, className, "setMobileDataEnabled", enabled);
        }

        public static boolean getMobileDataEnabled(android.net.ConnectivityManager receiver) {
            return (Boolean)executeMethod(receiver, className, "getMobileDataEnabled");
        }
    }

    public static final class WifiManager {
        public static final String className = "android.net.wifi.WifiManager";

        public static boolean setWifiApEnabled(android.net.wifi.WifiManager receiver, WifiConfiguration wifiConfig, boolean enabled) {
            return (Boolean)executeMethod(receiver, className, "setWifiApEnabled", wifiConfig, enabled);
        }

        public static WifiConfiguration getWifiApConfiguration(android.net.wifi.WifiManager receiver) {
            return (WifiConfiguration)executeMethod(receiver, className, "getWifiApConfiguration");
        }
    }

    public static final class AppWidgetManager {
        public static final String className = "android.appwidget.AppWidgetManager";

        public static void bindAppWidgetId(android.appwidget.AppWidgetManager receiver, int appWidgetId, ComponentName provider) {
            executeMethod(receiver, className, "bindAppWidgetId", appWidgetId, provider);
        }
    }

    public static final class View {
        public static final String className = "android.view.View";

        public static void setTransitionAlpha(android.view.View receiver, float alpha) {
            executeMethod(receiver, className, "setTransitionAlpha", alpha);
        }
        public static Object getViewRootImpl(android.view.View receiver) {
            return (Object) executeMethod(receiver, className, "getViewRootImpl");
        }
        public static void setmScrollX(Object receiver, int mOverScrollX) {
            setVariable(receiver, className, "mScrollX", mOverScrollX);
        }
    }

    public static final class ViewRootImpl {
        public static final String className = "android.view.ViewRootImpl";

        public static void cancelInvalidate(Object receiver, android.view.View view) {
            executeMethod(receiver, className, "cancelInvalidate", view);
        }
    }

    public static final class Notification {
        public static final String className = "android.app.Notification";

        public static int DEFAULT_SHOW = (Integer) getConstant(className, "DEFAULT_SHOW");

        public static int showType(android.app.Notification receiver) {
            return (Integer) getVariable(receiver, className, "showType");
        }
    }

    /**
     * execute the method
     *
     * @param ownerObj
     *            a receiver of invoking,to be null if it's a static method
     * @param className
     * @param methodName
     * @param params
     * @return
     */
    private static Object executeMethod(Object ownerObj, String className, String methodName, Object... params) {
        if (null == ownerObj) {
            Log.e(TAG, className + "." + methodName + "() ownerObj is null");
        }

        Object resObj = null;

        Log.d(TAG, className + "." + methodName + "() is run");

        try {
            Class<?> clz = clz(className);
            Class<?>[] paramsType = new Class[params.length];

            for (int i = 0; i < params.length; i++) {
                if (methodName.equals("putIBinder")) {
                    //Begin BugID:38292: fix broken pay interface of V4.0.
                    if (i == 1) {
                        paramsType[i] = IBinder.class;
                        continue;
                    }
                    //End BugID:38292
                }
                if (methodName.equals("yunosGetToken")) {
                    if(i == 1 ) {
                        paramsType[i] = TYIDManagerCallback.class;
                        continue;
                    }
                    if (i == 2 && params[i] == null) {
                        paramsType[i] = Handler.class;
                        continue;
                    }
                }
                if (methodName.equals("getPackageSizeInfo")) {
                    if(i == 1 ) {
                        paramsType[i] = IPackageStatsObserver.class;
                        continue;
                    }
                }
                if (methodName.equals("deletePackage")) {
                    if(i == 1 ) {
                        paramsType[i] = IPackageDeleteObserver.class;
                        continue;
                    }
                }
                if (methodName.equals("cancelInvalidate")) {
                    if(i == 0 ) {
                        paramsType[i] = android.view.View.class;
                        continue;
                    }
                }
                paramsType[i] = params[i].getClass();
                android.util.Log.d("Reflection ",  "methodName= " + methodName);
                android.util.Log.d("Reflection",  "paramsType["+ i +"]= " + paramsType[i].getSimpleName());
                if (paramsType[i].getSimpleName().contains("Integer")) {
                    paramsType[i] = int.class;
                }
                if (paramsType[i].getSimpleName().contains("Float")) {
                    paramsType[i] = float.class;
                }
                if (paramsType[i].getSimpleName().contains("Uri")) {
                    paramsType[i] = Uri.class;
                }
                if (paramsType[i].getSimpleName().contains("PackageInstallObserver")) {
                    paramsType[i] = IPackageInstallObserver.class;
                }
                if (paramsType[i].getSimpleName().contains("IBinder")) {
                    paramsType[i] = IBinder.class;
                }
                if (paramsType[i].getSimpleName().contains("IPackageDataObserver")) {
                    paramsType[i] = IPackageDataObserver.class;
                }
                if (paramsType[i].getSimpleName().contains("PackageDeleteObserver")) {
                    paramsType[i] = IPackageDeleteObserver.class;
                }
                if (paramsType[i].getSimpleName().contains("IPackageStatsObserver")) {
                    paramsType[i] = IPackageStatsObserver.class;
                }
                if (paramsType[i].getSimpleName().contains("IPackageMoveObserver")) {
                    paramsType[i] = IPackageMoveObserver.class;
                }
                if (paramsType[i].getSimpleName().contains("View")) {
                    paramsType[i] = View.class;
                }
                if (params[i] instanceof android.view.Window) {
                    paramsType[i] = android.view.Window.class;
                }
                if (params[i] instanceof Handler) {
                    paramsType[i] = Handler.class;
                }
                if (params[i] instanceof Context) {
                    paramsType[i] = Context.class;
                }
                if (params[i] instanceof Activity) {
                    paramsType[i] = Activity.class;
                }

                if (params[i] instanceof Boolean) {
                    paramsType[i] = boolean.class;
                }
            }

            Method method = clz.getMethod(methodName, paramsType);
            resObj = method.invoke(ownerObj, params);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return resObj;
    }

    /**
     * To get a variable by class name
     *
     * @param receiver
     * @param clzName
     * @param propName
     * @return
     */
    private static Object getVariable(Object receiver, String clzName, String propName) {
        Object res = null;
        try {
            Class<?> ownerClass = clz(clzName);
            Field field = ownerClass.getField(propName);
            res = field.get(receiver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static void setVariable(Object receiver, String clzName, String propName, Object value) {
        try {
            Class<?> ownerClass = clz(clzName);
            Field field = ownerClass.getDeclaredField(propName);
            field.setAccessible(true);
            field.set(receiver, value);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * To get a constant variable by class name
     *
     * @param className
     * @param constName
     * @return
     */
    private static Object getConstant(String className, String constName) {
        Object res = null;
        try {
            Class<?> ownerClass = clz(className);
            Field field = ownerClass.getField(constName);
            res = field.get(ownerClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static int getIntConstant(String className, String constName, int defValue) {
        Object obj = getConstant(className, constName);
        if (obj instanceof Integer) {
            return ((Integer) obj).intValue();
        } else {
            return defValue;
        }
    }

    /**
     * To get a class with cache for performance
     *
     * @param className
     *            name of class that you will get
     * @return the Class object you've gotten
     * @throws ClassNotFoundException
     */
    private static Class<?> clz(String className) throws ClassNotFoundException {
        Class<?> ownerClass = clzMap.get(className);
        if (ownerClass == null) {
            ownerClass = Class.forName(className);
            clzMap.put(className, ownerClass);
        }
        return ownerClass;
    }

    public static void printMethods(Class cl) {
        Method[] methods = cl.getDeclaredMethods();

        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            Class retType = m.getReturnType();
            Class[] paramTypes = m.getParameterTypes();
            String name = m.getName();
            Log.d(TAG, Modifier.toString(m.getModifiers()));
            Log.d(TAG, " " + retType.getName() + " " + name + "(");
            for (int j = 0; j < paramTypes.length; j++) {
                if (j > 0)
                    Log.d(TAG, ", ");
                Log.d(TAG, paramTypes[j].getName());
            }
            Log.d(TAG, ");");
        }
    }

    public static final class SystemBarColorManager {
        public static final String className = "com.aliyun.ams.systembar.SystemBarColorManager";

        public static Object newInstance(Activity activity) {
            return newInstance(activity.getWindow());
        }
        public static Object newInstance(android.view.Window window) {
            Object obj = null;
            try {
                Class[] paramTypes = { android.view.Window.class };
                Object[] params = {window};
                Class cls = clz(className);
                Constructor con = cls.getConstructor(paramTypes);
                obj = con.newInstance(params);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            return obj;
        }
        public static void setStatusBarColorResource(Object manager, int res) {
            executeMethod(manager, className, "setStatusBarColorResource", res);
        }
        public static void setStatusBarColor(Object manager, int color) {
            executeMethod(manager, className, "setStatusBarColor", color);
        }
        public static void setViewFitsSystemWindows(Object manager, Activity activity, Boolean showActionBar) {
            executeMethod(manager, className, "setViewFitsSystemWindows", activity, showActionBar);
        }
        public static void setViewFitsSystemWindows(Object manager, Activity activity, int rootLayoutId, Boolean showActionBar) {
            executeMethod(manager, className, "setViewFitsSystemWindows", activity, rootLayoutId, showActionBar);
        }
        public static void setTranslucentStatus(Object manager, Activity activity) {
            executeMethod(manager, className, "setTranslucentStatus", activity);
        }

        public static void setStatusBarDarkMode(Object manager, Activity activity) {
            executeMethod(manager, className, "setStatusBarDarkMode", activity);
        }
        public static void setStatusBarDarkMode(Object manager, Activity activity, int viewId) {
            executeMethod(manager, className, "setStatusBarDarkMode", activity, viewId);
        }
        public static void setStatusBarDarkMode(Object manager, android.view.Window window, Boolean darkmode) {
            executeMethod(manager, className, "setStatusBarDarkMode", window, darkmode);
        }
    }
    public static final class Window {
        public static final String className = "android.view.Window";
        public static final String paramClassName = "android.view.WindowManager$LayoutParams";
        public static int FLAG_TRANSLUCENT_STATUS =  getConstant(paramClassName, "FLAG_TRANSLUCENT_STATUS") == null ? 0 : (Integer)getConstant(paramClassName, "FLAG_TRANSLUCENT_STATUS");

        public static void addFlags(android.view.Window window, Integer flag) {
            executeMethod(window, className, "addFlags", flag);
        }

        public static void clearFlags(android.view.Window window, int flag) {
            executeMethod(window, className, "clearFlags", flag);
        }

    }
}
