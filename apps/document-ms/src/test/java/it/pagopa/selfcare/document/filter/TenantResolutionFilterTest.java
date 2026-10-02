package it.pagopa.selfcare.document.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.document.model.dto.response.Problem;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantResolutionFilterTest {

  @Mock TenantRegistry tenantRegistry;
  @Mock TenantContext tenantContext;
  @Mock ContainerRequestContext requestContext;
  @Mock UriInfo uriInfo;

  private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
  private TenantResolutionFilter filter;

  @BeforeEach
  void setUp() {
    filter = new TenantResolutionFilter();
    filter.tenantRegistry = tenantRegistry;
    filter.tenantContext = tenantContext;
    filter.tenantEnforcementEnabled = true;
    filter.defaultTenant = "AR";
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(requestContext.getHeaders()).thenReturn(headers);
    when(uriInfo.getPath()).thenReturn("/v1/documents");
    when(tenantRegistry.normalizeTenantId(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class).trim().toUpperCase());
  }

  @ParameterizedTest
  @ValueSource(strings = {"q", "q/health", "/q/health/ready", "/q/openapi"})
  void filter_shouldSkipTechnicalEndpoints(String path) {
    when(uriInfo.getPath()).thenReturn(path);

    filter.filter(requestContext);

    verifyNoInteractions(tenantRegistry, tenantContext);
    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void filter_shouldNotSkipPathsStartingWithQLetter() {
    assertFalse(TenantResolutionFilter.isTechnicalPath("/quick"));
    assertTrue(TenantResolutionFilter.isTechnicalPath("/q/metrics"));
  }

  @Test
  void filter_shouldResolveAndNormalizeHeaderTenant() {
    headers.add(TenantResolutionFilter.TENANT_HEADER, " ar ");

    filter.filter(requestContext);

    verify(tenantRegistry).resolve(" ar ");
    verify(tenantContext).setTenantId("AR");
    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void filter_shouldRejectMissingHeaderWhenEnforcementIsEnabled() {
    doThrow(new IllegalArgumentException("blank")).when(tenantRegistry).resolve(null);

    filter.filter(requestContext);

    assertBadRequest();
    verifyNoInteractions(tenantContext);
  }

  @Test
  void filter_shouldRejectUnknownTenant() {
    headers.add(TenantResolutionFilter.TENANT_HEADER, "PNPG");
    doThrow(new UnknownTenantException("PNPG")).when(tenantRegistry).resolve("PNPG");

    filter.filter(requestContext);

    assertBadRequest();
    verify(tenantContext, never()).setTenantId(anyString());
  }

  @Test
  void filter_shouldRejectDuplicatedHeaders() {
    headers.put(TenantResolutionFilter.TENANT_HEADER, List.of("AR", "PNPG"));

    filter.filter(requestContext);

    assertBadRequest();
    verifyNoInteractions(tenantRegistry, tenantContext);
  }

  @Test
  void filter_shouldRejectCommaSeparatedTenants() {
    headers.add(TenantResolutionFilter.TENANT_HEADER, "AR,PNPG");

    filter.filter(requestContext);

    assertBadRequest();
    verifyNoInteractions(tenantRegistry, tenantContext);
  }

  @Test
  void filter_shouldUseDefaultTenantWhenEnforcementIsDisabledAndHeaderIsMissing() {
    filter.tenantEnforcementEnabled = false;

    filter.filter(requestContext);

    verify(tenantRegistry).resolve("AR");
    verify(tenantContext).setTenantId("AR");
    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void filter_shouldPreferHeaderWhenEnforcementIsDisabled() {
    filter.tenantEnforcementEnabled = false;
    headers.add(TenantResolutionFilter.TENANT_HEADER, "PNPG");
    doThrow(new UnknownTenantException("PNPG")).when(tenantRegistry).resolve("PNPG");

    filter.filter(requestContext);

    assertBadRequest();
  }

  @Test
  void filter_shouldExposeResolvedTenantInMdc() {
    headers.add(TenantResolutionFilter.TENANT_HEADER, "ar");

    filter.filter(requestContext);

    assertEquals("AR", MDC.get(TenantResolutionFilter.TENANT_MDC_KEY));
    new TenantMdcCleanupFilter().filter(requestContext, null);
    assertNull(MDC.get(TenantResolutionFilter.TENANT_MDC_KEY));
  }

  @Test
  void filter_shouldNotLeakPreviousTenantInMdcWhenRejected() {
    MDC.put(TenantResolutionFilter.TENANT_MDC_KEY, "AR");
    headers.add(TenantResolutionFilter.TENANT_HEADER, "PNPG");
    doThrow(new UnknownTenantException("PNPG")).when(tenantRegistry).resolve("PNPG");

    filter.filter(requestContext);

    assertNull(MDC.get(TenantResolutionFilter.TENANT_MDC_KEY));
  }

  @Test
  void sanitize_shouldNeutralizeUnsafeHeaderValues() {
    assertEquals("unknown", TenantLogUtils.sanitize(null));
    assertEquals("unknown", TenantLogUtils.sanitize(" "));
    assertEquals("AR__FAKE", TenantLogUtils.sanitize("AR\n FAKE"));
    assertEquals(32, TenantLogUtils.sanitize("A".repeat(100)).length());
  }

  private void assertBadRequest() {
    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(captor.capture());
    Response response = captor.getValue();
    assertEquals(400, response.getStatus());
    assertEquals("application/problem+json", response.getMediaType().toString());
    Problem problem = (Problem) response.getEntity();
    assertEquals(TenantResolutionFilter.INVALID_TENANT_CONTEXT, problem.getTitle());
    assertEquals(TenantResolutionFilter.INVALID_TENANT_CONTEXT, problem.getDetail());
    assertEquals(400, problem.getStatus());
  }
}
