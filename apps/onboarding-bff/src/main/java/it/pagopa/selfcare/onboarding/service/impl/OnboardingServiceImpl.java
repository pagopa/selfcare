package it.pagopa.selfcare.onboarding.service.impl;

import it.pagopa.selfcare.onboarding.service.*;

import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.util.FilePayloadUtils;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.exception.InternalGatewayErrorException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.exception.ResourceConflictException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.util.LogUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.onboarding_json.api.*;
import org.openapi.quarkus.onboarding_json.model.*;
import org.owasp.encoder.Encode;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class OnboardingServiceImpl implements OnboardingService {

    public static final String PROD_IO = "prod-io";
    public static final String PROD_PAGOPA = "prod-pagopa";
    public static final String PROD_PN = "prod-pn";
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    private final OnboardingControllerApi onboardingApi;
    private final BillingPortalApi billingPortalApi;
    private final SupportApi supportApi;
    private final DocumentContentControllerApi documentContentControllerApi;
    private final TokenControllerApi tokenApi;
    private final AggregatesControllerApi aggregatesApi;
    private final OnboardingMapper onboardingMapper;
    private final InternalV1Api internalV1Api;

    public OnboardingServiceImpl(@RestClient OnboardingControllerApi onboardingApi,
                                    @RestClient BillingPortalApi billingPortalApi,
                                    @RestClient DocumentContentControllerApi documentContentControllerApi,
                                    @RestClient TokenControllerApi tokenApi,
                                    @RestClient SupportApi supportApi,
                                    @RestClient AggregatesControllerApi aggregatesApi,
                                    OnboardingMapper onboardingMapper,
                                    @RestClient InternalV1Api internalV1Api) {
        this.onboardingApi = onboardingApi;
        this.billingPortalApi = billingPortalApi;
        this.documentContentControllerApi = documentContentControllerApi;
        this.tokenApi = tokenApi;
        this.supportApi = supportApi;
        this.aggregatesApi = aggregatesApi;
        this.onboardingMapper = onboardingMapper;
        this.internalV1Api = internalV1Api;
    }

    @Override
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

    @Override
    public VerifyAggregateResponse aggregatesVerification(UploadedFile file, String productId) {
        log.info("validateAggregatesCsv for product: {}", LogUtils.sanitize(productId));
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
                log.error("Unsupported productId: {}", LogUtils.sanitize(productId));
                throw new InvalidRequestException("Unsupported productId: " + productId);
            }
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsers(OnboardingData onboardingData) {
        onboardingApi.onboardingUsers(onboardingMapper.toOnboardingUsersRequest(onboardingData)).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsersAggregator(OnboardingData onboardingData) {
        onboardingApi.onboardingUsersAggregator(onboardingMapper.toOnboardingUsersRequest(onboardingData)).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingCompany(OnboardingData onboardingData) {
        onboardingApi.onboardingPgCompletion(onboardingMapper.toOnboardingPgRequest(onboardingData)).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingTokenComplete(String onboardingId, UploadedFile contract) {
        InternalV1Api.CompleteOnboardingUsingPUTMultipartForm form = new InternalV1Api.CompleteOnboardingUsingPUTMultipartForm();
        form.contract = FilePayloadUtils.toTempFile(contract, "internal-", ".bin");
        try {
            internalV1Api.completeOnboardingUsingPUT(form, onboardingId).await().indefinitely();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw new InvalidRequestException(String.format("Onboarding with id %s not found or it is expired!", onboardingId));
            }
            throw e;
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingUsersComplete(String onboardingId, UploadedFile contract) {
        OnboardingControllerApi.CompleteOnboardingUserMultipartForm form = new OnboardingControllerApi.CompleteOnboardingUserMultipartForm();
        form.contract = FilePayloadUtils.toTempFile(contract, "onboarding-", ".bin");
        try {
            onboardingApi.completeOnboardingUser(form, onboardingId).await().indefinitely();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw new InvalidRequestException(String.format("Onboarding with id %s not found or it is expired!", onboardingId));
            }
            throw e;
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingPending(String onboardingId) {
        onboardingApi.getOnboardingPending(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void approveOnboarding(String onboardingId) {
        try {
            onboardingApi.approve(onboardingId).await().indefinitely();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw new InvalidRequestException("Onboarding not found");
            }
            if (e.getResponse() != null && e.getResponse().getStatus() == 409) {
                throw new ResourceConflictException("Onboarding already consumed");
            }
            throw e;
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void rejectOnboarding(String onboardingId, String reason) {
        ReasonRequest reasonForReject = new ReasonRequest();
        if (reason != null && !reason.isBlank()) {
            reasonForReject.setReasonForReject(reason);
        }
        try {
            onboardingApi.delete(onboardingId, reasonForReject).await().indefinitely();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw new InvalidRequestException("Onboarding not found");
            }
            throw e;
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public OnboardingGet getOnboarding(String onboardingId) {
        return onboardingApi.getById(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public OnboardingGet getOnboardingWithUserInfo(String onboardingId) {
        return onboardingApi.getByIdWithUserInfo(onboardingId).await().indefinitely();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getContract(String onboardingId) {
        try {
            File file = documentContentControllerApi.getContract(onboardingId).await().indefinitely();
            return FilePayloadUtils.toBinaryData(file, file.getName());
        } catch (Exception e) {
            throw new InternalGatewayErrorException("Error retrieving contract from document service");
        }
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getTemplateAttachment(String onboardingId, String filename) {
        File file = tokenApi.getTemplateAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAttachment(String onboardingId, String filename) {
        File file = documentContentControllerApi.getAttachment(onboardingId, filename).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, filename);
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        File file = aggregatesApi.getAggregatesCsv(onboardingId, productId).await().indefinitely();
        return FilePayloadUtils.toBinaryData(file, file.getName());
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public void onboardingPaAggregation(OnboardingData onboardingData) {
        onboardingApi.onboardingPaAggregation(onboardingMapper.toOnboardingPaAggregationRequest(onboardingData)).await().indefinitely();
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
                    String referenceOnboardingId = onboardingResponse.getReferenceOnboardingId();
                    return referenceOnboardingId == null || referenceOnboardingId.isBlank();
                })
                .filter(onboardingResponse -> onboardingResponse.getProductId().equals(productId))
                .toList() : List.of();
    }

    @Override
    @Retry(maxRetries = 2, delay = 5000)
    public CheckManagerResponse checkManager(CheckManagerRequest request) {
        return onboardingApi.checkManager(request).await().indefinitely();
    }

    @Override
    public RecipientCodeStatus checkRecipientCode(String originId, String recipientCode) {
        return billingPortalApi.checkRecipientCode(originId, recipientCode).await().indefinitely();
    }

    @Override
    public void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType) {
        log.trace("verifyOnboarding start");
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(REQUIRED_PRODUCT_ID_MESSAGE);
        }
        try {
            onboardingApi.verifyOnboardingInfoByFilters(institutionType, origin, originId, productId, subunitCode, taxCode)
                    .await().indefinitely();
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw new ResourceNotFoundException("Onboarding not found");
            }
            throw e;
        }
        log.trace("verifyOnboarding end");
    }

    @Override
    public void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        onboardingApi.onboardingUsersPg(onboardingMapper.toOnboardingUserPgRequest(onboardingData)).await().indefinitely();
        log.trace("onboardingUsersPgFromIcAndAde end");
    }

    @Override
    public OnboardingGetResponse onboardingWithFilter(String taxCode, String status) {
        log.trace("onboardingWithFilter start");
        OnboardingStatus onboardingStatus = parseOnboardingStatus(status);
        OnboardingGetResponse response = onboardingApi.getOnboardingWithFilter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                onboardingStatus,
                null,
                taxCode,
                null,
                null).await().indefinitely();
        log.trace("onboardingWithFilter end");
        return response;
    }

    @Override
    public void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName) {
        TokenControllerApi.UploadAttachmentMultipartForm form = new TokenControllerApi.UploadAttachmentMultipartForm();
        form._file = FilePayloadUtils.toTempFile(attachment, "token-", ".bin");
        tokenApi.uploadAttachment(form, onboardingId, attachmentName).await().indefinitely();
    }

    @Override
    public int headAttachment(String onboardingId, String filename) {
        log.info("headAttachment for onboardingId: {}, filename: {}", Encode.forJava(onboardingId), Encode.forJava(filename));
        int statusCode = tokenApi.headAttachment(onboardingId, filename).await().indefinitely().getStatus();
        log.info("headAttachment response status code: {}", statusCode);
        return statusCode;
    }

    private OnboardingStatus parseOnboardingStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OnboardingStatus.fromString(status);
        } catch (IllegalArgumentException ex) {
            String allowedValues = Arrays.stream(OnboardingStatus.values())
                    .map(OnboardingStatus::value)
                    .collect(Collectors.joining(", ", "[", "]"));
            throw new InvalidRequestException(String.format(
                    "Invalid status '%s'. Allowed values: %s",
                    status,
                    allowedValues));
        }
    }
}
