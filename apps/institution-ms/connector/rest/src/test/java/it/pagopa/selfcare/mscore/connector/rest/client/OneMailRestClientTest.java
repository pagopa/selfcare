package it.pagopa.selfcare.mscore.connector.rest.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.UnresolvedTenantException;
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
import org.springframework.context.annotation.Import;
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
                "MAIL_AR=test-api-key",
                "MAIL_PNPG=pnpg-api-key",
                "tenant.mongodb.required=false",
                "ONE_MAIL_REST_CLIENT_READ_TIMEOUT=5000"
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

    @Order(1)
    @RegisterExtension
    static WireMockExtension wmPnpg = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @TestConfiguration
    @EnableFeignClients(clients = OneMailRestClient.class)
    @Import(TenantConfiguration.class)
    static class OneMailRestClientTestConfig {
    }

    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            configureRegistry(applicationContext, wm.getRuntimeInfo().getHttpBaseUrl(),
                    wmPnpg.getRuntimeInfo().getHttpBaseUrl());
        }
    }

    static void configureRegistry(ConfigurableApplicationContext context, String arUrl, String pnpgUrl) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "tenant.registry.json=" + """
                {"AR":{"oneMail":{"url":"%s","sender":"ar@test.it","apiKeyEnvVar":"MAIL_AR"}},
                 "PNPG":{"oneMail":{"url":"%s","sender":"pnpg@test.it","apiKeyEnvVar":"MAIL_PNPG"}}}
                """.formatted(arUrl, pnpgUrl).replace("\n", ""));
    }

    @Autowired
    private OneMailRestClient restClient;

    @Autowired
    private TenantContext tenantContext;

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

        ResponseEntity<EmailSuccessResponseDTO> response = tenantContext.withTenant("AR",
                () -> restClient._v1EmailsSendHighPost(false, body));

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
        wmPnpg.stubFor(post(urlPathEqualTo("/v1/emails/send/high"))
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody("{\"requestId\":\"pnpg-request-id\"}")));
        assertEquals("pnpg-request-id", tenantContext.withTenant("PNPG",
                () -> restClient._v1EmailsSendHighPost(false, body)).getBody().getRequestId());
        wmPnpg.verify(postRequestedFor(urlPathEqualTo("/v1/emails/send/high"))
                .withHeader("x-api-key", equalTo("pnpg-api-key")));
        wm.verify(1, postRequestedFor(urlPathEqualTo("/v1/emails/send/high")));
    }

    @Test
    void missingTenantDoesNotSend() {
        assertThrows(UnresolvedTenantException.class,
                () -> restClient._v1EmailsSendHighPost(false, EmailHighPriorityBodyDTO.builder().build()));
        wm.verify(0, postRequestedFor(urlPathEqualTo("/v1/emails/send/high")));
        wmPnpg.verify(0, postRequestedFor(urlPathEqualTo("/v1/emails/send/high")));
    }
}
