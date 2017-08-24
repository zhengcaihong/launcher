package com.aliyun.homeshell.favorite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.icon.IconManager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FavoriteAppView extends LinearLayout {
    static String LOG_TAG = "FavoriteAppView";
    private static IconManager mIconManager;;
    private List<LinearLayout> mViewList = new ArrayList<LinearLayout>();
    private int FAVORITE_APP_COUNT = 4;

    public FavoriteAppView(Context context) {
        super(context);
    }

    public FavoriteAppView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        refresh();
    }

    public boolean refresh() {
        this.removeAllViews();
        if (mIconManager == null) {
            mIconManager = ((LauncherApplication) getContext()
                    .getApplicationContext()).getIconManager();
        }
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(Favorites.CONTENT_URI, new String[] {
                    Favorites._ID, Favorites.INTENT
            }, Favorites.FAVORITE_WEIGHT + " > 0", null,
                    Favorites.FAVORITE_WEIGHT + " DESC LIMIT " + FAVORITE_APP_COUNT);
            if (cursor != null && cursor.getCount() > 0) {
                Log.d(LOG_TAG, "The count of favorite app get from db is:"
                        + cursor.getCount());
                long id = 0;
                String title = "";
                String intentDescription = "";
                Intent intent = null;
                Drawable icon = null;
                int pos = 0;
                PackageManager pm = getContext().getPackageManager();
                final int intentIndex = cursor.getColumnIndexOrThrow(Favorites.INTENT);
                final int idIndex = cursor.getColumnIndexOrThrow(Favorites._ID);
                while (cursor.moveToNext()) {
                    try {
                        id = Long.valueOf(cursor.getString(idIndex));
                        intentDescription = cursor.getString(intentIndex);
                        intent = Intent.parseUri(intentDescription, 0);
                        title = pm.getActivityInfo(intent.getComponent(), 0).loadLabel(pm)
                                .toString();
                        icon = mIconManager.getAppUnifiedIcon(intent);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "get the single favorite_app info error", e);
                        continue;
                    }
                    LinearLayout favView = (LinearLayout) LayoutInflater.from(getContext())
                            .inflate(R.layout.favorite_app, null);
                    ImageView favImage = (ImageView) favView.findViewById(R.id.fav_image);
                    TextView favText = (TextView) favView.findViewById(R.id.fav_text);
                    favImage.setImageDrawable(icon);
                    favText.setText(title);
                    favView.setTag(new TagEntity(id, intent, pos++, title, intent.getComponent()
                            .getPackageName()));
                    favView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TagEntity entity = (TagEntity) view.getTag();
                            ((LauncherApplication) getContext().getApplicationContext())
                                    .collectUsageData(entity.getId());
                            Intent intent = entity.getIntent();
                            if (intent != null) {
                                userTrack(entity);
                                try {
                                    getContext().startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(LOG_TAG,
                                            "Start application from favorite_app_view error,intent is:"
                                                    + intent.toString());
                                }
                            }
                        }
                    });
                    mViewList.add(favView);
                }
            } else {
                Log.d(LOG_TAG, "has not get the favorite app from db.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "load the favorite_app error", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        if (mViewList.size() > 0) {
            for (LinearLayout view : mViewList) {
                this.addView(view);
            }
            mViewList.clear();
            return true;
        } else {
            return false;
        }
    }

    void userTrack(TagEntity entity) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("app_name", entity.getAppName());
        param.put("apk_name", entity.getPackageName());
        param.put("pos", String.valueOf(entity.getPos() + 1));
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_FAVORITE_APP, param);
    }

    private class TagEntity {
        long mId;
        Intent mIntent;
        int mPos;
        String mAppName;
        String mPackageName;

        public TagEntity(long id, Intent intent, int pos, String appName, String packageName) {
            this.mId = id;
            this.mIntent = intent;
            this.mAppName = appName;
            this.mPos = pos;
            this.mPackageName = packageName;
        }
        public long getId() {
            return mId;
        }

        public void setId(long mId) {
            this.mId = mId;
        }

        public Intent getIntent() {
            return mIntent;
        }

        public void setIntent(Intent mIntent) {
            this.mIntent = mIntent;
        }

        public void setPos(int pos) {
            mPos = pos;
        }

        public int getPos() {
            return mPos;
        }

        public void setAppName(String appName) {
            mAppName = appName;
        }

        public String getAppName() {
            return mAppName;
        }

        public void setPackageName(String packageName) {
            this.mPackageName = packageName;
        }

        public String getPackageName() {
            return mPackageName;
        }
    }
}
