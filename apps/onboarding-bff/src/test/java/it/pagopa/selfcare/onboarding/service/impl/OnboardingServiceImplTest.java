package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.OnboardingUploadRestClient;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.impl.ClientRequestValidator;
import jakarta.validation.Validation;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.jboss.resteasy.reactive.client.impl.multipart.QuarkusMultipartFormDataPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_json.api.SupportApi;
import org.openapi.quarkus.onboarding_json.model.ApproveRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.InstitutionPaSubunitType;
import org.openapi.quarkus.onboarding_json.model.InstitutionBaseRequest;
import org.openapi.quarkus.onboarding_json.model.InstitutionPspRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingPgRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingUserPgRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingUserRequest;
import org.openapi.quarkus.onboarding_json.model.Origin;
import org.openapi.quarkus.onboarding_json.model.PaymentServiceProviderRequest;
import org.openapi.quarkus.onboarding_json.model.UserRequest;
import org.openapi.quarkus.onboarding_json.model.InstitutionResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingPaRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingPspRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingDefaultRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingStatus;
import org.openapi.quarkus.onboarding_json.model.ReasonRequest;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {

    /** Operations retried by the Spring connector (resilience4j retryTimeout). */
    private static final Set<String> RETRIED_OPERATIONS = new TreeSet<>(List.of(
            "onboarding", "onboardingUsers", "onboardingUsersAggregator", "onboardingCompany",
            "onboardingTokenComplete", "onboardingUsersComplete", "onboardingPending", "approveOnboarding",
            "rejectOnboarding", "getOnboarding", "getOnboardingWithUserInfo", "onboardingPaAggregation",
            "checkManager", "triggerOnboardingRequest"));

    @Mock
    private OnboardingControllerApi onboardingApi;

    @Mock
    private SupportApi supportApi;

    @Mock
    private OnboardingUploadRestClient uploadClient;

    @Mock
    private OnboardingMapper onboardingMapper;

    @Spy
    private ClientRequestValidator requestValidator =
            new ClientRequestValidator(Validation.buildDefaultValidatorFactory().getValidator());

    @InjectMocks
    private OnboardingServiceImpl onboardingService;

    @Test
    void onboarding_routesByInstitutionType() {
        // given
        OnboardingData pa = onboardingData(InstitutionType.PA);
        OnboardingData psp = onboardingData(InstitutionType.PSP);
        OnboardingData other = onboardingData(InstitutionType.GSP);
        OnboardingPaRequest paRequest = new OnboardingPaRequest().productId("prod-io").institution(institutionBase());
        OnboardingPspRequest pspRequest = new OnboardingPspRequest().productId("prod-io")
                .institution(new InstitutionPspRequest()
                        .institutionType(org.openapi.quarkus.onboarding_json.model.InstitutionType.PSP).origin(Origin.SELC)
                        .originId("origin-id").digitalAddress("digital@address")
                        .paymentServiceProvider(new PaymentServiceProviderRequest()));
        OnboardingDefaultRequest defaultRequest = new OnboardingDefaultRequest().productId("prod-io")
                .institution(institutionBase());
        when(onboardingMapper.toOnboardingPaRequest(pa)).thenReturn(paRequest);
        when(onboardingMapper.toOnboardingPspRequest(psp)).thenReturn(pspRequest);
        when(onboardingMapper.toOnboardingDefaultRequest(other)).thenReturn(defaultRequest);
        when(onboardingApi.onboardingPa(paRequest)).thenReturn(Uni.createFrom().item(new OnboardingResponse()));
        when(onboardingApi.onboardingPsp(pspRequest)).thenReturn(Uni.createFrom().item(new OnboardingResponse()));
        when(onboardingApi.onboarding(defaultRequest)).thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        // when
        onboardingService.onboarding(pa).await().indefinitely();
        onboardingService.onboarding(psp).await().indefinitely();
        onboardingService.onboarding(other).await().indefinitely();

        // then
        verify(onboardingApi).onboardingPa(paRequest);
        verify(onboardingApi).onboardingPsp(pspRequest);
        verify(onboardingApi).onboarding(defaultRequest);
    }

    @Test
    void approveOnboarding_sendsUserUidOnlyWhenPresent() {
        // given
        when(onboardingApi.approve(eq("onb-1"), any(ApproveRequest.class)))
                .thenAnswer(call -> Uni.createFrom().item(Response.noContent().build()));

        // when
        onboardingService.approveOnboarding("onb-1", "uid-1").await().indefinitely();
        onboardingService.approveOnboarding("onb-1", " ").await().indefinitely();

        // then
        ArgumentCaptor<ApproveRequest> captor = ArgumentCaptor.forClass(ApproveRequest.class);
        verify(onboardingApi, org.mockito.Mockito.times(2)).approve(eq("onb-1"), captor.capture());
        assertEquals("uid-1", captor.getAllValues().get(0).getUserUid());
        assertNull(captor.getAllValues().get(1).getUserUid());
    }

    @Test
    void approveOnboarding_waitsForTheNativeResponseAndClosesIt() {
        // given
        AtomicReference<UniEmitter<? super Response>> pending = new AtomicReference<>();
        Response response = mock(Response.class);
        when(onboardingApi.approve(eq("onb-1"), any(ApproveRequest.class)))
                .thenReturn(Uni.createFrom().<Response>emitter(pending::set));

        // when
        UniAssertSubscriber<Void> result = onboardingService.approveOnboarding("onb-1", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(response);
        pending.get().complete(response);
        result.assertCompleted().assertItem(null);
        verify(response).close();
    }

    @Test
    void approveOnboarding_propagatesTheNativeFailure() {
        // given
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingApi.approve(eq("onb-1"), any(ApproveRequest.class))).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = onboardingService.approveOnboarding("onb-1", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void rejectOnboarding_sendsReasonAndUserUidOnlyWhenPresent() {
        // given
        when(onboardingApi.rejectOnboardingUsingPUT(eq("onb-1"), any(ReasonRequest.class)))
                .thenAnswer(call -> Uni.createFrom().item(Response.noContent().build()));

        // when
        onboardingService.rejectOnboarding("onb-1", "REJECTED_BY_USER", "uid-1").await().indefinitely();
        onboardingService.rejectOnboarding("onb-1", null, "").await().indefinitely();

        // then
        ArgumentCaptor<ReasonRequest> captor = ArgumentCaptor.forClass(ReasonRequest.class);
        verify(onboardingApi, org.mockito.Mockito.times(2)).rejectOnboardingUsingPUT(eq("onb-1"), captor.capture());
        assertEquals("REJECTED_BY_USER", captor.getAllValues().get(0).getReasonForReject());
        assertEquals("uid-1", captor.getAllValues().get(0).getUserUid());
        assertNull(captor.getAllValues().get(1).getReasonForReject());
        assertNull(captor.getAllValues().get(1).getUserUid());
    }

    @Test
    void rejectOnboarding_waitsForTheNativeResponseAndClosesIt() {
        // given
        AtomicReference<UniEmitter<? super Response>> pending = new AtomicReference<>();
        Response response = mock(Response.class);
        when(onboardingApi.rejectOnboardingUsingPUT(eq("onb-1"), any(ReasonRequest.class)))
                .thenReturn(Uni.createFrom().<Response>emitter(pending::set));

        // when
        UniAssertSubscriber<Void> result = onboardingService.rejectOnboarding("onb-1", "REJECTED_BY_USER", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(response);
        pending.get().complete(response);
        result.assertCompleted().assertItem(null);
        verify(response).close();
    }

    @Test
    void rejectOnboarding_propagatesTheNativeFailure() {
        // given
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingApi.rejectOnboardingUsingPUT(eq("onb-1"), any(ReasonRequest.class)))
                .thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = onboardingService.rejectOnboarding("onb-1", "REJECTED_BY_USER", "uid-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void getOnboarding_andWithUserInfoDelegate() {
        // given
        OnboardingGet plain = new OnboardingGet();
        OnboardingGet withUsers = new OnboardingGet();
        when(onboardingApi.getById("onb-1")).thenReturn(Uni.createFrom().item(plain));
        when(onboardingApi.getByIdWithUserInfo("onb-1")).thenReturn(Uni.createFrom().item(withUsers));

        // when
        var actualAsync1 = onboardingService.getOnboarding("onb-1").await().indefinitely();
        var actualAsync2 = onboardingService.getOnboardingWithUserInfo("onb-1").await().indefinitely();

        // then
        assertSame(plain, actualAsync1);
        assertSame(withUsers, actualAsync2);
    }

    @Test
    void getOnboardingWithUserInfo_isPendingUntilTheNativeClientEmitsAndKeepsItsFailure() {
        // given
        AtomicReference<UniEmitter<? super OnboardingGet>> pending = new AtomicReference<>();
        OnboardingGet withUsers = new OnboardingGet();
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingApi.getByIdWithUserInfo("onb-1")).thenReturn(Uni.createFrom().<OnboardingGet>emitter(pending::set));
        when(onboardingApi.getByIdWithUserInfo("onb-2")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<OnboardingGet> waiting = onboardingService.getOnboardingWithUserInfo("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<OnboardingGet> failed = onboardingService.getOnboardingWithUserInfo("onb-2")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        waiting.assertNotTerminated();
        pending.get().complete(withUsers);
        waiting.assertCompleted().assertItem(withUsers);
        assertSame(failure, failed.getFailure());
    }

    @Test
    void onboardingPending_completesWithoutBodyOnlyWhenTheNativeClientEmits() {
        // given
        AtomicReference<UniEmitter<? super OnboardingGet>> pending = new AtomicReference<>();
        when(onboardingApi.getOnboardingPending("onb-1")).thenReturn(Uni.createFrom().<OnboardingGet>emitter(pending::set));

        // when
        UniAssertSubscriber<Void> result = onboardingService.onboardingPending("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        pending.get().complete(new OnboardingGet());
        result.assertCompleted().assertItem(null);
    }

    @Test
    void checkManager_returnsTheDownstreamFlag() {
        // given
        CheckManagerRequest request = new CheckManagerRequest().productId("prod-io").userId(UUID.randomUUID());
        CheckManagerResponse response = new CheckManagerResponse();
        response.setResponse(true);
        when(onboardingApi.checkManager(request)).thenReturn(Uni.createFrom().item(response));

        // when
        var actualAsync1 = onboardingService.checkManager(request).await().indefinitely();

        // then
        assertTrue(actualAsync1);
    }

    @Test
    void getByFilters_keepsTheOnboardingOfTheRequestedProductWithoutReferenceAndNotSubunit() {
        // given
        OnboardingResponse wanted = onboardingResponse("prod-io", null, null);
        OnboardingResponse otherProduct = onboardingResponse("prod-pn", null, null);
        OnboardingResponse withReference = onboardingResponse("prod-io", "ref", null);
        OnboardingResponse uo = onboardingResponse("prod-io", null, InstitutionPaSubunitType.UO);
        OnboardingResponse aoo = onboardingResponse("prod-io", null, InstitutionPaSubunitType.AOO);
        when(supportApi.onboardingInstitutionUsingGET("IPA", "oid", OnboardingStatus.COMPLETED, null, "tax"))
                .thenReturn(Uni.createFrom().item(List.of(wanted, otherProduct, withReference, uo, aoo)));

        // when
        List<OnboardingResponse> result = onboardingService.getByFilters("prod-io", "tax", "IPA", "oid", null).await().indefinitely();

        // then
        assertEquals(List.of(wanted), result);
    }

    @Test
    void getByFilters_withSubunitCodeKeepsOnlyOnboardingsWithoutReference() {
        // given
        OnboardingResponse uo = onboardingResponse("prod-io", null, InstitutionPaSubunitType.UO);
        OnboardingResponse withReference = onboardingResponse("prod-io", "ref", InstitutionPaSubunitType.UO);
        when(supportApi.onboardingInstitutionUsingGET(null, null, OnboardingStatus.COMPLETED, "SUB", "tax"))
                .thenReturn(Uni.createFrom().item(List.of(uo, withReference)));

        // when
        var actualResult1 = onboardingService.getByFilters("prod-io", "tax", null, null, "SUB").await().indefinitely();
        // then
        assertEquals(List.of(uo), actualResult1);
    }

    @Test
    void getByFilters_nullDownstreamBodyIsEmpty() {
        // given
        when(supportApi.onboardingInstitutionUsingGET(null, null, OnboardingStatus.COMPLETED, null, "tax"))
                .thenReturn(Uni.createFrom().nullItem());

        // when
        var actualResult1 = onboardingService.getByFilters("prod-io", "tax", null, null, null).await().indefinitely();
        // then
        assertTrue(actualResult1.isEmpty());
    }

    @Test
    void onboardingWithFilter_mapsTheResponseAndNeverForwardsProductId() {
        // given
        OnboardingGetResponse response = new OnboardingGetResponse();
        List<OnboardingResult> mapped = List.of(new OnboardingResult());
        when(onboardingApi.getOnboardingWithFilter(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(OnboardingStatus.PENDING), isNull(), eq("tax"), isNull(), isNull()))
                .thenReturn(Uni.createFrom().item(response));
        when(onboardingMapper.toOnboardingWithFilter(response)).thenReturn(mapped);

        // when
        var actualResult1 = onboardingService.onboardingWithFilter("tax", "PENDING").await().indefinitely();
        // then
        assertSame(mapped, actualResult1);
    }

    @Test
    void onboardingWithFilter_unknownStatusIsRejectedWithoutCall() {
        // given
        for (String status : new String[]{"NOT_A_STATUS", "pending", " ", null}) {
        // when
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> onboardingService.onboardingWithFilter("tax", status).await().indefinitely());
        // then
            assertEquals("Unexpected value '" + status + "'", exception.getMessage());
        }
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void verifyOnboarding_requiresProductId() {
        // given
        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> onboardingService.verifyOnboarding(" ", "tax", null, null, null, null).await().indefinitely());

        // then
        assertEquals("A product Id is required", exception.getMessage());
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void verifyOnboarding_forwardsAllFilters() {
        // given
        when(onboardingApi.verifyOnboardingInfoByFilters("PA", "IPA", "oid", "prod-io", "SUB", "tax"))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        // when
        onboardingService.verifyOnboarding("prod-io", "tax", "IPA", "oid", "SUB", "PA").await().indefinitely();

        // then
        verify(onboardingApi).verifyOnboardingInfoByFilters("PA", "IPA", "oid", "prod-io", "SUB", "tax");
    }

    @Test
    void aggregatesVerification_supportsOnlyTheAggregatesProducts() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1});
        VerifyAggregateResponse response = new VerifyAggregateResponse();
        VerifyAggregateResult mapped = new VerifyAggregateResult();
        when(uploadClient.verifyAggregatesCsv(eq("prod-io"), any())).thenReturn(response);
        when(onboardingMapper.toVerifyAggregateResult(response)).thenReturn(mapped);

        // when
        var actualResult1 = onboardingService.aggregatesVerification(file, "prod-io").await().indefinitely();
        // then
        assertSame(mapped, actualResult1);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> onboardingService.aggregatesVerification(file, "prod-other").await().indefinitely());
        assertEquals("400 BAD_REQUEST Unsupported productId: prod-other", exception.getMessage());
    }

    @Test
    void aggregatesVerification_rejectsNullProductBeforeCallingTheDownstream() {
        // given
        // when
        assertThrows(NullPointerException.class, () -> onboardingService.aggregatesVerification(null, null).await().indefinitely());
        // then
        verifyNoInteractions(uploadClient, onboardingMapper);
    }

    @Test
    void onlyTheOperationsRetriedBySpringAreRetriedOnTransportErrorsOnly() {
        Set<String> retried = Arrays.stream(OnboardingServiceImpl.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Retry.class))
                .map(Method::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(RETRIED_OPERATIONS, retried);
        for (Method method : OnboardingServiceImpl.class.getDeclaredMethods()) {
            Retry retry = method.getAnnotation(Retry.class);
            if (retry != null) {
                assertEquals(2, retry.maxRetries(), method.getName());
                assertEquals(5000, retry.delay(), method.getName());
                assertEquals(Set.of(ProcessingException.class, IOException.class), Set.of(retry.retryOn()), method.getName());
            }
        }
        assertFalse(retried.contains("checkRecipientCode"));
    }

    @Test
    void onboardingUsers_missingOriginIsRejectedWithoutCallingTheDownstream() {
        OnboardingData data = onboardingData(InstitutionType.PA);
        when(onboardingMapper.toOnboardingUsersRequest(data)).thenReturn(new OnboardingUserRequest()
                .productId("prod-io").users(List.of(new UserRequest())));

        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> onboardingService.onboardingUsers(data));

        assertTrue(e.getMessage().contains("_onboardingUsers.onboardingUserRequest.origin: "), e.getMessage());
        assertTrue(e.getMessage().contains("_onboardingUsers.onboardingUserRequest.originId: "), e.getMessage());
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void onboardingUsersAggregator_missingOriginIsRejectedWithoutCallingTheDownstream() {
        OnboardingData data = onboardingData(InstitutionType.PA);
        when(onboardingMapper.toOnboardingUsersRequest(data)).thenReturn(new OnboardingUserRequest()
                .productId("prod-io").users(List.of(new UserRequest())));

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> onboardingService.onboardingUsersAggregator(data));

        assertTrue(e.getMessage().startsWith("_onboardingUsersAggregator.onboardingUserRequest."), e.getMessage());
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void onboardingUsers_validRequestIsSent() {
        OnboardingData data = onboardingData(InstitutionType.PA);
        OnboardingUserRequest request = new OnboardingUserRequest().productId("prod-io").origin("IPA")
                .originId("origin-id").users(List.of(new UserRequest()));
        when(onboardingMapper.toOnboardingUsersRequest(data)).thenReturn(request);
        when(onboardingApi.onboardingUsers(request)).thenReturn(Uni.createFrom().item(new OnboardingResponse()));

        onboardingService.onboardingUsers(data).await().indefinitely();

        verify(onboardingApi).onboardingUsers(request);
    }

    @Test
    void onboardingUsers_andAggregatorWaitForTheNativeClientAndKeepItsFailure() {
        // given
        OnboardingData data = onboardingData(InstitutionType.PA);
        OnboardingUserRequest request = new OnboardingUserRequest().productId("prod-io").origin("IPA")
                .originId("origin-id").users(List.of(new UserRequest()));
        AtomicReference<UniEmitter<? super OnboardingResponse>> pending = new AtomicReference<>();
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingMapper.toOnboardingUsersRequest(data)).thenReturn(request);
        when(onboardingApi.onboardingUsers(request)).thenReturn(Uni.createFrom().<OnboardingResponse>emitter(pending::set));
        when(onboardingApi.onboardingUsersAggregator(request)).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> users = onboardingService.onboardingUsers(data)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> aggregator = onboardingService.onboardingUsersAggregator(data)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        users.assertNotTerminated();
        pending.get().complete(new OnboardingResponse());
        users.assertCompleted().assertItem(null);
        assertSame(failure, aggregator.getFailure());
    }

    @Test
    void onboarding_invalidRequestsAreRejectedWithoutCallingTheDownstream() {
        // given
        OnboardingData pa = onboardingData(InstitutionType.PA);
        OnboardingData psp = onboardingData(InstitutionType.PSP);
        OnboardingData other = onboardingData(InstitutionType.GSP);
        when(onboardingMapper.toOnboardingPaRequest(pa)).thenReturn(new OnboardingPaRequest().productId("prod-io"));
        when(onboardingMapper.toOnboardingPspRequest(psp)).thenReturn(new OnboardingPspRequest().productId(""));
        when(onboardingMapper.toOnboardingDefaultRequest(other)).thenReturn(new OnboardingDefaultRequest());

        // when
        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.onboarding(pa).await().indefinitely()).getMessage()
                .startsWith("_onboardingPa.onboardingPaRequest.institution: "));
        // then
        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.onboarding(psp).await().indefinitely()).getMessage()
                .startsWith("_onboardingPsp.onboardingPspRequest."));
        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.onboarding(other).await().indefinitely()).getMessage()
                .startsWith("_onboarding.onboardingDefaultRequest."));
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void otherBodyCalls_areValidatedBeforeTheDownstreamCall() {
        // given
        OnboardingData data = onboardingData(InstitutionType.PG);
        when(onboardingMapper.toOnboardingPgRequest(data)).thenReturn(new OnboardingPgRequest());
        when(onboardingMapper.toOnboardingPaAggregationRequest(data)).thenReturn(new OnboardingPaRequest());
        when(onboardingMapper.toOnboardingUserPgRequest(data)).thenReturn(new OnboardingUserPgRequest());

        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.onboardingCompany(data).await().indefinitely())
                .getMessage().startsWith("_onboardingPgCompletion.onboardingPgRequest."));
        // when
        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.onboardingPaAggregation(data).await().indefinitely())
                .getMessage().startsWith("_onboardingPaAggregation.onboardingPaRequest."));
        // then
        assertTrue(assertThrows(InvalidRequestException.class,
                () -> onboardingService.onboardingUsersPgFromIcAndAde(data).await().indefinitely())
                .getMessage().startsWith("_onboardingUsersPg.onboardingUserPgRequest."));
        assertTrue(assertThrows(InvalidRequestException.class, () -> onboardingService.checkManager(new CheckManagerRequest()).await().indefinitely())
                .getMessage().startsWith("_checkManager.checkManagerRequest."));
        verifyNoInteractions(onboardingApi);
    }

    @Test
    void recipientCodeMappingWaitsForTheNativeClientResult() {
        // given
        AtomicReference<UniEmitter<? super org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus>> pending = new AtomicReference<>();
        var response = org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus.ACCEPTED;
        when(onboardingApi.checkRecipientCode("origin", "ABCDEF"))
                .thenReturn(Uni.createFrom().<org.openapi.quarkus.onboarding_json.model.RecipientCodeStatus>emitter(pending::set));
        when(onboardingMapper.toRecipientCodeStatusResult(response))
                .thenReturn(it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult.ACCEPTED);

        // when
        var result = onboardingService.checkRecipientCode("origin", "ABCDEF")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(onboardingMapper);
        pending.get().complete(response);
        result.assertCompleted().assertItem(it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult.ACCEPTED);
    }

    @Test
    void triggerWaitsForTheNativeResponseAndClosesIt() {
        // given
        AtomicReference<UniEmitter<? super Response>> pending = new AtomicReference<>();
        Response response = mock(Response.class);
        when(onboardingApi.triggerDocumentGate("onb-1")).thenReturn(Uni.createFrom().<Response>emitter(pending::set));

        // when
        UniAssertSubscriber<Void> result = onboardingService.triggerOnboardingRequest("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(response);
        pending.get().complete(response);
        result.assertCompleted().assertItem(null);
        verify(response).close();
    }

    @Test
    void triggerPropagatesTheNativeFailure() {
        // given
        ProcessingException failure = new ProcessingException("connection dropped");
        when(onboardingApi.triggerDocumentGate("onb-1")).thenReturn(Uni.createFrom().failure(failure));

        // when
        UniAssertSubscriber<Void> result = onboardingService.triggerOnboardingRequest("onb-1")
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        assertSame(failure, result.getFailure());
    }

    @Test
    void aggregatesUploadRunsLazilyOnTheWorkerAndKeepsItsFailure() {
        // given
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", new byte[]{1});
        ProcessingException failure = new ProcessingException("upload failed");
        AtomicReference<Thread> uploadThread = new AtomicReference<>();
        when(uploadClient.verifyAggregatesCsv(eq("prod-io"), any())).thenAnswer(invocation -> {
            uploadThread.set(Thread.currentThread());
            throw failure;
        });
        Uni<VerifyAggregateResult> operation = onboardingService.aggregatesVerification(file, "prod-io");
        verifyNoInteractions(uploadClient, onboardingMapper);
        Thread subscribingThread = Thread.currentThread();

        // when
        UniAssertSubscriber<VerifyAggregateResult> result = operation.subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.awaitFailure(Duration.ofSeconds(2));
        assertSame(failure, result.getFailure());
        assertNotSame(subscribingThread, uploadThread.get());
        verify(uploadClient).verifyAggregatesCsv(eq("prod-io"), any());
        verifyNoInteractions(onboardingMapper);
    }

    @Test
    void aggregatesUploadKeepsNameTypeAndBytes() {
        // given
        byte[] bytes = {1, 2, 3};
        UploadedFile file = new UploadedFile("aggregates.csv", "text/csv", bytes);
        VerifyAggregateResponse response = new VerifyAggregateResponse();
        when(uploadClient.verifyAggregatesCsv(eq("prod-io"), any())).thenReturn(response);
        when(onboardingMapper.toVerifyAggregateResult(response)).thenReturn(new VerifyAggregateResult());
        Uni<VerifyAggregateResult> operation = onboardingService.aggregatesVerification(file, "prod-io");

        // when
        operation.await().atMost(Duration.ofSeconds(2));

        // then
        ArgumentCaptor<ClientMultipartForm> form = ArgumentCaptor.forClass(ClientMultipartForm.class);
        verify(uploadClient).verifyAggregatesCsv(eq("prod-io"), form.capture());
        assertFilePart(form.getValue(), "aggregates", "aggregates.csv", "text/csv", bytes);
    }

    @Test
    void completeOperationsSendTheContractOnAWorkerAndCloseTheResponse() {
        // given
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> tokenThread = new AtomicReference<>();
        AtomicReference<Thread> usersThread = new AtomicReference<>();
        byte[] bytes = {4, 5, 6};
        UploadedFile contract = new UploadedFile("contract.pdf", "application/pdf", bytes);
        Response tokenResponse = mock(Response.class);
        Response usersResponse = mock(Response.class);
        when(uploadClient.completeOnboardingToken(eq("onb-1"), any())).thenAnswer(call -> {
            tokenThread.set(Thread.currentThread());
            return tokenResponse;
        });
        when(uploadClient.completeOnboardingUsers(eq("onb-1"), any())).thenAnswer(call -> {
            usersThread.set(Thread.currentThread());
            return usersResponse;
        });
        Uni<Void> token = onboardingService.onboardingTokenComplete("onb-1", contract);
        Uni<Void> users = onboardingService.onboardingUsersComplete("onb-1", contract);
        verifyNoInteractions(uploadClient, tokenResponse, usersResponse);

        // when
        token.await().atMost(Duration.ofSeconds(2));
        users.await().atMost(Duration.ofSeconds(2));

        // then
        assertNotSame(caller, tokenThread.get());
        assertNotSame(caller, usersThread.get());
        verify(tokenResponse).close();
        verify(usersResponse).close();
        ArgumentCaptor<ClientMultipartForm> tokenForm = ArgumentCaptor.forClass(ClientMultipartForm.class);
        ArgumentCaptor<ClientMultipartForm> usersForm = ArgumentCaptor.forClass(ClientMultipartForm.class);
        verify(uploadClient).completeOnboardingToken(eq("onb-1"), tokenForm.capture());
        verify(uploadClient).completeOnboardingUsers(eq("onb-1"), usersForm.capture());
        assertFilePart(tokenForm.getValue(), "contract", "contract.pdf", "application/pdf", bytes);
        assertFilePart(usersForm.getValue(), "contract", "contract.pdf", "application/pdf", bytes);
    }

    @Test
    void completeOperationsWithoutContentTypeUseOctetStream() {
        // given
        UploadedFile contract = new UploadedFile("contract.bin", null, new byte[]{7});
        Response response = mock(Response.class);
        when(uploadClient.completeOnboardingToken(eq("onb-1"), any())).thenReturn(response);

        // when
        onboardingService.onboardingTokenComplete("onb-1", contract).await().atMost(Duration.ofSeconds(2));

        // then
        ArgumentCaptor<ClientMultipartForm> form = ArgumentCaptor.forClass(ClientMultipartForm.class);
        verify(uploadClient).completeOnboardingToken(eq("onb-1"), form.capture());
        assertFilePart(form.getValue(), "contract", "contract.bin", "application/octet-stream", new byte[]{7});
    }

    @Test
    void completeOperationsKeepTheUploadFailureWithoutRepeatingTheWrite() {
        // given
        UploadedFile contract = new UploadedFile("contract.pdf", "application/pdf", new byte[]{1});
        ProcessingException failure = new ProcessingException("upload failed");
        when(uploadClient.completeOnboardingToken(eq("onb-1"), any())).thenThrow(failure);
        when(uploadClient.completeOnboardingUsers(eq("onb-1"), any())).thenThrow(failure);

        // when
        UniAssertSubscriber<Void> token = onboardingService.onboardingTokenComplete("onb-1", contract)
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        UniAssertSubscriber<Void> users = onboardingService.onboardingUsersComplete("onb-1", contract)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        token.awaitFailure(Duration.ofSeconds(2));
        users.awaitFailure(Duration.ofSeconds(2));
        assertSame(failure, token.getFailure());
        assertSame(failure, users.getFailure());
        verify(uploadClient).completeOnboardingToken(eq("onb-1"), any());
        verify(uploadClient).completeOnboardingUsers(eq("onb-1"), any());
    }

    @SuppressWarnings("unchecked")
    private static void assertFilePart(ClientMultipartForm form, String partName, String fileName, String mediaType,
                                       byte[] content) {
        List<QuarkusMultipartFormDataPart> parts;
        try {
            Field field = ClientMultipartForm.class.getDeclaredField("parts");
            field.setAccessible(true);
            parts = (List<QuarkusMultipartFormDataPart>) field.get(form);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        assertEquals(1, parts.size());
        QuarkusMultipartFormDataPart part = parts.get(0);
        assertEquals(partName, part.name());
        assertEquals(fileName, part.filename());
        assertEquals(mediaType, part.mediaType());
        assertArrayEquals(content, part.content().getBytes());
    }

    private static InstitutionBaseRequest institutionBase() {
        return new InstitutionBaseRequest()
                .institutionType(org.openapi.quarkus.onboarding_json.model.InstitutionType.PA)
                .origin(Origin.IPA).originId("origin-id").digitalAddress("digital@address");
    }

    private static OnboardingData onboardingData(InstitutionType type) {
        OnboardingData data = new OnboardingData();
        data.setInstitutionType(type);
        return data;
    }

    private static OnboardingResponse onboardingResponse(String productId, String referenceOnboardingId,
                                                         InstitutionPaSubunitType subunitType) {
        InstitutionResponse institution = new InstitutionResponse();
        institution.setSubunitType(subunitType);
        OnboardingResponse response = new OnboardingResponse();
        response.setProductId(productId);
        response.setReferenceOnboardingId(referenceOnboardingId);
        response.setInstitution(institution);
        return response;
    }
}
