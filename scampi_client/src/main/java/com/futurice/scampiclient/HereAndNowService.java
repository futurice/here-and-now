/*
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.scampiclient;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.active.IAltFuture;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.futurice.cascade.i.nonnull;

import fi.tkk.netlab.dtn.scampi.applib.*;

import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.ii;
import static com.futurice.cascade.Async.throwIllegalStateException;
import static com.futurice.cascade.Async.vv;

@CallOrigin
public abstract class HereAndNowService<T> extends AbstractScampiService<T> {
    public static final String KEY_FIELD_LABEL = "HereAndNowKeyField";
    public static final String MESSAGE_FIELD_LABEL = "HereAndNowMessageField";
    private static final Random random = new Random();
    final static ScheduledExecutorService hereAndNowScheduledExecService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "HereAndNowServiceThread"));

    /** Namespace for the metadata that's added to the outgoing Scampi messages. */
    protected static final String METADATA_NAMESPACE = "HereAndNow";
    /** Key for the event id metadata. */
    protected static final String EVENT_METADATA_KEY = "event";

    protected final int messageLifetime;
    protected final TimeUnit messageLifetimeTimeUnit;
    protected final boolean persistentMessages;

    /**
     * Generate a uid id for a card or topic
     *
     * @return
     */
    public static long generateUid() {
        return random.nextLong();
    }

    public HereAndNowService(@NonNull @nonnull final String serviceName,
                             final int messageLifetime,
                             @NonNull @nonnull final TimeUnit messageLifetimeTimeUnit,
                             final boolean persistentMessages,
                             @NonNull final ScampiHandler scampiHandler) {
        super(serviceName, scampiHandler);

        this.messageLifetime = messageLifetime;
        this.messageLifetimeTimeUnit = messageLifetimeTimeUnit;
        this.persistentMessages = persistentMessages;
        dd(origin, serviceName + " started");
    }

    //TODO Check out various services override this, might be sloppy
    public void messageReceived(@NonNull @nonnull final SCAMPIMessage scampiMessage) throws IOException {
        assertNotStopped();
        try {
            final String key;
            if (scampiMessage.hasString(KEY_FIELD_LABEL)) {
                key = scampiMessage.getString(KEY_FIELD_LABEL);
            } else {
                key = "key.not.found";
                ii(origin, "Received SCAMPI message with no key field: " + scampiMessage);
            }
            final long lifetime = scampiMessage.getLifetime();
            final T value = getValueFieldFromIncomingMessage(scampiMessage);

            notifyAllListeners(key, value);

            scheduleMessageExpiration(key, lifetime);
            //Units for lifetime object are: seconds from message creation
            // Expiration time = creation time (seconds from 1970) + time to live
        } catch (Exception e) {
            ee(origin, "Bad things happened after messageReceived, like fields not lining up", e);
        }
    }

    /**
     * Called from the timer thread
     *
     * @param key
     */
    protected abstract void notifyMessageExpired(@NonNull @nonnull String key);

    @NonNull
    @nonnull
    protected abstract T getValueFieldFromIncomingMessage(@NonNull @nonnull SCAMPIMessage scampiMessage) throws Exception;

    protected void assertNotStopped() {
        if (stopped) {
            throwIllegalStateException(origin, "HereAndNowService is stopped, can not sendEventMessage message");
        }
    }

    @NonNull
    @nonnull
    @CallSuper
    public IAltFuture<?, SCAMPIMessage> sendMessage(@NonNull @nonnull final String key, @NonNull @nonnull final T value) {
        assertNotStopped();

        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(messageLifetime, TimeUnit.MINUTES);
        builder.persistent(persistentMessages);

        final SCAMPIMessage scampiMessage = builder.build();
        scampiMessage.putString(KEY_FIELD_LABEL, key);

        addValueFieldToOutgoingMessage(scampiMessage, value);

        return scampiHandler.sendMessageAsync(getName(), scampiMessage);
    }

    @CallSuper
    public void stop() {
        super.stop();
        hereAndNowScheduledExecService.shutdown();
    }

    protected abstract void addValueFieldToOutgoingMessage(
            @NonNull @nonnull SCAMPIMessage scampiMessage,
            @NonNull @nonnull T value);

    private void scheduleMessageExpiration(@NonNull @nonnull final String key, final long secondsSince1970) {
        assertNotStopped();
        final long secondsFromNow = intervalSecondsToSecondsFromNow(secondsSince1970);
        dd(origin, "Scheduling message expiration in " + secondsFromNow + "s, key=" + key);
        hereAndNowScheduledExecService.schedule(() -> {
            try {
                vv(origin, "Attempting local message timeout");
                notifyMessageExpired(key);
            } catch (Exception e) {
                ee(origin, "Can not expire message, key=" + key, e);
            }
        }, secondsFromNow, TimeUnit.SECONDS);
    }

    private long intervalSecondsToSecondsFromNow(final long t) {
        final long t2 = (System.currentTimeMillis() / 1000) - t;

        vv(origin, "Message will expire in " + t2 + " seconds");
//        assertTrue("Time to message expiration is negative", t2 > 0);

        return t2;
    }
}
