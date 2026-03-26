package it.pagopa.selfcare.onboarding.connector;

import feign.FeignException;
import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.onboarding.connector.model.user.*;
import it.pagopa.selfcare.onboarding.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.connector.rest.model.user_registry.EmbeddedExternalId;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserResource;
import it.pagopa.selfcare.user_registry.generated.openapi.v1.dto.UserSearchDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static it.pagopa.selfcare.onboarding.connector.model.user.Certification.NONE;
import static it.pagopa.selfcare.onboarding.connector.model.user.Certification.SPID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistryConnectorImplTest {

    @Mock
    private UserRegistryRestClient restClientMock;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRegistryConnectorImpl userConnector;

    @Test
    void search_nullInfo() {
        //given
        String externalId = "externalId";
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = new User();
        when(restClientMock.search(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.search(externalId, fieldList).get();
        ///then
        assertNull(user.getName());
        assertNull(user.getFamilyName());
        assertNull(user.getEmail());
        assertNull(user.getId());
        assertNull(user.getWorkContacts());
        assertNull(user.getFiscalCode());
        ArgumentCaptor<EmbeddedExternalId> embeddedCaptor = ArgumentCaptor.forClass(EmbeddedExternalId.class);
        verify(restClientMock, Mockito.times(1))
                .search(embeddedCaptor.capture(), eq(EnumSet.allOf(User.Fields.class)));
        EmbeddedExternalId externalIdCaptured = embeddedCaptor.getValue();
        assertEquals(externalId, externalIdCaptured.getFiscalCode());
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void search_nullInfo_userNotFound() {
        //given
        String externalId = "externalId";
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        Mockito.doThrow(FeignException.NotFound.class)
                .when(restClientMock)
                .search(any(), any());
        //when
        Optional<User> user = userConnector.search(externalId, fieldList);
        ///then
        assertNotNull(user);
        assertTrue(user.isEmpty());
        ArgumentCaptor<EmbeddedExternalId> embeddedCaptor = ArgumentCaptor.forClass(EmbeddedExternalId.class);
        verify(restClientMock, Mockito.times(1))
                .search(embeddedCaptor.capture(), eq(EnumSet.allOf(User.Fields.class)));
        EmbeddedExternalId externalIdCaptured = embeddedCaptor.getValue();
        assertEquals(externalId, externalIdCaptured.getFiscalCode());
        Mockito.verifyNoMoreInteractions(restClientMock);
    }


    @Test
    void search_certificationNone() {
        //given
        String externalId = "externalId";
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = TestUtils.mockInstance(new User());
        userMock.setId(UUID.randomUUID().toString());
        Map<String, WorkContact> workContacts = new HashMap<>();
        workContacts.put("institutionId", TestUtils.mockInstance(new WorkContact()));
        userMock.setWorkContacts(workContacts);
        when(restClientMock.search(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.search(externalId, fieldList).get();
        ///then
        assertEquals(NONE, user.getName().getCertification());
        assertEquals(NONE, user.getEmail().getCertification());
        assertEquals(NONE, user.getFamilyName().getCertification());
        user.getWorkContacts().forEach((key1, value) -> assertEquals(NONE, value.getEmail().getCertification()));
        assertNotNull(user.getFiscalCode());

        ArgumentCaptor<EmbeddedExternalId> embeddedCaptor = ArgumentCaptor.forClass(EmbeddedExternalId.class);
        verify(restClientMock, Mockito.times(1))
                .search(embeddedCaptor.capture(), eq(EnumSet.allOf(User.Fields.class)));
        EmbeddedExternalId externalIdCaptured = embeddedCaptor.getValue();
        assertEquals(externalId, externalIdCaptured.getFiscalCode());
        Mockito.verifyNoMoreInteractions(restClientMock);
    }


    @Test
    void search_nullExternalId() {
        //given
        String externalId = null;
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        //when
        Executable executable = () -> userConnector.search(externalId, fieldList);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A TaxCode is required", e.getMessage());
        Mockito.verifyNoInteractions(restClientMock);
    }

    @Test
    void search_certificationNotNone() {
        //given
        String externalId = "externalId";
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = TestUtils.mockInstance(new User());
        userMock.getEmail().setCertification(SPID);
        userMock.getFamilyName().setCertification(SPID);
        userMock.getName().setCertification(SPID);
        Map<String, WorkContact> workContacts = new HashMap<>();
        WorkContact workContact = TestUtils.mockInstance(new WorkContact());
        workContact.getEmail().setCertification(SPID);
        userMock.setWorkContacts(workContacts);
        workContacts.put("institutionId", workContact);
        when(restClientMock.search(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.search(externalId, fieldList).get();
        //then
        assertEquals(SPID, user.getName().getCertification());
        assertEquals(SPID, user.getEmail().getCertification());
        assertEquals(SPID, user.getFamilyName().getCertification());
        user.getWorkContacts().forEach((key1, value) -> assertEquals(SPID, value.getEmail().getCertification()));
        assertNotNull(user.getFiscalCode());

        ArgumentCaptor<EmbeddedExternalId> embeddedCaptor = ArgumentCaptor.forClass(EmbeddedExternalId.class);
        verify(restClientMock, Mockito.times(1))
                .search(embeddedCaptor.capture(), eq(EnumSet.allOf(User.Fields.class)));
        EmbeddedExternalId externalIdCaptured = embeddedCaptor.getValue();
        assertEquals(externalId, externalIdCaptured.getFiscalCode());
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getUserByInternalId_nullInfo() {
        //given
        UUID userId = UUID.randomUUID();
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = new User();
        when(restClientMock.getUserByInternalId(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.getUserByInternalId(userId.toString(), fieldList);
        ///then
        assertNull(user.getName());
        assertNull(user.getFamilyName());
        assertNull(user.getEmail());
        assertNull(user.getId());
        assertNull(user.getWorkContacts());
        assertNull(user.getFiscalCode());
        verify(restClientMock, Mockito.times(1))
                .getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getUserByInternalId_nullInfo_nullUserResponse() {
        //given
        UUID userId = UUID.randomUUID();
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = null;
        when(restClientMock.getUserByInternalId(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.getUserByInternalId(userId.toString(), fieldList);
        ///then
        assertNull(user);

        verify(restClientMock, Mockito.times(1))
                .getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void getUserByInternalId_certificationNone() {
        //given
        UUID userId = UUID.randomUUID();
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = TestUtils.mockInstance(new User());
        userMock.setId(userId.toString());
        Map<String, WorkContact> workContacts = new HashMap<>();
        WorkContact workContact = TestUtils.mockInstance(new WorkContact());
        workContact.getEmail().setCertification(NONE);
        userMock.setWorkContacts(workContacts);
        workContacts.put("institutionId", workContact);
        when(restClientMock.getUserByInternalId(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.getUserByInternalId(userId.toString(), fieldList);
        ///then
        assertEquals(userId.toString(), user.getId());
        assertEquals(NONE, user.getName().getCertification());
        assertEquals(NONE, user.getEmail().getCertification());
        assertEquals(NONE, user.getFamilyName().getCertification());
        user.getWorkContacts().forEach((key1, value) -> assertEquals(NONE, value.getEmail().getCertification()));
        assertNotNull(user.getFiscalCode());

        verify(restClientMock, Mockito.times(1))
                .getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        Mockito.verifyNoMoreInteractions(restClientMock);
    }


    @Test
    void getUserByInternalId_nullExternalId() {
        //given
        String userId = null;
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        //when
        Executable executable = () -> userConnector.getUserByInternalId(userId, fieldList);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A userId is required", e.getMessage());
        Mockito.verifyNoInteractions(restClientMock);
    }

    @Test
    void getUserByInternalId_certificationNotNone() {
        //given
        UUID userId = UUID.randomUUID();
        final EnumSet<User.Fields> fieldList = EnumSet.allOf(User.Fields.class);
        User userMock = TestUtils.mockInstance(new User());
        userMock.setId(userId.toString());
        userMock.getEmail().setCertification(SPID);
        userMock.getFamilyName().setCertification(SPID);
        userMock.getName().setCertification(SPID);
        Map<String, WorkContact> workContacts = new HashMap<>();
        WorkContact workContact = TestUtils.mockInstance(new WorkContact());
        workContact.getEmail().setCertification(SPID);
        userMock.setWorkContacts(workContacts);
        workContacts.put("institutionId", workContact);
        when(restClientMock.getUserByInternalId(any(), any()))
                .thenReturn(userMock);
        //when
        User user = userConnector.getUserByInternalId(userId.toString(), fieldList);
        ///then
        assertEquals(userId.toString(), user.getId());
        assertEquals(SPID, user.getName().getCertification());
        assertEquals(SPID, user.getEmail().getCertification());
        assertEquals(SPID, user.getFamilyName().getCertification());
        user.getWorkContacts().forEach((key1, value) -> assertEquals(SPID, value.getEmail().getCertification()));
        assertNotNull(user.getFiscalCode());

        verify(restClientMock, Mockito.times(1))
                .getUserByInternalId(userId, EnumSet.allOf(User.Fields.class));
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void updateUser() {
        //given
        UUID id = UUID.randomUUID();
        MutableUserFieldsDto userDto = TestUtils.mockInstance(new MutableUserFieldsDto(), "setWorkContacts");
        //when
        Executable executable = () -> userConnector.updateUser(id, userDto);
        //then
        assertDoesNotThrow(executable);
        ArgumentCaptor<MutableUserFieldsDto> userDtoCaptor = ArgumentCaptor.forClass(MutableUserFieldsDto.class);
        verify(restClientMock, Mockito.times(1))
                .patchUser(any(), userDtoCaptor.capture());

        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void updateUser_nullId() {
        //given
        UUID id = null;
        MutableUserFieldsDto userDto = TestUtils.mockInstance(new MutableUserFieldsDto(), "setWorkContacts");

        //when
        Executable executable = () -> userConnector.updateUser(id, userDto);
        //then
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A UUID is required", e.getMessage());
        Mockito.verifyNoInteractions(restClientMock);
    }

    @Test
    void saveUser() {
        //given
        UserId id = TestUtils.mockInstance(new UserId());
        SaveUserDto saveUserDto = TestUtils.mockInstance(new SaveUserDto(), "setWorkContacts");
        when(restClientMock.saveUser(any()))
                .thenReturn(id);
        //when
        UserId userId = userConnector.saveUser(saveUserDto);
        //then
        assertEquals(id.getId(), userId.getId());
        ArgumentCaptor<SaveUserDto> savedDto = ArgumentCaptor.forClass(SaveUserDto.class);
        verify(restClientMock, Mockito.times(1))
                .saveUser(savedDto.capture());
        SaveUserDto captured = savedDto.getValue();
        assertSame(saveUserDto, captured);
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void saveUser_nullInfo() {
        //given
        SaveUserDto saveUserDto = TestUtils.mockInstance(new SaveUserDto(), "setWorkContacts");
        //when
        UserId id = userConnector.saveUser(saveUserDto);
        //then
        assertNull(id);
        ArgumentCaptor<SaveUserDto> savedDto = ArgumentCaptor.forClass(SaveUserDto.class);
        verify(restClientMock, Mockito.times(1))
                .saveUser(savedDto.capture());
        SaveUserDto captured = savedDto.getValue();
        assertSame(saveUserDto, captured);
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void deleteById() {
        //given
        UUID id = UUID.randomUUID();
        //when
        userConnector.deleteById(id.toString());
        //then
        verify(restClientMock, Mockito.times(1))
                .deleteById(id);
        Mockito.verifyNoMoreInteractions(restClientMock);
    }

    @Test
    void deleteById_nullId() {
        //given
        //when
        Executable executable = () -> userConnector.deleteById(null);
        //then
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("A UUID is required", illegalArgumentException.getMessage());
        Mockito.verifyNoInteractions(restClientMock);
    }

    @Test
    void searchUser_returnsUserId_whenRestClientRespondsSuccessfully() {
        // given
        String taxCode = "ABCDEF12G34H567I";
        UserResource mockUserResource = new UserResource();
        UUID uuid = UUID.randomUUID();
        UserId expectedUserId = new UserId();
        expectedUserId.setId(uuid);

        doReturn(ResponseEntity.ok(mockUserResource))
                .when(restClientMock)
                ._searchUsingPOST(anyString(), any(UserSearchDto.class));

        when(userMapper.toUserId(mockUserResource)).thenReturn(expectedUserId);

        // when
        UserId result = userConnector.searchUser(taxCode);

        // then
        assertNotNull(result);
        assertEquals(expectedUserId, result);
        verify(restClientMock)._searchUsingPOST(any(), any());
        verify(userMapper).toUserId(mockUserResource);
    }
}
