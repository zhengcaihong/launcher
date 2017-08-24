package com.aliyun.homeshell.appgroup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.utils.Utils;
import android.os.Build;

import com.aliyun.homeshell.setting.HomeShellSetting;

public class AppGroupManager {

    private static final String TAG = AppGroupManager.class.getSimpleName();
    public static final String URL = "http://apps.aliyun.com/api.htm";
    public static final String SP_UPDATE_LATEST_TIME = "update_latest_time";
    public static final String SP_LOAD_INFO_RESULT = "load_app_group_info_result";
    public static final String SP_INIT_DEFAULT_APP_DB = "init_default_app_db";
    public static final long UPDATE_PERIOD_TIME = 604800000;// weekly
    public static final int DEFAULT_BUFFER_SIZE = 8196;

    private static AppGroupManager sInstance = new AppGroupManager();
    private boolean mIsLoadedSuccess = false;
    private boolean mIsLoading = false;
    ArrayList<AppInfo> allAppInfoList = new ArrayList<AppInfo>(DEFAULT_TOP_SIZE);
    ArrayList<CategoryInfo> allCategoryInfoList = new ArrayList<CategoryInfo>();
    SharedPreferences mSp;
    private AppInfoDatabaseHelper mAppInfoInServerDB;
    private SQLiteDatabase mDatabase;
    private AtomicInteger mDBOpenCounter = new AtomicInteger();

    private static boolean sOff = false;

    static Object objectLock = new Object();

    class Params {
        public static final String ID = "id";
        public static final String METHOD = "method=";
        public static final String PAGE = "page=";
        public static final String PAGESIZE = "pageSize=";
        public static final String PACKAGENAME = "packageName=";
    }

    class Json {
        public final static String RESULT = "result";
        public final static String RESULT_CODE = "resultCode";
        public final static String RESULT_SUCCESS = "1000";
        public final static String CATEGORYLIST = "categorys";
        public final static String ITEMLIST = "itemList";

        public final static String ID = "id";
        public final static String CATEGORY_ID = "catId";
        public final static String ROOT_CATEGORY_ID = "rootCatId";
        public final static String NAME = "name";
        public final static String PARENT = "parent";
        public final static String TAG1 = "tag1";
        public final static String TAG2 = "tag2";
        public final static String PACKAGENAME = "packageName";
    }

    class Method {
        public final static String GET_CATEGORY = "listCatForDesk";
        public final static String GET_APPS = "getTopForDesk";
        public final static String CHECK_APP = "getCatByPkgForDesk";
    }

    public static final int DEFAULT_TOP_SIZE = 2000;
    public static final int DEFAULT_PAGE_SIZE = 200;
    public static final long DELAY_TIME_LONG = 300000;
    public static final long DELAY_TIME_SHORT = 500;
    //BugID:5860468:avoid server data less than min size
    //for full update, the top apps should more than min size
    public static final int DEFAULT_MIN_DOWNLOAD_SIZE = 500;

    static String mStrAdd;

    public interface Callback {
        public void onResult(FolderIcon fi, String folderName);
    }

    private AppGroupManager() {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        mSp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        mAppInfoInServerDB = new AppInfoDatabaseHelper(
                LauncherApplication.getContext());
    }

    public static AppGroupManager getInstance() {
        mStrAdd = LauncherApplication.getContext().getResources().getString(R.string.str_add);
        return sInstance;
    }

    public boolean isLoadedSuccess() {
        return mSp.getBoolean(SP_LOAD_INFO_RESULT, false);
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public void setStatus(boolean isLoading) {
        mIsLoading = isLoading;
    }

    private void setUrlConnParameter(HttpURLConnection url_con, byte[] bytes) throws IOException {
        url_con.setRequestMethod("GET");
        url_con.setConnectTimeout(5000);
        url_con.setReadTimeout(5000);
        url_con.setDoOutput(true);
        url_con.getOutputStream().write(bytes, 0, bytes.length);
        url_con.getOutputStream().flush();
        url_con.getOutputStream().close();
    }

    /**
     * Get folder name by package name from database
     * 
     * @param fi
     * @param cb
     * @param pkgName1
     * @param pkgName2
     * @return
     */
    public void handleFolderNameByPkgNames(final FolderIcon fi,
            final Callback cb, final String pkgName1, final String pkgName2) {

        long start = System.currentTimeMillis();
        Log.d(TAG, "getFolderNameByPkgNames pkgName1 pkgName2 " + pkgName1
                + " - " + pkgName2);

        String folderName = "";
        String catID1 = getCatIdByPkg(pkgName1);
        String catID2 = getCatIdByPkg(pkgName2);
        Log.d(TAG, "getFolderNameByPkgNames catID1 catID2 " + catID1
                + " - " + catID2);
        CategoryInfo info1 = null;
        CategoryInfo info2 = null;
        boolean isEmptyCatID1 = TextUtils.isEmpty(catID1);
        boolean isEmptyCatID2 = TextUtils.isEmpty(catID2);
        info1 = getCatInfoByCatID(catID1);
        info2 = getCatInfoByCatID(catID2);

        if (isEmptyCatID1 ^ isEmptyCatID2) {
            if (info1 != null) {
                folderName = info1.showName;
            }
            if (info2 != null) {
                folderName = info2.showName;
            }
        } else {
            if (info1 != null && !isEmptyCatID1
                    && catID1.equals(catID2)) {
                folderName = info1.showName;
            } else if (info1 != null && info2 != null) {
                String supName1 = info1.supName;
                String supName2 = info2.supName;
                String supID1 = info1.supID;
                String supID2 = info2.supID;
                Log.d(TAG,
                        "getFolderNameByPkgNames supName1 supName2 = "
                                + supName1 + " - " + supName2);
                if (!TextUtils.isEmpty(supName1)
                        && !TextUtils.isEmpty(supName2)) {
                    if (!TextUtils.isEmpty(supID1)
                            && supID1.equals(supID2)
                            && supName1.equals(supName2)) {
                        folderName = supName1;
                    } else {
                        folderName = supName1 + mStrAdd + supName2;
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(folderName)) {
//            cb.onResult(fi, folderName);
            fi.getFolderInfo().setTitle("");
            fi.setTitle(folderName);
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "getFolderNameByPkgNames name = " + folderName + " took " + (end - start) + "ms");
    }

    /**
     * get folder name by first two pkg names
     *
     * @param pkgName1
     * @param pkgName2
     * @return folder name
     */
    public String getFolderNameByPkgNames(final String pkgName1, final String pkgName2) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "Input pkgName1 pkgName2 " + pkgName1
                + " - " + pkgName2);

        String folderName = "";
        String catID1 = getCatIdByPkg(pkgName1);
        String catID2 = getCatIdByPkg(pkgName2);
        Log.d(TAG, "getFolderNameByPkgNames catID1 catID2 " + catID1
                + " - " + catID2);
        CategoryInfo info1 = null;
        CategoryInfo info2 = null;
        boolean isEmptyCatID1 = TextUtils.isEmpty(catID1);
        boolean isEmptyCatID2 = TextUtils.isEmpty(catID2);
        info1 = getCatInfoByCatID(catID1);
        info2 = getCatInfoByCatID(catID2);

        if (isEmptyCatID1 ^ isEmptyCatID2) {
            if (info1 != null) {
                folderName = info1.showName;
            }
            if (info2 != null) {
                folderName = info2.showName;
            }
        } else {
            if (info1 != null && !isEmptyCatID1
                    && catID1.equals(catID2)) {
                folderName = info1.showName;
            } else if (info1 != null && info2 != null) {
                String supName1 = info1.supName;
                String supName2 = info2.supName;
                String supID1 = info1.supID;
                String supID2 = info2.supID;
                Log.d(TAG,
                        "getFolderNameByPkgNames supName1 supName2 = "
                                + supName1 + " - " + supName2);
                if (!TextUtils.isEmpty(supName1)
                        && !TextUtils.isEmpty(supName2)) {
                    if (!TextUtils.isEmpty(supID1)
                            && supID1.equals(supID2)
                            && supName1.equals(supName2)) {
                        folderName = supName1;
                    } else {
                        folderName = supName1 + mStrAdd + supName2;
                    }
                }
            }
        }
        long end = System.currentTimeMillis();
        Log.d(TAG, "get folder name = " + folderName + " took " + (end - start) + "ms");
        return folderName;
    }
    /**
     * handle single application
     * 
     * @param pkgName
     */
    public void handleSingleApp(final String pkgName) {
        if (sOff) {
            return;
        }
        Log.d(TAG, "handleSingleApp pkgName " + pkgName);

        String catID = getCatIdByPkg(pkgName);

        if (TextUtils.isEmpty(catID)) {
            new Thread("handleSgingleApp") {
                public void run() {
                    if(Build.YUNOS_CTA_SUPPORT) {
                        return;
                    }
                    String catId = requestSingleAppFromServer(pkgName);
                    if (!TextUtils.isEmpty(catId)) {
                        SQLiteDatabase db = openDatabase();
                        if(null == db){
                            Log.e(TAG,"handleSingleApp error at openDatabase");
                            return;
                        }
                        addApp2DB(db, new AppInfo(pkgName, catId));
                        closeDatabase();
                    }
                }
            }.start();
        }
    }

    private boolean allowGetAppInfo() {
        return HomeShellSetting.getUpdateInfoValue(LauncherApplication.getContext());
    }

    /**
     * Get category id by package name from server
     * 
     * @param pkgName
     * @return
     */
    public String requestSingleAppFromServer(String pkgName) {
        if (!Utils.isNetworkConnected() || TextUtils.isEmpty(pkgName) || !allowGetAppInfo()) {
            return "";
        }
        HttpURLConnection url_con = null;
        AppInfo appInfo = null;
        InputStream in = null;
        BufferedReader rd = null;
        try {
            StringBuffer postBuffer = new StringBuffer();
            postBuffer.append(Params.METHOD + Method.CHECK_APP);
            postBuffer.append("&");
            postBuffer.append(Params.PACKAGENAME + pkgName);
            byte[] b = postBuffer.toString().getBytes();
            URL url = new URL(URL);
            url_con = (HttpURLConnection) url.openConnection();
            setUrlConnParameter(url_con, b);
            in = url_con.getInputStream();
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer temp = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                temp.append(tempLine);
                temp.append(crlf);
                tempLine = rd.readLine();
            }
            appInfo = parserSingleAppJson(temp.toString());
        } catch (Exception e) {
            Log.e(TAG, "getCatIDByPkgNameFromServer error ", e);
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                    rd = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "getCatIDByPkgNameFromServer io exception ", e);
            }
            if (url_con != null) {
                url_con.disconnect();
            }
        }
        if (appInfo == null) {
            return "";
        }
        synchronized (objectLock) {
            allAppInfoList.add(appInfo);
        }
        return appInfo.catID;
    }

    /**
     * Parser category information from xml by json
     * 
     * @param jsonData
     * @return
     */
    private ArrayList<CategoryInfo> parserCategoryJson(String jsonData) {
        JSONTokener jsonParser = new JSONTokener(jsonData);
        JSONObject object = null;
        ArrayList<CategoryInfo> categoryInfoList = new ArrayList<CategoryInfo>();
        CategoryInfo info;
        try {
            object = (JSONObject) jsonParser.nextValue();
            JSONObject result = object.getJSONObject(Json.RESULT);
            int resultCode = object.getInt(Json.RESULT_CODE);
            if (Json.RESULT_SUCCESS.equals(String.valueOf(resultCode))) {
                JSONArray categoryList = result.getJSONArray(Json.CATEGORYLIST);
                int length = categoryList.length();
                JSONObject item;
                String catID = "";
                String catName = "";
                String supCatID = "";
                String showName = "";
                String supName = "";
                for (int index = 0; index < length; index++) {
                    item = (JSONObject) categoryList.get(index);
                    int id = item.getInt(Json.ID);
                    if (id != -1) {
                        catID = String.valueOf(id);
                    }
                    catName = item.getString(Json.NAME);
                    int supId = item.getInt(Json.PARENT);
                    if (supId != 0) {
                        supCatID = String.valueOf(supId);
                        try {
                            showName = item.getString(Json.TAG1);
                            supName = item.getString(Json.TAG2);
                        } catch (Exception e) {
                            showName = "";
                            supName = "";
                        }
                    } else {
                        showName = supName = catName;
                        supCatID = String.valueOf(catID);
                    }

                    info = new CategoryInfo(catName, catID);
                    if (TextUtils.isEmpty(showName)
                            || TextUtils.isEmpty(supName)) {
                        CategoryInfo tempInfo = getCatInfoByCatID(catID);
                        if (tempInfo != null) {
                            showName = tempInfo.showName;
                            supName = tempInfo.showName;
                        }
                    }
                    info.showName = showName;
                    info.supName = supName;
                    info.supID  = supCatID;
                    categoryInfoList.add(info);
                }
            } else {
                mIsLoadedSuccess = false;
            }
        } catch (Exception e) {
            mIsLoadedSuccess = false;
            Log.e(TAG, "parserCategoryJson error ", e);
        }
        return categoryInfoList;
    }

    /**
     * Parser application information from xml by json
     * 
     * @param jsonData
     * @param categoryCode
     * @param categoryName
     * @return
     */
    private ArrayList<AppInfo> parserAppJson(String jsonData) {
        JSONTokener jsonParser = new JSONTokener(jsonData);
        JSONObject object = null;
        ArrayList<AppInfo> appInfoList = new ArrayList<AppInfo>();
        try {
            object = (JSONObject) jsonParser.nextValue();
            JSONObject result = object.getJSONObject(Json.RESULT);
            int resultCode = object.getInt(Json.RESULT_CODE);
            if (Json.RESULT_SUCCESS.equals(String.valueOf(resultCode))) {
                JSONArray appList = result.getJSONArray(Json.ITEMLIST);
                int length = appList.length();
                JSONObject item;
                String pkgName;
                int catID;
                AppInfo app;
                for (int index = 0; index < length; index++) {
                    item = (JSONObject) appList.get(index);
                    pkgName = item.getString(Json.PACKAGENAME);
                    catID = item.getInt(Json.CATEGORY_ID);
                    app = new AppInfo(pkgName, String.valueOf(catID));
                    appInfoList.add(app);
                }
            } else {
                mIsLoadedSuccess = false;
            }
        } catch (Exception e) {
            mIsLoadedSuccess = false;
            Log.e(TAG, "parserAppJson error ", e);
        }
        return appInfoList;
    }

    /**
     * Parser single application information from xml by json
     * 
     * @param jsonData
     * @return
     */
    private AppInfo parserSingleAppJson(String jsonData) {
        JSONTokener jsonParser = new JSONTokener(jsonData);
        JSONObject object = null;
        AppInfo app = null;
        try {
            object = (JSONObject) jsonParser.nextValue();
            JSONObject result = object.getJSONObject(Json.RESULT);
            int resultCode = object.getInt(Json.RESULT_CODE);
            if (Json.RESULT_SUCCESS.equals(String.valueOf(resultCode))) {
                int catID = result.getInt(Json.CATEGORY_ID);
                String pkgName = result.getString(Json.PACKAGENAME);
                app = new AppInfo(pkgName, String.valueOf(catID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return app;
    }

    private boolean copyDBFromAsset() {
        Context context = LauncherApplication.getContext();
        AssetManager am = context.getAssets();//
        FileOutputStream fos = null;
        InputStream is = null;
        boolean result = true;
        try {
            is = am.open(AppInfoDatabaseHelper.DB_DEFAULT_APPINFO);
            File file = context
                    .getDatabasePath(AppInfoDatabaseHelper.DB_NAME);
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int count = 0;
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            Log.d(TAG, " copyDBFromAsset success ");
            mSp.edit().putBoolean(SP_INIT_DEFAULT_APP_DB, true)
            .commit();
        } catch (IOException e) {
            result = false;
            Log.e(TAG, "copyDBFromAsset error ", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Initializes the application information,copy the application db to system
     */
    public void initAppInfos() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean flag = true;
                if (!mSp.getBoolean(SP_INIT_DEFAULT_APP_DB, false)) {
                    flag = copyDBFromAsset();
                }
                Log.d(TAG, " initAppInfos flag " + flag);
                if(flag) {
                    SQLiteDatabase db = openDatabase();
                    if(null == db){
                        Log.e(TAG,"initAppInfos error at openDatabase");
                        return;
                    }
                    addSystemApp2DB(db);
                    // load cat table
                    loadCatTableToCache(db);
                    // load appinfo table
                    loadAppInfoTableToCache(db);
                    closeDatabase();
                    Log.d(TAG, " initAppInfos load data from db to cache ok allAppInfoList.size " + allAppInfoList.size());
                }
            }
        }).start();
    }

    private void loadAppInfoTableToCache(SQLiteDatabase db) {
        if(db == null) {
            return;
        }
        AppInfo info = null;
        Cursor cursor = null;
        String pkgName = null;
        String catID = null;
        try {
            cursor = db.query(AppInfoDatabaseHelper.TABLE_APP_NAME, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                pkgName = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_PKG_NAME));
                catID = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_CATEGORY_ID));
                info = new AppInfo(pkgName, catID);
                allAppInfoList.add(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAppInfoTableToCache error ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void loadCatTableToCache(SQLiteDatabase db) {
        if(db == null) {
            return;
        }

        CategoryInfo info = null;
        String catName = "";
        String showName = "";
        String supId = "";
        String supName = "";
        String catId = "";
        Cursor cursor = null;
        try {
            cursor = db.query(AppInfoDatabaseHelper.TABLE_CAT_NAME, null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                catName = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_ORIGINAL_NAME));
                showName = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_SHOW_NAME));
                supId = cursor.getString(cursor
                        .getColumnIndex(AppInfoDatabaseHelper.COLUMN_SUPER_ID));
                supName = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_SUPER_NAME));
                catId = cursor
                        .getString(cursor
                                .getColumnIndex(AppInfoDatabaseHelper.COLUMN_CATEGORY_ID));
                info = new CategoryInfo(catName, catId, supId, showName, supName);
                allCategoryInfoList.add(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadAppInfoTableToCache error ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private ArrayList<AppInfo> getSystemAppList() {
        Resources res = LauncherApplication.getContext().getResources();
        String[] pkgNames = res.getStringArray(R.array.sys_app_pkg_name);
        String[] catIDs = res.getStringArray(R.array.sys_app_cat_id);
        int pkgNamesLength = pkgNames.length;
        int catIDsLength = catIDs.length;
        ArrayList<AppInfo> lstData = new ArrayList<AppInfo>(pkgNamesLength);
        for (int i = 0; i < pkgNamesLength && i < catIDsLength; i++) {
            lstData.add(new AppInfo(pkgNames[i], catIDs[i]));
        }
        return lstData;
    }

    /**
     * Load application group information from the server
     */
    public void loadAppGroupInfosFromServer() {
        if (!Utils.isNetworkConnected() || sOff || !allowGetAppInfo()) {
            mIsLoading = false;
            return;
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                mIsLoading = true;
                mIsLoadedSuccess = true;

                ArrayList<CategoryInfo> lstTemp = requestAppCategory();
                ArrayList<AppInfo> lstTemp2 = new ArrayList<AppInfo>(DEFAULT_TOP_SIZE);
                int pageCount = DEFAULT_TOP_SIZE / DEFAULT_PAGE_SIZE;
                for (int index = 1; index <= pageCount; index++) {
                    lstTemp2.addAll(requestAppInfoByCategory(index));
                }
                lstTemp2.addAll(getSystemAppList());

                if (mIsLoadedSuccess && lstTemp.size() > 0
                                               && lstTemp2.size() > DEFAULT_MIN_DOWNLOAD_SIZE) {
                    synchronized (objectLock) {
                        allCategoryInfoList.clear();
                        allCategoryInfoList.addAll(lstTemp);
                        allAppInfoList.clear();
                        allAppInfoList.addAll(lstTemp2);
                    }
                    Log.d(TAG, "sxsexe---------------> loadAppGroupInfosFromServer  ok allCategoryInfoList.size " + allCategoryInfoList.size()
                            + " allAppInfoList.size " + allAppInfoList.size());

                    SQLiteDatabase db = null;
                    synchronized (objectLock) {
                        try {
                            db = openDatabase();
                            if(null == db){
                                Log.e(TAG,"loadAppGoupInfosFromServer error at openDatabase");
                                return;
                            }
                            cleanAllData2DB(db);
                            Thread.sleep(100);

                            db.beginTransaction();
                            for (CategoryInfo categoryInfo : allCategoryInfoList) {
                                addCategory2DB(db, categoryInfo);
                            }
                            for (AppInfo app : allAppInfoList) {
                                addApp2DB(db, app);
                            }

                            db.setTransactionSuccessful();
                            db.endTransaction();

                            closeDatabase();
                        } catch (Exception e) {
                            mIsLoadedSuccess = false;
                            Log.e(TAG, "loadAppGroupInfosFromServer error", e);
                        } finally {
                            if (db != null) {
                                db.close();
                            }
                        }
                    }
                }
                long currentTime = System.currentTimeMillis();
                Editor editor = mSp.edit();
                editor.putBoolean(SP_LOAD_INFO_RESULT, mIsLoadedSuccess);
                editor.putLong(SP_UPDATE_LATEST_TIME, currentTime);
                editor.commit();
                mIsLoading = false;
                mIsLoadedSuccess = false;

            }
        }).start();

    }

    /**
     * Request the application category info by http
     */
    private ArrayList<CategoryInfo> requestAppCategory() {
        HttpURLConnection url_con = null;
        String responseContent = null;
        BufferedReader rd = null;
        InputStream in = null;
        ArrayList<CategoryInfo> listCatData = new ArrayList<CategoryInfo>();
        try {
            StringBuffer postBuffer = new StringBuffer(Params.METHOD
                    + Method.GET_CATEGORY);
            byte[] b = postBuffer.toString().getBytes();
            URL url = new URL(URL);
            url_con = (HttpURLConnection) url.openConnection();
            setUrlConnParameter(url_con, b);
            in = url_con.getInputStream();
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer temp = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                temp.append(tempLine);
                temp.append(crlf);
                tempLine = rd.readLine();
            }
            responseContent = temp.toString();
            listCatData = parserCategoryJson(responseContent);
        } catch (Exception e) {
            mIsLoadedSuccess = false;
            Log.e(TAG, "requestAppCategory error ", e);
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (url_con != null) {
                url_con.disconnect();
            }
        }
        return listCatData;
    }

    /**
     * Request the application information by http with the help of application
     * category
     * 
     * @param pageIndex
     */
    private ArrayList<AppInfo> requestAppInfoByCategory(int pageIndex) {
        HttpURLConnection url_con = null;
        String responseContent = null;
        BufferedReader rd = null;
        InputStream in = null;
        ArrayList<AppInfo> lstAppData = new ArrayList<AppInfo>(DEFAULT_PAGE_SIZE);
        try {

            StringBuffer postBuffer = new StringBuffer();
            postBuffer.append(Params.METHOD + Method.GET_APPS);
            postBuffer.append("&");
            postBuffer.append(Params.PAGE + pageIndex);
            postBuffer.append("&");
            postBuffer.append(Params.PAGESIZE + DEFAULT_PAGE_SIZE);
            byte[] b = postBuffer.toString().getBytes();
            URL url = new URL(URL);
            url_con = (HttpURLConnection) url.openConnection();
            setUrlConnParameter(url_con, b);
            in = url_con.getInputStream();
            rd = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tempLine = rd.readLine();
            StringBuffer temp = new StringBuffer();
            String crlf = System.getProperty("line.separator");
            while (tempLine != null) {
                temp.append(tempLine);
                temp.append(crlf);
                tempLine = rd.readLine();
            }
            responseContent = temp.toString();
            lstAppData = parserAppJson(responseContent);
        } catch (Exception e) {
            mIsLoadedSuccess = false;
            Log.e(TAG, "requestAppInfoByCategory error ", e);
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (url_con != null) {
                url_con.disconnect();
            }
        }
        return lstAppData;
    }

    /**
     * add system application informations to db
     * 
     * @param db
     */
    private void addSystemApp2DB(SQLiteDatabase db) {
        Resources res = LauncherApplication.getContext().getResources();
        String[] pkgNames = res.getStringArray(R.array.sys_app_pkg_name);
        String[] catIDs = res.getStringArray(R.array.sys_app_cat_id);
        int pkgNamesLength = pkgNames.length;
        int catIDsLength = catIDs.length;
        for (int i = 0; i < pkgNamesLength && i < catIDsLength; i++) {
            addApp2DB(db, new AppInfo(pkgNames[i], catIDs[i]));
        }
    }

    private boolean addApp2DB(SQLiteDatabase db, AppInfo app) {
        Cursor c = null;
        ContentValues values = new ContentValues();
        values.put(AppInfoDatabaseHelper.COLUMN_PKG_NAME, app.pkgName);
        values.put(AppInfoDatabaseHelper.COLUMN_CATEGORY_ID, app.catID);
        long result = -1;
        try {
            String selection = AppInfoDatabaseHelper.COLUMN_PKG_NAME + "='" + app.pkgName + "'";
            c = db.query(AppInfoDatabaseHelper.TABLE_APP_NAME, null, selection, null, null, null, null);
            if((c != null) && (c.getCount() > 0)){
                if(c.moveToFirst()){
                    String catId = c.getString(c.getColumnIndex(AppInfoDatabaseHelper.COLUMN_CATEGORY_ID));
                    if(app.catID.equals(catId)){
                        result = 0;
                    }else{
                        result = db.update(AppInfoDatabaseHelper.TABLE_APP_NAME, values, selection, null);
                    }
                }
            }else{
                result = db.insert(AppInfoDatabaseHelper.TABLE_APP_NAME, null,
                        values);
            }
        } catch (Exception e) {
            Log.e(TAG, "addApp2DB app " + app, e);
        }finally{
            if(c != null){
                c.close();
                c = null;
            }
        }
        return (result < 0) ? false : true;
    }

    private boolean addCategory2DB(SQLiteDatabase db, CategoryInfo categoryInfo) {
        ContentValues values = new ContentValues();
        values.put(AppInfoDatabaseHelper.COLUMN_CATEGORY_ID,
                categoryInfo.catID);
        values.put(AppInfoDatabaseHelper.COLUMN_ORIGINAL_NAME,
                categoryInfo.catName);
        values.put(AppInfoDatabaseHelper.COLUMN_SHOW_NAME,
                categoryInfo.showName);
        values.put(AppInfoDatabaseHelper.COLUMN_SUPER_NAME,
                categoryInfo.supName);
        values.put(AppInfoDatabaseHelper.COLUMN_SUPER_ID,
                categoryInfo.supID);
        long result = -1;
        try {
            result = db.insert(AppInfoDatabaseHelper.TABLE_CAT_NAME, null,
                    values);
        } catch (Exception e) {
            Log.e(TAG, "addCategory2DB categoryInfo " + categoryInfo, e);
        }
        return (result < 0) ? false : true;
    }

    private boolean cleanAllData2DB(SQLiteDatabase db) {
        boolean result = true;
        try {
            db.execSQL("delete from " + AppInfoDatabaseHelper.TABLE_APP_NAME);
            db.execSQL("delete from " + AppInfoDatabaseHelper.TABLE_CAT_NAME);
        } catch (Exception e) {
            result = false;
            Log.e(TAG, "cleanAllData2DB error ", e);
        } finally {
        }
        return result;
    }

    public String getCatIdByPkg(String pkgName) {
        if(allAppInfoList.isEmpty() || TextUtils.isEmpty(pkgName)) {
            return null;
        }
        ArrayList<AppInfo> tempList = null;
        synchronized (objectLock) {
            tempList = new ArrayList<AppInfo>(allAppInfoList.size());
            tempList.addAll(allAppInfoList);
        }
        for(AppInfo info : tempList) {
            if(pkgName.equals(info.pkgName)) {
                return info.catID;
            }
        }
        return null;
    }

    public CategoryInfo getCatInfoByCatID(String catID) {
        if(allCategoryInfoList.isEmpty() || TextUtils.isEmpty(catID)) {
            return null;
        }
        ArrayList<CategoryInfo> tempList = null;
        synchronized (objectLock) {
            tempList = new ArrayList<CategoryInfo>(allCategoryInfoList.size());
            tempList.addAll(allCategoryInfoList);
        }
        for(CategoryInfo info : tempList) {
            if(catID.equals(info.catID)) {
                return info;
            }
        }
        return null;
    }

    public void reloadAppGroupInfosFromServer() {
        if (mIsLoading || sOff) {
            return;
        }
        long updateLatestTime = mSp.getLong(SP_UPDATE_LATEST_TIME, 0);
        long currentTime = System.currentTimeMillis();
        boolean isLoadedSuccess = isLoadedSuccess();

        if (!isLoadedSuccess || (currentTime - updateLatestTime > UPDATE_PERIOD_TIME
                || currentTime < updateLatestTime)) {
            LauncherModel.startLoadAppGroupInfo(DELAY_TIME_SHORT);
        }
    }

    private synchronized SQLiteDatabase openDatabase() {
        if(mDBOpenCounter.incrementAndGet() == 1) {
            try{
                mDatabase = mAppInfoInServerDB.getWritableDatabase();
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed at getWritableDatabase : " + e.getMessage());
                mDatabase = null;
            }
        }
        return mDatabase;
    }

    private synchronized void closeDatabase() {
        if(mDBOpenCounter.decrementAndGet() == 0) {
            if(null != mDatabase)
                mDatabase.close();
        }
    }

    public static void switchOff() {
        sOff = true;
    }

    public static void switchOn() {
        sOff = false;
    }

    public static boolean isSwitchOn() {
        return !sOff;
    }

}
