package com.aliyun.homeshell.test.utility;

/*
 * Created by ruankong.rk on 3/20/15.
 */
import java.io.IOException;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;

public class Utility {
    public static String UTILITY_TAG = "launcher.utility";
    /**
     * 判断当前运行程序
     * 
     * @author ruankong.rk
     * @param context
     *            packageName
     * @return boolean
     */
    public static boolean isRunningApp(Context context, String packageName) {
        boolean isAppRunning = false;
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = am.getRunningTasks(100);
        for (RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(packageName)
                    && info.baseActivity.getPackageName().equals(packageName)) {
                isAppRunning = true;
                // find it, break
                break;
            }
        }

        return isAppRunning;
    }

    /**
     * ִ执行shell swipe命令
     * 
     * @author ruankong.rk
     * @param startX startY endX endY
     * @return
     */
    public static void execShellSwipeCmd(int startX, int startY, int endX,
            int endY) {
        try {
            Process proc = Runtime.getRuntime().exec(
                    "input swipe " + startX + " " + startY + " " + endX + " "
                            + endY);
            proc.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static long DEFAULT_SWIPE_OPERATION_TIME = 400;
    public static boolean execShellSwipeCmd(int startX, int startY, int endX,
            int endY,long opTime) {
        boolean bl = true;
        if(opTime <= 0){
            opTime = DEFAULT_SWIPE_OPERATION_TIME;
        }
        try {
            Process proc = Runtime.getRuntime().exec(
                    "input swipe " + startX + " " + startY + " " + endX + " "
                            + endY);
            proc.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        }
        return bl;
    }

    /**
     * simulate user click event
     * @author chenjian.chenjian
     * @param x coordinate x where to click
     * @param y coordinate y where to click
     */
    public static void execShellTapCmd(int x,int y){
        try {
            Process proc = Runtime.getRuntime().exec(
                    "input tap " + x + " " + y );
            proc.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @author chenjian.chenjian
     * @param event, keyboard event
     * @return if the command is executed successfully
     */
    public static boolean execShellKeyEvent(int event,long sleepTime){
        boolean bl = true;
        try {
            Process proc = Runtime.getRuntime().exec(
                    "input keyevent " + event);
            proc.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        }
        if(sleepTime > 0){
            sleep(sleepTime);
        }
        return bl;
    }
    
    /**
     * @author chenjian.chenjian
     * @param event, keyboard event
     * @return if the command is executed successfully
     */
    public static boolean execShellKeyEventLongPress(int event,long sleepTime){
        boolean bl = true;
        try {
//            String command[] = new String[4];
//            command[0] = "input";
//            command[1] = "keyevent";
//            command[2] = "--longpress";
//            command[3] = "" + event;
            String command = "input keyevent --longpress "+event;
            //String command = "input tap 100 100";

//            Process proc = Runtime.getRuntime().exec("su");
//            proc.waitFor();
//            DataInputStream ise = new DataInputStream(proc.getErrorStream());
//            Log.i("chenjian",ise.readLine());
//            PrintWriter pw = new PrintWriter(proc.getOutputStream());
//            pw.println(command);
//            pw.flush();
//            pw.close();

            Process proc = Runtime.getRuntime().exec(command);
            proc.waitFor();
//            ise = new DataInputStream(proc.getErrorStream());
//            Log.i("chenjian",command+",,"+ise.readLine());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            bl = false;
        }
        if(sleepTime > 0){
            sleep(sleepTime);
        }
        return bl;
    }
    /**
     * @author chenjian.chenjian
     * @param ms,time to sleep
     */
    public static void sleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static int getRandomInRange(int max){
        int rnd = 0;
        int maxtry = 20;
        while(maxtry-- > 0){
            rnd = (int) (Math.random()*max);
            if(rnd >=0 && rnd < max){
                return rnd;
            }
        }
        return 0;
    }

    public static int getRandomNotValue(int max,int value){
        int rnd = 0;
        int maxtry = 20;
        while(maxtry-- > 0){
            rnd = (int) (Math.random()*max);
            if(rnd >=0 && rnd < max && rnd != value){
                return rnd;
            }
        }
        return 0;
    }
}
