package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.external_api.mapper.TokenResourceMapper;
import it.pagopa.selfcare.external_api.model.token.TokenOnboardedUsers;
import it.pagopa.selfcare.external_api.model.token.TokensResource;
import it.pagopa.selfcare.external_api.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v1/tokens", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Token")
public class TokenController {

    private final TokenService tokenService;
    private final TokenResourceMapper tokenResourceMapper;

    @Autowired
    public TokenController(TokenService tokenService,
                           TokenResourceMapper tokenResourceMapper) {
        this.tokenService = tokenService;
        this.tokenResourceMapper = tokenResourceMapper;
    }

    /**
     * Retrieve tokens from productId in input
     *
     * @param productId String
     * @param page      Integer
     * @param size      Integer
     * @return List
     * * Code: 200, Message: successful operation
     * * Code: 404, Message: product not found
     */
    @Tag(name = "external-v2")
    @Tag(name = "Token")
    @Tag(name = "support")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.external_api.api.tokens.findFromProduct}", description = "${swagger.external_api.api.tokens.findFromProduct}", operationId = "#getTokensFromProductUsingGET")
    @GetMapping(value = "/products/{productId}")
    public ResponseEntity<TokensResource> getTokensFromProductUsingGET(@Parameter(description = "${swagger.external_api.api.tokens.productId}")
                                                                       @PathVariable(value = "productId") String productId,
                                                                       @Parameter(description = "${swagger.external_api.page.number}")
                                                                       @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                                       @Parameter(description = "${swagger.external_api.page.size}")
                                                                       @RequestParam(name = "size", defaultValue = "100") Integer size,
                                                                       @Parameter(description = "${swagger.external_api.api.tokens.status}")
                                                                       @RequestParam(name = "status", required = false) String status) {
        log.trace("findFromProduct start");
        log.debug("findFromProduct productId = {}", Encode.forJava(productId));
        List<TokenOnboardedUsers> tokens = tokenService.findByProductId(Encode.forJava(productId), page, size, status);

        TokensResource tokenListResponse = new TokensResource(
                tokens.stream()
                        .map(tokenResourceMapper::toResponse)
                        .toList());

        log.trace("findFromProduct end");
        return ResponseEntity.ok().body(tokenListResponse);
    }
}
