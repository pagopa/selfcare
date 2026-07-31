package it.pagopa.selfcare.mscore.connector.rest.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

class TenantHeaderInterceptorTest {

  private enum FakeTenantId {
    AR
  }

  private final TenantHeaderInterceptor interceptor = new TenantHeaderInterceptor();

  @AfterEach
  void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static void bindRequestWith(Object validatedTenant) {
    // A servlet API is not on this module's test classpath, so the request context is stubbed
    // rather than backed by MockHttpServletRequest.
    RequestAttributes attributes = mock(RequestAttributes.class);
    when(attributes.getAttribute(
            TenantHeaderInterceptor.TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
        .thenReturn(validatedTenant);
    RequestContextHolder.setRequestAttributes(attributes);
  }

  @Test
  void addsTheValidatedTenantHeader() {
    bindRequestWith(FakeTenantId.AR);
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertEquals(
        "AR",
        template.headers().get(TenantHeaderInterceptor.TENANT_HEADER).iterator().next());
  }

  @Test
  void addsNoHeaderWhenTheRequestHasNoValidatedTenant() {
    bindRequestWith(null);
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertFalse(template.headers().containsKey(TenantHeaderInterceptor.TENANT_HEADER));
  }

  @Test
  void addsNoHeaderOutsideAnActiveRequest() {
    RequestContextHolder.resetRequestAttributes();
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertTrue(template.headers().isEmpty());
  }

  @Test
  void ignoresANonEnumAttributeValue() {
    // Defensive: a plain String under that attribute name did not come from the filter.
    bindRequestWith("AR");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertFalse(template.headers().containsKey(TenantHeaderInterceptor.TENANT_HEADER));
  }
}
