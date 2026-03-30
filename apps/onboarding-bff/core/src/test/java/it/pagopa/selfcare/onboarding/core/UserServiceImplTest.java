package it.pagopa.selfcare.onboarding.core;

import static it.pagopa.selfcare.onboarding.connector.model.user.User.Fields.familyName;
import static it.pagopa.selfcare.onboarding.connector.model.user.User.Fields.name;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.connector.api.OnboardingMsConnector;
import it.pagopa.selfcare.onboarding.connector.api.UserRegistryConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.connector.model.institutions.ManagerVerification;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.User;
import it.pagopa.selfcare.onboarding.connector.model.user.Certification;
import it.pagopa.selfcare.onboarding.connector.model.user.CertifiedField;
import it.pagopa.selfcare.onboarding.connector.model.user.UserId;
import it.pagopa.selfcare.onboarding.core.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.core.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.core.strategy.UserAllowedValidationStrategy;
import it.pagopa.selfcare.onboarding.core.utils.PgManagerVerifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {


    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRegistryConnector userRegistryConnectorMock;

    @Mock
    private OnboardingMsConnector onboardingMsConnector;

    @Mock
    private PgManagerVerifier pgManagerVerifierMock;

    @Mock
    private UserAllowedValidationStrategy userAllowedValidationStrategy;


    @Test
    void validate_nullUser() {
        // given
        User user = null;
        // when
        final Executable executable = () -> userService.validate(user);
        // then
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("An user is required", e.getMessage());
        verifyNoInteractions(userRegistryConnectorMock);
    }


    @Test
    void validate_userNotFound() {
        // given
        User user = TestUtils.mockInstance(new User());
        // when
        userService.validate(user);
        // then
        verify(userRegistryConnectorMock, times(1))
                .search(user.getTaxCode(), EnumSet.of(name, familyName));
        verifyNoMoreInteractions(userRegistryConnectorMock);
    }


    @Test
    void validate_validUserData() {
        // given
        User user = TestUtils.mockInstance(new User());
        final it.pagopa.selfcare.onboarding.connector.model.user.User userFound =
                new it.pagopa.selfcare.onboarding.connector.model.user.User();
        final CertifiedField<String> certifiedFamilyName = new CertifiedField<>();
        certifiedFamilyName.setCertification(Certification.NONE);
        userFound.setFamilyName(certifiedFamilyName);
        when(userRegistryConnectorMock.search(any(), any()))
                .thenReturn(Optional.of(userFound));
        // when
        userService.validate(user);
        // then
        verify(userRegistryConnectorMock, times(1))
                .search(user.getTaxCode(), EnumSet.of(name, familyName));
        verifyNoMoreInteractions(userRegistryConnectorMock);
    }


    @Test
    void validate_invalidUserData() {
        // given
        User user = TestUtils.mockInstance(new User());
        final it.pagopa.selfcare.onboarding.connector.model.user.User userFound =
                new it.pagopa.selfcare.onboarding.connector.model.user.User();
        final CertifiedField<String> certifiedFamilyName = new CertifiedField<>();
        certifiedFamilyName.setCertification(Certification.SPID);
        certifiedFamilyName.setValue(user.getSurname());
        userFound.setFamilyName(certifiedFamilyName);
        final CertifiedField<String> certifiedName = new CertifiedField<>();
        certifiedName.setCertification(Certification.SPID);
        certifiedName.setValue("different value");
        userFound.setName(certifiedName);
        when(userRegistryConnectorMock.search(any(), any()))
                .thenReturn(Optional.of(userFound));
        // when
        final Executable executable = () -> userService.validate(user);
        // then
        final InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class, executable);
        assertNotNull(e.getInvalidFields());
        assertEquals(1, e.getInvalidFields().size());
        assertEquals("name", e.getInvalidFields().get(0).getName());
        assertEquals("the value does not match with the certified data", e.getInvalidFields().get(0).getReason());
        verify(userRegistryConnectorMock, times(1))
                .search(user.getTaxCode(), EnumSet.of(name, familyName));
        verifyNoMoreInteractions(userRegistryConnectorMock);
    }

    @Test
    void validate_invalidUserData_surname() {
        // given
        User user = TestUtils.mockInstance(new User());
        final it.pagopa.selfcare.onboarding.connector.model.user.User userFound =
                new it.pagopa.selfcare.onboarding.connector.model.user.User();
        final CertifiedField<String> certifiedName = new CertifiedField<>();
        certifiedName.setCertification(Certification.SPID);
        certifiedName.setValue(user.getName());
        userFound.setName(certifiedName);
        final CertifiedField<String> certifiedFamilyName = new CertifiedField<>();
        certifiedFamilyName.setCertification(Certification.SPID);
        certifiedFamilyName.setValue("different value");
        userFound.setFamilyName(certifiedFamilyName);
        when(userRegistryConnectorMock.search(any(), any()))
                .thenReturn(Optional.of(userFound));
        // when
        final Executable executable = () -> userService.validate(user);
        // then
        final InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class, executable);
        assertNotNull(e.getInvalidFields());
        assertEquals(1, e.getInvalidFields().size());
        assertEquals("surname", e.getInvalidFields().get(0).getName());
        assertEquals("the value does not match with the certified data", e.getInvalidFields().get(0).getReason());
        verify(userRegistryConnectorMock, times(1))
                .search(user.getTaxCode(), EnumSet.of(name, familyName));
        verifyNoMoreInteractions(userRegistryConnectorMock);
    }

    @Test
    void onboardingUsers() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        doNothing().when(onboardingMsConnector).onboardingUsers(any());
        // when
        userService.onboardingUsers(onboardingData);
        // then
        verify(onboardingMsConnector, times(1))
                .onboardingUsers(onboardingData);
        verifyNoMoreInteractions(onboardingMsConnector);
    }

    @Test
    void onboardingUsersAggregator() {
        // given
        OnboardingData onboardingData = new OnboardingData();
        doNothing().when(onboardingMsConnector).onboardingUsersAggregator(any());
        // when
        userService.onboardingUsersAggregator(onboardingData);
        // then
        verify(onboardingMsConnector, times(1))
                .onboardingUsersAggregator(onboardingData);
        verifyNoMoreInteractions(onboardingMsConnector);
    }

    @Test
    void checkManager() {
        // given
        CheckManagerData checkManagerData = new CheckManagerData();
        when(onboardingMsConnector.checkManager(any())).thenReturn(true);
        // when
        userService.checkManager(checkManagerData);
        // then
        verify(onboardingMsConnector, times(1))
                .checkManager(checkManagerData);
        verifyNoMoreInteractions(onboardingMsConnector);
    }

    @Test
    void getManagerInfo_shouldReturnManagerInfoWhenGivenUserIsManager() {
        // given
        String onboardingId = "onboardingId";
        String userTaxCode = "userTaxCode";
        OnboardingData onboardingData = TestUtils.mockInstance(new OnboardingData());
        onboardingData.getInstitutionUpdate().setTaxCode("institutionTaxCode");
        User manager = TestUtils.mockInstance(new User());
        manager.setName("managerName");
        manager.setSurname("managerSurname");
        manager.setRole(PartyRole.MANAGER);
        onboardingData.setUsers(List.of(manager));
        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);


        ManagerVerification managerVerification = new ManagerVerification();
        managerVerification.setVerified(true);
        when(pgManagerVerifierMock.doVerify(userTaxCode, "institutionTaxCode")).thenReturn(managerVerification);

        // when
        User result = userService.getManagerInfo(onboardingId, userTaxCode);

        // then
        assertNotNull(result);
        assertEquals("managerName", result.getName());
        assertEquals("managerSurname", result.getSurname());
        assertEquals(PartyRole.MANAGER, result.getRole());
        verify(onboardingMsConnector, times(1)).getOnboardingWithUserInfo(onboardingId);
        verify(pgManagerVerifierMock, times(1)).doVerify(userTaxCode, "institutionTaxCode");
    }

    @Test
    void getManagerInfo_shouldReturnManagerInfoWhenGivenUserIsAlreadyAdmin() {
        // given
        String onboardingId = "onboardingId";
        String userTaxCode = "userTaxCode";
        OnboardingData onboardingData = TestUtils.mockInstance(new OnboardingData());
        onboardingData.getInstitutionUpdate().setTaxCode("institutionTaxCode");
        User manager = TestUtils.mockInstance(new User());
        manager.setTaxCode(userTaxCode);
        manager.setName("managerName");
        manager.setSurname("managerSurname");
        manager.setRole(PartyRole.MANAGER);
        onboardingData.setUsers(List.of(manager));
        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);

        // when
        User result = userService.getManagerInfo(onboardingId, userTaxCode);

        // then
        assertNotNull(result);
        assertEquals("managerName", result.getName());
        assertEquals("managerSurname", result.getSurname());
        assertEquals(PartyRole.MANAGER, result.getRole());
        verify(onboardingMsConnector, times(1)).getOnboardingWithUserInfo(onboardingId);
        verify(pgManagerVerifierMock, times(0)).doVerify(userTaxCode, "institutionTaxCode");
    }

    @Test
    void getManagerInfo_shouldThrowResourceNotFoundExceptionWhenOnboardingNotFound() {
        // given
        String onboardingId = "onboardingId";
        String userTaxCode = "userTaxCode";
        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenThrow(new ResourceNotFoundException("Onboarding not found"));

        // when
        Executable executable = () -> userService.getManagerInfo(onboardingId, userTaxCode);

        // then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("Onboarding not found", exception.getMessage());
        verify(onboardingMsConnector, times(1)).getOnboardingWithUserInfo(onboardingId);
        verifyNoInteractions(pgManagerVerifierMock);
    }

    @Test
    void getManagerInfo_shouldThrowOnboardingNotAllowedExceptionWhenUserIsNotManager() {
        // given
        String onboardingId = "onboardingId";
        String userTaxCode = "userTaxCode";
        OnboardingData onboardingData = TestUtils.mockInstance(new OnboardingData());
        onboardingData.getInstitutionUpdate().setTaxCode("institutionTaxCode");

        User user = TestUtils.mockInstance(new User());
        user.setRole(PartyRole.MANAGER);
        onboardingData.setUsers(List.of(user));

        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);

        ManagerVerification managerVerification = new ManagerVerification();
        managerVerification.setVerified(false);
        when(pgManagerVerifierMock.doVerify(userTaxCode, "institutionTaxCode")).thenReturn(managerVerification);

        // when
        Executable executable = () -> userService.getManagerInfo(onboardingId, userTaxCode);

        // then
        OnboardingNotAllowedException exception = assertThrows(OnboardingNotAllowedException.class, executable);
        assertEquals("User is not an admin of the institution", exception.getMessage());
        verify(onboardingMsConnector, times(1)).getOnboardingWithUserInfo(onboardingId);
        verify(pgManagerVerifierMock, times(1)).doVerify(userTaxCode, "institutionTaxCode");
    }

    @Test
    void getManagerInfo_shouldThrowResourceNotFoundExceptionWhenManagerNotFound() {
        // given
        String onboardingId = "onboardingId";
        String userTaxCode = "userTaxCode";
        OnboardingData onboardingData = TestUtils.mockInstance(new OnboardingData());
        onboardingData.getInstitutionUpdate().setTaxCode("institutionTaxCode");
        User user = TestUtils.mockInstance(new User());
        user.setRole(PartyRole.OPERATOR);
        onboardingData.setUsers(List.of(user));

        when(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId)).thenReturn(onboardingData);

        // when
        Executable executable = () -> userService.getManagerInfo(onboardingId, userTaxCode);

        // then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, executable);
        assertEquals("Manager not found", exception.getMessage());
        verify(onboardingMsConnector, times(1)).getOnboardingWithUserInfo(onboardingId);
    }

  @Test
  void isAllowedUserByUidTest() {
    // given
    when(userAllowedValidationStrategy.isAuthorizedUser(anyString())).thenReturn(true);

    // when
    boolean result = userService.isAllowedUserByUid(anyString());

    // then
    assertTrue(result);
  }

    @Test
    void searchUser_returnsUserId_whenValidTaxCode() {
        // given
        String taxCode = "ABCDEF12G34H567I";
        UUID uuid = UUID.randomUUID();
        UserId userId = new UserId();
        userId.setId(uuid);

        when(userRegistryConnectorMock.searchUser(taxCode)).thenReturn(userId);

        // when
        UserId result = userService.searchUser(taxCode);

        // then
        assertNotNull(result);
        assertEquals(userId, result);
        verify(userRegistryConnectorMock).searchUser(taxCode);
    }
}