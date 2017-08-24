
package com.aliyun.homeshell;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.aliyun.utility.utils.ACA;


public class CardNotificationPanelView extends LinearLayout {
    public static final boolean DEBUG_GESTURES = true;
    public static final String TAG = "CardNotificationPanelView";
    private static final String CLOSE_CARD_ACTION = "com.aliyun.homeshell.CLOSE_CARD";
    private static final float RATIO = 0.685f;
    private LinearLayout mLatestItems;
    ArrayList<StatusBarNotification> mNotifications;
    HashMap<Integer, StatusBarNotificationHolder> mNotificationHolderMap;
    private Calendar mCalendar;
    private Context mContext;
    private int mDate;
    private String mPackage;
    private CharSequence mAppName;
    private TextView mAppNameView;
    private ImageView mIcon;
    private Drawable mDrawable;

    private LinearLayout mSetReadedButton;
    private GadgetCardHelper mCardHelper;
    private View mSplit;

    private static final HashSet<String> sNotAutoCloseEmptyCard = new HashSet<String>();
    static {
        sNotAutoCloseEmptyCard.add("com.tencent.mobileqq");
        sNotAutoCloseEmptyCard.add("com.alibaba.android.babylon");
    }

    public CardNotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mLatestItems = (LinearLayout) findViewById(R.id.latestItems);
        mCalendar = Calendar.getInstance();
        mDate = mCalendar.get(Calendar.DAY_OF_MONTH);
        mAppNameView = (TextView) findViewById(R.id.app_name);
        mIcon = (ImageView) findViewById(R.id.app_icon);
        mIcon.setImageResource(R.drawable.ic_default_icon);
        mSplit = findViewById(R.id.notification_split);
        mSetReadedButton = (LinearLayout) findViewById(R.id.set_notification_readed);
        mSetReadedButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                synchronized (mNotifications) {
                    Iterator<Integer> iter = mNotificationHolderMap.keySet().iterator();
                    while (iter.hasNext()) {
                        Object key = iter.next();
                        onNotificationClear(mNotificationHolderMap.get(key).sbn);
                    }
                }
                if (sNotAutoCloseEmptyCard.contains(mPackage)) {
                    Intent intent = new Intent(CLOSE_CARD_ACTION);
                    mContext.sendBroadcast(intent);
                }
                if (sNotAutoCloseEmptyCard.contains(mPackage)) {
                    Intent intent = new Intent(CLOSE_CARD_ACTION);
                    mContext.sendBroadcast(intent);
                }

                // add user track by wenliang.dwl
                Map<String, String> mp = new HashMap<String, String>();
                mp.put("PkgName", mPackage);
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CARD_READ, mp);
            }

        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initNotificationView();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setPackage(String pkg){
        mPackage = pkg;
    }

    public void setAppName(CharSequence appName) {
        mAppName = appName;
    }

    public void setIcon(Drawable drawable) {
        mDrawable = drawable;
    }

    public void setHelper(GadgetCardHelper helper) {

        mCardHelper = helper;
    }

    private void initNotificationView() {
        if (null != mAppName) {
            mAppNameView.setText(mAppName);
        }
        if (null != mDrawable) {
            mIcon.setImageDrawable(mDrawable);
        }
        synchronized (mNotifications) {
            for (StatusBarNotification sbn : mNotifications) {
                addNotificationView(sbn);
            }
        }
        toggleMarkAsRead();
    }

    private void toggleMarkAsRead() {
        if (null == mNotifications)
            return;
        boolean visible = false;
        synchronized (mNotifications) {
            for (StatusBarNotification sbn : mNotifications) {
                if (sbn.isClearable()) {
                    visible = true;
                    break;
                }
            }
        }
        int flag = visible ? VISIBLE : GONE;
        mSetReadedButton.setVisibility(flag);
        mSplit.setVisibility(flag);
    }

    public void addNotification(final StatusBarNotification sbn) {
        post(new Runnable() {
            public void run() {
                addNotificationView(sbn);
                toggleMarkAsRead();
            }
        });
    }

    private void addNotificationView(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        CharSequence titles = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(titles) || !TextUtils.isEmpty(text)) {
            inflateNormalViews(sbn);
        } else {
            inflateCustomViews(sbn);
        }
        if (mNotifications != null && !mNotifications.contains(sbn)) {
            mNotifications.add(sbn);
        }

        if (mNotifications != null && !mNotifications.contains(sbn)) {
            mNotifications.add(sbn);
        }

    }

    public void removeNotification(StatusBarNotification sbn) {
        int key = Arrays.hashCode(new Object[] {
                sbn.getPackageName(), sbn.getId(), sbn.getTag()
        });
        if (mNotificationHolderMap.containsKey(key)) {
            final View view = mNotificationHolderMap.get(key).view;
            mNotificationHolderMap.remove(key);
            post(new Runnable() {
                public void run() {
                    mLatestItems.removeView(view);
                    toggleMarkAsRead();
                }
            });
        }
        if (mNotificationHolderMap.size() < 1 &&
                !sNotAutoCloseEmptyCard.contains(sbn.getPackageName())) {
            Intent intent = new Intent(CLOSE_CARD_ACTION);
            mContext.sendBroadcast(intent);
        }

    }

    private void inflateNormalViews(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout itemLayout = (RelativeLayout) inflater.inflate(
                R.layout.card_notification_item, null);
        TextView title = (TextView) itemLayout.findViewById(R.id.title);
        TextView time = (TextView) itemLayout.findViewById(R.id.time);
        TextView text = (TextView) itemLayout.findViewById(R.id.sub_text);
        CharSequence titles = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence subText = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(titles) && !TextUtils.isEmpty(subText)) {
            title.setText(titles);
            text.setText(subText);
        } else if (TextUtils.isEmpty(subText)) {
            title.setText(mAppName);
            text.setText(titles);
        } else {
            title.setText(mAppName);
            text.setText(subText);
        }
        boolean isShowWhen = extras.getBoolean(Notification.EXTRA_SHOW_WHEN);
        long when = sbn.getNotification().when;
        if (isShowWhen && when != 0) {
            time.setVisibility(VISIBLE);
            time.setText(getTime(when));
        } else {
            time.setVisibility(GONE);
        }
        PendingIntent contentIntent = sbn.getNotification().contentIntent;
        if (contentIntent != null) {
            final View.OnClickListener listener = new NotificationClicker(contentIntent, sbn);
            itemLayout.setOnClickListener(listener);
        } else {
            itemLayout.setOnClickListener(null);
        }
        int key = Arrays.hashCode(new Object[] {
                sbn.getPackageName(), sbn.getId(), sbn.getTag()
        });
        if (mNotificationHolderMap.containsKey(key)) {
            View tmpView = mNotificationHolderMap.get(key).view;
            mLatestItems.removeView(tmpView);
        }
        mLatestItems.addView(itemLayout);
        StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder();
        sbnHolder.view = itemLayout;
        sbnHolder.sbn = sbn;
        mNotificationHolderMap.put(key, sbnHolder);
    }

    private void inflateCustomViews(StatusBarNotification sbn) {
        RemoteViews contentView = sbn.getNotification().contentView;
        if (null == contentView) {
            return;
        }
        int minHeight =
                mContext.getResources().getDimensionPixelSize(R.dimen.card_notification_min_height);
        int maxHeight =
                mContext.getResources().getDimensionPixelSize(R.dimen.card_notification_max_height);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup itemLayout = (ViewGroup) inflater.inflate(
                R.layout.card_custom_notification_item, mLatestItems, false);
        ViewGroup adaptive = (ViewGroup) itemLayout.findViewById(R.id.adaptive);
        View contentViewLocal = null;
        try {
            contentViewLocal = contentView.apply(mContext, adaptive);

        } catch (RuntimeException e) {
            final String ident = sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId());
            Log.e(TAG, "couldn't inflate view for notification " + ident, e);
            return;
        }
        PendingIntent contentIntent = sbn.getNotification().contentIntent;
        if (contentIntent != null) {
            final View.OnClickListener listener = new NotificationClicker(contentIntent, sbn);
            itemLayout.setOnClickListener(listener);
        } else {
            itemLayout.setOnClickListener(null);
        }
        contentViewLocal.setPivotX(0);
        contentViewLocal.setScaleX(RATIO);
        contentViewLocal.setScaleY(RATIO);
        Object params = ACA.SizeAdaptiveLayout.LayoutParams.getInstance(contentViewLocal.getLayoutParams());
        ACA.SizeAdaptiveLayout.LayoutParams.setMinHeight(params, (int) (minHeight * RATIO));
        ACA.SizeAdaptiveLayout.LayoutParams.setMaxHeight(params, (int) (maxHeight * RATIO));
        adaptive.addView(contentViewLocal, (android.view.ViewGroup.LayoutParams)params);
        setTextViewShadow(adaptive);
        int key = Arrays.hashCode(new Object[] {
                sbn.getPackageName(), sbn.getId(), sbn.getTag()
        });
        if (mNotificationHolderMap.containsKey(key)) {
            View tmpView = mNotificationHolderMap.get(key).view;
            mLatestItems.removeView(tmpView);
        }
        mLatestItems.addView(itemLayout);
        StatusBarNotificationHolder sbnHolder = new StatusBarNotificationHolder();
        sbnHolder.view = itemLayout;
        sbnHolder.sbn = sbn;
        mNotificationHolderMap.put(key, sbnHolder);
    }

    private void setTextViewShadow(ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        int color = mContext.getResources().getColor(R.color.common_text_color_shadow);
        for (int i = 0; i < count; i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof ViewGroup) {
                setTextViewShadow((ViewGroup) view);
            } else if (view instanceof TextView) {
                TextView v = ((TextView) view);
                if (v.getShadowRadius() == 0)
                    v.setShadowLayer(1, 0, 1, color);
            }
        }
    }

    private String getTime(long when) {

        Time t = new Time();
        t.set(when);
        t.second = 0;
        Date dateTime = new Date(t.year - 1900, t.month, t.monthDay, t.hour, t.minute);
        mCalendar.setTimeInMillis(when);
        DateFormat format;
        String time;
        int date = mCalendar.get(Calendar.DAY_OF_MONTH);
        if (date != mDate) {
            format = getDateFormat();

        } else {
            format = getTimeFormat();
        }
        time = format.format(dateTime);
        return time;
    }

    private DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(mContext);
    }

    private DateFormat getDateFormat() {
        String format = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.DATE_FORMAT);
        if (format == null || "".equals(format)) {
            return DateFormat.getDateInstance(DateFormat.SHORT);
        } else {
            try {
                return new SimpleDateFormat(format);
            } catch (IllegalArgumentException e) {
                // If we tried to use a bad format string, fall back to a
                // default.
                return DateFormat.getDateInstance(DateFormat.SHORT);
            }
        }
    }

    public void initStatusBarNotificationList(ArrayList<StatusBarNotification> notifications) {
        mNotificationHolderMap = new HashMap<Integer, StatusBarNotificationHolder>();
        mNotifications = notifications;
    }

    private void onNotificationClear(StatusBarNotification sbn) {
        if (null == sbn)
            return;
        mCardHelper.cancelNotification(sbn);
    }

    private class NotificationClicker implements View.OnClickListener {
        private PendingIntent mIntent;
        private StatusBarNotification mSbn;

        public NotificationClicker(PendingIntent intent, StatusBarNotification sbn) {
            mIntent = intent;
            mSbn = sbn;
        }

        public void onClick(View v) {

            if (mIntent != null) {
                int[] pos = new int[2];
                v.getLocationOnScreen(pos);
                Intent overlay = new Intent();
                overlay.setSourceBounds(
                        new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
                try {
                    mIntent.send(mContext, 0, overlay);
                    if ((mSbn.getNotification().flags & Notification.FLAG_AUTO_CANCEL) > 0) {
                        onNotificationClear(mSbn);
                    }

                } catch (PendingIntent.CanceledException e) {
                    // the stack trace isn't very helpful here. Just log the
                    // exception message.
                    Log.w(TAG, "Sending contentIntent failed: " + e);
                }
            }

            // add user track by wenliang.dwl
            Map<String, String> mp = new HashMap<String, String>();
            mp.put("PkgName", mPackage);
            UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_CARD_CLICKITEM, mp);
        }

    }

    private class StatusBarNotificationHolder {
        public StatusBarNotification sbn;
        public View view;
    }
}
