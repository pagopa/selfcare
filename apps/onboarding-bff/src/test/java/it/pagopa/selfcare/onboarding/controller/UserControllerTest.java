package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.User;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.model.dto.request.CheckManagerDto;
import it.pagopa.selfcare.onboarding.model.dto.request.OnboardingUserDto;
import it.pagopa.selfcare.onboarding.model.dto.request.UserDataValidationDto;
import it.pagopa.selfcare.onboarding.model.dto.request.UserTaxCodeDto;
import it.pagopa.selfcare.onboarding.model.dto.response.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.model.dto.response.ManagerInfoResponse;
import it.pagopa.selfcare.onboarding.service.UserService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    SecurityIdentity securityIdentity;

    @InjectMocks
    UserController userController;

    @BeforeEach
    void setUp() {
        userController.securityIdentity = securityIdentity;
    }

    @Test
    void validate_waitsForTheServiceThenReturnsNoContent() {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        UserDataValidationDto request = new UserDataValidationDto();
        User user = new User();
        when(userMapper.toUser(request)).thenReturn(user);
        when(userService.validate(user)).thenReturn(Uni.createFrom().<Void>emitter(pending::set));

        // when
        UniAssertSubscriber<Response> result = userController.validate(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), result.assertCompleted().getItem().getStatus());
    }

    @Test
    void validate_propagatesTheServiceFailure() {
        // given
        UserDataValidationDto request = new UserDataValidationDto();
        User user = new User();
        ProcessingException failure = new ProcessingException("registry unavailable");
        when(userMapper.toUser(request)).thenReturn(user);
        when(userService.validate(user)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Response> result = userController.validate(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void validate_missingBodyIsABadRequestWithoutCallingTheService() {
        // given
        // when
        assertThrows(InvalidRequestException.class, () -> userController.validate(null));

        // then
        verifyNoInteractions(userService, userMapper);
    }

    @Test
    void onboarding_waitsForTheServiceThenReturnsCreated() {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        OnboardingUserDto request = new OnboardingUserDto();
        OnboardingData entity = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);
        when(userService.onboardingUsers(entity)).thenReturn(Uni.createFrom().<Void>emitter(pending::set));

        // when
        UniAssertSubscriber<Response> result = userController.onboarding(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(Response.Status.CREATED.getStatusCode(), result.assertCompleted().getItem().getStatus());
        verify(userService).onboardingUsers(entity);
    }

    @Test
    void onboarding_propagatesTheServiceFailure() {
        // given
        OnboardingUserDto request = new OnboardingUserDto();
        OnboardingData entity = new OnboardingData();
        OnboardingNotAllowedException failure = new OnboardingNotAllowedException("not allowed");
        when(onboardingMapper.toEntity(request)).thenReturn(entity);
        when(userService.onboardingUsers(entity)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Response> result = userController.onboarding(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void onboardingAggregator_waitsForTheServiceThenReturnsCreated() {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        OnboardingUserDto request = new OnboardingUserDto();
        OnboardingData entity = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(entity);
        when(userService.onboardingUsersAggregator(entity)).thenReturn(Uni.createFrom().<Void>emitter(pending::set));

        // when
        UniAssertSubscriber<Response> result = userController.onboardingAggregator(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(null);
        assertEquals(Response.Status.CREATED.getStatusCode(), result.assertCompleted().getItem().getStatus());
        verify(userService).onboardingUsersAggregator(entity);
    }

    @Test
    void onboardingAggregator_propagatesTheServiceFailure() {
        // given
        OnboardingUserDto request = new OnboardingUserDto();
        OnboardingData entity = new OnboardingData();
        ProcessingException failure = new ProcessingException("onboarding unavailable");
        when(onboardingMapper.toEntity(request)).thenReturn(entity);
        when(userService.onboardingUsersAggregator(entity)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Response> result = userController.onboardingAggregator(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void checkManager_returnsMappedBooleanResult() {
        // given
        CheckManagerDto request = new CheckManagerDto();
        var checkRequest = new org.openapi.quarkus.onboarding_json.model.CheckManagerRequest();
        when(onboardingMapper.toCheckManagerData(request)).thenReturn(checkRequest);
        when(userService.checkManager(checkRequest)).thenReturn(Uni.createFrom().item(true));

        // when
        CheckManagerResponse response = userController.checkManager(request).await().indefinitely();

        // then
        assertEquals(true, response.isResult());
    }

    @Test
    void getManagerInfo_readsTheCallerFiscalCodeAndMapsOnlyWhenTheServiceEmits() {
        // given
        AtomicReference<UniEmitter<? super User>> pending = new AtomicReference<>();
        User manager = new User();
        ManagerInfoResponse mapped = new ManagerInfoResponse();
        when(securityIdentity.getAttribute(anyString()))
                .thenAnswer(call -> "fiscal_number".equals(call.getArgument(0)) ? "FISCAL-CODE" : null);
        when(userService.getManagerInfo("onb-1", "FISCAL-CODE")).thenReturn(Uni.createFrom().<User>emitter(pending::set));
        when(userMapper.toManagerInfoResponse(manager)).thenReturn(mapped);

        // when
        UniAssertSubscriber<ManagerInfoResponse> result = userController.getManagerInfo("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(userMapper);
        pending.get().complete(manager);
        result.assertCompleted().assertItem(mapped);
    }

    @Test
    void getManagerInfo_propagatesTheServiceFailure() {
        // given
        OnboardingNotAllowedException failure = new OnboardingNotAllowedException("User is not an admin of the institution");
        when(securityIdentity.getAttribute(anyString()))
                .thenAnswer(call -> "fiscal_number".equals(call.getArgument(0)) ? "FISCAL-CODE" : null);
        when(userService.getManagerInfo("onb-1", "FISCAL-CODE")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<ManagerInfoResponse> result = userController.getManagerInfo("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
        verifyNoInteractions(userMapper);
    }

    @Test
    void searchUser_returnsTheServiceResultOnlyWhenItEmits() {
        // given
        AtomicReference<UniEmitter<? super UserId>> pending = new AtomicReference<>();
        UserTaxCodeDto request = new UserTaxCodeDto();
        request.setTaxCode("TAX");
        UserId expected = new UserId();
        expected.setId(UUID.randomUUID());
        when(userMapper.toString(request)).thenReturn("TAX");
        when(userService.searchUser("TAX")).thenReturn(Uni.createFrom().<UserId>emitter(pending::set));

        // when
        UniAssertSubscriber<UserId> result = userController.searchUser(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(expected);
        result.assertCompleted().assertItem(expected);
    }

    @Test
    void searchUser_propagatesTheServiceFailure() {
        // given
        UserTaxCodeDto request = new UserTaxCodeDto();
        ProcessingException failure = new ProcessingException("registry unavailable");
        when(userMapper.toString(request)).thenReturn("TAX");
        when(userService.searchUser("TAX")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<UserId> result = userController.searchUser(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }
}
