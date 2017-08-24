package com.aliyun.homeshell.screenmanager;

import android.content.Context;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;

final class Const {

    private Const() {}
    static final String TAG = "ScreenManager";

    private static final Context mContext = LauncherApplication.getContext();

    static final int LABEL_TEXT_SIZE = mContext.getResources().getDimensionPixelSize(
            R.dimen.screen_edit_text_size);
    static final int LABEL_TEXT_OFFSET_X = (int) mContext.getResources().getDimension(
            R.dimen.screen_edit_card_offset_x);
    static final int LABEL_TEXT_OFFSET_Y = (int) mContext.getResources().getDimension(
            R.dimen.screen_edit_card_padding);

    static final int IVALID_SCREEN_INDEX = -1;
    static final int CATGORY_1_MAX_SCREENS = 4;
    static final int CATGORY_2_MAX_SCREENS = 8;
    static final int CATGORY_2_MAX_SCREENS_LAND = 9;


    static final int PADDIND_TOP = 80;

    static final float RIGHT_TRANS_CARD_1 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_right_card1);
    static final float RIGHT_TRANS_CARD_2 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_right_card2);
    static final float RIGHT_TRANS_CARD_3 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_right_card3);
    static final float DOWN_TRANS_CARD_1 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_down_card1);
    static final float DOWN_TRANS_CARD_2 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_down_card2);
    static final float DOWN_TRANS_CARD_3 = mContext.getResources()
            .getDimension(R.dimen.screen_edit_tran_down_card3);

    static final float FIRST_CARD_OFFSET_X = mContext.getResources()
            .getDimension(R.dimen.screen_edit_card_offset_x);
    static final float FIRST_CARD_OFFSET_Y = mContext.getResources()
            .getDimension(R.dimen.screen_edit_card_offset_y);

    static float[] RIGHT_TRANS = new float[]{0, RIGHT_TRANS_CARD_1,
            RIGHT_TRANS_CARD_2, RIGHT_TRANS_CARD_3};
    static float[] DOWN_TRANS = new float[]{0, DOWN_TRANS_CARD_1,
            DOWN_TRANS_CARD_2, DOWN_TRANS_CARD_3};
    static float[] ROTATES = new float[]{0, 5, 10, 15};

    static final float CARD_WIDTH = mContext.getResources()
            .getDimension(R.dimen.workspace_cell_width);
    static final float CARD_HEIGHT = mContext.getResources()
            .getDimension(R.dimen.workspace_cell_height);
    static final float CARD_PADDING = 40;

    static final int MAX_CARDS = 4;

    static final int TYPE_ENTER = 0;
    static final int TYPE_EXIT = 1;

    static final int MAX_SCREENS = mContext.getResources()
            .getInteger(R.integer.screen_max_count);

}
