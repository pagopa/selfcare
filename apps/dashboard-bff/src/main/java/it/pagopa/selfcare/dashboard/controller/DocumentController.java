package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.dashboard.model.document.DocumentStatusResource;
import it.pagopa.selfcare.dashboard.model.document.DocumentType;
import it.pagopa.selfcare.dashboard.model.document.DocumentsResource;
import it.pagopa.selfcare.dashboard.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.owasp.encoder.Encode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
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

    @GetMapping(value = "/institutions/{institutionId}/products/{productId}/document", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getDocument", description = "${swagger.dashboard.documents.api.getDocument}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ViewContract')")
    public ResponseEntity<byte[]> getDocument(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @Parameter(description = "${swagger.dashboard.products.model.id}")
                                              @PathVariable("productId") String productId,
                                              @Parameter(description = "${swagger.dashboard.documents.api.getDocument.name}")
                                              @RequestParam(value = "name", required = false) String name,
                                              @Parameter(description = "${swagger.dashboard.documents.model.type}")
                                              @RequestParam("type") DocumentType type) throws IOException {
        log.trace("getDocument start");
        log.debug("getDocument institutionId = {}, productId = {}, name = {}, type = {}",
                Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(name), type);
        Resource document = documentService.getDocument(institutionId, productId, name, type);
        log.trace("getDocument end");
        return getResponseEntity(document);
    }

    private ResponseEntity<byte[]> getResponseEntity(Resource document) throws IOException {
        try (InputStream inputStream = document.getInputStream()) {
            byte[] byteArray = IOUtils.toByteArray(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + document.getFilename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(byteArray);
        }
    }

}
