package com.futurice.hereandnow.card;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.activity.HereAndNowActivity;
import com.futurice.hereandnow.activity.NewCardActivity;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.utils.FlavorUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.*;


/**
 * Each topic contains 1 or more cards which can arrive in any order and with varying lifetimes
 */
public abstract class BaseTopic implements ITopic {
    @NonNull
    @nonnull
    protected final Context context;
    @NonNull
    @nonnull
    protected final List<ICard> cards = new CopyOnWriteArrayList<>();
    protected final int layoutResource;

    @NonNull
    @nonnull
    private final LayoutInflater inflater;
    private final String name;

    private long timestamp;
    private final long uid;
    @NonNull
    @nonnull
    private final ImmutableValue<String> creationOrigin = originAsync();
    private boolean isPrebuiltTopic = false;
    @Nullable
    @nullable
    protected View topicButtonBar;
    @NonNull
    @nonnull
    protected ImmutableValue<String> origin = originAsync();
    private int likes = 0;

    protected BaseTopic(
            @NonNull @nonnull final String name,
            final long uid,
            @NonNull @nonnull final Context context,
            final int layoutResource) {
        this.name = name;
        this.uid = uid;
        this.context = context;
        this.layoutResource = layoutResource;

        this.inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public boolean isPrebuiltTopic() {
        return isPrebuiltTopic;
    }

    public void setIsPrebuiltTopic(boolean value) {
        this.isPrebuiltTopic = value;
    }

    @NonNull
    @nonnull
    @Override // Topic
    public List<ICard> getCards() {
        return Collections.unmodifiableList(this.cards);
    }

    public void addCard(@NonNull @nonnull ICard card) {
        this.cards.add(card);
    }

    @Override
    public void removeCard(@NonNull @nonnull ICard card) {
        this.cards.remove(card);
    }

    @NonNull
    @nonnull
    @Override // Topic
    public View getView(@NonNull @nonnull final ViewGroup parentView, boolean isExpanded, final int color, final int highlightColor) {
        // Inflate the new view
        final View newView = this.inflateView(this.layoutResource, parentView);
        updateView(newView, isExpanded, color, highlightColor);

        return newView;
    }

    @Override // Topic
    public void updateView(
            @NonNull @nonnull final View view,
            final boolean isExpanded,
            final int color,
            final int highlightColor) {
        final View bar = view.findViewById(R.id.topic_button_bar);
        topicButtonBar = bar;
        bar.setVisibility(isExpanded && contentType() != PeerProfileCard.CARD_TYPE ? View.VISIBLE : View.GONE);

        // Show a red delete button for topics with flagged cards
        if (FlavorUtils.isSuperuserBuild && isFlagged()) {
            view.findViewById(R.id.topic_flagged).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.topic_flagged).setVisibility(View.GONE);
        }

        view.findViewById(R.id.topic_table_row).setBackgroundColor(highlightColor);
        view.findViewById(R.id.topic_button_bar).setBackgroundColor(color);

        if (likes > 0) {
            ((LinearLayout) view.findViewById(R.id.topic_likes_layout)).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.topic_like_text)).setText(Integer.toString(likes));
        } else {
            ((LinearLayout) view.findViewById(R.id.topic_likes_layout)).setVisibility(View.GONE);
        }

        view.findViewById(R.id.card_picture_button).setOnClickListener(
                v -> launchNewCardActivity("image/*"));

        view.findViewById(R.id.card_video_button).setOnClickListener(
                v -> launchNewCardActivity("video/*"));
    }

    private void launchNewCardActivity(String contentType) {
        HereAndNowActivity activity = (HereAndNowActivity) context;
        Intent createCardIntent = new Intent(activity, NewCardActivity.class);
        createCardIntent.putExtra(NewCardActivity.EXTRA_TOPIC, name);
        createCardIntent.putExtra(NewCardActivity.EXTRA_TOPIC_UID, uid);
        createCardIntent.putExtra(NewCardActivity.EXTRA_CONTENT_TYPE, contentType);
        activity.startActivity(createCardIntent);
    }

    @Override // Topic
    public long getUid() {
        return this.uid;
    }

    @Override // INamed
    @NonNull
    @nonnull
    public String getName() {
        return this.name;
    }

    @Override
    public boolean matchesSearch(@NonNull @nonnull final String search) {
        if (this.name.toLowerCase().contains(search.toLowerCase())) {
            return true;
        }

        for (ICard card : this.cards) {
            if (card.matchesSearch(search)) {
                return true;
            }
        }

        return false;
    }

    public void expanded() {
        if (topicButtonBar != null) {
            topicButtonBar.setVisibility(View.VISIBLE);
        } else {
            vv(origin, "No topic button bar to expand");
        }
    }

    public void collapsed() {
        if (topicButtonBar != null) {
            topicButtonBar.setVisibility(View.GONE);
        }
    }

    @NonNull
    @nonnull
    private View inflateView(final int resource, final ViewGroup parentView) {
        return this.inflater.inflate(resource, parentView, false);
    }

    private boolean isFlagged() {
        for (ICard card : this.cards) {
            if (((BaseCard) card).isFlagged()) {
                return true;
            }
        }

        return false;
    }

    private int contentType() {
        return cards.size() > 0 ? cards.get(0).getType() : -1;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(final int likes) {
        this.likes = likes;
        // TODO Refresh the image with number of likes
        // TODO Image would need to be reactive display
    }
}
