package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.web.security.JwtAuthenticationToken;
import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.model.IdentityTokenResource;
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
@RequestMapping(value = "/v2/token", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "token")
public class TokenV2Controller {

    private final ExchangeTokenServiceV2 exchangeTokenService;


    @Autowired
    public TokenV2Controller(ExchangeTokenServiceV2 exchangeTokenServiceV2) {
        this.exchangeTokenService = exchangeTokenServiceV2;
    }


    @GetMapping(value = "exchange", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.token.api.exchange}", description = "${swagger.dashboard.token.api.exchange}", operationId = "#v2Exchange")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:AccessProductBackoffice')")
    public IdentityTokenResource exchange(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                          @RequestParam("institutionId")
                                          String institutionId,
                                          @Parameter(description = "${swagger.dashboard.products.model.id}")
                                          @RequestParam("productId")
                                          String productId,
                                          @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.environment}")
                                          @RequestParam(value = "environment", required = false)
                                          Optional<String> environment) {

        log.trace("exchange start");
        log.debug("exchange institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));

        String token = exchangeTokenService.exchange(institutionId, productId, environment).getIdentityToken();
        IdentityTokenResource identityToken = new IdentityTokenResource();
        identityToken.setToken(token);


        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchange result = {}", identityToken);
        log.trace("exchange end");

        return identityToken;
    }

    @GetMapping(value = "exchange/back-office/admin", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.token.api.exchange.backoffice.admin}", description = "${swagger.dashboard.token.api.exchange.backoffice.admin}", operationId = "#v2ExchangeBackofficeAdmin")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:AccessProductBackofficeAdmin')")
    public URI exchangeBackofficeAdmin(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                          @RequestParam("institutionId")
                                          String institutionId,
                                          @Parameter(description = "${swagger.dashboard.products.model.id}")
                                          @RequestParam("productId")
                                          String productId,
                                          @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.environment}")
                                          @RequestParam(value = "environment", required = false)
                                          Optional<String> environment,
                                          @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.lang}")
                                          @RequestParam(value = "lang", required = false, defaultValue = "it")
                                          String lang) {

        log.trace("exchangeBackofficeAdmin start");
        log.debug("exchangeBackofficeAdmin institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));

        final ExchangedToken exchangedToken = exchangeTokenService.exchangeBackofficeAdmin(institutionId, productId, environment);
        final URI location = URI.create(exchangedToken.getBackOfficeUrl()
                .replace("<IdentityToken>", exchangedToken.getIdentityToken())
                .replace("<lang>", lang));

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "exchangeBackofficeAdmin result = {}", Encode.forJava(String.valueOf(location)));
        log.trace("exchangeBackofficeAdmin end");

        return location;
    }

    @GetMapping(value = "exchange/fatturazione", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "billingToken", description = "${swagger.dashboard.token.api.billingToken}")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ViewBilling')")
    public URI billingToken(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                            @RequestParam("institutionId")
                            String institutionId,
                            @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.environment}")
                            @RequestParam(value = "environment", required = false)
                            Optional<String> environment,
                            JwtAuthenticationToken jwtAuthenticationToken,
                            @Parameter(description = "${swagger.dashboard.product-backoffice-configurations.model.lang}")
                            @RequestParam(value = "lang", required = false)
                            String lang) {

        log.trace("billing exchange start");
        log.debug("billing exchange institutionId = {}", Encode.forJava(institutionId));
        log.info("env parameter: {}", Encode.forJava(environment.orElse("")));

        final ExchangedToken exchangedToken = exchangeTokenService.retrieveBillingExchangedToken(institutionId);
        final URI location = URI.create(exchangedToken.getBackOfficeUrl().replace("<IdentityToken>", exchangedToken.getIdentityToken()));
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "billing exchange result = {}", Encode.forJava(String.valueOf(location)));
        log.trace("billing exchange end");

        return location;
    }

}
