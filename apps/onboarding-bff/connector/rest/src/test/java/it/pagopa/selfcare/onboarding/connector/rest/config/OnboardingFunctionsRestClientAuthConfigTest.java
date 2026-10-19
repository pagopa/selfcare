package it.pagopa.selfcare.onboarding.connector.rest.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class OnboardingFunctionsRestClientAuthConfigTest {

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

    // when
    String interceptor =
        properties.getProperty(
            "feign.client.config.onboarding-functions-api.requestInterceptors[0]");
    String functionKey =
        properties.getProperty(
            "feign.client.config.onboarding-functions-api.defaultRequestHeaders.x-functions-key[0]");

    // then
    assertEquals(
        "it.pagopa.selfcare.commons.connector.rest.interceptor.AuthorizationHeaderInterceptor",
        interceptor);
    assertNotNull(functionKey);
  }
}
