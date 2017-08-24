package com.aliyun.homeshell.editmode;

import com.aliyun.homeshell.setting.HomeShellSetting;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.utility.FeatureUtility;
///xunhu: the default fades at 2016-08-05 by dsq{{&&
import com.mediatek.common.featureoption.XunhuOption;
///&&}}

public class EffectsPreviewAdapter extends BaseAdapter {
    private TypedArray mImgTypeArray;
    private String[] mEffectTitle;
    private String[] mEffectValue;
    private String mCurrentChoosedEffectValue;
    private SharedPreferences mSharedPref;
    private OnSharedPreferenceChangeListener mSharedPrefListener;
    private LayoutInflater mInflater;
    // private Context mContext;

    public EffectsPreviewAdapter(Context context) {
        Resources res = context.getResources();
        boolean has3D = FeatureUtility.has3dEffect();
        mImgTypeArray = res.obtainTypedArray(has3D ? R.array.editmode_effect_choose_img_3d : R.array.editmode_effect_choose_img);
        mEffectTitle = res.getStringArray(has3D ? R.array.entries_effect_preference_3d : R.array.entries_effect_preference);
        mEffectValue = res.getStringArray(has3D ? R.array.entryvalues_effect_preference_3d : R.array.entryvalues_effect_preference);
        mSharedPref = context.getSharedPreferences("com.aliyun.homeshell_preferences", Context.MODE_PRIVATE);
        ///xunhu: the default fades at 2016-08-05 by dsq{{&&
        //mCurrentChoosedEffectValue = mSharedPref.getString(HomeShellSetting.KEY_PRE_EFFECT_STYLE, "0");
        if (XunhuOption.XUNHU_DSQ_DEFAULT_FADE) {
            mCurrentChoosedEffectValue = mSharedPref.getString(HomeShellSetting.KEY_PRE_EFFECT_STYLE, "9");
        } else {
            mCurrentChoosedEffectValue = mSharedPref.getString(HomeShellSetting.KEY_PRE_EFFECT_STYLE, "0");
        }
        ///&&}}
        mSharedPrefListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (HomeShellSetting.KEY_PRE_EFFECT_STYLE.equals(key)) {
                    mCurrentChoosedEffectValue = sharedPreferences.getString(HomeShellSetting.KEY_PRE_EFFECT_STYLE, "0");
                    notifyDataSetChanged();
                }
            }
        };
        mSharedPref.registerOnSharedPreferenceChangeListener(mSharedPrefListener);
        mInflater = LayoutInflater.from(context);
        // mContext = context;
    }

    @Override
    public int getCount() {
        return mEffectValue.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            ViewHolder vh = new ViewHolder();
            convertView = mInflater.inflate(R.layout.preview_effects_item, parent, false);

            vh.previewImgView = (ImageView) convertView.findViewById(R.id.preview_effects_image);
            Drawable previewImg = mImgTypeArray.getDrawable(position);
            vh.previewImgView.setImageDrawable(previewImg);
            vh.previewImgView.setScaleType(ScaleType.FIT_CENTER);
            vh.titleTextView = (TextView) convertView.findViewById(R.id.preview_effects_title);
            vh.titleTextView.setVisibility(View.VISIBLE);
            vh.titleTextView.setText(mEffectTitle[position]);
            vh.previewChecked = (ImageView) convertView.findViewById(R.id.preview_effects_checked);
            if (isChecked(position)) {
                vh.previewChecked.setVisibility(View.VISIBLE);
            } else {
                vh.previewChecked.setVisibility(View.GONE);
            }
            vh.position = position;
            convertView.setTag(vh);

        } else {
            ViewHolder vh = (ViewHolder) convertView.getTag();
            Drawable previewImg = mImgTypeArray.getDrawable(position);
            vh.previewImgView.setImageDrawable(previewImg);
            vh.titleTextView.setVisibility(View.VISIBLE);
            vh.titleTextView.setText(mEffectTitle[position]);
            vh.position = position;
            if (isChecked(position)) {
                vh.previewChecked.setVisibility(View.VISIBLE);
            } else {
                vh.previewChecked.setVisibility(View.GONE);
            }

        }
        return convertView;
    }

    private boolean isChecked(int position) {
        if (mEffectValue[position].equals(mCurrentChoosedEffectValue)) {
            return true;
        }
        return false;
    }

    public String getCurrentEffectValue() {
        if(TextUtils.isEmpty(mCurrentChoosedEffectValue)) {
            mCurrentChoosedEffectValue = String.valueOf(HomeShellSetting.getSlideEffectMode(LauncherApplication.getContext()));
        }
        return mCurrentChoosedEffectValue;
    }

    public void setEffectValue(int position) {
        mCurrentChoosedEffectValue = mEffectValue[position];
    }

    public void commitEffectValueToPreference() {
        mSharedPref.edit().putString(HomeShellSetting.KEY_PRE_EFFECT_STYLE, mCurrentChoosedEffectValue).commit();
    }

    public String getEffectTitle(int position) {
        return mEffectTitle[position];
    }

    class ViewHolder {
        public int position;
        public ImageView previewImgView;
        public TextView titleTextView;
        public ImageView previewChecked;
    }

}
