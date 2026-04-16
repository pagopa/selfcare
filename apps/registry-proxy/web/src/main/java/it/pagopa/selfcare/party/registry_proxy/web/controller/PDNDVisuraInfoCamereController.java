package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.InvalidRequestException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries_pdnd.PDNDBusiness;
import it.pagopa.selfcare.party.registry_proxy.core.PDNDInfoCamereService;
import it.pagopa.selfcare.party.registry_proxy.web.model.PDNDBusinessResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.PDNDInfoCamereBusinessMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/visura-infocamere-pdnd", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "visura-infocamere-pdnd")
public class PDNDVisuraInfoCamereController {

  private final PDNDInfoCamereService pdndInfoCamereService;
  private final PDNDInfoCamereBusinessMapper pdndBusinessMapper;

  public PDNDVisuraInfoCamereController(
          PDNDInfoCamereService pdndInfoCamereService,
          PDNDInfoCamereBusinessMapper pdndBusinessMapper) {
    this.pdndInfoCamereService = pdndInfoCamereService;
    this.pdndBusinessMapper = pdndBusinessMapper;
  }

  @ResponseStatus(HttpStatus.OK)
  @Operation(
          summary = "${swagger.api.infocamere-pdnd.institution.summary}",
          description = "${swagger.api.infocamere-pdnd.institution.notes}",
          operationId = "institutionVisuraPdndByTaxCodeUsingGET")
  @GetMapping("/institutions/{taxCode}")
  public ResponseEntity<PDNDBusinessResource> getInstitution(@ApiParam("${swagger.model.institution.taxCode}") @PathVariable String taxCode) {
    PDNDBusiness business = pdndInfoCamereService.retrieveInstitutionDetail(taxCode);
    return ResponseEntity.ok().body(pdndBusinessMapper.toResource(business));
  }

  @ResponseStatus(HttpStatus.OK)
  @Operation(
          summary = "${swagger.api.infocamere-pdnd.institution.summary}",
          description = "${swagger.api.infocamere-pdnd.institution.notes}",
          operationId = "institutionVisuraDocumentByTaxCodeUsingGET",
          responses =  @ApiResponse(
                  responseCode = "200",
                  description = "Document successfully retrieved",
                  content = @Content(
                          mediaType = "application/xml",
                          schema = @Schema(type = "string", format = "byte")
                  )
          ))
  @GetMapping(value = "/institutions/{taxCode}/document", produces = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<byte[]> getInstitutionDocument(@ApiParam("${swagger.model.institution.taxCode}") @PathVariable String taxCode) {
    byte[] document = pdndInfoCamereService.retrieveInstitutionDocument(taxCode);
    return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(document);
  }

  @ResponseStatus(HttpStatus.OK)
  @Operation(
          summary = "${swagger.api.visura-infocamere-pdnd.institutions.summary}",
          description = "${swagger.api.visura-infocamere-pdnd.institution.notes}",
          operationId = "institutionsPdndByReaGET")
  @GetMapping("/institutions")
  public ResponseEntity<PDNDBusinessResource> institutionsPdndByRea(
          @ApiParam("${swagger.model.institution.rea}") @RequestParam String rea) {
    String[] parameters = rea.split("-");
    if (parameters.length != 2) {
      throw new InvalidRequestException(
              "Rea parameter is malformed. It should be in form of XX-123456");
    }
    PDNDBusiness business = pdndInfoCamereService.retrieveInstitutionFromRea(parameters[0], parameters[1]);
    return ResponseEntity.ok().body(pdndBusinessMapper.toResource(business));
  }
}
