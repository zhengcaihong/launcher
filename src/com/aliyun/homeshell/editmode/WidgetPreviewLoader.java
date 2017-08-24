package com.aliyun.homeshell.editmode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import app.aliyun.v3.gadget.GadgetInfo;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.icon.IconUtils;
import com.aliyun.homeshell.themeutils.ThemeUtils;

abstract class SoftReferenceThreadLocal<T> {
    private ThreadLocal<SoftReference<T>> mThreadLocal;
    public SoftReferenceThreadLocal() {
        mThreadLocal = new ThreadLocal<SoftReference<T>>();
    }

    abstract T initialValue();

    public void set(T t) {
        mThreadLocal.set(new SoftReference<T>(t));
    }

    public T get() {
        SoftReference<T> reference = mThreadLocal.get();
        T obj;
        if (reference == null) {
            obj = initialValue();
            mThreadLocal.set(new SoftReference<T>(obj));
            return obj;
        } else {
            obj = reference.get();
            if (obj == null) {
                obj = initialValue();
                mThreadLocal.set(new SoftReference<T>(obj));
            }
            return obj;
        }
    }
}

class CanvasCache extends SoftReferenceThreadLocal<Canvas> {
    @Override
    protected Canvas initialValue() {
        return new Canvas();
    }
}

class PaintCache extends SoftReferenceThreadLocal<Paint> {
    @Override
    protected Paint initialValue() {
        return null;
    }
}

class BitmapCache extends SoftReferenceThreadLocal<Bitmap> {
    @Override
    protected Bitmap initialValue() {
        return null;
    }
}

class RectCache extends SoftReferenceThreadLocal<Rect> {
    @Override
    protected Rect initialValue() {
        return new Rect();
    }
}

class BitmapFactoryOptionsCache extends SoftReferenceThreadLocal<BitmapFactory.Options> {
    @Override
    protected BitmapFactory.Options initialValue() {
        return new BitmapFactory.Options();
    }
}

public class WidgetPreviewLoader {
    static final String TAG = "WidgetPreviewLoader";

    private int mPreviewBitmapWidth;
    private int mPreviewBitmapHeight;
    private String mSize;
    private Context mContext;
    private Launcher mLauncher;
    private PackageManager mPackageManager;

    // Used for drawing shortcut previews
    private BitmapCache mCachedShortcutPreviewBitmap = new BitmapCache();
    private PaintCache mCachedShortcutPreviewPaint = new PaintCache();
    private CanvasCache mCachedShortcutPreviewCanvas = new CanvasCache();

    // Used for drawing widget previews
    private CanvasCache mCachedAppWidgetPreviewCanvas = new CanvasCache();
    private RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();
    private RectCache mCachedAppWidgetPreviewDestRect = new RectCache();
    private PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();
    private String mCachedSelectQuery;
    private BitmapFactoryOptionsCache mCachedBitmapFactoryOptions = new BitmapFactoryOptionsCache();

    private int mAppIconSize;
    private IconManager mIconManager = null;
//    private final float sWidgetPreviewIconPaddingPercentage = 0.25f;

    private CacheDb mDb;

    private HashMap<String, WeakReference<Bitmap>> mLoadedPreviews;
    private ArrayList<SoftReference<Bitmap>> mUnusedBitmaps;
    private static HashSet<String> sInvalidPackages;

    static {
        sInvalidPackages = new HashSet<String>();
    }

    public WidgetPreviewLoader(Launcher launcher) {
        mContext = mLauncher = launcher;
        mPackageManager = mContext.getPackageManager();
        mAppIconSize = mContext.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        LauncherApplication app = (LauncherApplication) launcher.getApplicationContext();
        mIconManager = app.getIconManager();
        mDb = app.getWidgetPreviewCacheDb();
        mLoadedPreviews = new HashMap<String, WeakReference<Bitmap>>();
        mUnusedBitmaps = new ArrayList<SoftReference<Bitmap>>();
    }

    /* YUNOS BEGIN PB*/
    //##modules(HomeShell): ##author:yongxing.lyx
    //##BugID:(158083) ##date:2014/10/28
    //##decrpition: add feature:edit mode.    
    public void setPreviewSize(int previewWidth, int previewHeight) {
        mPreviewBitmapWidth = previewWidth;
        mPreviewBitmapHeight = previewHeight;
        mSize = previewWidth + "x" + previewHeight;
    }
    
    public Bitmap getCachedPreview(final Object o) {
        String name = getObjectName(o);
        // check if the package is valid
        boolean packageValid = true;
        synchronized(sInvalidPackages) {
            packageValid = !sInvalidPackages.contains(getObjectPackage(o));
        }
        if (!packageValid) {
            return null;
        }
        if (packageValid) {
            synchronized(mLoadedPreviews) {
                // check if it exists in our existing cache
                if (mLoadedPreviews.containsKey(name) && mLoadedPreviews.get(name).get() != null) {
                    return mLoadedPreviews.get(name).get();
                }
            }
        }
        return null;
    }
    /*YUNOS END PB*/

    public Bitmap getPreview(final Object o) {
        String name = getObjectName(o);
        // check if the package is valid
        boolean packageValid = true;
        synchronized(sInvalidPackages) {
            packageValid = !sInvalidPackages.contains(getObjectPackage(o));
        }
        if (!packageValid) {
            return null;
        }
        if (packageValid) {
            synchronized(mLoadedPreviews) {
                // check if it exists in our existing cache
                if (mLoadedPreviews.containsKey(name) && mLoadedPreviews.get(name).get() != null) {
                    return mLoadedPreviews.get(name).get();
                }
            }
        }

        Bitmap unusedBitmap = null;
        synchronized(mUnusedBitmaps) {
            // not in cache; we need to load it from the db
            while ((unusedBitmap == null || !unusedBitmap.isMutable() ||
                    unusedBitmap.getWidth() != mPreviewBitmapWidth ||
                    unusedBitmap.getHeight() != mPreviewBitmapHeight)
                    && mUnusedBitmaps.size() > 0) {
                unusedBitmap = mUnusedBitmaps.remove(0).get();
            }
            if (unusedBitmap != null) {
                final Canvas c = mCachedAppWidgetPreviewCanvas.get();
                c.setBitmap(unusedBitmap);
                c.drawColor(0, PorterDuff.Mode.CLEAR);
                c.setBitmap(null);
            }
        }

        if (unusedBitmap == null) {
            /*YUNOS BEGIN*/        
            //##date:2014/1/17 ##author:xindong.zxd
        	//reconstruction widget bitmap, reduce the bitmap size
            //unusedBitmap = Bitmap.createBitmap(mPreviewBitmapWidth, mPreviewBitmapHeight,
            //        Bitmap.Config.ARGB_8888);         
            unusedBitmap = Bitmap.createBitmap(mPreviewBitmapWidth, mPreviewBitmapHeight,
                    Bitmap.Config.ARGB_4444);
            /* YUNOS END */
        }

        Bitmap preview = null;

        try {
            if (packageValid) {
                preview = readFromDb(name, unusedBitmap);
            }
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in readFromDb : throw SQLiteFullException");
        }

        if (preview != null) {
            synchronized(mLoadedPreviews) {
                mLoadedPreviews.put(name, new WeakReference<Bitmap>(preview));
            }
            return preview;
        } else {
            // it's not in the db... we need to generate it
            final Bitmap generatedPreview = generatePreview(o, unusedBitmap);
            preview = generatedPreview;
            if (preview != unusedBitmap) {
                /* YUNOS BEGIN */
                //author xiaodong.lxd comment exceptions
                //throw new RuntimeException("generatePreview is not recycling the bitmap " + o);
                Log.e(TAG, " generatePreview is not recycling the bitmap preview " + preview + " unusedBitmap " + unusedBitmap);
                return generatedPreview;
                /* YUNOS END */
            }

            synchronized(mLoadedPreviews) {
                mLoadedPreviews.put(name, new WeakReference<Bitmap>(preview));
            }

            // write to db on a thread pool... this can be done lazily and improves the performance
            // of the first time widget previews are loaded
            try {
                new AsyncTask<Void, Void, Void>() {
                    public Void doInBackground(Void ... args) {
                        writeToDb(o, generatedPreview);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Failed in writeToDb : throw SQLiteFullException");
            }

            return preview;
        }
    }

    public void recycleBitmap(Object o, Bitmap bitmapToRecycle) {
        String name = getObjectName(o);
        synchronized (mLoadedPreviews) {
            if (mLoadedPreviews.containsKey(name)) {
                Bitmap b = mLoadedPreviews.get(name).get();
                if (b == bitmapToRecycle) {
                    mLoadedPreviews.remove(name);
                    if (bitmapToRecycle.isMutable()) {
                        synchronized (mUnusedBitmaps) {
                            mUnusedBitmaps.add(new SoftReference<Bitmap>(b));
                        }
                    }
                } else {
                    throw new RuntimeException("Bitmap passed in doesn't match up");
                }
            }
        }
    }

    public static class CacheDb extends SQLiteOpenHelper {
        final static int DB_VERSION = 2;
        final static String DB_NAME = "widgetpreviews.db";
        final static String TABLE_NAME = "shortcut_and_widget_previews";
        final static String COLUMN_NAME = "name";
        final static String COLUMN_SIZE = "size";
        final static String COLUMN_PREVIEW_BITMAP = "preview_bitmap";
        Context mContext;

        public CacheDb(Context context) {
            super(context, new File(context.getCacheDir(), DB_NAME).getPath(), null, DB_VERSION);
            // Store the context for later use
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_SIZE + " TEXT NOT NULL, " +
                    COLUMN_PREVIEW_BITMAP + " BLOB NOT NULL, " +
                    "PRIMARY KEY (" + COLUMN_NAME + ", " + COLUMN_SIZE + ") " +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                // Delete all the records; they'll be repopulated as this is a cache
                db.execSQL("DELETE FROM " + TABLE_NAME);
            }
        }
    }

    private static final String WIDGET_PREFIX = "Widget:";
    /* YUNOS BEGIN */
    // ##gadget
    // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com##BugID:96378
    private static final String GADGET_PREFIX = "Gadget:";
    /* YUNOS END */
    private static final String SHORTCUT_PREFIX = "Shortcut:";

    private static String getObjectName(Object o) {
        // should cache the string builder
        StringBuilder sb = new StringBuilder();
        String output;
        if (o instanceof AppWidgetProviderInfo) {
            sb.append(WIDGET_PREFIX);
            sb.append(((AppWidgetProviderInfo) o).provider.flattenToString());
            output = sb.toString();
            sb.setLength(0);
            /* YUNOS BEGIN */
            // ##gadget
            // ##date:2014/02/27
            // ##author:kerong.skr@alibaba-inc.com##BugID:96378
        } else if (o instanceof GadgetInfo) {
            sb.append(GADGET_PREFIX);
            sb.append(((GadgetInfo) o).label);
            output = sb.toString();
            sb.setLength(0);
            /* YUNOS END */
        } else {
            sb.append(SHORTCUT_PREFIX);

            ResolveInfo info = (ResolveInfo) o;
            sb.append(new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name).flattenToString());
            output = sb.toString();
            sb.setLength(0);
        }
        return output;
    }

    private String getObjectPackage(Object o) {
        if (o instanceof AppWidgetProviderInfo) {
            return ((AppWidgetProviderInfo) o).provider.getPackageName();
            /* YUNOS BEGIN */
            // ##gadget
            // ##date:2014/02/27
            // ##author:kerong.skr@alibaba-inc.com##BugID:96378
        } else if (o instanceof GadgetInfo) {
            return ((GadgetInfo) o).label;
            /* YUNOS END */
        } else {
            ResolveInfo info = (ResolveInfo) o;
            return info.activityInfo.packageName;
        }
    }

    private void writeToDb(Object o, Bitmap preview) {
        try {
            String name = getObjectName(o);
            SQLiteDatabase db = mDb.getWritableDatabase();
            ContentValues values = new ContentValues();
    
            values.put(CacheDb.COLUMN_NAME, name);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            preview.compress(Bitmap.CompressFormat.PNG, 100, stream);
            values.put(CacheDb.COLUMN_PREVIEW_BITMAP, stream.toByteArray());
            values.put(CacheDb.COLUMN_SIZE, mSize);
            db.insert(CacheDb.TABLE_NAME, null, values);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in writeToDb : throw SQLiteFullException");
            throw e;
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in writeToDb : " + e.getMessage());
        }
    }

    public static void removeFromDb(final CacheDb cacheDb, final String packageName) {
        synchronized(sInvalidPackages) {
            sInvalidPackages.add(packageName);
        }
        try {
        new AsyncTask<Void, Void, Void>() {
            public Void doInBackground(Void ... args) {
                try {
                    SQLiteDatabase db = cacheDb.getWritableDatabase();
                    db.delete(CacheDb.TABLE_NAME,
                            CacheDb.COLUMN_NAME + " LIKE ? OR " +
                            CacheDb.COLUMN_NAME + " LIKE ?", // SELECT query
                            new String[] {
                                WIDGET_PREFIX + packageName + "/%",
                                SHORTCUT_PREFIX + packageName + "/%"} // args to SELECT query
                                );
                } catch (SQLiteFullException e) {
                    Log.e(TAG, "Failed in removeFromDb : throw SQLiteFullException");
                    throw e;
                } catch (SQLiteException e) {
                    Log.e(TAG, "Failed in removeFromDb : " + e.getMessage());
                }
                synchronized(sInvalidPackages) {
                    sInvalidPackages.remove(packageName);
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in removeFromDb : throw SQLiteFullException");
        }
    }

    private Bitmap readFromDb(String name, Bitmap b) {
        if (mCachedSelectQuery == null) {
            mCachedSelectQuery = CacheDb.COLUMN_NAME + " = ? AND " +
                    CacheDb.COLUMN_SIZE + " = ?";
        }
        
        try {
            SQLiteDatabase db = mDb.getReadableDatabase();
            Cursor result = db.query(CacheDb.TABLE_NAME,
                    new String[] { CacheDb.COLUMN_PREVIEW_BITMAP }, // cols to return
                    mCachedSelectQuery, // select query
                    new String[] { name, mSize }, // args to select query
                    null,
                    null,
                    null,
                    null);
            if (result.getCount() > 0) {
                result.moveToFirst();
                byte[] blob = result.getBlob(0);
                result.close();
                final BitmapFactory.Options opts = mCachedBitmapFactoryOptions.get();
                opts.inBitmap = b;
                opts.inSampleSize = 1;
                Bitmap out = BitmapFactory.decodeByteArray(blob, 0, blob.length, opts);
                return out;
            } else {
                result.close();
                return null;
            }
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in readFromDb : throw SQLiteFullException");
            throw e;
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in readFromDb : " + e.getMessage());
        }
        return null;
    }

    public Bitmap generatePreview(Object info, Bitmap preview) {
        if (preview != null &&
                (preview.getWidth() != mPreviewBitmapWidth ||
                preview.getHeight() != mPreviewBitmapHeight)) {
            /* YUNOS BEGIN */
            //author xiaodong.lxd comment exceptions
            //throw new RuntimeException("Improperly sized bitmap passed as argument");
            Log.e(TAG, "generatePreview Improperly sized bitmap passed as argument preview.getWidth() " + preview.getWidth() 
                    + " preview.getHeight() " + preview.getHeight() 
                    + "  mPreviewBitmapWidth " + mPreviewBitmapWidth 
                    + " mPreviewBitmapHeight " + + mPreviewBitmapHeight);
            /* YUNOS END */
        }
        if (info instanceof AppWidgetProviderInfo) {
            return generateWidgetPreview((AppWidgetProviderInfo) info, preview);

            /* YUNOS BEGIN */
            // ##gadget
            // ##date:2014/03/07 ##author:zhangqiang.zq
        } else if (info instanceof GadgetInfo) {
            GadgetInfo gadget = (GadgetInfo) info;
//            int max = gadget.spanX > gadget.spanY ? gadget.spanX : gadget.spanY;
            return ThemeUtils.getGadgetPreview(mContext, gadget,
                    mPreviewBitmapWidth,
                    mPreviewBitmapHeight);
            /* YUNOS END */

        } else {
            return generateShortcutPreview(
                    (ResolveInfo) info, mPreviewBitmapWidth, mPreviewBitmapHeight, preview);
        }
    }

    public Bitmap generateWidgetPreview(AppWidgetProviderInfo info, Bitmap preview) {
        int[] cellSpans = Launcher.getSpanForWidget(mLauncher, info);
        int maxWidth = maxWidthForWidgetPreview(cellSpans[0]);
        int maxHeight = maxHeightForWidgetPreview(cellSpans[1]);
        return generateWidgetPreview(info.provider, info.previewImage, info.icon,
                cellSpans[0], cellSpans[1], maxWidth, maxHeight, preview, null);
    }

    public int maxWidthForWidgetPreview(int spanX) {
        int w = mPreviewBitmapWidth * spanX / ConfigManager.getCellCountX();
        return Math.min(mPreviewBitmapWidth, w);
    }

    public int maxHeightForWidgetPreview(int spanY) {
        int h = mPreviewBitmapHeight * spanY / ConfigManager.getCellCountY();
        return Math.min(mPreviewBitmapHeight, h);
    }

    public Bitmap generateWidgetPreview(ComponentName provider, int previewImage,
            int iconId, int cellHSpan, int cellVSpan, int maxPreviewWidth, int maxPreviewHeight,
            Bitmap preview, int[] preScaledWidthOut) {
        // Load the preview image if possible
        String packageName = provider == null ? null : provider.getPackageName();
        if (maxPreviewWidth < 0) maxPreviewWidth = Integer.MAX_VALUE;
        if (maxPreviewHeight < 0) maxPreviewHeight = Integer.MAX_VALUE;

        Drawable drawable = null;
        if (previewImage != 0) {
            drawable = mPackageManager.getDrawable(packageName, previewImage, null);
            if (drawable == null) {
                Log.w(TAG, "Can't load widget preview drawable 0x" +
                        Integer.toHexString(previewImage) + " for provider: " + provider);
            }
        }

        int previewWidth;
        int previewHeight;
        Bitmap defaultPreview = null;
        boolean widgetPreviewExists = (drawable != null);
        if (widgetPreviewExists) {
            previewWidth = drawable.getIntrinsicWidth();
            previewHeight = drawable.getIntrinsicHeight();
        } else {
            // Generate a preview image if we couldn't load one
            if (cellHSpan < 1) cellHSpan = 1;
            if (cellVSpan < 1) cellVSpan = 1;

            BitmapDrawable previewDrawable = (BitmapDrawable) mContext.getResources()
                    .getDrawable(R.drawable.widget_preview_tile);
            final int previewDrawableWidth = previewDrawable
                    .getIntrinsicWidth();
            final int previewDrawableHeight = previewDrawable
                    .getIntrinsicHeight();
            previewWidth = previewDrawableWidth * cellHSpan; // subtract 2 dips
            previewHeight = previewDrawableHeight * cellVSpan;

            /*YUNOS BEGIN*/        
            //##date:2014/1/17 ##author:xindong.zxd
        	//reconstruction widget bitmap, reduce the bitmap size
            //defaultPreview = Bitmap.createBitmap(previewWidth, previewHeight,
            //        Config.ARGB_8888);          
            defaultPreview = Bitmap.createBitmap(previewWidth, previewHeight,Config.ARGB_4444);
            /* YUNOS END */
            
            try {
                Drawable icon = null;
                if (iconId > 0)
                    icon = mIconManager.getFullResIcon(packageName, iconId);
                if (icon != null) {
                    defaultPreview = IconUtils.drawableToBitmap(icon, mAppIconSize,mAppIconSize);
                }
            } catch (Resources.NotFoundException e) {
            }
        }

        // Scale to fit width only - let the widget preview be clipped in the
        // vertical dimension
        float scale = 1f;
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth;
        }
        if (previewWidth > maxPreviewWidth) {
            scale = maxPreviewWidth / (float) previewWidth;
        }
        /* YUNOS BEGIN */
        //added by xiaodong.lxd for bug 68996 in case previewWidth or previewHeight is 0
        if(scale <= 0.01f) {
            scale = 1f;
        }
        /* YUNOS END */
        if(scale != 1f) {
            previewWidth = (int) (scale * previewWidth);
            previewHeight = (int) (scale * previewHeight);
        }
        
        /* YUNOS BEGIN */
        //added by xiaodong.lxd for bug 68996 if previewWidth or previewHeight is 0 then use defaultPreview
        if(previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "sxsexe------->generateWidgetPreview  error previewWidth " + previewWidth + " previewHeight " + previewHeight);
            return defaultPreview;
        }
        /* YUNOS BEGIN */

        // If a bitmap is passed in, we use it; otherwise, we create a bitmap of the right size
        if (preview == null) {
            /*YUNOS BEGIN*/        
            //##date:2014/1/17 ##author:xindong.zxd
        	//reconstruction widget bitmap, reduce the bitmap size
            // preview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        	preview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_4444); 
        	/* YUNOS END */
        }

        // Draw the scaled preview into the final bitmap
        int x = (preview.getWidth() - previewWidth) / 2;
        if (widgetPreviewExists) {
            renderDrawableToBitmap(drawable, preview, x, 0, previewWidth,
                    previewHeight);
        } else {
            final Canvas c = mCachedAppWidgetPreviewCanvas.get();
            final Rect src = mCachedAppWidgetPreviewSrcRect.get();
            final Rect dest = mCachedAppWidgetPreviewDestRect.get();
            x = (preview.getWidth() - defaultPreview.getWidth()) / 2; 
            int y = (preview.getHeight() - defaultPreview.getHeight()) / 2;
            c.setBitmap(preview);
            src.set(0, 0, defaultPreview.getWidth(), defaultPreview.getHeight());
            dest.set(x, y, x + defaultPreview.getWidth(), y+defaultPreview.getHeight());

            Paint p = mCachedAppWidgetPreviewPaint.get();
            if (p == null) {
                p = new Paint();
                p.setFilterBitmap(true);
                mCachedAppWidgetPreviewPaint.set(p);
            }
            c.drawBitmap(defaultPreview, src, dest, p);
            c.setBitmap(null);
        }
        return preview;
    }

    private Bitmap generateShortcutPreview(
            ResolveInfo info, int maxWidth, int maxHeight, Bitmap preview) {
        Bitmap tempBitmap = mCachedShortcutPreviewBitmap.get();
        final Canvas c = mCachedShortcutPreviewCanvas.get();
        if (tempBitmap == null ||
                tempBitmap.getWidth() != maxWidth ||
                tempBitmap.getHeight() != maxHeight) {
            /*YUNOS BEGIN*/        
            //##date:2014/1/17 ##author:xindong.zxd
        	//reconstruction widget bitmap, reduce the bitmap size
            //tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
        	tempBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_4444);
        	/* YUNOS END */
            mCachedShortcutPreviewBitmap.set(tempBitmap);
        } else {
            c.setBitmap(tempBitmap);
            c.drawColor(0, PorterDuff.Mode.CLEAR);
            c.setBitmap(null);
        }
        // Render the icon
        Drawable icon = mIconManager.getFullResIcon(info);

        int paddingTop = mContext.
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_top);
        int paddingLeft = mContext.
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_left);
        int paddingRight = mContext.
                getResources().getDimensionPixelOffset(R.dimen.shortcut_preview_padding_right);

        int scaledIconWidth = (maxWidth - paddingLeft - paddingRight);
        int scaledIconHeight = maxHeight - paddingTop;

        renderDrawableToBitmap(
                icon, tempBitmap, paddingLeft, paddingTop, scaledIconWidth, scaledIconWidth);

        if (preview != null &&
                (preview.getWidth() != maxWidth || preview.getHeight() != maxHeight)) {
            throw new RuntimeException("Improperly sized bitmap passed as argument");
        } else if (preview == null) {
            /*YUNOS BEGIN*/        
            //##date:2014/1/17 ##author:xindong.zxd
        	//reconstruction widget bitmap, reduce the bitmap size
            //preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_8888);
        	preview = Bitmap.createBitmap(maxWidth, maxHeight, Config.ARGB_4444);
        	/* YUNOS END */
        }

        c.setBitmap(preview);
        // Draw a desaturated/scaled version of the icon in the background as a watermark
        Paint p = mCachedShortcutPreviewPaint.get();
        if (p == null) {
            p = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            p.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
            p.setAlpha((int) (255 * 0.06f));
            mCachedShortcutPreviewPaint.set(p);
        }
        //c.drawBitmap(tempBitmap, 0, 0, p);
        c.setBitmap(null);
        int offsetX = (scaledIconWidth-mAppIconSize)/2;
        int offsetY = (scaledIconHeight-mAppIconSize)/2;

        int iconHeight = icon.getIntrinsicHeight();
        int iconWidth = icon.getIntrinsicWidth();
        if (iconHeight > iconWidth) {
            iconWidth = iconWidth * mAppIconSize / iconHeight;
            iconHeight = mAppIconSize;
        } else {
            iconHeight = iconHeight * mAppIconSize / iconWidth;
            iconWidth = mAppIconSize;
        }
        renderDrawableToBitmap(icon, preview, offsetX, offsetY, iconWidth, iconHeight);

        return preview;
    }


    public static void renderDrawableToBitmap(
            Drawable d, Bitmap bitmap, int x, int y, int w, int h) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, 1f);
    }

    private static void renderDrawableToBitmap(
            Drawable d, Bitmap bitmap, int x, int y, int w, int h,
            float scale) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds); // Restore the bounds
            c.setBitmap(null);
        }
    }

}
