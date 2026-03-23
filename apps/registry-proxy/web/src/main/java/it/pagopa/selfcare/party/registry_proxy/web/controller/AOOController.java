package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.core.AOOService;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.AOOsResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.AOOMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "aoo", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "aoo")
public class AOOController {

    private final AOOService aooService;

    public AOOController(AOOService aooService) {
        log.trace("Initializing {}", AOOController.class.getSimpleName());
        this.aooService = aooService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.aoo.findAll.summary}",
            description = "${swagger.api.aoo.findAll.notes}",
            operationId = "findAOOUsingGET")
    public AOOsResource findAll(@ApiParam(value = "${swagger.model.*.page}")
                                @RequestParam(value = "page", required = false, defaultValue = "1")
                                Integer page,
                                @ApiParam(value = "${swagger.model.*.limit}")
                                @RequestParam(value = "limit", required = false, defaultValue = "10")
                                Integer limit) {
        log.trace("find all AOO start");
        log.debug("find all AOO  page = {}, limit = {}", page, limit);
        final QueryResult<AOO> result = aooService.findAll(page, limit);
        final AOOsResource aoosResource = AOOMapper.toResource(result.getItems().stream()
                        .map(AOOMapper::toResource)
                        .collect(Collectors.toList()),
                result.getTotalHits());
        log.debug("findAOOs result = {}", aoosResource);
        log.trace("findAOOs end");
        return aoosResource;
    }

    @GetMapping("/{codiceUniAoo}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.aoo.findBy.summary}",
            description = "${swagger.api.aoo.findBy.notes}",
            operationId = "findAOOByUnicodeUsingGET",
            tags = { "internal-v1", "aoo-controller" })
    public AOOResource findByUnicode(@ApiParam("${swagger.model.aoo.codiceUniAoo}")
                                     @PathVariable("codiceUniAoo") String codiceUniAoo,
                                     @ApiParam(value = "${swagger.model.*.categories}")
                                     @RequestParam(value = "categories", required = false)
                                     List<String> categories) {
        log.trace("find AOO start");
        log.debug("find AOO codiceUniAoo = {}", codiceUniAoo);
        final AOOResource aooResource = AOOMapper.toResource(aooService.findByUnicode(codiceUniAoo,categories));
        log.debug("findAOO result = {}", aooResource);
        log.trace("findAOO end");
        return aooResource;
    }

}
