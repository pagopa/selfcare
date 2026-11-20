package it.pagopa.selfcare.mscore.connector.rest.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.mscore.model.DelegationNotificationToSend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@ContextConfiguration(classes = EventHubRestClientTest.Config.class)
class EventHubRestClientTest extends BaseFeignRestClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @TestConfiguration
    @EnableFeignClients(clients = EventHubRestClient.class)
    static class Config {
    }

    @Autowired
    private EventHubRestClient client;

    @Test
    void usesPerRequestNamespaceAndTopic() {
        wm.stubFor(post(urlPathEqualTo("/ar-delegations/messages")).willReturn(aResponse().withStatus(201)));
        wm.stubFor(post(urlPathEqualTo("/pnpg-delegations/messages")).willReturn(aResponse().withStatus(201)));
        for (String tenant : new String[] {"ar", "pnpg"}) {
            client.sendMessage(URI.create(wm.getRuntimeInfo().getHttpBaseUrl() + "/" + tenant + "-delegations"),
                    new DelegationNotificationToSend(), Map.of("Authorization", "Bearer " + tenant));
            wm.verify(postRequestedFor(urlPathEqualTo("/" + tenant + "-delegations/messages"))
                    .withHeader("Authorization", equalTo("Bearer " + tenant)));
        }
    }
}
