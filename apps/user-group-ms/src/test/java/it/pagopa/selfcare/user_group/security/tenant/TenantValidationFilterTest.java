package it.pagopa.selfcare.user_group.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for the Spring port of {@code TenantValidationFilter} (Step_0 sub-task 5), mirroring
 * the Quarkus {@code TenantValidationFilterTest} scenarios in {@code libs/selfcare-sdk-security}.
 */
class TenantValidationFilterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final TenantValidationFilter filter = new TenantValidationFilter(objectMapper);

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;
  private StringWriter responseBody;

  @BeforeEach
  void setUp() throws Exception {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    responseBody = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noAuthenticatedPrincipal_shouldSkipEnforcement() throws Exception {
    // no Authentication set on the SecurityContext
    filter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void matchingHeaderAndClaim_shouldContinueChainAndExposeTenant() throws Exception {
    authenticateAs("PAGOPA", token("PAGOPA", "AR"));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("AR"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(request).setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, TenantId.AR);
    verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void missingHeader_shouldReject() throws Exception {
    authenticateAs("PAGOPA", token("PAGOPA", "AR"));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>()));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    assertResponseDetailContains("Missing, duplicated, or unknown X-Tenant-Id header");
  }

  @Test
  void headerMismatchingClaim_shouldReject() throws Exception {
    authenticateAs("PAGOPA", token("PAGOPA", "AR"));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("PNPG"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    assertResponseDetailContains("X-Tenant-Id header does not match the JWT tenant claim");
  }

  @Test
  void hubSpidLoginTokenMissingClaim_defaultsToPnpg() throws Exception {
    authenticateAs("SPID", token("SPID", null));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("PNPG"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    verify(request).setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, TenantId.PNPG);
  }

  @Test
  void hubSpidLoginTokenMissingClaim_headerNotPnpg_shouldReject() throws Exception {
    authenticateAs("SPID", token("SPID", null));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("AR"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void multipleTenantHeaders_shouldReject() throws Exception {
    authenticateAs("PAGOPA", token("PAGOPA", "AR"));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("AR", "PNPG"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  void unknownTenantHeaderValue_shouldReject() throws Exception {
    authenticateAs("PAGOPA", token("PAGOPA", "AR"));
    when(request.getHeaders(TenantConstants.TENANT_HEADER))
        .thenReturn(Collections.enumeration(new Vector<>(java.util.List.of("UNKNOWN"))));

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }

  private void authenticateAs(String issuer, String credentials) {
    SelfCareUser principal = mock(SelfCareUser.class);
    when(principal.getIssuer()).thenReturn(issuer);
    Authentication authentication = new TestingAuthenticationToken(principal, credentials);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  /** Builds a fake (unsigned) compact JWT string carrying only the claims under test. */
  private String token(String issuer, String tenantId) throws Exception {
    Map<String, Object> claims =
        tenantId == null
            ? Map.of("iss", issuer)
            : Map.of("iss", issuer, TenantConstants.TENANT_CLAIM, tenantId);
    String payload =
        Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(claims));
    return "header." + payload + ".signature";
  }

  private void assertResponseDetailContains(String expectedDetail) {
    String body = responseBody.toString();
    assertEquals(true, body.contains(expectedDetail), "expected body to contain: " + expectedDetail + " but was: " + body);
  }
}
