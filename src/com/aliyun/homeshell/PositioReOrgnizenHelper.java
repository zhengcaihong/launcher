package com.aliyun.homeshell;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;


public class PositioReOrgnizenHelper {
        /*YUNOS BEGIN*/
        //##date:2014/01/23 ##author:hao.liuhaolh ##BugID:86995
        //browser and image icon position change after fota
	private static final int CELLS_PER_LINE_OLD = 4;
        /*YUNOS END*/
	private static final int CELLS_PER_LINE_NEW = 4;
	private static final int CELLS_PER_SCREEN_OLD = 20;
	private static final int CELLS_PER_SCREEN_NEW = 16;
	
	private static Map<Integer, Integer> mReslutMap;
	
	public static void setAllPositions(Map<Integer, int[]> map) {
		if (map == null) {
			return;
		}
		
		int count = map.size();
		if (count == 0) {
			return;
		}
		Map<Integer, Integer> map1 = new HashMap<Integer, Integer>();
		ArrayList<Integer> keys = new ArrayList<Integer>();
		for (int i : map.keySet()) {
			int[] pos = map.get(i);
                        /*YUNOS BEGIN*/
                        //##date:2014/01/23 ##author:hao.liuhaolh ##BugID:86995
                        //browser and image icon position change after fota
			int key = (pos[0] - 1) * CELLS_PER_SCREEN_OLD +
					pos[2] * CELLS_PER_LINE_OLD + pos[1];
                        /*YUNOS BEGIN*/
			keys.add(key);
			map1.put(key, i);
		}
		Collections.sort(keys);
		mReslutMap = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < count; i ++) {
			Log.d("PositioReOrgnizenHelper", "sxsexe---->setAllPositions id " + (Integer) map1.get(keys.get(i)) + " i " + i);
			mReslutMap.put((Integer) map1.get(keys.get(i)), i);
		}
		
	}
	
	public static int[] getNewPosition(int id) {
		if (mReslutMap == null) {
			return null;
		}
		
		Log.d("PositioReOrgnizenHelper", "sxsexe---->getNewPosition id mReslutMap.containsKey" + mReslutMap.containsKey(id));
		if (!mReslutMap.containsKey(id)) {
			return null;
		}
		
		int pos = mReslutMap.get(id);
		int [] result = new int[3];
		result[0] = pos / CELLS_PER_SCREEN_NEW + 1;
		int temp = pos - CELLS_PER_SCREEN_NEW * (result[0] - 1);
                /*YUNOS BEGIN*/
                //##date:2014/01/23 ##author:hao.liuhaolh ##BugID:86995
                //browser and image icon position change after fota
		result[2] = temp / CELLS_PER_LINE_NEW;
		result[1] = temp % CELLS_PER_LINE_NEW;
                /*YUNOS END*/
		return result;
	}
	
}
