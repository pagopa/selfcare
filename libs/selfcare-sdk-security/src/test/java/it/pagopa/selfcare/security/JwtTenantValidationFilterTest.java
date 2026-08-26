package it.pagopa.selfcare.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTenantValidationFilterTest {

  @Mock SecurityIdentity securityIdentity;
  @Mock JsonWebToken jsonWebToken;
  @Mock ContainerRequestContext requestContext;

  private JwtTenantValidationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtTenantValidationFilter();
    filter.securityIdentity = securityIdentity;
  }

  @Test
  void filter_acceptsMatchingTenant() {
    when(securityIdentity.getPrincipal()).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn("SPID");
    when(jsonWebToken.getClaim("tenant_id")).thenReturn("AR");
    when(requestContext.getHeaderString("X-Tenant-Id")).thenReturn("AR");

    filter.filter(requestContext);

    verify(requestContext).getHeaderString("X-Tenant-Id");
  }

  @Test
  void filter_defaultsMissingTenantClaimToPnpg() {
    when(securityIdentity.getPrincipal()).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn("SPID");
    when(jsonWebToken.getClaim("tenant_id")).thenReturn(null);
    when(requestContext.getHeaderString("X-Tenant-Id")).thenReturn("PNPG");

    filter.filter(requestContext);

    verify(requestContext).getHeaderString("X-Tenant-Id");
  }

  @Test
  void filter_rejectsTenantMismatch() {
    when(securityIdentity.getPrincipal()).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn("SPID");
    when(jsonWebToken.getClaim("tenant_id")).thenReturn("AR");
    when(requestContext.getHeaderString("X-Tenant-Id")).thenReturn("PNPG");

    filter.filter(requestContext);

    var responseCaptor =
        org.mockito.ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), responseCaptor.getValue().getStatus());
  }

  @Test
  void filter_rejectsMissingTenantHeader() {
    when(securityIdentity.getPrincipal()).thenReturn(jsonWebToken);
    when(jsonWebToken.getIssuer()).thenReturn("SPID");
    when(jsonWebToken.getClaim("tenant_id")).thenReturn(null);
    when(requestContext.getHeaderString("X-Tenant-Id")).thenReturn(null);

    filter.filter(requestContext);

    verify(requestContext).abortWith(org.mockito.ArgumentMatchers.any(Response.class));
  }
}
