package com.aliyun.homeshell.gadgets;

import android.util.Pair;

final class AnimationParamsProvider {
    private static AnimationParamsProvider mProvider;
    private int mWaterWaveX;
    private int mAnimationOffset;
    private int mStage;
    private int mMemoryUsageRate;

    private AnimationParamsProvider() {}

    static AnimationParamsProvider getProvider() {
        if (mProvider == null) {
            synchronized (AnimationParamsProvider.class) {
                if (mProvider == null) {
                    mProvider = new AnimationParamsProvider();
                }
            }
        }
        return mProvider;
    }

    void buildParams(Pair<Integer, Integer> pair) {
        mStage = pair.first;
        switch (mStage) {
        case GadgetsConsts.NORMAL_STAGE:
            mWaterWaveX = 0;
            mAnimationOffset = 0;
            mMemoryUsageRate = 0;
            break;
        case GadgetsConsts.ANIMATION_STAGE_1:
            mWaterWaveX = pair.second;
            mMemoryUsageRate = (GadgetsConsts.FULL_PROGRESS - pair.second);
            mAnimationOffset = 0;
            break;
        case GadgetsConsts.ANIMATION_STAGE_2:
            mAnimationOffset = pair.second;
            mWaterWaveX = 0;
            mMemoryUsageRate = 0;
            break;
        case GadgetsConsts.ANIMATION_STAGE_3:
            mWaterWaveX = GadgetsConsts.FULL_PROGRESS - pair.second;
            mMemoryUsageRate = pair.second;
            mAnimationOffset = pair.second;
            if (mAnimationOffset > GadgetsConsts.FULL_PROGRESS / 2) {
                mAnimationOffset -= GadgetsConsts.FULL_PROGRESS;
            }
            break;
        default:
            throw new RuntimeException("Invalid animation stage");
        }
    }

    int getWaterWaveX() {
        return mWaterWaveX;
    }

    int getAnimationOffset() {
        return mAnimationOffset;
    }

    int getStage() {
        return mStage;
    }

    int getMemoryUsageRate() {
        return mMemoryUsageRate;
    }
}
