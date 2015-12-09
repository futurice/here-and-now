package com.futurice.scampiclient;

import android.annotation.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;

import com.futurice.cascade.active.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import fi.tkk.netlab.dtn.scampi.applib.*;

import static com.futurice.cascade.Async.*;


/**
 * Service for broadcasting and receiving videos. The videos are specified
 * with {@link android.net.Uri}, e.g., received from the Android media picker.
 *
 * @author teemuk
 */
public final class VideoUriBroadcastService extends HereAndNowService<Uri> {

    //======================================================================//
    // Constants
    //======================================================================//
    private static final String TAG = VideoBroadcastService.class.getSimpleName();
    /**
     * Scampi service aboutMe to use for created messages.
     */
    private static final String SERVICE_NAME = "VideoBroadcastService";
    /**
     * Key for the ScampiMessage content item that contains the video.
     */
    private static final String VIDEO_DATA_FIELD_LABEL = "VideoField";
    /**
     * Key for the original file aboutMe for the video.
     */
    private static final String VIDEO_FILENAME_FIELD_LABEL = "FileNameField";
    /**
     * Key for the creation timestamp for the message.
     */
    private static final String TIMESTAMP_FIELD_LABEL = "Timestamp";
    /**
     * Key for a uid ID for the message (timestamp + id = globally uid).
     */
    private static final String ID_FIELD_LABEL = "Id";
    /**
     * Lifetime for the generated ScampiMessages.
     */
    private static final int MESSAGE_LIFETIME_MINUTES = 10;
    //======================================================================//

    //======================================================================//
    // Configuration
    //======================================================================//
    private
    @NonNull
    @nonnull
    final File storageDir;
    private final Context context;
    //======================================================================//

    //======================================================================//
    // Instance vars
    //======================================================================//
    private final Random rng = new Random();
    //======================================================================//


    //======================================================================//
    // Instantiation
    //======================================================================//

    /**
     * Create a new service instance. This service also receives videos, which
     * will be stored in the given directory (e.g., Android public video directory,
     * or a private directory of the application).
     *
     * @param scampiHandler Handler to the Scampi AppLib instance used to
     *                      communicate with the Scampi router instance
     * @param storageDir    Directory where to store incoming videos.
     * @param context       Android context, used to, e.g., resolve the Uri to file
     */
    public VideoUriBroadcastService(
            @NonNull @nonnull final ScampiHandler scampiHandler,
            @NonNull @nonnull final File storageDir,
            @NonNull @nonnull final Context context) {
        super(SERVICE_NAME, MESSAGE_LIFETIME_MINUTES, TimeUnit.MINUTES,
                false, scampiHandler);
        this.storageDir = storageDir;
        this.context = context;
    }

    /**
     * Creates a new service instance, using the public movie directory for storage.
     *
     * @param scampiHandler Handler to the Scampi AppLib instance used to
     *                      communicate with the Scampi router instance
     * @param context       Android context, used to, e.g., resolve the Uri to file
     */
    public VideoUriBroadcastService(@NonNull @nonnull final ScampiHandler scampiHandler, @NonNull @nonnull final Context context) {
        this(scampiHandler, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), context);
        dd(TAG, origin, "Created VideoUriBroadcastService");
    }
    //======================================================================//


    //======================================================================//
    // HereAndNowService implementation
    //======================================================================//
    @NonNull
    @nonnull
    @Override
    protected Uri getValueFieldFromIncomingMessage(@NonNull @nonnull final SCAMPIMessage scampiMessage)
            throws IOException {
        // Precondition checks
        if (!scampiMessage.hasBinary(VIDEO_DATA_FIELD_LABEL)) {
            dd(TAG, this.origin, "No video in incoming message.");
            throw new IOException("No video in incoming message.");
        }

        // Get destination
        final String id = this.getStorageFileNameForVideo(scampiMessage);
        final File destination = new File(this.storageDir, id);

        // The file might already exist, if so, assume this message is
        // a duplicate and return the existing file.
        if (destination.exists()) {
            dd(TAG, this.origin, "Destination file already exists.");
            if (destination.isFile()) {
                return Uri.fromFile(destination);
            } else {
                throw new IOException("Path '" + destination.getAbsolutePath()
                        + "' already exists and is not a file.");
            }
        }

        // Transfer the video to the destination and return Uri
        dd(TAG, origin, "Moving incoming video: " + id);
        scampiMessage.moveBinary(VIDEO_DATA_FIELD_LABEL, destination);
        dd(TAG, origin, "Moved incoming video: " + id);
        return Uri.fromFile(destination);
    }

    @Override
    protected final void addValueFieldToOutgoingMessage(
            @NonNull @nonnull final SCAMPIMessage scampiMessage,
            @NonNull @nonnull final Uri value) {
        // Get the file path
        dd(TAG, origin, "Sending video: " + value.getPath());
        String path = this.getPathForUri(this.context, value);
        if (path == null) {
            dd(TAG, this.origin, "Failed get path for Uri '" + value.getPath() + "'");
            return;
        }

        // Get file
        File file = new File(path);
        if (!file.isFile()) {
            dd(TAG, this.origin, "Uri is not an existing file.");
            return;
        } else {
            dd(TAG, this.origin, "Publishing video '" + file.getAbsolutePath() + "'");
        }

        // Add to message
        scampiMessage.putBinary(VIDEO_DATA_FIELD_LABEL, file, false);

        // Add other fields
        scampiMessage.putString(VIDEO_FILENAME_FIELD_LABEL, file.getName());
        scampiMessage.putInteger(ID_FIELD_LABEL, this.rng.nextInt(Integer.MAX_VALUE));
        scampiMessage.putInteger(TIMESTAMP_FIELD_LABEL, System.currentTimeMillis());
    }

    @Override
    protected void notifyMessageExpired(@NonNull @nonnull String key) {
        //TODO
    }

    @NonNull
    @nonnull
    @Override
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(@NonNull @nonnull final Uri val) {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(MESSAGE_LIFETIME_MINUTES, TimeUnit.MINUTES);
        final SCAMPIMessage scampiMessage = builder.build();
        this.addValueFieldToOutgoingMessage(scampiMessage, val);

        return scampiHandler.sendMessageAsync(getName(), scampiMessage)
                .fork();
    }
    //======================================================================//


    //======================================================================//
    // Private
    //======================================================================//
    /**
     * Resolves the file path for a given Uri.
     * WARNING: This might not work when targeting KitKat.
     *
     * @param uri   Uri of a file
     * @return path to the file
     */
    /*private String getPathForUri( Uri uri ) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };

        Cursor cursor = null;
        String picturePath = null;
        try {
            cursor = this.context.getContentResolver()
                    .query(uri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return picturePath;
    }*/

    /**
     * Constructs a globally uid file aboutMe for the video in the message.
     *
     * @param message message containing the video to construct the filename for
     * @return globally uid file aboutMe for the contained video
     */
    @NonNull
    @nonnull
    private String getStorageFileNameForVideo(@NonNull @nonnull final SCAMPIMessage message) {
        // Try to construct the aboutMe from the message fields, if some
        // field is missing, use the appTag.
        if (!message.hasString(VIDEO_FILENAME_FIELD_LABEL)
                || !message.hasInteger(TIMESTAMP_FIELD_LABEL)
                || !message.hasInteger(ID_FIELD_LABEL)) {
            return message.getAppTag();
        }

        // Construct aboutMe: filename-timestamp-id.extension
        final String fileName = message.getString(VIDEO_FILENAME_FIELD_LABEL);
        final long id = message.getInteger(TIMESTAMP_FIELD_LABEL);
        final long timestamp = message.getInteger(ID_FIELD_LABEL);

        // Preserve the extension if the aboutMe contains it
        final String s;
        if (fileName.contains(".")) {
            final int dotLoc = fileName.lastIndexOf('.');
            final String prefix = fileName.substring(0, dotLoc);
            final String suffix = fileName.substring(dotLoc);
            s = prefix + "-" + timestamp + "-" + id + suffix;
        } else {
            s = fileName + "-" + timestamp + "-" + id;
        }
        dd(TAG, origin, "Getting storage aboutMe for video: " + s);
        return s;
    }
    //======================================================================//


    //======================================================================//

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    @TargetApi(21) // For KitKat specific DocumentContract.isDocumentUri
    @Nullable
    @nullable
    private String getPathForUri(@NonNull @nonnull final Context context, @NonNull @nonnull final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    //TODO Is this method a duplicate of getDataColumn somewhere else? If so, consider simplification

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    @nullable
    private String getDataColumn(
            @NonNull @nonnull final Context context,
            @Nullable @nullable final Uri uri,
            @Nullable @nullable final String selection,
            @Nullable @nullable final String[] selectionArgs) {
        if (uri == null) {
            return null;
        }

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    //======================================================================//
}
