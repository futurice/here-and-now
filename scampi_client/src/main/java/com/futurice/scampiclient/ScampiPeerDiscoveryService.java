package com.futurice.scampiclient;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.Async;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.scampiclient.items.Peer;
import com.futurice.scampiclient.utils.ArrayUtils;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;

import static com.futurice.cascade.Async.dd;

/**
 * Peer discovery service implemented on Scampi.
 *
 * @author teemuk
 */
public final class ScampiPeerDiscoveryService extends HereAndNowService<Peer> {
    private static final String SERVICE_NAME = "com.futurice.hereandnow.profiles";
    private static final String TAG_FIELD_LABEL = "Tag"; // Key for the ScampiMessage content item that contains the user's chosen anonymous identity id
    private static final String ID_TAG_FIELD_LABEL = "IDTag"; // Key for the ScampiMessage content item that contains the a unique user ID
    private static final String ABOUT_ME_FIELD_LABEL = "AboutMe"; // Key for the ScampiMessage content item that contains the aboutMe
    private static final String TAG_FIELD_LIKES = "Likes"; // A newline-delimited set of uid Ids which the user has liked. The maximum length is FIFO limited
    private static final String TAG_FIELD_DELETIONS = "Deletions"; // A newline-delimited set of uid Ids which the user has deleted. The maximum length is FIFO limited
    private static final String TAG_FIELD_FLAGS = "Flags"; // A newline-delimited set of uid Ids which the user has flagged as inappropriate. The maximum length is FIFO limited
    private static final String TAG_FIELD_COMMENTS = "Comments"; // A newline-delimited set of user's comments. The maximum length is FIFO limited
    private static final String TIMESTAMP_FIELD_LABEL = "Timestamp"; // Key for the creation timestamp for the message
    private static final String APPTAG = "com.futurice.hereandnow.localprofile"; // App tag for local advertisement message
    public static final int MESSAGE_LIFETIME_SECONDS = 25; // Lifetime for the generated ScampiMessages
    public static final int MESSAGE_BROADCAST_INTERVAL_SECONDS = 20;

    private final ImmutableValue<String> origin;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> scheduledAdvert;
    private volatile Peer localUser;

    /**
     * Creates a new discovery service that will receive peer updates and
     * advertise the local user.
     *
     * @param scampiHandler handler to the Scampi instance to use for networking
     * @param localUser     local user to advertise
     */
    public ScampiPeerDiscoveryService(@NonNull @nonnull final ScampiHandler scampiHandler,
                                      @NonNull @nonnull final Peer localUser) {
        super(SERVICE_NAME, MESSAGE_LIFETIME_SECONDS, TimeUnit.MINUTES,
                false, scampiHandler);

        this.origin = Async.originAsync();
        this.localUser = localUser;
    }

    /**
     * Starts advertising the local user over Scampi.
     */
    public final void startAdvertisingLocalUser() {
        // Do nothing if we're already advertising
        if (this.scheduledAdvert != null) {
            dd(this, origin, "startAdvertisingLocalUser() called multiple times.");
            return;
        }

        // Schedule and advert at the message lifetime rate. This way a new
        // advertisement is created every time the old one expires from the
        // network.
        this.scheduledAdvert = this.timer.scheduleAtFixedRate(
                () -> broadcastUserAdvertAsync().fork(),
                0,
                MESSAGE_BROADCAST_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    private IAltFuture<?, SCAMPIMessage> broadcastUserAdvertAsync() {
        Log.d("Scampi", "broadcasting user advert");
        return scampiHandler.sendMessageAsync(SERVICE_NAME, getLocalAdvert())
                .then(() -> {
                    scampiHandler.sendMessageAsync(SERVICE_NAME, getLocalAdvert());
                });
    }

    /**
     * The list of card UIDs liked by this user (length is limited, FIFO expire when length is too long)
     *
     * @return
     */
    @NonNull
    @nonnull
    public long[] getLocalUserLikes() {
        return localUser.cardLikeUniqueIds;
    }

    /**
     * The list of comments by this user (length is limited, FIFO expire when length is too long)
     *
     * @return
     */
    @NonNull
    @nonnull
    public String[] getLocalUserComments() {
        return localUser.comments;
    }

    /**
     * The list of card UIDs deleted by all user (length is limited, FIFO expire when length is too long)
     *
     * @return
     */
    @NonNull
    @nonnull
    public long[] getLocalUserDeletions() {
        return localUser.cardDeletionUniqueIds;
    }

    /**
     * The list of card UIDs flagged by all user (length is limited, FIFO expire when length is too long)
     *
     * @return
     */
    @NonNull
    @nonnull
    public long[] getLocalUserFlags() {
        return localUser.cardDeletionUniqueIds;
    }

    /**
     * The user just pressed "like" on a card with this UID
     *
     * @param cardUniqueId
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserLikesACardAsync(final long cardUniqueId) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                ArrayUtils.prepend(user.cardLikeUniqueIds, cardUniqueId),
                user.cardDeletionUniqueIds,
                user.cardFlagUniqueIds,
                user.comments,
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }

    /**
     * The user just pressed "unlike" on a card with this UID
     *
     * @param cardUniqueId
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserUnlikesACardAsync(final long cardUniqueId) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                ArrayUtils.remove(user.cardLikeUniqueIds, cardUniqueId),
                user.cardDeletionUniqueIds,
                user.cardFlagUniqueIds,
                user.comments,
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }

    /**
     * The user commented on a card
     *
     * @param commentJSON
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserCommentsACardAsync(String commentJSON) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                user.cardLikeUniqueIds,
                user.cardDeletionUniqueIds,
                user.cardFlagUniqueIds,
                ArrayUtils.prepend(user.comments, commentJSON),
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }

    /**
     * The user removes a comment
     *
     * @param commentJSON
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserRemovesCommentAsync(String commentJSON) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                user.cardLikeUniqueIds,
                user.cardDeletionUniqueIds,
                user.cardFlagUniqueIds,
                ArrayUtils.remove(user.comments, commentJSON),
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }



    /**
     * The user just pressed "delete" on a card with this UID
     *
     * @param cardUniqueId
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserDeletesACardAsync(final long cardUniqueId) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                user.cardLikeUniqueIds,
                ArrayUtils.prepend(user.cardDeletionUniqueIds, cardUniqueId),
                user.cardFlagUniqueIds,
                user.comments,
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }

    /**
     * The user just pressed "flag" on a card with this UID
     *
     * @param cardUniqueId
     * @return
     */
    @NonNull
    @nonnull
    public IAltFuture<?, SCAMPIMessage> localUserFlagsACardAsync(final long cardUniqueId) {
        final Peer user = localUser;

        localUser = new Peer(user.tag, user.idTag, user.aboutMe,
                user.cardLikeUniqueIds,
                user.cardDeletionUniqueIds,
                ArrayUtils.prepend(user.cardFlagUniqueIds, cardUniqueId),
                user.comments,
                System.currentTimeMillis());

        return broadcastUserAdvertAsync();
    }

    /**
     * Stops advertising the local user over Scampi.
     */
    public final void stopAdvertisingLocalUser() {
        if (this.scheduledAdvert != null) {
            this.scheduledAdvert.cancel(false);
            this.scheduledAdvert = null;
        }
    }

    /**
     * Stop the service, for example when the app is shutting down
     */
    @Override // AbstractScampiService
    public void stop() {
        stopAdvertisingLocalUser();
        super.stop();
    }

    /**
     * Refreshes the local advert message. Can be called after updating
     * the local user's data in order to push the update into the network
     * immediately
     */
    public final void refreshLocalAdvert() {
        this.stopAdvertisingLocalUser();
        this.startAdvertisingLocalUser();
    }

    /**
     * Updates the local user's data that is advertised to other users.
     * The updated data will be reflected in the next message refresh,
     * if you want to immediately sendEventMessage out the new data, call
     * {@link #refreshLocalAdvert()}.
     *
     * @param localUser local user to advertise
     */
    public final void updateLocalUser(@NonNull @nonnull final Peer localUser) {
        this.localUser = localUser;
    }

    @NonNull
    @nonnull
    @Override // HereAndNowService
    protected Peer getValueFieldFromIncomingMessage(@NonNull @nonnull final SCAMPIMessage scampiMessage)
            throws IOException {
        // Precondition check
        if (!scampiMessage.hasString(ABOUT_ME_FIELD_LABEL)) {
            throw new IOException("No username in incoming peer advert.");
        }

        if (!scampiMessage.hasString(ID_TAG_FIELD_LABEL)) {
            throw new IOException("No id tag in incoming peer advert.");
        }

        final String name = scampiMessage.getString(ABOUT_ME_FIELD_LABEL);
        final String tag = this.getStringOrNull(scampiMessage, TAG_FIELD_LABEL);
        final String idTag = this.getStringOrNull(scampiMessage, ID_TAG_FIELD_LABEL);
        final long[] likes = ArrayUtils.parseArray(this.getStringOrNull(scampiMessage, TAG_FIELD_LIKES));
        final long timestamp = scampiMessage.getInteger(TIMESTAMP_FIELD_LABEL);
        final long[] deletions = ArrayUtils.parseArray(this.getStringOrNull(scampiMessage, TAG_FIELD_DELETIONS));
        final long[] flags = ArrayUtils.parseArray(this.getStringOrNull(scampiMessage, TAG_FIELD_FLAGS));
        final String[] comments = ArrayUtils.parseStringArray(this.getStringOrNull(scampiMessage, TAG_FIELD_COMMENTS));

        return new Peer(tag, idTag, name, likes, deletions, flags, comments, timestamp);
    }

    @Override // HereAndNowService
    protected void addValueFieldToOutgoingMessage(
            @NonNull @nonnull final SCAMPIMessage scampiMessage,
            @NonNull @nonnull final Peer value) {
        Log.d("ScampiPeerDiscovery", "outgoing tag=" + value.tag);
        scampiMessage.putString(ID_TAG_FIELD_LABEL, value.idTag);
        scampiMessage.putString(ABOUT_ME_FIELD_LABEL, value.aboutMe);
        scampiMessage.putInteger(TIMESTAMP_FIELD_LABEL, System.currentTimeMillis());
        scampiMessage.putString(TAG_FIELD_LIKES, ArrayUtils.musterArray(value.cardLikeUniqueIds));
        scampiMessage.putString(TAG_FIELD_DELETIONS, ArrayUtils.musterArray(value.cardDeletionUniqueIds));
        scampiMessage.putString(TAG_FIELD_FLAGS, ArrayUtils.musterArray(value.cardFlagUniqueIds));
        scampiMessage.putString(TAG_FIELD_COMMENTS, ArrayUtils.musterArray(value.comments));
        this.putStringIfNotNull(scampiMessage, TAG_FIELD_LABEL, value.tag);
    }

    @Override
    protected void notifyMessageExpired(@NonNull @nonnull String key) {
        //TODO
    }

    @NonNull
    @nonnull
    @Override // HereAndNowService
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(@NonNull @nonnull final Peer val) {
        throw new UnsupportedOperationException("Peer discovery service " +
                "cannot sendEventMessage messages.");
    }

    @Nullable
    @nullable
    private String getStringOrNull(
            @NonNull @nonnull final SCAMPIMessage message,
            @NonNull @nonnull final String field) {
        if (!message.hasString(field)) {
            return null;
        }

        return message.getString(field);
    }

    private void putStringIfNotNull(
            @NonNull @nonnull final SCAMPIMessage message,
            @NonNull @nonnull final String field,
            @Nullable @nullable final String value) {
        if (value == null) {
            return;
        }
        message.putString(field, value);
    }

    @NonNull
    @nonnull
    private SCAMPIMessage getLocalAdvert() {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();

        builder.lifetime(MESSAGE_LIFETIME_SECONDS, TimeUnit.SECONDS);
        builder.appTag(APPTAG);
        final SCAMPIMessage scampiMessage = builder.build();
        this.addValueFieldToOutgoingMessage(scampiMessage, this.localUser);

        return scampiMessage;
    }
}
