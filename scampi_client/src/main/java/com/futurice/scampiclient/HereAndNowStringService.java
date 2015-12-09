package com.futurice.scampiclient;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.i.nonnull;

import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;

/**
 *
 */
public class HereAndNowStringService extends HereAndNowService<String> {
    protected String stringFieldLabel = "HereAndNowStringFieldLabel";

    public HereAndNowStringService(
            @NonNull @nonnull final String serviceName,
            final int messageLifetime,
            @NonNull @nonnull final TimeUnit messageLifetimeTimeUnit,
            final boolean persistentMessages,
            @NonNull @nonnull final ScampiHandler scampiHandler) {
        super(serviceName, messageLifetime, messageLifetimeTimeUnit, persistentMessages, scampiHandler);
    }

    @NonNull
    @nonnull
    @Override
    protected String getValueFieldFromIncomingMessage(@NonNull @nonnull final SCAMPIMessage scampiMessage) {
        return scampiMessage.getString(stringFieldLabel);
    }

    @NonNull
    @nonnull
    @Override
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(@NonNull @nonnull final String val) {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(messageLifetime, messageLifetimeTimeUnit);
        final SCAMPIMessage scampiMessage = builder.build();
        scampiMessage.putString(stringFieldLabel, val);

        return scampiHandler.sendMessageAsync(getName(), scampiMessage);
    }

    @Override
    protected void addValueFieldToOutgoingMessage(
            @NonNull @nonnull final SCAMPIMessage scampiMessage,
            @NonNull @nonnull final String value) {
        scampiMessage.putString(stringFieldLabel, value);
    }

    @Override
    protected void notifyMessageExpired(@NonNull @nonnull String key) {
        //TODO
    }
}
