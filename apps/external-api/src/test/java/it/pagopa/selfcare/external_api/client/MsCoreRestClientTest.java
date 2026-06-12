package it.pagopa.selfcare.external_api.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.external_api.client.config.MsCoreRestClientTestConfig;
import it.pagopa.selfcare.external_api.client.decoder.FeignErrorDecoder;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitution;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitutionRequest;
import it.pagopa.selfcare.external_api.model.pnpg.InstitutionPnPgResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static it.pagopa.selfcare.commons.utils.TestUtils.checkNotNullFields;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestPropertySource(
        locations = "classpath:config/ms-core-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.external_api.connector.rest=DEBUG",
                "spring.application.name=selc-external-api-connector-rest",
                "spring.cloud.openfeign.okhttp.enabled=true"
        })
@ContextConfiguration(
        initializers = MsCoreRestClientTest.RandomPortInitializer.class,
        classes = {MsCoreRestClientTestConfig.class, FeignErrorDecoder.class})
@EnableFeignClients(clients = MsCoreRestClient.class)
class MsCoreRestClientTest extends BaseFeignRestClientTest {
    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(RestTestUtils.getWireMockConfiguration("stubs/ms-core"))
            .build();


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("MS_CORE_URL=%s",
                            wm.getRuntimeInfo().getHttpBaseUrl())
            );
        }
    }

    @Autowired
    private MsCoreRestClient restClient;

    @Test
    void createPnPgInstitution() {
        //given
        CreatePnPgInstitution createPnPgInstitution = new CreatePnPgInstitution();
        createPnPgInstitution.setExternalId("externalId");
        createPnPgInstitution.setDescription("description");
        CreatePnPgInstitutionRequest request = new CreatePnPgInstitutionRequest(createPnPgInstitution);
        //when
        InstitutionPnPgResponse response = restClient.createPnPgInstitution(request);
        //then
        assertNotNull(response);
        checkNotNullFields(response);
    }


}