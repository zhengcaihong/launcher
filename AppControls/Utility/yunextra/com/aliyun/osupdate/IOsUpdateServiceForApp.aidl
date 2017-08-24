package com.aliyun.osupdate;
import com.aliyun.osupdate.IOsUpdateListener;
interface IOsUpdateServiceForApp
{
    void checkUpdate(String packageName);
    void addAppListener(String packageName,IOsUpdateListener listener);
    void removeAppListener(String packageName);
    int  doUpdate(String packageName);
}
