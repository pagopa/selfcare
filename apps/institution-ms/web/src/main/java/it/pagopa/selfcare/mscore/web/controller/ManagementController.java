package it.pagopa.selfcare.mscore.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.mscore.core.InstitutionService;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.web.model.institution.BulkInstitutions;
import it.pagopa.selfcare.mscore.web.model.institution.BulkPartiesSeed;
import it.pagopa.selfcare.mscore.web.model.mapper.InstitutionMapperCustom;
import it.pagopa.selfcare.mscore.web.util.CustomExceptionMessage;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.selfcare.mscore.constant.GenericError.GET_INSTITUTION_BY_ID_ERROR;

@Slf4j
@RestController
@Tag(name = "Management")
public class ManagementController {

    private final InstitutionService institutionService;

    public ManagementController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.mscore.institution}", description = "${swagger.mscore.institution}")
    @PostMapping(value = "/bulk/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity<BulkInstitutions> retrieveInstitutionByIds(@Parameter(description = "${swagger.mscore.institutions.model.internalIds}")
                                                                     @RequestBody @Valid BulkPartiesSeed bulkPartiesSeed) {
        CustomExceptionMessage.setCustomMessage(GET_INSTITUTION_BY_ID_ERROR);
        List<String> ids = new ArrayList<>(bulkPartiesSeed.getPartyIdentifiers());
        List<Institution> institution = institutionService.retrieveInstitutionByIds(ids);
        return ResponseEntity.ok().body(InstitutionMapperCustom.toBulkInstitutions(institution, ids));
    }
}
