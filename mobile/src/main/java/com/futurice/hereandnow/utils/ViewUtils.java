package com.futurice.hereandnow.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.futurice.cascade.i.nonnull;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.R;

/**
 * Get UI theme color for each of the different list views
 * <p>
 * Created by pper on 16/04/15.
 */
public final class ViewUtils {

    @SuppressWarnings("deprecated")
    public static int getColor(@NonNull @nonnull final Context context, final int listViewMode) {
        switch (listViewMode) {
            case Constants.LISTVIEWMODE_TRENDING:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.trending, null);
                }
                return context.getResources().getColor(R.color.trending);

            case Constants.LISTVIEWMODE_HAPPENING_NOW:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.happening_now, null);
                }
                return context.getResources().getColor(R.color.happening_now);

            case Constants.LISTVIEWMODE_PEOPLE_NEARBY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.people_nearby, null);
                }
                return context.getResources().getColor(R.color.people_nearby);

            default:
                throw new UnsupportedOperationException("Unsupported ListView mode: " + listViewMode);
        }
    }

    @SuppressWarnings("deprecated")
    public static int getHighlightColor(@NonNull @nonnull final Context context, final int listViewMode) {
        switch (listViewMode) {
            case Constants.LISTVIEWMODE_TRENDING:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.trending_highlight, null);
                }
                return context.getResources().getColor(R.color.trending_highlight);

            case Constants.LISTVIEWMODE_HAPPENING_NOW:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.happening_now_highlight, null);
                }
                return context.getResources().getColor(R.color.happening_now_highlight);

            case Constants.LISTVIEWMODE_PEOPLE_NEARBY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return context.getResources().getColor(R.color.my_tribe_highlight, null);
                }
                return context.getResources().getColor(R.color.my_tribe_highlight);

            default:
                throw new UnsupportedOperationException("Unsupported ListView mode: " + listViewMode);
        }
    }
}
