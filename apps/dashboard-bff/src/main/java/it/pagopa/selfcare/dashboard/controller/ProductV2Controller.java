package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.security.ExchangeTokenServiceV2;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(value = "/v2/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "products")
public class ProductV2Controller {

    private final ExchangeTokenServiceV2 exchangeTokenService;

    @Autowired
    public ProductV2Controller(ExchangeTokenServiceV2 exchangeTokenServiceV2) {
        this.exchangeTokenService = exchangeTokenServiceV2;
    }

    @GetMapping(value = "/{productId}/back-office", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.product.api.retrieveProductBackoffice}", description = "${swagger.dashboard.product.api.retrieveProductBackoffice}", operationId = "v2RetrieveProductBackofficeUsingGET")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:AccessProductBackoffice')")
    public URI retrieveProductBackoffice(@Parameter(description = "${swagger.dashboard.products.model.id}")
                                         @PathVariable("productId")
                                         String productId,
                                         @Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                         @RequestParam("institutionId")
                                         String institutionId,
                                         @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.environment}")
                                         @RequestParam(value = "environment", required = false)
                                         Optional<String> environment,
                                         @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.lang}")
                                         @RequestParam(value = "lang", required = false, defaultValue = "it")
                                         String lang) {
        log.trace("accessProductBackoffice start");
        log.debug("accessProductBackoffice institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        final ExchangedToken exchangedToken = exchangeTokenService.exchange(institutionId, productId, environment);
        final URI location = URI.create(exchangedToken.getBackOfficeUrl()
                .replace("<IdentityToken>", exchangedToken.getIdentityToken())
                .replace("<lang>", lang));
        log.trace("accessProductBackoffice end");
        return location;

    }
}
