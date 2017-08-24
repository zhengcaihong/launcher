package com.aliyun.homeshell.test;

import java.util.ArrayList;

import com.aliyun.homeshell.CellLayout;
import com.aliyun.homeshell.DragLayer;
import com.aliyun.homeshell.FolderIcon;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.Workspace;
import com.aliyun.homeshell.editmode.HorizontalListView;
import com.aliyun.homeshell.editmode.PreviewContainer;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.icon.BubbleTextView;
import com.aliyun.homeshell.screenmanager.ScreenCardView;
import com.aliyun.homeshell.screenmanager.ScreenManagerView;
import com.aliyun.homeshell.test.utility.Utility;

import android.app.Instrumentation;
import android.appwidget.AppWidgetHostView;
import android.content.Intent;
import android.os.SystemClock;
import android.test.SingleLaunchActivityTestCase;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.widget.Toast;
import app.aliyun.v3.gadget.GadgetView;

public class LauncherActivityEditModeTest extends
        SingleLaunchActivityTestCase<Launcher> {

    private static final int FLAG_TYPE_APP_FOLDER = 1;
    private static final int FLAG_TYPE_WIDGET_GADGET = 2;
    private Launcher mLauncher = null;
    private Instrumentation mInstrumentation = null;
    private int mScreenWidth = 480;
    private int mScreenHeight = 854;
    PreviewContainer mEditModePreviewContainer;
    HorizontalListView mPreviewList;
    Workspace mWorkspace;

    public LauncherActivityEditModeTest(){
        super("com.aliyun.homeshell",Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        mInstrumentation = getInstrumentation();
        Intent intentLauncher = new Intent(Intent.ACTION_MAIN);
        intentLauncher.addCategory(Intent.CATEGORY_HOME);
        //super.
        //setActivityIntent(intentLauncher);
        // ��ȡ�����Ե�Activity
        mLauncher = getActivity();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        DisplayMetrics metric = new DisplayMetrics();
        mLauncher.getWindowManager().getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels;
        mScreenHeight = metric.heightPixels;
        mEditModePreviewContainer = (PreviewContainer)mLauncher.findViewById(R.id.editmode_container);
        mPreviewList = (HorizontalListView) mEditModePreviewContainer.findViewById(R.id.preview_list);
        mWorkspace = mLauncher.getWorkspace();
    }

    @Override
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }
    /**
     * 屏幕编辑检查
     */
    public void test00_01ScreenEditCheck(){
        Log.i(Utility.UTILITY_TAG,"test0ScreenEditCheck,00-001屏幕编辑检查开始");
        int[] location = new int[2];
        int maxtry = 12;
        //step 1-4
        int screenCnt = mLauncher.getWorkspace().getChildCount();
        for(int i=0;i<screenCnt;i++){
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,1000);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,1000);
            this.snapToNextPage();
            Toast.makeText(mLauncher, "testCase00-001屏幕编辑检查", Toast.LENGTH_LONG).show();
        }
        //step 5-8
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,1000);
        maxtry = 12;
        while(maxtry-- >= 0 && mLauncher.getWorkspace().getChildCount() <= 12){
            snapToLastPage();
            CellLayout layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
            if(!layout.isFakeChild()){
                break;
            }
            assertEquals(layout.getChildCount(),2);
            View child = layout.getChildAt(1);
            View btnAdd = child.findViewById(R.id.add_btn);
            btnAdd.getLocationOnScreen(location);
            Utility.execShellTapCmd((location[0])+btnAdd.getMeasuredWidth()/2, location[1]+btnAdd.getMeasuredHeight()/2);
            Utility.sleep(1000);
        }
        snapToLastPage();
        Utility.sleep(1000);
        snapToPrevPage();
        snapToPrevPage();
        CellLayout layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
        assertEquals(layout.getChildCount(),2);
        View child = layout.getChildAt(1);
        View btnDel = child.findViewById(R.id.delete_btn);
        btnDel.getLocationOnScreen(location);
        Utility.execShellTapCmd((location[0]+btnDel.getMeasuredWidth()/2), location[1]+btnDel.getMeasuredHeight()/2);
        Utility.sleep(1000);
        snapToLastPage();
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,1000);

//        //步骤9-10
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,2000);
        Utility.execShellKeyEventLongPress(KeyEvent.KEYCODE_HOME,2000);//长按home键需要系统权限，这里获取不到系统权限，所以长按home键没有起到作用
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);

//        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME);
//        long downTime = event.getDownTime();
//        long eventTime = event.getEventTime();
//        int metaState = event.getMetaState();
//        int deviceId = 4;
//        int scancode = 139;
//        int source = InputDevice.SOURCE_KEYBOARD;
//        int flags = KeyEvent.FLAG_VIRTUAL_HARD_KEY | KeyEvent.FLAG_FROM_SYSTEM;
//        int code = KeyEvent.KEYCODE_MENU;
//        downTime = SystemClock.uptimeMillis();
//        eventTime = SystemClock.uptimeMillis();
//        flags |= KeyEvent.FLAG_LONG_PRESS;
//
//        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MENU));
//        Utility.sleep(ViewConfiguration.getLongPressTimeout());
//        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MENU));
//        KeyEvent newEvent = new KeyEvent(downTime, eventTime, KeyEvent.ACTION_DOWN, code, 0, metaState,
//                deviceId, scancode, flags, source);
//        mInstrumentation.sendKeySync(newEvent);
//        Utility.sleep(500);
//
//        flags |= KeyEvent.FLAG_LONG_PRESS;
//        eventTime = SystemClock.uptimeMillis();
//        newEvent = new KeyEvent(downTime, eventTime, KeyEvent.ACTION_DOWN, code, 1, metaState,
//                deviceId, scancode, flags, source);
//        mInstrumentation.sendKeySync(newEvent);
//
//        flags &= ~KeyEvent.FLAG_LONG_PRESS;
//        newEvent = new KeyEvent(downTime, eventTime, KeyEvent.ACTION_DOWN, code, 2, metaState,
//                deviceId, scancode, flags, source);
//        mInstrumentation.sendKeySync(newEvent);
//
//        newEvent = new KeyEvent(downTime, eventTime, KeyEvent.ACTION_DOWN, code, 3, metaState,
//                deviceId, scancode, flags, source);
//        mInstrumentation.sendKeySync(newEvent);
//
//        eventTime = SystemClock.uptimeMillis();
//        //flags = KeyEvent.FLAG_CANCELED;
//        newEvent = new KeyEvent(downTime, eventTime, KeyEvent.ACTION_UP, code, 0, metaState,
//                deviceId, scancode, flags, source);
//        mInstrumentation.sendKeySync(newEvent);

        Utility.sleep(3000);

//        CellLayout layout = (CellLayout)mLauncher.getWorkspace().getChildAt(0);
//        int cnt = layout.getShortcutAndWidgetContainer().getChildCount();
//        View firstChild = null;
//        View lastChild = null;
//        if(cnt > 0){
//            firstChild = layout.getShortcutAndWidgetContainer().getChildAt(0);
//            lastChild = layout.getShortcutAndWidgetContainer().getChildAt(cnt-1);
//        }
//        final int [] start = new int[2];
//        final int [] stop = new int[2];
//        firstChild.getLocationOnScreen(start);
//        lastChild.getLocationOnScreen(stop);
//        long downTime = SystemClock.uptimeMillis();
//        long eventTime = SystemClock.uptimeMillis();
//        Log.i("chenjian","normalMode,startX="+(start[0]) + ",startY="+(start[1]));
//        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, start[0]+20f, start[1]+20f, 0);
//        mInstrumentation.sendPointerSync(event);
//        Log.i("chenjian","normalMode,startX="+(start[0]) + ",startY="+(start[1]));
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e2) {
//            // TODO Auto-generated catch block
//            e2.printStackTrace();
//        }
//        Utility.execShellSwipeCmd(start[0]+20, start[1]+20, stop[0]+40, stop[1]+40);
//        eventTime = SystemClock.uptimeMillis();
//        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, stop[0]+40f, stop[1]+40f, 0);
//        mInstrumentation.sendPointerSync(event);
//        Log.i("chenjian","up,stopX="+(stop[0]+20f) + ",stopY="+(stop[1]+20f));
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        sendKeys(KeyEvent.KEYCODE_MENU);
//        sendKeys(KeyEvent.ACTION_DOWN);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        firstChild.getLocationOnScreen(start);
//        Log.i("chenjian","EditMode,x="+start[0]+",y="+start[1]);
        Log.i(Utility.UTILITY_TAG,"test0ScreenEditCheck,00-001屏幕编辑检查结束");
    }

    /**
     * 桌面整理-单个应用图标
     */
    public void test00_02SingleIconArrange(){
        Log.i(Utility.UTILITY_TAG,"test00_02SingleIconArrange,00-002桌面整理-单个应用图标开始");

        //步骤1-2
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU,2000);
        CellLayout layout;
        View target = null;
        View dropTarget = null;
        View[] views = new View[2];
        int maxTry = 12;
        int index;
        long downTime;
        boolean bNext = true;
        while(!SelectTwoRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()), views)
                && (maxTry-- > 0) ){
            if(bNext)
                snapToNextPage();
            else
                snapToPrevPage();
            if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
                bNext = false;
            }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
                bNext = true;
            }
        }
        int[] start = new int[2];
        int[] middle = new int[2];
        int[] stop = new int[2];
        target = views[0];
        dropTarget = views[1];
        if(target != null && dropTarget != null){
            target.getLocationOnScreen(start);
            dropTarget.getLocationOnScreen(stop);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            stop[0] += dropTarget.getMeasuredWidth()/2;
            stop[1] += dropTarget.getMeasuredHeight()/2;
            layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
            layout.getLocationOnScreen(middle);
            middle[0] += target.getMeasuredWidth();
            middle[1] += target.getHeight();
            downTime = this.LongPressView(target, start);
            Utility.sleep(1000);
            //assertTrue(mEditModePreviewContainer.isShowing());
            this.LongPressMove(downTime, start, middle);
            Utility.sleep(1000);
            this.LongPressMove(downTime, middle, stop);
            Utility.sleep(1000);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }

        //步骤3
        snapToRandomPage();
        if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
            snapToPrevPage();
        }
        target = SelectOneRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        if(target != null){//拖动到底部缩略图松手
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getHeight()/2;
            downTime = LongPressView(target, start);
            Utility.sleep(2000);
            index = Utility.getRandomNotValue(mPreviewList.getChildCount(),mLauncher.getWorkspace().getCurrentPage());
            dropTarget = mPreviewList.getChildAt(index);
            dropTarget.getLocationOnScreen(stop);
            stop[0] += target.getMeasuredWidth()/2;
            stop[1] += target.getHeight()/2;
            this.LongPressMove(downTime, start, stop);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }
        //步骤4-5
        snapToRandomPage();
        if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
            snapToPrevPage();
        }
        target = SelectOneRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        if(target != null){//拖动到底部缩略图悬停，切换后拖动图标到新的屏幕某一位置松手
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getHeight()/2;
            downTime = LongPressView(target, start);
            Utility.sleep(1000);
            index = Utility.getRandomNotValue(mPreviewList.getChildCount(),mLauncher.getWorkspace().getCurrentPage());
            dropTarget = mPreviewList.getChildAt(index);
            dropTarget.getLocationOnScreen(middle);
            middle[0] += dropTarget.getMeasuredWidth()/2;
            middle[1] += dropTarget.getHeight()/2;
            this.LongPressMove(downTime, start, middle);
            Utility.sleep(1000);
            layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
            layout.getLocationOnScreen(stop);
            index = Utility.getRandomInRange(layout.getMeasuredWidth());
            if(index < stop[0]+target.getMeasuredWidth()){
                stop[0] += target.getMeasuredWidth();
            }else{
                stop[0] = index;
            }
            index = Utility.getRandomInRange(layout.getMeasuredHeight());
            if(index < stop[1]+target.getMeasuredHeight()){
                stop[1] += target.getMeasuredHeight();
            }else{
                stop[1] = index;
            }
            LongPressMove(downTime, middle, stop);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }

        //步骤6,拖动到屏幕边缘,切换至下一屏
        snapToRandomPage();
        if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
            snapToPrevPage();
        }
        target = SelectOneRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            middle[0] = mScreenWidth;
            middle[1] = mScreenHeight/2;
            stop[0] = mScreenWidth/2;
            stop[1] = mScreenHeight/2;
            downTime = LongPressView(target, start);
            LongPressMove(downTime, start, middle);
            Utility.sleep(600);
            LongPressMove(downTime,middle,stop);
            LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }

      //步骤7-9
        snapToRandomPage();
        if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
            snapToPrevPage();
        }
        target = SelectOneRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        bNext = true;
        maxTry = 12;
        while(target == null && maxTry-- > 0){
            if(bNext)
              snapToNextPage();
          else
              snapToPrevPage();
          if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
              bNext = false;
          }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
              bNext = true;
          }
          target = SelectOneRandomIcon((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        }
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = LongPressView(target, start);
            Utility.sleep(1000);
            middle[0] = mScreenWidth/4;
            maxTry = 15;
            do{
                dropTarget = mPreviewList.getChildAt(mPreviewList.getChildCount()-1);
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(3000);
                middle[1] = stop[1];
                //if(stop[0]+dropTarget.getMeasuredWidth()/2 <= mScreenWidth){
                middle[0] = mScreenWidth;
                this.LongPressMove(downTime, stop, middle);
                Utility.sleep(2000);
                stop[0] = middle[0];
                //}
                middle[0] = mScreenWidth / 4;
                this.LongPressMove(downTime, stop, middle);
                start[0] = middle[0];
                start[1] = middle[1];
                Utility.sleep(1000);
            }while(mLauncher.getWorkspace().getChildCount() < 12 && (maxTry-- > 0));//新建十二屏
            stop[0] = mScreenWidth;
            stop[1] = start[1];
            this.LongPressMove(downTime, start, stop);//拖动到previewlist最后，看看是否还有+号屏
            Utility.sleep(3000);
            start[0] = stop[0];
            dropTarget = mPreviewList.getChildAt(mPreviewList.getChildCount()-1);
            dropTarget.getLocationOnScreen(stop);
            stop[0] += dropTarget.getMeasuredWidth()/2;
            stop[1] += dropTarget.getMeasuredHeight()/2;
            this.LongPressMove(downTime, start, stop);
            Utility.sleep(2000);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(2000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_02SingleIconArrange,00-002桌面整理-单个应用图标结束");
    }

    /**
     * 整理小工具
     */
    public void test00_03SingleWidgetArrange(){
        Log.i(Utility.UTILITY_TAG,"test00_03SingleWidgetArrange,00-003桌面整理-单个小工具开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        CellLayout layout;
        View target = null;
        View dropTarget = null;
        View[] views = new View[2];
        int maxTry = 12;
        int index;
        long downTime;
        boolean bNext = true;
        int[] start = new int[2];
        int[] middle = new int[2];
        int[] stop = new int[2];
      //步骤1-2
        target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        if(target == null){
            Log.i(Utility.UTILITY_TAG,"SingleWidgetArrange,target is null,currentPage="+mWorkspace.getCurrentPage());
        }
        if(target != null){//随意移动，然后在底部预览某一屏松手
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth() / 2;
            start[1] += target.getMeasuredHeight() / 2;
            downTime = LongPressView(target, start);
            layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
            for(int i=0;i< 4;i++){
                stop[0] = Utility.getRandomInRange(layout.getWidth() - 200) + 100;
                stop[1] = Utility.getRandomInRange(layout.getHeight() - 200) + 100;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(1000);
                start[0] = stop[0];
                start[1] = stop[1];
            }
            index = Utility.getRandomNotValue(mPreviewList.getChildCount(),mLauncher.getWorkspace().getCurrentPage());
            if(index > 0 && (index == mPreviewList.getChildCount()-1)){
                index--;
            }
            dropTarget = mPreviewList.getChildAt(index);
            dropTarget.getLocationOnScreen(middle);
            middle[0] += dropTarget.getMeasuredWidth()/2;
            middle[1] += dropTarget.getMeasuredHeight()/2;
            this.LongPressMove(downTime, start, middle);
            Utility.sleep(2000);
            this.LongPressRelease(middle, downTime);
            Utility.sleep(3000);
        }

        //步骤3-4
        snapToRandomPage();
        target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        maxTry = 24;
        while( target == null && (maxTry-- > 0) ){
            if(bNext)
                snapToNextPage();
            else
                snapToPrevPage();
            if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
                bNext = false;
            }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
                bNext = true;
            }
            target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        }
        if(target != null){//拖动到底部预览，在某一屏悬停，屏幕跳转，在新屏幕的某一个位置松手
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth() / 2;
            start[1] += target.getMeasuredHeight() / 2;
            downTime = LongPressView(target, start);
            Utility.sleep(2000);
            index = Utility.getRandomNotValue(mPreviewList.getChildCount(),mLauncher.getWorkspace().getCurrentPage());
            if(index > 0 && (index == mPreviewList.getChildCount()-1)){
                index--;
            }
            dropTarget = mPreviewList.getChildAt(index);
            dropTarget.getLocationOnScreen(middle);
            middle[0] += dropTarget.getMeasuredWidth()/2;
            middle[1] += dropTarget.getMeasuredHeight()/2;
            this.LongPressMove(downTime, start, middle);
            Utility.sleep(2000);
            layout = (CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage());
            layout.getLocationOnScreen(stop);
            stop[0] = Utility.getRandomInRange(layout.getWidth() - 200) + 100;
            stop[1] = Utility.getRandomInRange(layout.getHeight() - 200) + 100;
            LongPressMove(downTime, middle, stop);
            Utility.sleep(2000);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(1000);
        }

        //步骤5-6,拖动到屏幕边缘,切换至下一屏
        target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        maxTry = 24;
        while( target == null && (maxTry-- > 0) ){
            if(bNext)
                snapToNextPage();
            else
                snapToPrevPage();
            if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
                bNext = false;
            }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
                bNext = true;
            }
            target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        }
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            middle[0] = mScreenWidth;
            middle[1] = mScreenHeight/2;
            stop[0] = mScreenWidth/2;
            stop[1] = mScreenHeight/2;
            downTime = LongPressView(target, start);
            LongPressMove(downTime, start, middle);
            Utility.sleep(600);
            LongPressMove(downTime,middle,stop);
            LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }

        //步骤7-8，新建十二屏
        target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        bNext = true;
        maxTry = 12;
        while(target == null && maxTry-- > 0){
          if(bNext)
              snapToNextPage();
          else
              snapToPrevPage();
          if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
              bNext = false;
          }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
              bNext = true;
          }
          target = SelectOneRandomWidget((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
        }
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = LongPressView(target, start);
            Utility.sleep(1000);
            middle[0] = mScreenWidth/4;
            maxTry = 15;
            do{
                dropTarget = mPreviewList.getChildAt(mPreviewList.getChildCount()-1);
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(3000);
                middle[1] = stop[1];
                //if(stop[0]+dropTarget.getMeasuredWidth()/2 <= mScreenWidth){
                middle[0] = mScreenWidth;
                this.LongPressMove(downTime, stop, middle);
                Utility.sleep(2000);
                stop[0] = middle[0];
                //}
                middle[0] = mScreenWidth / 4;
                this.LongPressMove(downTime, stop, middle);
                start[0] = middle[0];
                start[1] = middle[1];
                Utility.sleep(1000);
            }while(mLauncher.getWorkspace().getChildCount() < 12 && (maxTry-- > 0));//新建十二屏
            stop[0] = mScreenWidth;
            stop[1] = start[1];
            this.LongPressMove(downTime, start, stop);//拖动到previewlist最后，看看是否还有+号屏
            Utility.sleep(3000);
            start[0] = stop[0];
            dropTarget = mPreviewList.getChildAt(mPreviewList.getChildCount()-1);
            dropTarget.getLocationOnScreen(stop);
            stop[0] += dropTarget.getMeasuredWidth()/2;
            stop[1] += dropTarget.getMeasuredHeight()/2;
            this.LongPressMove(downTime, start, stop);
            Utility.sleep(2000);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(2000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_03SingleWidgetArrange,00-003桌面整理-单个小工具结束");
    }

    public void test00_04MutiIconArrange(){
        Log.i(Utility.UTILITY_TAG,"test00_04MutiIconArrange,00-004桌面整理-多个图标开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View target = null;
        View dropTarget = null;
        int index = 0;
        long downTime;
        int[] start = new int[2];
        int[] middle = new int[2];
        int[] stop = new int[2];
        //步骤1-6
        target = CheckMultiIconsFromMultiScreen();
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth() / 2;
            start[1] += target.getMeasuredHeight() / 2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            index = Utility.getRandomNotValue(mPreviewList.getChildCount(),mLauncher.getWorkspace().getCurrentPage());
            if(index > 0 && (index == mPreviewList.getChildCount()-1)){
                index--;
            }
            dropTarget = mPreviewList.getChildAt(index);
            dropTarget.getLocationOnScreen(middle);
            middle[0] += dropTarget.getMeasuredWidth()/2;
            middle[1] += dropTarget.getMeasuredHeight()/2;
            this.LongPressMove(downTime, start, middle);
            Utility.sleep(2000);
            this.LongPressRelease(middle, downTime);
            Utility.sleep(1000);
        }
        //步骤7 拖动切换至下一屏
        target = CheckMultiIconsFromMultiScreen();
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            middle[0] = mScreenWidth;
            middle[1] = mScreenHeight/2;
            stop[0] = mScreenWidth/2;
            stop[1] = mScreenHeight/2;
            downTime = LongPressView(target, start);
            LongPressMove(downTime, start, middle);
            Utility.sleep(600);
            LongPressMove(downTime,middle,stop);
            LongPressRelease(stop, downTime);
            Utility.sleep(3000);
        }
        //步骤8 点击widget
        target = SelectOneRandomWidget(getCurrentCellLayout());
        if(target != null){//点击widget的四个角以及中间位置
            target.getLocationOnScreen(start);
            middle[0] = start[0];
            middle[1] = start[1];
            Utility.execShellTapCmd(middle[0], middle[1]);
            middle[0] = start[0];
            middle[1] = (int) (start[1]+target.getMeasuredHeight()*0.8-1);
            Utility.execShellTapCmd(middle[0], middle[1]);
            middle[0] = (int) (start[0]+target.getMeasuredWidth()*0.8-1);
            middle[1] = (int) (start[1]+target.getMeasuredHeight()*0.8-1);
            Utility.execShellTapCmd(middle[0], middle[1]);
            middle[0] = (int) (start[0]+target.getMeasuredWidth()*0.8-1);
            middle[1] = start[1];
            Utility.execShellTapCmd(middle[0], middle[1]);
            middle[0] = start[0]+target.getMeasuredWidth()/2;
            middle[1] = start[1]+target.getMeasuredHeight()/2;
            Utility.execShellTapCmd(middle[0], middle[1]);
            Utility.sleep(2000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_04MutiIconArrange,00-004桌面整理-多个图标结束");
    }

    /**
     * not implement now,reserved
     */
    public void test00_05RingAlarmMessage(){
        Log.i(Utility.UTILITY_TAG,"test00_05RingAlarmMessage,00-005桌面整理页面收到来电，消息以及闹钟提醒");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_05RingAlarmMessage,00-005桌面整理页面收到来电，消息以及闹钟提醒");
    }

    public void test00_08FolderRename(){
        Log.i(Utility.UTILITY_TAG,"test00_05RingAlarmMessage,00-008分组重命名");

        FolderIcon folderIcon = null;
        View title;
        View target;
        long downTime;
        int[] start = new int[2];
        int[] stop = new int[2];
        int folderScreenIndex = 0;
        folderIcon = (FolderIcon) SelectOneFolder();
        if(folderIcon != null){//打开folder，编辑名字
            folderIcon.getLocationOnScreen(start);
            start[0] += folderIcon.getMeasuredWidth()/2;
            start[1] += folderIcon.getMeasuredHeight()/2;
            Utility.execShellTapCmd(start[0], start[1]);
            Utility.sleep(2000);
            title = folderIcon.getFolder().findViewById(R.id.folder_name);
            title.getLocationOnScreen(start);
            start[0] += title.getMeasuredWidth()/2;
            start[1] += title.getMeasuredHeight()/2;
            Log.i("chenjian","x="+start[0]+",y="+start[1]);
            Utility.execShellTapCmd(start[0], start[1]);
            Utility.sleep(1000);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_A, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_L, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_I, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_B, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_A, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_B, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_A, 200);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_ENTER, 200);
            Utility.sleep(2000);
            Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU, 1000);
        }
        folderScreenIndex = mWorkspace.getCurrentPage();
        target = this.SelectOneIcon();
        if(target != null){//选择一个图标拖入文件夹，观察文件夹显示是否正确
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(1000);
            while(folderScreenIndex != mWorkspace.getCurrentPage()){
                if(folderScreenIndex < mWorkspace.getCurrentPage()){
                    this.DragToPrevPage(target, start, downTime);
                }else if(folderScreenIndex > mWorkspace.getCurrentPage()){
                    this.DragToNextPage(target, start, downTime);
                }
            }
            if(folderIcon != null){
                folderIcon.getLocationOnScreen(stop);
                stop[0] += folderIcon.getMeasuredWidth()/2;
                stop[1] += folderIcon.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(2000);
                this.LongPressRelease(stop, downTime);
            }else{
                this.LongPressRelease(start, downTime);
            }
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_05RingAlarmMessage,00-005分组重命名");
    }

    public void test00_09FlingIcon(){
        Log.i(Utility.UTILITY_TAG,"test00_09FlingIcon,00-009Fling应用后打开桌面编辑");

        Utility.sleep(2000);
        snapToNextPage();
        Utility.sleep(1000);
        View target = SelectOneIcon();
        int start[] = new int[2];
        long downTime;
        target.getLocationOnScreen(start);
        start[0] += target.getMeasuredWidth()/2;
        start[1] += target.getMeasuredHeight() / 2;
        downTime = LongPressView(target, start);
        this.LongPressFlingAndRelease(downTime, start, true);//flingToRight
        Utility.sleep(3000);
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);
        snapToPrevPage();
        Utility.sleep(3000);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test00_09FlingIcon,00-009Fling应用后打开桌面编辑");
    }

    public void test01_01WallPaper(){
        Log.i(Utility.UTILITY_TAG,"test01_01WallPaper,01-001检查壁纸显示内容");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View wallpaper = menu.findViewById(R.id.menu_item_wallpaper);
        MenuCommonTest001(wallpaper);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test01_01WallPaper,00-001检查壁纸显示内容");
    }

    public void test01_02WallPaperLibrary(){
        Log.i(Utility.UTILITY_TAG,"test01_02WallPaperLibrary,01-002打开壁纸库开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View wallpaper = menu.findViewById(R.id.menu_item_wallpaper);
        //MenuCommonTest002(wallpaper);//由于打开壁纸中心以后，Launcher切换到后台，不会再响应接下来的操作，所以暂停不对其进行测试

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test01_02WallPaperLibrary,01-002打开壁纸库结束");
    }

    public void test01_04WallPaperArrange(){
        Log.i(Utility.UTILITY_TAG,"test01_04WallPaperArrange,01-004打开壁纸页面进行桌面整理开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int[] start = new int[2];
        int[] stop = new int[2];
        long downTime;
        View menu = mLauncher.getmMenu();
        View target;
        View wallpaper = menu.findViewById(R.id.menu_item_wallpaper);
        for(int i=0;i<2;i++){//移动图标以及小工具
            wallpaper.getLocationOnScreen(wpLoc);
            wpLoc[0] += wallpaper.getMeasuredWidth() / 2;
            wpLoc[1] += wallpaper.getMeasuredHeight() / 2;
            Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
            Utility.sleep(2000);

            if(i==0){
                target = this.SelectOneIcon();
            }else{
                target = this.SelectOneRandomWidget(getCurrentCellLayout());
            }
            if(target != null){
                target.getLocationOnScreen(start);
                start[0] += target.getMeasuredWidth()/2;
                start[1] += target.getMeasuredHeight()/2;
                downTime = this.LongPressView(target, start);
                Utility.sleep(2000);
                stop[0] = Utility.getRandomInRange(mScreenWidth);
                stop[1] = Utility.getRandomInRange(mScreenHeight);
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(1000);
                this.LongPressMove(downTime, stop, start);
                Utility.sleep(1000);
                this.LongPressRelease(start, downTime);
                }
            Utility.sleep(3000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test01_04WallPaperArrange,01-004设置壁纸过程中进行桌面整理结束");
    }

    public void test02_01Theme(){
        Log.i(Utility.UTILITY_TAG,"test02_01Theme,02-001检查主题显示内容开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View theme = menu.findViewById(R.id.menu_item_theme);
        MenuCommonTest001(theme);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test02_01Theme,02-001检查主题显示内容结束");
    }

    public void test02_02WallThemeLibrary(){
        Log.i(Utility.UTILITY_TAG,"test02_02WallThemeLibrary,02-002打开主题库开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View theme = menu.findViewById(R.id.menu_item_theme);
        //MenuCommonTest002(theme);//由于打开壁纸中心以后，Launcher切换到后台，不会再响应接下来的操作，所以暂停不对其进行测试

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test02_02WallThemeLibrary,02-002打开主题库结束");
    }

    public void test02_04ThemeArrange(){
        Log.i(Utility.UTILITY_TAG,"test02_04ThemeArrange,02-004设置主题过程中进行桌面整理开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int[] start = new int[2];
        int[] stop = new int[2];
        long downTime;
        View menu = mLauncher.getmMenu();
        View target;
        View theme = menu.findViewById(R.id.menu_item_theme);
        for(int i=0;i<2;i++){//移动图标以及小工具
            theme.getLocationOnScreen(wpLoc);
            wpLoc[0] += theme.getMeasuredWidth() / 2;
            wpLoc[1] += theme.getMeasuredHeight() / 2;
            Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
            Utility.sleep(2000);

            if(i==0){
                target = this.SelectOneIcon();
            }else{
                target = this.SelectOneRandomWidget(getCurrentCellLayout());
            }
            if(target != null){
                target.getLocationOnScreen(start);
                start[0] += target.getMeasuredWidth()/2;
                start[1] += target.getMeasuredHeight()/2;
                downTime = this.LongPressView(target, start);
                Utility.sleep(2000);
                stop[0] = Utility.getRandomInRange(mScreenWidth);
                stop[1] = Utility.getRandomInRange(mScreenHeight);
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(1000);
                this.LongPressMove(downTime, stop, start);
                Utility.sleep(1000);
                this.LongPressRelease(start, downTime);
                }
            Utility.sleep(3000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test02_04ThemeArrange,02-004设置主题过程中进行桌面整理结束");
    }

    public void test02_05WallThemeHardKey(){
        Log.i(Utility.UTILITY_TAG,"test02_05WallThemeHardKey,02-005设置主题过程中按硬键操作开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int start[] = new int[2];
        View menu = mLauncher.getmMenu();
        View theme = menu.findViewById(R.id.menu_item_theme);
        theme.getLocationOnScreen(wpLoc);
        wpLoc[0] += theme.getMeasuredWidth() / 2;
        wpLoc[1] += theme.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
        Utility.sleep(2000);

        int index = Utility.getRandomInRange(mPreviewList.getChildCount());
        if(index == 0 && mPreviewList.getChildCount() > 1){
            index = 1;
        }
        View child = mPreviewList.getChildAt(index);
        child.getLocationOnScreen(start);
        start[0] += child.getMeasuredWidth()/2;
        start[1] += child.getMeasuredHeight()/2;
        Utility.execShellTapCmd(start[0], start[1]);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_MENU, 1000);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        snapToNextPage();
        Utility.sleep(1000);
        snapToNextPage();
        Utility.sleep(1000);
        snapToNextPage();
        Utility.sleep(1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test02_05WallThemeHardKey,02-002设置主题过程中按硬键操作结束");
    }

    public void test03_01Effect(){
        Log.i(Utility.UTILITY_TAG,"test03_01Effect,03-001检查特效显示内容开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View effect = menu.findViewById(R.id.menu_item_effects);
        MenuCommonTest001(effect);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test03_01Effect,03-001检查特效显示内容结束");
    }

    public void test03_02EffectLoop(){
        Log.i(Utility.UTILITY_TAG,"test03_02EffectLoop,03-002遍历所有特效开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int start[] = new int[2];
        int bound[] = new int[2];
        View menu = mLauncher.getmMenu();
        View effect = menu.findViewById(R.id.menu_item_effects);
        effect.getLocationOnScreen(wpLoc);
        wpLoc[0] += effect.getMeasuredWidth() / 2;
        wpLoc[1] += effect.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
        Utility.sleep(2000);
        int maxtry = 3;
        int index;
        View child;
        int cnt;
        int listRight,Y;
        while(maxtry-- > 0){
            cnt = mPreviewList.getChildCount();
            for(int i=0;i<cnt;i++){
                child = mPreviewList.getChildAt(i);
                child.getLocationOnScreen(start);
                start[0] += child.getMeasuredWidth()/2;
                start[1] += child.getMeasuredHeight()/2;
                Utility.execShellTapCmd(start[0], start[1]);
                Utility.sleep(2000);
            }
            mPreviewList.getLocationOnScreen(bound);
            listRight = bound[0] + mPreviewList.getMeasuredWidth();
            Y = bound[1] + mPreviewList.getMeasuredHeight()/2;
            Utility.execShellSwipeCmd(listRight - 30, Y, bound[0]+30, Y, 500);//slide to right
            Utility.sleep(2000);
        }

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test03_02EffectLoop,03-002遍历所有特效结束");
    }

    public void test03_04EffectArrange(){
        Log.i(Utility.UTILITY_TAG,"test03_04EffectArrange,03-004设置特效过程中进行桌面整理开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int[] start = new int[2];
        int[] stop = new int[2];
        long downTime;
        View menu = mLauncher.getmMenu();
        View target;
        View theme = menu.findViewById(R.id.menu_item_effects);
        for(int i=0;i<2;i++){//移动图标以及小工具
            theme.getLocationOnScreen(wpLoc);
            wpLoc[0] += theme.getMeasuredWidth() / 2;
            wpLoc[1] += theme.getMeasuredHeight() / 2;
            Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
            Utility.sleep(2000);

            if(i==0){
                target = this.SelectOneIcon();
            }else{
                target = this.SelectOneRandomWidget(getCurrentCellLayout());
            }
            if(target != null){
                target.getLocationOnScreen(start);
                start[0] += target.getMeasuredWidth()/2;
                start[1] += target.getMeasuredHeight()/2;
                downTime = this.LongPressView(target, start);
                Utility.sleep(2000);
                stop[0] = Utility.getRandomInRange(mScreenWidth);
                stop[1] = Utility.getRandomInRange(mScreenHeight);
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(1000);
                this.LongPressMove(downTime, stop, start);
                Utility.sleep(1000);
                this.LongPressRelease(start, downTime);
                }
            Utility.sleep(3000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test03_04EffectArrange,03-004设置特效过程中进行桌面整理结束");
    }

    public void test04_01WidgetCheck(){
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-001小工具页面检查开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        View menu = mLauncher.getmMenu();
        View widget = menu.findViewById(R.id.menu_item_widgets);
        MenuCommonTest001(widget);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-001小工具页面检查结束");
    }

    public void test04_02WidgetToDock(){
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-001小工具拖拽至dock区开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int[] start = new int[2];
        int[] stop = new int[2];
        int[] bound = new int[2];
        int Y;
        int index;
        View menu = mLauncher.getmMenu();
        View child;
        View widget = menu.findViewById(R.id.menu_item_widgets);
        widget.getLocationOnScreen(wpLoc);
        wpLoc[0] += widget.getMeasuredWidth() / 2;
        wpLoc[1] += widget.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
        Utility.sleep(2000);
        index = Utility.getRandomInRange(mPreviewList.getChildCount());
        child = mPreviewList.getChildAt(index);
        child.getLocationOnScreen(start);
        start[0] += child.getMeasuredWidth()/2;
        start[1] += child.getMeasuredHeight()/2;
        mPreviewList.getLocationOnScreen(bound);
        int listRight = bound[0] + mPreviewList.getMeasuredWidth();
        Y = bound[1] + mPreviewList.getMeasuredHeight()/2;
        long downTime = this.LongPressView(child, start);
        Utility.sleep(1000);
        stop[0] = bound[0];
        stop[1] = Y;
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(1000);
        start[0] = stop[0];
        start[1] = stop[1];
        stop[0] = bound[0] + mPreviewList.getMeasuredWidth();
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(1000);
        start[0] = stop[0];
        index = Utility.getRandomInRange(mPreviewList.getChildCount());
        View child1 = mPreviewList.getChildAt(index);
        child1.getLocationOnScreen(stop);
        stop[0] += child1.getMeasuredWidth()/2;
        stop[1] += child1.getMeasuredHeight()/2;
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(2000);
        this.LongPressRelease(stop, downTime);
        Utility.sleep(2000);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-001小工具拖拽至dock区结束");
    }

    public void test04_03WidgetToDesk(){
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-003小工具拖拽至桌面开始");
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);

        int[] wpLoc = new int[2];
        int[] start = new int[2];
        int[] stop = new int[2];
        int[] bound = new int[2];
        int Y;
        int index;
        View menu = mLauncher.getmMenu();
        View child;
        View widget = menu.findViewById(R.id.menu_item_widgets);
        widget.getLocationOnScreen(wpLoc);
        wpLoc[0] += widget.getMeasuredWidth() / 2;
        wpLoc[1] += widget.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
        Utility.sleep(2000);
        index = Utility.getRandomInRange(mPreviewList.getChildCount());
        child = mPreviewList.getChildAt(index);
        child.getLocationOnScreen(start);
        start[0] += child.getMeasuredWidth()/2;
        start[1] += child.getMeasuredHeight()/2;
        CellLayout layout = (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        layout.getLocationOnScreen(bound);
        Y = bound[1] + layout.getMeasuredHeight()/2;
        long downTime = this.LongPressView(child, start);
        Utility.sleep(1000);
        stop[0] = bound[0];
        stop[1] = Y;
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(1000);
        start[0] = stop[0];
        start[1] = stop[1];
        stop[0] = bound[0] + mPreviewList.getMeasuredWidth();
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(1000);
        start[0] = stop[0];
        index = Utility.getRandomInRange(mPreviewList.getChildCount());
        stop[0] = Utility.getRandomInRange(layout.getMeasuredWidth() - 100) + 100;
        stop[1] = Utility.getRandomInRange(layout.getMeasuredHeight()-100) + 100;
        this.LongPressMove(downTime, start, stop);
        Utility.sleep(2000);
        this.LongPressRelease(stop, downTime);
        Utility.sleep(2000);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test04_01WidgetCheck,04-003小工具拖拽至桌面结束");
    }

    public void test05CardOps(){
        Utility.sleep(2000);

        int start[] = new int[2];
        int top[] = new int[2];
        CellLayout layout = getCurrentCellLayout();
        int screens = mWorkspace.getChildCount();
        int cnt;
        layout.getLocationOnScreen(top);
        snapToPrevPage();
        Utility.sleep(1000);
        for(int i=0;i<screens;i++){
            layout = getCurrentCellLayout();
            cnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for(int j=0;j<cnt;j++){
                layout = getCurrentCellLayout();
                View target = layout.getShortcutAndWidgetContainer().getChildAt(j);
                if(target instanceof BubbleTextView){
                    target.getLocationOnScreen(start);
                    Utility.execShellSwipeCmd(start[0] + target.getMeasuredWidth()/2, start[1]+target.getMeasuredHeight()-5, start[0]+target.getMeasuredWidth()/2, start[1]-50, 5);
                    Utility.sleep(2000);
                    Utility.execShellTapCmd(top[0]+1, top[1]+1);
                    Utility.sleep(2000);
                    Log.i("chenjian","x1="+(start[0] + target.getMeasuredWidth()/2)+",y1="+(start[1]+target.getMeasuredHeight()-5)+",x2="+(start[0]+target.getMeasuredWidth()/2)+",y2="+(start[1]-50));
                }
            }
            this.snapToNextPage();
            Utility.sleep(1000);
        }

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
    }

    public void test06_01LoopDeskHideSeat(){
        Log.i(Utility.UTILITY_TAG,"test06_01LoopDeskHideSeat,15-001打开循环桌面时打开隐藏区后滑动桌面开始");
        Utility.sleep(2000);

        boolean bl = openHideSeat(true);
        int start[] = new int[2];
        int stop[] = new int[2];
        View target = this.SelectOneIcon();
        Hideseat hideseat;
        long downTime;
        if(bl && target != null){
            hideseat = mLauncher.getHideseat();
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            hideseat.getLocationOnScreen(stop);
            stop[0] += hideseat.getMeasuredWidth()/2;
            stop[1] += hideseat.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            this.LongPressMove(downTime, start, stop);
            Utility.sleep(2000);
            this.LongPressRelease(stop, downTime);
            Utility.sleep(2000);
        }
        for(int i=0;i<12;i++){
            snapToNextPage();
            Utility.sleep(1000);
        }
        openHideSeat(false);
        Utility.sleep(3000);

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK,2000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test06_01LoopDeskHideSeat,15-001打开循环桌面时打开隐藏区后滑动桌面结束");
    }

    public void test06_02LoopDeskEffectVerify(){
        Log.i(Utility.UTILITY_TAG,"test06_02LoopDeskEffectVerify,15-002循环滑屏功能验证开始");
        Utility.sleep(2000);

        int maxtry = 3;
        int[] wpLoc = new int[2];
        int start[] = new int[2];
        int bound[] = new int[2];
        int index;
        View child;
        int cnt;
        int listRight,Y;
        int shouldSnap=0;
        sendKeys(KeyEvent.KEYCODE_MENU);
        Utility.sleep(2000);
        View menu = mLauncher.getmMenu();
        View effect = menu.findViewById(R.id.menu_item_effects);
        effect.getLocationOnScreen(wpLoc);
        wpLoc[0] += effect.getMeasuredWidth() / 2;
        wpLoc[1] += effect.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
        Utility.sleep(2000);
        mPreviewList.getLocationOnScreen(bound);
        listRight = bound[0] + mPreviewList.getMeasuredWidth();
        Y = bound[1] + mPreviewList.getMeasuredHeight()/2;
        while(maxtry-- > 0){
            cnt = mPreviewList.getChildCount();
            for(int i=0;i<cnt;i++){
                if(shouldSnap > 0){
                    for(int k=0;k<shouldSnap;k++){
                        Utility.execShellSwipeCmd(listRight - 30, Y, bound[0]+30, Y, 500);//slide to right
                        Utility.sleep(2000);
                    }
                }
                child = mPreviewList.getChildAt(i);
                if(child == null){
                    break;
                }
                child.getLocationOnScreen(start);
                start[0] += child.getMeasuredWidth()/2;
                start[1] += child.getMeasuredHeight()/2;
                Utility.execShellTapCmd(start[0], start[1]);
                Utility.sleep(2000);
                Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
                Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
                for(int j=0;j <= mWorkspace.getChildCount();j++){
                    this.snapToNextPage();
                    Utility.sleep(500);
                }
                for(int j=0;j <= mWorkspace.getChildCount();j++){
                    this.snapToPrevPage();
                    Utility.sleep(500);
                }
                sendKeys(KeyEvent.KEYCODE_MENU);
                Utility.sleep(2000);
                Utility.execShellTapCmd(wpLoc[0], wpLoc[1]);
                Utility.sleep(2000);
            }
            shouldSnap++;
        }

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.sleep(3000);
        Log.i(Utility.UTILITY_TAG,"test06_02LoopDeskEffectVerify,15-002循环滑屏功能验证开始");
    }

    public void test06_03LoopDeskScreenEdit(){
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);

        this.enterScreenEdit();
        int cnt;
        View child;
        int index1,index2;
        int start[] = new int[2];
        int stop[] = new int[2];
        View target;
        View dropTarget;
        ArrayList<View> cardList = new ArrayList<View>();
        DragLayer dragLayer = mLauncher.getDragLayer();
        ScreenManagerView screenView = null;
        cnt = dragLayer.getChildCount();
        for(int i=0;i<cnt;i++){
            child = dragLayer.getChildAt(i);
            if(child instanceof ScreenManagerView){
                screenView = (ScreenManagerView) child;
                break;
            }
        }
        if(screenView != null){
            cnt = screenView.getChildCount();
            for(int i=0;i<cnt;i++){
                child = screenView.getChildAt(i);
                if(child instanceof ScreenCardView){
                    cardList.add(child);
                }
            }
        }

        if(cardList.size() > 2){
            index1 = Utility.getRandomInRange(cardList.size());
            index2 = Utility.getRandomNotValue(cardList.size(), index1);
            target = cardList.get(index1);
            dropTarget = cardList.get(index2);
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            dropTarget.getLocationOnScreen(stop);
            stop[0] += target.getMeasuredWidth()/2;
            stop[1] += target.getMeasuredHeight()/2;
            long downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            this.LongPressMove(downTime, start, stop);
            Utility.sleep(2000);
            this.LongPressRelease(stop, downTime);
        }
        Utility.sleep(2000);
        this.exitScreenEdit();
        Utility.sleep(2000);

        cnt = mWorkspace.getChildCount();
        for(int i=0;i<=cnt;i++){
            this.snapToNextPage();
            Utility.sleep(1000);
        }

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.sleep(3000);
    }

    public void test07_01FolderGenerate(){
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);

        View target = this.SelectOneIcon();
        View dropTarget;
        int maxTry = 12;
        boolean bNext = true;
        long downTime;
        int[] start = new int[2];
        int[] stop = new int[2];
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            dropTarget = this.SelectOneIconNotValue(target);
            if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount()-1){
                bNext = true;
            }else{
                bNext = false;
            }
            while(dropTarget == null && maxTry-- > 0){
                if(bNext){
                    this.DragToNextPage(target, start, downTime);
                }else{
                    this.DragToPrevPage(target, start, downTime);
                }
                Utility.sleep(1000);
                dropTarget = this.SelectOneIconNotValue(target);
            }
            if(dropTarget != null){
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(2000);
                this.LongPressRelease(stop, downTime);
                Utility.sleep(3000);
                Utility.execShellTapCmd(stop[0], stop[1]);
                Utility.sleep(3000);
                Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 100);
            }else{
                this.LongPressRelease(start, downTime);
            }
            Utility.sleep(3000);
        }

        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.sleep(3000);
    }

    public void test07_02FolderNotGenerate(){
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);

        //步骤1-2，拖动分组到一个应用图标，不生成分组
        View target = this.SelectOneFolder();
        View dropTarget;
        int maxTry = 12;
        boolean bNext = true;
        long downTime;
        int[] start = new int[2];
        int[] stop = new int[2];
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            dropTarget = this.SelectOneIconNotValue(target);
            if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount()-1){
                bNext = true;
            }else{
                bNext = false;
            }
            while(dropTarget == null && maxTry-- > 0){
                if(bNext){
                    this.DragToNextPage(target, start, downTime);
                }else{
                    this.DragToPrevPage(target, start, downTime);
                }
                Utility.sleep(1000);
                dropTarget = this.SelectOneIconNotValue(target);
            }
            if(dropTarget != null){
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(2000);
                this.LongPressRelease(stop, downTime);
                Utility.sleep(3000);
            }else{
                this.LongPressRelease(start, downTime);
            }
            Utility.sleep(3000);
        }

        //步骤3-4，拖动一个应用到另一个应用，出现文件夹外框，然后移走，不生成文件夹
        target = this.SelectOneIcon();
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            dropTarget = this.SelectOneIconNotValue(target);
            if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount()-1){
                bNext = true;
            }else{
                bNext = false;
            }
            while(dropTarget == null && maxTry-- > 0){
                if(bNext){
                    this.DragToNextPage(target, start, downTime);
                }else{
                    this.DragToPrevPage(target, start, downTime);
                }
                Utility.sleep(1000);
                dropTarget = this.SelectOneIconNotValue(target);
            }
            if(dropTarget != null){
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(3000);
                this.LongPressMove(downTime, stop, start);
                Utility.sleep(3000);
                this.LongPressRelease(start, downTime);
                Utility.sleep(3000);
                Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 100);
            }else{
                this.LongPressRelease(start, downTime);
            }
            Utility.sleep(3000);
        }

        //步骤5，拖动图标到hotseat，不生成分组
        target = this.SelectOneIcon();
        if(target != null){
            target.getLocationOnScreen(start);
            start[0] += target.getMeasuredWidth()/2;
            start[1] += target.getMeasuredHeight()/2;
            downTime = this.LongPressView(target, start);
            Utility.sleep(2000);
            CellLayout hotSeatLayout = mLauncher.getHotseat().getCellLayout();
            int index = Utility.getRandomInRange(hotSeatLayout.getShortcutAndWidgetContainer().getChildCount());
            dropTarget = hotSeatLayout.getShortcutAndWidgetContainer().getChildAt(index);
            if(dropTarget != null){
                dropTarget.getLocationOnScreen(stop);
                stop[0] += dropTarget.getMeasuredWidth()/2;
                stop[1] += dropTarget.getMeasuredHeight()/2;
                this.LongPressMove(downTime, start, stop);
                Utility.sleep(3000);
                this.LongPressRelease(stop, downTime);
            }else{
                this.LongPressRelease(start, downTime);
            }
            Utility.sleep(3000);
        }
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
        Utility.sleep(3000);
    }

    public void MenuCommonTest001(View view){//
        int[] bound = new int[2];
        int[] start = new int[2];
        int listRight;
        int Y;
        view.getLocationOnScreen(start);
        start[0] += view.getMeasuredWidth() / 2;
        start[1] += view.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(start[0], start[1]);
        Utility.sleep(2000);
        mPreviewList.getLocationOnScreen(bound);
        listRight = bound[0] + mPreviewList.getMeasuredWidth();
        Y = bound[1] + mPreviewList.getMeasuredHeight()/2;
        Utility.execShellSwipeCmd(listRight - 30, Y, bound[0]+30, Y, 500);//slide to right
        Utility.sleep(1000);
        Utility.execShellSwipeCmd(listRight - 30, Y, bound[0]+30, Y, 500);//slide to right
        Utility.sleep(1000);
        Utility.execShellSwipeCmd(bound[0]+30, Y, listRight - 30, Y, 500);
        Utility.sleep(1000);
        int index = Utility.getRandomInRange(mPreviewList.getChildCount());
        if(index == 0 && mPreviewList.getChildCount() > 1){
            index = 1;
        }
        View child = mPreviewList.getChildAt(index);
        child.getLocationOnScreen(start);
        start[0] += child.getMeasuredWidth()/2;
        start[1] += child.getMeasuredHeight()/2;
        Utility.execShellTapCmd(start[0], start[1]);
        Utility.sleep(5000);
    }

    public void MenuCommonTest002(View view){
        int[] start = new int[2];
        view.getLocationOnScreen(start);
        start[0] += view.getMeasuredWidth() / 2;
        start[1] += view.getMeasuredHeight() / 2;
        Utility.execShellTapCmd(start[0], start[1]);
        Utility.sleep(2000);
        View child = mPreviewList.getChildAt(0);
        child.getLocationOnScreen(start);
        start[0] += child.getMeasuredWidth()/2;
        start[1] += child.getMeasuredHeight()/2;
        Utility.execShellTapCmd(start[0], start[1]);
        Utility.sleep(2000);
        Utility.execShellTapCmd(50, 90);
        Utility.sleep(2000);
        Utility.execShellKeyEvent(KeyEvent.KEYCODE_BACK, 1000);
    }

    /**
     * 在当前屏中随机选择一个图标或者文件夹
     * @param layout
     * @return
     */
    public View SelectOneRandomIcon(CellLayout layout){
        ArrayList<View> mArray = new ArrayList<View>();
        View child;
        int childCnt;
        childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
        if(childCnt < 1){
            return null;
        }
        for(int i=0;i<childCnt;i++){
            child = layout.getShortcutAndWidgetContainer().getChildAt(i);
            if(child instanceof BubbleTextView || child instanceof FolderIcon){
                mArray.add(child);
            }
        }
        if(mArray.size() < 1){
            return null;
        }
        int index = (int) Math.floor(Math.random()*mArray.size());
        if(index >= 0 && index < mArray.size()){
            return mArray.remove(index);
        }
        return null;
    }

    /**
     * 将当前拖拽的图标拖入下一页
     * @param target
     * @param start
     * @param downTime
     */
    public void DragToNextPage(View target,int[] start,long downTime){
        int[] middle = new int[2];
        int[] stop = new int[2];
        if(target != null){
            middle[0] = mScreenWidth;
            middle[1] = mScreenHeight/2;
            stop[0] = mScreenWidth/2;
            stop[1] = mScreenHeight/2;
            LongPressMove(downTime, start, middle);
            Utility.sleep(600);
            LongPressMove(downTime,middle,stop);
            start[0] = stop[0];
            start[1] = stop[1];
            Utility.sleep(3000);
        }
    }

    /**
     * 将当前拖拽的图标拖入上一屏
     * @param target
     * @param start
     * @param downTime
     */
    public void DragToPrevPage(View target,int[] start,long downTime){
        int[] middle = new int[2];
        int[] stop = new int[2];
        if(target != null){
            middle[0] = 0;
            middle[1] = mScreenHeight/2;
            stop[0] = mScreenWidth/2;
            stop[1] = mScreenHeight/2;
            LongPressMove(downTime, start, middle);
            Utility.sleep(600);
            LongPressMove(downTime,middle,stop);
            start[0] = stop[0];
            start[1] = stop[1];
            Utility.sleep(3000);
        }
    }

    /**
     * 选择一个icon，如果当前屏没有，遍历其它屏一直到找到位置
     * @return
     */
    public View SelectOneIcon(){//Only icon,not folder
        View child = null;
        CellLayout layout;
        int childCnt;
        int maxTry = 24;
        boolean bNext;
        if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount() -2){
            bNext = true;
        }else{
            bNext = false;
        }
        while(maxTry-- > 0){
            layout = this.getCurrentCellLayout();
            childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for(int i=0;i<childCnt;i++){
                child = layout.getShortcutAndWidgetContainer().getChildAt(i);
                if(child instanceof BubbleTextView){
                    return child;
                }
            }
            if(bNext)
                this.snapToNextPage();
            else
                this.snapToPrevPage();

            if(mWorkspace.getCurrentPage() == 0){
                bNext = true;
            }else if(mWorkspace.getCurrentPage() == mWorkspace.getChildCount()-1){
                bNext = false;
            }
        }
        return null;
    }

    public View SelectOneIconNotValue(View prev){//Only icon,not folder
        View child = null;
        CellLayout layout;
        int childCnt;

        layout = this.getCurrentCellLayout();
        childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
        for(int i=0;i<childCnt;i++){
            child = layout.getShortcutAndWidgetContainer().getChildAt(i);
            if(child instanceof BubbleTextView && child!= prev){
                return child;
            }
        }

        return null;
    }

    /**
     * 遍历寻找文件夹
     * @return
     */
    public View SelectOneFolder(){
        View child = null;
        CellLayout layout;
        int childCnt;
        int maxTry = 24;
        boolean bNext;
        if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount() -2){
            bNext = true;
        }else{
            bNext = false;
        }
        while(maxTry-- > 0){
            layout = this.getCurrentCellLayout();
            childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for(int i=0;i<childCnt;i++){
                child = layout.getShortcutAndWidgetContainer().getChildAt(i);
                if(child instanceof FolderIcon){
                    return child;
                }
            }
            if(bNext)
                this.snapToNextPage();
            else
                this.snapToPrevPage();

            if(mWorkspace.getCurrentPage() == 0){
                bNext = true;
            }else if(mWorkspace.getCurrentPage() == mWorkspace.getChildCount()-1){
                bNext = false;
            }
        }
        return null;
    }

    /**
     * 获取给定屏幕的所有Icon以及文件夹
     * @param layout
     * @return
     */
    public ArrayList<View> getAllIcons(CellLayout layout){
        ArrayList<View> mArray = new ArrayList<View>();
        View child;
        int childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
        if(childCnt < 1){
            return mArray;
        }
        for(int i=0;i<childCnt;i++){
            child = layout.getShortcutAndWidgetContainer().getChildAt(i);
            if(child instanceof BubbleTextView || child instanceof FolderIcon){
                mArray.add(child);
            }
        }
        return mArray;
    }

    public ArrayList<View> getAllWidgets(CellLayout layout){
        ArrayList<View> mArray = new ArrayList<View>();
        View child;
        int childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
        if(childCnt < 1){
            return mArray;
        }
        for(int i=0;i<childCnt;i++){
            child = layout.getShortcutAndWidgetContainer().getChildAt(i);
            if(child instanceof GadgetView || child instanceof AppWidgetHostView){
                mArray.add(child);
            }
        }
        return mArray;
    }

    public View CheckMultiIconsFromMultiScreen(){
        ArrayList<View> mArray;
        int cnt;
        View target = null;
        int[] start = new int[2];
        int index=0,i,j;
        int lastSelIndex = 0;
        boolean bNext;
        int cnt1;

        cnt = Utility.getRandomInRange(mWorkspace.getChildCount());
        if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount()-2){
            bNext = true;
        }else{
            bNext = false;
        }
        for(j=0;j<cnt;j++){
            //snapToValidPage(FLAG_TYPE_APP_FOLDER, 1);
            mArray = this.getAllIcons(getCurrentCellLayout());
            cnt1 = mArray.size();
            if(cnt1 > 0)
            {
                cnt1 = Utility.getRandomInRange(cnt1)+1;
                for(i=0;i<cnt1;i++){
                    index = Utility.getRandomInRange(mArray.size());
                    target = mArray.remove(index);
                    target.getLocationOnScreen(start);
                    lastSelIndex = mWorkspace.getCurrentPage();
                    start[0] += target.getMeasuredWidth()/2;
                    start[1] += target.getMeasuredHeight()/2;
                    Utility.execShellTapCmd(start[0], start[1]);
                    Utility.sleep(1000);
                }
            }
            Log.i("chenjian","lastSelIndex="+lastSelIndex+",cnt1="+cnt1+",arrSize="+mArray.size());
            if(bNext){
                this.snapToNextPage();
            }else{
                this.snapToLastPage();
            }
        }
        this.snapToPage(lastSelIndex);
        return target;
    }

    public View SelectOneRandomWidget(CellLayout layout){
        ArrayList<View> mArray = new ArrayList<View>();
        View child = null;
        int childCnt;
        int maxTry = 24;
        boolean bNext;
        if(mWorkspace.getCurrentPage() < mWorkspace.getChildCount() -2){
            bNext = true;
        }else{
            bNext = false;
        }
        while(maxTry-- > 0){
            layout = this.getCurrentCellLayout();
            childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
            for(int i=0;i<childCnt;i++){
                child = layout.getShortcutAndWidgetContainer().getChildAt(i);
                if(child instanceof GadgetView || child instanceof AppWidgetHostView){
                    mArray.add(child);
                }
            }
            if(mArray.size() > 0){
                int index = (int) Math.floor(Math.random()*mArray.size());
                if(index >= 0 && index < mArray.size()){
                    return mArray.remove(index);
                }
            }
            if(bNext)
                this.snapToNextPage();
            else
                this.snapToPrevPage();

            if(mWorkspace.getCurrentPage() == 0){
                bNext = true;
            }else if(mWorkspace.getCurrentPage() == mWorkspace.getChildCount()-1){
                bNext = false;
            }
        }
        return null;
    }

    public boolean SelectTwoRandomIcon(CellLayout layout,View[] view){
        ArrayList<View> mArray = new ArrayList<View>();
        View child;
        int childCnt;
        childCnt = layout.getShortcutAndWidgetContainer().getChildCount();
        if(childCnt < 2){
            return false;
        }
        for(int i=0;i<childCnt;i++){
            child = layout.getShortcutAndWidgetContainer().getChildAt(i);
            if(child instanceof BubbleTextView){
                mArray.add(child);
            }
        }
        if(mArray.size() < 2){
            return false;
        }
        int index = (int) Math.floor(Math.random()*mArray.size());
        if(index >= 0 && index < mArray.size()){
            view[0] = mArray.remove(index);
        }
        index = (int) Math.floor(Math.random()*mArray.size());
        if(index >= 0 && index < mArray.size()){
            view[1] = mArray.remove(index);
        }
        if(view[0] == null || view[1] == null){
            return false;
        }
        return true;
    }

    public long LongPressView(View view,int[] start){
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, start[0], start[1], 0);
        mInstrumentation.sendPointerSync(event);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        return downTime;
    }

    /**
     *
     * @param downTime
     * @param start
     * @param direct,方向，true:left, false:right
     */
    public void LongPressFlingAndRelease(long downTime,int[] start,boolean direct){
        int[] middle = new int[2];
        int[] stop = new int[2];
        int span = 50;

        middle[1] = mScreenHeight / 2;
        long eventTime;
        MotionEvent event;
        if(direct){
            stop[0] = 0;
            stop[1] = mScreenHeight / 2;
            middle[0] = mScreenWidth-span;
        }else{
            middle[0] = span;
            stop[0] = mScreenWidth;
            stop[1] = mScreenHeight / 2;
        }
        LongPressMove(downTime,start,middle);
        Utility.sleep(1000);
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, stop[0], stop[1], 0);
        mInstrumentation.sendPointerSync(event);

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, stop[0]*2-middle[0], stop[1], 0);
        mInstrumentation.sendPointerSync(event);

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, stop[0]*2-middle[0], stop[1], 0);
        mInstrumentation.sendPointerSync(event);
    }

    public void LongPressMove(long downTime,int[] start,int[] stop){
        int interval = 20;
        int bestInterval = 20;
        int stopX = start[0];
        int stopY = start[1];
        int distanceX = stop[0] - start[0];
        int distanceY = stop[1] - start[1];
        int distance = Math.abs(distanceX) > Math.abs(distanceY) ? distanceX:distanceY;
        if(Math.abs(distance) < bestInterval){
            interval = 2;
        }else{
            interval = Math.abs(distance)/bestInterval;
        }
        int intervalX = (stop[0] - start[0])/interval;
        int intervalY = (stop[1] - start[1])/interval;
        long eventTime;
        MotionEvent event;
        for(int i=0;i<interval;i++){
            stopX += intervalX;
            stopY += intervalY;
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, stopX, stopY, 0);
            mInstrumentation.sendPointerSync(event);
        }
        Utility.sleep(8);
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, stop[0], stop[1], 0);
        mInstrumentation.sendPointerSync(event);
    }

    public void LongPressRelease(int stop[],long downTime){
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, stop[0], stop[1], 0);
        mInstrumentation.sendPointerSync(event);
    }

    public CellLayout getCurrentCellLayout(){
        return (CellLayout)mWorkspace.getChildAt(mWorkspace.getCurrentPage());
    }

    public void TwoFingerOperation(boolean direct){//true,两指捏合; false,两指分开
        CellLayout layout = getCurrentCellLayout();
        int upPercent = 8;
        int downPercent = 2;
        int bound[] = new int[2];
        int X;
        int Height;
        int[] start1 = new int[2];
        int[] start2 = new int[2];
        int[] stop1 = new int[2];
        int[] stop2 = new int[2];
        int[] middle1 = new int[2];
        int[] middle2 = new int[2];
        layout.getLocationOnScreen(bound);
        X = bound[0] + layout.getMeasuredWidth() / 2;
        Height = layout.getMeasuredHeight();
        start1[0] = start2[0] = stop1[0] = stop2[0] = middle1[0] = middle2[0] = X;
        if(direct){//两指捏合
            start1[1] = bound[1] + Height * upPercent / 10;
            start2[1] = bound[1] + Height * downPercent / 10;
            stop1[1] = stop2[1] = bound[1] + Height * 5/10;
        }else{
            stop1[1] = bound[1] + Height * upPercent / 10;
            stop2[1] = bound[1] + Height * downPercent / 10;
            start1[1] = start2[1] = bound[1] + Height * 5/10;
        }

        MotionEvent event;
        long eventTime;
        long downTime1 = SystemClock.uptimeMillis();
        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime1, eventTime, MotionEvent.ACTION_DOWN, start1[0], start1[1], 0);
        mInstrumentation.sendPointerSync(event);
        long downTime2 = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime2, eventTime, MotionEvent.ACTION_POINTER_DOWN, start2[0], start2[1], 0);
        mInstrumentation.sendPointerSync(event);

        int interval1 = (stop1[1] - start1[1])/5;
        int interval2 = (stop2[1] - start2[1])/5;
        middle1[1] = start1[1];
        middle2[1] = start2[1];
        for(int i=0;i<5;i++){
            middle1[1] += interval1;
            middle2[1] += interval2;
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime1, eventTime, MotionEvent.ACTION_MOVE, middle1[0], middle1[1], 0);
            PointerCoords[] pc = new PointerCoords[2];
            event.addBatch(eventTime, pc, 0);
            event.addBatch(eventTime, middle2[0], middle2[1], 0, 0, 0);
            mInstrumentation.sendPointerSync(event);
            Log.i("chenjian","x="+event.getX()+",y="+event.getY()+",cnt="+event.getPointerCount());
            Log.i("chenjian","y1="+middle1[1]+",y2="+middle2[1]);
            //event = MotionEvent.obtain(downTime2, eventTime, MotionEvent.ACTION_MOVE, middle2[0], middle2[1], 0);
            //mInstrumentation.sendPointerSync(event);
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime1, eventTime, MotionEvent.ACTION_UP, stop1[0], stop1[1], 0);
        mInstrumentation.sendPointerSync(event);
        event = MotionEvent.obtain(downTime2, eventTime, MotionEvent.ACTION_POINTER_DOWN, stop2[0], stop2[1], 0);
        mInstrumentation.sendPointerSync(event);
        Log.i("chenjian","s1x="+start1[0]+",s1y="+start1[1]+",s2x="+start2[0]+",s2y="+start2[1]+
                            ",m1x="+middle1[0]+",m1y="+middle1[1]+",m2x="+middle2[0]+",m2y="+middle2[1]+
                            ",e1x="+stop1[0]+",e1y="+stop1[1]+",e2x="+stop2[0]+",e2y="+stop2[1]);
    }

    public boolean snapToNextPage(){
        long opTime = 400;
        int lastPage = mLauncher.getWorkspace().getCurrentPage();
        int maxPage = mLauncher.getWorkspace().getChildCount();
        int startX = (int) (mScreenWidth*0.9f);
        int endX = (int)(mScreenWidth*0.1f);
        int Y = mScreenHeight - mScreenHeight/4;

        int maxTry = 5;
        int currentPage;
        while(maxTry-- > 0){
            Utility.execShellSwipeCmd(startX, Y ,endX, Y,opTime);
            Utility.sleep(opTime*2);

            currentPage = mLauncher.getWorkspace().getCurrentPage();
            if( (lastPage+1 == currentPage) || ( (lastPage == maxPage -1) && (currentPage == lastPage)) || ((lastPage == maxPage -1) && (currentPage == 0))){
                return true;
            }
            Log.i("chenjian","try+1");
        }
        return false;
    }

    public boolean snapToPrevPage(){
        long opTime = 400;
        int lastPage = mLauncher.getWorkspace().getCurrentPage();
        int maxPage = mLauncher.getWorkspace().getChildCount();
        int startX = (int) (mScreenWidth*0.1f);
        int endX = (int)(mScreenWidth*0.9f);
        int Y = mScreenHeight/2;
        int maxTry = 5;
        int currentPage;
        while(maxTry-- > 0){
            Utility.execShellSwipeCmd(startX, Y ,endX, Y,opTime);
            Utility.sleep(opTime*2);
            currentPage = mLauncher.getWorkspace().getCurrentPage();
            if( (lastPage-1 == currentPage) || ( (lastPage == 0) && (currentPage == lastPage)) || ((lastPage == 0) && (currentPage == maxPage-1))){
                return true;
            }
        }
        return false;
    }

    public void snapToRandomPage(){
        int maxtry = (int) (Math.random()*5);
        int rand;
        do{
            rand = (int) (Math.random()*100 % 2);
            if(rand == 0){
                snapToPrevPage();
            }else{
                snapToNextPage();
            }
        }while(maxtry-- > 0);
    }

    public void snapToPage(int index){
        if(index < 0 || index >= mWorkspace.getChildCount() || index == mWorkspace.getCurrentPage())
            return;
        while(index != mWorkspace.getCurrentPage()){
            if(index < mWorkspace.getCurrentPage()){
                this.snapToPrevPage();
            }else{
                this.snapToNextPage();
            }
        }
    }

    public boolean snapToLastPage(){
        long opTime = 400;
        int maxPage = mLauncher.getWorkspace().getChildCount();
        int currentPage = mLauncher.getWorkspace().getCurrentPage();
        int maxTry = 12;
        int startX = (int) (mScreenWidth*0.9f);
        int endX = (int)(mScreenWidth*0.1f);
        int Y = mScreenHeight/2;
        while(maxTry-- > 0){
            Utility.execShellSwipeCmd(startX, Y ,endX, Y,opTime);
            Utility.sleep(opTime*2);
            currentPage = mLauncher.getWorkspace().getCurrentPage();
            if(currentPage == maxPage -1){
                return true;
            }
        }
        return false;
    }

    public boolean snapToFirstPage(){
        if( mWorkspace.getCurrentPage() == 0)
            return true;
        long opTime = 400;
        int currentPage = mLauncher.getWorkspace().getCurrentPage();
        int maxTry = 12;
        int endX = (int) (mScreenWidth*0.9f);
        int startX = (int)(mScreenWidth*0.1f);
        int Y = mScreenHeight/2;
        while(maxTry-- > 0){
            Utility.execShellSwipeCmd(startX, Y ,endX, Y,opTime);
            Utility.sleep(opTime*2);
            currentPage = mLauncher.getWorkspace().getCurrentPage();
            if(currentPage == 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Snap to a page where the specific type of icons is not less than cnt
     * @param type
     * @param cnt
     */
    public void snapToValidPage(int type,int cnt){
        int maxTry = 24;
        ArrayList<View> mArray;
        boolean bNext = true;
        switch(type){
        case FLAG_TYPE_APP_FOLDER:
            mArray = this.getAllIcons((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
            while(mArray.size() < cnt && maxTry-- > 0){
                if(bNext)
                    snapToNextPage();
                else
                    snapToPrevPage();
                if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
                    bNext = false;
                }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
                    bNext = true;
                }
                mArray = this.getAllIcons((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
            }
            break;
        case FLAG_TYPE_WIDGET_GADGET:
            mArray = this.getAllWidgets((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
            while(mArray.size() < cnt && maxTry-- > 0){
                if(bNext)
                    snapToNextPage();
                else
                    snapToPrevPage();
                if(mLauncher.getWorkspace().getCurrentPage() == (mLauncher.getWorkspace().getChildCount()-1)){
                    bNext = false;
                }else if(mLauncher.getWorkspace().getCurrentPage() == 0){
                    bNext = true;
                }
                mArray = this.getAllWidgets((CellLayout)mLauncher.getWorkspace().getChildAt(mLauncher.getWorkspace().getCurrentPage()));
            }
            break;
        }
    }

    public boolean openHideSeat(final boolean bl){
        mLauncher.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(bl){
                    mLauncher.openHideseat(false);
                }else{
                    mLauncher.hideHideseat(true);
                }
                //mWorkspace.invalidate();
            }
        });
        Utility.sleep(10000);
        int maxtry = 10;
        while(maxtry-- > 0 && mLauncher.isHideseatShowing()!= bl){
            Utility.sleep(1000);
        }
        if(!mLauncher.isHideseatShowing()){
            Log.i(Utility.UTILITY_TAG,"打开/关闭隐藏区失败,bl="+bl);
            return false;
        }
        return true;
    }

    public void enterScreenEdit(){
        mLauncher.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLauncher.enterScreenEditMode();
                //mWorkspace.invalidate();
            }
        });
        Utility.sleep(10000);
    }

    public void exitScreenEdit(){
        mLauncher.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLauncher.exitScreenEditMode(true);
                //mWorkspace.invalidate();
            }
        });
        Utility.sleep(10000);
    }
}
