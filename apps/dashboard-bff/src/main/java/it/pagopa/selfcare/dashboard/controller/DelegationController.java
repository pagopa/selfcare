package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationId;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationIdResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import it.pagopa.selfcare.dashboard.model.mapper.DelegationMapper;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/v1/delegations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "delegations")
public class DelegationController {

    private final DelegationService delegationService;
    private final DelegationMapper delegationMapper;

    public DelegationController(DelegationService delegationService,
                                DelegationMapper delegationMapper) {
        this.delegationService = delegationService;
        this.delegationMapper = delegationMapper;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "createDelegation", description = "${swagger.dashboard.delegation.api.createDelegation}")
    @ApiResponse(responseCode = "409", description = "Conflict")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#delegationRequest.getFrom(), #delegationRequest.getProductId(), null), 'Selc:CreateDelegation')")
    public DelegationIdResource createDelegation(@RequestBody @Valid DelegationRequestDto delegationRequest) {
        log.trace("createDelegation start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "createDelegation request = {}", delegationRequest);
        DelegationId delegationId = delegationService.createDelegation(delegationMapper.toDelegation(delegationRequest));
        DelegationIdResource result = delegationMapper.toIdResource(delegationId);
        log.debug("createDelegation result = {}", result);
        log.trace("createDelegation end");
        return result;
    }
}

