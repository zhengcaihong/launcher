package com.aliyun.homeshell.smartlocate;

import java.util.Calendar;
import java.util.TimeZone;

import com.aliyun.homeshell.model.LauncherModel;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class is used to trigger <code>AppLaunchManager</code> to execute
 * daily update of app launch data.
 * @see AppLaunchManager
 * @author zhanggong.zg
 */
public final class AppLaunchUpdater extends BroadcastReceiver {

    static final String PREF_LAST_UPDATE_YEAR = "app_launch_last_update_year";
    static final String PREF_LAST_UPDATE_MONTH = "app_launch_last_update_month";
    static final String PREF_LAST_UPDATE_DAY = "app_launch_last_update_day";

    static final int UPDATE_HOUR_OF_DAY = 3; // 3:00 am

    private static AlarmManager getAlarmManager(Context ctx) {
        return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * App-launch data will be updated at 3:00 am each day. Using
     * alarm service to enable this feature.
     */
    public static void setupAlarm(Context ctx) {
        if (AppLaunchManager.ENABLED) {
            Calendar updateTime = Calendar.getInstance();
            updateTime.setTimeZone(TimeZone.getDefault());
            updateTime.set(Calendar.HOUR_OF_DAY, UPDATE_HOUR_OF_DAY);
            updateTime.set(Calendar.MINUTE, 0);

            Intent intent = new Intent(ctx, AppLaunchUpdater.class);
            PendingIntent recurringIntent = PendingIntent.getBroadcast(ctx,
                    0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarm = getAlarmManager(ctx);
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    updateTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, recurringIntent);
            Log.d(AppLaunchManager.TAG, "updater: alarm set");
        } else {
            Log.d(AppLaunchManager.TAG, "updater: alarm not set (feature disabled)");
        }
    }

    static void cancelAlarm(Context ctx) {
        Intent intent = new Intent(ctx, AppLaunchUpdater.class);
        PendingIntent recurringIntent = PendingIntent.getBroadcast(ctx,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarm = getAlarmManager(ctx);
        alarm.cancel(recurringIntent);
        Log.d(AppLaunchManager.TAG, "updater: alarm is canceled");
    }

    /**
     * Determine whether today is already updated the data or not.
     * If not, update the data in worker thread right now.<p>
     * Called on finish binding.
     */
    public static void updateIfNecessary(final Context context) {
        if (!AppLaunchManager.ENABLED) {
            return;
        }
        boolean updated = isUpdatedToday(context);
        if (updated) {
            Log.d(AppLaunchManager.TAG, "updater: today is already updated");
            return;
        }
        final AppLaunchManager manager = AppLaunchManager.getInstance();
        if (manager == null) {
            Log.w(AppLaunchManager.TAG, "updater: manager is null");
            return;
        }
        if (!manager.isInitialized()) {
            Log.w(AppLaunchManager.TAG, "updater: manager is not inited");
            return;
        }
        Log.d(AppLaunchManager.TAG, "updater: needs update right now");
        LauncherModel.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                manager.dailyUpdate();
                updatePreference(context);
            }
        });
    }

    private static boolean isUpdatedToday(Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return false;
        }
        Calendar time = Calendar.getInstance();
        if (time.get(Calendar.HOUR_OF_DAY) < UPDATE_HOUR_OF_DAY) {
            // wait for alarm.
            return true;
        }
        int savedDay = preference.getInt(PREF_LAST_UPDATE_DAY, 0);
        if (savedDay != time.get(Calendar.DAY_OF_MONTH)) {
            return false;
        }
        int savedMonth = preference.getInt(PREF_LAST_UPDATE_MONTH, 0);
        if (savedMonth != time.get(Calendar.MONTH)) {
            return false;
        }
        int savedYear = preference.getInt(PREF_LAST_UPDATE_YEAR, 0);
        if (savedYear != time.get(Calendar.YEAR)) {
            return false;
        }
        return true;
    }

    private static void updatePreference(Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        Calendar updateTime = Calendar.getInstance();
        int year = updateTime.get(Calendar.YEAR);
        int month = updateTime.get(Calendar.MONTH);
        int day = updateTime.get(Calendar.DAY_OF_MONTH);
        editor.putInt(PREF_LAST_UPDATE_YEAR, year);
        editor.putInt(PREF_LAST_UPDATE_MONTH, month);
        editor.putInt(PREF_LAST_UPDATE_DAY, day);
        editor.commit();
        Log.d(AppLaunchManager.TAG, "updater: last update: " + String.format("%d/%d/%d", year, month + 1, day));
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(AppLaunchManager.TAG, "updater: onReceive in");
        if (AppLaunchManager.ENABLED) {
            final AppLaunchManager manager = AppLaunchManager.getInstance();
            if (manager == null) {
                Log.w(AppLaunchManager.TAG, "updater: manager is null");
                return;
            }
            if (!manager.isInitialized()) {
                Log.w(AppLaunchManager.TAG, "updater: manager is not inited");
                return;
            }
            LauncherModel.runOnWorkerThread(new Runnable() {
                @Override
                public void run() {
                    updatePreference(context);
                    manager.dailyUpdate();
                }
            });
        } else {
            Log.d(AppLaunchManager.TAG, "updater: cancel alarm (feature disabled)");
            cancelAlarm(context);
        }
        Log.d(AppLaunchManager.TAG, "updater: onReceive out");
    }

}
