package com.futurice.hereandnow.card;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.futurice.cascade.i.nonnull;
import com.futurice.hereandnow.HereAndNowApplication;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.activity.ImageViewActivity;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.utils.FileUtils;
import com.futurice.hereandnow.utils.ImageUtils;
import com.futurice.scampiclient.HereAndNowService;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.futurice.cascade.Async.ee;


/**
 * Card that matches the EIT demo card.
 *
 * @author teemuk
 */
public class ImageCard extends BaseCard {
    public static final int CARD_TYPE = 0;    // Needed for ListView recycling

    @NonNull
    @nonnull
    private String text = "";
    @NonNull
    @nonnull
    private Uri imageUri = Uri.EMPTY;
    @NonNull
    @nonnull
    private Uri thumbnailUri = Uri.EMPTY;

    public static final String TAG = ImageCard.class.getName();

    public ImageCard(@NonNull @nonnull final String name, @NonNull @nonnull final Context context) {
        this(name, HereAndNowService.generateUid(), context);
    }

    public ImageCard(final String name, final long uid, final Context context) {
        super(name, uid, context, R.layout.card_layout);
    }

    @Override
    public void updateView(@NonNull @nonnull View view, int color, int highlightColor) {
        super.updateView(view, color, highlightColor);

        final String date = DateUtils.getRelativeDateTimeString(view.getContext(),
                this.getDate().getTime(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0).toString();

        Picasso.with(context)
                .load(this.getImageUri())
                .resize(500, 500) // Downscale huge images first
                .onlyScaleDown()
                .centerInside() // To keep the aspect ratio on resize
                .into((ImageView) view.findViewById(R.id.card_image));

        ((TextView) view.findViewById(R.id.card_text)).setText(this.getText());
        ((TextView) view.findViewById(R.id.card_author)).setText("@" + this.getAuthor() + "\n" + date);
        int likes = ModelSingleton.instance().getLikes(this);
        final List<Comment> comments = ModelSingleton.instance().getCommentsList(this);
        if (likes > 0) {
            String likedText = view.getContext().getString(R.string.card_liked_this);
            if (likes == 1)
                likedText = view.getContext().getString(R.string.card_liked_this_singular);
            ((LinearLayout) view.findViewById(R.id.likes_bar)).setVisibility(View.VISIBLE);
          //  ((TextView) view.findViewById(R.id.likes_number)).setText(Integer.toString(likes));
            ((TextView) view.findViewById(R.id.likes_text)).setText(String.format(likedText, likes) + " " + ModelSingleton.instance().getLikesList(this));
        } else {
            ((LinearLayout) view.findViewById(R.id.likes_bar)).setVisibility(View.GONE);
        }
        if (userAlreadyLikes()) {
            ((ImageView) view.findViewById(R.id.card_like_button)).setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.card_navbar_liked));
        } else {
            ((ImageView) view.findViewById(R.id.card_like_button)).setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.card_navbar_like));
        }
        LinearLayout commentsBar = (LinearLayout) view.findViewById(R.id.comments_bar);
        if (comments != null && !comments.isEmpty()) {
            commentsBar.setVisibility(View.VISIBLE);
            populateCommentsBar(comments, commentsBar);
        } else {
            commentsBar.setVisibility(View.GONE);
        }
        view.findViewById(R.id.card_image).setOnClickListener(v -> {
            Intent viewImageIntent = new Intent(this.context, ImageViewActivity.class);
            viewImageIntent.putExtra(ImageViewActivity.IMAGE_URI, this.getImageUri().toString());
            this.context.startActivity(viewImageIntent);
        });
    }

    /**
     * Helper function for saving a local thumbnail of the image file.
     *
     * @param imageFile
     * @return
     */
    @NonNull
    @nonnull
    public static Uri createThumbnail(@NonNull @nonnull final Uri imageFile) {
        // For resource content we'll just use the main image as thumbnail
        if (imageFile.toString().startsWith("android.resource")) {
            return imageFile;
        }

        try {
            String filePath = FileUtils.getPath(HereAndNowApplication.getStaticContext(), imageFile);
            if (filePath == null) {
                throw new IllegalArgumentException("Can not find path to file: " + imageFile);
            }

            File localImageFile = new File(filePath);
            Bitmap bmThumbnail = ImageUtils.createImageThumbnail(filePath, MediaStore.Video.Thumbnails.MICRO_KIND);

            File directory = new File(Environment.getExternalStorageDirectory() + "/image_thumbnails/");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            final File file = new File(directory.getAbsolutePath() + "/" + localImageFile.getName() + ".jpg");
            final OutputStream outStream = new FileOutputStream(file);

            try {
                bmThumbnail.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                outStream.flush();
            } catch (NullPointerException e) {
                ee(ImageCard.class.getSimpleName(), "Can not compress thumbnail", e);
            } finally {
                outStream.close();
            }

            return Uri.fromFile(file);

        } catch (IOException e) {
            ee(BaseCard.class.getSimpleName(), "Problem writing thumbnail", e);
        }

        return Uri.EMPTY;
    }

    @Override // BaseCard
    public int getType() {
        return CARD_TYPE;
    }

    @Override
    public boolean matchesSearch(@NonNull @nonnull final String search) {
        return text.toLowerCase().contains(search.toLowerCase());
    }

    @NonNull
    @nonnull
    public String getText() {
        return text;
    }

    public void setText(@NonNull @nonnull final String text) {
        this.text = text;
    }

    @NonNull
    @nonnull
    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(@NonNull @nonnull final Uri imageUri) {
        this.imageUri = imageUri;
        this.setThumbnailUri(createThumbnail(this.imageUri));
    }

    @NonNull
    @nonnull
    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(@NonNull @nonnull final Uri thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public void populateCommentsBar(List<Comment> comments, LinearLayout commentsBar) {
        Log.d(TAG, "Populating the comment bar: " + comments.size());
        if (comments != null && !comments.isEmpty() && commentsBar != null) {
            commentsBar.removeAllViews();
            for (Comment comment: comments) {
                final LinearLayout commentItem = (LinearLayout) this.inflateView(R.layout.comment_item, null);
                commentItem.setTag(comment);
                TextView name = (TextView) commentItem.findViewById(R.id.comment_name);
                TextView date = (TextView) commentItem.findViewById(R.id.comment_date);
                TextView text = (TextView) commentItem.findViewById(R.id.comment_text);
                ImageView deleteButton = (ImageView) commentItem.findViewById(R.id.delete_button);
                if (ModelSingleton.instance().myIdTag.get().equalsIgnoreCase(comment.getUserIdTag())) {
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            commentItem.setVisibility(View.GONE);
                            deleteLocalUserComment(comment);
                        }
                    });
                } else {
                    deleteButton.setVisibility(View.GONE);
                }
                name.setText(comment.getUserTag());
                Date timestamp = new Date();
                timestamp.setTime(comment.getTimestamp());
                date.setText(new SimpleDateFormat("dd.MM H.mm").format(timestamp));
                text.setText(comment.getText());
                /*
                commentItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });*/

                commentsBar.addView(commentItem);
            }
        }
    }

}
