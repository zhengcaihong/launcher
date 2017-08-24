package com.aliyun.homeshell.appgroup;

public class AppInfo {
    public String pkgName;
    public String catID;

    AppInfo(String pkgName, String categoryID) {
        this.pkgName = pkgName;
        this.catID = categoryID;
    }

    public String toString() {
        return "pkgName = " + this.pkgName + ";catID = " + this.catID;
    }

}
