package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.controller.request.CheckManagerDto;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingUserDto;
import it.pagopa.selfcare.onboarding.controller.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.controller.request.UserTaxCodeDto;
import it.pagopa.selfcare.onboarding.controller.response.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    UserMapper userMapper;

    @InjectMocks
    UserController userController;

    @Test
    void validate_callsServiceAndReturnsNoContent() {
        UserDataValidationDto request = new UserDataValidationDto();
        User user = new User();
        when(userMapper.toUser(request)).thenReturn(user);

        Response response = userController.validate(request);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(userService).validate(user);
    }

    @Test
    void onboarding_callsServiceAndReturnsCreated() {
        OnboardingUserDto request = new OnboardingUserDto();
        it.pagopa.selfcare.onboarding.client.model.OnboardingData entity = new it.pagopa.selfcare.onboarding.client.model.OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);

        Response response = userController.onboarding(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(userService).onboardingUsers(entity);
    }

    @Test
    void onboardingAggregator_callsServiceAndReturnsCreated() {
        OnboardingUserDto request = new OnboardingUserDto();
        it.pagopa.selfcare.onboarding.client.model.OnboardingData entity = new it.pagopa.selfcare.onboarding.client.model.OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);

        Response response = userController.onboardingAggregator(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(userService).onboardingUsersAggregator(entity);
    }

    @Test
    void checkManager_returnsMappedBooleanResult() {
        CheckManagerDto request = new CheckManagerDto();
        var checkRequest = new org.openapi.quarkus.onboarding_json.model.CheckManagerRequest();
        when(onboardingMapper.toCheckManagerData(request)).thenReturn(checkRequest);
        when(userService.checkManager(checkRequest)).thenReturn(true);

        CheckManagerResponse response = userController.checkManager(request);

        assertEquals(true, response.isResult());
    }

    @Test
    void searchUser_returnsServiceResult() {
        UserTaxCodeDto request = new UserTaxCodeDto();
        request.setTaxCode("TAX");
        UserId expected = new UserId();
        expected.setId(UUID.randomUUID());
        when(userMapper.toString(request)).thenReturn("TAX");
        when(userService.searchUser("TAX")).thenReturn(expected);

        UserId result = userController.searchUser(request);

        assertSame(expected, result);
    }
}
