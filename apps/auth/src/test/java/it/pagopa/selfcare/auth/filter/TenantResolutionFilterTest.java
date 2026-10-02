package it.pagopa.selfcare.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.conf.TenantDefinition;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.context.AuthTenantContext;
import it.pagopa.selfcare.auth.controller.response.Problem;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TenantResolutionFilterTest {

  private TenantResolutionFilter filter;
  private TenantRegistry tenantRegistry;
  private AuthTenantContext authTenantContext;
  private TenantContext resourceTenantContext;

  @BeforeEach
  void setUp() {
    filter = new TenantResolutionFilter();
    tenantRegistry = mock(TenantRegistry.class);
    authTenantContext = new AuthTenantContext();
    resourceTenantContext = new TenantContext();
    filter.tenantRegistry = tenantRegistry;
    filter.tenantContext = authTenantContext;
    filter.resourceTenantContext = resourceTenantContext;
  }

  private String sanitize(String value) throws Exception {
    Method method = TenantResolutionFilter.class.getDeclaredMethod("sanitizeForLog", String.class);
    method.setAccessible(true);
    return (String) method.invoke(new TenantResolutionFilter(), value);
  }

  @Test
  void sanitizeRemovesCrlfPreventingAuditLogForgery() throws Exception {
    String forged = "otp/verify\nevent=tenant_request_rejected reason=none";

    String sanitized = sanitize(forged);

    assertFalse(sanitized.contains("\n"));
    assertFalse(sanitized.contains("\r"));
    assertFalse(sanitized.contains(" "));
    assertTrue(sanitized.startsWith("otp/verify"));
  }

  @Test
  void sanitizeKeepsLegitimatePathCharacters() throws Exception {
    assertEquals("otp/verify", sanitize("otp/verify"));
    assertEquals("oidc/exchange-v2.1", sanitize("oidc/exchange-v2.1"));
  }

  @Test
  void sanitizeTruncatesOverlongPaths() throws Exception {
    String sanitized = sanitize("a".repeat(500));

    assertEquals(200, sanitized.length());
  }

  @Test
  void sanitizeHandlesNull() throws Exception {
    assertEquals("", sanitize(null));
  }

  @Test
  void validTenantOnAuthRoutePopulatesBothContexts() {
    TenantRegistry.Tenant ar =
        new TenantRegistry.Tenant(
            "AR",
            new TenantDefinition(
                "https://selfcare.pagopa.it",
                "https://api.selfcare.pagopa.it",
                List.of("https://selfcare.pagopa.it"),
                TenantRegistry.ONE_IDENTITY,
                true));
    when(tenantRegistry.resolveEnabledTenant("AR")).thenReturn(ar);
    ContainerRequestContext request = request("/oidc/exchange", "AR");

    filter.filter(request);

    assertSame(ar, authTenantContext.getTenant());
    assertEquals("AR", resourceTenantContext.getTenantId());
    verify(request, never()).abortWith(any());
  }

  @Test
  void missingTenantAbortsWithBadRequestProblem() {
    when(tenantRegistry.resolveEnabledTenant(null))
        .thenThrow(new InvalidRequestException("X-Tenant-Id header is required"));
    ContainerRequestContext request = request("/otp/verify", null);

    filter.filter(request);

    assertAbortResponse(request, 400, "Invalid tenant context");
    verify(tenantRegistry).resolveEnabledTenant(null);
  }

  @Test
  void unknownTenantAbortsWithBadRequestProblem() {
    when(tenantRegistry.resolveEnabledTenant("UNKNOWN"))
        .thenThrow(new InvalidRequestException("Unknown tenant"));
    ContainerRequestContext request = request("/saml/callback", "UNKNOWN");

    filter.filter(request);

    assertAbortResponse(request, 400, "Invalid tenant context");
  }

  @Test
  void disabledTenantAbortsWithForbiddenProblem() {
    when(tenantRegistry.resolveEnabledTenant("PNPG"))
        .thenThrow(new ForbiddenException("Tenant is not enabled for auth"));
    ContainerRequestContext request = request("/oidc/exchange", "PNPG");

    filter.filter(request);

    assertAbortResponse(request, 403, "Tenant is not enabled for this authentication flow");
  }

  @Test
  void unrelatedRoutesDoNotResolveOrSetTenant() {
    ContainerRequestContext request = request("/health/ready", "AR");

    filter.filter(request);

    verify(tenantRegistry, never()).resolveEnabledTenant(any());
    verify(request, never()).abortWith(any());
    assertFalse(resourceTenantContext.isInitialized());
  }

  private ContainerRequestContext request(String path, String tenantId) {
    ContainerRequestContext request = mock(ContainerRequestContext.class);
    UriInfo uriInfo = mock(UriInfo.class);
    when(request.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn(path);
    when(request.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn(tenantId);
    when(request.getMethod()).thenReturn("POST");
    return request;
  }

  private void assertAbortResponse(
      ContainerRequestContext request, int expectedStatus, String expectedDetail) {
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(request).abortWith(responseCaptor.capture());
    Response response = responseCaptor.getValue();

    assertEquals(expectedStatus, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    Problem problem = assertInstanceOf(Problem.class, response.getEntity());
    assertEquals(expectedStatus, problem.getStatus());
    assertEquals(expectedDetail, problem.getDetail());
  }
}
