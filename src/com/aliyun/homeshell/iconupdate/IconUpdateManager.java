package com.aliyun.homeshell.iconupdate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import com.aliyun.homeshell.LauncherApplication;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Parcelable;
import android.util.Log;

/**
 * the main functions of this class are: handle icon update intent including add
 * and cancel; load icon update infos from db when homeshell start and keep
 * these infos in a Map; remove info if the app is uninstalled; check whether the
 * update time expired
 * 
 * @author liuhao
 *
 */
public final class IconUpdateManager {
	private static IconUpdateManager mIconUpdateMgr = new IconUpdateManager();

	private HashMap<ComponentName, UpdateIconInfo> mUpdateIconMap = new HashMap<ComponentName, UpdateIconInfo>();
	private IconUpdateDBHelper mIconUpdateDB = null;
	private SQLiteDatabase mDatabase = null;

	public static final String ID = "_id";

	// intent extra and database column
	public static final String COMPONENTNAME = "componentName";
	public static final String ICON = "icon";
	public static final String STARTTIME = "startTime";
	public static final String ENDTIME = "endTime";

	// icon update type: add or cancel
	public static final String IUTYPE = "iutype";
	public static final String IUTYPE_ADD = "add";
	public static final String IUTYPE_CANCEL = "cancel";

	public static final String ACTION_ICON_UPAATE = "com.aliyun.homeshell.action.ICON_UPDATE";

	private static final String TAG = "IconUpdateManager";

	public static IconUpdateManager getInstance() {
		return mIconUpdateMgr;
	}

	private IconUpdateManager() {
		mIconUpdateDB = new IconUpdateDBHelper(LauncherApplication.getContext());
	}

	/**
	 * handle the icon update intent and parse intent to create a UpdateIconInfo
	 * if the intent type is add and the intent is correct; Or remove the info
	 * if the intent type is cancel. update the info into db and list, notify UI
	 * to update the icon
	 * 
	 * @param intent
	 *            The icon update intent to be handled
	 * 
	 */
	public void handleIconUpdateInent(Intent intent) {
		Log.d(TAG, "intent is " + intent.toString());
		// check the intent extra first
		if (!isIconUpdateIntentCorrect(intent)) {
			Log.d(TAG, "handleIconUpdateInent intent is incorrect");
			return;
		}

		// check the app is installed
		ComponentName cmpName = ComponentName.unflattenFromString(intent.getStringExtra(COMPONENTNAME));
		if (!isAppInstalled(cmpName)) {
			return;
		}

		boolean success = false;
		String iUType = intent.getStringExtra(IUTYPE);
		if (iUType.equals(IUTYPE_ADD)) {
			if (isDateTimeCorrect(intent)) {
				success = handleIconUpdateAdd(intent);
			}
		} else {
			success = handleIconUpdateCancel(intent);
		}
		if (success) {
			// update the icon in UI
			updateIconInUI(cmpName);
		}
	}

	private boolean isDateTimeCorrect(Intent intent) {
		Boolean result = false;
		String dateStr = intent.getStringExtra(ENDTIME);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = null;
		try {
			date = sdf.parse(dateStr);
			Date curDate = new Date(System.currentTimeMillis());
			if (date.compareTo(curDate) > 0) {
				result = true;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG, "isDateTimeCorrect result is " + result);
		return result;
	}

	private boolean isAppInstalled(ComponentName cmpName) {
		boolean result = false;
		Context context = LauncherApplication.getLauncher();
		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo appInfo = pm.getApplicationInfo(cmpName.getPackageName(), 0);
			result = true;
		} catch (NameNotFoundException ex) {
			Log.d(TAG, "the app isn't installed");
		}
		return result;
	}

	private boolean handleIconUpdateAdd(Intent intent) {
		boolean success = false;
		ComponentName cmpName = ComponentName.unflattenFromString(intent.getStringExtra(COMPONENTNAME));
		UpdateIconInfo info = null;
		if (mUpdateIconMap.containsKey(cmpName)) {
			info = mUpdateIconMap.get(cmpName);
			success = changeIconUpdateInfoFromIntent(info, intent);
			if (success) {
				changeUpdateIconInfoInDB(info);
			}
		} else {
			info = createIconUpdateInfoFromIntent(intent);
			if (info != null) {
				success = true;
				mUpdateIconMap.put(info.getComponentName(), info);
				addUpdateIconInfoInDB(info);
			}
		}
		return success;
	}

	private boolean updateIconInUI(ComponentName cmp) {
		return LauncherApplication.getLauncher().getModel().UpdateItemIconByComponentName(cmp);
	}

	private boolean handleIconUpdateCancel(Intent intent) {
		boolean result = false;
		ComponentName cmp = ComponentName.unflattenFromString(intent.getStringExtra(COMPONENTNAME));
		if (mUpdateIconMap.containsKey(cmp)) {
			UpdateIconInfo info = mUpdateIconMap.remove(cmp);
			if (info != null) {
				removeUpdateIconInfoInDB(info);
			}
			result = true;
		}
		return result;
	}

	private boolean isIconUpdateIntentCorrect(Intent intent) {
		if (intent == null) {
			return false;
		}

		String iUType = intent.getStringExtra(IUTYPE);
		if (iUType == null || iUType.isEmpty()) {
			return false;
		}

		if (!(iUType.equals(IUTYPE_ADD)) && !(iUType.equals(IUTYPE_CANCEL))) {
			return false;
		}

		String cmpName = intent.getStringExtra(COMPONENTNAME);
		if (cmpName == null || cmpName.isEmpty()) {
			return false;
		}

		ComponentName cmp = ComponentName.unflattenFromString(cmpName);
		if (cmp.getPackageName() == null || cmp.getPackageName().isEmpty()) {
			return false;
		}

		if (iUType.equals(IUTYPE_ADD)) {
			String endTime = intent.getStringExtra(ENDTIME);
			if (endTime == null || endTime.isEmpty()) {
				return false;
			}

			if (intent.hasExtra(ICON) == false) {
				return false;
			}
		}
		return true;
	}

	private Bitmap loadIconFromIntent(Intent intent) {
		Parcelable bitmap = intent.getParcelableExtra(ICON);
		if (bitmap instanceof Bitmap) {
			return (Bitmap) bitmap;
		}
		return null;
	}

	private UpdateIconInfo createIconUpdateInfoFromIntent(Intent intent) {
		UpdateIconInfo info = null;
		Bitmap icon = loadIconFromIntent(intent);
		if (icon == null) {
			return null;
		}
		ComponentName cmpName = ComponentName.unflattenFromString(intent.getStringExtra(COMPONENTNAME));
		info = new UpdateIconInfo(cmpName, intent.getStringExtra(STARTTIME), intent.getStringExtra(ENDTIME), icon);
		return info;
	}

	private boolean changeIconUpdateInfoFromIntent(UpdateIconInfo info, Intent intent) {
		Bitmap icon = loadIconFromIntent(intent);
		if (icon == null) {
			return false;
		}
		info.setComponentName(ComponentName.unflattenFromString(intent.getStringExtra(COMPONENTNAME)));
		info.setIcon(icon);
		info.setStartTime(intent.getStringExtra(STARTTIME));
		info.setEndTime(intent.getStringExtra(ENDTIME));
		return true;
	}

	public void loadUpdateIconInfoFromDB() {
		SQLiteDatabase db = openDatabase();
		if(null == db){
		    Log.e(TAG,"loadUpdateIconInfoFromDB error at openDatabase");
		    return;
		}
		Cursor cr = db.query("iconupdate", null, null, null, null, null, null);
		try {
			final int cmpNmaeIndex = cr.getColumnIndexOrThrow(COMPONENTNAME);
			final int iconIndex = cr.getColumnIndexOrThrow(ICON);
			final int startTimeIndex = cr.getColumnIndexOrThrow(STARTTIME);
			final int endTimeIndex = cr.getColumnIndexOrThrow(ENDTIME);

			while (cr.moveToNext()) {
				UpdateIconInfo info = new UpdateIconInfo();
				String cmpStr = cr.getString(cmpNmaeIndex);
				if (cmpStr == null) {
					continue;
				}
				info.setComponentName(ComponentName.unflattenFromString(cmpStr));
				info.setStartTime(cr.getString(startTimeIndex));
				info.setEndTime(cr.getString(endTimeIndex));
				byte[] data = cr.getBlob(iconIndex);
				try {
					info.setIcon(BitmapFactory.decodeByteArray(data, 0, data.length));
				} catch (ArrayIndexOutOfBoundsException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				if (info.mUpdateIcon != null) {
					mUpdateIconMap.put(info.mComponentName, info);
				}
			}
		} finally {
			cr.close();
		}
		closeDatabase();
	}

	public void addUpdateIconInfoInDB(UpdateIconInfo info) {
		ContentValues values = new ContentValues();
		info.onAddToDatabase(values);
		SQLiteDatabase db = openDatabase();
		if(null == db){
		    Log.e(TAG,"addUpdateIconInfoInDB error at openDatabase");
		    return;
		}
		db.insert("iconupdate", null, values);
		closeDatabase();
	}

	private void removeUpdateIconInfoInDB(UpdateIconInfo info) {
		SQLiteDatabase db = openDatabase();
		if(null == db){
		    Log.e(TAG,"removeUpdateIconInfoInDB error at openDatabase");
		    return;
		}
		db.delete("iconupdate", "componentName=?", new String[] { info.mComponentName.flattenToString() });
		closeDatabase();
	}

	public void changeUpdateIconInfoInDB(UpdateIconInfo info) {
		ContentValues values = new ContentValues();
		info.onAddToDatabase(values);
		SQLiteDatabase db = openDatabase();
		if(null == db){
		    Log.e(TAG,"changeUpdateIconInfoInDB error at openDatabase");
		    return;
		}
		db.update("iconupdate", values, "componentName=?", new String[] { info.mComponentName.flattenToString() });
		closeDatabase();
	}

	private synchronized SQLiteDatabase openDatabase() {
		if (mDatabase == null || !mDatabase.isOpen()) {
		    try{
		        mDatabase = mIconUpdateDB.getWritableDatabase();
		    }catch (SQLiteException e) {
                Log.e(TAG, "Failed at getWritableDatabase : " + e.getMessage());
                mDatabase = null;
            }
		}
		return mDatabase;
	}

	private synchronized void closeDatabase() {
	    if( null != mDatabase)
	        mDatabase.close();
	}

//	public static void testForIconUpdateInfoAdd(ComponentName component) {
//		Intent intent = new Intent(ACTION_ICON_UPAATE);
//		Context context = LauncherApplication.getContext();
//		intent.putExtra(COMPONENTNAME, component.flattenToString());
//		intent.putExtra(IUTYPE, IUTYPE_ADD);
//		intent.putExtra(ENDTIME, "2018-10-01 00:00:00");
//
//		Bitmap bmp = Bitmap.createBitmap(100, 100, Config.ARGB_8888);
//		for (int i = 0; i < 100; i++) {
//			for (int j = 0; j < 100; j++) {
//				bmp.getPixel(i, j);
//				bmp.setPixel(i, j, Color.RED);
//			}
//		}
//		intent.putExtra(ICON, bmp);
//		Log.d(TAG, "liuhao send broadcast");
//		context.sendBroadcast(intent);
//	}

	public Bitmap getIconByComponent(ComponentName cmp) {
		UpdateIconInfo info = mUpdateIconMap.get(cmp);
		if (info != null) {
			return info.mUpdateIcon;
		}
		return null;
	}

	public void clearUninstallInfo(ComponentName cmp) {
		if (cmp == null) {
			return;
		}
		if (mUpdateIconMap.containsKey(cmp)) {
			removeUpdateIconInfoInDB(mUpdateIconMap.remove(cmp));
		}
	}

	public void findAndRemoveExpireForCurrentItems() {
		ArrayList<UpdateIconInfo> delList = new ArrayList<UpdateIconInfo>();
		for(UpdateIconInfo info:mUpdateIconMap.values()) {
			String dateStr = info.mEndTime;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			Date date = null;
			try {
				date = sdf.parse(dateStr);
				Date curDate = new Date(System.currentTimeMillis());
				if (date.compareTo(curDate) <= 0) {
					//the update icon is expired, delete it
					delList.add(info);
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (UpdateIconInfo delInfo:delList) {
			removeUpdateIconInfoInDB(mUpdateIconMap.remove(delInfo.mComponentName));
			LauncherApplication.getLauncher().getModel().UpdateItemIconByComponentName(delInfo.mComponentName);
		}
	}
}