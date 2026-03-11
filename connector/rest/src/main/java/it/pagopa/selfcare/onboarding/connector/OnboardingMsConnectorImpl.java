package it.pagopa.selfcare.onboarding.connector;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.onboarding.common.InstitutionPaSubunitType;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.connector.api.OnboardingMsConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.InvalidRequestException;
import it.pagopa.selfcare.onboarding.connector.model.OnboardingResult;
import it.pagopa.selfcare.onboarding.connector.model.RecipientCodeStatusResult;
import it.pagopa.selfcare.onboarding.connector.model.institutions.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CheckManagerData;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.onboarding.connector.rest.client.*;
import it.pagopa.selfcare.onboarding.connector.rest.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class OnboardingMsConnectorImpl implements OnboardingMsConnector {

    public static final String PROD_IO = "prod-io";
    public static final String PROD_PAGOPA = "prod-pagopa";
    public static final String PROD_PN = "prod-pn";
    private final MsOnboardingApiClient msOnboardingApiClient;
    private final MsOnboardingBillingApiClient msOnboardingBillingApiClient;
    private final MsOnboardingSupportApiClient msOnboardingSupportApiClient;
    private final MsOnboardingTokenApiClient msOnboardingTokenApiClient;
    private final MsOnboardingAggregatesApiClient msOnboardingAggregatesApiClient;
    private final OnboardingMapper onboardingMapper;
    private final MsOnboardingInternalApiClient msOnboardingInternalApiClient;
    protected static final String REQUIRED_PRODUCT_ID_MESSAGE = "A product Id is required";

    public OnboardingMsConnectorImpl(MsOnboardingApiClient msOnboardingApiClient, MsOnboardingBillingApiClient msOnboardingBillingApiClient,
                                     MsOnboardingTokenApiClient msOnboardingTokenApiClient,
                                     MsOnboardingSupportApiClient msOnboardingSupportApiClient,
                                     MsOnboardingAggregatesApiClient msOnboardingAggregatesApiClient, OnboardingMapper onboardingMapper,
                                     MsOnboardingInternalApiClient msOnboardingInternalApiClient) {
        this.msOnboardingApiClient = msOnboardingApiClient;
        this.msOnboardingBillingApiClient = msOnboardingBillingApiClient;
        this.msOnboardingTokenApiClient = msOnboardingTokenApiClient;
        this.msOnboardingSupportApiClient = msOnboardingSupportApiClient;
        this.msOnboardingAggregatesApiClient = msOnboardingAggregatesApiClient;
        this.onboardingMapper = onboardingMapper;
        this.msOnboardingInternalApiClient = msOnboardingInternalApiClient;
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboarding(OnboardingData onboardingData) {
        if (onboardingData.getInstitutionType() == InstitutionType.PA) {
            msOnboardingApiClient._onboardingPa(onboardingMapper.toOnboardingPaRequest(onboardingData));
        } else if (onboardingData.getInstitutionType() == InstitutionType.PSP) {
            msOnboardingApiClient._onboardingPsp(onboardingMapper.toOnboardingPspRequest(onboardingData));
        } else {
            msOnboardingApiClient._onboarding(onboardingMapper.toOnboardingDefaultRequest(onboardingData));
        }
    }

    @Override
    public VerifyAggregateResult aggregatesVerification(MultipartFile file, String productId) {
        log.info("validateAggregatesCsv for product: {}", productId);
        switch (productId) {
            case PROD_IO -> {
                return onboardingMapper.toVerifyAggregateResult(msOnboardingAggregatesApiClient._verifyAppIoAggregatesCsv(file).getBody());
            }
            case PROD_PAGOPA -> {
                return onboardingMapper.toVerifyAggregateResult(msOnboardingAggregatesApiClient._verifyPagoPaAggregatesCsv(file).getBody());
            }
            case PROD_PN -> {
                return onboardingMapper.toVerifyAggregateResult(msOnboardingAggregatesApiClient._verifySendAggregatesCsv(file).getBody());
            }
            default -> {
                log.error("Unsupported productId: {}", productId);
                throw new InvalidRequestException(String.format("%s Unsupported productId: %s", HttpStatus.BAD_REQUEST, productId));
            }
        }
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingUsers(OnboardingData onboardingData) {
        msOnboardingApiClient._onboardingUsers(onboardingMapper.toOnboardingUsersRequest(onboardingData));
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingUsersAggregator(OnboardingData onboardingData) {
        msOnboardingApiClient._onboardingUsersAggregator(onboardingMapper.toOnboardingUsersRequest(onboardingData));
    }


    @Override
    @Retry(name = "retryTimeout")
    public void onboardingCompany(OnboardingData onboardingData) {
        msOnboardingApiClient._onboardingPgCompletion(onboardingMapper.toOnboardingPgRequest(onboardingData));
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingTokenComplete(String onboardingId, MultipartFile contract) {
        msOnboardingInternalApiClient._completeOnboardingUsingPUT(onboardingId, contract);
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingUsersComplete(String onboardingId, MultipartFile contract) {
        msOnboardingApiClient._completeOnboardingUser(onboardingId, contract);
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingPending(String onboardingId) {
        msOnboardingApiClient._getOnboardingPending(onboardingId);
    }

    @Override
    @Retry(name = "retryTimeout")
    public void approveOnboarding(String onboardingId) {
        msOnboardingApiClient._approve(onboardingId);
    }

    @Override
    @Retry(name = "retryTimeout")
    public void rejectOnboarding(String onboardingId, String reason) {
        ReasonRequest reasonForReject = new ReasonRequest();
        if (StringUtils.hasText(reason)) {
            reasonForReject.setReasonForReject(reason);
        }
        msOnboardingApiClient._delete(onboardingId, reasonForReject);
    }

    @Override
    @Retry(name = "retryTimeout")
    public OnboardingData getOnboarding(String onboardingId) {
        OnboardingGet onboardingGet = msOnboardingApiClient._getById(onboardingId).getBody();
        return onboardingMapper.toOnboardingData(onboardingGet);
    }

    @Override
    @Retry(name = "retryTimeout")
    public OnboardingData getOnboardingWithUserInfo(String onboardingId) {
        OnboardingGet onboardingGet = msOnboardingApiClient._getByIdWithUserInfo(onboardingId).getBody();
        return onboardingMapper.toOnboardingData(onboardingGet);
    }

    @Override
    @Retry(name = "retryTimeout")
    public Resource getContract(String onboardingId) {
        return msOnboardingTokenApiClient._getContract(onboardingId).getBody();
    }

    @Override
    @Retry(name = "retryTimeout")
    public Resource getTemplateAttachment(String onboardingId, String filename) {
        return msOnboardingTokenApiClient._getTemplateAttachment(onboardingId, filename).getBody();
    }

    @Override
    @Retry(name = "retryTimeout")
    public Resource getAttachment(String onboardingId, String filename) {
        return msOnboardingTokenApiClient._getAttachment(onboardingId, filename).getBody();
    }

    @Override
    @Retry(name = "retryTimeout")
    public Resource getAggregatesCsv(String onboardingId, String productId) {
        return msOnboardingAggregatesApiClient._getAggregatesCsv(onboardingId, productId).getBody();
    }

    @Override
    @Retry(name = "retryTimeout")
    public void onboardingPaAggregation(OnboardingData onboardingData) {
        msOnboardingApiClient._onboardingPaAggregation(onboardingMapper.toOnboardingPaAggregationRequest(onboardingData));
    }

    @Override
    public List<OnboardingData> getByFilters(String productId, String taxCode, String origin, String originId, String subunitCode) {
        List<OnboardingResponse> result = msOnboardingSupportApiClient._onboardingInstitutionUsingGET(origin, originId, OnboardingStatus.COMPLETED, subunitCode, taxCode).getBody();
        return Objects.nonNull(result) ? result.stream()
                // TODO this filter should be into query
                .filter(onboardingResponse -> {
                    if (Objects.isNull(subunitCode) && Objects.nonNull(onboardingResponse.getInstitution().getSubunitType())) {
                        return !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.UO.name())
                                && !onboardingResponse.getInstitution().getSubunitType().name().equals(InstitutionPaSubunitType.AOO.name());
                    }
                    return !StringUtils.hasText(onboardingResponse.getReferenceOnboardingId());
                })
                .filter(onboardingResponse -> onboardingResponse.getProductId().equals(productId))
                .map(onboardingMapper::toOnboardingData)
                .toList() : List.of();
    }

    @Override
    @Retry(name = "retryTimeout")
    public boolean checkManager(CheckManagerData checkManagerData) {
        return Objects.requireNonNull(msOnboardingApiClient._checkManager(onboardingMapper.toCheckManagerRequest(checkManagerData)).getBody()).getResponse();
    }

    @Override
    public RecipientCodeStatusResult checkRecipientCode(String originId, String recipientCode) {
        return onboardingMapper.toRecipientCodeStatusResult(msOnboardingBillingApiClient._checkRecipientCode(originId, recipientCode).getBody());
    }

    public void verifyOnboarding(String productId, String taxCode, String origin, String originId, String subunitCode, String institutionType) {
        log.trace("verifyOnboarding start");
        Assert.hasText(productId, REQUIRED_PRODUCT_ID_MESSAGE);
        msOnboardingApiClient._verifyOnboardingInfoByFilters(institutionType, origin, originId, productId, subunitCode, taxCode);
        log.trace("verifyOnboarding end");
    }

    @Override
    public void onboardingUsersPgFromIcAndAde(OnboardingData onboardingData) {
        log.trace("onboardingUsersPgFromIcAndAde start");
        msOnboardingApiClient._onboardingUsersPg(onboardingMapper.toOnboardingUserPgRequest(onboardingData));
        log.trace("onboardingUsersPgFromIcAndAde end");
    }

    @Override
    public List<OnboardingResult> onboardingWithFilter(String taxCode, String status) {
        log.trace("onboardingWithFilter start");
        ResponseEntity<OnboardingGetResponse> response = msOnboardingApiClient._getOnboardingWithFilter( null,
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
                null);
        List<OnboardingResult> onboardingResults = onboardingMapper.toOnboardingWithFilter(response.getBody());
        log.trace("onboardingWithFilter end");
        return onboardingResults;
    }

    @Override
    public void uploadAttachment(String onboardingId, MultipartFile attachment, String attachmentName) {
        msOnboardingTokenApiClient._uploadAttachment(onboardingId, attachmentName, attachment);
    }

    @Override
    public HttpStatusCode headAttachment(String onboardingId, String filename) {
        log.info("headAttachment for onboardingId: {}, filename: {}", Encode.forJava(onboardingId), Encode.forJava(filename));
        ResponseEntity<Void> responseEntity = msOnboardingTokenApiClient._headAttachment(onboardingId, filename);
        log.info("headAttachment response status code: {}", responseEntity.getStatusCode());
        return HttpStatus.resolve(responseEntity.getStatusCode().value());
    }

}
