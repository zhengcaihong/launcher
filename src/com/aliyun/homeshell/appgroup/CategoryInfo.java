package com.aliyun.homeshell.appgroup;

public class CategoryInfo {
    public String catID;
    public String supID;
    public String catName;
    public String showName;
    public String supName;

    CategoryInfo(String catName, String catID) {
        this.catName = catName;
        this.catID = catID;
    }

    CategoryInfo(String catName, String catID, String supID, String showName, String supName) {
        this.catName = catName;
        this.catID = catID;
        this.supID = supID;
        this.supName = supName;
        this.showName = showName;
    }

    public String toString() {
        return "catID = " + this.catID + ";supID = " + this.supID + ";catName = " + this.catName
                + ";showName = " + this.showName + ";supName = " + this.supName;
    }

}
