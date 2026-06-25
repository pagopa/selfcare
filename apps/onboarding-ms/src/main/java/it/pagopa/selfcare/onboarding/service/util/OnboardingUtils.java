package it.pagopa.selfcare.onboarding.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.DocumentType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.PayloadTooLargeException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.service.RegistryProxyService;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.party_registry_proxy_json.model.UOResource;

import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;
import static it.pagopa.selfcare.onboarding.util.ErrorMessage.DOCUMENT_SIZE_EXCEEDED;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

@Slf4j
@ApplicationScoped
public class OnboardingUtils {


    private final RegistryProxyService registryProxyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnboardingUtils(RegistryProxyService registryProxyService) {
        this.registryProxyService = registryProxyService;
    }

    public Uni<UOResource> getUoFromRecipientCode(String recipientCode) {
        return registryProxyService.findUoByRecipientCode(recipientCode, null)
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
            FormItem formItem, Product product, DocumentType documentType, List<String> fiscalCodes,
            int signingStep) {
        DocumentContentControllerApi.UploadSignedContractMultipartForm request = new DocumentContentControllerApi.UploadSignedContractMultipartForm();
        request.skipSignatureVerification = skipSignatureVerification;
        request._file = formItem.getFile();
        request.fileName = formItem.getFileName();
        request.signingStep = signingStep;
        if (OnboardingStatus.PENDING_IN_REVIEW.equals(onboarding.getStatus())) {
            request.skipSignerIdentityCheck =
                    Objects.nonNull(product.getSigningConfiguration())
                            && product.getSigningConfiguration().isSkipSignerIdentityCheck();
        }

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
     * Gestisce due scenari:
     * 1. Il REST client ritorna un Uni con Response di errore (onItem path)
     * 2. Il REST client lancia WebApplicationException su non-2xx (onFailure path)
     * <p>
     * In entrambi i casi, estrae codice e dettaglio dal Problem JSON di document-ms
     * (title = code, detail = message) e propaga un {@link InvalidRequestException}.
     */
    public Uni<Void> ensureSuccessfulDocumentResponse(Uni<Response> responseUni, String operation, String onboardingId) {
        return responseUni
                .onFailure(WebApplicationException.class).recoverWithUni(ex -> {
                    WebApplicationException wae = (WebApplicationException) ex;
                    if (wae.getResponse() != null
                            && wae.getResponse().getStatus() == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                        return Uni.createFrom().failure(new PayloadTooLargeException(
                                "Uploaded file exceeds allowed size",
                                "DOC-413"));
                    }
                    InvalidRequestException parsed = extractDocumentError(wae.getResponse(), operation, onboardingId);
                    return Uni.createFrom().failure(Objects.requireNonNullElse(parsed, ex));
                })
                .onItem().transformToUni(response -> {
                    int status = Objects.nonNull(response)
                            ? response.getStatus()
                            : Response.Status.BAD_GATEWAY.getStatusCode();
                    boolean isSuccess = Objects.nonNull(response)
                            && Objects.nonNull(response.getStatusInfo())
                            && SUCCESSFUL.equals(response.getStatusInfo().getFamily());

                    if (!isSuccess) {
                        if (status == Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
                            if (Objects.nonNull(response)) response.close();
                            return Uni.createFrom().failure(new PayloadTooLargeException(
                                    DOCUMENT_SIZE_EXCEEDED.getMessage(),
                                    DOCUMENT_SIZE_EXCEEDED.getCode()));
                        }
                        InvalidRequestException parsed = extractDocumentError(response, operation, onboardingId);
                        if (Objects.nonNull(response)) response.close();
                        return Uni.createFrom().failure(Objects.requireNonNullElseGet(parsed, () -> new WebApplicationException(
                                String.format("Document service call failed while trying to %s for onboarding %s. status=%s",
                                        operation, onboardingId, status), status)));
                    }

                    response.close();
                    log.debug("Document service call succeeded: operation={}, onboardingId={}", operation, onboardingId);
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Tenta di estrarre codice e dettaglio dal Problem JSON restituito da document-ms.
     * Il formato atteso è: { "title": "002-1003", "detail": "Only CAdES ...", "status": 400 }
     */
    private InvalidRequestException extractDocumentError(Response response, String operation, String onboardingId) {
        String body = readResponseBody(response);
        if (body == null) return null;

        log.warn("Document service call failed: operation={}, onboardingId={}, body={}", operation, onboardingId, body);

        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("title") && root.has("detail")) {
                String errorCode = root.get("title").asText(null);
                String errorDetail = root.get("detail").asText(null);
                if (errorCode != null && errorDetail != null) {
                    return new InvalidRequestException(errorDetail, errorCode);
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse document-ms error response as JSON: {}", e.getMessage());
        }
        return null;
    }

    private String readResponseBody(Response response) {
        if (Objects.isNull(response)) return null;
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            log.debug("Could not read response body: {}", e.getMessage());
            return null;
        }
    }
}
