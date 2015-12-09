package com.futurice.hereandnow.singleton;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.AsyncBuilder;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.scampiclient.BigScreenControllerService;
import com.futurice.scampiclient.EventService;
import com.futurice.scampiclient.PictureCardService;
import com.futurice.scampiclient.ScampiHandler;
import com.futurice.scampiclient.ScampiPeerDiscoveryService;
import com.futurice.scampiclient.VideoService;
import com.futurice.scampiclient.VideoUriBroadcastService;
import com.futurice.scampiclient.items.Peer;

import java.io.File;

import static com.futurice.cascade.Async.assertNotNull;

/**
 * Singleton class for application-wide services and settings
 */
public class ServiceSingleton {

    @Nullable
    @nullable
    private static ServiceSingleton instance;
    @NonNull
    @nonnull
    private final ScampiHandler scampiHandler;
    @NonNull
    @nonnull
    private final Context context;

    // Scampi services
    // private static ChatBotService chatBotTopLineService;
    @Nullable
    @nullable
    private ScampiPeerDiscoveryService peerDiscoveryService;
    @Nullable
    @nullable
    private PictureCardService pictureCardService;
    @Nullable
    @nullable
    private VideoService videoService;
    @Nullable
    @nullable
    private VideoUriBroadcastService videoBroadcastService;
    @Nullable
    @nullable
    private BigScreenControllerService bigScreenControllerService;
    @Nullable
    @nullable
    private EventService eventService;

    private ServiceSingleton(@NonNull @nonnull final Context c) {
        context = c;
        new AsyncBuilder(context).build(); // TODO Disable for production builds
        scampiHandler = new ScampiHandler();
    }

    public static void create(@NonNull @nonnull final Context context) {
        instance = new ServiceSingleton(context);
    }

    @NonNull
    @nonnull
    public static ServiceSingleton instance() {
        return assertNotNull(instance);
    }

    @NonNull
    @nonnull
    public ScampiHandler scampiHandler() {
        return scampiHandler;
    }

    @NonNull
    @nonnull
    public ScampiPeerDiscoveryService peerDiscoveryService() {
        if (peerDiscoveryService == null) {
            peerDiscoveryService = new ScampiPeerDiscoveryService(
                    scampiHandler,
                    new Peer(ModelSingleton.instance().myTag.get(),
                            ModelSingleton.instance().myIdTag.get(),
                            ModelSingleton.instance().myAboutMe.get(),
                            ModelSingleton.instance().myLikes.get(),
                            ModelSingleton.instance().deletedCards.get(),
                            ModelSingleton.instance().flaggedCards.get(),
                            ModelSingleton.instance().myComments.get(),
                            System.currentTimeMillis())
            );
        }

        return peerDiscoveryService;
    }

    @NonNull
    @nonnull
    public PictureCardService pictureCardService() {
        if (pictureCardService == null) {
            final File picCardStorage = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "HereAndNowPics"
            );
            pictureCardService = new PictureCardService(scampiHandler, picCardStorage, context);
        }
        return pictureCardService;
    }

    @NonNull
    @nonnull
    public VideoService videoService() {
        if (videoService == null) {
            final File receivedVideoStorage = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "HereAndNowVideos"
            );
            videoService = new VideoService(scampiHandler, receivedVideoStorage, context);
        }
        return videoService;
    }

    @NonNull
    @nonnull
    public VideoUriBroadcastService videoBroadcastService() {
        if (videoBroadcastService == null) {
            videoBroadcastService = new VideoUriBroadcastService(scampiHandler, context);
        }
        return videoBroadcastService;
    }

    @NonNull
    @nonnull
    public BigScreenControllerService bigScreenControllerService() {
        if (bigScreenControllerService == null) {
            bigScreenControllerService = new BigScreenControllerService(scampiHandler);
        }
        return bigScreenControllerService;
    }

    @NonNull
    @nonnull
    public EventService eventService() {
        if (this.eventService == null) {
            this.eventService = new EventService(scampiHandler);
        }
        return this.eventService;
    }
}
