package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;
import it.pagopa.selfcare.dashboard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/v1/documents", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getDocumentStatus", description = "${swagger.dashboard.documents.api.getDocumentStatus}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ViewContract')")
    public DocumentStatusResource getDocumentStatus(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                    @RequestParam("institutionId") String institutionId,
                                                    @Parameter(description = "${swagger.dashboard.documents.model.productIds}")
                                                    @RequestParam("productIds") List<String> productIds) {
        log.trace("getDocumentStatus start");
        log.debug("getDocumentStatus institutionId = {}, productIds = {}", Encode.forJava(institutionId), Encode.forJava(String.valueOf(productIds)));
        DocumentStatusResource result = documentService.getDocumentStatus(institutionId, productIds);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getDocumentStatus result = {}", result);
        log.trace("getDocumentStatus end");
        return result;
    }

    @GetMapping(value = "/institutions/{institutionId}/products/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getDocumentsList", description = "${swagger.dashboard.documents.api.getDocumentsList}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ViewContract')")
    public DocumentsResource getDocumentsList(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                          @PathVariable("institutionId") String institutionId,
                                          @Parameter(description = "${swagger.dashboard.products.model.id}")
                                          @PathVariable("productId") String productId) {
        log.trace("getDocumentsList start");
        log.debug("getDocumentsList institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        DocumentsResource result = documentService.getDocumentsList(institutionId, productId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getDocumentsList result = {}", result);
        log.trace("getDocumentsList end");
        return result;
    }

}
