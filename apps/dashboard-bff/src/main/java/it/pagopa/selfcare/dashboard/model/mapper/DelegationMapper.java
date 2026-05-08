package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.delegation.Delegation;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationId;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequest;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationIdResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DelegationMapper {

    DelegationRequest toDelegation(DelegationRequestDto request);
    DelegationIdResource toIdResource(DelegationId delegationId);
    DelegationResource toDelegationResource(Delegation delegation);
}
