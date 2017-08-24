package com.aliyun.homeshell.lifecenter;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.aliyun.homeshell.R;

public class LifeCenterReceiver extends BroadcastReceiver {
    private static final String TAG = "LifeCenterReceiver";

    private final String ACTION_DELETE_ACCOUNT = "com.aliyun.xiaoyunmi.action.DELETE_ACCOUNT";
    private final String ACTION_LOGIN_ACCOUNT = "com.aliyun.xiaoyunmi.action.AYUN_LOGIN_BROADCAST";
    private final String ACTION_UPDATE_ACCOUNT = "com.aliyun.xiaoyunmi.action.UPDATE_ACCOUNT";
    private final String ACTION_SYNC_NOTIFY = "com.aliyun.action.RECEIVE_SYNC_NOTIFY";

    private final String ACTION_PACKAGE_REPLACE = "android.intent.action.PACKAGE_REPLACED";

    private Context mContext;

    public LifeCenterReceiver(Context context) {
        mContext = context;
    }

    private LifecenterListener mListener;
    public static interface LifecenterListener {
        void onLifeCardInstalled();
        void onAccountChanged(boolean login);
    }

    public void setListener(LifecenterListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.e(TAG, "onReceive intent is null.");
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "onReceive action : " + action);

        if (Intent.ACTION_PACKAGE_REPLACED.equals(action) || Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                String pkg = data.getSchemeSpecificPart();
                if (CardBridge.LIFECENTER_PKG_NAME.equals(pkg)) {
                    if (isSystemApp(context, pkg)) {
                        if (mListener != null && Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                            mListener.onLifeCardInstalled();
                        } else {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    }
                }
            }
            return;
        }
        if (ACTION_DELETE_ACCOUNT.equals(action)) {
            if (mListener != null) {
                mListener.onAccountChanged(false);
            }
        } else if (ACTION_LOGIN_ACCOUNT.equals(action)) {
            if (mListener != null) {
                mListener.onAccountChanged(true);
            }
        }
    }

    private static int getAlertDialogTheme(Context context) {
        int theme = context.getResources().getIdentifier("Theme.Ali.Dialog.Alert", "style", "hwdroid");
        if (theme <= 0) {
            theme = android.app.AlertDialog.THEME_HOLO_LIGHT;
        }

        return theme;
    }

    private boolean isSystemApp(Context context, String pkgName) {
        boolean systemApp = false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(pkgName, 0);
            if (info != null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                systemApp = true;
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed in isSystemApp : " + e.getMessage());
        }

        Log.d(TAG, "isSystemApp systemApp : " + systemApp);
        return systemApp;
    }

    public void register() {
        Context context = mContext;
        IntentFilter accountFilter = new IntentFilter();

        accountFilter.addAction(ACTION_DELETE_ACCOUNT);
        accountFilter.addAction(ACTION_LOGIN_ACCOUNT);
        accountFilter.addAction(ACTION_UPDATE_ACCOUNT);
        accountFilter.addAction(ACTION_SYNC_NOTIFY);

        context.registerReceiver(this, accountFilter,
                "com.aliyun.account.permission.SEND_MANAGE_DATA", null);

        IntentFilter installFilter = new IntentFilter();
        installFilter.addAction(ACTION_PACKAGE_REPLACE);
        installFilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        installFilter.addDataScheme("package");
        context.registerReceiver(this, installFilter);
    }

    public void unRegister() {
        mContext.unregisterReceiver(this);
    }

    private AlertDialog mDialog;
    protected void showPackageUpateDialog() {
        if (mDialog != null) {
            return;
        }

        Context context = mContext;
        Resources res = context.getResources();
        int theme = getAlertDialogTheme(context);

        res.getString(R.string.uninstall_app_confirm);
        Builder builder = new AlertDialog.Builder(context, theme).setTitle("")
                .setMessage(res.getString(R.string.lifecenter_update_msg))
                .setPositiveButton(res.getString(R.string.confirm_btn_label), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }).setNegativeButton(res.getString(R.string.cancel_btn_label), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

        mDialog = builder.create();
        mDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialog = null;
            }
        });

        mDialog.show();
    }

    public void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
