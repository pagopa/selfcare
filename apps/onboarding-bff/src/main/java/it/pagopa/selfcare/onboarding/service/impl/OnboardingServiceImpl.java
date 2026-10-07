package it.pagopa.selfcare.onboarding.service.impl;

import io.vertx.core.buffer.Buffer;
import it.pagopa.selfcare.onboarding.client.OnboardingUploadRestClient;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.client.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.ClientRequestValidator;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import it.pagopa.selfcare.onboarding.util.Preconditions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.client.api.ClientMultipartForm;
import org.openapi.quarkus.onboarding_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_json.api.SupportApi;
import org.openapi.quarkus.onboarding_json.model.ApproveRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingStatus;
import org.openapi.quarkus.onboarding_json.model.ReasonRequest;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Onboarding-ms facade. Only the operations that were retried by the former connector are retried here, and only on
 * connection problems or timeouts: a downstream HTTP answer is never replayed.
 */
@ApplicationScoped
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";
    static final String PROD_IO = "prod-io";
    static final String PROD_PAGOPA = "prod-pagopa";
    static final String PROD_PN = "prod-pn";
    private static final String CONTRACT_PART = "contract";
    private static final String AGGREGATES_PART = "aggregates";

    private final OnboardingControllerApi onboardingApi;
    private final SupportApi supportApi;
    private final OnboardingUploadRestClient uploadClient;
    private final OnboardingMapper onboardingMapper;
    private final ClientRequestValidator requestValidator;

    public OnboardingServiceImpl(@RestClient OnboardingControllerApi onboardingApi,
                                 @RestClient SupportApi supportApi,
                                 @RestClient OnboardingUploadRestClient uploadClient,
                                 OnboardingMapper onboardingMapper,
                                 ClientRequestValidator requestValidator) {
        this.onboardingApi = onboardingApi;
        this.supportApi = supportApi;
        this.uploadClient = uploadClient;
        this.onboardingMapper = onboardingMapper;
        this.requestValidator = requestValidator;
    }

    private <T> T validated(String clientMethod, String parameter, T request) {
        return requestValidator.validated(clientMethod, parameter, request);
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboarding(OnboardingData onboardingData) {
        if (onboardingData.getInstitutionType() == InstitutionType.PA) {
            onboardingApi.onboardingPa(validated("_onboardingPa", "onboardingPaRequest", onboardingMapper.toOnboardingPaRequest(onboardingData))).await().indefinitely();
        } else if (onboardingData.getInstitutionType() == InstitutionType.PSP) {
            onboardingApi.onboardingPsp(validated("_onboardingPsp", "onboardingPspRequest", onboardingMapper.toOnboardingPspRequest(onboardingData))).await().indefinitely();
        } else {
            onboardingApi.onboarding(validated("_onboarding", "onboardingDefaultRequest", onboardingMapper.toOnboardingDefaultRequest(onboardingData))).await().indefinitely();
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingUsers(OnboardingData onboardingData) {
        onboardingApi.onboardingUsers(validated("_onboardingUsers", "onboardingUserRequest", onboardingMapper.toOnboardingUsersRequest(onboardingData))).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingUsersAggregator(OnboardingData onboardingData) {
        onboardingApi.onboardingUsersAggregator(validated("_onboardingUsersAggregator", "onboardingUserRequest", onboardingMapper.toOnboardingUsersRequest(onboardingData))).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingCompany(OnboardingData onboardingData) {
        onboardingApi.onboardingPgCompletion(validated("_onboardingPgCompletion", "onboardingPgRequest", onboardingMapper.toOnboardingPgRequest(onboardingData))).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingTokenComplete(String onboardingId, UploadedFile contract) {
        uploadClient.completeOnboardingToken(onboardingId, multipart(CONTRACT_PART, contract)).close();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingUsersComplete(String onboardingId, UploadedFile contract) {
        uploadClient.completeOnboardingUsers(onboardingId, multipart(CONTRACT_PART, contract)).close();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingPending(String onboardingId) {
        onboardingApi.getOnboardingPending(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void approveOnboarding(String onboardingId, String userUid) {
        ApproveRequest approveRequest = new ApproveRequest();
        if (StringUtils.isNotBlank(userUid)) {
            approveRequest.setUserUid(userUid);
        }
        onboardingApi.approve(onboardingId, approveRequest).await().indefinitely().close();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void rejectOnboarding(String onboardingId, String reason, String userUid) {
        ReasonRequest reasonForReject = new ReasonRequest();
        if (StringUtils.isNotBlank(reason)) {
            reasonForReject.setReasonForReject(reason);
        }
        if (StringUtils.isNotBlank(userUid)) {
            reasonForReject.setUserUid(userUid);
        }
        onboardingApi.rejectOnboardingUsingPUT(onboardingId, reasonForReject).await().indefinitely().close();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public OnboardingGet getOnboarding(String onboardingId) {
        return onboardingApi.getById(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public OnboardingGet getOnboardingWithUserInfo(String onboardingId) {
        return onboardingApi.getByIdWithUserInfo(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void onboardingPaAggregation(OnboardingData onboardingData) {
        onboardingApi.onboardingPaAggregation(validated("_onboardingPaAggregation", "onboardingPaRequest", onboardingMapper.toOnboardingPaAggregationRequest(onboardingData))).await().indefinitely();
    }

    @Override
    public List<OnboardingResponse> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode) {
        List<OnboardingResponse> result = supportApi.onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, subunitCode, taxCode)
                .await().indefinitely();
        return Objects.nonNull(result) ? result.stream()
                .filter(onboardingResponse -> {
                    if (Objects.isNull(subunitCode) && Objects.nonNull(onboardingResponse.getInstitution().getSubunitType())) {
                        return !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.UO.name())
                                && !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.AOO.name());
                    }
                    return StringUtils.isBlank(onboardingResponse.getReferenceOnboardingId());
                })
                .filter(onboardingResponse -> onboardingResponse.getProductId().equals(productId))
                .toList() : List.of();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public boolean checkManager(CheckManagerRequest request) {
        CheckManagerResponse response = onboardingApi.checkManager(validated("_checkManager", "checkManagerRequest", request)).await().indefinitely();
        return Objects.requireNonNull(response).getResponse();
    }

    @Override
    public RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode) {
        return onboardingMapper.toRecipientCodeStatusResult(onboardingApi.checkRecipientCode(originId, recipientCode).await().indefinitely());
    }

    @Override
    public void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType) {
        log.trace("verifyOnboarding start");
        Preconditions.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        onboardingApi.verifyOnboardingInfoByFilters(institutionType, origin, originId, productId, subunitCode, taxCode)
                .await().indefinitely().close();
        log.trace("verifyOnboarding end");
    }

    @Override
    public void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        onboardingApi.onboardingUsersPg(validated("_onboardingUsersPg", "onboardingUserPgRequest", onboardingMapper.toOnboardingUserPgRequest(onboardingData))).await().indefinitely();
        log.trace("onboardingUsersPgFromIcAndAde end");
    }

    @Override
    public List<OnboardingResult> onboardingWithFilter(String taxCode, String status) {
        log.trace("onboardingWithFilter start");
        OnboardingGetResponse response = onboardingApi.getOnboardingWithFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                parseOnboardingStatus(status),
                null,
                taxCode,
                null,
                null).await().indefinitely();
        List<OnboardingResult> results = onboardingMapper.toOnboardingWithFilter(response);
        log.trace("onboardingWithFilter end");
        return results;
    }

    @Override
    public VerifyAggregateResult aggregatesVerification(UploadedFile file, String productId) {
        log.info("validateAggregatesCsv for product: {}", productId);
        switch (productId) {
            case PROD_IO, PROD_PAGOPA, PROD_PN -> {
                VerifyAggregateResponse response = uploadClient.verifyAggregatesCsv(productId, multipart(AGGREGATES_PART, file));
                return onboardingMapper.toVerifyAggregateResult(response);
            }
            default -> {
                log.error("Unsupported productId: {}", productId);
                throw new InvalidRequestException(String.format("%s Unsupported productId: %s", "400 BAD_REQUEST", productId));
            }
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000, delayUnit = ChronoUnit.MILLIS, jitter = 0, retryOn = {ProcessingException.class, IOException.class})
    public void triggerOnboardingRequest(String onboardingId) {
        log.trace("triggerOnboardingRequest start");
        onboardingApi.triggerDocumentGate(onboardingId).await().indefinitely().close();
        log.trace("triggerOnboardingRequest end");
    }

    // The downstream contract is strict, unlike the case-insensitive generated fromString
    private static OnboardingStatus parseOnboardingStatus(String status) {
        for (OnboardingStatus candidate : OnboardingStatus.values()) {
            if (candidate.value().equals(status)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + status + "'");
    }

    private static ClientMultipartForm multipart(String partName, UploadedFile file) {
        String contentType = file.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : file.contentType();
        return ClientMultipartForm.create()
                .binaryFileUpload(partName, file.fileName(), Buffer.buffer(file.content()), contentType);
    }
}
