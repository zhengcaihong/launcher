package com.aliyun.osupdate;
interface IOsUpdateListener
{
    void onGetUpdate(int result,String packageName, String version,String updateDetail);
}
