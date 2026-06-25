package it.pagopa.selfcare.external_api.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.external_api.client.config.MsPartyRegistryProxyRestClientTestConfig;
import it.pagopa.selfcare.external_api.client.decoder.FeignErrorDecoder;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.institution.InstitutionResource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(
        locations = "classpath:config/ms-party-registry-proxy-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.external_api.connector.rest=DEBUG",
                "spring.application.name=selc-external-api-connector-rest",
                "spring.cloud.openfeign.okhttp.enabled=true"
        })
@ContextConfiguration(
        initializers = MsPartyRegistryProxyRestClientTest.RandomPortInitializer.class,
        classes = {MsPartyRegistryProxyRestClientTestConfig.class, FeignErrorDecoder.class})
@EnableFeignClients(clients = MsPartyRegistryProxyRestClient.class)
class MsPartyRegistryProxyRestClientTest extends BaseFeignRestClientTest {

        @Order(1)
        @RegisterExtension
        static WireMockExtension wm = WireMockExtension.newInstance()
                .options(RestTestUtils.getWireMockConfiguration("stubs/ms-party-registry-proxy"))
                .build();


        static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
                @SneakyThrows
                @Override
                public void initialize(ConfigurableApplicationContext applicationContext) {
                        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                                String.format("USERVICE_PARTY_REGISTRY_PROXY_URL=%s",
                                        wm.getRuntimeInfo().getHttpBaseUrl())
                        );
                }
        }


        @Autowired
        private MsPartyRegistryProxyRestClient restClient;


        @Test
        void findInstitution_fullyValued() {
                //given
                String institutionExternalId = "institutionExteranlIdFullyValued";
                //when
                InstitutionResource response = restClient.findInstitution(institutionExternalId, null, null);
                //then
                assertNotNull(response);
        }

        @Test
        void findInstitution_notFound() {
                // given
                String institutionExternalId = "institutionExternalIdNotFound";
                // when
                Executable executable = () -> restClient.findInstitution(institutionExternalId, null, null);
                //then
                assertThrows(ResourceNotFoundException.class, executable);
        }
}
