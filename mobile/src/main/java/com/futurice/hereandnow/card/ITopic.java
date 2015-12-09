package com.futurice.hereandnow.card;

import android.support.annotation.NonNull;
import android.view.*;

import com.futurice.cascade.i.*;
import com.futurice.cascade.i.nonnull;

import java.util.*;

/**
 * Topic containing Cards.
 *
 * @author teemuk
 */
public interface ITopic extends INamed {

    /**
     * Returns a list of all cards attached to this topic.
     *
     * @return list of all cards of this topic.
     */
    @NonNull
    @nonnull
    public List<ICard> getCards();

    /**
     * Adds a card to the list of cards attached to this topic.
     *
     * @param card
     */
    public void addCard(@NonNull @nonnull ICard card);

    /**
     * Removes a card from the list of cards attached to this topic.
     *
     * @param card
     */
    public void removeCard(@NonNull @nonnull ICard card);

    /**
     * Retruns a newly inflated view of this topic.
     *
     * @param parentView     parent view for the view.
     * @param color
     * @param highlightColor
     * @return view of this topic
     */
    @NonNull
    @nonnull
    public View getView(@NonNull @nonnull ViewGroup parentView, boolean isExpanded, int color, int highlightColor);

    /**
     * Update the given view with the Topic's data. The passed view will have
     * been previously created by {@link #getView}.
     *
     * @param view           the view to update
     * @param color
     * @param highlightColor
     */
    public void updateView(@NonNull @nonnull View view, boolean isExpanded, int color, int highlightColor);

    /**
     * Returns the UID of this topic.
     *
     * @return
     */
    public long getUid();

    /**
     * Returns true when topic matches the search string, false otherwise.
     *
     * @param search
     * @return
     */
    public boolean matchesSearch(@NonNull @nonnull String search);

    /**
     * Returns the comparison result between this topic and other.
     *
     * @param other
     * @return
     */
    public int compare(@NonNull @nonnull ITopic other);

    /**
     * Return true for prebuilt topics, false otherwise. This allows the UI to
     * treat prebuilt topics differently from user-generated content.
     *
     * @return
     */
    public boolean isPrebuiltTopic();

    /**
     * Sets the number of likes the cards in this topic have aggregated.
     *
     * @param likes
     */
    public void setLikes(int likes);
}
