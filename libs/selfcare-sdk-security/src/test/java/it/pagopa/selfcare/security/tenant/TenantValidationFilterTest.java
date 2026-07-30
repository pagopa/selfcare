package it.pagopa.selfcare.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantValidationFilterTest {

  private static final String TENANT_HEADER = TenantConstants.TENANT_HEADER;
  private static final String TENANT_CLAIM = TenantConstants.TENANT_CLAIM;

  @Mock
  private JsonWebToken jwt;

  @Mock
  private ContainerRequestContext requestContext;

  private TenantContext tenantContext;
  private TenantValidationFilter filter;

  @BeforeEach
  void setUp() {
    tenantContext = new TenantContext();
    filter = new TenantValidationFilter();
    filter.jwt = jwt;
    filter.tenantContext = tenantContext;
  }

  @Test
  void shouldSkipEnforcementWhenNoAuthenticatedIssuer() {
    when(jwt.getIssuer()).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
    assertEquals(null, tenantContext.getTenant());
  }

  @Test
  void shouldAcceptConsistentHeaderAndClaimForOneIdentityFlow() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn("AR");

    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
    assertEquals(TenantId.AR, tenantContext.getTenant());
  }

  @Test
  void shouldRejectWhenHeaderMissing() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
    assertEquals(null, tenantContext.getTenant());
  }

  @Test
  void shouldRejectWhenHeaderIsUnknownTenant() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("UNKNOWN");

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
  }

  @Test
  void shouldRejectWhenHeaderIsDuplicated() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR,PNPG");

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
  }

  @Test
  void shouldRejectWhenClaimMissingForNonHubSpidLoginIssuer() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
    assertEquals(null, tenantContext.getTenant());
  }

  @Test
  void shouldRejectOnHeaderClaimMismatch() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn("PNPG");

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
    assertEquals(null, tenantContext.getTenant());
  }

  @Test
  void shouldDefaultToPnpgWhenHubSpidLoginTokenMissesClaim() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.HUB_SPID_LOGIN_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("PNPG");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
    assertEquals(TenantId.PNPG, tenantContext.getTenant());
  }

  @Test
  void shouldRejectHubSpidLoginTokenMissingClaimWhenHeaderIsNotPnpg() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.HUB_SPID_LOGIN_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
    assertEquals(null, tenantContext.getTenant());
  }

  @Test
  void shouldRejectWhenClaimIsUnknownTenant() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn("AR");
    when(jwt.getClaim(TENANT_CLAIM)).thenReturn("NOT_A_TENANT");

    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
  }

  @Test
  void rejectionResponseShouldBeProblemJsonWithBadRequestStatus() {
    when(jwt.getIssuer()).thenReturn(TenantConstants.ONE_IDENTITY_ISSUER);
    when(requestContext.getHeaderString(TENANT_HEADER)).thenReturn(null);

    filter.filter(requestContext);

    org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(captor.capture());
    Response response = captor.getValue();
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    assertTrue(response.getEntity() instanceof TenantProblem);
  }
}
