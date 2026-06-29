package it.pagopa.selfcare.onboarding.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.connector.model.product.RequiredDocumentModel;
import it.pagopa.selfcare.onboarding.core.ProductService;
import it.pagopa.selfcare.onboarding.web.model.OriginResponse;
import it.pagopa.selfcare.onboarding.web.model.mapper.ProductMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/v2/product", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "product-ms")
public class ProductV2Controller {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @Autowired
    public ProductV2Controller(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.product.ms.api.getOrigins.summary}",
            description = "${swagger.product.ms.api.getOrigins.description}", operationId = "getOrigins")
    public OriginResponse getOrigins(@ApiParam("${swagger.onboarding.institutions.model.institutionType}")
                                      @RequestParam(value = "productId", required = true)
                                      String productId) {
        log.trace("getOrigins start");
        String productIdSanitized = Encode.forJava(productId);
        log.debug("getOrigins productId = {}", productIdSanitized);
        OriginResult originEntries = productService.getOrigins(productId);
        OriginResponse response = productMapper.toOriginResponse(originEntries);
        log.trace("getOrigins end");
        return response;
    }

    @GetMapping(value = "/{productId}/required-documents")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get required documents for a product",
            description = "Returns the list of required documents for the given product, institutionType and origin.",
            operationId = "getRequiredDocuments")
    public List<RequiredDocumentModel> getRequiredDocuments(
            @ApiParam("The product id") @PathVariable("productId") String productId,
            @RequestParam("institutionType") String institutionType,
            @RequestParam("origin") String origin) {
        log.trace("getRequiredDocuments start");
        log.debug("getRequiredDocuments productId = {}, institutionType = {}, origin = {}", productId, institutionType, origin);
        List<RequiredDocumentModel> result = productService.getRequiredDocuments(productId, institutionType, origin);
        log.debug("getRequiredDocuments size = {}", result.size());
        log.trace("getRequiredDocuments end");
        return result;
    }

    @GetMapping(value = "/{productId}/required-documents/enabled")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Check if required documents are enabled for a product",
            description = "Returns true if required documents are configured for the given product, institutionType and origin.",
            operationId = "isRequiredDocumentsEnabled")
    public boolean isRequiredDocumentsEnabled(
            @ApiParam("The product id") @PathVariable("productId") String productId,
            @RequestParam("institutionType") String institutionType,
            @RequestParam("origin") String origin) {
        log.trace("isRequiredDocumentsEnabled start");
        log.debug("isRequiredDocumentsEnabled productId = {}, institutionType = {}, origin = {}", productId, institutionType, origin);
        boolean result = productService.isRequiredDocumentsEnabled(productId, institutionType, origin);
        log.debug("isRequiredDocumentsEnabled result = {}", result);
        log.trace("isRequiredDocumentsEnabled end");
        return result;
    }

}
