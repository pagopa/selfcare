package it.pagopa.selfcare.onboarding.client.transport;

import io.netty.handler.codec.PrematureChannelClosureException;
import io.vertx.core.http.HttpClosedException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;

/**
 * Recognises a connection lost after the request was sent and before the downstream began to
 * answer: the only transport failure {@code HttpURLConnection} (the Spring transport) repeats.
 *
 * <p>The REST client reports that moment as a plain {@link IOException} "Connection was closed" (or
 * a {@link SocketException} "Connection reset"). The same closure while the answer is being read
 * is an {@link HttpClosedException}, and a closure inside the status line or headers a {@link
 * PrematureChannelClosureException}: neither is repeated, as in Spring.
 */
final class ConnectionDrops {

    private static final int MAX_DEPTH = 10;

    private ConnectionDrops() {
    }

    static boolean isDroppedBeforeAnswer(Throwable failure) {
        boolean dropped = false;
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < MAX_DEPTH; depth++, cause = cause.getCause()) {
            if (cause instanceof HttpClosedException || cause instanceof PrematureChannelClosureException) {
                return false;
            }
            dropped |= isConnectionLost(cause);
        }
        return dropped;
    }

    private static boolean isConnectionLost(Throwable failure) {
        String message = failure.getMessage();
        if (message == null) {
            return false;
        }
        if (failure.getClass() == IOException.class) {
            return message.equals("Connection was closed");
        }
        return failure instanceof SocketException
                && !(failure instanceof ConnectException)
                && !(failure instanceof NoRouteToHostException)
                && (message.startsWith("Connection reset") || message.startsWith("Broken pipe"));
    }
}
