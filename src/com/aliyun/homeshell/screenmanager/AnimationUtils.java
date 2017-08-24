package com.aliyun.homeshell.screenmanager;

import com.aliyun.homeshell.LauncherAnimUtils;
import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.view.View;

class AnimationUtils {

    static Animator createTranslateAnimator(View v,
            float srcX, float dstX, float srcY, float dstY) {
        return LauncherAnimUtils.ofPropertyValuesHolder(
                v, PropertyValuesHolder.ofFloat(ViewHidePropertyName.TRANSLATION_X, srcX, dstX),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.TRANSLATION_Y, srcY, dstY));
    }

    static Animator createUnfolderAnimator(View v,
            float srcDegree, float dstDegree, float privotX,
            float privotY, float srcTX,
            float dstTX, float srcTY, float dstTY) {
        v.setPivotX(privotX);
        v.setPivotY(privotY);
        return LauncherAnimUtils.ofPropertyValuesHolder(
                v, PropertyValuesHolder.ofFloat(ViewHidePropertyName.ROTATION, srcDegree, dstDegree),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.TRANSLATION_X, srcTX, dstTX),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.TRANSLATION_Y, srcTY, dstTY));
    }

    static Animator createFadeAnimator(View v,
            float srcAlpha, float dstAlpha) {
        return LauncherAnimUtils.ofPropertyValuesHolder(
                v, PropertyValuesHolder.ofFloat(ViewHidePropertyName.ALPHA,
                        srcAlpha, dstAlpha));
    }

    private static class ViewHidePropertyName {
        static final String ROTATION = "rotation";
        static final String TRANSLATION_X = "translationX";
        static final String TRANSLATION_Y = "translationY";
        static final String ALPHA = "alpha";
    }
}
