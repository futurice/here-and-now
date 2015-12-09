package com.futurice.hereandnow.card;

import android.content.*;
import android.graphics.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.HereAndNowApplication;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.utils.FileUtils;
import com.futurice.scampiclient.BigScreenControllerService;
import com.futurice.scampiclient.items.VideoCardVO;

import java.io.*;

import static com.futurice.cascade.Async.*;

/**
 * Card that contains a video and allows sending to a big screen for playing.
 *
 * @author teemuk
 */
public class VideoCard extends ImageCard {
    public static final int CARD_TYPE = 1;    // Needed for ListView recycling.
    private static final long MIN_SEND_INTERVAL_MILLIS = 5 * 1000;
    private static final String TAG = VideoCard.class.getSimpleName();

    @NonNull
    @nonnull
    private Uri videoUri = Uri.EMPTY;
    @Nullable
    @nullable
    private VideoCardVO videoCardVO;
    private long lastSend;     // Command button spamming prevention

    public VideoCard(final String name, long uid, Context context) {
        super(name, uid, context);
    }

    /**
     * Sets the video message that this card will display. The message should really
     * be passed in constructor, but for ease of testing purposes do it like this for now.
     *
     * @param videoCardVO video message to display
     */
    public final void setVideoCardVO(@NonNull @nonnull final VideoCardVO videoCardVO) {
        Log.d(TAG, "setVideoCard: " + videoCardVO.author + ", " + videoCardVO.authorId);
        this.videoCardVO = videoCardVO;
        this.setVideoUri(Uri.fromFile(videoCardVO.videoFile));

        createVideoThumbnail(videoCardVO);
    }

    /**
     * Helper function for saving a local thumbnail of the video file.
     *
     * @param videoFile
     * @return
     */
    @NonNull
    @nonnull
    public static Uri createThumbnail(@NonNull @nonnull final Uri videoFile) {
        try {
            final String filePath = assertNotNull(FileUtils.getPath(HereAndNowApplication.getStaticContext(), videoFile));
            final File localVideoFile = new File(filePath);
            Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.MINI_KIND);

            final File directory = new File(Environment.getExternalStorageDirectory() + "/video_thumbnails/");
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new RuntimeException("Can not create directory: " + directory);
                }
            }

            final File file = new File(directory.getAbsolutePath() + "/" + localVideoFile.getName() + ".jpg");
            final OutputStream outStream = new FileOutputStream(file);

            try {
                bmThumbnail.compress(Bitmap.CompressFormat.JPEG, 85, outStream);
                outStream.flush();
            } finally {
                outStream.close();
            }

            return Uri.fromFile(file);

        } catch (Exception e) {
            ee(VideoCard.class.getSimpleName(), "Problem writing thumbnail", e);
        }

        return Uri.EMPTY;
    }

    private void createVideoThumbnail(@NonNull @nonnull final VideoCardVO videoCard) {
        dd(origin, "Setting thumbnail for: " + videoCard.getCardName());

        Uri videoUri = Uri.fromFile(videoCard.videoFile);
        Uri thumbnailUri = createThumbnail(videoUri);

        if (thumbnailUri != Uri.EMPTY) {
            this.setImageUri(thumbnailUri);
        }

        dd(origin, "Thumbnail set: " + thumbnailUri);
    }

    @Override
    public void updateView(@NonNull @nonnull final View view, final int color, final int highlightColor) {
        super.updateView(view, color, highlightColor);

        vv(origin, "updateView");
        view.findViewById(R.id.video_card_play_button_image).setVisibility(View.VISIBLE);
      //  view.findViewById(R.id.video_card_tobigscreen_layout).setVisibility(View.VISIBLE);

      /*  final ImageView bigScreenButton = (ImageView) view.findViewById(R.id.video_card_tobigscreen_button);
        bigScreenButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() - this.lastSend > MIN_SEND_INTERVAL_MILLIS) {
                final VideoCardVO vo = assertNotNull(this.videoCardVO);
                ServiceSingleton.instance().bigScreenControllerService().sendMessageAsync(
                        new BigScreenControllerService.Command(
                                BigScreenControllerService.Command.PLAY_COMMAND,
                                vo.uid
                        )
                );
                this.lastSend = System.currentTimeMillis();
            }
        });
*/
        final View.OnClickListener onClickListener = v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(this.getVideoUri(), "video/*");
            this.context.startActivity(intent);
        };

        view.findViewById(R.id.card_image).setOnClickListener(onClickListener);
        view.findViewById(R.id.video_card_play_button_image).setOnClickListener(onClickListener);
    }

    @Override // Card
    public int getType() {
        return CARD_TYPE;
    }

    @NonNull
    @nonnull
    public Uri getVideoUri() {
        return videoUri;
    }

    public void setVideoUri(@NonNull @nonnull final Uri videoUri) {
        this.videoUri = videoUri;
    }
}
