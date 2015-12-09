package com.futurice.hereandnow.card;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.futurice.cascade.i.nonnull;
import com.futurice.hereandnow.R;

/**
 * Card to be shown for peer profiles in the tribe view.
 *
 * @author teemuk
 */
public class PeerProfileCard extends BaseCard {

    public static final int CARD_TYPE = 2;    // Needed for ListView recycling.
    private static final long TOPIC_UID_FOR_ALL_PEER_CARDS = -9001;

    @NonNull
    @nonnull
    private String peerTag = "";
    @NonNull
    @nonnull
    private String peerIdTag = "";
    @NonNull
    @nonnull
    private String peerAboutMe = "";
    @NonNull
    @nonnull
    private String peerLikes = "";

    public PeerProfileCard(@NonNull @nonnull final String name, @NonNull @nonnull final Context context) {
        super(name, TOPIC_UID_FOR_ALL_PEER_CARDS, context, R.layout.peer_card_layout);
    }

    @Override
    public void updateView(@NonNull @nonnull final View view, final int color, final int highlightColor) {
        super.updateView(view, color, highlightColor);

        ((TextView) view.findViewById(R.id.peer_tag_text)).setText(peerTag);
        ((TextView) view.findViewById(R.id.peer_about_me_text)).setText(peerAboutMe);
    }

    @Override // BaseCard
    public final int getType() {
        return CARD_TYPE;
    }

    @Override
    public boolean matchesSearch(@NonNull @nonnull final String search) {
        return false;
    }

    @NonNull
    @nonnull
    public String getPeerIdTag() {
        return peerIdTag;
    }

    public void setPeerIdTag(@NonNull @nonnull final String peerIdTag) {
        this.peerIdTag = peerIdTag;
    }

    @NonNull
    @nonnull
    public String getPeerTag() {
        return peerTag;
    }

    public void setPeerTag(@NonNull @nonnull final String peerTag) {
        this.peerTag = peerTag;
    }

    @NonNull
    @nonnull
    public String getPeerAboutMe() {
        return peerAboutMe;
    }

    public void setPeerAboutMe(@NonNull @nonnull final String peerAboutMe) {
        this.peerAboutMe = peerAboutMe;
    }

    @NonNull
    @nonnull
    public String getPeerLikes() {
        return peerLikes;
    }

    public void setPeerLikes(@NonNull @nonnull final String peerLikes) {
        this.peerLikes = peerLikes;
    }
}
