package com.aliyun.homeshell.editmode;

/**
 * Interface definition for a callback to be invoked when the view scroll state
 * has changed.
 */
public interface OnScrollStateChangedListener {
    public enum ScrollState {
        /**
         * The view is not scrolling. Note navigating the list using the
         * trackball counts as being in the idle state since these transitions
         * are not animated.
         */
        SCROLL_STATE_IDLE,

        /**
         * The user is scrolling using touch, and their finger is still on the
         * screen
         */
        SCROLL_STATE_TOUCH_SCROLL,

        /**
         * The user had previously been scrolling using touch and had performed
         * a fling. The animation is now coasting to a stop
         */
        SCROLL_STATE_FLING
    }

    /**
     * Callback method to be invoked when the scroll state changes.
     *
     * @param scrollState
     *            The current scroll state.
     */
    public void onScrollStateChanged(ScrollState scrollState);

    public void onScroll(int leftIndex, int rightIndex, int count);
}
