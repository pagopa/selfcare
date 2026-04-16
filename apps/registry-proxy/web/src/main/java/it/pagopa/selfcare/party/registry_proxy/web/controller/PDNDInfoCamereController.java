package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.core.PDNDInfoCamereService;
import it.pagopa.selfcare.party.registry_proxy.web.model.PDNDBusinessResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.PDNDInfoCamereBusinessMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/infocamere-pdnd", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "infocamere-pdnd")
public class PDNDInfoCamereController {

  private final PDNDInfoCamereService pdndInfoCamereService;
  private final PDNDInfoCamereBusinessMapper pdndBusinessMapper;

  public PDNDInfoCamereController(
          PDNDInfoCamereService pdndInfoCamereService,
          PDNDInfoCamereBusinessMapper pdndBusinessMapper) {
    this.pdndInfoCamereService = pdndInfoCamereService;
    this.pdndBusinessMapper = pdndBusinessMapper;
  }

  @ResponseStatus(HttpStatus.OK)
  @Operation(
          summary = "${swagger.api.infocamere-pdnd.institutions.summary}",
          description = "${swagger.api.infocamere-pdnd.institutions.notes}",
          operationId = "institutionsPdndByDescriptionUsingGET")
  @GetMapping("/institutions")
  public ResponseEntity<List<PDNDBusinessResource>> institutionsPdndByDescription(
          @ApiParam("${swagger.model.institution.description}") @RequestParam String description) {
    List<PDNDBusiness> businessList =
            pdndInfoCamereService.retrieveInstitutionsPdndByDescription(description);
    return ResponseEntity.ok().body(pdndBusinessMapper.toResources(businessList));
  }

  @Tag(name = "internal-v1")
  @Tag(name = "infocamere-pdnd")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
          summary = "${swagger.api.infocamere-pdnd.institution.summary}",
          description = "${swagger.api.infocamere-pdnd.institution.notes}",
          operationId = "institutionPdndByTaxCodeUsingGET")
  @GetMapping("/institution/{taxCode}")
  public ResponseEntity<PDNDBusinessResource> institutionPdndByTaxCode(
          @ApiParam("${swagger.model.institution.taxCode}") @PathVariable String taxCode) {
    PDNDBusiness business = pdndInfoCamereService.retrieveInstitutionPdndByTaxCode(taxCode);
    return ResponseEntity.ok().body(pdndBusinessMapper.toResource(business));
  }
}
