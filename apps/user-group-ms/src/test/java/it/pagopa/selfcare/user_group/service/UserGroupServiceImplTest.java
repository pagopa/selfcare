package it.pagopa.selfcare.user_group.service;

import com.mongodb.client.result.UpdateResult;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.user_group.api.UserGroupOperations;
import it.pagopa.selfcare.user_group.config.CoreTestConfig;
import it.pagopa.selfcare.user_group.dao.UserGroupRepository;
import it.pagopa.selfcare.user_group.exception.ResourceAlreadyExistsException;
import it.pagopa.selfcare.user_group.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user_group.exception.ResourceUpdateException;
import it.pagopa.selfcare.user_group.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ValidationException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserGroupServiceImpl.class, CoreTestConfig.class, UserGroupRepository.class, MongoTemplate.class})
@TestPropertySource(properties = {
        "ALLOWED_SORTING_PARAMETERS=name"
})
class UserGroupServiceImplTest {

    @BeforeEach
    void beforeEach() {
        TestSecurityContextHolder.clearContext();
    }

    @MockBean
    private UserGroupRepository userGroupRepository;

    @MockBean
    private AuditorAware<String> auditorAware;

    @MockBean
    private MappingMongoConverter mappingMongoConverter;

    @MockBean
    private MongoTemplate mongoTemplateMock;

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
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
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
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
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
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
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

        when(userGroupRepository.insert(any(UserGroupEntity.class))).thenReturn(new UserGroupEntity());
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(Collections.emptyList());

        //when
        UserGroupOperations output = groupService.createGroup(input);
        //then
        assertNotNull(output);
        verify(userGroupRepository).insert(any(UserGroupEntity.class));
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
    }

    @Test
    void createGroup_duplicateKey() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("userId")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupOperations input = TestUtils.mockInstance(new DummyGroup(), "setId", "setCreateAt", "setModifiedAt");

        when(userGroupRepository.insert(any(UserGroupEntity.class))).thenThrow(new DuplicateKeyException("Duplicate key"));
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(Collections.emptyList());

        //when
        Executable executable = () -> groupService.createGroup(input);

        //then
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class, executable);
        assertEquals("Failed _id or unique index constraint.", exception.getMessage());
        verify(userGroupRepository).insert(any(UserGroupEntity.class));
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
    }

    @Test
    void addMember_nullId() {
        //given
        UUID memberId = UUID.randomUUID();
        //when
        Executable executable = () -> groupService.addMember(null, memberId);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void addMember_nullMemberId() {
        //given
        String groupId = "groupId";
        //when
        Executable executable = () -> groupService.addMember(groupId, null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void addMember_groupNotFound() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        //when
        Executable executable = () -> groupService.addMember(groupId, memberId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void addMember_groupSuspended() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.SUSPENDED);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.addMember(groupId, memberId);
        //then
        ResourceUpdateException exception = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void addMember_success() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.addMember(groupId, memberId);
        //then
        verify(userGroupRepository).findById(groupId);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void addMember_insertMemberFails() {
        //given
        String groupId = "groupId";
        UUID memberId = UUID.randomUUID();
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        Executable executable = () -> groupService.addMember(groupId, memberId);
        //then
        ResourceUpdateException exception = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Couldn't update resource", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_nullGroupId() {
        //given
        String memberId = "memberId";
        //when
        Executable executable = () -> groupService.deleteMember(null, memberId);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_nullMemberId() {
        //given
        String groupId = "groupId";
        //when
        Executable executable = () -> groupService.deleteMember(groupId, null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_groupNotFound() {
        //given
        String groupId = "groupId";
        String memberId = "memberId";
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        //when
        Executable executable = () -> groupService.deleteMember(groupId, memberId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_groupSuspended() {
        //given
        String groupId = "groupId";
        String memberId = "memberId";
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.SUSPENDED);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        //when
        Executable executable = () -> groupService.deleteMember(groupId, memberId);
        //then
        ResourceUpdateException exception = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_success() {
        //given
        String groupId = "groupId";
        String memberId = "memberId";
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.deleteMember(groupId, memberId);
        //then
        verify(userGroupRepository).findById(groupId);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMember_updateFails() {
        //given
        String groupId = "groupId";
        String memberId = "memberId";
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(group.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        Executable executable = () -> groupService.deleteMember(groupId, memberId);
        //then
        ResourceUpdateException exception = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Couldn't update resource", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMembers_nullMemberId() {
        //given
        String institutionId = "institutionId";
        String productId = "productId";
        //when
        Executable executable = () -> groupService.deleteMembers(null, institutionId, productId);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A member id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMembers_nullInstitutionId() {
        //given
        String memberId = "memberId";
        String productId = "productId";
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, null, productId);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A institution id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMembers_nullProductId() {
        //given
        String memberId = "memberId";
        String institutionId = "institutionId";
        //when
        Executable executable = () -> groupService.deleteMembers(memberId, institutionId, null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A product id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMembers_success() {
        //given
        String memberId = "memberId";
        String institutionId = "institutionId";
        String productId = "productId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateMulti(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.deleteMembers(memberId, institutionId, productId);
        //then
        verify(mongoTemplateMock).updateMulti(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteMembers_noModification() {
        //given
        String memberId = "memberId";
        String institutionId = "institutionId";
        String productId = "productId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateMulti(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.deleteMembers(memberId, institutionId, productId);
        //then
        verify(mongoTemplateMock).updateMulti(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroup_nullId() {
        //when
        Executable executable = () -> groupService.getUserGroup(null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroup_emptyId() {
        //when
        Executable executable = () -> groupService.getUserGroup("");
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroup_groupNotFound() {
        //given
        String groupId = "groupId";
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        //when
        Executable executable = () -> groupService.getUserGroup(groupId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroup_success() {
        //given
        String groupId = "groupId";
        UserGroupEntity group = mock(UserGroupEntity.class);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        //when
        UserGroupOperations result = groupService.getUserGroup(groupId);
        //then
        assertNotNull(result);
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroups_invalidSortParameter() {
        //given
        UserGroupFilter filter = new UserGroupFilter();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidParam"));
        //when
        Executable executable = () -> groupService.getUserGroups(filter, pageable);
        //then
        ValidationException exception = assertThrows(ValidationException.class, executable);
        assertEquals("Given sort parameters aren't valid", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroups_validSortParameter() {
        //given
        UserGroupFilter filter = new UserGroupFilter();
        filter.setProductId("productId"); // Aggiungi un productId per soddisfare la condizione
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        List<UserGroupEntity> userGroups = Collections.singletonList(mock(UserGroupEntity.class));
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(userGroups);
        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn((long) userGroups.size());
        //when
        Page<UserGroupOperations> result = groupService.getUserGroups(filter, pageable);
        //then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
        verify(mongoTemplateMock).count(any(Query.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroups_withAllFilters() {
        //given
        UserGroupFilter filter = new UserGroupFilter();
        filter.setProductId("productId");
        filter.setInstitutionId("institutionId");
        filter.setParentInstitutionId("parentInstitutionId");
        filter.setUserId("userId");
        filter.setStatus(List.of(UserGroupStatus.ACTIVE));
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        List<UserGroupEntity> userGroups = Collections.singletonList(mock(UserGroupEntity.class));
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(userGroups);
        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn((long) userGroups.size());
        //when
        Page<UserGroupOperations> result = groupService.getUserGroups(filter, pageable);
        //then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
        verify(mongoTemplateMock).count(any(Query.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroups_sortingNotAllowedWithoutProductIdOrInstitutionId() {
        //given
        UserGroupFilter filter = new UserGroupFilter();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        //when
        Executable executable = () -> groupService.getUserGroups(filter, pageable);
        //then
        ValidationException exception = assertThrows(ValidationException.class, executable);
        assertEquals("Sorting not allowed without productId or institutionId", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void getUserGroups_statusFilterWithoutRequiredFields() {
        //given
        UserGroupFilter filter = new UserGroupFilter();
        filter.setStatus(Collections.singletonList(UserGroupStatus.ACTIVE));
        Pageable pageable = PageRequest.of(0, 10);
        //when
        Executable executable = () -> groupService.getUserGroups(filter, pageable);
        //then
        ValidationException exception = assertThrows(ValidationException.class, executable);
        assertEquals("At least one of productId, institutionId and userId must be provided with status filter", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteGroup_nullId() {
        //when
        Executable executable = () -> groupService.deleteGroup(null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteGroup_emptyId() {
        //when
        Executable executable = () -> groupService.deleteGroup("");
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteGroup_success() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.deleteGroup(groupId);
        //then
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void suspendGroup_nullId() {
        //when
        Executable executable = () -> groupService.suspendGroup(null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void suspendGroup_emptyId() {
        //when
        Executable executable = () -> groupService.suspendGroup("");
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void suspendGroup_success() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.suspendGroup(groupId);
        //then
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void activateGroup_nullId() {
        //when
        Executable executable = () -> groupService.activateGroup(null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void activateGroup_emptyId() {
        //when
        Executable executable = () -> groupService.activateGroup("");
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void activateGroup_success() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        groupService.activateGroup(groupId);
        //then
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void activateGroup_groupNotFound() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        Executable executable = () -> groupService.activateGroup(groupId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void deleteGroup_groupNotFound() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        Executable executable = () -> groupService.deleteGroup(groupId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void suspendGroup_groupNotFound() {
        //given
        String groupId = "groupId";
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getMatchedCount()).thenReturn(0L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);
        //when
        Executable executable = () -> groupService.suspendGroup(groupId);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(mongoTemplateMock).updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class));
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_nullId() {
        //given
        UserGroupOperations group = mock(UserGroupOperations.class);
        //when
        Executable executable = () -> groupService.updateGroup(null, group);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_emptyId() {
        //given
        UserGroupOperations group = mock(UserGroupOperations.class);
        //when
        Executable executable = () -> groupService.updateGroup("", group);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group id is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_nullGroup() {
        //given
        String groupId = "groupId";
        //when
        Executable executable = () -> groupService.updateGroup(groupId, null);
        //then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A user group is required", exception.getMessage());
        verifyNoInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_groupNotFound() {
        //given
        String groupId = "groupId";
        UserGroupOperations group = mock(UserGroupOperations.class);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.empty());
        //when
        Executable executable = () -> groupService.updateGroup(groupId, group);
        //then
        assertThrows(ResourceNotFoundException.class, executable);
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_groupSuspended() {
        //given
        String groupId = "groupId";
        UserGroupOperations group = mock(UserGroupOperations.class);
        UserGroupEntity foundGroup = mock(UserGroupEntity.class);
        when(foundGroup.getStatus()).thenReturn(UserGroupStatus.SUSPENDED);
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(foundGroup));
        //when
        Executable executable = () -> groupService.updateGroup(groupId, group);
        //then
        ResourceUpdateException exception = assertThrows(ResourceUpdateException.class, executable);
        assertEquals("Trying to modify suspended group", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verifyNoMoreInteractions(mongoTemplateMock, userGroupRepository);
    }

    @Test
    void updateGroup_success() {
        //given
        String groupId = "groupId";
        UserGroupOperations group = mock(UserGroupOperations.class);
        UserGroupEntity foundGroup = mock(UserGroupEntity.class);
        when(foundGroup.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(group.getName()).thenReturn("groupName");
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(foundGroup));
        when(userGroupRepository.save(any(UserGroupEntity.class))).thenReturn(foundGroup);
        //when
        UserGroupOperations result = groupService.updateGroup(groupId, group);
        //then
        assertNotNull(result);
        verify(userGroupRepository).findById(groupId);
        verify(userGroupRepository).save(any(UserGroupEntity.class));
    }

    @Test
    void updateGroup_duplicateKey() {
        //given
        String groupId = "groupId";
        UserGroupOperations group = mock(UserGroupOperations.class);
        UserGroupEntity foundGroup = mock(UserGroupEntity.class);
        when(foundGroup.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(group.getName()).thenReturn("Group Name");
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(foundGroup));
        when(userGroupRepository.save(any(UserGroupEntity.class))).thenThrow(new DuplicateKeyException("Duplicate key"));
        //when
        Executable executable = () -> groupService.updateGroup(groupId, group);
        //then
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class, executable);
        assertEquals("Failed _id or unique index constraint.", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verify(userGroupRepository).save(any(UserGroupEntity.class));
    }

    @Test
    void createGroup_duplicateName() {
        //given
        SelfCareUser selfCareUser = SelfCareUser.builder("userId")
                .email("test@example.com")
                .name("name")
                .surname("surname")
                .build();
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken(selfCareUser, null);
        TestSecurityContextHolder.setAuthentication(authenticationToken);
        UserGroupOperations input = TestUtils.mockInstance(new DummyGroup(), "setId", "setCreateAt", "setModifiedAt");
        input.setName("duplicateName");
        input.setProductId("productId");
        input.setInstitutionId("institutionId");

        UserGroupEntity existingGroup = new UserGroupEntity();
        existingGroup.setId("existingGroupId");
        existingGroup.setName("duplicateName");
        existingGroup.setProductId("productId");
        existingGroup.setInstitutionId("institutionId");
        existingGroup.setStatus(UserGroupStatus.ACTIVE);

        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(Collections.singletonList(existingGroup));
        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn(1L);

        //when
        Executable executable = () -> groupService.createGroup(input);

        //then
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class, executable);
        assertEquals("A group with the same name already exists in ACTIVE or SUSPENDED state", exception.getMessage());
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
    }

    @Test
    void updateGroup_duplicateName() {
        //given
        String groupId = "groupId";
        UserGroupOperations group = mock(UserGroupOperations.class);
        when(group.getName()).thenReturn("duplicateName");
        UserGroupEntity foundGroup = mock(UserGroupEntity.class);
        when(foundGroup.getStatus()).thenReturn(UserGroupStatus.ACTIVE);
        when(foundGroup.getProductId()).thenReturn("productId");
        when(foundGroup.getInstitutionId()).thenReturn("institutionId");
        when(userGroupRepository.findById(groupId)).thenReturn(Optional.of(foundGroup));

        UserGroupEntity existingGroup = new UserGroupEntity();
        existingGroup.setId("existingGroupId");
        existingGroup.setName("duplicateName");
        existingGroup.setProductId("productId");
        existingGroup.setInstitutionId("institutionId");
        existingGroup.setStatus(UserGroupStatus.ACTIVE);

        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class))).thenReturn(Collections.singletonList(existingGroup));
        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn(1L);

        //when
        Executable executable = () -> groupService.updateGroup(groupId, group);

        //then
        ResourceAlreadyExistsException exception = assertThrows(ResourceAlreadyExistsException.class, executable);
        assertEquals("A group with the same name already exists in ACTIVE or SUSPENDED state", exception.getMessage());
        verify(userGroupRepository).findById(groupId);
        verify(mongoTemplateMock).find(any(Query.class), eq(UserGroupEntity.class));
        verify(group).getName();
    }

    @Test
    void addMembers_ok() {
        String institutionId = "inst1";
        String parentInstitutionId = "parent1";
        String productId = "prod1";
        String name = "groupName";
        String description = "groupDescription";
        String member = UUID.randomUUID().toString();
        Set<String> members = Set.of(member);

        GroupDto userGroup = new GroupDto();
        userGroup.setInstitutionId(institutionId);
        userGroup.setParentInstitutionId(parentInstitutionId);
        userGroup.setMembers(members);
        userGroup.setProductId(productId);
        userGroup.setName(name);
        userGroup.setDescription(description);

        UserGroupEntity entity = new UserGroupEntity();
        entity.setId("group123");
        entity.setParentInstitutionId(parentInstitutionId);

        groupService.createGroupOrAddMembers(userGroup);

        verify(mongoTemplateMock).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(UserGroupEntity.class));
    }

    @Test
    void addMembers_nullInstitutionId_shouldThrow() {
        Set<String> members = Set.of(UUID.randomUUID().toString());
        GroupDto userGroup = new GroupDto();
        userGroup.setParentInstitutionId("parentInstitutionId");
        userGroup.setMembers(members);
        userGroup.setProductId("productId");
        userGroup.setName("name");
        userGroup.setDescription("description");

        assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroupOrAddMembers(userGroup));
    }

    @Test
    void addMembers_nullParentInstitutionId_shouldThrow() {
        Set<String> members = Set.of(UUID.randomUUID().toString());
        GroupDto userGroup = new GroupDto();
        userGroup.setInstitutionId("institutionId");
        userGroup.setMembers(members);
        userGroup.setProductId("productId");
        userGroup.setName("name");
        userGroup.setDescription("description");

        assertThrows(IllegalArgumentException.class,
                () -> groupService.createGroupOrAddMembers(userGroup));
    }

    @Test
    void deleteMembersWithParentInstitutionId_ok() {
        String institutionId = "inst1";
        String parentInstitutionId = "parent1";
        String productId = "prod1";
        UUID member = UUID.randomUUID();
        Set<UUID> members = Set.of(member);

        UserGroupEntity entity = new UserGroupEntity();
        entity.setId("group123");

        UpdateResult updateResult = mock(UpdateResult.class);

        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplateMock.updateFirst(any(Query.class), any(Update.class), eq(UserGroupEntity.class))).thenReturn(updateResult);

        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn(1L);
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class)))
                .thenReturn(List.of(entity));
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));

        groupService.deleteMembersWithParentInstitutionId(institutionId, parentInstitutionId, productId, members);

        verify(mongoTemplateMock).updateFirst(
                any(),
                any(Update.class),
                eq(UserGroupEntity.class)
        );
    }

    @Test
    void deleteMembersWithParentInstitutionId_nullInstitutionId_shouldThrow() {
        Set<UUID> members = Set.of(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class,
                () -> groupService.deleteMembersWithParentInstitutionId(null, "parent", "prod", members));
    }

    @Test
    void deleteMembersWithParentInstitutionId_nullParentInstitutionId_shouldThrow() {
        Set<UUID> members = Set.of(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class,
                () -> groupService.deleteMembersWithParentInstitutionId("inst", null, "prod", members));
    }

    @Test
    void deleteMembersWithParentInstitutionId_noGroupFound_shouldThrowResourceNotFound() {
        Set<UUID> members = Set.of(UUID.randomUUID());

        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn(0L);
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class)))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> groupService.deleteMembersWithParentInstitutionId("inst", "parent", "prod", members));
    }

    @Test
    void deleteMembersWithParentInstitutionId_multipleGroupsFound_shouldThrowIllegalState() {
        Set<UUID> members = Set.of(UUID.randomUUID());

        UserGroupEntity e1 = new UserGroupEntity();
        e1.setId("id1");
        UserGroupEntity e2 = new UserGroupEntity();
        e2.setId("id2");

        when(mongoTemplateMock.count(any(Query.class), eq(UserGroupEntity.class))).thenReturn(2L);
        when(mongoTemplateMock.find(any(Query.class), eq(UserGroupEntity.class)))
                .thenReturn(List.of(e1, e2));

        assertThrows(IllegalStateException.class,
                () -> groupService.deleteMembersWithParentInstitutionId("inst", "parent", "prod", members));
    }

}