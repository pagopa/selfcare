package it.pagopa.selfcare.iam.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
class TokenPropagationFilterTest {

  @Mock JsonWebToken jwt;
  @Mock TenantContext tenantContext;
  @Mock ClientRequestContext requestContext;

  private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
  private TokenPropagationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new TokenPropagationFilter();
    filter.jwt = jwt;
    filter.tenantContext = tenantContext;
    lenient().when(requestContext.getHeaders()).thenAnswer(invocation -> headers);
  }

  @Test
  void propagatesAuthorizationAndTenantHeaders() throws Exception {
    when(jwt.getRawToken()).thenReturn("raw-token");
    when(tenantContext.isInitialized()).thenReturn(true);
    when(tenantContext.requiredTenantId()).thenReturn("PNPG");

    filter.filter(requestContext);

    assertEquals("Bearer raw-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    assertEquals("PNPG", headers.getFirst(TenantResolutionFilter.TENANT_HEADER));
  }

  @Test
  void skipsHeadersWhenTokenAndTenantAreMissing() throws Exception {
    when(jwt.getRawToken()).thenReturn(null);
    when(tenantContext.isInitialized()).thenReturn(false);

    filter.filter(requestContext);

    assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION));
    assertFalse(headers.containsKey(TenantResolutionFilter.TENANT_HEADER));
  }

  @Test
  void skipsHeadersWhenTokenOrTenantLookupFails() throws Exception {
    when(jwt.getRawToken()).thenThrow(new IllegalStateException("no request scope"));
    when(tenantContext.isInitialized()).thenThrow(new IllegalStateException("no request scope"));

    filter.filter(requestContext);

    assertFalse(headers.containsKey(HttpHeaders.AUTHORIZATION));
    assertFalse(headers.containsKey(TenantResolutionFilter.TENANT_HEADER));
  }
}
