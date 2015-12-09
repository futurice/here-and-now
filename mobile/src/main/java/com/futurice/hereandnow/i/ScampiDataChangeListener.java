package com.futurice.hereandnow.i;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.nonnull;

public interface ScampiDataChangeListener<T> {
    /**
     * Callback for new items appearing
     *
     * @param item
     */
    void onItemAdded(@NonNull @nonnull T item);

    /**
     * Callback for removing items
     *
     * @param uids
     */
    void onItemsUpdated(@NonNull @nonnull long[] uids);

    /**
     * Callback for removing items
     *
     * @param uids
     */
    void onItemsRemoved(@NonNull @nonnull long[] uids);
}