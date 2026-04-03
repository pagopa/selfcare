package it.pagopa.selfcare.onboarding.service.util;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

@Slf4j
@ApplicationScoped
public class OnboardingUtils {

    @RestClient
    @Inject
    UoApi uoApi;

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return uoApi.findByUnicodeUsingGET1(recipientCode, null)
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() == 404
                        ? Uni.createFrom().failure(new ResourceNotFoundException(
                        String.format(UO_NOT_FOUND.getMessage(),
                                recipientCode
                        )))
                        : Uni.createFrom().failure(ex));
    }

    public Uni<CustomError> getValidationRecipientCodeError(String originIdEC, UOResource uoResource) {
        if (!originIdEC.equals(uoResource.getCodiceIpa())) {
            return Uni.createFrom().item(DENIED_NO_ASSOCIATION);
        }
        if (Objects.isNull(uoResource.getCodiceFiscaleSfe())) {
            return Uni.createFrom().item(DENIED_NO_BILLING);
        }
        return Uni.createFrom().nullItem();
    }

    public Uni<DocumentContentControllerApi.UploadSignedContractMultipartForm> buildUploadSignedContractRequest(
            Onboarding onboarding,
            boolean skipSignatureVerification,
            FormItem formItem, Product product, DocumentType documentType, List<String> fiscalCodes) {
        DocumentContentControllerApi.UploadSignedContractMultipartForm request = new DocumentContentControllerApi.UploadSignedContractMultipartForm();
        request.skipSignatureVerification = skipSignatureVerification;
        request._file = formItem.getFile();
        request.fileName = formItem.getFileName();

        String institutionType = onboarding.getInstitution().getInstitutionType().name();
        ContractTemplate contractTemplate = product.getInstitutionContractTemplate(institutionType);
        request.request = DocumentBuilderRequest.builder()
                .onboardingId(onboarding.getId())
                .productId(product.getId())
                .documentType(org.openapi.quarkus.document_json.model.DocumentType.fromString(documentType.name()))
                .templateVersion(contractTemplate.getContractTemplateVersion())
                .templatePath(contractTemplate.getContractTemplatePath())
                .fiscalCodes(fiscalCodes)
                .productTitle(product.getTitle())
                .build();
        return Uni.createFrom().item(request);
    }

    /**
     * Verifica che la risposta del document service sia successful (2xx).
     * In caso contrario logga e propaga un {@link WebApplicationException}.
     */
    public Uni<Void> ensureSuccessfulDocumentResponse(Uni<Response> responseUni, String operation, String onboardingId) {
        return responseUni.onItem().transformToUni(response -> {
            int status = Objects.nonNull(response)
                    ? response.getStatus()
                    : Response.Status.BAD_GATEWAY.getStatusCode();
            boolean isSuccess = Objects.nonNull(response)
                    && Objects.nonNull(response.getStatusInfo())
                    && SUCCESSFUL.equals(response.getStatusInfo().getFamily());

            if (Objects.nonNull(response)) response.close();

            if (!isSuccess) {
                log.warn("Document service call failed: operation={}, onboardingId={}, status={}", operation, onboardingId, status);
                return Uni.createFrom().failure(new WebApplicationException(
                        String.format("Document service call failed while trying to %s for onboarding %s. status=%s",
                                operation, onboardingId, status), status));
            }
            log.debug("Document service call succeeded: operation={}, onboardingId={}", operation, onboardingId);
            return Uni.createFrom().voidItem();
        });
    }
}
