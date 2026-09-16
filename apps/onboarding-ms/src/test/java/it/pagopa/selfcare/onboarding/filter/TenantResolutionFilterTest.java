package it.pagopa.selfcare.onboarding.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.ws.rs.container.ContainerRequestContext;
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

    @Mock
    TenantRegistry tenantRegistry;

    @Mock
    TenantContext tenantContext;

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    UriInfo uriInfo;

    private TenantResolutionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantResolutionFilter();
        filter.tenantRegistry = tenantRegistry;
        filter.tenantContext = tenantContext;
        filter.tenantEnforcementEnabled = true;
        filter.defaultTenant = "AR";
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
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
        when(uriInfo.getPath()).thenReturn("onboarding");
        when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn(" ar ");
        when(tenantRegistry.normalizeTenantId(" ar ")).thenReturn("AR");

        filter.filter(requestContext);

        verify(tenantRegistry).resolve(" ar ");
        verify(tenantContext).setTenantId("AR");
        verify(requestContext, never()).abortWith(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filter_shouldUseDefaultTenantWhenEnforcementIsDisabledAndHeaderIsMissing() {
        filter.tenantEnforcementEnabled = false;
        filter.defaultTenant = "PNPG";
        when(uriInfo.getPath()).thenReturn("onboarding");
        when(tenantRegistry.normalizeTenantId("PNPG")).thenReturn("PNPG");

        filter.filter(requestContext);

        verify(tenantRegistry).resolve("PNPG");
        verify(tenantContext).setTenantId("PNPG");
    }

    @Test
    void filter_shouldUseDefaultTenantWhenEnforcementIsDisabledAndHeaderIsBlank() {
        filter.tenantEnforcementEnabled = false;
        when(uriInfo.getPath()).thenReturn("onboarding");
        when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn(" ");
        when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");

        filter.filter(requestContext);

        verify(tenantRegistry).resolve("AR");
        verify(tenantContext).setTenantId("AR");
    }

    @Test
    void filter_shouldPreferHeaderWhenEnforcementIsDisabled() {
        filter.tenantEnforcementEnabled = false;
        when(uriInfo.getPath()).thenReturn("onboarding");
        when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("pnpg");
        when(tenantRegistry.normalizeTenantId("pnpg")).thenReturn("PNPG");

        filter.filter(requestContext);

        verify(tenantRegistry).resolve("pnpg");
        verify(tenantContext).setTenantId("PNPG");
    }

    @Test
    void filter_shouldAbortWithBadRequestWhenTenantCannotBeResolved() {
        when(uriInfo.getPath()).thenReturn("onboarding");
        when(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER)).thenReturn("UNKNOWN");
        when(tenantRegistry.resolve("UNKNOWN")).thenThrow(new IllegalArgumentException("Unknown tenant"));

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        Response response = responseCaptor.getValue();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Invalid tenant context", response.getEntity());
        assertEquals("application/problem+json", response.getMediaType().toString());
        verify(tenantContext, never()).setTenantId(org.mockito.ArgumentMatchers.any());
    }
}
