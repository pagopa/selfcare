package it.pagopa.selfcare.onboarding.connector.rest.client;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.onboarding.connector.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.connector.model.institutions.MatchInfoResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.infocamere.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.connector.rest.config.PartyRegistryProxyRestClientTestConfig;
import it.pagopa.selfcare.onboarding.connector.rest.model.institution_pnpg.InstitutionByLegalTaxIdRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

@TestPropertySource(
        locations = "classpath:config/party-registry-proxy-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.onboarding.connector.rest=DEBUG",
                "spring.application.name=selc-onboarding-connector-rest",
                "feign.okhttp.enabled=true"
        })
@ContextConfiguration(
        initializers = PartyRegistryProxyRestClientTest.RandomPortInitializer.class,
        classes = {PartyRegistryProxyRestClientTestConfig.class})
class PartyRegistryProxyRestClientTest extends BaseFeignRestClientTest {

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(RestTestUtils.getWireMockConfiguration("stubs/party-registry-proxy"))
            .build();


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("USERVICE_PARTY_REGISTRY_PROXY_URL:%s/external/ur/v1",
                            wm.getRuntimeInfo().getHttpBaseUrl())
            );
        }
    }

    @Autowired
    private PartyRegistryProxyRestClient restClient;

    @Test
    void getInstitutionsByUserLegalTaxId_fullyValued() {
        //given
        InstitutionByLegalTaxIdRequest request = mockInstance(new InstitutionByLegalTaxIdRequest());
        //when
        InstitutionInfoIC response = restClient.getInstitutionsByUserLegalTaxId(request);
        //then
        assertNotNull(response);
        assertNotNull(response.getRequestDateTime());
        assertNotNull(response.getLegalTaxId());
        assertNotNull(response.getBusinesses());
        assertNotNull(response.getBusinesses().get(0).getBusinessName());
        assertNotNull(response.getBusinesses().get(0).getBusinessTaxId());
    }

    @Test
    void matchInstitutionAndUser_checkTrue() {
        //given
        String institutionExternalId = "instituionId";
        String taxCode = "taxCode";
        //when
        MatchInfoResult response = restClient.matchInstitutionAndUser(institutionExternalId, taxCode);
        //then
        assertNotNull(response);
        assertTrue(response.isVerificationResult());
    }

    @Test
    void getInstitutionLegalAddress_fullyValued() {
        //given
        String institutionExternalId = "extInstituionId";
        //when
        InstitutionLegalAddressData response = restClient.getInstitutionLegalAddress(institutionExternalId);
        //then
        assertNotNull(response);
        assertNotNull(response.getAddress());
        assertNotNull(response.getZipCode());
    }

}