package com.aliyun.homeshell.smartsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.EncodingUtils;

import com.aliyun.homeshell.R;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;


public class HanziToPinyin {

    public byte[] mHanziPinyin;

    private static final String TAG = "HanziToPinyin";

    private static final int PINYIN_DATA_SIZE = 379719;
    private static final int PINYIN_CANDIDATE_LENGTH_MAX = 6;
    private static final int PINYIN_CANDIDATE_PER_CHAR = 3;
    private static final int PINYIN_POLY_MAX = 16;

    public static final int HANZI_START = 19968;
    public static final int HANZI_COUNT = 20902;

    public static final char[] Data_Letters_To_T9 = {
        '2', '2', '2', '3', '3', '3', '4', '4', '4', '5', '5', '5', '6', '6', '6', '7', '7',
        '7', '7', '8', '8', '8', '9', '9', '9', '9'
    };

    private static HanziToPinyin mInsatnce = new HanziToPinyin();

    private HanziToPinyin() {}

    public static HanziToPinyin getInstance() {
        return mInsatnce;
    }

    public void initHanziPinyinForAllChars(Context context) {
        if (mHanziPinyin != null) {
            return;
        }
        byte[] result = readHanziPinyinDataToByteArray(context);
        trimLineBreaksInPinyinData(result);
        mHanziPinyin = result;
    }

    private byte[] readHanziPinyinDataToByteArray(Context context) {
        byte[] result = new byte[PINYIN_DATA_SIZE];
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(R.raw.data_hzpinyin);
            int n,cnt = 0;
            while (cnt < PINYIN_DATA_SIZE) {
                n = is.read(result, cnt, PINYIN_DATA_SIZE - cnt);
                if (n < 0) {
                    Log.e(TAG, "readHanziPinyinDataToByteArray: got unexpected EOF at count "+cnt+" with return value "+n+".");
                    break;
                }
                cnt += n;
            }
        } catch (NotFoundException e) {
            Log.e(TAG, "Resource not found for R.raw.data_hzpinyin ("+R.raw.data_hzpinyin+")."+e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "Cannot read from resource R.raw.data_hzpinyin ("+R.raw.data_hzpinyin+")."+e.getMessage(), e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return result;
    }

    private void trimLineBreaksInPinyinData(byte[] pinyinData) {
        int readPos = 0, writePos = 0;
        byte readB;
        while (readPos < PINYIN_DATA_SIZE) {
            readB = pinyinData[readPos];
            if ((readB == '\n') || (readB == '\r')) {
                readPos++;
                continue;
            }
            pinyinData[writePos++] = readB;
            readPos++;
        }
        while (writePos < PINYIN_DATA_SIZE) {
            pinyinData[writePos++] = '\0';
        }
    }

    private String getHanziPinyinDataForCharCode(int chCode, int index) {
        int startPosition = (chCode - HANZI_START) * (PINYIN_CANDIDATE_LENGTH_MAX * PINYIN_CANDIDATE_PER_CHAR) + index * PINYIN_CANDIDATE_LENGTH_MAX;

        int lastNonSpaceIndex = 5;
        while (lastNonSpaceIndex > 0) {
            if (mHanziPinyin[startPosition + lastNonSpaceIndex] != ' ') {
                break;
            }
            lastNonSpaceIndex--;
        }
        if (mHanziPinyin[startPosition + lastNonSpaceIndex] == ' ') {
            return null;
        }
        String pinyin = EncodingUtils.getAsciiString(mHanziPinyin, startPosition, lastNonSpaceIndex+1);
        return pinyin;
    }

    public List<List<String>> getHanziPinyin(String s) {
        if (mHanziPinyin == null || s == null || s.isEmpty()) {
            return new ArrayList<List<String>>();
        }

        List<List<String>> hanzi = new ArrayList<List<String>>();
        char[] sCh = s.toCharArray();
        for (char c : sCh) {
            List<String> pinyin4Char = new ArrayList<String>();
            if ((c >= HANZI_START) && (c < (HANZI_START + HANZI_COUNT))) {
                for (int count = 0; count < PINYIN_CANDIDATE_PER_CHAR; count++) {
                    String pinyin = getHanziPinyinDataForCharCode(c, count);
                    if (pinyin == null) {
                        break;
                    }
                    pinyin4Char.add(pinyin);
                }
            } else {
                pinyin4Char.add(c+"");
            }
            hanzi.add(pinyin4Char);
        }

        return getAllPinyin(hanzi);
    }

    private static List<List<String>> getAllPinyin(List<List<String>> hanzi) {
        int hanziNumber = hanzi.size();
        int[] pickCount = new int[hanziNumber];

        for (int i = 0; i < pickCount.length; i++) {
            pickCount[i] = hanzi.get(i).size();
        }

        int count = 1;
        int[] pickBase = new int[hanziNumber];
        for (int i = 0; i < pickCount.length; i++) {
            count *= pickCount[i];
            pickBase[i] = count;
        }

        count = (count < PINYIN_POLY_MAX) ? count : PINYIN_POLY_MAX;

        List<List<String>> pinyinList = new ArrayList<List<String>>();
        for (int i = 0; i < count; i++) {
            List<String> py = new ArrayList<String>();
            for (int j = 0; j < hanziNumber; j++) {
                int index = i % pickBase[j] % pickCount[j];
                py.add(hanzi.get(j).get(index));
            }
            pinyinList.add(py);
        }

        return pinyinList;
    }
}
