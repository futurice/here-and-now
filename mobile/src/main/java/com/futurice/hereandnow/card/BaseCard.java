package com.futurice.hereandnow.card;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.util.TestUtil;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.utils.FlavorUtils;
import com.futurice.scampiclient.items.Peer;
import com.futurice.scampiclient.utils.ArrayUtils;

import java.util.Date;
import java.util.List;

import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * Base class for cards.
 *
 * @author teemuk
 */
public abstract class BaseCard implements ICard {

    @NonNull
    @nonnull
    protected final Context context;
    protected final int layoutResource;

    @NonNull
    @nonnull
    private final LayoutInflater inflater;
    @NonNull
    @nonnull
    private final String name;
    private final long uid;

    private boolean flagged = false;
    private String author;
    private String authorId;
    private Date date;
    @NonNull
    @nonnull
    protected ImmutableValue<String> origin = originAsync();

    protected BaseCard(
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

    /**
     * A new card view is being created by the list model
     *
     * @param parentView
     * @param color
     * @param highlightColor
     * @return
     */
    @Override
    @NonNull
    @nonnull
    public View getView(@NonNull @nonnull final ViewGroup parentView, final int color, final int highlightColor) {
        // Inflate the new view
        final View newView = this.inflateView(this.layoutResource, parentView);
        updateView(newView, color, highlightColor);

        return newView;
    }

    /**
     * An existing View is being recycled by the list model
     *
     * @param view           view whose contents to update
     * @param color
     * @param highlightColor
     */
    @Override
    public void updateView(@NonNull @nonnull final View view, final int color, final int highlightColor) {
        vv(origin, "updateView");
        try {
            if (FlavorUtils.isSuperuserBuild && this.flagged) {
                // Show delete button in red for flagged cards
                ((ImageView) view.findViewById(R.id.card_delete_button)).setColorFilter(Color.rgb(255, 0, 0));
            }
            view.findViewById(R.id.card_base).setBackgroundColor(highlightColor);
            view.findViewById(R.id.topic_button_bar).setBackgroundColor(color);

            view.findViewById(R.id.card_like_button).setOnClickListener(v -> likeButtonClicked(v, color, highlightColor));
            view.findViewById(R.id.card_comment_button).setOnClickListener(v -> commentButtonClicked(v, color, highlightColor));
            view.findViewById(R.id.card_user_button).setOnClickListener(this::userButtonClicked);
            view.findViewById(R.id.card_delete_button).setOnClickListener(v -> deleteCardButtonClicked(v));
            view.findViewById(R.id.card_flag_button).setOnClickListener(v -> flagCardButtonClicked(v));
        } catch (Exception e) {
            // Elements not present in the people card
        }
    }

    protected boolean userAlreadyLikes() {
        return ArrayUtils.valueExists(ModelSingleton.instance().myLikes.get(), getUid());
    }

    private boolean cardAlreadyDeleted() {
        return ArrayUtils.valueExists(ModelSingleton.instance().deletedCards.get(), getUid());
    }

    private boolean cardAlreadyFlagged() {
        return ArrayUtils.valueExists(ModelSingleton.instance().flaggedCards.get(), getUid());
    }

    @SuppressWarnings("deprecation")
    private void likeButtonClicked(@NonNull @nonnull final View view, final int color, final int highlightColor) {
        final Toast toast = makeToast(R.layout.basic_toast, Toast.LENGTH_SHORT);
        if (!userAlreadyLikes()) {
            addLocalUserLike();
            ((ImageView) view.findViewById(R.id.card_like_button)).setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.card_navbar_liked));
            ((ImageView) toast.getView().findViewById(R.id.toast_image)).setImageDrawable(context.getResources().getDrawable(R.drawable.card_navbar_liked));
            ((TextView) toast.getView().findViewById(R.id.toast_text)).setText(context.getString(R.string.card_like_toast_message));
           // toast.getView().setBackgroundColor(highlightColor);
           // toast.show();
        } else {
            deleteLocalUserLike();
            ((ImageView) view.findViewById(R.id.card_like_button)).setImageDrawable(view.getContext().getResources().getDrawable(R.drawable.card_navbar_like));
            ((ImageView) toast.getView().findViewById(R.id.toast_image)).setImageDrawable(context.getResources().getDrawable(R.drawable.card_navbar_like));
            ((TextView) toast.getView().findViewById(R.id.toast_text)).setText(context.getString(R.string.card_unlike_toast_message));
        }
         toast.getView().setBackgroundColor(color);
         toast.show();
    }

    private void commentButtonClicked(@NonNull @nonnull final View view, final int color, final int highlightColor) {
        final Context context = view.getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogView = View.inflate(view.getContext(), R.layout.comment_box, null);
        builder.setView(dialogView);
        EditText commentText = (EditText) dialogView.findViewById(R.id.comment_text);
        builder.setPositiveButton(R.string.gen_ok, (dialogInterface, i) -> addLocalUserComment(commentText.getText().toString()))
                .setNegativeButton(R.string.gen_cancel, (dialogInterface, i) -> {
                });
        builder.create().show();
    }

    private void addLocalUserComment(String s) {
        if (TextUtils.isEmpty(s))
            return;
        Comment comment = new Comment(this.getUid(), ModelSingleton.instance().myTag.get(), ModelSingleton.instance().myIdTag.get(), s.trim());
        ServiceSingleton.instance().peerDiscoveryService().localUserCommentsACardAsync(comment.toJSONString())
                .then(() -> {
                    ModelSingleton.instance().myComments.set(
                            ServiceSingleton.instance().peerDiscoveryService().getLocalUserComments());
                })
                .fork();
    }

    protected void deleteLocalUserComment(Comment comment) {
        if (comment == null)
            return;
        ServiceSingleton.instance().peerDiscoveryService().localUserRemovesCommentAsync(comment.toJSONString())
                .then(() -> {
                    ModelSingleton.instance().myComments.set(
                            ServiceSingleton.instance().peerDiscoveryService().getLocalUserComments());
                })
                .fork();
    }

    private void deleteCardButtonClicked(@NonNull @nonnull final View view) {
        final Context context = view.getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(R.string.topic_delete_confirmation)
                .setPositiveButton(R.string.gen_ok, (dialogInterface, i) -> deleteCard())
                .setNegativeButton(R.string.gen_cancel, (dialogInterface, i) -> {
                });
        builder.create().show();
    }

    private void flagCardButtonClicked(@NonNull @nonnull final View view) {
        final Context context = view.getContext();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(R.string.topic_flag_confirmation)
                .setPositiveButton(R.string.gen_ok, (dialogInterface, i) -> flagCard())
                .setNegativeButton(R.string.gen_cancel, (dialogInterface, i) -> {
                });
        builder.create().show();
    }

    private void addLocalUserLike() {
        if (!userAlreadyLikes()) {
            ServiceSingleton.instance().peerDiscoveryService().localUserLikesACardAsync(this.getUid())
                    .then(() -> {
                        ModelSingleton.instance().myLikes.set(
                                ServiceSingleton.instance().peerDiscoveryService().getLocalUserLikes());
                    })
                    .fork();
        }
    }

   private void deleteLocalUserLike() {
        if (userAlreadyLikes()) {
            ServiceSingleton.instance().peerDiscoveryService().localUserUnlikesACardAsync(this.getUid())
                    .then(() -> {
                        ModelSingleton.instance().myLikes.set(
                                ServiceSingleton.instance().peerDiscoveryService().getLocalUserLikes());
                    })
                    .fork();
        }
    }

    private void deleteCard() {
        if (!cardAlreadyDeleted()) {
            ServiceSingleton.instance().peerDiscoveryService().localUserDeletesACardAsync(getUid())
                    .then(() -> ModelSingleton.instance().deleteCard(getUid()))
                    .fork();
        }
    }

    private void flagCard() {
        if (!cardAlreadyFlagged()) {
            ServiceSingleton.instance().peerDiscoveryService().localUserFlagsACardAsync(getUid())
                    .then(() -> ModelSingleton.instance().flagCard(getUid()))
                    .fork();
        }
    }

    protected void userButtonClicked(View view) {
        ModelSingleton modelSingleton = ModelSingleton.instance();
        List<Peer> peerModel = modelSingleton.getPeerModel();

        for (final Peer p : peerModel) {
            Log.d("BaseCard", "authorId=" + authorId + ", idTag=" + p.idTag);
            if (authorId != null && authorId.equals(p.idTag)) {
                displayAuthorDialog(p.tag, p.aboutMe);
                return;
            }
        }
        Toast.makeText(context, String.format(context.getResources().getString(R.string.people_user_not_found), author), Toast.LENGTH_LONG).show();
    }

    public void displayAuthorDialog(String name, String aboutMe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View authorLayout = inflater.inflate(R.layout.peer_card_layout, null);
        ((TextView) authorLayout.findViewById(R.id.peer_tag_text)).setText(name);
        ((TextView) authorLayout.findViewById(R.id.peer_about_me_text)).setText(aboutMe);
        builder.setView(authorLayout);
        builder.setCancelable(false)
                .setPositiveButton("Ok", (dialog, id) -> {
                });
        builder.create().show();
    }

    /**
     * Specify the layout, customize what comes back if you like, subscribeTarget .show() the Toast
     *
     * @param layoutResource
     * @param toastDuration
     */
    @NonNull
    @nonnull
    protected Toast makeToast(final int layoutResource, final int toastDuration) {
        final View view = inflater.inflate(layoutResource, null);
        final Toast toast = new Toast(context);
        toast.setView(view);
        toast.setDuration(toastDuration);

        return toast;
    }

    @NonNull
    @nonnull
    protected View inflateView(final int resource, @NonNull @nonnull final ViewGroup parentView) {
        return this.inflater.inflate(resource, parentView, false);
    }

    @Override // ICard
    public long getUid() {
        return uid;
    }

    @Override // INamed
    @NonNull
    @nonnull
    public String getName() {
        return this.name;
    }

    @NonNull
    @nonnull
    public String getAuthor() {
        if (author == null) {
            //assertTrue("Author is non-empty", author.length() > 0);
            return context.getResources().getString(R.string.people_tag_anonymous);
        }

        return author;
    }

    public void setAuthor(@NonNull @nonnull final String author, @NonNull @nonnull final String authorId) {
        this.author = author;
        this.authorId = authorId;
    }

    @NonNull
    @nonnull
    public Date getDate() {
        if (date == null) {
            //FIXME set date=newDate() ?
            return new Date();
        }

        return date;
    }


    public void setDate(@NonNull @nonnull final Date date) {
        this.date = date;
    }


    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(final boolean flagged) {
        this.flagged = flagged;
    }
}
