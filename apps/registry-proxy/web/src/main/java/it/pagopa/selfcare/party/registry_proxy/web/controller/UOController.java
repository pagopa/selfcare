package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import it.pagopa.selfcare.party.registry_proxy.core.InstitutionService;
import it.pagopa.selfcare.party.registry_proxy.core.UOService;
import it.pagopa.selfcare.party.registry_proxy.web.model.UOResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.UOsResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.UOMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "uo", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "uo")
public class UOController {

    private final UOService uoService;
    private final UOMapper uoMapper;
    private final InstitutionService institutionService;

    public UOController(UOService uoService,
                        UOMapper uoMapper, InstitutionService institutionService) {
        this.institutionService = institutionService;
        log.trace("Initializing {}", UOController.class.getSimpleName());
        this.uoService = uoService;
        this.uoMapper = uoMapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.uo.findAll.summary}",
            description = "${swagger.api.uo.findAll.notes}",
            operationId = "findUosUsingGET")
    public UOsResource findAll(@ApiParam(value = "${swagger.model.*.limit}")
                               @RequestParam(value = "limit", required = false, defaultValue = "10")
                               Integer limit,
                               @ApiParam(value = "${swagger.model.*.page}")
                               @RequestParam(value = "page", required = false, defaultValue = "1")
                               Integer page,
                               @ApiParam(value = "${swagger.model.uo.taxCodeInvoicing}")
                               @RequestParam(value = "taxCodeInvoicing", required = false)
                               Optional<String> taxCodeInvoicing) {
        log.trace("find all UO start");
        log.debug("find all UO, page = {}, limit = {}", page, limit);
        final QueryResult<UO> result = uoService.findAll(taxCodeInvoicing, page, limit);
        final UOsResource uosResource = UOsResource.builder()
                .count(result.getTotalHits())
                .items(result.getItems().stream().map(uoMapper::toResource).toList())
                .build();
        log.debug("find all UO result = {}", uosResource);
        log.trace("find all UO end");
        return uosResource;
    }

    @GetMapping("/{codiceUniUo}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.uo.findBy.summary}",
            description = "${swagger.api.uo.findBy.notes}",
            operationId = "findUoByUnicodeUsingGET",
            tags = { "internal-v1", "uo-controller" })
    public UOResource findByUnicode(@ApiParam("${swagger.model.uo.codiceUniUo}")
                                    @PathVariable("codiceUniUo") String codiceUniUo,
                                    @ApiParam(value = "${swagger.model.*.categories}")
                                    @RequestParam(value = "categories", required = false)
                                    List<String> categories) {
        log.trace("find UO start");
        log.debug("find UO = {}", codiceUniUo);
        final UOResource uoResource = uoMapper.toResource(uoService.findByUnicode(codiceUniUo,categories));
        if (StringUtils.isBlank(uoResource.getMail1())) {
            final Institution institution = institutionService.findById(uoResource.getCodiceFiscaleEnte(), Optional.of(Origin.IPA), Objects.isNull(categories) ? List.of() : categories);
            uoResource.setMail1(institution.getDigitalAddress());
        }
        log.debug("find UO result = {}", uoResource);
        log.trace("find UO end");
        return uoResource;
    }

}
