package com.aliyun.homeshell.gadgets;

interface BoostAnimationListener {
    void onAnimationStart(BoostAnimationPlayer player);
    void onAnimationEnd(BoostAnimationPlayer player);
    void onAnimationCancel(BoostAnimationPlayer player);
    void onAnimationRepeat(BoostAnimationPlayer player);
    void onAnimationUpdate(BoostAnimationPlayer player);
}
