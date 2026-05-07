package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.dashboard.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.exception.InvalidMemberListException;
import it.pagopa.selfcare.dashboard.exception.InvalidUserGroupException;
import it.pagopa.selfcare.dashboard.model.groups.*;
import it.pagopa.selfcare.dashboard.model.mapper.GroupMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.PageOfUserGroupResource;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UpdateUserGroupDto;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDataResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDetailResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.dashboard.service.UserGroupV2ServiceImpl.REQUIRED_GROUP_ID_MESSAGE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGroupV2ServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private UserGroupV2ServiceImpl userGroupV2Service;

    @Mock
    private UserGroupRestClient userGroupRestClient;

    @Mock
    private UserInstitutionApiRestClient userInstitutionApiRestClient;

    @Mock
    private UserApiRestClient userApiRestClient;

    @Spy
    private GroupMapperImpl groupMapper;

    @Spy
    private UserMapperImpl userMapper;

    @BeforeEach
    void init() {
        super.setUp();
    }

    @Test
    void getUserGroupByIdNullGroupId() {
        assertThrows(IllegalArgumentException.class, () -> userGroupV2Service.getUserGroupById(null));
    }

    @Test
    void getUserGroups() throws IOException {

        String institutionId = "setInstitutionId";
        String productId = "ProductId";
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);

        UserGroupFilter userGroupFilter = new UserGroupFilter();
        userGroupFilter.setInstitutionId(Optional.of(institutionId));
        userGroupFilter.setProductId(Optional.of(productId));
        userGroupFilter.setUserId(Optional.of(userId));

        ClassPathResource pathResource = new ClassPathResource("expectations/UserGroup.json");
        byte[] resourceStream = Files.readAllBytes(pathResource.getFile().toPath());
        UserGroup userGroup = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        Page<UserGroup> mockPage = new PageImpl<>(List.of(userGroup, userGroup));
        PageOfUserGroupResource pageOfUserGroupResource = new PageOfUserGroupResource();
        pageOfUserGroupResource.setTotalElements(2L);
        pageOfUserGroupResource.setContent(Arrays.asList(mockInstance(new UserGroupResource()), mockInstance(new UserGroupResource())));
        when(userGroupRestClient._getUserGroupsUsingGET(any(), any(), any(), any(), any(), any(), anyString()))
                .thenReturn(ResponseEntity.ok(pageOfUserGroupResource));

        Page<UserGroup> result = userGroupV2Service.getUserGroups(institutionId, productId, userId, pageable);

        assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        assertEquals(mockPage.getContent().get(0).getInstitutionId(), result.getContent().get(0).getInstitutionId());
    }

    @Test
    void updateUserGroupSuccessfullyUpdatesGroup() {
        String groupId = "groupId";
        String userId = UUID.randomUUID().toString();
        UpdateUserGroup group = mockInstance(new UpdateUserGroup());
        UserGroupInfo userGroupInfo = mockInstance(new UserGroupInfo());
        group.setMembers(List.of(userId));
        UserInstitutionResponse userInstitutionResponse = mockInstance(new UserInstitutionResponse(), "setUserId");
        userInstitutionResponse.setUserId(userId);

        when(userGroupRestClient._getUserGroupUsingGET(groupId)).thenReturn(ResponseEntity.ok(mockInstance(new UserGroupResource())));
        when(groupMapper.toUserGroupInfo(any())).thenReturn(userGroupInfo);
        when(userInstitutionApiRestClient._retrieveUserInstitutions("setInstitutionId", null, List.of("setProductId"), null, List.of("ACTIVE","SUSPENDED"), null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(userInstitutionResponse)));
        when(groupMapper.toUpdateUserGroupDto(group)).thenReturn(mockInstance(new UpdateUserGroupDto()));

        assertDoesNotThrow(() -> userGroupV2Service.updateUserGroup(groupId, group));
        verify(userGroupRestClient, times(1))._updateUserGroupUsingPUT(eq(groupId), any(UpdateUserGroupDto.class));
    }

    @Test
    void updateUserGroupThrowsInvalidMemberListExceptionWhenMembersNotAllowed() {
        String groupId = "groupId";
        UpdateUserGroup group = mockInstance(new UpdateUserGroup());
        group.setMembers(List.of("userId1", "userId2"));
        UserGroupInfo userGroupInfo = mockInstance(new UserGroupInfo());

        when(userGroupRestClient._getUserGroupUsingGET(groupId)).thenReturn(ResponseEntity.ok(mockInstance(new UserGroupResource())));
        when(groupMapper.toUserGroupInfo(any())).thenReturn(userGroupInfo);
        when(userInstitutionApiRestClient._retrieveUserInstitutions("setInstitutionId", null, List.of("setProductId"), null, List.of("ACTIVE","SUSPENDED"), null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(mockInstance(new UserInstitutionResponse()))));

        InvalidMemberListException exception = assertThrows(InvalidMemberListException.class, () -> userGroupV2Service.updateUserGroup(groupId, group));
        assertEquals("Some members in the list aren't allowed for this institution", exception.getMessage());
    }

    @Test
    void getUserGroupsNullInstitutionId() {

        String productId = "ProductId";
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);

        UserGroupFilter userGroupFilter = new UserGroupFilter();
        userGroupFilter.setInstitutionId(Optional.empty());
        userGroupFilter.setProductId(Optional.of(productId));
        userGroupFilter.setUserId(Optional.of(userId));

        UserGroup mockGroup1 = mockInstance(new UserGroup());
        UserGroup mockGroup2 = mockInstance(new UserGroup());
        Page<UserGroup> mockPage = new PageImpl<>(Arrays.asList(mockGroup1, mockGroup2));
        PageOfUserGroupResource pageOfUserGroupResource = new PageOfUserGroupResource();
        pageOfUserGroupResource.setTotalElements(2L);
        pageOfUserGroupResource.setContent(Arrays.asList(mockInstance(new UserGroupResource()), mockInstance(new UserGroupResource())));
        when(userGroupRestClient._getUserGroupsUsingGET(any(), any(), any(), any(), any(), any(), anyString())).thenReturn(ResponseEntity.ok(pageOfUserGroupResource));
        Page<UserGroup> result = userGroupV2Service.getUserGroups(null, productId, userId, pageable);

        assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        assertEquals(mockPage.getContent().get(0).getInstitutionId(), result.getContent().get(0).getInstitutionId());
    }

    @Test
    void createGroup() {

        CreateUserGroup userGroup = mockInstance(new CreateUserGroup());
        String id1 = randomUUID().toString();
        List<String> userIds = List.of(id1);

        userGroup.setMembers(userIds);
        UserInfo userInfoMock1 = mockInstance(new UserInfo(), 1, "setId");
        userInfoMock1.setId(id1);

        UserInstitutionResponse userInstitutionResponse = mockInstance(new UserInstitutionResponse(), "setUserId");
        userInstitutionResponse.setUserId(id1);

        when(userInstitutionApiRestClient._retrieveUserInstitutions("setInstitutionId", null, List.of("setProductId"), null, List.of("ACTIVE","SUSPENDED"),null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(userInstitutionResponse)));
        when(userGroupRestClient._createGroupUsingPOST(any())).thenReturn(ResponseEntity.ok(mockInstance(new UserGroupResource())));
        String groupId = userGroupV2Service.createUserGroup(userGroup);

        assertEquals("setId", groupId);
    }

    @Test
    void createGroup_invalidList() {

        CreateUserGroup userGroup = mockInstance(new CreateUserGroup());
        String id1 = randomUUID().toString();
        List<String> userIds = List.of(id1);

        userGroup.setMembers(userIds);
        UserInfo userInfoMock1 = mockInstance(new UserInfo(), 1, "setId");
        userInfoMock1.setId(id1);
        when(userInstitutionApiRestClient._retrieveUserInstitutions("setInstitutionId", null, List.of("setProductId"), null, List.of("ACTIVE","SUSPENDED"),null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(mockInstance(new UserInstitutionResponse()))));

        Executable executable = () -> userGroupV2Service.createUserGroup(userGroup);

        InvalidMemberListException e = assertThrows(InvalidMemberListException.class, executable);
        assertEquals("Some members in the list aren't allowed for this institution", e.getMessage());
    }

    @Test
    void delete() {

        String groupId = "relationshipId";

        userGroupV2Service.delete(groupId);
    }

    @Test
    void delete_nullGroupId() {

        Executable executable = () -> userGroupV2Service.delete(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_GROUP_ID_MESSAGE, e.getMessage());
    }

    @Test
    void activate() {

        String groupId = "relationshipId";

        userGroupV2Service.activate(groupId);
    }

    @Test
    void activate_nullGroupId() {

        Executable executable = () -> userGroupV2Service.activate(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_GROUP_ID_MESSAGE, e.getMessage());
    }

    @Test
    void suspend() {

        String groupId = "relationshipId";

        userGroupV2Service.suspend(groupId);
    }

    @Test
    void suspend_nullGroupId() {

        Executable executable = () -> userGroupV2Service.suspend(null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_GROUP_ID_MESSAGE, e.getMessage());
    }



    @Test
    void testDeleteMembersByUserIdUserNotFound() {
        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), anyList(), anyString()))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(mockInstance(new UserInstitutionResponse(), "setInstitutionId"))));
        userGroupV2Service.deleteMembersByUserId(UUID.randomUUID().toString(), "institutionId", "prod-io");
        verifyNoInteractions(userGroupRestClient);
    }


    @Test
    void testDeleteMembersByUserIdUserFound() {
        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), anyList(), anyString()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        when(userGroupRestClient._deleteMemberFromUserGroupsUsingDELETE(any(), any(), any()))
                .thenReturn(ResponseEntity.ok().build());
        Assertions.assertDoesNotThrow(() -> userGroupV2Service.deleteMembersByUserId(UUID.randomUUID().toString(), "institutionId", "prod-io"));
    }


    @Test
    void addMemberToUserGroup_invalidMember() {

        String groupId = "groupId";
        String productId = "productId";
        UserGroupInfo foundGroup = mockInstance(new UserGroupInfo(), "setId", "setInstitutionId");
        foundGroup.setId(groupId);
        UUID userId = randomUUID();

        UserGroupResource userGroupResource = new UserGroupResource();
        userGroupResource.setProductId(productId);
        when(userGroupRestClient._getUserGroupUsingGET(anyString())).thenReturn(ResponseEntity.ok(userGroupResource));
        when(userInstitutionApiRestClient._retrieveUserInstitutions(null, null, List.of("productId"),null, List.of("ACTIVE","SUSPENDED"),null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(mockInstance(new UserInstitutionResponse(), "setInstitutionId"))));

        Executable executable = () -> userGroupV2Service.addMemberToUserGroup(groupId, userId);

        InvalidMemberListException e = assertThrows(InvalidMemberListException.class, executable);
        assertEquals("This user is not allowed for this group", e.getMessage());
    }

    @Test
    void addMemberToUserGroup_validMember() {

        String groupId = "groupId";
        String productId = "productId";
        UserGroupInfo foundGroup = mockInstance(new UserGroupInfo(), "setId", "setInstitutionId");
        foundGroup.setId(groupId);
        UUID userId = randomUUID();
        UserInstitutionResponse userInstitutionResponse = mockInstance(new UserInstitutionResponse(), "setUserId");
        userInstitutionResponse.setUserId(userId.toString());

        UserGroupResource userGroupResource = new UserGroupResource();
        userGroupResource.setProductId(productId);
        when(userGroupRestClient._getUserGroupUsingGET(anyString())).thenReturn(ResponseEntity.ok(userGroupResource));
        when(userInstitutionApiRestClient._retrieveUserInstitutions(null, null, List.of("productId"),null, List.of("ACTIVE","SUSPENDED"),null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(userInstitutionResponse)));

        Executable executable = () -> userGroupV2Service.addMemberToUserGroup(groupId, userId);

       assertDoesNotThrow(executable);
    }

    @Test
    void getUserGroupById() {
        UUID userId = randomUUID();
        String groupId = "groupId";
        String createdBy = "9c28efd4-a876-464a-bf2b-95a4bec60715";
        String modifiedBy = "61658143-1870-4238-a54c-5029e2a70d4f";
        UserGroupResource userGroupResource = mockInstance(new UserGroupResource());
        userGroupResource.setMembers(List.of(userId));
        userGroupResource.setCreatedBy(createdBy);
        userGroupResource.setModifiedBy(modifiedBy);
        UserGroupInfo userGroupInfo = mockInstance(new UserGroupInfo());

        UserDataResponse user = mockInstance(new UserDataResponse());
        user.setRole("MANAGER");
        user.setStatus("ACTIVE");

        UserDetailResponse userDetail = mockInstance(new UserDetailResponse());

        when(userGroupRestClient._getUserGroupUsingGET(groupId)).thenReturn(ResponseEntity.ok(userGroupResource));
        when(userApiRestClient._retrieveUsers(anyString(), anyString(), any(), any(),any(), any(), any())).thenReturn(ResponseEntity.ok(List.of(user)));
        when(userApiRestClient._getUserDetailsById(createdBy, "name,familyName", "setInstitutionId")).thenReturn(ResponseEntity.ok(userDetail));
        when(userApiRestClient._getUserDetailsById(modifiedBy, "name,familyName", "setInstitutionId")).thenReturn(ResponseEntity.ok(userDetail));

        UserGroupInfo groupInfo = userGroupV2Service.getUserGroupById(groupId);
        Assertions.assertEquals(userGroupInfo.getInstitutionId(), groupInfo.getInstitutionId());
        Assertions.assertEquals(userGroupInfo.getCreatedBy().getId(), groupInfo.getCreatedBy().getId());
        Assertions.assertEquals(userGroupInfo.getModifiedBy().getId(), groupInfo.getModifiedBy().getId());
    }

    @Test
    void getUserGroupByIdAndMemberId() {
        final String groupId = "groupId";
        final UUID memberUUID = UUID.randomUUID();
        final String memberId = memberUUID.toString();

        final UserGroupResource userGroupResource = new UserGroupResource();
        userGroupResource.setId(groupId);
        userGroupResource.setProductId("productId");
        userGroupResource.setStatus(UserGroupResource.StatusEnum.ACTIVE);
        userGroupResource.setInstitutionId("institutionId");
        userGroupResource.setName("groupName");
        userGroupResource.setDescription("groupDescription");
        userGroupResource.setMembers(List.of(memberUUID));
        userGroupResource.setCreatedBy("");

        final UserDataResponse userDataResponse = new UserDataResponse();
        userDataResponse.setId("userInstitutionId");
        userDataResponse.setUserId(memberId);
        userDataResponse.setStatus("ACTIVE");
        userDataResponse.setRole("MANAGER");
        userDataResponse.setInstitutionId("institutionId");

        when(userGroupRestClient._getUserGroupUsingGET(groupId)).thenReturn(ResponseEntity.ok(userGroupResource));
        when(userApiRestClient._retrieveUsers(anyString(), anyString(), any(), any(),any(), any(), any())).thenReturn(ResponseEntity.ok(List.of(userDataResponse)));

        Assertions.assertDoesNotThrow(() -> userGroupV2Service.getUserGroupById(groupId, memberId));
    }

    @Test
    void getUserGroupByIdAndNonExistentMemberId() {
        final String groupId = "groupId";
        final UUID memberUUID = UUID.randomUUID();
        final String memberId = memberUUID.toString();

        final UserGroupResource userGroupResource = new UserGroupResource();
        userGroupResource.setId(groupId);
        userGroupResource.setProductId("productId");
        userGroupResource.setStatus(UserGroupResource.StatusEnum.ACTIVE);
        userGroupResource.setInstitutionId("institutionId");
        userGroupResource.setName("groupName");
        userGroupResource.setDescription("groupDescription");
        userGroupResource.setMembers(List.of(UUID.fromString("81b90c5e-67e4-44bc-85d3-3556f409f11d")));

        final UserDataResponse userDataResponse = new UserDataResponse();
        userDataResponse.setId("userInstitutionId");
        userDataResponse.setUserId("81b90c5e-67e4-44bc-85d3-3556f409f11d");
        userDataResponse.setStatus("ACTIVE");
        userDataResponse.setRole("MANAGER");
        userDataResponse.setInstitutionId("institutionId");

        when(userGroupRestClient._getUserGroupUsingGET(groupId)).thenReturn(ResponseEntity.ok(userGroupResource));
        when(userApiRestClient._retrieveUsers(anyString(), anyString(), any(), any(),any(), any(), any())).thenReturn(ResponseEntity.ok(List.of(userDataResponse)));

        Assertions.assertThrows(InvalidUserGroupException.class, () -> userGroupV2Service.getUserGroupById(groupId, memberId));
    }

    @Test
    void deleteMemberFromUserGroup() {

        String groupId = "groupId";
        UUID userId = randomUUID();

        Executable executable = () -> userGroupV2Service.deleteMemberFromUserGroup(groupId, userId);

        assertDoesNotThrow(executable);
    }

    @Test
    void deleteMemberFromUserGroup_nullId() {

        UUID userId = randomUUID();

        Executable executable = () -> userGroupV2Service.deleteMemberFromUserGroup(null, userId);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(REQUIRED_GROUP_ID_MESSAGE, e.getMessage());
    }

    @Test
    void deleteMemberFromUserGroup_nullUserId() {

        String groupId = "groupId";

        Executable executable = () -> userGroupV2Service.deleteMemberFromUserGroup(groupId, null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A userId is required", e.getMessage());
    }

    @Test
    void testDeleteMembersByUserIdUserNotFound1() {
        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), anyList(), anyString()))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));
        userGroupV2Service.deleteMembersByUserId(UUID.randomUUID().toString(), "institutionId", "prod-io");
        Assertions.assertDoesNotThrow(() -> userGroupV2Service.deleteMembersByUserId(UUID.randomUUID().toString(), "institutionId", "prod-io"));
    }


    @Test
    void testDeleteMembersByUserIdUserFound2() {
        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), anyList(), anyString()))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(mockInstance(new UserInstitutionResponse(), "setInstitutionId"))));
        Assertions.assertDoesNotThrow(() -> userGroupV2Service.deleteMembersByUserId(UUID.randomUUID().toString(), "institutionId", "prod-io"));
        verifyNoInteractions(userGroupRestClient);
    }
}
