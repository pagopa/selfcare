package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.util.FilePayloadUtils;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.onboarding_json.api.*;
import org.openapi.quarkus.onboarding_json.model.*;
import org.owasp.encoder.Encode;

import java.io.File;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class OnboardingMsClient {

    public static final String PROD_IO = "prod-io";
    public static final String PROD_PAGOPA = "prod-pagopa";
    public static final String PROD_PN = "prod-pn";
    private final OnboardingControllerApi onboardingApi;
    private final BillingPortalApi billingPortalApi;
    private final SupportApi supportApi;
    private final TokenControllerApi tokenApi;
    private final AggregatesControllerApi aggregatesApi;
    private final OnboardingMapper onboardingMapper;
    private final InternalV1Api internalV1Api;
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    public OnboardingMsClient(@RestClient OnboardingControllerApi onboardingApi,
                                     @RestClient BillingPortalApi billingPortalApi,
                                     @RestClient TokenControllerApi tokenApi,
                                     @RestClient SupportApi supportApi,
                                     @RestClient AggregatesControllerApi aggregatesApi,
                                     OnboardingMapper onboardingMapper,
                                     @RestClient InternalV1Api internalV1Api) {
        this.onboardingApi = onboardingApi;
        this.billingPortalApi = billingPortalApi;
        this.tokenApi = tokenApi;
        this.supportApi = supportApi;
        this.aggregatesApi = aggregatesApi;
        this.onboardingMapper = onboardingMapper;
        this.internalV1Api = internalV1Api;
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboarding(OnboardingData onboardingData) {
        if (onboardingData.getInstitutionType() == InstitutionType.PA) {
            onboardingApi.onboardingPa(onboardingMapper.toOnboardingPaRequest(onboardingData)).await().indefinitely();
        } else if (onboardingData.getInstitutionType() == InstitutionType.PSP) {
            onboardingApi.onboardingPsp(onboardingMapper.toOnboardingPspRequest(onboardingData)).await().indefinitely();
        } else {
            onboardingApi.onboarding(onboardingMapper.toOnboardingDefaultRequest(onboardingData)).await().indefinitely();
        }
    }
    public VerifyAggregateResponse aggregatesVerification(UploadedFile file, String productId) {
        log.info("validateAggregatesCsv for product: {}", productId);
        switch (productId) {
            case PROD_IO -> {
                AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm();
                form.aggregates = FilePayloadUtils.toTempFile(file, "aggregates-", ".csv");
                return aggregatesApi.verifyAppIoAggregatesCsv(form).await().indefinitely();
            }
            case PROD_PAGOPA -> {
                AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm();
                form.aggregates = FilePayloadUtils.toTempFile(file, "aggregates-", ".csv");
                return aggregatesApi.verifyPagoPaAggregatesCsv(form).await().indefinitely();
            }
            case PROD_PN -> {
                AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm();
                form.aggregates = FilePayloadUtils.toTempFile(file, "aggregates-", ".csv");
                return aggregatesApi.verifySendAggregatesCsv(form).await().indefinitely();
            }
            default -> {
                log.error("Unsupported productId: {}", productId);
                throw new InvalidRequestException("Unsupported productId: " + productId);
            }
        }
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsers(OnboardingData onboardingData) {
        onboardingApi.onboardingUsers(onboardingMapper.toOnboardingUsersRequest(onboardingData)).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsersAggregator(OnboardingData onboardingData) {
        onboardingApi.onboardingUsersAggregator(onboardingMapper.toOnboardingUsersRequest(onboardingData)).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingCompany(OnboardingData onboardingData) {
        onboardingApi.onboardingPgCompletion(onboardingMapper.toOnboardingPgRequest(onboardingData)).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingTokenComplete(String onboardingId, UploadedFile contract) {
        InternalV1Api.CompleteOnboardingUsingPUTMultipartForm form = new InternalV1Api.CompleteOnboardingUsingPUTMultipartForm();
        form.contract = FilePayloadUtils.toTempFile(contract, "internal-", ".bin");
        internalV1Api.completeOnboardingUsingPUT(form, onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsersComplete(String onboardingId, UploadedFile contract) {
        OnboardingControllerApi.CompleteOnboardingUserMultipartForm form = new OnboardingControllerApi.CompleteOnboardingUserMultipartForm();
        form.contract = FilePayloadUtils.toTempFile(contract, "onboarding-", ".bin");
        onboardingApi.completeOnboardingUser(form, onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingPending(String onboardingId) {
        onboardingApi.getOnboardingPending(onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void approveOnboarding(String onboardingId) {
        onboardingApi.approve(onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void rejectOnboarding(String onboardingId, String reason) {
        ReasonRequest reasonForReject = new ReasonRequest();
        if (reason != null && !reason.isBlank()) {
            reasonForReject.setReasonForReject(reason);
        }
        onboardingApi.delete(onboardingId, reasonForReject).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public OnboardingGet getOnboarding(String onboardingId) {
        return onboardingApi.getById(onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public OnboardingGet getOnboardingWithUserInfo(String onboardingId) {
        return onboardingApi.getByIdWithUserInfo(onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getContract(String onboardingId) {
        File file = tokenApi.getContract(onboardingId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getTemplateAttachment(String onboardingId, String filename) {
        File file = tokenApi.getTemplateAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAttachment(String onboardingId, String filename) {
        File file = tokenApi.getAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        File file = aggregatesApi.getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingPaAggregation(OnboardingData onboardingData) {
        onboardingApi.onboardingPaAggregation(onboardingMapper.toOnboardingPaAggregationRequest(onboardingData)).await().indefinitely();
    }
    public List<OnboardingResponse> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode) {
        List<OnboardingResponse> result = supportApi.onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, subunitCode, taxCode)
            .await().indefinitely();
        return Objects.nonNull(result) ? result.stream()
                // TODO this filter should be into query
                .filter(onboardingResponse -> {
                    if (Objects.isNull(subunitCode) && Objects.nonNull(onboardingResponse.getInstitution().getSubunitType())) {
                        return !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.UO.name())
                                && !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.AOO.name());
                    }
                    String referenceOnboardingId = onboardingResponse.getReferenceOnboardingId();
                    return referenceOnboardingId == null || referenceOnboardingId.isBlank();
                })
                .filter(onboardingResponse -> onboardingResponse.getProductId().equals(productId))
                .toList() : List.of();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public CheckManagerResponse checkManager(CheckManagerRequest request) {
        return onboardingApi.checkManager(request).await().indefinitely();
    }
    public RecipientCodeStatus checkRecipientCode(String originId, String recipientCode) {
        return billingPortalApi.checkRecipientCode(originId, recipientCode).await().indefinitely();
    }

    public void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType) {
        log.trace("verifyOnboarding start");
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_PRODUCT_ID_MESSAGE);
        }
        onboardingApi.verifyOnboardingInfoByFilters(institutionType, origin, originId, productId, subunitCode, taxCode)
            .await().indefinitely();
        log.trace("verifyOnboarding end");
    }
    public void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        onboardingApi.onboardingUsersPg(onboardingMapper.toOnboardingUserPgRequest(onboardingData)).await().indefinitely();
        log.trace("onboardingUsersPgFromIcAndAde end");
    }
    public OnboardingGetResponse onboardingWithFilter(String taxCode, String status) {
        log.trace("onboardingWithFilter start");
        OnboardingGetResponse response = onboardingApi.getOnboardingWithFilter( null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                taxCode,
                null,
                null).await().indefinitely();
        log.trace("onboardingWithFilter end");
        return response;
    }
    public void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName) {
        TokenControllerApi.UploadAttachmentMultipartForm form = new TokenControllerApi.UploadAttachmentMultipartForm();
        form._file = FilePayloadUtils.toTempFile(attachment, "token-", ".bin");
        tokenApi.uploadAttachment(form, onboardingId, attachmentName).await().indefinitely();
    }
    public int headAttachment(String onboardingId, String filename) {
        log.info("headAttachment for onboardingId: {}, filename: {}", Encode.forJava(onboardingId), Encode.forJava(filename));
        int statusCode = tokenApi.headAttachment(onboardingId, filename).await().indefinitely().getStatus();
        log.info("headAttachment response status code: {}", statusCode);
        return statusCode;
    }

}
