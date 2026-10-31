package it.pagopa.selfcare.m2m;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import jakarta.ws.rs.core.MultivaluedHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineTokenRequestFilterTest {
  private MachineTokenRequestFilter filter;
  private ResteasyReactiveClientRequestContext context;
  private CompletableFuture<String> token;

  @BeforeEach
  void setUp() {
    filter = new MachineTokenRequestFilter();
    filter.tokens = mock(MachineTokenClient.class);
    when(filter.tokens.isEnabled()).thenReturn(true);
    filter.audience = Optional.of("onboarding-functions");
    filter.scope = Optional.of("onboarding-functions.invoke");
    token = new CompletableFuture<>();
    when(filter.tokens.token("AR", "onboarding-functions", "onboarding-functions.invoke")).thenReturn(token);
    context = mock(ResteasyReactiveClientRequestContext.class);
    when(context.getHeaderString("X-Tenant-Id")).thenReturn("AR");
    when(context.getHeaders()).thenReturn(new MultivaluedHashMap<>());
  }

  @Test
  void suspendsAndResumesWithoutBlockingWhileTokenIsAcquired() {
    filter.filter(context);
    verify(context).suspend();
    verify(context, never()).resume();
    token.complete("machine-jwt");
    assertEquals("Bearer machine-jwt", context.getHeaders().getFirst("Authorization"));
    verify(context).resume();
  }

  @Test
  void resumesWithFailureInsteadOfSendingUnauthenticatedRequest() {
    filter.filter(context);
    IllegalStateException failure = new IllegalStateException("Issuer unavailable");
    token.completeExceptionally(failure);
    verify(context).resume(failure);
    verify(context, never()).resume();
  }

  @Test
  void leavesPropagatedUserSessionUntouched() {
    when(context.getHeaderString("Authorization")).thenReturn("Bearer user-jwt");
    filter.filter(context);
    verify(context, never()).suspend();
    verify(filter.tokens, never()).token(anyString(), anyString(), anyString());
  }
}
