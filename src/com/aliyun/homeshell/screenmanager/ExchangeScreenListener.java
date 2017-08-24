package com.aliyun.homeshell.screenmanager;

interface ExchangeScreenListener {
    void onDragOver(int dragIndex, int dropIndex);
    void onDrop(int dropIndex);
}
