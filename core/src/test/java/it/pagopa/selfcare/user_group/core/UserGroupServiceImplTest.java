package it.pagopa.selfcare.user_group.core;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.user_group.connector.DummyGroup;
import it.pagopa.selfcare.user_group.connector.api.UserGroupConnector;
import it.pagopa.selfcare.user_group.connector.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.connector.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.connector.exception.ResourceUpdateException;
import it.pagopa.selfcare.user_group.connector.model.UserGroupFilter;
import it.pagopa.selfcare.user_group.connector.model.UserGroupStatus;
import it.pagopa.selfcare.user_group.core.config.CoreTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ValidationException;
import java.util.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.data.support.PageableExecutionUtils.getPage;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserGroupServiceImpl.class, CoreTestConfig.class})
@TestPropertySource(properties = {
        "ALLOWED_SORTING_PARAMETERS=name"
})
class UserGroupServiceImplTest {

    @BeforeEach
    void beforeEach() {
        TestSecurityContextHolder.clearContext();
    }

    @MockBean
    private UserGroupConnector groupConnectorMock;

    @Autowired
    private UserGroupServiceImpl groupService;

    @Captor
    private ArgumentCaptor<UserGroupFilter> filter;

    @Test
    void createGroup_nullAuth() {
        //given
        UserGroupOperations input = null;
        //when
        Executable executable = () -> groupService.createGroup(input);
        //then
        IllegalStateException e = assertThrows(IllegalStateException.class, executable);
        assertEquals("Authentication is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void createGroup_nullPrincipal() {
        //given
        UserGroupOperations input = null;
        Authentication authentication = new TestingAuthenticationToken(null, null);
        TestSecurityContextHolder.setAuthentication(authentication);
        //when
        Executable executable = () -> groupService.createGroup(input);
        //then
        IllegalStateException illegalStateException = Assertions.assertThrows(IllegalStateException.class, executable);
        Assertions.assertEquals("Not SelfCareUser principal", illegalStateException.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void createGroup_nullGroup() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("id")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupOperations input = null;
        //when
        Executable executable = () -> groupService.createGroup(input);
        //then
        IllegalArgumentException illegalArgumentException = Assertions.assertThrows(IllegalArgumentException.class, executable);
        Assertions.assertEquals("A group is required", illegalArgumentException.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void createGroup_ok() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("userId")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupOperations input = TestUtils.mockInstance(new DummyGroup(), "setId", "setCreateAt", "setModifiedAt");
        Page<UserGroupOperations> existingGroups = getPage(Collections.emptyList(), Pageable.unpaged(), () -> 0L);
        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(existingGroups);
        when(groupConnectorMock.insert(any(UserGroupOperations.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, UserGroupOperations.class));
        //when
        UserGroupOperations output = groupService.createGroup(input);
        //then
        assertNotNull(output);

        verify(groupConnectorMock, times(1))
                .insert(any(UserGroupOperations.class));
        verify(groupConnectorMock, times(1))
                .findAll(filter.capture(), any());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void createGroup_conflict() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("userId")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupOperations input = TestUtils.mockInstance(new DummyGroup(), "setId", "setCreateAt", "setModifiedAt");
        input.setName("existingGroupName");

        UserGroupOperations existingGroup = TestUtils.mockInstance(new DummyGroup(), "setCreateAt", "setModifiedAt");
        existingGroup.setName("existingGroupName");
        Page<UserGroupOperations> existingGroups = getPage(Collections.singletonList(existingGroup), Pageable.unpaged(), () -> 1L);

        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(existingGroups);

        //when
        Executable executable = () -> groupService.createGroup(input);

        //then
        assertThrows(ResourceAlreadyExistsException.class, executable);

        verify(groupConnectorMock, times(1))
                .findAll(filter.capture(), any());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void deleteGroup() {
        //given
        String id = "id";
        //when
        groupService.deleteGroup(id);
        //then
        verify(groupConnectorMock, times(1))
                .deleteById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }


    @Test
    void deleteGroup_nullId() {
        //given
        String id = null;
        //when
        Executable executable = () -> groupService.deleteGroup(id);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void suspendGroup() {
        //given
        String id = "id";
        //when
        groupService.suspendGroup(id);
        //then
        verify(groupConnectorMock, times(1))
                .suspendById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }


    @Test
    void suspendGroup_nullId() {
        //given
        String id = null;
        //when
        Executable executable = () -> groupService.suspendGroup(id);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void activateGroup() {
        //given
        String id = "id";
        //when
        groupService.activateGroup(id);
        //then
        verify(groupConnectorMock, times(1))
                .activateById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void activateGroup_nullId() {
        //given
        String id = null;
        //when
        Executable executable = () -> groupService.activateGroup(id);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_nullId() {
        //given
        String id = null;
        UserGroupOperations input = new DummyGroup();
        //when
        Executable executable = () -> groupService.updateGroup(id, input);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_nullGroup() {
        //given
        String id = "id";
        UserGroupOperations input = null;
        //when
        Executable executable = () -> groupService.updateGroup(id, input);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_foundGroupSuspended() {
        //given
        String id = "id";
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup());
        group.setStatus(UserGroupStatus.SUSPENDED);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.updateGroup(id, group);
        //then
        ResourceUpdateException e = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", e.getMessage());
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_notExists() {
        //given
        String id = "id";
        UserGroupOperations input = new DummyGroup();
        //when
        Executable executable = () -> groupService.updateGroup(id, input);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_exists_ok() {
        //given
        String id = "id";
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup(), "setId");
        UserGroupOperations foundGroup = TestUtils.mockInstance(new DummyGroup());
        Page<UserGroupOperations> existingGroups = getPage(Collections.emptyList(), Pageable.unpaged(), () -> 0L);

        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(existingGroups);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(foundGroup));
        when(groupConnectorMock.save(any()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, UserGroupOperations.class));
        //when
        UserGroupOperations saved = groupService.updateGroup(id, group);
        //then
        assertEquals(saved.getDescription(), group.getDescription());
        assertEquals(saved.getMembers(), group.getMembers());
        assertEquals(saved.getName(), group.getName());
        verify(groupConnectorMock, times(1))
                .findById(id);
        verify(groupConnectorMock, times(1))
                .findAll(any(), any());
        verify(groupConnectorMock, times(1))
                .save(any());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_exists_notChangingName() {
        //given
        String id = "id";
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup(), "setId");
        group.setName("existingGroupName");

        UserGroupOperations foundGroup = TestUtils.mockInstance(new DummyGroup());
        foundGroup.setId(id);
        foundGroup.setName("existingGroupName");
        foundGroup.setStatus(UserGroupStatus.ACTIVE);

        // existing group find the same group without changing the name
        Page<UserGroupOperations> existingGroups = getPage(Collections.singletonList(foundGroup), Pageable.unpaged(), () -> 1L);

        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(existingGroups);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(foundGroup));
        when(groupConnectorMock.save(any()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, UserGroupOperations.class));

        //when
        UserGroupOperations saved = groupService.updateGroup(id, group);
        //then
        assertEquals(saved.getDescription(), group.getDescription());
        assertEquals(saved.getMembers(), group.getMembers());
        assertEquals(saved.getName(), group.getName());

        verify(groupConnectorMock, times(1))
                .findById(id);
        verify(groupConnectorMock, times(1))
                .findAll(any(), any());
        verify(groupConnectorMock, times(1))
                .save(any());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void updateGroup_exists_conflict() {
        //given
        String id = "id";
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup(), "setId");
        group.setName("existingGroupName");
        UserGroupOperations foundGroup = TestUtils.mockInstance(new DummyGroup());
        foundGroup.setStatus(UserGroupStatus.ACTIVE);

        UserGroupOperations existingGroup = TestUtils.mockInstance(new DummyGroup(), "setId");
        existingGroup.setName("existingGroupName");
        existingGroup.setId("differentId");
        Page<UserGroupOperations> existingGroups = getPage(Collections.singletonList(existingGroup), Pageable.unpaged(), () -> 1L);

        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(existingGroups);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(foundGroup));
        when(groupConnectorMock.save(any()))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, UserGroupOperations.class));

        //when
        Executable executable = () -> groupService.updateGroup(id, group);

        //then
        assertThrows(ResourceAlreadyExistsException.class, executable);

        verify(groupConnectorMock, times(1))
                .findById(id);
        verify(groupConnectorMock, times(1))
                .findAll(any(), any());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void addMember_nullId() {
        //given
        String id = null;
        UUID memberId = randomUUID();
        //when
        Executable executable = () -> groupService.addMember(id, memberId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void addMember_nullMemberId() {
        //given
        String id = "id";
        UUID memberId = null;
        //when
        Executable executable = () -> groupService.addMember(id, memberId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void addMember_doesNotExist() {
        //given
        String id = "id";
        UUID memberId = randomUUID();
        //when
        Executable executable = () -> groupService.addMember(id, memberId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);

    }

    @Test
    void addMember_groupSuspended() {
        //given
        String id = "id";
        UUID memberId = randomUUID();
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup());
        group.setStatus(UserGroupStatus.SUSPENDED);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.addMember(id, memberId);
        //then
        ResourceUpdateException e = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", e.getMessage());
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void addMember() {
        //given
        String id = "id";
        UUID memberUUID = randomUUID();
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup());
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(group));
        //when
        groupService.addMember(id, memberUUID);
        //then
        verify(groupConnectorMock, times(1))
                .findById(id);
        verify(groupConnectorMock, times(1))
                .insertMember(Mockito.anyString(), Mockito.anyString());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void deleteMember_nullId() {
        //given
        String id = null;
        String memberId = randomUUID().toString();
        //when
        Executable executable = () -> groupService.deleteMember(id, memberId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void deleteMember_nullMemberId() {
        //given
        String id = "id";
        String memberId = null;
        //when
        Executable executable = () -> groupService.deleteMember(id, memberId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void deleteMember_doesNotExist() {
        //given
        String id = "id";
        String memberId = randomUUID().toString();
        //when
        Executable executable = () -> groupService.deleteMember(id, memberId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);

    }

    @Test
    void deleteMember_groupSuspended() {
        //given
        String id = "id";
        String memberId = randomUUID().toString();
        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup());
        group.setStatus(UserGroupStatus.SUSPENDED);
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.deleteMember(id, memberId);
        //then
        ResourceUpdateException e = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", e.getMessage());
        verify(groupConnectorMock, times(1))
                .findById(id);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void deleteMember() {
        //given
        String id = "id";
        String memberId = randomUUID().toString();

        UserGroupOperations group = TestUtils.mockInstance(new DummyGroup());
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.deleteMember(id, memberId);
        //then
        assertDoesNotThrow(executable);
        verify(groupConnectorMock, times(1))
                .findById(id);
        verify(groupConnectorMock, times(1))
                .deleteMember(Mockito.anyString(), Mockito.anyString());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void deleteMembers_nullMemberId() {
        //given
        String memberId = null;
        String institutionId = "institutionId";
        String productId = "productId";
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, institutionId, productId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void deleteMembers_nullInstitutionId() {
        //given
        String memberId = "memberid";
        String institutionId = null;
        String productId = "productId";
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, institutionId, productId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A institution id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void deleteMembers_nullProductId() {
        //given
        String memberId = "memberId";
        String institutionId = "institutionId";
        String productId = null;
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, institutionId, productId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A product id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void deleteMembers() {
        //given
        String memberId = "memberId";
        String institutionId = "institutionId";
        String productId = "productId";
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, institutionId, productId);
        //then
        assertDoesNotThrow(executable);
        verify(groupConnectorMock, times(1))
                .deleteMembers(memberId, institutionId, productId);
        verifyNoMoreInteractions(groupConnectorMock);
    }


    @Test
    void getGroup() {
        //given
        String groupId = "groupId";
        when(groupConnectorMock.findById(Mockito.anyString()))
                .thenAnswer(invocation -> Optional.of(new DummyGroup()));
        //when
        UserGroupOperations group = groupService.getUserGroup(groupId);
        //then
        assertNotNull(group);
        verify(groupConnectorMock, times(1))
                .findById(groupId);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void getGroup_null() {
        //given
        String groupId = "groupId";
        //when
        Executable executable = () -> groupService.getUserGroup(groupId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(groupConnectorMock, times(1))
                .findById(groupId);
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void getGroup_nullId() {
        //given
        String groupId = null;
        //when
        Executable executable = () -> groupService.getUserGroup(groupId);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

    @Test
    void getUserGroups() {
        //given
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = randomUUID().toString();
        List<UserGroupStatus> allowedStatus = List.of(UserGroupStatus.ACTIVE);
        UserGroupFilter filterMock = new UserGroupFilter(institutionId, productId, userId, allowedStatus);
        Pageable pageable = PageRequest.of(0, 3, Sort.by("name"));
        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(getPage(List.of(new DummyGroup()), pageable, () -> pageable.isPaged()
                        ? (long) pageable.getPageSize() * pageable.getPageNumber() + 1
                        : 1));
        //when
        Page<UserGroupOperations> page = groupService.getUserGroups(filterMock, pageable);
        //then
        assertEquals(1, page.getContent().size());
        verify(groupConnectorMock, times(1))
                .findAll(filter.capture(), any());
        UserGroupFilter capturedFilter = filter.getValue();
        assertEquals(capturedFilter.getInstitutionId(), filterMock.getInstitutionId());
        assertEquals(capturedFilter.getProductId(), filterMock.getProductId());
        assertEquals(capturedFilter.getUserId(), filterMock.getUserId());
        assertEquals(capturedFilter.getStatus(), filterMock.getStatus());
        verifyNoMoreInteractions(groupConnectorMock);
    }

    @Test
    void getUserGroups_invalidSortParams() {
        //given
        Pageable pageable = PageRequest.of(0, 3, Sort.by("description"));
        when(groupConnectorMock.findAll(any(), any()))
                .thenReturn(getPage(Collections.singletonList(new DummyGroup()), pageable, () -> pageable.isPaged()
                        ? (long) pageable.getPageSize() * pageable.getPageNumber() + 1
                        : 1));
        //when
        Executable executable = () -> groupService.getUserGroups(null, pageable);
        //then
        ValidationException e = assertThrows(ValidationException.class, executable);
        assertEquals("Given sort parameters aren't valid", e.getMessage());
        verifyNoInteractions(groupConnectorMock);
    }

}