package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.web.model.Problem;
import it.pagopa.selfcare.external_api.mapper.NationalRegistryMapper;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerification;
import it.pagopa.selfcare.external_api.model.national_registries.LegalVerificationResource;
import it.pagopa.selfcare.external_api.model.national_registries.VerifyRequestDto;
import it.pagopa.selfcare.external_api.service.InstitutionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "Proxy")
@RequestMapping(value = "/v2/national-registries", produces = MediaType.APPLICATION_JSON_VALUE)
public class NationalRegistryController {
    private final InstitutionService institutionService;
    private final NationalRegistryMapper nationalRegistryMapper;

    public NationalRegistryController(InstitutionService institutionService, NationalRegistryMapper nationalRegistryMapper) {
        this.institutionService = institutionService;
        this.nationalRegistryMapper = nationalRegistryMapper;
    }

    @PostMapping(value = "/legal-tax/verification", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Tag(name = "NationalRegistry")
    @Tag(name = "support-pnpg")
    @Operation(summary = "verifyLegal", description = "${swagger.external-api.national-registries.api.verifyLegal}", operationId = "#verifyLegalByPOST")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LegalVerificationResource.class), mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    public LegalVerificationResource verifyLegal(@RequestBody @Valid VerifyRequestDto requestDto){
        log.trace("verifyLegal start");
        log.debug("verifyLegal taxId = {}, vatNumber = {}", Encode.forJava(requestDto.getTaxId()), Encode.forJava(requestDto.getVatNumber()));
        LegalVerification legalVerification = institutionService.verifyLegal(requestDto.getTaxId(), requestDto.getVatNumber());
        log.trace("verifyLegal end");
        return nationalRegistryMapper.toResource(legalVerification);
    }
}
