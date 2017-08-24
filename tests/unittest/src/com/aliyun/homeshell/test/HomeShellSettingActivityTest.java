package com.aliyun.homeshell.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.aliyun.homeshell.Launcher;

public class HomeShellSettingActivityTest extends
        ActivityInstrumentationTestCase2<Launcher> {

    private Instrumentation mInstrumentation;
    private Launcher mLauncher;

    public HomeShellSettingActivityTest() {
        super(Launcher.class);
    }

    //重写setUp方法，在该方法中进行相关的初始化操作
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
