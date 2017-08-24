package com.aliyun.homeshell;

public class ScreenPosition {
    public long c;   //container
    public int s;   //screen index
    public int x;   //x index
    public int y;   //y index

    // ##date:2015/02/11 ##author:zhanggong.zg ##BugID:5613700
    // Modified to support pad orientation
    public int xLand;
    public int yLand;
    public int xPort;
    public int yPort;

    public ScreenPosition(long c, int s, int x, int y) {
        this(s, x, y);
        this.c = c;
    }

    public ScreenPosition(int s, int x, int y) {
        this.s = s;
        this.x = x;
        this.y = y;
        this.xPort = x;
        this.yPort = y;
        this.xLand = x;
        this.yLand = y;
    }

    //for pad land mode
    public ScreenPosition(int s, int xport, int yport, int xland, int yland) {
        this.s = s;
        this.xPort = xport;
        this.yPort = yport;
        this.xLand = xland;
        this.yLand = yland;
        if(LauncherApplication.isInLandOrientation()) {
            this.x = this.xLand;
            this.y = this.yLand;
        } else {
            this.x = this.xPort;
            this.y = this.yPort;
        }
    }

    @Override
    public String toString() {
        return "ScreenPosition [s=" + s + ", x=" + x + ", y=" + y + ", xLand=" + xLand + ", yLand=" + yLand + ", xPort=" + xPort
                + ", yPort=" + yPort + "]";
    }
    
}
