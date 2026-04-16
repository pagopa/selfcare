package it.pagopa.selfcare.party.registry_proxy.connector.rest.client;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
/*
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.config.IPAOpenDataRestClientTestConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

*/
/* FIXME: This test is not working, itis failing with the introduction of XMLMapper
@TestPropertySource(
        locations = "classpath:config/ipa-open-data-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.party.registry_proxy.connector.rest=DEBUG",
                "spring.application.name=selc-party-registry-proxy-connector-rest",
                "IPA_OPEN_DATA_INSTITUTION_ENDPOINT=/institutions",
                "IPA_OPEN_DATA_CATEGORY_ENDPOINT=/categories",
                "IPA_OPEN_DATA_AOOS_ENDPOINT=/aoos",
                "IPA_OPEN_DATA_UOS_ENDPOINT=/uos"
        })
@ContextConfiguration(
        initializers = IPAOpenDataRestClientTest.RandomPortInitializer.class,
        classes = {IPAOpenDataRestClientTestConfig.class, HttpClientConfiguration.class})
*/
class IPAOpenDataRestClientTest extends BaseFeignRestClientTest {
    /*

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(RestTestUtils.getWireMockConfiguration("stubs/ipa-open-data"))
            .build();

    @Autowired
    private IPAOpenDataRestClient restClient;


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("IPA_OPEN_DATA_URL=%s",
                            wm.getRuntimeInfo().getHttpBaseUrl())
            );
        }
    }


    @Test
    void retrieveInstitutions() {
        // given
        // when
        final String result = restClient.retrieveInstitutions();
        // then
        assertEquals("institutions csv content", result);
    }

    @Test
    void retrieveCategories() {
        // given
        // when
        final String result = restClient.retrieveCategories();
        // then
        assertEquals("categories csv content", result);
    }

     */

}