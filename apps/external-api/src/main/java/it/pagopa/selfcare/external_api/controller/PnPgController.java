package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.external_api.mapper.PnPgMapper;
import it.pagopa.selfcare.external_api.model.pnpg.CreatePnPgInstitutionDto;
import it.pagopa.selfcare.external_api.model.pnpg.PnPgInstitutionIdResource;
import it.pagopa.selfcare.external_api.service.InstitutionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v1/pn-pg", produces = APPLICATION_JSON_VALUE)
@Tag(name = "institutions-pnpg")
public class PnPgController {
    private final InstitutionService institutionService;

    @Autowired
    public PnPgController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @PostMapping(value = "/institutions/add")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "addInstitution", description = "${swagger.external-api.institutions.api.addInstitution}")
    public PnPgInstitutionIdResource addInstitution(@Parameter(description = "swagger.external-api.institutions.model.searchInstitutionDto")
                                 @RequestBody
                                 @Valid
                                 CreatePnPgInstitutionDto createPnPgInstitutionDto) {
        log.trace("addInstitution start");
        log.debug("addInstitution searchInstitutionDto = {}", Encode.forJava(createPnPgInstitutionDto.toString()));
        String institutionId = institutionService.addInstitution(PnPgMapper.fromDto(createPnPgInstitutionDto));
        PnPgInstitutionIdResource id = new PnPgInstitutionIdResource(institutionId);
        log.debug("addInstitution result = {}", id);
        log.trace("addInstitution end");
        return id;
    }
}
