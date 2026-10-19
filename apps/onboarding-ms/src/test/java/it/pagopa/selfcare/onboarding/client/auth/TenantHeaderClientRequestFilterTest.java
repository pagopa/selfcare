package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TenantHeaderClientRequestFilterTest {

  private AuthenticationPropagationHeadersFactory headersFactory;
  private Instance<JsonWebToken> tokenInstance;
  private JsonWebToken token;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    TenantContext tenantContext = new TenantContext();
    tenantContext.setTenantId("AR");
    TenantRegistry tenantRegistry = mock(TenantRegistry.class);
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");
    headersFactory = new AuthenticationPropagationHeadersFactory(tenantContext, tenantRegistry);
    tokenInstance = mock(Instance.class);
    token = mock(JsonWebToken.class);
  }

  @Test
  void propagatesSessionTokenAndValidatedTenant() {
    // given
    when(tokenInstance.isResolvable()).thenReturn(true);
    when(tokenInstance.get()).thenReturn(token);
    when(token.getRawToken()).thenReturn("session-jwt");
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(
            headersFactory, tokenInstance, "technical-jwt", "function-key");
    MultivaluedMap<String, Object> outgoing = new MultivaluedHashMap<>();

    // when
    filter.filter(requestContext(outgoing));

    // then
    assertEquals(List.of("AR"), outgoing.get("X-Tenant-Id"));
    assertEquals(List.of("Bearer session-jwt"), outgoing.get("Authorization"));
    assertEquals(List.of("function-key"), outgoing.get("x-functions-key"));
  }

  @Test
  void usesTechnicalTokenWithoutHttpSecurityContext() {
    // given
    when(tokenInstance.isResolvable()).thenReturn(false);
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(
            headersFactory, tokenInstance, "technical-jwt", "function-key");
    MultivaluedMap<String, Object> outgoing = new MultivaluedHashMap<>();

    // when
    filter.filter(requestContext(outgoing));

    // then
    assertEquals(List.of("Bearer technical-jwt"), outgoing.get("Authorization"));
    assertEquals(List.of("function-key"), outgoing.get("x-functions-key"));
  }

  @Test
  void failsClosedWhenNoTokenIsAvailable() {
    // given
    when(tokenInstance.isResolvable()).thenReturn(false);
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(headersFactory, tokenInstance, "", "function-key");

    // when / then
    assertThrows(
        IllegalStateException.class,
        () -> filter.filter(requestContext(new MultivaluedHashMap<>())));
  }

  private static ClientRequestContext requestContext(MultivaluedMap<String, Object> outgoing) {
    ClientRequestContext requestContext = mock(ClientRequestContext.class);
    when(requestContext.getHeaders()).thenReturn(outgoing);
    when(requestContext.getStringHeaders()).thenReturn(new MultivaluedHashMap<>());
    return requestContext;
  }
}
