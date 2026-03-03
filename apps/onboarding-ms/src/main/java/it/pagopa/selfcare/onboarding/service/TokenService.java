package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.response.ContractSignedReport;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;
import java.util.List;

public interface TokenService {

    Uni<List<Token>> getToken(String onboardingId);

    Uni<RestResponse<File>> retrieveContract(String onboardingId, boolean isSigned);

    Uni<RestResponse<File>> retrieveSignedFile(String onboardingId);

    Uni<RestResponse<File>> retrieveTemplateAttachment(String onboardingId, String attachmentName);

    Uni<RestResponse<File>> retrieveAttachment(String onboardingId, String attachmentName);

    Uni<Void> uploadAttachment(String onboardingId, FormItem file, String attachmentName);

    Uni<Long> updateContractSigned(String onboardingId, String documentSignedPath);

    Uni<List<String>> getAttachments(String onboardingId);

    Uni<ContractSignedReport> reportContractSigned(String onboardingId);

    String getAndVerifyDigest(FormItem file, ContractTemplate contract, boolean skipDigestCheck);

    String getTemplateAndVerifyDigest(FormItem file, String contractTemplatePath, boolean skipDigestCheck);

    String getContractPathByOnboarding(String onboardingId, String filename);

    Uni<Boolean> existsAttachment(String onboardingId, String attachmentName);

    Uni<Token> retrieveToken(String onboardingId);

    Uni<Token> retrieveToken(Onboarding onboarding, FormItem formItem, Product product);

    Uni<String> updateTokenWithFilePath(String filepath, Token token);

    Uni<Void> updateTokenUpdatedAt(String onboardingId);

    Uni<Void> persistTokenForImport(Onboarding onboardingPersisted, Product product, OnboardingImportContract contractImported);
}
