package it.pagopa.selfcare.onboarding.client.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.handler.codec.PrematureChannelClosureException;
import io.vertx.core.http.HttpClosedException;
import jakarta.ws.rs.ProcessingException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class ConnectionDropsTest {

    @Test
    void aConnectionClosedBeforeTheAnswer_isADrop() {
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(new IOException("Connection was closed")));
    }

    @Test
    void theDropIsFoundInsideTheWrappers() {
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(
                new ProcessingException(new IOException("Connection was closed"))));
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(
                new CompletionException(new IOException("Connection was closed"))));
    }

    @Test
    void aResetOrBrokenPipe_isADrop() {
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(
                new ProcessingException(new SocketException("Connection reset"))));
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(new SocketException("Connection reset by peer")));
        assertTrue(ConnectionDrops.isDroppedBeforeAnswer(new SocketException("Broken pipe")));
    }

    @Test
    void aClosureWhileTheAnswerIsRead_isNotADrop() {
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new HttpClosedException("Connection was closed")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(
                new CompletionException(new HttpClosedException("Connection was closed"))));
    }

    @Test
    void aClosureInsideTheStatusLineOrHeaders_isNotADrop() {
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new ProcessingException(
                new PrematureChannelClosureException("Connection closed before received headers"))));
    }

    @Test
    void connectionFailuresAndTimeouts_areNotDrops() {
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new ConnectException("Connection refused")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new NoRouteToHostException("No route to host")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new UnknownHostException("ms-onboarding")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new SocketTimeoutException("Read timed out")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new TimeoutException("The timeout period of 60000ms has been exceeded")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new ConnectException("Connection reset")));
    }

    @Test
    void otherFailures_areNotDrops() {
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new IOException("Premature end of stream")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new IOException()));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new IllegalArgumentException("invalid version format: GARBAGE")));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new SocketException()));
        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(new RuntimeException()));
    }

    @Test
    void aCauseCycle_doesNotLoop() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);
        first.initCause(second);

        assertFalse(ConnectionDrops.isDroppedBeforeAnswer(first));
    }
}
