package com.aliyun.homeshell.test;

/*
 * 如果test方法在一个场景或一个模块下，方法名请按照如下命名规范：test+模块序号+“_”+编号+XXX
 * 例：test2_1ClickMenuTheme()
 * 这样可以保证一个场景或一个模块下的test方法可以按顺序一起执行，不被打乱。
 * test2 ---- Menu
 * test3 ---- Launcher.java
 * test4 ---- IconManager
 * test5 ---- search
 */
import java.util.ArrayList;
import java.util.List;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.icon.IconManager.IconCursorInfo;
import com.aliyun.homeshell.icon.IconUtils;
import com.aliyun.homeshell.test.utility.Utility;
import com.aliyun.homeshell.utils.Utils;

public class LauncherActivityTest extends
        ActivityInstrumentationTestCase2<Launcher> {
  //  private static final String TAG = "LauncherUnitTest";
    private static final String TAG = "vqx376";
    private Instrumentation mInstrumentation;
    private Launcher mLauncher;

    public LauncherActivityTest() {
        super(Launcher.class);
    }

    // 重写setUp方法，在该方法中进行相关的初始化操作
    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        /**
         * 如果需要发送key事件， 所以，必须在调用getActivity之前，调用下面的方法来关闭 touch模式，否则key事件会被忽略
         */
        // 关闭touch模式
        // setActivityInitialTouchMode(false);
        mInstrumentation = getInstrumentation();
        Intent intentLauncher = new Intent(Intent.ACTION_MAIN);
        intentLauncher.addCategory(Intent.CATEGORY_HOME);
        setActivityIntent(intentLauncher);
        // 获取被测试的Activity
        mLauncher = getActivity();
    }

    /**
     * 该测试用例实现在测试其他用例之前，测试确保获取的组件不为空
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test1PreConditions() {
        assertNotNull(mLauncher);
    }

    /**
     * 测试点击菜单键
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test2ClickMenu() {
        mInstrumentation.waitForIdleSync();
        // 调用sendKeys方法
        sendKeys(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT);
    }

    /**
     * 测试点击菜单item主题
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test2_1ClickMenuTheme() {
        sendKeys(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT);
        mInstrumentation.waitForIdleSync();
        View menu = mLauncher.getmMenu();
        assertNotNull(menu);
        final View theme = (View) menu.findViewById(
                R.id.menu_item_theme);
        assertNotNull(theme);
        mLauncher.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                theme.requestFocus();
                theme.performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(Utility.isRunningApp(mLauncher,
                "com.yunos.theme.thememanager"));
    }

    /**
     * 测试点击菜单item壁纸ֽ
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test2_2ClickMenuWallpaper() {
        sendKeys(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT);
        mInstrumentation.waitForIdleSync();
        View menu = mLauncher.getmMenu();
        assertNotNull(menu);
        final View wallpaper = (View) menu.findViewById(
                R.id.menu_item_wallpaper);
        assertNotNull(wallpaper);
        mLauncher.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                wallpaper.requestFocus();
                wallpaper.performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(Utility.isRunningApp(mLauncher,
                "com.yunos.theme.thememanager"));
    }

    /**
     * 测试点击菜单item设置
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test2_3ClickMenuEffect() {
        sendKeys(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT);
        mInstrumentation.waitForIdleSync();
        View menu = mLauncher.getmMenu();
        assertNotNull(menu);
        final View setting = (View) menu.findViewById(
                R.id.menu_item_effects);
        assertNotNull(setting);
        mLauncher.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                setting.requestFocus();
                setting.performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
        // assertTrue(Utility.isRunningApp(mLauncher, "com.android.settings"));
    }

    /**
     * 测试点击菜单item桌面设置
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test2_4ClickMenuHomeShellWidget() {
        sendKeys(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_LEFT);
        mInstrumentation.waitForIdleSync();
        View menu = mLauncher.getmMenu();
        assertNotNull(menu);
        final View homeshellsetting = (View) menu
                .findViewById(R.id.menu_item_widgets);
        assertNotNull(homeshellsetting);
        mLauncher.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                homeshellsetting.requestFocus();
                homeshellsetting.performClick();
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    //added by dongjun for testing Launcher.java begin
    public void test3_01MoveToDefaultScreen() {
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                launcher.moveToDefaultScreen(true);
                int screen = launcher.getCurrentScreen();
                assertEquals(screen, 0);
            }
        });
    }

    public void test3_02getCurrentSreen(){
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                int screen = launcher.getCurrentScreen();
                assertEquals(screen, 0);
            }
        });
    }

    //desktop has a folder is precondition
    //the function test : open folder and close folder
    public void test3_03OpenCloseFolder(){
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                FolderIcon folder = null;
                //open folder and check
                int screens = launcher.getWorkspace().getChildCount();
                top:for(int j = 0 ; j < screens;j++){
                    CellLayout layout = (CellLayout)launcher.getWorkspace().getChildAt(j);
                    int count = layout.getShortcutAndWidgetContainer().getChildCount();
                    for(int i = 0 ; i < count ; i++){
                        View child = layout.getShortcutAndWidgetContainer().getChildAt(i);
                        if(child instanceof FolderIcon){
                            folder = (FolderIcon)child;
                            break top;
                        }
                    }
                }

                if(folder != null){
                    launcher.openFolder(folder);
                    assertNotNull(launcher.getWorkspace().getOpenFolder());
                }else{
                    Log.w(TAG,"can't find folder on desktop");
                }

                //close folder and check
                launcher.closeFolder();
                assertNull(launcher.getWorkspace().getOpenFolder());
            }
        });
    }

    public void test3_04bindItems(){
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                //add a test icon on screen 0, cellX 0, cellY 2;
                ArrayList<ItemInfo> itemlist = createBindItemList();
                launcher.bindItems(itemlist, 0, itemlist.size());

                //check the icon
                CellLayout layout = (CellLayout)launcher.getWorkspace().getChildAt(0);
                BubbleTextView v =(BubbleTextView)layout.getChildAt(0, 2);
                assertEquals(v.getText(), "test");
            }
        });
    }

    public void test3_05bindItemsUpdated(){
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                //add a test icon on screen 0, cellX 0, cellY 2;
                ArrayList<ItemInfo> itemlist = createBindItemList();
                launcher.bindItems(itemlist, 0, itemlist.size());

                //check the icon
                CellLayout layout = (CellLayout)launcher.getWorkspace().getChildAt(0);
                BubbleTextView v =(BubbleTextView)layout.getChildAt(0, 2);
                assertEquals(v.getText(), "test");

                //update item
                createBindItemUpdateList(itemlist);
                launcher.bindItemsUpdated(itemlist);
                assertEquals(v.getText(), "test_upate");
            }
        });
    }

    public void test3_06bindItemsRemoved(){
        mInstrumentation.waitForIdleSync();
        final Launcher launcher = mLauncher;
        launcher.runOnUiThread(new Runnable(){
            @Override
            public void run() {
                //add a test icon on screen 0, cellX 0, cellY 2;
                ArrayList<ItemInfo> itemlist = createBindItemList();
                launcher.bindItems(itemlist, 0, itemlist.size());

                //check the icon
                CellLayout layout = (CellLayout)launcher.getWorkspace().getChildAt(0);
                BubbleTextView v =(BubbleTextView)layout.getChildAt(0, 2);
                assertEquals(v.getText(), "test");

                //remove test icon
                launcher.bindItemsRemoved(itemlist);

            }
        });

        mInstrumentation.waitForIdleSync();

        launcher.runOnUiThread(new Runnable(){
            @Override
            public void run() {
               //check remove result
                CellLayout layout = (CellLayout)launcher.getWorkspace().getChildAt(0);
                BubbleTextView v =(BubbleTextView)layout.getChildAt(0, 2);
                assertNull(v);
            }
        });
    }

    public void test3_07getCustomHideseat(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getCustomHideseat());
    }

    public void test3_08getDragController(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getDragController());
    }

    public void test3_09getDragLayer(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getDragLayer());
    }

    public void test3_10getGestureLayer(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getGestureLayer());
    }

    public void test3_11getHideseat(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getHideseat());
    }

    public void test3_12getHoteseat(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getHotseat());
    }

    public void test3_13getIconManager(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getIconManager());
    }

    public void test3_14getLifeCenterHost(){
        mInstrumentation.waitForIdleSync();
        View child = mLauncher.getWorkspace().getChildAt(0);
        //assertTrue(child instanceof LifeCenterCellLayout);
    }

    public void test3_15getModel(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getModel());
    }

    public void test3_16getWorkspace(){
        mInstrumentation.waitForIdleSync();
        assertNotNull(mLauncher.getWorkspace());
    }

    private ArrayList<ItemInfo> createBindItemList(){
        ArrayList<ItemInfo> itemlist = new ArrayList<ItemInfo>();
        ShortcutInfo info = new ShortcutInfo();
        info.intent = new Intent(Intent.ACTION_MAIN);
        info.intent.addCategory(Intent.CATEGORY_LAUNCHER);
        info.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        info.intent.setComponent(new ComponentName("com.test.test","test"));
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        info.screen = 0;
        info.cellX =  0;
        info.cellY =  2;
        info.title = "test";
        info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        itemlist.add(info);
        return itemlist;
    }

    private void createBindItemUpdateList(ArrayList<ItemInfo> list){
       for(ItemInfo info : list){
           info.title = "test_upate";
       }
    }
    //added by dongjun for testing Launcher.java end

    //added by dongjun for testing IconManage begin
    public void test4_1getAppCardBackgroud(){
        mInstrumentation.waitForIdleSync();
        List<ItemInfo> infolist = createItemInfos();
        assertNotNull(infolist);

        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        for(ItemInfo info:infolist){
            Drawable cardbg = manager.getAppCardBackgroud(info);
            assertNotNull(cardbg);
        }
    }

    public void test4_2getAppUnifiedIcon(){
        mInstrumentation.waitForIdleSync();
        List<ItemInfo> infolist = createItemInfos();
        assertNotNull(infolist);

        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        for(ItemInfo info:infolist){
            Drawable icon = manager.getAppUnifiedIcon(info, null);
            assertNotNull(icon);
        }
    }

    public void test4_3getAppUnifiedIcon(){
        mInstrumentation.waitForIdleSync();
        List<ItemInfo> infolist = createItemInfos();
        assertNotNull(infolist);

        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        for(ItemInfo info:infolist){
            Drawable icon = manager.getAppUnifiedIcon(((ShortcutInfo)info).intent);
            assertNotNull(icon);
        }
    }

    public void test4_4getTitleColor(){
        mInstrumentation.waitForIdleSync();
        List<ItemInfo> infolist = createItemInfos();
        assertNotNull(infolist);

        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        boolean result = false;
        for(ItemInfo info:infolist){
            int color =  manager.getTitleColor((ShortcutInfo)info);
            if(color == IconUtils.TITLE_COLOR_BLACK ||  color == IconUtils.TITLE_COLOR_WHITE){
                result = true;
            }
            assertTrue(result);
        }
    }

    public void test4_5buildHotSeatIcon(){
        mInstrumentation.waitForIdleSync();
        List<ItemInfo> infolist = createItemInfos();
        assertNotNull(infolist);

        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        for(ItemInfo info:infolist){
            Drawable icon = manager.getAppUnifiedIcon(((ShortcutInfo)info).intent);
            assertNotNull(icon);
            Drawable dockicon = manager.buildHotSeatIcon(icon);
            assertNotNull(dockicon);
        }
    }

    public void test4_6getIconFromCursor(){
        final IconManager manager = mLauncher.getIconManager();
        assertNotNull(manager);

        String key = LauncherSettings.Favorites.ICON_TYPE;
        ContentResolver cr = mLauncher.getContentResolver();
        Cursor cursor = cr.query(Favorites.CONTENT_URI,
                new String[] {Favorites._ID, Favorites.ICON,Favorites.TITLE},
                key + "=?",
                new String[] { String.valueOf(Favorites.ICON_TYPE_BITMAP) },
                null);
        if (cursor != null) {
            int iconIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
            while (cursor.moveToNext()) {
                IconCursorInfo cursorInfo = new IconCursorInfo(cursor,iconIndex);
                Drawable cursorIcon = (FastBitmapDrawable)manager.getIconFromCursor(cursorInfo);
                if (cursorIcon == null) {
                    cursorIcon = (FastBitmapDrawable)manager.getDefaultIcon();
                }
                assertNotNull(cursorIcon);
            }
            cursor.close();
        }
    }

    private List<ItemInfo> createItemInfos(){
        List<ItemInfo> iteminfos = new ArrayList<ItemInfo>();
        PackageManager pm = mLauncher.getPackageManager();
        List<ApplicationInfo> infos = pm.getInstalledApplications(0);
        for(ApplicationInfo appinfo:infos){
            ShortcutInfo info = new ShortcutInfo();
            info.intent = new Intent(Intent.ACTION_MAIN);
            info.intent.setComponent(new ComponentName(appinfo.packageName,appinfo.className != null?appinfo.className:"test"));
            info.intent.addCategory(Intent.CATEGORY_LAUNCHER);
            info.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
            iteminfos.add(info);
        }
        return iteminfos;
    }

    //added by dongjun for testing IconManage end

    /**
     * 测试下拉进入搜索
     * 
     * @author ruankong.rk
     * @param
     * @return
     */
    public void test5PulltoSearch() {
        Utility.execShellSwipeCmd(22, 300, 22, 500);
        if (Utils.isSupportSearch(mLauncher)) {
            assertTrue(Utility.isRunningApp(mLauncher,
                    "com.yunos.alimobilesearch"));
        }
    }
    @Override
    public Launcher getActivity() {
        // TODO Auto-generated method stub
        return super.getActivity();
    }

    @Override
    public void setActivityIntent(Intent i) {
        // TODO Auto-generated method stub
        super.setActivityIntent(i);
    }

    @Override
    public void setActivityInitialTouchMode(boolean initialTouchMode) {
        // TODO Auto-generated method stub
        super.setActivityInitialTouchMode(initialTouchMode);
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    @Override
    protected void runTest() throws Throwable {
        // TODO Auto-generated method stub
        super.runTest();
    }

}
