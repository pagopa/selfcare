package it.pagopa.selfcare.dashboard.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.FeignException;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.dashboard.config.UserRegistryRestClientTestConfig;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user_registry.EmbeddedExternalId;
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

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(
        locations = "classpath:config/user-registry-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.dashboard.connector.rest=DEBUG",
                "spring.application.name=selc-dashboard-connector-rest",
                "feign.okhttp.enabled=true"
        }
)
@ContextConfiguration(
        initializers = UserRegistryRestClientTest.RandomPortInitializer.class,
        classes = {UserRegistryRestClientTestConfig.class})
@EnableFeignClients(clients = {UserRegistryRestClient.class})
class UserRegistryRestClientTest extends BaseFeignRestClientTest {

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(RestTestUtils.getWireMockConfiguration("stubs/user-registry"))
            .build();


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("USERVICE_USER_REGISTRY_URL=%s/pdnd-interop-uservice-user-registry/0.0.1",
                            wm.getRuntimeInfo().getHttpBaseUrl())
            );
        }
    }

    @Autowired
    private UserRegistryRestClient restClient;

    @Test
    void getUserByInternalId_nullFieldList() {
        //given
        UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        //when
        final Executable executable = () -> restClient.getUserByInternalId(userId, null);
        //then
        final FeignException.NotFound e = assertThrows(FeignException.NotFound.class, executable);
        assertTrue(e.getMessage().contains("Request was not matched"));
    }

    @Test
    void getUserByInternalId_emptyFieldList() {
        //given
        UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        final EnumSet<User.Fields> fieldList = EnumSet.noneOf(User.Fields.class);
        //when
        final Executable executable = () -> restClient.getUserByInternalId(userId, fieldList);
        //then
        final FeignException.NotFound e = assertThrows(FeignException.NotFound.class, executable);
        assertTrue(e.getMessage().contains("Request was not matched"));
    }

    @Test
    void getUserByInternalId_fullyValued() {
        //given
        UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        //when
        User response = restClient.getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        //then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertNotNull(response.getName().getValue());
        assertNotNull(response.getFamilyName().getValue());
    }

    @Test
    void getUserByInternalId_fullyNull() {
        //given
        UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa7");
        //when
        User response = restClient.getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        //then
        assertNotNull(response);
        assertNull(response.getId());
    }

    @Test
    void search_fullyValued() {
        //given
        String externalId = "externalId1";
        //when
        User response = restClient.search(new EmbeddedExternalId(externalId), EnumSet.allOf(User.Fields.class));
        //then
        assertNotNull(response);
        assertNotNull(response.getWorkContacts());
        assertEquals(externalId, response.getFiscalCode());
    }



    @Test
    void delete() {
        //given
        UUID uuid = UUID.randomUUID();
        //when
        Executable executable = () -> restClient.deleteById(uuid);
        //then
        assertDoesNotThrow(executable);
    }

}