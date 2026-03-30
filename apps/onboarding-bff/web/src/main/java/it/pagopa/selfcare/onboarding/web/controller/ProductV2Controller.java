package it.pagopa.selfcare.onboarding.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.onboarding.connector.model.product.OriginResult;
import it.pagopa.selfcare.onboarding.core.ProductService;
import it.pagopa.selfcare.onboarding.web.model.OriginResponse;
import it.pagopa.selfcare.onboarding.web.model.mapper.ProductMapper;
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

}
