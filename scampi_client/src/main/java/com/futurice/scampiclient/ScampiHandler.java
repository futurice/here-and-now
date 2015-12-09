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

import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.applib.AppLib;
import fi.tkk.netlab.dtn.scampi.applib.AppLibLifecycleListener;
import fi.tkk.netlab.dtn.scampi.applib.LocationUpdateCallback;
import fi.tkk.netlab.dtn.scampi.applib.MessageReceivedCallback;
import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;
import fi.tkk.netlab.dtn.scampi.applib.impl.parser.Protocol;

import static com.futurice.cascade.Async.SERIAL_WORKER;
import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.ii;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * Connect to the local SCAMPI service over TCP for sending split receiving messages
 */
@NotCallOrigin
public class ScampiHandler {
    private static final long RECONNECT_INTERVAL = 5000;
    public final AppLib appLib = AppLib.builder().build();
    final protected ImmutableValue<String> origin = originAsync();
    //    final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> new Thread(r, "ScampiHandler"));
//    final IThreadType scampiIThreadType = new DefaultThreadType("ScampiAsync", executorService, new LinkedBlockingQueue<>());
    final ScheduledExecutorService reconnectService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "ScampiReconnect"));
    final LinkedList<IScampiService> scampiServices = new LinkedList<>();
    final ConcurrentLinkedQueue<SCAMPIMessage> unpublishedSCAMPIMessages = new ConcurrentLinkedQueue<>();
    private volatile ScampiConnectionState state = ScampiConnectionState.DISCONNECTED;
    private Runnable reconnectRunnable;
    private volatile Future<?> reconnectFuture;
    private volatile boolean paused;

    public ScampiHandler() {
        appLib.start();

        appLib.addLifecycleListener(new AppLibLifecycleListener() {
            @Override
            public void onConnected(String s) {
                dd(origin, "Scampi CONNECTED: " + s);
                setScampiConnectionState(ScampiConnectionState.CONNECTED);
            }

            @Override
            public void onDisconnected() {
                dd(origin, "Scampi disconnected");
                setScampiConnectionState(ScampiConnectionState.DISCONNECTED);
            }

            @Override
            public void onConnectFailed() {
                dd(origin, "Scampi connect failed");
                setScampiConnectionState(ScampiConnectionState.CONNECT_FAILED);
            }

            @Override
            public void onStopped() {
                dd(origin, "Scampi stopped");
                setScampiConnectionState(ScampiConnectionState.STOPPED);
            }
        });

        appLib.addLocationUpdateCallback(new LocationUpdateCallback() {
            @Override
            @SuppressWarnings("deprecation")
            public void locationUpdated(AppLib appLib, double v, double v2, double v3, double v4, long l) {
                dd(origin, "location updated: " + v + ", " + v2 + ", " + v3 + ", " + v4 + ", " + l);
            }

            @Override
            public void gpsLocationUpdated(Protocol.GpsLocation gpsLocation) {
                dd(origin, "gps location updated: lat:" + gpsLocation.latitude + ", lon:" + gpsLocation.longitude + ", el:" + gpsLocation.elevation + ", error:" + gpsLocation.error + ", timestamp:" + gpsLocation.timestamp);
            }
        });

        appLib.addMessageReceivedCallback(new MessageReceivedCallback() {
            @Override
            public void messageReceived(SCAMPIMessage scampiMessage, String service) {
                vv(origin, "message received: " + scampiMessage + ", " + service);
                try {
                    scampiMessageReceived(scampiMessage, service);
                } catch (IOException e) {
                    ee(origin, "Problem in synchonous message dispatch", e);
                }
//TODO Debug, was working but..
/*                scampiIThreadType.subscribeTarget(() -> scampiMessageReceived(scampiMessage, service))
                        .subscribeTarget((SCAMPIMessage message) -> Async.UI.v(TAG, "Message received split handled by " + service + ": " + message))
                        .onError((message, e) -> Aspect.UI.e(TAG, "Message receive handler error to service=" + service + ": " + message, e));
*/
            }
        });
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        UI.then(() -> {
            this.paused = paused;
            if (paused) {
                cancelReconnect();
            }
        }).fork();
    }

    //TODO only shedule reconnect if not in PAUSED or STOPPED state in parent Activity
    public synchronized void scheduleReconnect() {
        if (reconnectRunnable == null) {
            vv(origin, "Scheduling reconnect every " + RECONNECT_INTERVAL + "ms");
            cancelReconnect();
            reconnectRunnable = () -> {
                try {
                    if (isPaused()) {
                        cancelReconnect();
                    }
                    vv(origin, "Attempting reconnect");
                    connect();
                } catch (Exception e) {
                    ee(origin, "Can not reconnect", e);
                }
            };
            reconnectFuture = reconnectService.scheduleAtFixedRate(reconnectRunnable, 0, RECONNECT_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void cancelReconnect() {
        if (reconnectFuture != null) {
            vv(origin, "Reconnect cancalled");
            reconnectFuture.cancel(false);
            reconnectFuture = null;
        }
    }

    /**
     * Call this only before connect()
     *
     * @param scampiService
     * @throws InterruptedException
     */
    public void addService(IScampiService scampiService) throws InterruptedException {
        scampiServices.add(scampiService);
        appLib.subscribe(scampiService.getName());
    }

    /**
     * Call after registering services. Service callbacks split messages will start to arrive at some point after this
     *
     * @return
     */
    public void connect() {
        if (state != ScampiConnectionState.CONNECTED) {
            dd(origin, "Attempt connect");
            appLib.connect();
        }
    }

    @NonNull
    @nonnull
    public ScampiConnectionState getState() {
        return state;
    }

    /**
     * Call this _after_ you have registered to receive connection state change callbacks to get the
     * initial state.
     *
     * @return
     */
    public boolean isConnected() {
        return state == ScampiConnectionState.CONNECTED;
    }

    private void setScampiConnectionState(@NonNull @nonnull final ScampiConnectionState newState) {
        this.state = newState;
    }

    /**
     * Hold the SCAMPI thread split use it to copy values until we have received the entire message
     * contents. This throttles the rate at which messages from the cache are delivered at startup
     * to prevent memory/queue overruns.
     *
     * @param scampiMessage
     * @param serviceName
     */
    @NonNull
    @nonnull
    private SCAMPIMessage scampiMessageReceived(
            @NonNull @nonnull final SCAMPIMessage scampiMessage,
            @NonNull @nonnull final String serviceName) throws IOException {
        try {
            if (scampiMessageIsExpired(scampiMessage)) {
                ii(origin, "Received EXPIRED scampi message- ignoring: " + scampiMessage);
                return scampiMessage;
            }
            for (final IScampiService scampiService : scampiServices) {
                if (scampiService.getName().equals(serviceName)) {
                    dd(origin, "Routing message to the right scampi service: " + serviceName);
                    scampiService.messageReceived(scampiMessage);
                    return scampiMessage;
                }
            }
            dd(origin, "Received split ignored message for unknown scampi service: " + serviceName);

            return scampiMessage;
        } finally {
            scampiMessage.close(); // We have a copy- the service can now deallocate the message in the scampi cache as needed
        }
    }

    private boolean scampiMessageIsExpired(@NonNull final SCAMPIMessage scampiMessage) {
        return scampiMessage.getLifetime() <= 0;
    }

    /**
     * IScampiService implementations call this method to dispatch messages
     *
     * @param service
     * @param scampiMessage
     * @return
     */
    @NonNull
    @nonnull
    IAltFuture<?, SCAMPIMessage> sendMessageAsync(
            @NonNull @nonnull final String service,
            @NonNull @nonnull final SCAMPIMessage scampiMessage) {
        unpublishedSCAMPIMessages.add(scampiMessage);

        return SERIAL_WORKER
                .then(() -> {
                    appLib.publish(
                            scampiMessage,
                            service,
                            (appLib1, publishedScampiMessage) -> unpublishedSCAMPIMessages.remove(publishedScampiMessage));
                    dd(origin, "Message sent to " + service);
                    return scampiMessage;
                });
    }

    /**
     * Close split exit after any pending subscribeTarget operations
     * <p>
     * Note that this can not be called from the workerExecutorService thread. It is normally called
     * during app lifecycle events.
     *
     * @param timeoutMillis
     * @return A list of all SCAMPIMessage objects which have been published but not yet acknowledged by the persistent router cache. These may need to be stored by the app split re-published at next application start
     */
    public List<SCAMPIMessage> stop(final int timeoutMillis) {
        for (IScampiService service : scampiServices) {
            /*
             * It is allowed but may or may not perform well for IScampiService.stop() to trigger other
              * asynchronous activities such as message sending during service stop. These activities
              * will try to complete, but if they are too slow during an application end-of-life event
              * subscribeTarget there is a risk the phone will interrupt() these actions
              *
              */
            service.stop();
        }

        try {
            IAltFuture<?, List<SCAMPIMessage>> servicesStoppedAltFuture = SERIAL_WORKER
                    .then(() -> ii(origin, "Shutdown-associated tasks done"))
                    .onError(e -> {
                        ee(origin, "Some services were not stopped", e);
                        return true;
                    })
                    .then(() -> {
                        reconnectService.shutdownNow();
                        dd(origin, "Sending applib stop");
                        appLib.stop();
                        List<SCAMPIMessage> unpublishedMessage = new ArrayList<>(unpublishedSCAMPIMessages.size());
                        unpublishedMessage.addAll(unpublishedSCAMPIMessages);
                        synchronized (ScampiHandler.this) {
                            ScampiHandler.this.notifyAll();
                        }

                        return unpublishedMessage;
                    })
                    .fork();

            synchronized (this) {
                if (!servicesStoppedAltFuture.isDone()) {
                    dd(origin, "Shutdown-associated tasks not done, waiting " + timeoutMillis);
                    this.wait(timeoutMillis);
                }
                if (servicesStoppedAltFuture.isDone()) {
                    dd(origin, "Shutdown-associated tasks are done, continuing");
                    return servicesStoppedAltFuture.get();
                }
            }
        } catch (InterruptedException e) {
            ee(origin, "Interrupted waiting for IScampiService(s) to stop", e);
        }

        return new ArrayList<>(); // Return an empty list if there was a shutdown problem
    }

    public enum ScampiConnectionState {
        DISCONNECTED("Disconnected"),
        CONNECTED("Connected"),
        CONNECT_FAILED("Connect Failed"),
        STOPPED("Stopped");

        private final String name;

        private ScampiConnectionState(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return (otherName == null) ? false : name.equals(otherName);
        }

        public String toString() {
            return name;
        }
    }
}
