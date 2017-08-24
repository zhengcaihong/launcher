package com.aliyun.homeshell.themeutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.aliyun.homeshell.R;

import app.aliyun.v3.gadget.GadgetHelper;
import app.aliyun.v3.gadget.GadgetInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

public class ThemeUtils {

    public static final int ICON_TYPE_APP = 1;
    public static final int ICON_TYPE_CLOUDAPP = 2;
    public static final int ICON_TYPE_BROSWER_SHORTCUT = 3;
    public static final int ICON_TYPE_FOLDER = 4;
    public static final int ICON_TYPE_APP_TEMPORARY = 5;

    private static final String TAG = "ThemeUtils";

    private static Object mMutiThemeManagerObject;
    private static PackageManager mPackageManager = null;
    private static boolean mInited;

    private static GadgetNameParser mGadgetNameParser;

    private static final String DEFAULT_GADGET_PATH = "/system/auitheme/default/gadgets/";
    /* YUNOS BEGIN */
    // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
    // private static String mLocale;
    /* YUNOS END */

    private static HashMap<String, Bitmap> sCacheIconMap = null;

    public static void init(Context context) {
        if (mInited) {
            return;
        }
        mInited = true;
        try {
            @SuppressWarnings("rawtypes")
            Class mutiThemeManagerClass = ClassProxyUtil
                    .loadClass("com.aliyun.theme.MultiThemeManager");
            mMutiThemeManagerObject = ClassProxyUtil.invokeMethod(
                    mutiThemeManagerClass, "getInstance");
        } catch (Exception e) {
            mMutiThemeManagerObject = null;
            mInited = false;
        }
        mPackageManager = context.getPackageManager();
    }

    public static Bitmap getAppIcon(Context context, Intent intent) {
        if (!mInited) {
            init(context);
        }
        return getUnifiedIconBitmap(context, intent);
    }

    public static int getIconSize(Context context) {
        if (!mInited) {
            init(context);
        }
        if (mMutiThemeManagerObject != null) {
            try {
                Integer rst = (Integer) ClassProxyUtil.invokeMethod(
                        mMutiThemeManagerObject, "getIconSize");
                return rst != null ? rst.intValue() : -1;
            } catch (Exception e) {
            }
        }
        return -1;
    }

    public static boolean needCustom(Context context, String packageName, String className) {
        // TODO Auto-generated method stub
        if (!mInited) {
            init(context);
        }
        if (mMutiThemeManagerObject != null) {
            try {
                return (Boolean) ClassProxyUtil.invokeMethod(
                        mMutiThemeManagerObject, "isNeedCustom", String.class,
                        String.class, packageName, className);
            } catch (Exception e) {
                Log.e(TAG, "needCustom Exception", e);
            }
        }
        return false;
    }

    public static Bitmap buildUnifiedIcon(Context context, Bitmap source,
            int style) {
        // TODO Auto-generated method stub
        if (!mInited) {
            init(context);
        }

        if (null == source || ThemeUtils.ICON_TYPE_APP == style) {
            return source;
        }
        if (null != mMutiThemeManagerObject) {
            try {
                return (Bitmap) ClassProxyUtil.invokeMethod(
                        mMutiThemeManagerObject, "buildUnifiedIcon",
                        Bitmap.class, int.class, source, style);
            } catch (Exception e) {
            }
        }

        return source;
    }

    public static Bitmap getGadgetPreview(Context context, GadgetInfo info,
            int width, int height) {
        String path = info.path.substring(0, info.path.indexOf(info.label))
                + "preview_" + info.label + ".png";
        Bitmap bp = getGadgetPreview(context, path, width, height);
        if (bp == null) {
            Log.d(TAG, "get theme preview fail!!!");
            String defaultPath = DEFAULT_GADGET_PATH + "preview_" + info.label
                    + ".png";
            return getGadgetPreview(context, defaultPath, width, height);
        }
        return bp;
    }

    public static String getGadgetName(Context context, GadgetInfo info) {
        String locale = context.getResources().getConfiguration().locale
                .toString();
        /* YUNOS BEGIN */
        // ##date:2014/4/28 ##author:hongchao.ghc ##BugID:111144
        String gadgetXmlFilePath = info.path.substring(0, info.path.indexOf(info.label))
                + "gadgets-" + locale + ".xml";
        initParser(gadgetXmlFilePath);
        /* YUNOS END */
        Map<String, String> map = mGadgetNameParser.getGadgetName();
        if (map == null) {
            gadgetXmlFilePath = info.path.substring(0, info.path.indexOf(info.label))
                    + "gadgets.xml";
            initParser(gadgetXmlFilePath);
            map = mGadgetNameParser.getGadgetName();
        }

        if (map == null) {
            initParser(DEFAULT_GADGET_PATH + "gadgets-" + locale + ".xml");
            map = mGadgetNameParser.getGadgetName();
        }

        if (map == null) {
            initParser(DEFAULT_GADGET_PATH + "gadgets.xml");
            map = mGadgetNameParser.getGadgetName();
        }
        String label =  map == null ? null : map.get(info.label);
        // show gadget's category name when label is empty
        return label == null ? info.title : label;
    }

    /* YUNOS BEGIN*/
    //##BugID:(8490832) date:20160707 author:hongwei.zhw@alibaba-inc-com
    //##description: hide the weather for cmcc
    public static Map<String, GadgetInfo> listGadgets(Context context) {
        Map<String, GadgetInfo> map = GadgetHelper.listGadgetSet(false);
        return map;
    }
    /* YUNOS END*/

    private static Bitmap getGadgetPreview(Context context, String path,
            int width, int height) {
        FileInputStream is = null;
        try {
            File file = new File(path);
            is = new FileInputStream(file);
            BitmapFactory.Options options = new BitmapFactory.Options();

            Bitmap bp = Bitmap.createBitmap(width, height, Config.ARGB_8888);
            Canvas c = new Canvas();
            c.setBitmap(bp);
            c.save();
            Bitmap src = BitmapFactory.decodeStream(is, null, options);
            int srcWidth = src.getWidth();
            int srcHeight = src.getHeight();

            if (srcWidth == 0 || srcHeight == 0) {
                return bp;
            }

            int dstWidth = width, dstHeight = height;
            if ((float) srcWidth / width >= (float) srcHeight / height) {
                dstHeight = srcHeight * width / srcWidth;
            } else {
                dstWidth = srcWidth * height / srcHeight;
            }
            return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally{
            if (is != null)
            {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static void initParser(String path) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            mGadgetNameParser = new GadgetNameParser();
            File gadgetXmlFile = new File(path);
            FileInputStream xmlStream = new FileInputStream(gadgetXmlFile);
            parser.parse(xmlStream, mGadgetNameParser);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/5/28 ##author:jun.dongj ##BugID:124436
    // read all icons from theme
    public static void destroyIconCache(){
        if(sCacheIconMap != null){
            //added by xiaodong.lxd #BugID:5659311
            Set<String> keys = sCacheIconMap.keySet();
            Bitmap bitmap = null;
            for(String key : keys) {
                bitmap = sCacheIconMap.get(key);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            //end 5659311
            sCacheIconMap.clear();
            sCacheIconMap = null;
        }
    }

    private static String generateCacheKey(String pkgName,String className, int resid){
       try{
           return com.aliyun.theme.jar.icon.IconCacheReader.getIconKey(pkgName,className,resid);
       }catch(Exception e){
           return null;
       }
    }

    private static Bitmap getIconBitmap(String packageName, String className,
            int resid) {
        if (mMutiThemeManagerObject != null) {
            if(sCacheIconMap == null){
                try{
                    sCacheIconMap = com.aliyun.theme.jar.icon.IconCacheReader.getBitmapFromCacheFile();
                }catch(Exception e){
                    sCacheIconMap = null;
                }
            }
            Bitmap bmp = null;
            if(sCacheIconMap!=null){
                String key = generateCacheKey(packageName,className,resid);
                if(key != null){
                    bmp = sCacheIconMap.remove(key);
                }
                Log.d(TAG,"Icon cache size : "+sCacheIconMap.size());
            }
            if (bmp == null) {
                try {
                   bmp =  (Bitmap) ClassProxyUtil.invokeMethod(
                            mMutiThemeManagerObject, "getIconBitmap", String.class,
                            String.class, int.class, packageName, className, resid);
                } catch (Exception e) {
                }
            }
            return bmp;
        }
        return null;
    }
    /* YUNOS END */

    private static Bitmap getUnifiedIconBitmap(Context context, Intent intent) {
        ResolveInfo resolveInfo = getActivityFirstResolveInfo(intent);
        if (resolveInfo != null && mPackageManager != null) {
            ComponentInfo ci = (resolveInfo.activityInfo == null ? resolveInfo.serviceInfo
                    : resolveInfo.activityInfo);
            String pkg = resolveInfo.resolvePackageName;
            if (pkg == null && ci != null)
                pkg = ci.packageName;
            String clz = (ci == null ? null : ci.name);
            ApplicationInfo ai = (ci == null ? null : ci.applicationInfo);
            int resId = resolveInfo.icon;
            if (resId == 0 && ci != null) {
                resId = ci.icon;
                if (resId == 0 && ai != null)
                    resId = ai.icon;
            }
            Bitmap icon = getIconBitmap(pkg, clz, resId);
            if (icon != null) {
                Log.d(TAG, "get preload icon : " + pkg);
                return icon;
            } else {
                Log.d(TAG, "get app icon : " + pkg);
                /* YUNOS BEGIN */
                // ##date:2014/07/24 ##author:hongchao.ghc ##BugID:141159
                Drawable drawable = null;
                try {
                    drawable = resolveInfo.loadIcon(mPackageManager);
                } catch (OutOfMemoryError e) {
                    // TODO: handle exception
                }
                if (drawable != null) {
                    if(drawable instanceof BitmapDrawable || drawable instanceof NinePatchDrawable){
                        int suggetionwidth = getIconSize(context);
                        int suggetionheight = getIconSize(context);
                        if(suggetionwidth == -1){
                            suggetionwidth = context.getResources().getDimensionPixelSize(R.dimen.suggestion_icon_width);
                        }

                        if(suggetionheight == -1){
                            suggetionheight = context.getResources().getDimensionPixelSize(R.dimen.suggestion_icon_height);
                        }

                        Bitmap bitmap = Bitmap
                                .createBitmap(
                                        suggetionwidth,
                                        suggetionheight,
                                        drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                                : Bitmap.Config.RGB_565);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, suggetionwidth,
                                suggetionheight);
                        drawable.draw(canvas);
                        return bitmap;
                    }
                }
            }
        } else if (mPackageManager != null) {
            /* YUNOS BEGIN */
            // ##date:2014/9/2 ##author:zhanggong.zg ##BugID:5244146
            // Failed to get ResolvedInfo. Seems it is a frozen app.
            // Otherwise the intent argument is illegal.
            final ComponentName cname = intent.getComponent();
            if (cname == null) {
                Log.w(TAG, "frozen app icon: intent.getComponent() == null: " + intent);
                return null;
            }
            final String className = cname.getClassName();
            if (TextUtils.isEmpty(className)) {
                Log.w(TAG, "frozen app icon: ComponentName.getClassName() is empty: " + cname);
                return null;
            }
            ActivityInfo ai = null;
            PackageInfo pi = null;
            try {
                pi = mPackageManager.getPackageInfo(cname.getPackageName(), PackageManager.GET_ACTIVITIES);
            } catch (NameNotFoundException e) {
                Log.w(TAG, "frozen app icon: failed to get package info: " + cname.getPackageName());
                return null;
            } catch (java.lang.RuntimeException re) {
                Log.w(TAG, "getPackageInfo() failed:" + cname.getPackageName());
                re.printStackTrace();
                return null;
            }

            if(pi == null || pi.activities == null){
                Log.w(TAG, "get app info error : " + cname.getPackageName());
                return null;
            }
            for (ActivityInfo activityInfo : pi.activities) {
                if (className.equals(activityInfo.name)) {
                    ai = activityInfo;
                    break;
                }
            }
            if (ai != null) {
                Bitmap icon = getIconBitmap(ai.packageName, ai.name, ai.icon);
                if (icon != null) {
                    Log.d(TAG, "get preload icon (frozen app): " + ai.packageName);
                    return icon;
                } else {
                    Log.d(TAG, "get app icon (frozen app): " + ai.packageName);
                    Drawable drawable = null;
                    try {
                        drawable = ai.loadIcon(mPackageManager);
                        if (drawable == null) {
                            drawable = pi.applicationInfo.loadIcon(mPackageManager);
                        }
                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, "frozen app icon: out of memory", e);
                        return null;
                    }
                    if (drawable != null) {
                        if (drawable instanceof BitmapDrawable) {
                            return ((BitmapDrawable) drawable).getBitmap();
                        } else if (drawable instanceof NinePatchDrawable) {
                            Bitmap bitmap = Bitmap
                                    .createBitmap(
                                            drawable.getIntrinsicWidth(),
                                            drawable.getIntrinsicHeight(),
                                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                                    : Bitmap.Config.RGB_565);
                            Canvas canvas = new Canvas(bitmap);
                            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                                    drawable.getIntrinsicHeight());
                            drawable.draw(canvas);
                            return bitmap;
                        }
                    }
                }
            } else {
                Log.w(TAG, "frozen app icon: failed to find activity info: " + cname);
            }
            /* YUNOS END */
        }
        return null;
    }

    private static ResolveInfo getActivityFirstResolveInfo(Intent intent) {
        if (intent == null || mPackageManager == null) {
            return null;
        }
        List<ResolveInfo> resolveInfo = mPackageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null || resolveInfo.size() == 0) {
            return null;
        }
        return resolveInfo.get(0);
    }
}
