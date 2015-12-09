package com.futurice.hereandnow.card;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.futurice.cascade.i.nonnull;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.scampiclient.HereAndNowService;
import com.squareup.picasso.Picasso;

/**
 * Simple Topic that matches the EIT demo version topic.
 *
 * @author teemuk
 */
public final class Topic extends BaseTopic {

    @NonNull
    @nonnull
    private String text = "";
    @NonNull
    @nonnull
    private Uri imageUri = Uri.EMPTY;
 //   private int likes = 0;

    public Topic(@NonNull @nonnull final String name, @NonNull @nonnull final Context context) {
        this(name, HereAndNowService.generateUid(), context);
    }

    public Topic(@NonNull @nonnull final String name, final long topicUid, final Context context) {
        super(name, topicUid, context, R.layout.topic_layout);
    }

    public void setText(@NonNull @nonnull final String text) {
        this.text = text;
    }

    public void setImageUri(@NonNull @nonnull final Uri uri) {
        this.imageUri = uri;
    }

    @Override
    public void addCard(@NonNull @nonnull final ICard card) {
        super.addCard(card);

        // Set the topic icon to the first image added to the topic
        if (this.imageUri == Uri.EMPTY && card instanceof ImageCard) {
            setImageUri(((ImageCard) card).getThumbnailUri());
        }
    }

    @Override
    public void updateView(
            @NonNull @nonnull final View view,
            final boolean isExpanded,
            final int color,
            final int highlightColor) {
        super.setLikes(getLikes());
        super.updateView(view, isExpanded, color, highlightColor);

        ((ImageView) view.findViewById(R.id.topic_icon)).setScaleType(ImageView.ScaleType.CENTER_CROP);
        ((TextView) view.findViewById(R.id.topic_title)).setText(this.text);

        //TODO Show number of likes on top of the image
        Picasso.with(context)
                .load(this.imageUri)
                .resize(90, 90) // Downscale images first
                .onlyScaleDown()
                .centerInside() // To keep the aspect ratio on resize
                .into((ImageView) view.findViewById(R.id.topic_icon));
      /*  if (getLikes() > 0) {
            ((LinearLayout) view.findViewById(R.id.topic_likes_layout)).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.topic_like_text)).setText(Integer.toString(getLikes()));
        } else {
            ((LinearLayout) view.findViewById(R.id.topic_likes_layout)).setVisibility(View.GONE);
        }*/
    }

    @Override
    public boolean matchesSearch(@NonNull @nonnull final String search) {
        return this.text.toLowerCase().contains(search) || super.matchesSearch(search);
    }

    @Override
    public int compare(@NonNull @nonnull final ITopic other) {
        return ((Topic) other).getLikes() - getLikes();
    }

    @NonNull
    @nonnull
    public String getText() {
        return text;
    }

    @NonNull
    @nonnull
    public Uri getImageUri() {
        return imageUri;
    }

 /*   public int getLikes() {
        return likes;
    }

    public void setLikes(final int likes) {
        this.likes = likes;
        // TODO Refresh the image with number of likes
        // TODO Image would need to be reactive display
    }
    */
}
