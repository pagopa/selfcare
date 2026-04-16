package it.pagopa.selfcare.onboarding.core;

import it.pagopa.selfcare.onboarding.connector.api.DocumentMsConnector;
import it.pagopa.selfcare.onboarding.connector.api.OnboardingMsConnector;
import it.pagopa.selfcare.onboarding.connector.exceptions.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingData;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final OnboardingMsConnector onboardingMsConnector;
    private final DocumentMsConnector documentMsConnector;
    private final ProductAzureService productAzureService;

    private static final String ONBOARDING_ID_REQUIRED_MESSAGE = "OnboardingId is required";
    private static final String TOKEN_ID_IS_REQUIRED = "TokenId is required";

    public TokenServiceImpl(OnboardingMsConnector onboardingMsConnector,
                            DocumentMsConnector documentMsConnector,
                            ProductAzureService productAzureService) {
        this.onboardingMsConnector = onboardingMsConnector;
        this.documentMsConnector = documentMsConnector;
        this.productAzureService = productAzureService;
    }

    @Override
    public OnboardingData verifyOnboarding(String onboardingId) {
        log.trace("verifyOnboarding start");
        log.debug("verifyOnboarding id = {}", onboardingId);
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMsConnector.getOnboarding(onboardingId);
        log.debug("verifyOnboarding result = success");
        log.trace("verifyOnboarding end");
        return onboardingData;
    }

    @Override
    public void approveOnboarding(String onboardingId) {
        log.trace("approveOnboarding start");
        log.debug("approveOnboarding id = {}", onboardingId);
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.approveOnboarding(onboardingId);
        log.debug("approveOnboarding result = success");
        log.trace("approveOnboarding end");
    }

    @Override
    public void rejectOnboarding(String onboardingId, String reason) {
        log.trace("rejectOnboarding start");
        log.debug("rejectOnboarding id = {}", onboardingId);
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.rejectOnboarding(onboardingId, reason);
        log.debug("rejectOnboarding result = success");
        log.trace("rejectOnboarding end");
    }

    @Override
    public OnboardingData getOnboardingWithUserInfo(String onboardingId) {
        log.trace("getOnboardingWithUserInfo start");
        log.debug("getOnboardingWithUserInfo id = {}", onboardingId);
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        OnboardingData onboardingData = onboardingMsConnector.getOnboardingWithUserInfo(onboardingId);
        log.debug("getOnboardingWithUserInfo result = success");
        log.trace("getOnboardingWithUserInfo end");
        return onboardingData;
    }

    @Override
    public void completeTokenV2(String onboardingId, MultipartFile contract) {
        log.trace("completeTokenAsync start");
        log.debug("completeTokenAsync id = {}", onboardingId);
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        onboardingMsConnector.onboardingTokenComplete(onboardingId, contract);
        log.debug("completeTokenAsync result = success");
        log.trace("completeTokenAsync end");
    }

    @Override
    public void completeOnboardingUsers(String onboardingId, MultipartFile contract) {
        log.trace("completeOnboardingUsersAsync start");
        log.debug("completeOnboardingUsersAsync id = {}", onboardingId);
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        onboardingMsConnector.onboardingUsersComplete(onboardingId, contract);
        log.debug("completeOnboardingUsersAsync result = success");
        log.trace("completeOnboardingUsersAsync end");
    }

    @Override
    public Resource getContract(String onboardingId) {
        log.trace("getContract start");
        log.debug("getContract id = {}", onboardingId);
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Resource resource = documentMsConnector.getContract(onboardingId);
        log.debug("getContract result = success");
        log.trace("getContract end");
        return resource;
    }

    @Override
    public Resource getTemplateAttachment(String onboardingId, String filename) {
        log.trace("getTemplateAttachment start");
        log.debug("getTemplateAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Assert.notNull(filename, "filename is required");
        OnboardingData onboarding = onboardingMsConnector.getOnboarding(onboardingId);
        Product product = productAzureService.getProductValid(onboarding.getProductId());
        String templatePath = getAttachmentTemplate(filename, onboarding, product).getTemplatePath();
        Resource resource = documentMsConnector.getTemplateAttachment(onboarding, filename, templatePath);
        log.debug("getTemplateAttachment result = success");
        log.trace("getTemplateAttachment end");
        return resource;
    }

    private AttachmentTemplate getAttachmentTemplate(String attachmentName, OnboardingData onboarding, Product product) {
        return product
                .getInstitutionContractMappings()
                .get(onboarding.getInstitutionType().name())
                .getAttachments()
                .stream()
                .filter(a -> a.getName().equals(attachmentName))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                String.format("Attachment with name %s not found", attachmentName)
                        )
                );
    }

    @Override
    public Resource getAttachment(String onboardingId, String filename) {
        log.trace("getAttachment start");
        log.debug("getAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Assert.notNull(filename, "filename is required");
        Resource resource = documentMsConnector.getAttachment(onboardingId, filename);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
        return resource;
    }

    @Override
    public Resource getAggregatesCsv(String onboardingId, String productId) {
        log.trace("getAggregatesCsv start");
        log.debug("getAggregatesCsv id = {}, productId = {}", Encode.forJava(onboardingId), Encode.forJava(productId));
        Assert.notNull(onboardingId, ONBOARDING_ID_REQUIRED_MESSAGE);
        Assert.notNull(productId, "ProductId is required");
        Resource resource = documentMsConnector.getAggregatesCsv(onboardingId, productId);
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
    public void uploadAttachment(String onboardingId, MultipartFile attachment, String attachmentName) {
        log.trace("uploadAttachment start");
        log.debug("uploadAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(attachmentName));
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Assert.notNull(attachmentName, "filename is required");
        Assert.notNull(attachment, "file is required");
        OnboardingData onboarding = onboardingMsConnector.getOnboarding(onboardingId);
        Product product = productAzureService.getProductValid(onboarding.getProductId());
        AttachmentTemplate template = getAttachmentTemplate(attachmentName, onboarding, product);
        documentMsConnector.uploadAttachment(onboardingId, attachment, attachmentName, product.getId(), template);
        log.debug("getAttachment result = success");
        log.trace("getAttachment end");
    }

    @Override
    public HttpStatusCode headAttachment(String onboardingId, String filename) {
        log.trace("headAttachment start");
        log.debug("headAttachment id = {}, filename = {}",  Encode.forJava(onboardingId),  Encode.forJava(filename));
        Assert.notNull(onboardingId, TOKEN_ID_IS_REQUIRED);
        Assert.notNull(filename, "filename is required");
        HttpStatusCode resource = documentMsConnector.headAttachment(onboardingId, filename);
        log.debug("headAttachment result {}", resource.value());
        log.trace("headAttachment end");
        return resource;
    }
}
