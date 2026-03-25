package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import it.pagopa.selfcare.party.registry_proxy.connector.model.QueryResult;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.core.StationService;
import it.pagopa.selfcare.party.registry_proxy.web.model.StationResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.StationsResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.StationMapper;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/stations", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = "stations")
public class StationController {

    private final StationService stationService;
    private final StationMapper stationMapper;

    @Autowired
    public StationController(StationService stationService,
                             StationMapper stationMapper) {
        log.trace("Initializing {}", StationController.class.getSimpleName());
        this.stationService = stationService;
        this.stationMapper = stationMapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.station.search.summary}",
            description = "${swagger.api.station.search.notes}",
            operationId = "searchStationsUsingGET")
    public StationsResource search(@ApiParam("${swagger.model.*.search}")
                                   @RequestParam(value = "search", required = false)
                                   Optional<String> search,
                                   @ApiParam(value = "${swagger.model.*.page}")
                                   @RequestParam(value = "page", required = false, defaultValue = "1")
                                   Integer page,
                                   @ApiParam(value = "${swagger.model.*.limit}")
                                   @RequestParam(value = "limit", required = false, defaultValue = "10")
                                   Integer limit) {
        log.trace("search start");
        log.debug("search search = {}, page = {}, limit = {}", search, page, limit);
        final QueryResult<Station> result = stationService.search(search, page, limit);
        final StationsResource stationsResource = StationsResource.builder()
                .items(result.getItems().stream()
                        .map(stationMapper::toResource)
                        .collect(Collectors.toList()))
                .count(result.getTotalHits())
                .build();
        log.debug("search result = {}", stationsResource);
        log.trace("search end");
        return stationsResource;
    }

    @GetMapping("/{taxId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.api.station.search.byId.summary}",
            description = "${swagger.api.station.search.byId.notes}",
            operationId = "searchStationByTaxCodeUsingGET")
    public StationResource searchByTaxCode(@ApiParam("${swagger.model.station.taxCode}")
                                           @PathVariable("taxId") String taxId) {
        log.trace("find SA start");
        log.debug("find SA = {}", taxId);
        final StationResource stationResource = stationMapper.toResource(stationService.findByTaxId(taxId));
        log.debug("find SA result = {}", stationResource);
        log.trace("find SA end");
        return stationResource;
    }

}
