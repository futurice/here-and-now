package com.futurice.hereandnow.card;

import android.support.annotation.NonNull;
import android.view.*;

import com.futurice.cascade.i.*;
import com.futurice.cascade.i.nonnull;

/**
 * Interface for cards.
 *
 * @author teemuk
 */
public interface ICard extends INamed {

    /**
     * Returns a newly inflated view of the card.
     *
     * @param parentView
     * @param color
     * @return newly created view of the card.
     */
    @NonNull
    @nonnull
    public View getView(@NonNull @nonnull ViewGroup parentView, int color, int highlightColor);

    /**
     * Updates the view contents with the card's data. The passed view will have
     * been previously created by calling {@link #getView};
     *
     * @param view           view whose contents to update
     * @param color
     * @param highlightColor
     */
    public void updateView(@NonNull @nonnull View view, int color, int highlightColor);

    /**
     * Returns a uid identifier for this card.
     *
     * @return uid identifier
     */
    public long getUid();

    /**
     * Return a type code for this card. This is needed by ListView to recycle
     * views, and the type corresponds to the view type that the card uses.
     *
     * @return card type
     */
    public int getType();

    /**
     * Returns true when card matches the search string, false otherwise.
     *
     * @param search
     * @return
     */
    public boolean matchesSearch(@NonNull @nonnull String search);
}
