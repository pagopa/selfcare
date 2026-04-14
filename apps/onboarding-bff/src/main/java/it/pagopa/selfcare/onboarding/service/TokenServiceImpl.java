package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.onboarding.service.DocumentService;
import it.pagopa.selfcare.onboarding.client.model.BinaryData;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import java.util.Objects;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final OnboardingService onboardingMsConnector;
    private final DocumentService documentMsClient;
    private final ProductService productService;
    private final OnboardingMapper onboardingMapper;

    private static final String ONBOARDING_ID_REQUIRED_MESSAGE = "OnboardingId is required";
    private static final String TOKEN_ID_IS_REQUIRED = "TokenId is required";

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
        BinaryData resource = documentMsClient.getContract(onboardingId);
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

        OnboardingData onboarding = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));
        Product product = productService.getProductValid(onboarding.getProductId());
        String templatePath = getAttachmentTemplate(filename, onboarding, product).getTemplatePath();

        BinaryData resource = documentMsClient.getTemplateAttachment(
                onboarding.getId(),
                onboarding.getInstitutionUpdate().getDescription(),
                filename,
                onboarding.getProductId(),
                templatePath);
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
        BinaryData resource = documentMsClient.getAttachment(onboardingId, filename);
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
        BinaryData resource = documentMsClient.getAggregatesCsv(onboardingId, productId);
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

        OnboardingData onboarding = onboardingMapper.toOnboardingData(onboardingMsConnector.getOnboarding(onboardingId));
        Product product = productService.getProductValid(onboarding.getProductId());
        AttachmentTemplate template = getAttachmentTemplate(attachmentName, onboarding, product);

        documentMsClient.uploadAttachment(onboardingId, attachment, attachmentName, product.getId(), template);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
    }

    @Override
    public int headAttachment(String onboardingId, String filename) {
        log.trace("headAttachment start");
        log.debug("headAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Objects.requireNonNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Objects.requireNonNull(filename, "filename is required");
        int resource = documentMsClient.headAttachment(onboardingId, filename);
        log.debug("headAttachment result {}", resource);
        log.trace("headAttachment end");
        return resource;
    }

    private AttachmentTemplate getAttachmentTemplate(String attachmentName, OnboardingData onboarding, Product product) {
        return product
                .getInstitutionContractMappings()
                .get(onboarding.getInstitutionType().name())
                .getAttachments()
                .stream()
                .filter(a -> attachmentName.equals(a.getName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Attachment with name %s not found", attachmentName)));
    }
}
