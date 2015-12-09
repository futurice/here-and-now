package com.futurice.hereandnow;

/**
 * Created by pper on 16/04/15.
 */
public class Constants {
    // Had to change these from an enum to int to work with PersistentValue storage mode limitations. Can change back later
    public static final int LISTVIEWMODE_TRENDING = 0;
    public static final int LISTVIEWMODE_HAPPENING_NOW = 1;
    public static final int LISTVIEWMODE_PEOPLE_NEARBY = 2;
    public static final int NUMBER_OF_LIST_VIEW_MODES = 3;

    public static final String EMPTY_IMAGE_FILE_NAME = "empty_image_here_and_now.jpg";

    public static int[] TAB_ICONS = {R.drawable.topmenu_trending, R.drawable.topmenu_happening_now, R.drawable.topmenu_my_tribe};

    public static final String USER_AUTHOR_KEY = "author_key";

    public static final int DELAY_IN_MINUTES = 30;
}
