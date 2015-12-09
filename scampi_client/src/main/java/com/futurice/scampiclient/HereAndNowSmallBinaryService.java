package com.futurice.scampiclient;

import android.support.annotation.NonNull;

import com.futurice.cascade.active.IAltFuture;
import com.futurice.cascade.i.nonnull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;

/**
 *
 */
public class HereAndNowSmallBinaryService extends HereAndNowService<byte[]> {
    protected String smallBinaryFieldLabel = "HereAndNowSmallBinaryField";

    public HereAndNowSmallBinaryService(
            @NonNull @nonnull final String serviceName,
            final int messageLifetime,
            @NonNull @nonnull final TimeUnit messageLifetimeTimeUnit,
            final boolean persistentMessages,
            @NonNull @nonnull final ScampiHandler scampiHandler) {
        super(serviceName, messageLifetime, messageLifetimeTimeUnit, persistentMessages, scampiHandler);
    }

    @NonNull @nonnull
    @Override
    public IAltFuture<?, SCAMPIMessage> sendMessageAsync(@NonNull @nonnull final byte[] val) {
        final SCAMPIMessage.Builder builder = SCAMPIMessage.builder();
        builder.lifetime(messageLifetime, messageLifetimeTimeUnit);
        final SCAMPIMessage scampiMessage = builder.build();
        scampiMessage.putBinary(smallBinaryFieldLabel, val);

        return scampiHandler.sendMessageAsync(getName(), scampiMessage);
    }

    @NonNull @nonnull
    @Override
    protected byte[] getValueFieldFromIncomingMessage(@NonNull @nonnull final SCAMPIMessage scampiMessage) throws IOException {
        return scampiMessage.getBinaryBuffer(smallBinaryFieldLabel);
    }

    @Override
    protected void addValueFieldToOutgoingMessage(@NonNull @nonnull final SCAMPIMessage scampiMessage, @NonNull @nonnull byte[] value) {
        scampiMessage.putBinary(MESSAGE_FIELD_LABEL, value);
    }

    @Override
    protected void notifyMessageExpired(@NonNull @nonnull String key) {
        //TODO
    }
}
