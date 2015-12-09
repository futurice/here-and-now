package com.futurice.hereandnow.utils;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.futurice.cascade.i.nonnull;

import java.util.ArrayList;
import java.util.List;

public final class HereAndNowUtils {

    private static final String GOOGLE_PHOTOS_URI = "com.google.android.apps.photos.content";
    private static final String MEDIA_DOCUMENT_URI = "com.android.providers.media.documents";
    private static final String DOWNLOADS_DOCUMENT_URI = "com.android.providers.downloads.documents";
    private static final String EXTERNAL_STORAGE_URI = "com.android.externalstorage.documents";

    /**
     * Return the original object if no cleanup is done, or for example "" if there are zero characters worth
     * keeping after cleanup
     * <p>
     * Cleanup means conformance to "#tagnumberlowercasedigitsnotinfirstposition"
     *
     * @param value
     * @return
     */
    @NonNull
    @nonnull
    public static String cleanupTag(@NonNull @nonnull final Object value, final char firstCharacter) {
        final String tag = ((String) value);
        String cleanedTag = tag.trim().toLowerCase();

        while (cleanedTag.startsWith("##") || cleanedTag.startsWith("@@")) {
            cleanedTag = cleanedTag.substring(1);
        }
        final StringBuffer sb = new StringBuffer(cleanedTag.length());
        boolean first = true;
        for (char c : cleanedTag.toCharArray()) {
            if (!Character.isLetter(c)) {
                // Is not a letter
                if (!first && Character.isDigit(c)) {
                    sb.append(c);
                }
                continue;
            }
            sb.append(c);
            first = false;
        }
        String s = sb.toString();
        if (!s.startsWith("" + firstCharacter)) {
            s = firstCharacter + s;
        }
        if (!s.equals(tag)) {
            return s;
        }

        return tag;
    }

    @NonNull
    @nonnull
    public static String cleanupTagList(@NonNull @nonnull final Object value, final char firstCharacter) {
        String tags = (String) value;

        String[] lines = ((String) value).split("[\\p{Space}\\p{Punct}\\r?\\n]");
        List<String> nonTrivialLines = new ArrayList<>();
        for (String line : lines) {
            String s = cleanupTag(line, firstCharacter);
            if (s.length() > 1) {
                nonTrivialLines.add(s);
            }
        }
        StringBuffer sb = new StringBuffer(tags.length());
        for (String line : nonTrivialLines) {
            sb.append(line);
            sb.append("\n");
        }
        String cleanedUpTagList = sb.toString().trim();
        if (!tags.equals(cleanedUpTagList)) {
            return cleanedUpTagList;
        }

        return tags;
    }

    /**
     * Convert R id into a Uri which can be used for pushing that image etc to the UI
     *
     * @param resourceId
     * @return
     */
    @NonNull
    @nonnull
    public static Uri getResourceUri(@NonNull @nonnull final int resourceId) {
        return Uri.parse("android.resource://com.futurice.hereandnow/" + resourceId);
    }

    //From https://code.google.com/p/openintents/source/browse/trunk/compatibility/AndroidSupportV2/src/android/support/v2/app/FragmentPagerAdapter.java#104
    //We can get the fragment tag given by the adapter using this method
    @NonNull
    @nonnull
    public static String getFragmentTag(final int viewPagerId, final int fragmentPosition) {
        return "android:switcher:" + viewPagerId + ":" + fragmentPosition;
    }
}
