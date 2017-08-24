
package com.aliyun.homeshell.editmode;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.editmode.ThemesPreviewAdapter.ThemeAttr;
import com.aliyun.homeshell.editmode.WallpapersPreviewAdapter.WallpaperAttr;

public abstract class ThemeWallpaperBaseAdapter extends BaseAdapter {
    public final static int DATACHANGE_TYPE_WALLPAPER = 0;
    public final static int DATACHANGE_TYPE_THEME = 1;
    public final static int DATACHANGE_TYPE_ALL = 2;

    public final static String ID = "_id";
    public final static String KEY_ID = "key_id";
    public final static String IS_SYSTEM = "is_system";
    public final static String NAME = "label";
    public final static String PACKAGE_NAME = "theme_package";
    public final static String IS_CHECKED = "is_used";
    public final static String THUMBNAIL = "thumbnail";
    public final static String LAST_MODIFIED = "last_modified";

    public final static int OP_TYPE_INSERT = 0;
    public final static int OP_TYPE_DELETE = 1;
    public final static int OP_TYPE_UPDATE = 2;
    
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected ContentResolver mResolver;
    protected Uri mUri;

    private int mType;
    private String mCheckedID = null;
    private HashMap<String,BaseAttr> mCache = new HashMap<String,BaseAttr>();

    private ArrayList<String> mKeyIds;//chenjian, the key id of Theme&Wallpaper Database

    public static final String TAG = "ThemeWallpaperAdapter";

    /* YUNOS BEGIN PB */
    // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
    // ##BugID:(5735133) ##date:2015/1/30
    // ##description: wallpaper and theme thumbnail use matrix scale type.
    protected int mImageViewWidth, mImageViewHeight;

    private Matrix mMatrix = new Matrix();
    /* YUNOS END PB */

    /* YUNOS BEGIN PB */
    // ##module:HomeShell ##author:jinjiang.wjj
    // ##BugID:5631647 ##date:2014/12/15
    // ##description:restore factory settings, in edit mode wallpaper display
    // blank
    private View mListView;

    private View mLoadingLayout;

    /* YUNOS END PB */
    private ContentObserver mDBObserver = new ContentObserver(new Handler()){

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d(TAG, " ContentObserver onChange ");
            sHandler.post(new Runnable() {

                @Override
                public void run() {
                    loadItemKeyIds();
                }
            });

            if (mListView != null && mLoadingLayout != null) {
                mListView.setVisibility(View.VISIBLE);
                mLoadingLayout.setVisibility(View.INVISIBLE);
            }
        }
    };

    private HandlerThread mHandlerThread;
    private static MyHandler sHandler;

    private static final int MSG_DATA_INVALID = 0;

    private class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DATA_INVALID :
                    unlockSettingProcess();
                    if(LauncherApplication.mLauncher != null) {
                        LauncherApplication.mLauncher.postRunnableToMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "notifyDataSetChanged");
                                notifyDataSetChanged();
                            }
                        }, 0);
                    }
                    break;
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            int refreshType = intent.getIntExtra("type", 0);
            final String keyId = intent.getStringExtra("keyId");
            final int opType = intent.getIntExtra("op_type", OP_TYPE_UPDATE);
            Log.d(TAG, "onReceive com.yunos.theme.NOTIFY_DATA_CHANGE " + " keyId " + keyId + " mCheckedID " + mCheckedID);
            if (mType == refreshType) {
                /* YUNOS BEGIN PB */
                // ##date:2015/5/15 ##author:suhang.sh@alibaba-inc.com
                // ##module(homeshell)
                // ##description: load item ids asynchronously due to AliTheme
                // process will be in heavy overload after user
                // downloaded massive amount of wallpaper and themes
                sHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        synchronized (mKeyIds) {

                            if (opType == OP_TYPE_UPDATE) {
                                synchronized (mCache) {
                                    int i = 0;
                                    String prevCheckedIndex = null;
                                    String curCheckedIndex = null;
                                    for (; i < mKeyIds.size(); i++) {
                                        if (mKeyIds.get(i).equals(mCheckedID)) {
                                            prevCheckedIndex = mKeyIds.get(i);
                                        }
                                        if (mKeyIds.get(i).equals(keyId)) {
                                            curCheckedIndex = mKeyIds.get(i);
                                            // mCheckedID = curCheckedIndex;
                                        }
                                    }
                                    if (prevCheckedIndex != null)
                                        mCache.remove(prevCheckedIndex);
                                    if (curCheckedIndex != null)
                                        mCache.remove(curCheckedIndex);
                                }
                            }
                        }
                        sHandler.sendEmptyMessage(MSG_DATA_INVALID);
                    }
                });
                /* YUNOS END PB */

                /* YUNOS BEGIN PB */
                // ##module:HomeShell ##author:jinjiang.wjj
                // ##BugID:5631647 ##date:2014/12/15
                // ##description:restore factory settings, in edit mode
                // wallpaper display blank
                if (mListView != null && mLoadingLayout != null) {
                    mListView.setVisibility(View.VISIBLE);
                    mLoadingLayout.setVisibility(View.INVISIBLE);
                }
                /* YUNOS END PB */
            }
        }
    };

    public ThemeWallpaperBaseAdapter(Context context, int type) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mResolver = mContext.getContentResolver();
        mType = type;
        mUri = Uri.parse("content://" + "com.yunos.theme.thememanager.provider" + "/"
                + (type == DATACHANGE_TYPE_WALLPAPER ? "wallpapers" : "themes"));
        mKeyIds = new ArrayList<String>();
        /* YUNOS BEGIN PB */
        // ##modules(HomeShell): ##yongxing.lyx@alibaba-inc.com
        // ##BugID:(5735133) ##date:2015/1/30
        // ##description: wallpaper and theme thumbnail use matrix scale type.
        mImageViewWidth = (int) context.getResources().getDimension(R.dimen.preview_theme_image_width);
        mImageViewHeight = (int) context.getResources().getDimension(R.dimen.preview_theme_image_height);

        mHandlerThread = new HandlerThread("theme_wallpaper_thread");
        mHandlerThread.start();
        sHandler = new MyHandler(mHandlerThread.getLooper());

        /* YUNOS END PB */

        IntentFilter filter = new IntentFilter("com.yunos.theme.NOTIFY_DATA_CHANGE");
        mContext.registerReceiver(mBroadcastReceiver, filter);
        mResolver.registerContentObserver(mUri, true, mDBObserver);
        mDBObserver.onChange(false);
    }
    private void loadItemKeyIds() {
        String[] projection;
        String selection = THUMBNAIL + " IS NOT NULL";
        String sorOrder = LAST_MODIFIED + " ASC";
        if(mType == DATACHANGE_TYPE_WALLPAPER){
            projection = new String[] {KEY_ID};
            sorOrder = KEY_ID + " ASC";
        }else{
            projection = new String[] {PACKAGE_NAME};
            if (mType == DATACHANGE_TYPE_THEME) {
                sorOrder = ID + " ASC";
            } else {
                sorOrder = LAST_MODIFIED + " ASC";
            }
        }
        Log.d(TAG, " loadItemKeyIds mType " + mType);
        /* YUNOS BEGIN PB */
        //##modules(HomeShell): ##author:wencai.swc
        //##BugID:(5975538) ##date:2015/05/10
        Cursor c = null;
        synchronized (mKeyIds) {
            try {
                c = mResolver.query(mUri, projection, selection, null, sorOrder);
                synchronized (mCache) {
                    mCache.clear();
                }
                mKeyIds.clear();
        /* YUNOS END PB */
                if (c != null) {
                    while (c.moveToNext()) {
                        mKeyIds.add(c.getString(0));
                    }
                    if(mType == DATACHANGE_TYPE_WALLPAPER){
                        sortWallpaper();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
            }
        }
        sHandler.sendEmptyMessage(MSG_DATA_INVALID);
    }

    public void sortWallpaper(){
        Comparator<String> kidComparator = new Comparator<String>() {
                        @Override
                        public int compare(String lhs, String rhs) {
                            try {
                                if (lhs == null || rhs == null) {
                                    return 0;
                                }
                                long lhl = Long.valueOf(lhs);
                                long rhl = Long.valueOf(rhs);
                                return (int)(lhl - rhl);
                            } catch(Exception e) {
                                return lhs.compareTo(rhs);
                            }
                        }
                    };
        Collections.sort(mKeyIds, kidComparator);
    }
    @Override
    public int getCount() {
        int count = 0;
        if (null == mKeyIds) {
            count = 0;
        } else {
            synchronized (mKeyIds) {
                count = mKeyIds.size();
            }
        }

        return ConfigManager.isLandOrienSupport() ? count : count + 1;
    }

    private BaseAttr getItemAtPosition(int position) {
        BaseAttr item = null;

        synchronized (mKeyIds) {
            item = getItemByKeyId(mKeyIds.get(position));
        }

        return item;
    }

    private BaseAttr getItemByKeyId(String key_id) {
        String selection;
        BaseAttr item = null;
        //SoftReference<BaseAttr> cachedItem = null;
        //BaseAttr cachedItem = null;

        if(mType == DATACHANGE_TYPE_WALLPAPER){
            selection = KEY_ID + "=" + key_id.toString();
        }else{
            selection = PACKAGE_NAME + "='" + key_id.toString() + "'";
        }

        synchronized (mCache) {
            item = mCache.get(key_id);
        }
        if(item != null && item.checked) {
          mCheckedID = key_id;
        }
        Log.d(TAG, " getItemByKeyId from cache keyid " + key_id + " item " + item + " mCheckedID " + mCheckedID);
//        if (cachedItem != null) {
//            item = cachedItem.get();
//            if(item != null && item.checked) {
//                mCheckedID = key_id;
//            }
//            Log.d(TAG, " getItemByKeyId from cache keyid " + key_id + " item " + item + " mCheckedID " + mCheckedID);
//        }
        Cursor c = null;
        if (item == null) {
            try {
                // modified for
                // bugID:5964302:APR-MTK6735M_Pbase3.0.5-com.aliyun.homeshell-javaexception:NullPointerException
                //Uri uri = Uri.parse(mUri.toString()+"/"+key_id);
                //c = mResolver.query(uri,
                  //      null, null, null, null);
                //ContentUris.withAppendedId(contentUri, id)
                c = mResolver.query(mUri,null,selection,null,null);
                if (c != null) {
                    if (c.getCount() <= 0) {
                        return item;
                    }
                    if (c.moveToFirst()) {
                        item = getNewItem();
                        populateItem(item, c);
                        mCache.put(key_id, item/*new SoftReference<BaseAttr>(item)*/);
                        if (item.checked) {
                            mCheckedID = key_id;
                        } else if (mCheckedID != null && mCheckedID.equals(key_id)) {
                            mCheckedID = null;
                        }
                        Log.d(TAG, "getItemById from db  keyid " + key_id + " item " + item + " mCheckedID " + mCheckedID);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return item;
    }

    protected abstract void populateItem(BaseAttr item, Cursor cursor);

    protected BaseAttr getNewItem() {
        return new BaseAttr();
    }

    @Override
    public Object getItem(int position) {
        return getItemAtPosition(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
		//YUNOS BEGIN
		//chenjian.chenjian, skip the change
		//NEED to refine if not fluent
        Log.d(TAG, "getView  beging position " + position);
        ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = mInflater.inflate(R.layout.preview_theme_item, parent, false);
            vh.previewImgView = (ImageView) convertView.findViewById(R.id.preview_image);
            vh.previewChecked = (ImageView) convertView.findViewById(R.id.preview_checked);
            vh.titleTextView = (TextView) convertView.findViewById(R.id.preview_title);
            vh.previewAddMoreImgView = (ImageView)convertView.findViewById(R.id.preview_add_more_image);
            convertView.setTag(vh);
        }
        else {
            vh = (ViewHolder) convertView.getTag();
        }

        BaseAttr item = null;
        if (ConfigManager.isLandOrienSupport()) {
            item = getItemAtPosition(position);
        } else {
            if (position == 0) {
                if (mType == DATACHANGE_TYPE_WALLPAPER) {
                    vh.previewAddMoreImgView.setImageResource(R.drawable.wallpaper_add_more);
                    vh.titleTextView.setText((String) mContext.getResources().getString(R.string.edit_mode_ttitle_wallpaper_repo));
                } else {
                    vh.previewAddMoreImgView.setImageResource(R.drawable.theme_add_more);
                    vh.titleTextView.setText((String) mContext.getResources().getString(R.string.edit_mode_title_theme_repo));
                }
                vh.previewAddMoreImgView.setVisibility(View.VISIBLE);
                vh.previewImgView.setVisibility(View.GONE);
                vh.titleTextView.setVisibility(View.VISIBLE);
                vh.previewChecked.setVisibility(View.GONE);
                return convertView;
            }
            item = getItemAtPosition(position - 1);
        }

        if (item == null || item.thumbnail == null) {
            return convertView;
        }
        vh.previewAddMoreImgView.setVisibility(View.GONE);
        vh.titleTextView.setVisibility(View.GONE);
        vh.previewImgView.setVisibility(View.VISIBLE);
        vh.previewChecked.setVisibility(View.GONE);
        vh.previewImgView.setImageBitmap(item.thumbnail);
        float scale = ((float) mImageViewWidth) / item.thumbnail.getWidth();
        mMatrix.setScale(scale, scale);
        PointF offset = translatePreviewImage(item.thumbnail, scale);
        if (offset != null) {
            mMatrix.postTranslate(offset.x, offset.y);
        } else {
            mMatrix.postTranslate(0, 0);
        }
        vh.previewImgView.setScaleType(ScaleType.MATRIX);
        vh.previewImgView.setImageMatrix(mMatrix);
        vh.previewImgView.setImageBitmap(item.thumbnail);
        if (mType == DATACHANGE_TYPE_WALLPAPER) {
            vh.titleTextView.setVisibility(View.GONE);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)vh.previewChecked.getLayoutParams();
            int marginTop = mContext.getResources().getDimensionPixelSize(R.dimen.preview_checked_image_wallpaper_margin_top);
            int marginRight = mContext.getResources().getDimensionPixelSize(R.dimen.preview_checked_image_wallpaper_margin_right);
            params.setMargins(params.leftMargin, marginTop, marginRight, params.bottomMargin);
        } else {
            vh.titleTextView.setText(item.name);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)vh.previewChecked.getLayoutParams();
            int marginTop = mContext.getResources().getDimensionPixelSize(R.dimen.preview_checked_image_theme_margin_top);
            int marginRight = mContext.getResources().getDimensionPixelSize(R.dimen.preview_checked_image_theme_margin_right);
            params.setMargins(params.leftMargin, marginTop, marginRight, params.bottomMargin);
        }

        if (item.checked) {
            vh.previewChecked.setVisibility(View.VISIBLE);
        }
        else {
            vh.previewChecked.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG, "getView  end position " + position);
        return convertView;
    }

    protected PointF translatePreviewImage(Bitmap thumbnail, float scale) {
        // no translate by default
        return null;
    }

    @Override
    public long getItemId(int position) {
        //long id = 0;
        //if(position == 0)
            //return 0;
        //synchronized (mKeyIds) {
            //id = mKeyIds == null ? -1 : mKeyIds.get(position-1);
        //}

        return position;
    }

    /* YUNOS BEGIN PB */
    // ##module:HomeShell ##author:jinjiang.wjj
    // ##BugID:5631647 ##date:2014/12/15
    // ##description:restore factory settings, in edit mode wallpaper display
    // blank
    public void setView(View listView, View loadLayout) {
        mListView = listView;
        mLoadingLayout = loadLayout;
    }

    /* YUNOS END PB */

    private boolean mIsInSettingProcess = false;
    private Timer mTimer = new Timer();
    private MyTimerTask myTimerTask;
    private static final int TIMER_TIMEOUT = 2000;

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.d(TAG, "MyTimerTask.run mIsInSettingProcess " + mIsInSettingProcess);
            if (mIsInSettingProcess) {
                unlockSettingProcess();
            }
            mIsInSettingProcess = false;
        }
    }

    private void lockSettingProcess() {
        Log.d(TAG, "lockSettingProcess");
        mIsInSettingProcess = true;
        if (myTimerTask != null) {
            myTimerTask.cancel();
        }
        myTimerTask = new MyTimerTask();
        mTimer.schedule(myTimerTask, TIMER_TIMEOUT, TIMER_TIMEOUT);
    }

    private void unlockSettingProcess() {
        Log.d(TAG, "unlockSettingProcess");
        if (myTimerTask != null) {
            myTimerTask.cancel();
            myTimerTask = null;
        }
        mIsInSettingProcess = false;
    }

    private boolean isProcessLocked() {
        return mIsInSettingProcess;
    }

    public boolean setCheckedItem(BaseAttr attr) {
        String attrKey = null;
        if (attr instanceof WallpaperAttr) {
            WallpaperAttr wallpaperAttr = (WallpaperAttr) attr;
            attrKey = wallpaperAttr.keyID;
        } else if (attr instanceof ThemeAttr) {
            ThemeAttr themeAttr = (ThemeAttr) attr;
            attrKey = themeAttr.packageName;
        }
        if (attrKey == null) {
            return false;
        }
        Log.d(TAG, ">>>> setCheckedItem mCheckedID " + mCheckedID + " attr " + attr + " mIsInSettingProcess "
                + mIsInSettingProcess);
        if (mCheckedID != null && mCheckedID.equals(attrKey)) {
            return false;
        }
        if (mCheckedID == null || (mCheckedID != null && !mCheckedID.equals(attrKey))) {
            // check setting is in process, if true, ignore this click event
            if (isProcessLocked()) {
                // do nothing
                return false;
            } else {
                lockSettingProcess();
                if (mCheckedID != null) {
                    BaseAttr previousAttr = getItemByKeyId(mCheckedID);
                    if (previousAttr != null) {
                        previousAttr.checked = false;
                    }
                }
                mCheckedID = attrKey;
                attr.checked = true;
                Log.d(TAG, "setCheckedItem notifyDataSetChanged");
                notifyDataSetChanged();
                return true;
            }
        }
        return false;
    }

    public void onExit() {
        if (mContext != null && mBroadcastReceiver != null) {
            try {
                mContext.unregisterReceiver(mBroadcastReceiver);
            } catch (Exception e) {
            }
        }
        if (mHandlerThread != null && mHandlerThread.isAlive()) {
            mHandlerThread.quitSafely();
        }
        if (mContext != null && mDBObserver != null) {
            try {
                mContext.getContentResolver().unregisterContentObserver(mDBObserver);
            } catch (Exception e) {
            }
        }
    }

    class ViewHolder {
        public int position;
        public ImageView previewImgView;
        public TextView titleTextView;
        public ImageView previewChecked;
        public ImageView previewAddMoreImgView;
    }

    class BaseAttr {
        String id;
        String name;
        boolean isSystem = false;
        Bitmap thumbnail;
        boolean checked = false;
    }

}
