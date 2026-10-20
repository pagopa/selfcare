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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
            headersFactory, tokenInstance, Optional.of(technicalJwt("AR")), "function-key");
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
    String technicalToken = technicalJwt("AR");
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(
            headersFactory, tokenInstance, Optional.of(technicalToken), "function-key");
    MultivaluedMap<String, Object> outgoing = new MultivaluedHashMap<>();

    // when
    filter.filter(requestContext(outgoing));

    // then
    assertEquals(List.of("Bearer " + technicalToken), outgoing.get("Authorization"));
    assertEquals(List.of("function-key"), outgoing.get("x-functions-key"));
  }

  @Test
  void failsClosedWhenNoTokenIsAvailable() {
    // given
    when(tokenInstance.isResolvable()).thenReturn(false);
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(
            headersFactory, tokenInstance, Optional.empty(), "function-key");

    // when
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> filter.filter(requestContext(new MultivaluedHashMap<>())));

    // then
    assertEquals("No bearer token available for onboarding-functions", exception.getMessage());
  }

  @Test
  void rejectsTechnicalTokenForDifferentTenant() {
    // given
    when(tokenInstance.isResolvable()).thenReturn(false);
    TenantHeaderClientRequestFilter filter =
        new TenantHeaderClientRequestFilter(
            headersFactory, tokenInstance, Optional.of(technicalJwt("PNPG")), "function-key");

    // when
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> filter.filter(requestContext(new MultivaluedHashMap<>())));

    // then
    assertEquals(
        "Invalid technical JWT for onboarding-functions: tenant_id does not match X-Tenant-Id",
        exception.getMessage());
  }

  private static ClientRequestContext requestContext(MultivaluedMap<String, Object> outgoing) {
    ClientRequestContext requestContext = mock(ClientRequestContext.class);
    when(requestContext.getHeaders()).thenReturn(outgoing);
    when(requestContext.getStringHeaders()).thenReturn(new MultivaluedHashMap<>());
    return requestContext;
  }

  private static String technicalJwt(String tenant) {
    String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    String payload = String.format(
        "{\"exp\":%d,\"iss\":\"PAGOPA\",\"uid\":\"fake-onboarding-ms\",\"tenant_id\":\"%s\"}",
        Instant.now().plusSeconds(300).getEpochSecond(), tenant);
    return encode(header) + "." + encode(payload) + "." + encode("fake-signature");
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
