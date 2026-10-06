package it.pagopa.selfcare.product.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

  @Mock TenantRegistry tenantRegistry;

  @Mock TenantContext tenantContext;

  @Mock ContainerRequestContext requestContext;

  @Mock UriInfo uriInfo;

  private TenantResolutionFilter filter;

  @BeforeEach
  void setUp() {
    filter = new TenantResolutionFilter(tenantRegistry, tenantContext, true, "AR");
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    lenient().when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
  }

  @Test
  void filter_shouldSkipQuarkusEndpoints() {
    when(uriInfo.getPath()).thenReturn("q/health");

    filter.filter(requestContext);

    verifyNoInteractions(tenantRegistry, tenantContext);
    verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void filter_shouldSkipQuarkusRootEndpoint() {
    when(uriInfo.getPath()).thenReturn("q");

    filter.filter(requestContext);

    verifyNoInteractions(tenantRegistry, tenantContext);
  }

  @Test
  void filter_shouldResolveAndNormalizeHeaderTenantWhenEnforcementIsEnabled() {
    when(uriInfo.getPath()).thenReturn("v1/products");
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn(" ar ");
    when(tenantRegistry.normalizeTenantId(" ar ")).thenReturn("AR");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve(" ar ");
    verify(tenantContext).setTenantId("AR");
    verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void filter_shouldPreferQueryTenantOverHeaderWhenPresent() {
    when(uriInfo.getPath()).thenReturn("product/prod-test");
    when(uriInfo.getQueryParameters()).thenReturn(queryParameters("pnpg"));
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("pnpg");
    when(tenantRegistry.normalizeTenantId("pnpg")).thenReturn("PNPG");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("pnpg");
    verify(tenantContext).setTenantId("PNPG");
    verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void filter_shouldResolveQueryTenantWithoutHeader() {
    when(uriInfo.getPath()).thenReturn("contract-template/123");
    when(uriInfo.getQueryParameters()).thenReturn(queryParameters("ar"));
    when(tenantRegistry.normalizeTenantId("ar")).thenReturn("AR");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("ar");
    verify(tenantContext).setTenantId("AR");
    verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void filter_shouldIgnoreProductPathWhenQueryTenantIsAbsent() {
    when(uriInfo.getPath()).thenReturn("product/prod-test");
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("AR");
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("AR");
    verify(tenantContext).setTenantId("AR");
  }

  @Test
  void filter_shouldAbortWhenQueryTenantConflictsWithHeaderTenant() {
    when(uriInfo.getPath()).thenReturn("product/prod-test");
    when(uriInfo.getQueryParameters()).thenReturn(queryParameters("ar"));
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("PNPG");
    when(tenantRegistry.normalizeTenantId("ar")).thenReturn("AR");
    when(tenantRegistry.normalizeTenantId("PNPG")).thenReturn("PNPG");

    filter.filter(requestContext);

    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    assertEquals(
        Response.Status.BAD_REQUEST.getStatusCode(), responseCaptor.getValue().getStatus());
    verify(tenantContext, never()).setTenantId(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void filter_shouldUseDefaultTenantWhenEnforcementIsDisabledAndHeaderIsMissing() {
    filter = new TenantResolutionFilter(tenantRegistry, tenantContext, false, "PNPG");
    when(uriInfo.getPath()).thenReturn("v1/products");
    when(tenantRegistry.normalizeTenantId("PNPG")).thenReturn("PNPG");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("PNPG");
    verify(tenantContext).setTenantId("PNPG");
  }

  @Test
  void filter_shouldUseDefaultTenantWhenEnforcementIsDisabledAndHeaderIsBlank() {
    filter = new TenantResolutionFilter(tenantRegistry, tenantContext, false, "AR");
    when(uriInfo.getPath()).thenReturn("v1/products");
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn(" ");
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("AR");
    verify(tenantContext).setTenantId("AR");
  }

  @Test
  void filter_shouldPreferHeaderWhenEnforcementIsDisabled() {
    filter = new TenantResolutionFilter(tenantRegistry, tenantContext, false, "AR");
    when(uriInfo.getPath()).thenReturn("v1/products");
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("pnpg");
    when(tenantRegistry.normalizeTenantId("pnpg")).thenReturn("PNPG");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("pnpg");
    verify(tenantContext).setTenantId("PNPG");
  }

  @Test
  void filter_shouldAbortWithBadRequestWhenTenantCannotBeResolved() {
    when(uriInfo.getPath()).thenReturn("v1/products");
    when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER))
        .thenReturn("UNKNOWN");
    when(tenantRegistry.resolve("UNKNOWN"))
        .thenThrow(new IllegalArgumentException("Unknown tenant"));

    filter.filter(requestContext);

    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(responseCaptor.capture());
    Response response = responseCaptor.getValue();
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("Invalid tenant context", response.getEntity());
    assertEquals("application/problem+json", response.getMediaType().toString());
    verify(tenantContext, never()).setTenantId(org.mockito.ArgumentMatchers.any());
  }

  private static MultivaluedMap<String, String> queryParameters(String tenantId) {
    MultivaluedMap<String, String> parameters = new MultivaluedHashMap<>();
    parameters.add("tenantId", tenantId);
    return parameters;
  }
}
