package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.client.OnboardingMsClient;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class TokenServiceImpl implements TokenService {

    private final OnboardingMsClient onboardingMsConnector;
    private final OnboardingMapper onboardingMapper;

    private static final String ONBOARDING_ID_REQUIRED_MESSAGE = "OnboardingId is required";
    private static final String TOKEN_ID_IS_REQUIRED = "TokenId is required";

    public TokenServiceImpl(OnboardingMsClient onboardingMsConnector, OnboardingMapper onboardingMapper) {
        this.onboardingMsConnector = onboardingMsConnector;
        this.onboardingMapper = onboardingMapper;
    }

    @Override
    public OnboardingData verifyOnboarding(String onboardingId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));
        log.debug("verifyOnboarding result = success");
        log.trace("verifyOnboarding end");
        return onboardingData;
    }

    @Override
    public void approveOnboarding(String onboardingId) {
        log.trace("approveOnboarding start");
        log.debug("approveOnboarding id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.approveOnboarding(onboardingId);
        log.debug("approveOnboarding result = success");
        log.trace("approveOnboarding end");
    }

    @Override
    public void rejectOnboarding(String onboardingId, String reason) {
        log.trace("rejectOnboarding start");
        log.debug("rejectOnboarding id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.rejectOnboarding(onboardingId, reason);
        log.debug("rejectOnboarding result = success");
        log.trace("rejectOnboarding end");
    }

    @Override
    public OnboardingData getOnboardingWithUserInfo(String onboardingId) {
        log.trace("getOnboardingWithUserInfo start");
        log.debug("getOnboardingWithUserInfo id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboardingWithUserInfo(onboardingId));
        log.debug("getOnboardingWithUserInfo result = success");
        log.trace("getOnboardingWithUserInfo end");
        return onboardingData;
    }

    @Override
    public void completeTokenV2(String onboardingId, UploadedFile contract) {
        log.trace("completeTokenAsync start");
        log.debug("completeTokenAsync id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        onboardingMsConnector.onboardingTokenComplete(onboardingId, contract);
        log.debug("completeTokenAsync result = success");
        log.trace("completeTokenAsync end");
    }

    @Override
    public void completeOnboardingUsers(String onboardingId, UploadedFile contract) {
        log.trace("completeOnboardingUsersAsync start");
        log.debug("completeOnboardingUsersAsync id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.onboardingUsersComplete(onboardingId, contract);
        log.debug("completeOnboardingUsersAsync result = success");
        log.trace("completeOnboardingUsersAsync end");
    }

    @Override
    public BinaryData getContract(String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract id = {}", onboardingId);
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        BinaryData resource = onboardingMsConnector.getContract(onboardingId);
        log.debug("getContract result = success");
        log.trace("getContract end");
        return resource;
    }

    @Override
    public BinaryData getTemplateAttachment(String onboardingId, String filename) {
        log.trace("getTemplateAttachment start");
        log.debug("getTemplateAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        BinaryData resource = onboardingMsConnector.getTemplateAttachment(onboardingId, filename);
        log.debug("getTemplateAttachment result = success");
        log.trace("getTemplateAttachment end");
        return resource;
    }

    @Override
    public BinaryData getAttachment(String onboardingId, String filename) {
        log.trace("getAttachment start");
        log.debug("getAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        BinaryData resource = onboardingMsConnector.getAttachment(onboardingId, filename);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
        return resource;
    }

    @Override
    public BinaryData getAggregatesCsv(String onboardingId, String productId) {
        log.trace("getAggregatesCsv start");
        log.debug("getAggregatesCsv id = {}, productId = {}", Encode.forJava(onboardingId), Encode.forJava(productId));
        Objects.requireNonNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        Objects.requireNonNull(productId, "ProductId is required");
        BinaryData resource = onboardingMsConnector.getAggregatesCsv(onboardingId, productId);
        log.debug("getAggregatesCsv result = success");
        log.trace("getAggregatesCsv end");
        return resource;
    }

  @Override
  public boolean verifyAllowedUserByRole(String onboardingId, String uid) {
    log.trace("verifyAllowedUserRole for {} - {}", onboardingId, uid);
    OnboardingData onboardingData = getOnboardingWithUserInfo(onboardingId);
    return onboardingData.getUsers().stream().anyMatch(user -> uid.equalsIgnoreCase(user.getId()));
  }

    @Override
    public void uploadAttachment(String onboardingId, UploadedFile attachment, String attachmentName) {
        log.trace("uploadAttachment start");
        log.debug("uploadAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(attachmentName));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(attachmentName, "filename is required");
        Objects.requireNonNull(attachment, "file is required");
        onboardingMsConnector.uploadAttachment(onboardingId, attachment, attachmentName);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
    }

    @Override
    public int headAttachment(String onboardingId, String filename) {
        log.trace("headAttachment start");
        log.debug("headAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        int resource = onboardingMsConnector.headAttachment(onboardingId, filename);
        log.debug("headAttachment result {}", resource);
        log.trace("headAttachment end");
        return resource;
    }
}
