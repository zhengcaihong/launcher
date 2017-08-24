package com.aliyun.homeshell.gadgets;

public class AnimationParams {
    private int mMemoryUsageValue;
    private int mWaterWaveX;
    private int mAnimationOffset;
    private int mStage;
    private boolean mHotseatMode;

    boolean isEqual(int memoryUsage, int waterWaveX, int animationOffsetX,
            boolean hotseatMode, int stage) {
        return mStage == stage && mMemoryUsageValue == memoryUsage &&
                mHotseatMode == hotseatMode && mWaterWaveX == waterWaveX
                && mAnimationOffset == animationOffsetX;
    }

    public void setParams(int memoryUsage, int waterWaveX, int animationOffsetX,
            boolean hotseatMode, int stage) {
        mMemoryUsageValue = memoryUsage;
        mWaterWaveX = waterWaveX;
        mAnimationOffset = animationOffsetX;
        mStage = stage;
        mHotseatMode = hotseatMode;
    }
}
