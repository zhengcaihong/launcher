package com.aliyun.homeshell;

import com.aliyun.homeshell.atom.AtomManager;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.utility.FeatureUtility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;

public final class ConfigManager {
    private static final String TAG = "ConfigManager";
    private static Context mContext;
    private static final String CONFIG_KEY = "com.aliyun.homeshell.config";

    // SharedPreferences keys
    private static final String EXIST_KEY = "exist";

    private static String CELL_COUNT_X_KEY = "cell_count_x";
    private static String CELL_COUNT_Y_KEY = "cell_count_y";
    private static String HOTSEAT_MAX_COUNT_X_KEY = "hotseat_max_count_x";
    private static String HOTSEAT_MAX_COUNT_Y_KEY = "hotseat_max_count_y";
    private static String HOTSEAT_ATOM_RANK = "hotseat_atom_rank";
    private static String CELL_MAX_COUNT_X_KEY = "cell_max_count_x";
    private static String CELL_MAX_COUNT_Y_KEY = "cell_max_count_y";
    private static final String SCREEN_MAX_COUNT_KEY = "screen_max_count";
    private static final String FOLDER_MAX_COUNT_X_KEY = "folder_max_count_x";
    private static final String FOLDER_MAX_COUNT_Y_KEY = "folder_max_count_y";
    private static final String HOTSEAT_MAX_COUNT_KEY = "hotseat_max_count";
    private static final String HIDESEAT_MAX_SCREEN_COUNT_KEY = "hideseat_max_screen_count";
    private static final String HIDESEAT_MAX_COUNT_X_KEY = "hideseat_max_count_x";
    private static final String HIDESEAT_MAX_COUNT_Y_KEY = "hideseat_max_count_y";

    // Screen
    public static final int DEFAULT_HOME_SCREEN_INDEX = 0;
    public static final int DEFAULT_FIND_EMPTY_SCREEN_START = 1;
    public static final int DEFAULT_CELL_COUNT_X = 4;
    public static final int DEFAULT_CELL_COUNT_Y = 4;
    private static final int DEFAULT_SCREEN_MAX_COUNT = 12;

    public static final int DEFAULT_CELL_MAX_COUNT_X = 4;
    public static final int DEFAULT_CELL_MAX_COUNT_Y = 4;

    // Folder
    private static final int DEFAULT_FOLDER_MAX_COUNT_X = 3;
    private static final int DEFAULT_FOLDER_MAX_COUNT_Y = 3;
    private static final int CARD_FOLDER_MAX_COUNT_Y = 3;
        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
    private static final int FOLDER_MAX_COUNT_Y_AGED_MODE = 3;
        /* YUNSO END */
    private static final int DEFAULT_FOLDER_ITEMS_MAX_COUNT = DEFAULT_FOLDER_MAX_COUNT_X * DEFAULT_FOLDER_MAX_COUNT_Y;
//    private static final int CARD_FOLER_ITEMS_MAX_COUNT = DEFAULT_FOLDER_MAX_COUNT_X
//            * CARD_FOLDER_MAX_COUNT_Y;
    // Hotseat
    public static final int DEFAULT_HOTSEAT_MAX_COUNT_X = 4;
    private static final int DEFAULT_HOTSEAT_MAX_COUNT_Y = 1;
    private static final int DEFAULT_HOTSEAT_MAX_COUNT = DEFAULT_HOTSEAT_MAX_COUNT_X * DEFAULT_HOTSEAT_MAX_COUNT_Y;

    // Hideseat
    /* YUNOS BEGIN */
    // ##date:2015/6/15 ##author:zhanggong.zg ##BugID:6078201
    // Hide-seat icon number increased from 24 to 36.
    // Note that some hard-coded numbers should be also modified at same time when
    // this value changed. (search for "HIDESEAT_SCREEN_NUM_MARKER" in the entire project)
    public static final int DEFAULT_HIDESEAT_SCREEN_MAX_COUNT = 9;
    public static final int DEFAULT_HIDESEAT_MAX_COUNT_X = 4;
    public static final int DEFAULT_HIDESEAT_MAX_COUNT_Y = 1;
    private static final int DEFAULT_HIDESEAT_ITEMS_MAX_COUNT = DEFAULT_HIDESEAT_SCREEN_MAX_COUNT * DEFAULT_HIDESEAT_MAX_COUNT_X * DEFAULT_HIDESEAT_MAX_COUNT_Y;
    /* YUNOS END */
    private static final int DEFAULT_HOTSEAT_ATOM_RANK = -1;

    // Screen
    private static int sScreenMaxCount = DEFAULT_SCREEN_MAX_COUNT;
    private static int sCellCountX = DEFAULT_CELL_COUNT_X;
    private static int sCellCountY = DEFAULT_CELL_COUNT_Y;
    private static int sCellMaxCountX = DEFAULT_CELL_MAX_COUNT_X;
    private static int sCellMaxCountY = DEFAULT_CELL_MAX_COUNT_Y;
    // Folder
    private static int sFolderMaxCountX = DEFAULT_FOLDER_MAX_COUNT_X;
    private static int sFolderMaxCountY = DEFAULT_FOLDER_MAX_COUNT_Y;
    private static int sFolderItemsMaxCount = DEFAULT_FOLDER_ITEMS_MAX_COUNT;
    // Hotseat
    private static int sHotseatMaxCountX = DEFAULT_HOTSEAT_MAX_COUNT_X;
    private static int sHotseatMaxCountY = DEFAULT_HOTSEAT_MAX_COUNT_Y;
    private static int sHotseatMaxCount = DEFAULT_HOTSEAT_MAX_COUNT;
    private static int sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
    // Hideseat
    private static int sHideseatScreenMaxCount = DEFAULT_HIDESEAT_SCREEN_MAX_COUNT;
    private static int sHideseatMaxCountX = DEFAULT_HIDESEAT_MAX_COUNT_X;
    private static int sHideseatMaxCountY = DEFAULT_HIDESEAT_MAX_COUNT_Y;
    private static int sHideseatItemsMaxCount = DEFAULT_HIDESEAT_ITEMS_MAX_COUNT;

    private static boolean isLandscapeSupport;

    private ConfigManager(){
    }

    public static void init() {
        isLandscapeSupport = FeatureUtility.isLandscapeSupport();

        mContext = LauncherApplication.getContext();
        // load the params neednot from sharedprefer
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        //BugID:111674
        if (sp.contains(EXIST_KEY) == false) {
            getAndSaveDefaultConfig(sp);
            Log.d(TAG, "init(): Save config complete.");
        } else {
            getAllConfigs(sp);
            Log.d(TAG, "init(): Get config complete.");
        }
        if (!ConfigManager.checkLayoutDataValid(mContext)) {
            Log.d(TAG, " init(): config data invalid !!!   reset all config data");
            getAndSaveDefaultConfig(sp);
        }
        Log.d(TAG, "sxsexe_test     init(): " + toLogString());
    }

    private static void getAndSaveDefaultConfig(SharedPreferences sharedPreferences) {
        // Config shared preference xml isn't created, get default shared preferences from default config.xml,
        // and create config shared preference xml.
        // Move the configurations from res/xml/default_config.xml to res/values/config.xml ##BugID: 110407
        // Screen
        Resources res = mContext.getResources();
        sScreenMaxCount = res.getInteger(R.integer.screen_max_count);
        sCellCountX = res.getInteger(R.integer.cell_count_x);
        sCellCountY = res.getInteger(R.integer.cell_count_y);
        sCellMaxCountX = res.getInteger(R.integer.cell_max_count_x);
        sCellMaxCountY = res.getInteger(R.integer.cell_max_count_y);
        // Folder
        sFolderMaxCountX = res.getInteger(R.integer.folder_max_count_x);
        sFolderMaxCountY = res.getInteger(R.integer.folder_max_count_y);
        sFolderItemsMaxCount = sFolderMaxCountX * sFolderMaxCountY;

        // Hideseat Hotseat
        if (AgedModeUtil.isAgedMode()) {
            sHotseatMaxCountX = res.getInteger(
                    R.integer.hotseat_max_count_x_aged_mode);
            /* YUNOS BEGIN */
            //## modules(Home Shell)
            //## date: 2016/03/15 ## author: wangye.wy
            //## BugID: 7762285: remove atom of xiao yun in hotseat
        /*
            View atom = AtomManager.getAtomManager().getRootView(mContext);
            if (atom != null) {
                sHotSeatAtomRank = res.getInteger(R.integer.hotseat_atom_rank_aged_mode);
            } else {
                sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
            }
        */
            sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
            /* YUNOS END */
            sHideseatScreenMaxCount = res.getInteger(R.integer.hideseat_screen_max_count_3_3);
            sHideseatMaxCountX = res.getInteger(
                    R.integer.hideseat_max_count_x_3_3);
        } else {
            sHotseatMaxCountX = res.getInteger(R.integer.hotseat_max_count_x);
            /* YUNOS BEGIN */
            //## modules(Home Shell)
            //## date: 2016/03/15 ## author: wangye.wy
            //## BugID: 7762285: remove atom of xiao yun in hotseat
        /*
            View atom = AtomManager.getAtomManager().getRootView(mContext);
            if (atom != null) {
                sHotSeatAtomRank = res.getInteger(R.integer.hotseat_atom_rank);
            } else {
                sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
            }
        */
            sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
            /* YUNOS END */
            sHideseatScreenMaxCount = res.getInteger(R.integer.hideseat_screen_max_count);
            sHideseatMaxCountX = res.getInteger(R.integer.hideseat_max_count_x);
        }
        sHotseatMaxCountY = res.getInteger(R.integer.hotseat_max_count_y);
        sHotseatMaxCount = sHotseatMaxCountX * sHotseatMaxCountY;

        sHideseatMaxCountY = res.getInteger(R.integer.hideseat_max_count_y);
        sHideseatItemsMaxCount = sHideseatScreenMaxCount * sHideseatMaxCountX * sHideseatMaxCountY;
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(EXIST_KEY, true);
        // Screen
        editor.putInt(SCREEN_MAX_COUNT_KEY, sScreenMaxCount);
        editor.putInt(CELL_COUNT_X_KEY, sCellCountX);
        editor.putInt(CELL_COUNT_Y_KEY, sCellCountY);
        editor.putInt(CELL_MAX_COUNT_X_KEY, sCellMaxCountX);
        editor.putInt(CELL_MAX_COUNT_Y_KEY, sCellMaxCountY);
        // Folder
        editor.putInt(FOLDER_MAX_COUNT_X_KEY, sFolderMaxCountX);
        editor.putInt(FOLDER_MAX_COUNT_Y_KEY, sFolderMaxCountY);
        // Hotseat
        editor.putInt(HOTSEAT_MAX_COUNT_X_KEY, sHotseatMaxCountX);
        editor.putInt(HOTSEAT_MAX_COUNT_Y_KEY, sHotseatMaxCountY);
        editor.putInt(HOTSEAT_ATOM_RANK, sHotSeatAtomRank);
        // Hideseat
        editor.putInt(HIDESEAT_MAX_SCREEN_COUNT_KEY, sHideseatScreenMaxCount);
        editor.putInt(HIDESEAT_MAX_COUNT_X_KEY, sHideseatMaxCountX);
        editor.putInt(HIDESEAT_MAX_COUNT_Y_KEY, sHideseatMaxCountY);
        editor.apply();
    }

    private static void getAllConfigs(SharedPreferences sharedPreferences) {
        Resources res = mContext.getResources();
        // Screen
        sScreenMaxCount = sharedPreferences.getInt(SCREEN_MAX_COUNT_KEY, DEFAULT_SCREEN_MAX_COUNT);
        sCellCountX = sharedPreferences.getInt(CELL_COUNT_X_KEY, DEFAULT_CELL_COUNT_X);
        sCellCountY = sharedPreferences.getInt(CELL_COUNT_Y_KEY, DEFAULT_CELL_COUNT_Y);
        sCellMaxCountX = sharedPreferences.getInt(CELL_MAX_COUNT_X_KEY, DEFAULT_CELL_MAX_COUNT_X);
        sCellMaxCountY = sharedPreferences.getInt(CELL_MAX_COUNT_Y_KEY, DEFAULT_CELL_MAX_COUNT_Y);

        // Folder
        //BugID:134833:get sFolderMaxCountX and sFolderMaxCountY from xml
        //sFolderMaxCountX = sharedPreferences.getInt(FOLDER_MAX_COUNT_X_KEY, DEFAULT_FOLDER_MAX_COUNT_X);
        //sFolderMaxCountY = sharedPreferences.getInt(FOLDER_MAX_COUNT_Y_KEY, DEFAULT_FOLDER_MAX_COUNT_Y);
        sFolderMaxCountX = res.getInteger(R.integer.folder_max_count_x);
        sFolderMaxCountY = res.getInteger(R.integer.folder_max_count_y);
        sFolderItemsMaxCount = sFolderMaxCountX * sFolderMaxCountY;
        // Hotseat
        sHotseatMaxCountX = sharedPreferences.getInt(HOTSEAT_MAX_COUNT_X_KEY, DEFAULT_HOTSEAT_MAX_COUNT_X);
        sHotseatMaxCountY = sharedPreferences.getInt(HOTSEAT_MAX_COUNT_Y_KEY, DEFAULT_HOTSEAT_MAX_COUNT_Y);
        sHotseatMaxCount = sHotseatMaxCountX * sHotseatMaxCountY;
        // Hideseat
        // BugID:5244146: HideseatScreenMaxCount was changed from 3 to 6
        //sHideseatScreenMaxCount = sharedPreferences.getInt(HIDESEAT_MAX_SCREEN_COUNT_KEY, DEFAULT_HIDESEAT_SCREEN_MAX_COUNT);
        if (AgedModeUtil.isAgedMode()) {
            sHideseatScreenMaxCount = res.getInteger(R.integer.hideseat_screen_max_count_3_3);
            sHideseatMaxCountX = res.getInteger(R.integer.hideseat_max_count_x_3_3);
        } else {
            sHideseatScreenMaxCount = res.getInteger(R.integer.hideseat_screen_max_count);
            sHideseatMaxCountX = res.getInteger(R.integer.hideseat_max_count_x);
        }
        // sHideseatMaxCountX =
        // sharedPreferences.getInt(HIDESEAT_MAX_COUNT_X_KEY,
        // DEFAULT_HIDESEAT_MAX_COUNT_X);
        sHideseatMaxCountY = sharedPreferences.getInt(HIDESEAT_MAX_COUNT_Y_KEY, DEFAULT_HIDESEAT_MAX_COUNT_Y);
        sHideseatItemsMaxCount = sHideseatScreenMaxCount * sHideseatMaxCountX * sHideseatMaxCountY;
        /* YUNOS BEGIN */
        //## modules(Home Shell)
        //## date: 2016/03/15 ## author: wangye.wy
        //## BugID: 7762285: remove atom of xiao yun in hotseat
    /*
        View atom = AtomManager.getAtomManager().getRootView(mContext);
        if (atom != null) {
            sHotSeatAtomRank = res.getInteger(R.integer.hotseat_atom_rank);
        } else {
            sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
        }
    */
        sHotSeatAtomRank = DEFAULT_HOTSEAT_ATOM_RANK;
        /* YUNOS END */
    }

    // Screen
    public static int getIconScreenMaxCount() {
            return sScreenMaxCount;
    }

    public static void setIconScreenMaxCount(int screenMaxCount) {
        sScreenMaxCount = screenMaxCount;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(SCREEN_MAX_COUNT_KEY, sScreenMaxCount).commit();
    }

    public static int getCellCountX() {
        return sCellCountX;
    }

    public static void setCellCountX(int cellCountX) {
        sCellCountX = cellCountX;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(CELL_COUNT_X_KEY, sCellCountX).commit();
    }

    public static int getCellCountY() {
        return sCellCountY;
    }

    public static void setCellCountY(int cellCountY) {
        sCellCountY = cellCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(CELL_COUNT_Y_KEY, sCellCountY).commit();
    }

    /* YUNOS BEGIN */
    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Modified to support pad orientation
    public static int getCellCountX(int orientation) {
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                              Math.max(sCellCountX, sCellCountY) :
                              Math.min(sCellCountX, sCellCountY);
    }

    public static int getCellCountY(int orientation) {
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                              Math.min(sCellCountX, sCellCountY) :
                              Math.max(sCellCountX, sCellCountY);
    }
    /* YUNOS END */

    public static int getCellMaxCountX() {
        return sCellMaxCountX;
    }

    public static void setCellMaxCountX(int cellMaxCountX) {
        sCellMaxCountX = cellMaxCountX;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(CELL_MAX_COUNT_X_KEY, sCellMaxCountX).commit();
    }

    public static int getCellMaxCountY() {
        return sCellMaxCountY;
    }

    public static void setCellMaxCountY(int cellMaxCountY) {
        sCellMaxCountY = cellMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(CELL_MAX_COUNT_Y_KEY, sCellMaxCountY).commit();
    }

    // Folder
    public static int getFolderMaxCountX() {
        return sFolderMaxCountX;
    }

    public static void setFolderMaxCountX(int folderMaxCountX) {
        sFolderMaxCountX = folderMaxCountX;
        sFolderItemsMaxCount = sFolderMaxCountX * sFolderMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(FOLDER_MAX_COUNT_X_KEY, sFolderMaxCountX).commit();
    }

    public static int getFolderMaxCountY() {
        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        IconManager im = ((LauncherApplication) LauncherApplication.getContext()).getIconManager();
        if (im == null) {
            im = new IconManager(mContext);
        }
        if (AgedModeUtil.isAgedMode()) {
            return FOLDER_MAX_COUNT_Y_AGED_MODE;
        } else if (im.supprtCardIcon()) {
            return CARD_FOLDER_MAX_COUNT_Y;
        } else {
            return sFolderMaxCountY;
        }
        /* YUNSO END */
    }

    public static void setFolderMaxCountY(int FolderMaxCountY) {
        sFolderMaxCountY = FolderMaxCountY;
        sFolderItemsMaxCount = sFolderMaxCountX * sFolderMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(FOLDER_MAX_COUNT_Y_KEY, sFolderMaxCountY).commit();
    }

    public static int getFolderMaxItemsCount() {
        return sFolderItemsMaxCount;
    }

    // Hotseat
    public static int getHotseatMaxCountX() {
        return sHotseatMaxCountX;
    }

    public static int getHotseatAtomRank() {
        return sHotSeatAtomRank;
    }

    public static void setHotseatAtomRank(int hotseatAtomRank) {
        sHotSeatAtomRank = hotseatAtomRank;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_ATOM_RANK, sHotSeatAtomRank).commit();
    }

    public static void setHotseatMaxCountX(int hotseatMaxCountX) {
        sHotseatMaxCountX = hotseatMaxCountX;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_MAX_COUNT_X_KEY, sHotseatMaxCountX).commit();
        setHotseatMaxCount(sHotseatMaxCountX * sHotseatMaxCountY);
    }

    public static void setHotseatMaxCountX(Context context, int hotseatMaxCountX) {
        if (context == null) {
            return;
        }
        sHotseatMaxCountX = hotseatMaxCountX;
        SharedPreferences sp = context.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_MAX_COUNT_X_KEY, sHotseatMaxCountX).commit();
        setHotseatMaxCount(context, sHotseatMaxCountX * sHotseatMaxCountY);
    }

    public static int getHotseatMaxCountY() {
        return sHotseatMaxCountY;
    }

    public static void setHotseatMaxCountY(int hotseatMaxCountY) {
        sHotseatMaxCountY = hotseatMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_MAX_COUNT_Y_KEY, sHotseatMaxCountY).commit();
        setHotseatMaxCount(sHotseatMaxCountX * sHotseatMaxCountY);
    }

    public static int getHotseatMaxCount() {
        return sHotseatMaxCount;
    }

    public static void setHotseatMaxCount(int hotseatMaxCount) {
        sHotseatMaxCount = hotseatMaxCount;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_MAX_COUNT_KEY, sHotseatMaxCount).commit();
    }

    public static void setHotseatMaxCount(Context context, int hotseatMaxCount) {
        if (context == null) {
            return;
        }
        sHotseatMaxCount = hotseatMaxCount;
        SharedPreferences sp = context.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HOTSEAT_MAX_COUNT_KEY, sHotseatMaxCount).commit();
    }

    // Hideseat
    public static int getHideseatScreenMaxCount() {
        return sHideseatScreenMaxCount;
    }

    public static void setHideseatScreenMaxCount(int hideseatScreenMaxCount) {
        sHideseatScreenMaxCount = hideseatScreenMaxCount;
        sHideseatItemsMaxCount = sHideseatScreenMaxCount * sHideseatMaxCountX * sHideseatMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HIDESEAT_MAX_SCREEN_COUNT_KEY, sHideseatScreenMaxCount).commit();
    }

    public static int getHideseatMaxCountX() {
        return sHideseatMaxCountX;
    }

    public static void setHideseatMaxCountX(int hideseatMaxCountX) {
        sHideseatMaxCountX = hideseatMaxCountX;
        sHideseatItemsMaxCount = sHideseatScreenMaxCount * sHideseatMaxCountX * sHideseatMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HIDESEAT_MAX_COUNT_X_KEY, sHideseatMaxCountX).commit();
    }

    public static int getHideseatMaxCountY() {
        return sHideseatMaxCountY;
    }

    public static void setHideseatMaxCountY(int hideseatMaxCountY) {
        sHideseatMaxCountY = hideseatMaxCountY;
        sHideseatItemsMaxCount = sHideseatScreenMaxCount * sHideseatMaxCountX * sHideseatMaxCountY;
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        sp.edit().putInt(HIDESEAT_MAX_COUNT_Y_KEY, sHideseatMaxCountY).commit();
    }

    public static int getHideseatItemsMaxCount() {
        return sHideseatItemsMaxCount;
    }

    public static void adjustToThreeLayout() {
        setHideseatScreenMaxCount(mContext.getResources()
                .getInteger(R.integer.hideseat_screen_max_count_3_3));
        setHideseatMaxCountX(mContext.getResources()
                .getInteger(R.integer.hideseat_max_count_x_3_3));
        setHotseatMaxCountX(mContext.getResources()
                .getInteger(R.integer.hotseat_max_count_x_aged_mode));
        setHotseatAtomRank(mContext.getResources()
                .getInteger(R.integer.hotseat_atom_rank_aged_mode));
    }

    public static void adjustFromThreeLayout() {
        setHideseatScreenMaxCount(mContext.getResources()
                .getInteger(R.integer.hideseat_screen_max_count));
        setHideseatMaxCountX(mContext.getResources()
                .getInteger(R.integer.hideseat_max_count_x));
        setHotseatMaxCountX(mContext.getResources()
                .getInteger(R.integer.hotseat_max_count_x));
        setHotseatAtomRank(mContext.getResources()
                .getInteger(R.integer.hotseat_atom_rank));
    }

    public static boolean checkLayoutDataValid(Context context) {
        if (ConfigManager.isLandOrienSupport()) {
            int currentOrientation = context.getResources().getConfiguration().orientation;
            switch(currentOrientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (sCellCountX != 6 || sCellCountY != 4) {
                        return false;
                    }
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    if (sCellCountX != 4 || sCellCountY != 6) {
                        return false;
                    }
                    break;
                default :
                    return true;
            }
        }

        return true;
    }

    public static boolean checkAgedModeDataValid(Context context) {
        if(!FeatureUtility.hasAgedModeFeature()){
            return false;
        }
        boolean dataInvalid = false;
        Resources res = context.getResources();
        int hotseatCountAgedMode = res.getInteger(R.integer.hotseat_max_count_x_aged_mode)
                * res.getInteger(R.integer.hotseat_max_count_y);
        int hidesetCountXAgedMode = res.getInteger(R.integer.hideseat_max_count_x_3_3);

        if (AgedModeUtil.isAgedMode()) {
            if (getCellCountX() != 3 || getCellCountY() != 3 || getHotseatMaxCount() != hotseatCountAgedMode
                    || getHideseatMaxCountX() != hidesetCountXAgedMode) {
                Log.d(AgedModeUtil.TAG, "AgedModeUtil is agedMode while UI not agedMode, call onAgedModeChanged:true");
                dataInvalid = true;
            }
        } else {
            if (getCellCountX() == 3 || getCellCountY() == 3 || getHotseatMaxCount() == hotseatCountAgedMode
                    || getHideseatMaxCountX() == hidesetCountXAgedMode) {
                Log.d(AgedModeUtil.TAG,
                        "AgedModeUtil is not agedMode while UI is agedMode,so call onAgedModeChanged:false");
                dataInvalid = true;
            }
        }
        return dataInvalid;
    }

    public static String toLogString() {
        return "ConfigManager [sScreenMaxCount=" + sScreenMaxCount
                        + ", sHotseatMaxCount=" + sHotseatMaxCount + ", sCellCountX=" + sCellCountX
                        + ", sCellCountY=" + sCellCountY + ", sHideseatScreenMaxCount=" + sHideseatScreenMaxCount
                        + ", sHideseatMaxCountX=" + sHideseatMaxCountX + ", sHideseatMaxCountY=" + sHideseatMaxCountY
                        + ", sHideseatItemsMaxCount=" + sHideseatItemsMaxCount + ", sFolderMaxCountX=" + sFolderMaxCountX
                        + ", sFolderMaxCountY=" + sFolderMaxCountY + ", sFolderItemsMaxCount=" + sFolderItemsMaxCount
                        + ", sCellMaxCountX=" + sCellMaxCountX + ", sCellMaxCountY=" + sCellMaxCountX + "]";
    }
  //the data stored in sharedpref is port mode data
    public static void reCreateConfigDataOnOrientationChanged() {
        SharedPreferences sp = mContext.getSharedPreferences(CONFIG_KEY, Context.MODE_PRIVATE);
        getAndSaveDefaultConfig(sp);
    }

    public static boolean isLandOrienSupport() {
        return isLandscapeSupport;
    }

    /* YUNOS BEGIN */
    // ##date:2015/2/28 ##author:zhanggong.zg ##BugID:AF1-366
    // Disable gadget card on pad.
    public static boolean isGadgetCardSupported() {
        return isLandOrienSupport() == false;
    }

    // Disable hide-seat on pad.
    public static boolean isHideseatSupported() {
        return isLandOrienSupport() == false;
    }
    /* YUNOS END */

}
