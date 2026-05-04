package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.dashboard.client.UserRegistryRestClient;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.support.SupportRequest;
import it.pagopa.selfcare.dashboard.model.support.SupportResponse;
import it.pagopa.selfcare.dashboard.model.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.UUID;

import static it.pagopa.selfcare.dashboard.model.user.User.Fields.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SupportServiceImplTest extends BaseServiceTest {

    @InjectMocks
    SupportServiceImpl supportServiceImpl;
    @Mock
    private UserRegistryRestClient userRegistryRestClient;
    private static final EnumSet<User.Fields> USER_FIELD_LIST = EnumSet.of(name, familyName, fiscalCode);

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void sendRequest() throws IOException {

        ReflectionTestUtils.setField(supportServiceImpl, "supportApiKey", "testSupportApiKeyLongerThan256bitsToAllowHS256Encoding");
        ReflectionTestUtils.setField(supportServiceImpl, "returnTo", "testReturnTo");
        ReflectionTestUtils.setField(supportServiceImpl, "zendeskOrganization", "testZendeskOrganization");
        ReflectionTestUtils.setField(supportServiceImpl, "actionUrl", "testActionUrl");

        SupportResponse expectation = new SupportResponse();
        expectation.setRedirectUrl("testReturnTo?product=productId&institution=institutionId");
        expectation.setActionUrl("testActionUrl");

        ClassPathResource resource = new ClassPathResource("json/SupportRequest.json");
        byte[] supportRequestStream = Files.readAllBytes(resource.getFile().toPath());
        SupportRequest supportRequest = new ObjectMapper().readValue(supportRequestStream, new TypeReference<>() {
        });
        supportRequest.setUserId(UUID.randomUUID().toString());

        ClassPathResource userResource = new ClassPathResource("json/User.json");
        byte[] userStream = Files.readAllBytes(userResource.getFile().toPath());
        User user = new ObjectMapper().readValue(userStream, new TypeReference<>() {
        });

        when(userRegistryRestClient.getUserByInternalId(UUID.fromString(supportRequest.getUserId()), USER_FIELD_LIST)).thenReturn(user);

        SupportResponse response = supportServiceImpl.sendRequest(supportRequest);

        Assertions.assertEquals(expectation.getRedirectUrl(), response.getRedirectUrl());
        Assertions.assertEquals(expectation.getActionUrl(), response.getActionUrl());
        Assertions.assertNotNull(response.getJwt());
        Mockito.verify(userRegistryRestClient, Mockito.times(1)).getUserByInternalId(UUID.fromString((supportRequest.getUserId())), USER_FIELD_LIST);
    }

    @Test
    void sendRequest_withData() throws IOException {

        ReflectionTestUtils.setField(supportServiceImpl, "supportApiKey", "testSupportApiKeyLongerThan256bitsToAllowHS256Encoding");
        ReflectionTestUtils.setField(supportServiceImpl, "returnTo", "testReturnTo");
        ReflectionTestUtils.setField(supportServiceImpl, "zendeskOrganization", "testZendeskOrganization");
        ReflectionTestUtils.setField(supportServiceImpl, "actionUrl", "testActionUrl");

        SupportResponse expectation = new SupportResponse();
        expectation.setRedirectUrl("testReturnTo?product=productId&institution=institutionId&data=%7B%22logId%22%3A%20%22hello%20world%22%7D");
        expectation.setActionUrl("testActionUrl");

        ClassPathResource resource = new ClassPathResource("json/SupportRequest.json");
        byte[] supportRequestStream = Files.readAllBytes(resource.getFile().toPath());
        SupportRequest supportRequest = new ObjectMapper().readValue(supportRequestStream, new TypeReference<>() {
        });
        supportRequest.setUserId(UUID.randomUUID().toString());
        supportRequest.setData("%7B%22logId%22%3A%20%22hello%20world%22%7D");

        ClassPathResource userResource = new ClassPathResource("json/User.json");
        byte[] userStream = Files.readAllBytes(userResource.getFile().toPath());
        User user = new ObjectMapper().readValue(userStream, new TypeReference<>() {
        });

        when(userRegistryRestClient.getUserByInternalId(UUID.fromString(supportRequest.getUserId()), USER_FIELD_LIST)).thenReturn(user);

        SupportResponse response = supportServiceImpl.sendRequest(supportRequest);

        Assertions.assertEquals(expectation.getRedirectUrl(), response.getRedirectUrl());
        Assertions.assertEquals(expectation.getActionUrl(), response.getActionUrl());
        Assertions.assertNotNull(response.getJwt());
        Mockito.verify(userRegistryRestClient, Mockito.times(1)).getUserByInternalId(UUID.fromString((supportRequest.getUserId())), USER_FIELD_LIST);
    }

    @Test
    void sendRequest_userIdNotFound() throws IOException {

        ClassPathResource resource = new ClassPathResource("json/SupportRequest.json");
        byte[] supportRequestStream = Files.readAllBytes(resource.getFile().toPath());
        SupportRequest supportRequest = new ObjectMapper().readValue(supportRequestStream, new TypeReference<>() {
        });
        supportRequest.setUserId(UUID.randomUUID().toString());

        when(userRegistryRestClient.getUserByInternalId(UUID.fromString(supportRequest.getUserId()), USER_FIELD_LIST)).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> supportServiceImpl.sendRequest(supportRequest));
    }
}