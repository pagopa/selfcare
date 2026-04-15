package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.strategy.UserAllowedValidationStrategy;
import it.pagopa.selfcare.onboarding.util.PgManagerVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

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

    @Mock
    private UserAllowedValidationStrategy userAllowedValidationStrategy;

    @Test
    void validate_nullUser() {
        assertThrows(NullPointerException.class, () -> userService.validate(null));
    }

    @Test
    void validate_validUser_noException() {
        User user = new User();
        user.setTaxCode("TAX");
        when(userRegistryClient.search(any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userService.validate(user));
    }

    @Test
    void onboardingUsers_delegates() {
        OnboardingData onboardingData = new OnboardingData();

        userService.onboardingUsers(onboardingData);

        verify(onboardingMsClient).onboardingUsers(onboardingData);
    }

    @Test
    void checkManager_whenResponseTrue_returnsTrue() {
        CheckManagerRequest req = new CheckManagerRequest();
        CheckManagerResponse response = new CheckManagerResponse();
        response.setResponse(true);
        when(onboardingMsClient.checkManager(req)).thenReturn(response);

        assertTrue(userService.checkManager(req));
    }

    @Test
    void getManagerInfo_onboardingNotFound_throwsResourceNotFound() {
        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenThrow(new ResourceNotFoundException("nf"));

        assertThrows(ResourceNotFoundException.class, () -> userService.getManagerInfo("id", "TAX"));
    }

    @Test
    void getManagerInfo_userAlreadyAdmin_returnsManager() {
        OnboardingGet onboardingGet = new OnboardingGet();
        OnboardingData onboardingData = new OnboardingData();
        it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate update = new it.pagopa.selfcare.onboarding.client.model.InstitutionUpdate();
        update.setTaxCode("COMPANY");
        onboardingData.setInstitutionUpdate(update);

        User manager = new User();
        manager.setTaxCode("TAX");
        manager.setRole(it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER);
        onboardingData.setUsers(List.of(manager));

        when(onboardingMsClient.getOnboardingWithUserInfo("id")).thenReturn(onboardingGet);
        when(onboardingMapper.toOnboardingData(onboardingGet)).thenReturn(onboardingData);

        User result = userService.getManagerInfo("id", "TAX");

        assertSame(manager, result);
        verify(pgManagerVerifier, never()).doVerify(any(), any());
    }

    @Test
    void searchUser_delegates() {
        UserId expected = new UserId();
        when(userRegistryClient.searchUser("TAX")).thenReturn(expected);

        UserId result = userService.searchUser("TAX");

        assertSame(expected, result);
    }
}
