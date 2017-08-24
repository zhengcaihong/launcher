package com.aliyun.homeshell.test;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aliyun.homeshell.Launcher;

import android.content.Intent;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.util.TypedValue;
import static com.aliyun.homeshell.R.dimen.*;

/**
 * Unit test for HomeShell layout parameters.
 * @author zhanggong.zg
 */
public class LauncherLayoutTest extends ActivityInstrumentationTestCase2<Launcher> {

    private static final String TAG = "LauncherLayoutTest";
    private static final Map<Integer, int[]> sStandardValueMap;

    static {
        sStandardValueMap = new LinkedHashMap<Integer, int[]>();
        initStandardValues(sStandardValueMap);
    }

    private enum Columns {
        // do not modify the order
        SW340, SW320_854x480, SW320
    }

    /**
     * Document:
     *     http://docs.alibaba-inc.com:8090/pages/viewpage.action?pageId=251352148
     *     2015/6/8
     */
    private static void initStandardValues(Map<Integer, int[]> m) {
        //                                              SW340  854    SW320
        // Workspace:
        m.put(qsb_bar_height,                       arr(38,    43,    43)); // workspace.top
        m.put(button_bar_height,                    arr(108,  100,   100)); // workspace.bottom
        m.put(cell_layout_left_padding,             arr(18,    14,    17));
        m.put(cell_layout_right_padding,            arr(18,    14,    17));
        m.put(cell_layout_top_padding,              arr(18,     8,     8)); // 4x5 (default)
        m.put(cell_layout_bottom_padding,           arr(18,     8,     8));
        m.put(cell_layout_top_padding_port_4_4,     arr(30,    10,    10)); // 4x4 (default)
        m.put(cell_layout_bottom_padding_port_4_4,  arr(30,    25,    25));

        // Hotseat:
        m.put(button_bar_height_plus_padding,       arr(88,    85,    85)); // hotseat.height
        m.put(page_indicator_height,                arr(24,    22,    22));
        m.put(hotseat_left_padding_port,            arr(20,    8,     8));
        m.put(hotseat_right_padding_port,           arr(20,    8,     8));
        m.put(button_bar_height_top_padding,        arr(0,     0,     0));

        // Icon:
        m.put(workspace_cell_width,                 arr(66,    59,    53));
        m.put(workspace_cell_height,                arr(80,    72,    64));
        m.put(hotseat_cell_width,                   arr(76,    68,    68));
        m.put(hotseat_cell_height,                  arr(84,    78,    78));
        m.put(bubble_textview_padding_left,         arr(2,     2,     2));
        m.put(bubble_textview_padding_right,        arr(2,     2,     2));
        m.put(bubble_textview_hotseat_top_padding,  arr(0,     2,     2));
        m.put(bubble_textview_hotseat_left_padding, arr(5,     5,     5));

        // Folder:
        m.put(folder_cell_layout_start_padding,     arr(30,    30,    30));
        m.put(folder_cell_layout_end_padding,       arr(30,    30,    30));
        m.put(folder_cell_layout_top_padding,       arr(34,    25,    25));
        m.put(folder_cell_layout_bottom_padding,    arr(34,    25,    25));
        m.put(folder_name_content_gap,              arr(20,    20,    20));

        // Icon in folder:
        m.put(folder_cell_width,                    arr(66,    59,    53)); // big icon
        m.put(folder_cell_height,                   arr(80,    72,    64));
        m.put(folder_cell_width_gap,                arr(20,    20,    20));
        m.put(folder_cell_height_gap,               arr(31,    26,    26));
        m.put(folder_cell_width_small,              arr(66,    66,    66)); // small icon
        m.put(folder_cell_height_small,             arr(80,    80,    80));
        m.put(folder_cell_width_gap_small,          arr(15,    15,    15));
        m.put(folder_cell_height_gap_small,         arr(0,     0,     0));

        // Hideseat:
        m.put(hideseat_cell_layout_top_padding,     arr(8,     6,     6));
        m.put(hideseat_height,                      arr(98,    98,    98));

        // Icon in hideseat:
        m.put(hideseat_cell_layout_cell_height,     arr(76,    88,    88));

        // TODO:
        // ScreenEdit, DeleteZone, AgedMode, EditMode
    }

    private static int[] arr(int... values) { return values; }

    //// Instance Members ////

    private Launcher mLauncher = null;
    private Resources mResources = null;

    public LauncherLayoutTest() {
        super(Launcher.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent();
        intent.setClassName("com.aliyun.homeshell", Launcher.class.getName());
        setActivityIntent(intent);
        mLauncher = getActivity();
        mResources = mLauncher.getResources();
    }

    @Override
    protected void tearDown() throws Exception {
        mLauncher = null;
        mResources = null;
        super.tearDown();
    }

    private int getRawIntValue(int resId) {
        TypedValue typedValue = new TypedValue();
        mResources.getValue(resId, typedValue, true);
        return (int) TypedValue.complexToFloat(typedValue.data);
    }

    private static int getStandardValue(int resId, Columns col) {
        return sStandardValueMap.get(resId)[col.ordinal()];
    }

    private void testValues(Columns col) {
        Log.d(TAG, "test values for: " + col);
        for (int resId : sStandardValueMap.keySet()) {
            int stdValue = getStandardValue(resId, col);
            Log.d(TAG, "check: " + mResources.getResourceName(resId));
            assertEquals(stdValue, getRawIntValue(resId));
        }
    }

    private int getPixels(int resId) {
        return mResources.getDimensionPixelSize(resId);
    }

    //// Test Methods ////

    public void testSW340() {
        testValues(Columns.SW340);
    }

    public void testSW320_854x480() {
        testValues(Columns.SW320_854x480);
    }

    public void testSW320() {
        testValues(Columns.SW320);
    }

    public void testConstraints() {
        int workspace_width = mLauncher.getWorkspace().getWidth();
        int workspace_height = mLauncher.getWorkspace().getHeight();
        int hotseat_width = mLauncher.getHotseat().getWidth();
        int workspace_bottom = getPixels(button_bar_height);
        int cellLayout_left = getPixels(cell_layout_left_padding);
        int cellLayout_right = getPixels(cell_layout_right_padding);
        int cellLayout_top = getPixels(cell_layout_top_padding);
        int cellLayout_bottom = getPixels(cell_layout_bottom_padding);
        int icon_width = getPixels(workspace_cell_width);
        int icon_height = getPixels(workspace_cell_height);
        int icon_left = getPixels(bubble_textview_padding_left);
        int icon_right = getPixels(bubble_textview_padding_right);
        int hotseat_icon_width = getPixels(hotseat_cell_width);
        int hotseat_icon_height = getPixels(hotseat_cell_height);
        int hotseat_top = getPixels(button_bar_height_top_padding);
        int hotseat_height = getPixels(button_bar_height_plus_padding);
        int hotseat_left = getPixels(hotseat_left_padding_port);
        int hotseat_right = getPixels(hotseat_right_padding_port);
        int indicator_height = getPixels(page_indicator_height);
        int folder_icon_width = getPixels(folder_cell_width);
        int folder_icon_hgap = getPixels(folder_cell_width_gap);
        int folder_cellLayout_left = getPixels(folder_cell_layout_start_padding);
        int folder_cellLayout_right = getPixels(folder_cell_layout_end_padding);

        assertTrue("workspace icons should not collide with page indicator",
                workspace_bottom + cellLayout_bottom >= hotseat_height + indicator_height / 2);

        assertTrue("workspace icons should not collide with each other",
                cellLayout_left + 4 * icon_width + cellLayout_right < workspace_width &&
                cellLayout_top + 5 * icon_height + cellLayout_bottom < workspace_height);

        assertTrue("hotseat icons should not collide with each other",
                hotseat_top + hotseat_icon_height <= hotseat_height &&
                hotseat_left + 4 * hotseat_icon_width + hotseat_right < hotseat_width);

        assertTrue("left/right padding should be equal",
                cellLayout_left == cellLayout_right &&
                hotseat_left == hotseat_right &&
                icon_left == icon_right);

        assertTrue("folder width should be less than screen width",
                (folder_icon_width * 3 + folder_icon_hgap * 2 + folder_cellLayout_left + folder_cellLayout_right)
                / (double) workspace_width < 0.9);

        // TODO: add more constraint tests
    }

    public void testLogValues() {
        // this method is used to logcat all values
        for (int resId : sStandardValueMap.keySet()) {
            String name = mResources.getResourceName(resId);
            int value = getRawIntValue(resId);
            Log.d(TAG, String.format("%s = %d", name, value));
        }
    }

}
