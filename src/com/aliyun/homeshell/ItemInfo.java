/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.homeshell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aliyun.homeshell.model.LauncherModel;

/**
 * Represents an item in the launcher.
 */
public class ItemInfo {
    
    public static final int NO_ID = -1;
    
    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;
    
    /**
     * One of {@link LauncherSettings.Favorites#ITEM_TYPE_APPLICATION},
     * {@link LauncherSettings.Favorites#ITEM_TYPE_SHORTCUT},
     * {@link LauncherSettings.Favorites#ITEM_TYPE_FOLDER}, or
     * {@link LauncherSettings.Favorites#ITEM_TYPE_APPWIDGET}.
     */
    public int itemType;
    
    public int itemFlags;
    /**
     * The id of the container that holds this item. For the desktop, this will be 
     * {@link LauncherSettings.Favorites#CONTAINER_DESKTOP}. For the all applications folder it
     * will be {@link #NO_ID} (since it is not stored in the settings DB). For user folders
     * it will be the id of the folder.
     */
    public long container = NO_ID;
    
    /**
     * Indicates the screen in which the shortcut appears.
     */
    public int screen = -1;
    
    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    /**
     * Indicates the X position of the associated cell in port mode.
     */
    public int cellXPort = -1;

    /**
     * Indicates the Y position of the associated cell in port mode.
     */
    public int cellYPort = -1;

    /**
     * Indicates the X position of the associated cell in land mode.
     */
    public int cellXLand = -1;

    /**
     * Indicates the Y position of the associated cell in land mode.
     */
    public int cellYLand = -1;

    /**
     * Indicates the X cell span.
     */
    public int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    public int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    public int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    public int minSpanY = 1;
    
    /**
     * Indicates whether this item can be deleted
     */
    public boolean deletable = true;

    /**-
     * Indicates the message number this item can show
     */
    public int messageNum;

    /**
     * Indicates that this item needs to be updated in the db
     */
    boolean requiresDbUpdate = false;

    /**
     * Title of the item
     */
    public CharSequence title;

    /**
     * The position of the item in a drag-and-drop operation.
     */
    int[] dropPos = null;

    public ItemInfo() {
    }

    protected ItemInfo(ItemInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        screen = info.screen;
        itemType = info.itemType;
        container = info.container;
        deletable = info.deletable;
        messageNum = info.messageNum;
        isNew = info.isNew;
        // tempdebug:
        LauncherModel.checkItemInfo(this);
    }

    /** Returns the package name that the intent will resolve to, or an empty string if
     *  none exists. */
    static String getPackageName(Intent intent) {
        if (intent != null) {
            String packageName = intent.getPackage();
            if (packageName == null && intent.getComponent() != null) {
                packageName = intent.getComponent().getPackageName();
            }
            if (packageName != null) {
                return packageName;
            }
        }
        return "";
    }

    /**
     * Write the fields of this item to the DB
     * 
     * @param values
     */
    public void onAddToDatabase(ContentValues values) {
        values.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, itemType);
        values.put(LauncherSettings.Favorites.CONTAINER, container);
        values.put(LauncherSettings.Favorites.SCREEN, screen);
        values.put(LauncherSettings.Favorites.CELLX, cellX);
        values.put(LauncherSettings.Favorites.CELLY, cellY);
        //for pad land mode
        values.put(LauncherSettings.Favorites.CELLXPORT, cellXPort);
        values.put(LauncherSettings.Favorites.CELLYPORT, cellYPort);
        values.put(LauncherSettings.Favorites.CELLXLAND, cellXLand);
        values.put(LauncherSettings.Favorites.CELLYLAND, cellYLand);
        values.put(LauncherSettings.Favorites.SPANX, spanX);
        values.put(LauncherSettings.Favorites.SPANY, spanY);
        values.put(LauncherSettings.Favorites.MESSAGE_NUM, messageNum);
        values.put(LauncherSettings.Favorites.IS_NEW, isNew);
    }

    static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        //BugID:5241319:out of memory exception
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream(size);
        } catch (OutOfMemoryError ex) {
            Log.e("ItemInfo", ex.toString());
            return null;
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

    public static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put(LauncherSettings.Favorites.ICON, data);
        }
    }
    
    public static void writeBitmap(ContentValues values, Drawable bitmap) {
        if (bitmap != null) {
            Bitmap src = null;
            if (bitmap instanceof FastBitmapDrawable) {
                src = ((FastBitmapDrawable) bitmap).getBitmap();
            } else {
                src = ((BitmapDrawable) bitmap.getCurrent()).getBitmap();
            }
            writeBitmap(values,src);
        }
    }

    /**
     * It is very important that sub-classes implement this if they contain any references
     * to the activity (anything in the view hierarchy etc.). If not, leaks can result since
     * ItemInfo objects persist across rotation and can hence leak by holding stale references
     * to the old view hierarchy / activity.
     */
    public void unbind() {
    }

    @Override
    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container
            + " screen=" + screen + " cellX=" + cellX + " cellY=" + cellY +" cellXPort=" 
        	+ cellXPort +" cellYPort=" + cellYPort+" cellXLand=" + cellXLand + " cellYLand=" 
            + cellYLand + " spanX=" + spanX + " spanY=" + spanY+")";
    }

    /*YUNOS BEGIN*/
    //##date:2013/11/18 ##author:hao.liuhaolh
    //add installed app's shortcut to workspace
    public int isNew = 0;

    public boolean isNewItem() {
        return isNew == 1;
    }

    public void setIsNewItem(boolean isNew) {
        this.isNew = isNew ? 1 : 0;
    }
    /*YUNOS END*/
    
    public boolean isDeletable() {
        return deletable;
    }

	public void dumpXY(String msgTag) {
        Log.d(msgTag, title + " cellX " + cellX + " cellY "
				+ cellY + " cellPortX " + cellXPort + " cellPortY " + cellYPort
				+ " cellLandX " + cellXLand + " cellLandY " + cellYLand
				+ " containter " + container + " screen " + screen);
	}

	public void initCellXYOnLoad(int container, int cellXPort, int cellYPort,
			int cellXLand, int cellYLand) {
		switch (container) {
		case LauncherSettings.Favorites.CONTAINER_HOTSEAT: {
			if (LauncherApplication.isInLandOrientation()) {
				this.screen = cellYLand;
			} else {
				this.screen = cellXPort;
			}
		}
		// !!no break
		case LauncherSettings.Favorites.CONTAINER_DESKTOP: {

			if (LauncherApplication.isInLandOrientation()) {
				this.cellX = cellXLand;
				this.cellY = cellYLand;
			} else {
				this.cellX = cellXPort;
				this.cellY = cellYPort;
			}
		}
			break;
		case LauncherSettings.Favorites.CONTAINER_HIDESEAT: {
			// FIXME
			this.cellX = cellXPort;
			this.cellY = cellYPort;
		}
			break;
		default: {
			// In folder, cellX and cellY are always same in land and port
			this.cellX = cellXPort;
			this.cellY = cellYPort;
			this.cellYLand = this.cellYPort = cellYPort;
			this.cellXLand = this.cellXPort = cellXPort;
		}
			break;
		}
	}

	public void setCellXY() {
		if(LauncherApplication.isInLandOrientation()) {
			this.cellX = this.cellXLand;
			this.cellY = this.cellYLand;
		} else {
			this.cellX = cellXPort;
			this.cellY = cellYPort;
		}
	}
	public void setCurrentOrientationXY(int cellX, int cellY) {
	    if(ConfigManager.isLandOrienSupport()) {
	        if(LauncherApplication.isInLandOrientation()) {
                this.cellXLand = cellX;
                this.cellYLand = cellY;
                if(container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    this.cellXPort = cellY;
                    this.cellYPort = cellX;
                }
            } else {
                this.cellXPort = cellX;
                this.cellYPort = cellY;
                if(container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    this.cellXLand = cellY;
                    this.cellYLand = cellX;
                }
            }
        } else {
            this.cellXLand = cellX;
            this.cellYLand = cellY;
            this.cellXPort = cellX;
            this.cellYPort = cellY;
        }
	}

	public void setPosition(ScreenPosition pos) {
        if (ConfigManager.isLandOrienSupport()) {
	        this.screen = pos.s;
	        this.cellXPort = pos.xPort;
	        this.cellYPort = pos.yPort;
	        this.cellXLand = pos.xLand;
	        this.cellYLand = pos.yLand;
	        if (LauncherApplication.isInLandOrientation()) {
                this.cellX = cellXLand;
                this.cellY = cellYLand;
            } else {
                this.cellX = cellXPort;
                this.cellY = cellYPort;
            }
	    } else {
            this.screen = pos.s;
            this.cellX = pos.x;
            this.cellY = pos.y;
            this.cellXPort = pos.x;
            this.cellYPort = pos.y;
            this.cellXLand = pos.xLand;
            this.cellYLand = pos.yLand;
	    }
	}
}
