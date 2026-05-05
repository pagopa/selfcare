package it.pagopa.selfcare.external_api.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.external_api.client.config.UserRegistryRestClientTestConfig;
import it.pagopa.selfcare.external_api.client.decoder.FeignErrorDecoder;
import it.pagopa.selfcare.external_api.exception.ResourceNotFoundException;
import it.pagopa.selfcare.external_api.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.external_api.model.user.SaveUserDto;
import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserId;
import it.pagopa.selfcare.external_api.model.user_registry.EmbeddedExternalId;
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
                "logging.level.it.pagopa.selfcare.external_api.connector.rest=DEBUG",
                "spring.application.name=selc-external-api-connector-rest",
                "spring.cloud.openfeign.okhttp.enabled=true"
        }
)
@ContextConfiguration(
        initializers = UserRegistryRestClientTest.RandomPortInitializer.class,
        classes = {UserRegistryRestClientTestConfig.class, FeignErrorDecoder.class})
@EnableFeignClients(clients = UserRegistryRestClient.class)
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
        final EnumSet<User.Fields> fieldList = null;
        //when
        final Executable executable = () -> restClient.getUserByInternalId(userId, fieldList);
        //then
        final ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertTrue(e.getMessage().contains("Not Found"));
    }

    @Test
    void getUserByInternalId_emptyFieldList() {
        //given
        UUID userId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        final EnumSet<User.Fields> fieldList = EnumSet.noneOf(User.Fields.class);
        //when
        final Executable executable = () -> restClient.getUserByInternalId(userId, fieldList);
        //then
        final ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, executable);
        assertTrue(e.getMessage().contains("Not Found"));
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
    void saveUser() {
        //given
        SaveUserDto userDto = TestUtils.mockInstance(new SaveUserDto(), "setWorkContacts");
        //when
        UserId id = restClient.saveUser(userDto);
        //then
        assertNotNull(id);
    }

    @Test
    void patchUser() {
        //given
        UUID id = UUID.randomUUID();
        MutableUserFieldsDto mutableUserFieldsDto = TestUtils.mockInstance(new MutableUserFieldsDto(), "setWorkContacts");
        //when
        Executable executable = () -> restClient.patchUser(id, mutableUserFieldsDto);
        //then
        assertDoesNotThrow(executable);
    }

}