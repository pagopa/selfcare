package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.Certification;
import it.pagopa.selfcare.onboarding.client.model.CertifiedField;
import it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.exception.InvalidUserFieldsException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.service.UserRegistryService;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final EnumSet<RegistryUser.Fields> FIELDS = EnumSet.of(RegistryUser.Fields.name, RegistryUser.Fields.familyName);
    private static final String MISMATCH = "the value does not match with the certified data";

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRegistryService userRegistryClient;

    @Mock
    private OnboardingService onboardingMsClient;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Mock
    private PgManagerVerifier pgManagerVerifier;

    @Test
    void validate_nullUser() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> userService.validate(null));

        assertEquals("An user is required", e.getMessage());
        verifyNoInteractions(userRegistryClient);
    }

    @Test
    void validate_userNotFoundInRegistryIsAccepted() {
        User user = user("Mario", "Rossi");
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.empty());

        userService.validate(user);

        verify(userRegistryClient).search("TAX", FIELDS);
        verifyNoMoreInteractions(userRegistryClient);
    }

    @Test
    void validate_notCertifiedFieldsAreNeverCompared() {
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.NONE, "Other"));
        found.setFamilyName(certified(Certification.NONE, "Other"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        userService.validate(user("Mario", "Rossi"));

        verify(userRegistryClient).search("TAX", FIELDS);
    }

    @Test
    void validate_missingCertifiedFieldsAreAccepted() {
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(new RegistryUser()));

        userService.validate(user("Mario", "Rossi"));
    }

    @Test
    void validate_certifiedValuesAreComparedIgnoringCase() {
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "Mario"));
        found.setFamilyName(certified(Certification.SPID, "Rossi"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        userService.validate(user("mario", "ROSSI"));
    }

    @Test
    void validate_nameMismatchIsReported() {
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "different value"));
        found.setFamilyName(certified(Certification.SPID, "Rossi"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> userService.validate(user("Mario", "Rossi")));

        assertNotNull(e.getInvalidFields());
        assertEquals(1, e.getInvalidFields().size());
        assertEquals("name", e.getInvalidFields().get(0).getName());
        assertEquals(MISMATCH, e.getInvalidFields().get(0).getReason());
    }

    @Test
    void validate_surnameMismatchIsReported() {
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "Mario"));
        found.setFamilyName(certified(Certification.SPID, "different value"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> userService.validate(user("Mario", "Rossi")));

        assertEquals(1, e.getInvalidFields().size());
        assertEquals("surname", e.getInvalidFields().get(0).getName());
        assertEquals(MISMATCH, e.getInvalidFields().get(0).getReason());
    }

    @Test
    void validate_bothMismatchesAreReportedNameFirst() {
        RegistryUser found = new RegistryUser();
        found.setName(certified(Certification.SPID, "x"));
        found.setFamilyName(certified(Certification.SPID, "y"));
        when(userRegistryClient.search("TAX", FIELDS)).thenReturn(Optional.of(found));

        InvalidUserFieldsException e = assertThrows(InvalidUserFieldsException.class,
                () -> userService.validate(user("Mario", "Rossi")));

        assertEquals(List.of("name", "surname"),
                e.getInvalidFields().stream().map(InvalidUserFieldsException.InvalidField::getName).toList());
    }

    @Test
    void onboardingUsers_delegates() {
        OnboardingData onboardingData = new OnboardingData();

        userService.onboardingUsers(onboardingData);

        verify(onboardingMsClient).onboardingUsers(onboardingData);
    }

    @Test
    void onboardingUsersAggregator_delegates() {
        OnboardingData onboardingData = new OnboardingData();

        userService.onboardingUsersAggregator(onboardingData);

        verify(onboardingMsClient).onboardingUsersAggregator(onboardingData);
    }

    @Test
    void checkManager_returnsTheDownstreamAnswer() {
        CheckManagerRequest request = new CheckManagerRequest();
        when(onboardingMsClient.checkManager(request)).thenReturn(true, false);

        assertTrue(userService.checkManager(request));
        assertFalse(userService.checkManager(request));
    }

    @Test
    void getManagerInfo_onboardingNotFound() {
        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenThrow(new ResourceNotFoundException("raw body"));

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () -> userService.getManagerInfo("id", "TAX"));

        assertEquals("Onboarding not found", e.getMessage());
        verifyNoInteractions(pgManagerVerifier);
    }

    @Test
    void getManagerInfo_userAlreadyAdminSkipsTheVerification() {
        OnboardingData onboardingData = onboardingWith(manager("TAX"));
        stubOnboarding(onboardingData);

        User result = userService.getManagerInfo("id", "TAX");

        assertEquals(PartyRole.MANAGER, result.getRole());
        verify(pgManagerVerifier, never()).doVerify(any(), any());
    }

    @Test
    void getManagerInfo_verifiedUserGetsTheManager() {
        User manager = manager("MANAGER-TAX");
        stubOnboarding(onboardingWith(manager));
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(true);
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenReturn(verification);

        assertSame(manager, userService.getManagerInfo("id", "TAX"));
    }

    @Test
    void getManagerInfo_unverifiedUserIsNotAllowed() {
        stubOnboarding(onboardingWith(manager("MANAGER-TAX")));
        ManagerVerification verification = new ManagerVerification();
        verification.setVerified(false);
        when(pgManagerVerifier.doVerify("TAX", "COMPANY")).thenReturn(verification);

        OnboardingNotAllowedException e = assertThrows(OnboardingNotAllowedException.class,
                () -> userService.getManagerInfo("id", "TAX"));

        assertEquals("User is not an admin of the institution", e.getMessage());
    }

    @Test
    void getManagerInfo_withoutManagerIsNotFound() {
        User operator = manager("OP-TAX");
        operator.setRole(PartyRole.OPERATOR);
        stubOnboarding(onboardingWith(operator));

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class,
                () -> userService.getManagerInfo("id", "TAX"));

        assertEquals("Manager not found", e.getMessage());
        verifyNoInteractions(pgManagerVerifier);
    }

    @Test
    void searchUser_delegates() {
        UserId expected = new UserId();
        expected.setId(UUID.randomUUID());
        when(userRegistryClient.searchUser("TAX")).thenReturn(expected);

        assertSame(expected, userService.searchUser("TAX"));
    }

    private void stubOnboarding(OnboardingData onboardingData) {
        OnboardingGet downstream = new OnboardingGet();
        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenReturn(downstream);
        when(onboardingMapper.toOnboardingData(downstream)).thenReturn(onboardingData);
    }

    private static OnboardingData onboardingWith(User... users) {
        InstitutionUpdate update = new InstitutionUpdate();
        update.setTaxCode("COMPANY");
        OnboardingData onboardingData = new OnboardingData();
        onboardingData.setInstitutionUpdate(update);
        onboardingData.setUsers(List.of(users));
        return onboardingData;
    }

    private static User manager(String taxCode) {
        User manager = new User();
        manager.setTaxCode(taxCode);
        manager.setRole(PartyRole.MANAGER);
        return manager;
    }

    private static User user(String name, String surname) {
        User user = new User();
        user.setTaxCode("TAX");
        user.setName(name);
        user.setSurname(surname);
        return user;
    }

    private static CertifiedField<String> certified(Certification certification, String value) {
        CertifiedField<String> field = new CertifiedField<>();
        field.setCertification(certification);
        field.setValue(value);
        return field;
    }
}
