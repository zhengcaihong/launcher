/*
 * Copyright 2011 Alibaba Group
 *
 */

package com.aliyun.app;

import java.io.Serializable;

/** 
 * @file      AliyunNotification.java
 * This file include AliyunNotification definition. 
 *
 * @author    
 * @since     1.0.0.0
 * @version   1.0.0.0
 * @date      2010-12-23
 *
 * \if TOSPLATFORM_CONFIDENTIAL_PROPRIETARY
 * ============================================================================\n
 *\n
 *           Copyright (c) 2010 .  All Rights Reserved.\n
 *\n
 * ============================================================================\n
 *\n
 *                              Update History\n
 *\n
 * Author (Name[WorkID]) | Modification | Tracked Id | Description\n
 * --------------------- | ------------ | ---------- | ------------------------\n
 * \endif
 *
 * <tt>
 *\n
 * Release History:\n
 *\n
 * Author (Name[WorkID]) | ModifyDate | Version | Description \n
 * --------------------- | ---------- | ------- | -----------------------------\n
 * daiping.zhao[32590] | 2010-12-23 | 1.0.0.0 | Initial created. \n
 *\n
 * </tt>
 */
//=============================================================================
//                                  IMPORT PACKAGES
//=============================================================================

//=============================================================================
//                                 CLASS DEFINITIONS
//=============================================================================
/**
 * This class is the interface between application and Notification Manager.
 * Any application wants to update the icons on status bar need to refer to this class.
 * 
 * Application must use pre-defined notification id to update icon. Or notification manager 
 * will refuse the request. And only special package can update status bar icons, those 
 * package's name also have been included in this class.
 * 
 * 
 */
public class AliyunNotification
{
    
     // Package name Definition. NotificationManager will only insert 
     // the following package related notification icon into status bar.
	
    /** 
     * Alarm package name, Currently, the notification invoked by android framework.
     */
    public final static String PACKAGE_NAME_ALARM = "android";
    /**
     *  Email package name
     */
    public final static String PACKAGE_NAME_EMAIL = "com.aliyun.mobile.email";
    /**
     * Ali Wangwang package name
     */
    public final static String PACKAGE_NAME_IM = "com.aliyun.mobile.im";
    /**
     *  Message package name
     */
    public final static String PACKAGE_NAME_MMS = "com.android.mms";
    /**
     *  Music package name
     */
    public final static String PACKAGE_NAME_MUSIC = "com.android.music";
    /** 
     * Phone package name
     */
    public final static String PACKAGE_NAME_PHONE_SDK20 = "com.android.phone";
    public final static String PACKAGE_NAME_PHONE_SDK21 = "com.android.server.telecom";
    /**
     *  Sync package name
     */
    public final static String PACKAGE_NAME_SYNC = "android";
	/**
     *  usb connected package name
     */
    public final static String PACKAGE_NAME_USB = "android";
	/**
     *  audio recorder package name
     */
    public final static String PACKAGE_NAME_AUDIO_RECORDER = "com.aliyun.note";
	public final static String PACKAGE_NAME_BROWSER = "com.aliyun.mobile.browser";
	public final static String PACKAGE_NAME_BLUETOOTH = "com.android.bluetooth";
	public final static String PACKAGE_NAME_SYSTEM_UPDATE = "android";
	public final static String PACKAGE_NAME_SYSTEM_UPDATE_DOWNLOAD = "android";
	


    /**
     * Notification ID definition. Only the following notification can be inserted
     * to status bar
     */

	/**
     * Call forward notification id.
     */
	//DUAL_CARD_START
    //yi.ruany modify
    public final static int ID_CALL_FORWARD = 6300;
	
	/*TIANYURD liugang 20120208 modfiy for status bar forwarded call*/
    public final static int ID_CALL_FORWARD_SUB2 = 6301;
    //DUAL_CARD_END
	/**
     * Alarm notification id.
     */
    public final static int ID_ALARM = 6302;
	 /**
     * Sync notification id.
     */
    public final static int ID_SYNC =  6304;

    /**
     * new notification id.
     */
    public final static int ID_NEW_NOTIFICATION = 6305;

    /**
     * music notification id.
     */
    public final static int ID_MUSIC_NOTIFICATION = 6303;
	 

	/**
		*  system update notification id 
		*/
	   public final static int ID_SYSTEM_UPDATE = 5300;

	/**
	
	 * brief Music notification id.
	 */
	public final static int ID_MUSIC = 5301;

	/**
	 *	Audio recorder notification id 
	 */
	public final static int ID_AUDIO_RECORDER = 5302;
	/**
	 *	browser download notification id 
	 */	 
	public final static int ID_BROWSER_DOWNLOAD = 5303;
	/**
	 *	bluetooth transfer notification id 
	 */
	public final static int ID_BLUETOOTH_TRANSFER = 5304;
	
	/**
	 *	system update file download notification id 
	 */
	public final static int ID_SYSTEM_UPDATE_FILE_DOWNLOAD = 5306;    
    /**
     * Email notification id.
     */
    public final static int ID_EMAIL = 5307;
    /**
     * IM notification id.
     */
    public final static int ID_IM = 5308;
    /**
     * Unread Message notification id.
     */
    public final static int ID_UNREAD_MESSAGE = 5309;
    
    /**
     * Missed call notification id.
     */
    public final static int ID_MISSED_CALL = 5310;
    
   
    
    /**
     * Phone talking notification id.
     */    
    public final static int ID_CALL_TALKING = 5311;

	/**
	 *	USB connected notification id 
	 */
	 public final static int ID_USB_CONNECTED = 5312;



	

	/**
	 *	VPN connected notification id 
	 */
	 public final static int ID_VPN_CONNECTED = 6000;
	
    
    /**
     * Notification manager will broadcast intent when it receive
     * Icon notification.
     */
    public final static String ACTION_APPLICATION_NOTIFICATION 
        = "com.aliyun.action.application.notification";
    /**
     * The data name with intent INTENT_APPLICATION_NOTIFICATION.
     */
    public final static String DATA_NAME_NOTIFICATION = "data";    
    
    /**
     * Notify type.
     */
    public final static int TYPE_NOTIFICATION_NOTIFY = 0;   
    
    /**
     * Cancel type.
     */
    public final static int TYPE_NOTIFICATION_CANCEL = 1;   

    /**
     * The Icon data type definition, which will be broadcast as  
     * INTENT_APPLICATION_NOTIFICATION's data.
     */
    public static class IconData implements Serializable
    {
        static final long serialVersionUID = 1;
        /**
         * The name of package invoking the notification.
         */
        public String packageName;
        /**
         * The activity class name corresponding to application icon in Launcher,
         * the icon will be updated by the notification. 
         */
        public String className;
        /**
         * Notification id
         */
        public int id;
        /**
         * Icon resource id. 
         */
        public int icon;
        /**
         * Event number.
         */
        public int num;
        /**
         * The time when the notification triggered.
         */
        public long when;

        /**
         * notify type:  notify an event or cancel event.
         * It can be TYPE_NOTIFICATION_NOTIFY or TYPE__NOTIFICATION_CANCEL;
         */
        public int type;
    };
}
