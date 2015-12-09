/*
 Copyright (c) 2015 Futurice GmbH. All rights reserved. http://futurice.com/
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 - Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.scampiclient;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;

import static com.futurice.cascade.Async.SERIAL_WORKER;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 *
 */
public class VideoBroadcastService extends HereAndNowService<byte[]> {
    private static final String SERVICE_NAME = "VideoBroadcastService";
    private static final String VIDEO_DATA_FIELD_LABEL = "VideoField";
    private static final int MESSAGE_LIFETIME_MINUTES = 10;

    private final ImmutableValue<String> origin;
    private Resources resources;
    private int id;

    public VideoBroadcastService(
            @NonNull @nonnull final ScampiHandler scampiHandler,
            @NonNull @nonnull final Resources resources,
            final int id) {
        super(SERVICE_NAME, MESSAGE_LIFETIME_MINUTES, TimeUnit.MINUTES, false, scampiHandler);

        this.resources = resources;
        this.id = id;
        origin = originAsync();

//TODO Timed sendEventMessage disabled. Remove when no longer needed        scheduleVideoBot(id, 60000); //ms
        dd(this, origin, "VideoBroadcastService started");
    }

    @SuppressWarnings("missingsupercall")
    public void stop() {
        dd(this, origin, "Stopping VideoBroadcastService");
        SERIAL_WORKER
                .then(super::stop)
                .onError(e -> {
                    super.stop();
                    return ee(this, origin, "Problem stopping VideoBroadcastService", e);
                })
                .fork();
    }

    public static final String NOTIFICATION_KEY = "VideoBroadcast";

    public void messageReceived(@NonNull @nonnull SCAMPIMessage scampiMessage) {
        byte[] video = new byte[0];
        try {
            video = scampiMessage.getBinaryBuffer(VIDEO_DATA_FIELD_LABEL);
            notifyAllListeners(NOTIFICATION_KEY, video);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @nonnull
    @Override
    protected byte[] getValueFieldFromIncomingMessage(@NonNull @nonnull SCAMPIMessage scampiMessage) throws IOException {
        return scampiMessage.getBinaryBuffer(VIDEO_DATA_FIELD_LABEL);
    }

    @Override
    protected void addValueFieldToOutgoingMessage(@NonNull @nonnull SCAMPIMessage scampiMessage, @NonNull @nonnull byte[] value) {
        scampiMessage.putBinary(HereAndNowService.MESSAGE_FIELD_LABEL, value);
    }

    @Override
    protected void notifyMessageExpired(@NonNull @nonnull String key) {
        //TODO
    }

    @NonNull
    @nonnull
    @Override
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(@NonNull @nonnull byte[] val) {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(MESSAGE_LIFETIME_MINUTES, TimeUnit.MINUTES);
        final SCAMPIMessage scampiMessage = builder.build();
        scampiMessage.putBinary(VIDEO_DATA_FIELD_LABEL, val);

        return scampiHandler.sendMessageAsync(getName(), scampiMessage);
    }

    @NotCallOrigin
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(int id) {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(MESSAGE_LIFETIME_MINUTES, TimeUnit.MINUTES);
        final SCAMPIMessage scampiMessage = builder.build();

        InputStream is = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            is = resources.openRawResource(id);
            final byte[] buf = new byte[16384];
            int read;
            while ((read = is.read(buf)) >= 0) {
                bos.write(buf, 0, read);
            }
            scampiMessage.putBinary(VIDEO_DATA_FIELD_LABEL, bos.toByteArray());
        } catch (IOException e) {
            ee(this, origin, "Can not do buffer copy hack to sendEventMessage a video", e);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    ee(this, origin, "Can not close videostream input", e);
                }
            }
            try {
                bos.close();
            } catch (IOException e) {
                ee(this, origin, "Can not close videostream buffer output", e);
            } finally {
                bos = null;
            }
        }

        return scampiHandler.sendMessageAsync(getName(), scampiMessage);
    }

    private ScheduledFuture scheduleVideoBot(int id, long interval) {
        final long botMessageInterval = interval;

        vv(this, origin, "Scheduling VideoBroadcastService sendEventMessage in 60 seconds: " + id);
        return hereAndNowScheduledExecService.schedule(new Runnable() {
            @CallOrigin
            public void run() {
                if (!stopped) {
                    try {
                        vv(this, origin, "Attempting VideoBroadcastService message sendEventMessage");
                        sendMessageAsync(id)
                                .then(() -> dd(this, origin, "VideoBroadcastService video sent: " + id))
                                .onError(e -> ee(this, origin, "VideoBroadcastService video sendEventMessage FAIL: " + id, e))
                                .fork();
                    } catch (Exception e) {
                        ee(this, origin, "Can not sendEventMessage VideoBroadcast message", e);
                    } finally {
                        hereAndNowScheduledExecService.schedule(this, botMessageInterval, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }, botMessageInterval, TimeUnit.MILLISECONDS);
    }
}

