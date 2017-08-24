package com.aliyun.homeshell.setting;

import java.util.HashMap;
import java.util.Map;

import storeaui.app.HWPreferenceActivity;
import storeaui.widget.FooterBar.FooterBarButton;
import storeaui.widget.FooterBar.FooterBarType.OnFooterItemClick;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.aliyun.homeshell.R;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.views.AliRadioButtonPreference;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.utility.utils.ACA;

public class EffectChooseActivity extends HWPreferenceActivity implements
        OnPreferenceClickListener, AliRadioButtonPreference.OnClickListener {

    private PreferenceScreen mContainer;
    private TypedArray mImgTypeArray;
    private String[] mEffectTitle;
    private String[] mEffectValue;

    private SharedPreferences mSharedPref;

    private Resources res;
    private String mCurrentChoosedEffectValue;
    boolean mIsAddPreferencesOK = false;
    private static final String TAG = "EffectChooseActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
        getActionBarView().setTitleColor(getResources().getColor(R.color.setting_title_color));
        setTitle2(getResources().getString(
                R.string.settings_screen_slide_effect));
        showBackKey(true);
        getActionBarView().setBackgroundResource(R.drawable.setting_head);
        ACA.Window.addFlags(this.getWindow(), ACA.Window.FLAG_TRANSLUCENT_STATUS);
        Object systemBarManager = ACA.SystemBarColorManager.newInstance(this);
        if(systemBarManager != null){
            ACA.SystemBarColorManager.setStatusBarColor(systemBarManager, getResources().getColor(R.color.setting_header_color));
            ACA.SystemBarColorManager.setViewFitsSystemWindows(systemBarManager, this, true);
            ACA.SystemBarColorManager.setStatusBarDarkMode(systemBarManager, getWindow(), getResources().getBoolean(R.bool.setting_dark_mode));
        } else {
            ACA.Window.clearFlags(this.getWindow(), ACA.Window.FLAG_TRANSLUCENT_STATUS);
        }
        mSharedPref = getPreferenceManager().getSharedPreferences();
        mContainer = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(mContainer);
        prepareResource();
        addPreferences();
        // addFooter();
    }

    private void prepareResource() {
        res = getResources();
        boolean has3D = FeatureUtility.has3dEffect();
        mImgTypeArray = res.obtainTypedArray(has3D ?
                R.array.effect_choose_img_3d : R.array.effect_choose_img);
        mEffectTitle = res.getStringArray(has3D ?
                R.array.entries_effect_preference_3d : R.array.entries_effect_preference);
        mEffectValue = res.getStringArray(has3D ?
                        R.array.entryvalues_effect_preference_3d : R.array.entryvalues_effect_preference);
    }

    private void addPreferences() {
        mIsAddPreferencesOK = false;
        mCurrentChoosedEffectValue = mSharedPref.getString(
                HomeShellSetting.KEY_PRE_EFFECT_STYLE, "0");
        boolean isChecked = false;
        int length = mImgTypeArray.length();
        Log.d(TAG, "---------mCurrentChoosedEffectValue----------- " + mCurrentChoosedEffectValue);
        for (int i = 0; i < length; i++) {
            if (mCurrentChoosedEffectValue.equals(mEffectValue[i])) {
                isChecked = true;
            } else {
                isChecked = false;
            }
            addRadioButton(mEffectTitle[i], mImgTypeArray.getDrawable(i),
                    mEffectValue[i], isChecked);
        }
        mIsAddPreferencesOK = true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return true;
    }

    @Override
    protected void onStop() {
        UserTrackerHelper
                .pageLeave(UserTrackerMessage.MSG_LAUNCHER_SETTING_LAUNCHER_SETTING_EFFECTS);
        super.onStop();
    }

    private void addFooter() {
        ViewGroup root = (ViewGroup) getListView().getParent();
        FooterBarButton mFooterBarButton = new FooterBarButton(this);
        mFooterBarButton.addItem(0,
                res.getString(R.string.settings_effect_cancel_btn));
        mFooterBarButton.setOnFooterItemClick(new OnFooterItemClick() {
            @Override
            public void onFooterItemClick(View arg0, int arg1) {
                finish();
            }
        });
        mFooterBarButton.getLayoutParams().height = res
                .getDimensionPixelSize(R.dimen.setting_effect_cancel_btn_height);
        mFooterBarButton.updateItems();
        root.addView(mFooterBarButton);
    }

    private void addRadioButton(String title, Drawable icon, String key,
            boolean isChecked) {
        Log.d(TAG, "---------addRadioButton----------- title isChecked " + title+" - "+isChecked);
        AliRadioButtonPreference preference = new AliRadioButtonPreference(this);
        preference.setTitle(title);
        preference.setKey(key);
        preference.setIcon(icon);
        preference.setChecked(isChecked);
        preference.setOnClickListener(this);
        mContainer.addPreference(preference);
    }

    private void updateRadioButtons(String activeKey) {
        Log.d(TAG, "---------updateRadioButtons----------- activeKey " + activeKey);
        for (String cs : mEffectValue) {
            if (activeKey.equals(cs)) {
                ((AliRadioButtonPreference) findPreference(cs))
                        .setChecked(true);
            } else {
                ((AliRadioButtonPreference) findPreference(cs))
                        .setChecked(false);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(AliRadioButtonPreference preference) {
        if (!mIsAddPreferencesOK) {
            return;
        }
        mCurrentChoosedEffectValue = preference.getKey();
        updateRadioButtons(mCurrentChoosedEffectValue);
        mSharedPref
                .edit()
                .putString(HomeShellSetting.KEY_PRE_EFFECT_STYLE,
                        mCurrentChoosedEffectValue).commit();
        Map<String, String> param = new HashMap<String, String>();
        param.put(UserTrackerMessage.Key.POSITION, mCurrentChoosedEffectValue);
        UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_EFFECT_RESULT, param);
    }

}
