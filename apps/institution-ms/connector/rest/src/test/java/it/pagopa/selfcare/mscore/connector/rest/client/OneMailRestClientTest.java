package it.pagopa.selfcare.mscore.connector.rest.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.RetryableException;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailAddress;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailHighPriorityBodyDTO;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailSuccessResponseDTO;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.Map;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(
        locations = "classpath:config/one-mail-rest-client.properties",
        properties = {
                "ONE_MAIL_API_KEY=test-api-key",
                "ONE_MAIL_REST_CLIENT_READ_TIMEOUT=500"
        }
)
@ContextConfiguration(
        initializers = OneMailRestClientTest.RandomPortInitializer.class,
        classes = OneMailRestClientTest.OneMailRestClientTestConfig.class)
class OneMailRestClientTest extends BaseFeignRestClientTest {

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @TestConfiguration
    @EnableFeignClients(clients = OneMailRestClient.class)
    static class OneMailRestClientTestConfig {
    }

    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("ONE_MAIL_URL=%s", wm.getRuntimeInfo().getHttpBaseUrl()));
        }
    }

    @Autowired
    private OneMailRestClient restClient;

    @Test
    void sendHighPriorityEmail() {
        wm.stubFor(post(urlPathEqualTo("/v1/emails/send/high"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"requestId\":\"request-id\"}")));

        EmailHighPriorityBodyDTO body = EmailHighPriorityBodyDTO.builder()
                .from(new EmailAddress().email("noreply@test.it"))
                .to(new EmailAddress().email("user@test.it"))
                .templateContent(Map.of(
                        "templateId", "selfcare_user_pt_delegation",
                        "templateAttributes", Map.of("productName", "product")))
                .build();

        ResponseEntity<EmailSuccessResponseDTO> response = restClient._v1EmailsSendHighPost(false, body);

        assertEquals("request-id", Objects.requireNonNull(response.getBody()).getRequestId());
        wm.verify(postRequestedFor(urlPathEqualTo("/v1/emails/send/high"))
                .withQueryParam("dryRun", equalTo("false"))
                .withHeader("x-api-key", equalTo("test-api-key"))
                .withRequestBody(equalToJson("""
                        {
                          "from": {"email": "noreply@test.it"},
                          "to": {"email": "user@test.it"},
                          "templateContent": {
                            "templateId": "selfcare_user_pt_delegation",
                            "templateAttributes": {"productName": "product"}
                          }
                        }
                        """)));
    }

    @Test
    void sendHighPriorityEmail_readTimeout() {
        wm.stubFor(post(urlPathEqualTo("/v1/emails/send/high"))
                .willReturn(aResponse().withStatus(202).withFixedDelay(2000)));
        EmailHighPriorityBodyDTO body = EmailHighPriorityBodyDTO.builder().build();

        assertThrows(RetryableException.class, () -> restClient._v1EmailsSendHighPost(false, body));
    }
}
