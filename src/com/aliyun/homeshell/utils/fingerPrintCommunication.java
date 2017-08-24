package com.aliyun.homeshell.utils;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.yunos.alifinger.IEventNotifyService;
import java.util.List;

public class fingerPrintCommunication {
    private static final boolean DEBUG = true;
    private static final String TAG = "AliFingerService";

    public static final int FP_EVENT_INTERRUPT = 0;
    public static final int FP_EVENT_DETECTED = 1;
    public static final int FP_EVENT_VERIFY = 2;
    public static final int FP_EVENT_GOT_VERIFIED_FEATURE = 4;
    public static final int FP_EVENT_GOT_IMAGE_FAIL = 8;
    public static final int FP_EVENT_WAITING_INPUT = 16;
    public static final int FP_EVENT_FINGER_LEFT = 32;

    private Context mContext;
    private static boolean mHasRegistered;
    private static long mRegisterTime;
    private fpListener mListener;

    public interface fpListener {
        public void onCatchedEvent(int event, int result);
    }

    private static final int RECEIVE_EVENT = 0;
    private static MainHandler mHandler;
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_EVENT: {
                    if(mListener != null) {
                        mListener.onCatchedEvent(msg.getData().getInt("event", -1), msg.getData().getInt("result", -1));
                    }
                    break;
                }
            }
        }
    }

    public fingerPrintCommunication(Context context, fpListener listener) {
        this(context, listener, false);
    }
    public fingerPrintCommunication(Context context, fpListener listener, boolean acrossProcessCall) {
        super();
        this.mContext = context;
        mListener = listener;
        if(acrossProcessCall) {
            mHandler = null;
        } else {
            mHandler = new MainHandler();
        }
        mHasRegistered = false;
        mRegisterTime = -1;
    }
    public static boolean isAliFingerSupported(Context cxt) {
        if(cxt == null) {
            return false;
        }
        final PackageManager pm = cxt.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            pm.getPackageInfo("com.yunos.alifinger", PackageManager.GET_SERVICES);
            return true;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "finger service is not installed!");
        return false;
    }

    private static int registerCounter = 0;
    private static int unregisterCounter = 0;
    public void registerFpListener(int event, boolean statusbar) {
        registerFpListener(event, statusbar, false);
    }
    public void registerFpListener(int event, boolean statusbar, boolean extra) {
        if(mHasRegistered) {
            return;
        }
        try {
            Log.e(TAG, "activity registerFpListener------------------------registerCounter:" + registerCounter);

            EventNotifyService.setFpEventListener(mHandler, mListener);

            mRegisterTime = SystemClock.uptimeMillis();
            Intent intent = new Intent();
			intent.setAction("com.yunos.fp_service.interruptService_register");
			intent.setPackage("com.yunos.alifinger");
            intent.putExtra("source_component", new ComponentName(mContext, EventNotifyService.class));
            intent.putExtra("fp_event", event);
            intent.putExtra("statusbar", statusbar);
            intent.putExtra("extra", extra);
            intent.putExtra("timestamp",  mRegisterTime);

            int retryCounter = 1;
            while(mContext.startService(intent) == null && retryCounter++ < 3) {
                Log.e(TAG, "activity registerFpListener--------------------startService fail, try again!");
            }
            if(retryCounter >= 3) {
                Log.e(TAG, "activity registerFpListener--------------------startService fail");
                return;
            }
            mHasRegistered = true;
            registerCounter++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterFpListener() {
        if(!mHasRegistered){
            return;
        }
        try {
            Log.e(TAG, "activity unregisterFpListener------------------------unregisterCounter:" + unregisterCounter);

            EventNotifyService.setFpEventListener(null, null);

            Intent intent = new Intent();
			intent.setAction("com.yunos.fp_service.interruptService_unregister");
			intent.setPackage("com.yunos.alifinger");
            intent.putExtra("source_component", new ComponentName(mContext, EventNotifyService.class));
            intent.putExtra("timestamp",  SystemClock.uptimeMillis());
            mContext.startService(intent);
            unregisterCounter++;
            mHasRegistered = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class EventNotifyService extends Service {
        private static final String TAG = "AliFingerService";
        private static fpListener mListener;
        private static Handler mHandler;

        public static void setFpEventListener(Handler handler, fpListener listener) {
            mHandler = handler;
            mListener = listener;
        }
        @Override
        public IBinder onBind(Intent arg0) {
            Log.e(TAG, "EventNotifyService onBind, class:" + EventNotifyService.class.getName());
            return mBinder;
        }
        private final IEventNotifyService.Stub mBinder = new IEventNotifyService.Stub() {
            @Override
            public void onCatchedEvent(int event, int result)
                    throws RemoteException {
                Log.e(TAG, "EventNotifyService onCatchedEvent, event: " + event + ", result:" + result);
                if(mHandler != null) {
                    Message msg = Message.obtain(mHandler, fingerPrintCommunication.RECEIVE_EVENT);
                    Bundle bundle = new Bundle();
                    bundle.putInt("event", event);
                    bundle.putInt("result", result);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                } else if(mListener != null) {
                    mListener.onCatchedEvent(event, result);
                }
            }
            @Override
            public boolean isListenerShowing(){return (mListener != null);}
        };
    }
}
