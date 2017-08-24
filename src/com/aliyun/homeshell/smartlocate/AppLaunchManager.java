package com.aliyun.homeshell.smartlocate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.smartlocate.AppLaunchDBHelper.RecordVisitor;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.Time;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import android.util.SparseArray;

/**
 * This class caches all app-launch data from database, and maintains the consistency
 * between cache and database.<p>
 * The public methods of this class are thread-safe.<p>
 * @see http://docs.alibaba-inc.com:8090/pages/viewpage.action?pageId=258942252
 * @author zhanggong.zg
 */
public final class AppLaunchManager {

    static final String TAG = "AppLaunchManager";

    public static final boolean ENABLED = true;

    //// Algorithm Parameters ////

    /**
     * Weaken old record weight by multiplying this factor.
     * @see #dailyUpdate()
     */
    static final float DAILY_WEAKEN_FACTOR = 0.8f;

    /**
     * Delete records of which count is less than this threshold.
     * @see #dailyUpdate()
     */
    static final float MIN_COUNT_THRESHOLD = (float) Math.pow(DAILY_WEAKEN_FACTOR, 7); // 7 days

    /**
     * The time span that used to generate suggested app list.
     * @see #getSuggestedApps(int, Collection)
     */
    static final int DEFAULT_QUERY_TIME_SPAN = 10; // +/-5 minutes

    /**
     * The ideal row count of database. If row count is too much larger than
     * this value, homeshell might encounter performance issue. There are
     * two mechanisms to reduce database size: {@link #dailyUpdate()} and
     * {@link #coalesce(int)}.<p>
     * This value is not a hard limit, and it's sufficient for regular daily
     * use.
     */
    static final int IDEAL_DATABASE_SIZE = 500; // 500 rows

    /**
     * This value is used in {@link #coalesce(int)} algorithm. If time difference
     * of two records are larger than this threshold, they are not going to be
     * merge into one record.
     * @see #computeEndIndexForCoalescing(SparseArray, int, int)
     */
    static final int MAX_COALESCE_TIME_SPAN = 60; // 60 minutes

    //// Singleton ////

    private static AppLaunchManager sInstance = null;

    public static void init(SQLiteOpenHelper helper) {
        sInstance = new AppLaunchManager(helper);
    }

    public static AppLaunchManager getInstance() {
        return sInstance;
    }

    //// Instance Members ////

    private final AppLaunchDBHelper mDbHelper;
    private final Map<Long, SparseArray<Float>> mCachedData; // id -> [count, count, ... , count]
    private final ConcurrentMap<Long, ShortcutInfo> mShortcutMap; // id -> ShortcutInfo
    private boolean mInited = false;
    private int mInsertionCount = 0;
    private final LastLaunchTimeHelper mLastLaunchTimeHelper;

    @SuppressLint("UseSparseArrays")
    private AppLaunchManager(SQLiteOpenHelper helper) {
        mDbHelper = new AppLaunchDBHelper(helper);
        mCachedData = new HashMap<Long, SparseArray<Float>>();
        mShortcutMap = new ConcurrentHashMap<Long, ShortcutInfo>();
        mLastLaunchTimeHelper = new LastLaunchTimeHelper(helper);
    }

    /**
     * Loads all database records to cache.<p>
     * This method is called in LauncherModel.LoaderTask.loadWorkspace(),
     * and should be executed in worker thread.
     */
    public synchronized void initialize(final Map<Long, ItemInfo> allItemMap) {
        Log.d(TAG, "initialize in");
        mCachedData.clear();
        mShortcutMap.clear();
        final Set<Long> invalidIDs = new HashSet<Long>();
        final int[] recordCount = { 0 };

        // traverse database records, load to cache
        if (ENABLED) {
            try {
                mDbHelper.traverse(new RecordVisitor() {
                    @Override
                    public void visitRecord(long id, int time, float count) {
                        Long idObj = id;
                        if (invalidIDs.contains(idObj)) {
                            return;
                        }
                        ItemInfo info = allItemMap.get(idObj);
                        if (!isValidItem(info)) {
                            invalidIDs.add(idObj);
                            return;
                        }
                        mShortcutMap.put(idObj, (ShortcutInfo) info);
                        SparseArray<Float> array = mCachedData.get(idObj);
                        if (array == null) {
                            array = new SparseArray<Float>();
                            mCachedData.put(idObj, array);
                        }
                        array.put(time, count);
                        recordCount[0]++;
                    }
                });
            } catch (SQLiteException ex) {
                Log.e(TAG, "initialize error", ex);
            }
        }
        mInsertionCount = recordCount[0];
        Log.d(TAG, "initialize record count: " + recordCount[0]);

        // delete invalid items
        if (!invalidIDs.isEmpty()) {
            try {
                mDbHelper.transaction(new Runnable() {
                    @Override
                    public void run() {
                        for (Long id : invalidIDs) {
                            mDbHelper.delete(id);
                        }
                    }
                });
                Log.d(TAG, "initialize delete invalid record: " + invalidIDs.size());
            } catch (SQLiteException ex) {
                Log.e(TAG, "initialize delete error", ex);
            }
            invalidIDs.clear();
        }

        if (ENABLED) {
            mLastLaunchTimeHelper.initialize();
        }

        mInited = true;
        Log.d(TAG, "initialize out");
    }

    public synchronized boolean isInitialized() {
        return mInited;
    }

    public LastLaunchTimeHelper geLastLaunchTimeHelper() {
        return mLastLaunchTimeHelper;
    }

    /**
     * This method is called when user click an icon, and should be
     * executed in worker thread.
     */
    public synchronized void increaseCount(ShortcutInfo item) {
        Log.d(TAG, "increaseCount in");
        if (ENABLED && isValidItem(item)) {
            Log.d(TAG, "increaseCount item: " + item.title);
            mShortcutMap.put(item.id, item);
            increaseCount(item.id);
            mLastLaunchTimeHelper.updateLastLaunchTime(item.id);
        }
        Log.d(TAG, "increaseCount out");
    }

    /**
     * Increases launch count by 1 for given id, at current time.
     */
    void increaseCount(long id) {
        increaseCount(id, getCurrentTime());
    }

    /**
     * Increases launch count by 1 for given id and time.
     */
    void increaseCount(long id, int time) {
        SparseArray<Float> array = mCachedData.get(id);
        Float count = array != null ? array.get(time) : null;
        if (count == null) {
            // update cache
            if (array == null) {
                array = new SparseArray<Float>();
                mCachedData.put(id, array);
            }
            array.put(time, 1.0f);
            // update database
            try {
                mDbHelper.insert(id, time, 1.0f);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "increaseCount error DB is full", e);
                throw e;
            } catch (SQLiteException ex) {
                Log.e(TAG, "increaseCount error", ex);
            }
            // coalesce data if necessary
            if (++mInsertionCount > IDEAL_DATABASE_SIZE) {
                coalesce(2);
                mInsertionCount = 0;
            }
        } else {
            // update cache
            count += 1.0f;
            array.put(time, count);
            // update database
            try {
                mDbHelper.update(id, time, count);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "increaseCount error DB is full", e);
                //throw e;
            } catch (SQLiteException ex) {
                Log.e(TAG, "increaseCount error", ex);
            }
        }
    }

    /**
     * Retrieves suggested app list based on app-launch data. The result list is sorted
     * by relevance. (The first element is most relevant.)<p>
     * This method should be called in UI thread.
     * @param resultLimit the maximum size of result list
     * @param exclude excludes from result, can be null
     * @return the result list
     */
    public List<ShortcutInfo> getSuggestedApps(int resultLimit, Collection<ShortcutInfo> exclude) {
        return getSuggestedApps(getCurrentTime(), DEFAULT_QUERY_TIME_SPAN, resultLimit, exclude);
    }

    /**
     * Retrieves suggested app list based on app-launch data. The result list is sorted
     * by relevance. (The first element is most relevant.)<p>
     * This method should be called in UI thread.
     * @param time the index of minute in a day
     * @param timeSpan the time span (the unit is minute)
     * @param resultLimit the maximum size of result list
     * @param exclude excludes from result, can be null
     * @return the result list
     */
    public List<ShortcutInfo> getSuggestedApps(final int time, int timeSpan, int resultLimit, Collection<ShortcutInfo> exclude) {
        Log.d(TAG, "getSuggestedApps in");
        if (timeSpan <= 0) return Collections.emptyList();
        if (resultLimit <= 0) return Collections.emptyList();

        // determine time range
        Pair<Integer, Integer> range = getTimeRange(time, timeSpan);
        int startTime = range.first, endTime = range.second;
        Log.d(TAG, "getSuggestedApps startTime=" + startTime + " endTime=" + endTime);

        // compute launch count for each app, in time range
        final LongSparseArray<Float> map = new LongSparseArray<Float>();
        ArrayList<Long> idList = new ArrayList<Long>();
        final Set<Long> moreCandidates = new HashSet<Long>();
        synchronized (this) {
            for (Entry<Long, SparseArray<Float>> entry : mCachedData.entrySet()) {
                Long id = entry.getKey();
                if (exclude != null) {
                    for (ShortcutInfo info : exclude) {
                        if (info.id == id.longValue()) {
                            continue;
                        }
                    }
                }
                SparseArray<Float> array = entry.getValue();
                float count = getCount(array, startTime, endTime);
                if (count > 0.0f) {
                    map.put(id, count);
                } else {
                    moreCandidates.add(id);
                }
            }
        }
        Log.d(TAG, "getSuggestedApps count computed: " + map.size());

        // sort app by launch count
        Collections.sort(idList, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                float cmp = map.get(id2) - map.get(id1);
                return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
            }
        });
        map.clear();
        Log.d(TAG, "getSuggestedApps sorted: " + idList.size());

        // if result is not enough, find more apps
        if (idList.size() < resultLimit) {
            idList.addAll(getMoreSuggestedApps(moreCandidates, resultLimit - idList.size(), time));
            moreCandidates.clear();
        }

        // map id to item
        List<ShortcutInfo> result = new ArrayList<ShortcutInfo>(idList.size());
        int cnt = Math.min(idList.size(), resultLimit);
        for (int i = 0; i < cnt; i++) {
            ShortcutInfo info = mShortcutMap.get(idList.get(i));
            if (info != null) {
                result.add(info);
            } else {
                Log.w(TAG, "getSuggestedApps unknown id: " + idList.get(i));
            }
        }
        Log.d(TAG, "getSuggestedApps result count: " + result.size());
        Log.d(TAG, "getSuggestedApps out");
        return result;
    }

    private synchronized List<Long> getMoreSuggestedApps(Collection<Long> candidates, int limit, int time) {
        Log.d(TAG, "getMoreSuggestedApps in");
        // compute nearest launch time for each candidate
        final LongSparseArray<Pair<Integer, Integer>> pairMap = new LongSparseArray<Pair<Integer, Integer>>();
        ArrayList<Long> idList = new ArrayList<Long>();
        for (Long id : candidates) {
            SparseArray<Float> array = mCachedData.get(id);
            if (array != null) {
                Pair<Integer, Integer> pair = getNearestLaunchTime(array, time);
                if (pair != null) {
                    pairMap.put(id, pair);
                }
            }
        }

        // sort candidates by time difference
        Collections.sort(idList, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                return pairMap.get(id1).second - pairMap.get(id2).second;
            }
        });

        // limit result size
        if (idList.size() > limit) {
            idList = (ArrayList<Long>) idList.subList(0, limit);
        }

        if (idList.isEmpty()) {
            Log.d(TAG, "getMoreSuggestedApps out: return empty list");
            return idList;
        }

        // determine time range
        int distance = pairMap.get(idList.get(idList.size() - 1)).second;
        Pair<Integer, Integer> range = getTimeRange(time, distance * 2);
        int startTime = range.first, endTime = range.second;
        Log.d(TAG, "getMoreSuggestedApps startTime=" + startTime + " endTime=" + endTime);

        // compute launch count for each app in new time range
        final LongSparseArray<Float> countMap = new LongSparseArray<Float>();
        for (Long id : idList) {
            float count = getCount(mCachedData.get(id), startTime, endTime);
            countMap.put(id, count);
        }

        // sort apps by launch count
        Collections.sort(idList, new Comparator<Long>() {
            @Override
            public int compare(Long id1, Long id2) {
                float cmp = countMap.get(id2) - countMap.get(id1);
                return cmp < 0 ? -1 : (cmp > 0 ? 1 : 0);
            }
        });

        Log.d(TAG, "getMoreSuggestedApps out: " + idList.size());
        return idList;
    }

    private Pair<Integer, Integer> getTimeRange(int time, int timeSpan) {
        int startTime = 0, endTime = 0;
        if (timeSpan == 0) {
            startTime = endTime = time;
        } else if (timeSpan == 1) {
            startTime = time;
            endTime = (time + 1) % 1440;
        } else if (timeSpan < 1440 / 2) {
            startTime = (time + 1440 - timeSpan / 2) % 1440;
            endTime = (time + timeSpan / 2) % 1440;
        } else {
            startTime = 0;
            endTime = 1440;
        }
        return new Pair<Integer, Integer>(startTime, endTime);
    }

    /**
     * Returns the nearest launch time around specified <code>time</code>;
     * Or returns <code>null</code> if no launch records at all.<p>
     * The first value in returning pair is the nearest launch time, and the
     * second is the distance to <code>time</code>.
     */
    private Pair<Integer, Integer> getNearestLaunchTime(SparseArray<Float> array, final int time) {
        int size = array.size();
        if (size == 0) {
            // app never launched
            return null;
        } else if (size == 1) {
            // launched only one time
            int t = array.keyAt(0);
            return new Pair<Integer, Integer>(t, Math.min(getTimeDistance(time, t),
                                                          getTimeDistance(t, time)));
        } else if (array.indexOfKey(time) < 0) {
            // temporarily insert a pair
            array.put(time, 0.0f); // the implementation is optimized by binary-search
            int index = array.indexOfKey(time);
            size = array.size();
            // assertTrue: index >= 0 && size >= 3;
            final int leftTime = array.keyAt(index > 0 ? (index - 1) : (size - 1));
            final int rightTime = array.keyAt(index < size - 1 ? (index + 1) : 0);
            int leftSpan = getTimeDistance(leftTime, time);
            int rightSpan = getTimeDistance(time, rightTime);
            array.remove(time);
            return leftSpan < rightSpan ? new Pair<Integer, Integer>(leftTime, leftSpan) :
                                          new Pair<Integer, Integer>(rightTime, rightSpan);
        } else {
            return new Pair<Integer, Integer>(time, 0);
        }
    }

    private int getTimeDistance(int left, int right) {
        if (left <= right) {
            return right - left;
        } else {
            return 1440 + right - left;
        }
    }

    float getCount(SparseArray<Float> array, int startTime, int endTime) {
        if (startTime == endTime) return 0.0f;
        if (array == null) return 0.0f;
        float count = 0.0f;
        if (startTime < endTime) {
            for (int time = startTime; time < endTime; time++) {
                Float value = array.get(time);
                if (value != null) {
                    count += value.floatValue();
                }
            }
        } else {
            for (int time = startTime; time < 1440; time++) {
                Float value = array.get(time);
                if (value != null) {
                    count += value.floatValue();
                }
            }
            for (int time = 0; time < endTime; time++) {
                Float value = array.get(time);
                if (value != null) {
                    count += value.floatValue();
                }
            }
        }
        return count;
    }

    public void dailyUpdate() {
        dailyUpdate(DAILY_WEAKEN_FACTOR, MIN_COUNT_THRESHOLD, true);
    }

    public void dailyUpdate(final float weakenFactor, final float minThreshold, boolean allowCoalescing) {
        Log.d(TAG, "dailyUpdate in");
        if (ENABLED) {
            final int[] recordCount = { 0 };
            mDbHelper.transaction(new Runnable() {
                @Override
                public void run() {
                    synchronized (AppLaunchManager.this) {
                        int countBefore = 0, countAfter = 0;
                        Iterator<Entry<Long, SparseArray<Float>>> itr = mCachedData.entrySet().iterator();
                        try {
                            while (itr.hasNext()) {
                                Entry<Long, SparseArray<Float>> entry = itr.next();
                                SparseArray<Float> array = entry.getValue();
                                int size = array.size();
                                countBefore += size;
                                countAfter += size;
                                for (int i = 0; i < size; i++) {
                                    int time = array.keyAt(i);
                                    float newCount = array.valueAt(i) * weakenFactor;
                                    if (newCount > minThreshold) {
                                        // update data
                                        array.put(time, newCount);
                                        mDbHelper.update(entry.getKey(), time, newCount);
                                    } else {
                                        // less than threshold, delete data
                                        array.removeAt(i);
                                        i--;
                                        size--;
                                        countAfter--;
                                        mDbHelper.delete(entry.getKey(), time);
                                    }
                                }
                                if (size == 0) {
                                    itr.remove();
                                }
                            }
                        } catch (SQLiteFullException e) {
                            Log.e(TAG, "dailyUpdate error DB is full", e);
                            throw e;
                        } catch (SQLiteException ex) {
                            Log.e(TAG, "dailyUpdate error", ex);
                        }
                        recordCount[0] = countAfter;
                        Log.d(TAG, "dailyUpdate updated: " + countAfter + "/" + countBefore);
                    }
                }
            });
            // coalesce data if necessary
            if (allowCoalescing && recordCount[0] > IDEAL_DATABASE_SIZE) {
                final int divisor = recordCount[0] / IDEAL_DATABASE_SIZE + 1;
                coalesce(divisor);
            }
            mInsertionCount = 0;
        }
        Log.d(TAG, "dailyUpdate out");
    }

    /**
     * This method is used to reduce database size, by merging records that are
     * near enough in time, which is called "time coalescing".<p>
     * Parameter <code>divisor</code> indicates the strength of merging. For
     * example, pass in "3" means every 3 neighbor records will be merged into
     * one record, if possible.
     */
    private void coalesce(final int divisor) {
        Log.d(TAG, "coalesce in: divisor: " + divisor);
        if (divisor <= 1) {
            Log.d(TAG, "coalesce out");
            return;
        }
        try {
            mDbHelper.transaction(new Runnable() {
                @Override
                public void run() {
                    synchronized (AppLaunchManager.this) {
                        int count = 0;
                        for (Entry<Long, SparseArray<Float>> entry : mCachedData.entrySet()) {
                            SparseArray<Float> array = entry.getValue();
                            coalesceArray(entry.getKey(), array, divisor);
                            count += array.size();
                        }
                        Log.d(TAG, "coalesce record count: " + count);
                    }
                }
            });
        } catch (SQLiteFullException e) {
            Log.e(TAG, "dailyUpdate error DB is full", e);
            throw e;
        } catch (SQLiteException ex) {
            Log.e(TAG, "dailyUpdate error", ex);
        }
        Log.d(TAG, "coalesce out");
    }

    private void coalesceArray(long id, SparseArray<Float> array, int minCount) {
        if (minCount <= 1) return;
        if (array == null) return;

        // coalesce algorithm
        int startIndex = 0;
        while (startIndex < array.size() - 1) {
            int endIndex = computeEndIndexForCoalescing(array, startIndex, minCount);
            int length = endIndex - startIndex;
            if (length > 1) {
                coalesceRecords(id, array, startIndex, length);
            }
            startIndex++;
        }
    }

    private void coalesceRecords(long id, SparseArray<Float> array, int offset, int length) {
        if (length <= 1) {
            return;
        }
        float count = 0.0f;
        int coTime = array.keyAt(offset + length / 2);
        for (int i = offset; i < offset + length; i++) {
            count += array.valueAt(i);
            int time = array.keyAt(i);
            if (time != coTime) {
                array.removeAt(i);
                mDbHelper.delete(id, time);
                i--;
                length--;
            }
        }
        array.put(coTime, count);
        mDbHelper.update(id, coTime, count);
    }

    private int computeEndIndexForCoalescing(SparseArray<Float> array, int startIndex, int minCount) {
        int size = array.size();
        int t0 = array.keyAt(startIndex);
        int endIndex = startIndex + 1;
        while (endIndex < size) {
            int dt = array.keyAt(endIndex) - t0;
            if (dt > MAX_COALESCE_TIME_SPAN) {
                // too far away, do not coalesce
                break;
            } else if (dt < DEFAULT_QUERY_TIME_SPAN ||
                       endIndex - startIndex < minCount) {
                // coalesce records that are near enough,
                // or not reach at minCount
                endIndex++;
            } else {
                break;
            }
        }
        return endIndex;
    }

    /**
     * This method is called in LauncherModel.deleteItemFromDatabase(),
     * and should be executed in worker thread.
     */
    public synchronized void deleteItem(ItemInfo info) {
        Log.d(TAG, "deleteItem in");
        if (ENABLED && isValidItem(info)) {
            Long id = info.id;
            mShortcutMap.remove(info.id);
            if (mCachedData.remove(id) != null) {
                try {
                    mDbHelper.delete(id);
                } catch (SQLiteException ex) {
                    Log.e(TAG, "dailyUpdate error", ex);
                }
                Log.d(TAG, "deleteItem item: " + info.title);
            }
            mLastLaunchTimeHelper.removeLastLaunchTime(id);
        }
        Log.d(TAG, "deleteItem out");
    }

    /**
     * Deletes all data in cache and database.
     */
    void deleteAll() {
        mCachedData.clear();
        mShortcutMap.clear();
        try {
            mDbHelper.deleteAll();
        } catch (SQLiteException ex) {
            Log.e(TAG, "dailyUpdate error", ex);
        }
    }

    private int getCurrentTime() {
        Time time = new Time();
        time.setToNow();
        return time.hour * 60 + time.minute;
    }

    private boolean isValidItem(ItemInfo info) {
        if (info instanceof ShortcutInfo) {
            return info.itemType == Favorites.ITEM_TYPE_APPLICATION;
        } else {
            return false;
        }
    }

    //// Test and Debug Only ////

    /**
     * Only used to support unit-test and debug. Do not call
     * in homeshell.
     */
    public TestProxy getTestProxy() {
        return new TestProxy();
    }

    public final class TestProxy {
        public boolean isValidItem(ItemInfo info) {
            return AppLaunchManager.this.isValidItem(info);
        }
        public float getLaunchCount(ShortcutInfo item, int time) {
            synchronized (AppLaunchManager.this) {
                SparseArray<Float> array = mCachedData.get(item.id);
                Float count = array != null ? array.get(time) : null;
                return count != null ? count.floatValue() : 0.0f;
            }
        }
        public void increaseCount(ShortcutInfo item, int time) {
            synchronized (AppLaunchManager.this) {
                mInsertionCount = 0;
                mShortcutMap.put(item.id, item);
                AppLaunchManager.this.increaseCount(item.id, time);
            }
        }
        public int getRowCount() {
            synchronized (AppLaunchManager.this) {
                int count = 0;
                for (SparseArray<Float> array : mCachedData.values()) {
                    count += array.size();
                }
                return count;
            }
        }
        public Pair<Integer, Integer> getTimeRange(int time, int timeSpan) {
            return AppLaunchManager.this.getTimeRange(time, timeSpan);
        }
        public int getTimeDistance(int left, int right) {
            return AppLaunchManager.this.getTimeDistance(left, right);
        }
        public Pair<Integer, Integer> getNearestLaunchTime(SparseArray<Float> array, final int time) {
            synchronized (AppLaunchManager.this) {
                return AppLaunchManager.this.getNearestLaunchTime(array, time);
            }
        }
    }

}
