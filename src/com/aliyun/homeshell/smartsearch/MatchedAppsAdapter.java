package com.aliyun.homeshell.smartsearch;

import java.util.List;

import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.themeutils.ThemeUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class MatchedAppsAdapter extends BaseAdapter {

    private static final int DEFAULT_LIST_COUNT = 1;

    private List<MatchResult> mList;
    private LayoutInflater mInflater;
    private Context mContext;
    private int mCount = DEFAULT_LIST_COUNT;
    private static IconManager mIconManager;

    public MatchedAppsAdapter(Context context, List<MatchResult> list) {
        mList = list;
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCount = mList == null ? DEFAULT_LIST_COUNT : mList.size() + DEFAULT_LIST_COUNT;
        if (mIconManager == null) {
            mIconManager = ((LauncherApplication)mContext.getApplicationContext()).getIconManager();
        }
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public Object getItem(int arg0) {
        return arg0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == mCount - DEFAULT_LIST_COUNT) {
            if (convertView instanceof FrameLayout) {
                return convertView;
            }

            convertView = mInflater.inflate(R.layout.search_app_store, null, false);
            Intent intent = new Intent();
            intent.setClassName("com.aliyun.wireless.vos.appstore",
                    "com.aliyun.wireless.vos.appstore.LogoActivity");
            convertView.setTag(intent);
            return convertView;
        }

        if (convertView == null || convertView instanceof FrameLayout) {
            convertView = mInflater.inflate(R.layout.search_item, null, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.title);
        MatchResult match = mList.get(position);
        CharSequence tvNameText = match.title.replace(" ", "");
        int color = mContext.getResources().getColor(R.color.search_highlight_color);
        SpannableStringBuilder spanName = new SpannableStringBuilder(tvNameText);
        spanName.setSpan(
                new ForegroundColorSpan(color), match.startIndex, match.startIndex + match.matchLength,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        textView.setText(spanName);

        ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);

       imageView.setImageDrawable(getIcon(match));

        TextView subTitle = (TextView) convertView.findViewById(R.id.subtitle);
        subTitle.setText(getTypeStringId(match.type));
        convertView.setTag(match);
        return convertView;
    }

    private int getTypeStringId(int type) {
        switch (type)  {
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                return R.string.search_bookmark_mark;
// remove vp install
//            case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
//                return R.string.search_vp_mark;
            case LauncherSettings.Favorites.ITEM_TYPE_CLOUDAPP:
                return R.string.search_cloudapp_mark;
            case LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION:
                return R.string.search_app_mark;
             default:
                 return R.string.search_other_mark;
        }
    }

    private Drawable getIcon(MatchResult match) {
        Drawable icon = null;

        if (match.type == LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION) {
            icon = mIconManager.getAppUnifiedIcon(match.intent2);
        }

        if (icon == null) {
            try {
                Cursor c = mContext.getContentResolver()
                    .query(LauncherSettings.Favorites.CONTENT_URI,
                    new String[] { "icon"},
                    "_id=" + match.id,
                    null, null);
                if (c != null) {
                    if (c.moveToPosition(0)) {
                        byte[] data = c.getBlob(0);
                        Bitmap originicon = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Bitmap bp = ThemeUtils.buildUnifiedIcon(mContext,
                                originicon, getIconThemeType(match.type));
                        icon = new FastBitmapDrawable(bp);
                    }
                c.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (icon == null) {
            icon = mIconManager.getDefaultIcon();
        }
        return icon;
    }

    private int getIconThemeType(int type) {
        switch (type)  {
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                return ThemeUtils.ICON_TYPE_BROSWER_SHORTCUT;
// remove vp install
//            case LauncherSettings.Favorites.ITEM_TYPE_VPINSTALL:
//                return ThemeUtils.ICON_TYPE_BROSWER_SHORTCUT;
            case LauncherSettings.Favorites.ITEM_TYPE_CLOUDAPP:
                return ThemeUtils.ICON_TYPE_CLOUDAPP;
            case LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION:
                return ThemeUtils.ICON_TYPE_APP;
             default:
                 return ThemeUtils.ICON_TYPE_APP;
        }
    }

}
