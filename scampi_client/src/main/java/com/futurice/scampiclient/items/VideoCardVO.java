package com.futurice.scampiclient.items;

import android.support.annotation.NonNull;
import android.util.Log;

import com.futurice.cascade.i.nonnull;

import java.io.File;

/**
 * Type sent and received by VideoService.
 * <p>
 * VO = "Value Object", a data structure
 *
 * @author teemuk
 */
public final class VideoCardVO extends ScampiCard {
    @NonNull
    @nonnull
    public final String topic;    // Topic to which this card is attached
    public final long topicUid;    // Unique id of the topic to which this card is attached
    @NonNull
    @nonnull
    public final File videoFile;    // File that contains the video
    @NonNull
    @nonnull
    public final String videoType;    // Type of the video (file extension)
    @NonNull
    @nonnull
    public final File thumbnailFile;    // File that contains thumbnail for the video (can be null)
    @NonNull
    @nonnull
    public final String thumbnailType;    // Type of the thumbnail image (file extension)
    @NonNull
    @nonnull
    public final String title;    // Title of this card.
    public final long creationTime;    // Time when this card was created
    @NonNull
    @nonnull
    public final String author;    // Author of this card.
    @NonNull
    @nonnull
    public final String authorId;    // Author of this card.

    public final static String TAG = VideoCardVO.class.getName();


    public VideoCardVO(@NonNull @nonnull final String topic,
                       final long topicUid,
                       @NonNull @nonnull final File videoFile,
                       @NonNull @nonnull final String videoType,
                       @NonNull @nonnull final File thumbnailFile,
                       @NonNull @nonnull final String thumbnailType,
                       @NonNull @nonnull final String title,
                       final long creationTime,
                       final long uid,
                       @NonNull @nonnull final String author,
                       @NonNull @nonnull final String authorId,
                       @NonNull @nonnull final String eventId) {
        super(eventId);

        this.topic = topic;
        this.topicUid = topicUid;
        this.videoFile = videoFile;
        this.videoType = videoType;
        this.thumbnailFile = thumbnailFile;
        this.thumbnailType = thumbnailType;
        this.title = title;
        this.creationTime = creationTime;
        this.uid = uid;
        this.author = author;
        this.authorId = authorId;
        Log.d(TAG, "Created a video card with author " + author + ", " + authorId);
    }

    /**
     * Returns a aboutMe that should be used for a card that displays this message.
     *
     * @return card aboutMe to use for this message
     */
    @NonNull
    @nonnull
    public final String getCardName() {
        return this.topic
                + "-" + this.topicUid
                + "-" + this.author
                + "-" + this.creationTime
                + "-" + this.uid;
    }

    @Override
    @NonNull
    @nonnull
    public final String toString() {
        return "(topic:" + this.topic
                + ", topicUid:" + this.topicUid
                + ", path:" + this.videoFile.getAbsolutePath()
                + ", videoType:" + this.videoType
                + ", creationTime:" + creationTime
                + ", uid:" + uid
                + ", author:" + author + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final VideoCardVO that = (VideoCardVO) o;

        return creationTime == that.creationTime
                && topicUid == that.topicUid
                && uid == that.uid
                && author.equals(that.author)
                && authorId.equals(that.authorId)
                && topic.equals(that.topic);

    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result *= 31 + (int) (creationTime ^ (creationTime >>> 32));
        result *= 31 + (int) (uid ^ (uid >>> 32));
        result *= 31 + (int) (topicUid ^ (topicUid >>> 32));
        result *= 31 + author.hashCode();
        return result;
    }
}
