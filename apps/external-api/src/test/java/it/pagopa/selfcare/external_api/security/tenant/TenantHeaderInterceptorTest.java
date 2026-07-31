package it.pagopa.selfcare.external_api.security.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TenantHeaderInterceptorTest {

  private final TenantHeaderInterceptor interceptor = new TenantHeaderInterceptor();

  @AfterEach
  void clearRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static void bindRequestWith(Object validatedTenant) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (validatedTenant != null) {
      request.setAttribute(TenantConstants.TENANT_REQUEST_ATTRIBUTE, validatedTenant);
    }
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  @Test
  void addsTheValidatedTenantHeader() {
    bindRequestWith(TenantId.AR);
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertEquals("AR", template.headers().get(TenantConstants.TENANT_HEADER).iterator().next());
  }

  @Test
  void addsNoHeaderWhenTheRequestHasNoValidatedTenant() {
    // Unauthenticated/public requests are not enforced by the filter, so nothing was validated.
    bindRequestWith(null);
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertFalse(template.headers().containsKey(TenantConstants.TENANT_HEADER));
  }

  @Test
  void addsNoHeaderOutsideAnActiveRequest() {
    RequestContextHolder.resetRequestAttributes();
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertTrue(template.headers().isEmpty());
  }

  @Test
  void ignoresANonTenantAttributeValue() {
    // Defensive: never forward whatever happens to sit under that attribute name.
    bindRequestWith("AR");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertFalse(template.headers().containsKey(TenantConstants.TENANT_HEADER));
  }
}
