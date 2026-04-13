package it.pagopa.selfcare.onboarding.connector;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.BinaryData;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.OnboardingMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.AggregatesControllerApi;
import org.openapi.quarkus.onboarding_json.api.BillingPortalApi;
import org.openapi.quarkus.onboarding_json.api.InternalV1Api;
import org.openapi.quarkus.onboarding_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_json.api.SupportApi;
import org.openapi.quarkus.onboarding_json.api.TokenControllerApi;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingStatus;
import org.openapi.quarkus.onboarding_json.model.ReasonRequest;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Slf4j
public class OnboardingMsConnectorImpl {

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

    public OnboardingMsConnectorImpl(@RestClient OnboardingControllerApi onboardingApi,
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
    public VerifyAggregateResult aggregatesVerification(UploadedFile file, String productId) {
        log.info("validateAggregatesCsv for product: {}", productId);
        switch (productId) {
            case PROD_IO -> {
                AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifyAppIoAggregatesCsvMultipartForm();
                form.aggregates = toTempFile(file, "aggregates-", ".csv");
                VerifyAggregateResponse response = aggregatesApi.verifyAppIoAggregatesCsv(form).await().indefinitely();
                return onboardingMapper.toVerifyAggregateResult(response);
            }
            case PROD_PAGOPA -> {
                AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifyPagoPaAggregatesCsvMultipartForm();
                form.aggregates = toTempFile(file, "aggregates-", ".csv");
                VerifyAggregateResponse response = aggregatesApi.verifyPagoPaAggregatesCsv(form).await().indefinitely();
                return onboardingMapper.toVerifyAggregateResult(response);
            }
            case PROD_PN -> {
                AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm form =
                    new AggregatesControllerApi.VerifySendAggregatesCsvMultipartForm();
                form.aggregates = toTempFile(file, "aggregates-", ".csv");
                VerifyAggregateResponse response = aggregatesApi.verifySendAggregatesCsv(form).await().indefinitely();
                return onboardingMapper.toVerifyAggregateResult(response);
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
        form.contract = toTempFile(contract, "internal-", ".bin");
        internalV1Api.completeOnboardingUsingPUT(form, onboardingId).await().indefinitely();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsersComplete(String onboardingId, UploadedFile contract) {
        OnboardingControllerApi.CompleteOnboardingUserMultipartForm form = new OnboardingControllerApi.CompleteOnboardingUserMultipartForm();
        form.contract = toTempFile(contract, "onboarding-", ".bin");
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
    public OnboardingData getOnboarding(String onboardingId) {
        OnboardingGet onboardingGet = onboardingApi.getById(onboardingId).await().indefinitely();
        return onboardingMapper.toOnboardingData(onboardingGet);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public OnboardingData getOnboardingWithUserInfo(String onboardingId) {
        OnboardingGet onboardingGet = onboardingApi.getByIdWithUserInfo(onboardingId).await().indefinitely();
        return onboardingMapper.toOnboardingData(onboardingGet);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getContract(String onboardingId) {
        File file = tokenApi.getContract(onboardingId).await().indefinitely();
        return toBinaryData(file, file.getName());
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getTemplateAttachment(String onboardingId, String filename) {
        File file = tokenApi.getTemplateAttachment(onboardingId, filename).await().indefinitely();
        return toBinaryData(file, filename);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAttachment(String onboardingId, String filename) {
        File file = tokenApi.getAttachment(onboardingId, filename).await().indefinitely();
        return toBinaryData(file, filename);
    }
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        File file = aggregatesApi.getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return toBinaryData(file, file.getName());
    }
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingPaAggregation(OnboardingData onboardingData) {
        onboardingApi.onboardingPaAggregation(onboardingMapper.toOnboardingPaAggregationRequest(onboardingData)).await().indefinitely();
    }
    public List<OnboardingData> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode) {
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
                .map(onboardingMapper::toOnboardingData)
                .toList() : List.of();
    }
    @Retry(maxRetries = 2, delay = 5000)
    public boolean checkManager(CheckManagerData checkManagerData) {
        CheckManagerResponse response = onboardingApi.checkManager(onboardingMapper.toCheckManagerRequest(checkManagerData)).await().indefinitely();
        return Objects.requireNonNull(response).getResponse();
    }
    public RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode) {
        return onboardingMapper.toRecipientCodeStatusResult(billingPortalApi.checkRecipientCode(originId, recipientCode).await().indefinitely());
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
    public List<OnboardingResult> onboardingWithFilter(String taxCode, String status) {
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
        List<OnboardingResult> onboardingResults = onboardingMapper.toOnboardingWithFilter(response);
        log.trace("onboardingWithFilter end");
        return onboardingResults;
    }
    public void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName) {
        TokenControllerApi.UploadAttachmentMultipartForm form = new TokenControllerApi.UploadAttachmentMultipartForm();
        form._file = toTempFile(attachment, "token-", ".bin");
        tokenApi.uploadAttachment(form, onboardingId, attachmentName).await().indefinitely();
    }
    public int headAttachment(String onboardingId, String filename) {
        log.info("headAttachment for onboardingId: {}, filename: {}", Encode.forJava(onboardingId), Encode.forJava(filename));
        int statusCode = tokenApi.headAttachment(onboardingId, filename).await().indefinitely().getStatus();
        log.info("headAttachment response status code: {}", statusCode);
        return statusCode;
    }

    private static BinaryData toBinaryData(File file, String fallbackName) {
        try {
            String fileName = fallbackName == null || fallbackName.isBlank() ? file.getName() : fallbackName;
            return new BinaryData(fileName, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read downloaded file", e);
        }
    }

    private static File toTempFile(UploadedFile uploadedFile, String prefix, String defaultExtension) {
        try {
            String fileName = uploadedFile.fileName();
            String suffix = (fileName == null || fileName.isBlank()) ? defaultExtension : "-" + fileName;
            File file = Files.createTempFile(prefix, suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }

}
