package it.pagopa.selfcare.onboarding.client.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClosedException;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ReplayOnConnectionDropInterceptorTest {

    private final ReplayOnConnectionDropInterceptor interceptor = new ReplayOnConnectionDropInterceptor();

    interface Client {
        @GET
        Uni<String> read();

        @GET
        String synchronousRead();

        @POST
        Uni<String> write(String body);
    }

    @Test
    void givenColdRead_whenConnectionDrops_thenResubscribesWithoutReinvokingClient() throws Exception {
        InvocationContext context = invocation("read");
        AtomicInteger subscriptions = new AtomicInteger();
        Uni<String> source = Uni.createFrom().deferred(() -> subscriptions.incrementAndGet() == 1
                ? Uni.createFrom().failure(drop())
                : Uni.createFrom().item("document"));
        when(context.proceed()).thenReturn(source);

        Uni<?> result = assertInstanceOf(Uni.class, interceptor.replay(context));
        assertEquals(0, subscriptions.get());
        Object response = result.await().atMost(Duration.ofSeconds(2));

        assertEquals("document", response);
        assertEquals(2, subscriptions.get());
        verify(context).proceed();
    }

    @Test
    void givenColdRead_whenReplayAlsoDrops_thenFailsAfterTwoSubscriptions() throws Exception {
        InvocationContext context = invocation("read");
        AtomicInteger subscriptions = new AtomicInteger();
        ProcessingException failure = drop();
        Uni<String> source = Uni.createFrom().deferred(() -> {
            subscriptions.incrementAndGet();
            return Uni.createFrom().failure(failure);
        });
        when(context.proceed()).thenReturn(source);

        Uni<?> result = assertInstanceOf(Uni.class, interceptor.replay(context));
        ProcessingException actual = assertThrows(ProcessingException.class,
                () -> result.await().atMost(Duration.ofSeconds(2)));

        assertSame(failure, actual);
        assertEquals(2, subscriptions.get());
        verify(context).proceed();
    }

    @Test
    void givenRequestWithBody_whenConnectionDrops_thenDoesNotReplay() throws Exception {
        InvocationContext context = invocation("write", "document");
        AtomicInteger subscriptions = new AtomicInteger();
        ProcessingException failure = drop();
        Uni<String> source = Uni.createFrom().deferred(() -> {
            subscriptions.incrementAndGet();
            return Uni.createFrom().failure(failure);
        });
        when(context.proceed()).thenReturn(source);

        Uni<?> result = assertInstanceOf(Uni.class, interceptor.replay(context));
        ProcessingException actual = assertThrows(ProcessingException.class,
                () -> result.await().atMost(Duration.ofSeconds(2)));

        assertSame(failure, actual);
        assertEquals(1, subscriptions.get());
        verify(context).proceed();
    }

    @ParameterizedTest
    @MethodSource("nonReplayableFailures")
    void givenColdRead_whenFailureIsNotBeforeAnswerDrop_thenDoesNotReplay(RuntimeException failure)
            throws Exception {
        InvocationContext context = invocation("read");
        AtomicInteger subscriptions = new AtomicInteger();
        Uni<String> source = Uni.createFrom().deferred(() -> {
            subscriptions.incrementAndGet();
            return Uni.createFrom().failure(failure);
        });
        when(context.proceed()).thenReturn(source);

        Uni<?> result = assertInstanceOf(Uni.class, interceptor.replay(context));
        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> result.await().atMost(Duration.ofSeconds(2)));

        assertSame(failure, actual);
        assertEquals(1, subscriptions.get());
        verify(context).proceed();
    }

    @Test
    void givenSynchronousRead_whenSuccessful_thenReturnsOriginalResult() throws Exception {
        InvocationContext context = invocation("synchronousRead");
        String response = "document";
        when(context.proceed()).thenReturn(response);

        Object actual = interceptor.replay(context);

        assertSame(response, actual);
        verify(context).proceed();
    }

    @Test
    void givenSynchronousRead_whenConnectionDrops_thenReinvokesOnce() throws Exception {
        InvocationContext context = invocation("synchronousRead");
        when(context.proceed()).thenThrow(drop()).thenReturn("document");

        Object actual = interceptor.replay(context);

        assertEquals("document", actual);
        verify(context, times(2)).proceed();
    }

    @Test
    void givenSynchronousRead_whenReplayAlsoDrops_thenPropagatesSecondFailure() throws Exception {
        InvocationContext context = invocation("synchronousRead");
        ProcessingException secondFailure = drop();
        when(context.proceed()).thenThrow(drop()).thenThrow(secondFailure);

        ProcessingException actual = assertThrows(ProcessingException.class, () -> interceptor.replay(context));

        assertSame(secondFailure, actual);
        verify(context, times(2)).proceed();
    }

    @Test
    void givenSynchronousRead_whenReadTimesOut_thenDoesNotReplay() throws Exception {
        InvocationContext context = invocation("synchronousRead");
        ProcessingException failure = new ProcessingException(new SocketTimeoutException("Read timed out"));
        when(context.proceed()).thenThrow(failure);

        ProcessingException actual = assertThrows(ProcessingException.class, () -> interceptor.replay(context));

        assertSame(failure, actual);
        verify(context).proceed();
    }

    private static Stream<RuntimeException> nonReplayableFailures() {
        return Stream.of(
                new ProcessingException(new SocketTimeoutException("Read timed out")),
                new ProcessingException(new HttpClosedException("Connection was closed")),
                new WebApplicationException("Downstream failed", 500));
    }

    private static ProcessingException drop() {
        return new ProcessingException(new IOException("Connection was closed"));
    }

    private static InvocationContext invocation(String methodName, Object... arguments) {
        Method method = Arrays.stream(Client.class.getMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        InvocationContext context = mock(InvocationContext.class);
        when(context.getMethod()).thenReturn(method);
        when(context.getParameters()).thenReturn(arguments);
        return context;
    }
}
