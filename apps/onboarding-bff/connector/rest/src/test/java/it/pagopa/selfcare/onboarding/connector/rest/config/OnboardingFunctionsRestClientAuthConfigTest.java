package it.pagopa.selfcare.onboarding.connector.rest.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import feign.RequestTemplate;
import it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor;
import it.pagopa.selfcare.onboarding.connector.rest.interceptor.TenantHeaderInterceptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class OnboardingFunctionsRestClientAuthConfigTest {

  @AfterEach
  void resetRequestContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void configuresSessionTokenAndFunctionKeyPropagation() throws IOException {
    // given
    Properties properties = new Properties();
    try (InputStream stream =
        getClass()
            .getClassLoader()
            .getResourceAsStream("config/onboarding-functions-rest-client.properties")) {
      assertNotNull(stream);
      properties.load(stream);
    }
    MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
    incomingRequest.addHeader("Authorization", "Bearer fake-session-token");
    incomingRequest.addHeader("X-Tenant-Id", "AR");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));
    RequestTemplate requestTemplate = new RequestTemplate();

    // when
    String interceptor =
        properties.getProperty(
            "feign.client.config.onboarding-functions-api.requestInterceptors[0]");
    String functionKey =
        properties.getProperty(
            "feign.client.config.onboarding-functions-api.defaultRequestHeaders.x-functions-key[0]");
    new AuthorizationHeaderInterceptor().apply(requestTemplate);
    new TenantHeaderInterceptor().apply(requestTemplate);
    requestTemplate.header("x-functions-key", functionKey);

    // then
    assertEquals(
        "it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor",
        interceptor);
    assertNotNull(functionKey);
    assertEquals(List.of("Bearer fake-session-token"), requestTemplate.headers().get("Authorization"));
    assertEquals(List.of("AR"), requestTemplate.headers().get("X-Tenant-Id"));
    assertEquals(List.of(functionKey), requestTemplate.headers().get("x-functions-key"));
  }
}
