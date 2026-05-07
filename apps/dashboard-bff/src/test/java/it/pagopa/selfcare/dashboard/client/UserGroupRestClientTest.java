package it.pagopa.selfcare.dashboard.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import it.pagopa.selfcare.commons.connector.rest.BaseFeignRestClientTest;
import it.pagopa.selfcare.commons.connector.rest.RestTestUtils;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.dashboard.config.UserGroupRestClientTestConfig;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.PageOfUserGroupResource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(
        locations = "classpath:config/user-group-rest-client.properties",
        properties = {
                "logging.level.it.pagopa.selfcare.dashboard=DEBUG",
                "spring.application.name=it.pagopa.selfcare.dashboard.SelfCareDashboardApplication",
                "feign.okhttp.enabled=true"
        }
)
@ContextConfiguration(
        initializers = UserGroupRestClientTest.RandomPortInitializer.class,
        classes = {UserGroupRestClientTestConfig.class})
@EnableFeignClients(clients = {UserGroupRestClient.class})
class UserGroupRestClientTest extends BaseFeignRestClientTest {

    @Order(1)
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(RestTestUtils.getWireMockConfiguration("stubs/user-group"))
            .build();


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    String.format("MS_USER_GROUP_URL=%s",
                            wm.getRuntimeInfo().getHttpBaseUrl())
            );
        }
    }

    private enum TestCase {
        FULLY_VALUED,
        FULLY_NULL
    }

    private static final Map<TestCase, String> testCase2igroupdMap = new EnumMap<>(TestCase.class) {{
        put(TestCase.FULLY_VALUED, "groupId1");
        put(TestCase.FULLY_NULL, "groupId2");
    }};
    @Autowired
    private UserGroupRestClient restClient;

    @Test
    void createGroup() {
        //given
        it.pagopa.selfcare.group.generated.openapi.v1.dto.CreateUserGroupDto request = TestUtils.mockInstance(new it.pagopa.selfcare.group.generated.openapi.v1.dto.CreateUserGroupDto());
        request.setMembers(Set.of(TestUtils.mockInstance(UUID.randomUUID())));
        //when
        Executable executable = () -> restClient._createGroupUsingPOST(request);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void deleteUserGroup() {
        //given
        String id = "id";
        //when
        Executable executable = () -> restClient._deleteGroupUsingDELETE(id);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void activateUserGroup() {
        //given
        String id = "id";
        //when
        Executable executable = () -> restClient._activateGroupUsingPOST(id);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void suspendUserGroup() {
        //given
        String id = "id";
        //when
        Executable executable = () -> restClient._suspendGroupUsingPOST(id);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void updateUserGroup() {
        //given
        String id = "id";
        it.pagopa.selfcare.group.generated.openapi.v1.dto.UpdateUserGroupDto request = TestUtils.mockInstance(new it.pagopa.selfcare.group.generated.openapi.v1.dto.UpdateUserGroupDto());
        request.setMembers(Set.of(TestUtils.mockInstance(UUID.randomUUID())));
        //when
        Executable executable = () -> restClient._updateUserGroupUsingPUT(id, request);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void getUserGroupInfo_fullyValued() {
        //given
        String groupId = testCase2igroupdMap.get(TestCase.FULLY_VALUED);
        //when
        it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource response = restClient._getUserGroupUsingGET(groupId).getBody();
        //then
        assertNotNull(response);
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getCreatedBy());
        assertNotNull(response.getDescription());
        assertNotNull(response.getId());
        assertNotNull(response.getMembers());
        assertNotNull(response.getName());
        assertNotNull(response.getInstitutionId());
        assertNotNull(response.getModifiedAt());
        assertNotNull(response.getModifiedBy());
        assertNotNull(response.getStatus());

    }

    @Test
    void getUserGroupInfo_fullyNull() {
        //given
        String groupId = testCase2igroupdMap.get(TestCase.FULLY_NULL);
        //when
        it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource response = restClient._getUserGroupUsingGET(groupId).getBody();
        //then
        assertNotNull(response);
        Assertions.assertNull(response.getCreatedAt());
        Assertions.assertNull(response.getCreatedBy());
        Assertions.assertNull(response.getDescription());
        Assertions.assertNull(response.getId());
        Assertions.assertNull(response.getMembers());
        Assertions.assertNull(response.getName());
        Assertions.assertNull(response.getInstitutionId());
        Assertions.assertNull(response.getModifiedAt());
        Assertions.assertNull(response.getModifiedBy());
        Assertions.assertNull(response.getStatus());
    }

    @Test
    void addMemberToUserGroup() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        //when
        Executable executable = () -> restClient._addMemberToUserGroupUsingPUT(groupId, memberId);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void deleteMemberFromUserGroup() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        //when
        Executable executable = () -> restClient._deleteMemberFromUserGroupUsingDELETE(groupId, memberId);
        //then
        assertDoesNotThrow(executable);
    }

    @Test
    void getUserGroups_fullyValued() {
        //given
        String institutionId = null;
        String productId = null;
        UUID userId = null;
        String status = String.join(",", UserGroupStatus.ACTIVE.name(), UserGroupStatus.SUSPENDED.name());
        List<String> sortParams = new ArrayList<>();
        // when
        ResponseEntity<PageOfUserGroupResource> response = restClient._getUserGroupsUsingGET(institutionId,3,2 , sortParams,productId, userId, status);
        //then
        assertNotNull(response);
        assertEquals(0, Objects.requireNonNull(response.getBody()).getNumber());
        assertEquals(20, response.getBody().getSize());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getTotalPages());
        assertNotNull(response.getBody().getContent());
        assertNotNull(response.getBody().getContent().get(0).getCreatedAt());
        assertNotNull(response.getBody().getContent().get(0).getCreatedBy());
        assertNotNull(response.getBody().getContent().get(0).getDescription());
        assertNotNull(response.getBody().getContent().get(0).getId());
        assertNotNull(response.getBody().getContent().get(0).getMembers());
        assertNotNull(response.getBody().getContent().get(0).getName());
        assertNotNull(response.getBody().getContent().get(0).getInstitutionId());
        assertNotNull(response.getBody().getContent().get(0).getModifiedAt());
        assertNotNull(response.getBody().getContent().get(0).getModifiedBy());
        assertNotNull(response.getBody().getContent().get(0).getStatus());
    }

    @Test
    void getUserGroups_fullyValuedPageable() {
        //given
        String institutionId = null;
        String productId = null;
        UUID userId = null;
        String status = String.join(",", UserGroupStatus.ACTIVE.name(), UserGroupStatus.SUSPENDED.name());
        List<String> sortParams = new ArrayList<>();
        // when
        ResponseEntity<PageOfUserGroupResource> response = restClient._getUserGroupsUsingGET(institutionId,3,2 , sortParams,productId, userId, status);
        //then
        assertNotNull(response);
        assertEquals(0, Objects.requireNonNull(response.getBody()).getNumber());
        assertEquals(20, response.getBody().getSize());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getTotalPages());
        assertNotNull(response.getBody().getContent());
        assertNotNull(response.getBody().getContent().get(0).getCreatedAt());
        assertNotNull(response.getBody().getContent().get(0).getCreatedBy());
        assertNotNull(response.getBody().getContent().get(0).getDescription());
        assertNotNull(response.getBody().getContent().get(0).getId());
        assertNotNull(response.getBody().getContent().get(0).getMembers());
        assertNotNull(response.getBody().getContent().get(0).getName());
        assertNotNull(response.getBody().getContent().get(0).getInstitutionId());
        assertNotNull(response.getBody().getContent().get(0).getModifiedAt());
        assertNotNull(response.getBody().getContent().get(0).getModifiedBy());
        assertNotNull(response.getBody().getContent().get(0).getStatus());
    }

    @Test
    void deleteMemberFromUserGroups() {
        //given
        String institutionId = "institutionId";
        String productId = "productId";
        UUID memberId = UUID.randomUUID();
        //when
        Executable executable = () -> restClient._deleteMemberFromUserGroupsUsingDELETE(memberId, institutionId, productId);
        //then
        assertDoesNotThrow(executable);
    }

}
