package com.aliyun.homeshell.backuprestore;

import org.json.JSONException;
import org.json.JSONObject;

public class BackupRecord {
    private JSONObject m_value;
    public BackupRecord(JSONObject value) {
        m_value = value;
    }

    public JSONObject getValue(){
        return m_value;
    }

    public String getField(String fieldName){
        String ret = null;
        try {
            ret = (String) m_value.get(fieldName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public byte[] getBlobField(String fieldName){
        byte[] ret = null;
        try {
            ret = (byte[]) m_value.get(fieldName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setField(String fieldName, String value){
        try {
            m_value.put(fieldName, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setField(String fieldName, byte[] value){
        try {
            m_value.put(fieldName, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
