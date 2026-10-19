package it.pagopa.selfcare.mscore.connector.rest.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.RetryableException;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailHighPriorityBodyDTO;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.net.SocketTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(
        locations = "classpath:config/one-mail-rest-client.properties",
        properties = {
                "ONE_MAIL_API_KEY=test-api-key",
                "ONE_MAIL_REST_CLIENT_READ_TIMEOUT=500"
        }
)
@ContextConfiguration(
        initializers = OneMailRestClientTimeoutTest.RandomPortInitializer.class,
        classes = OneMailRestClientTest.OneMailRestClientTestConfig.class)
class OneMailRestClientTimeoutTest extends BaseFeignRestClientTest {

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("ONE_MAIL_URL=%s", wm.getRuntimeInfo().getHttpBaseUrl()));
        }
    }

    @Autowired
    private OneMailRestClient restClient;

    @Test
    void sendHighPriorityEmail_readTimeout() {
        wm.stubFor(post(urlPathEqualTo("/v1/emails/send/high"))
                .willReturn(aResponse().withStatus(202).withFixedDelay(2000)));
        EmailHighPriorityBodyDTO body = EmailHighPriorityBodyDTO.builder().build();

        RetryableException exception = assertThrows(RetryableException.class,
                () -> restClient._v1EmailsSendHighPost(false, body));

        assertInstanceOf(SocketTimeoutException.class, exception.getCause());
        // The client can time out before WireMock records the request.
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> wm.verify(postRequestedFor(urlPathEqualTo("/v1/emails/send/high"))));
    }
}
