package it.pagopa.selfcare.mscore.web.model.mapper;

import it.pagopa.selfcare.mscore.model.delegation.Delegation;
import it.pagopa.selfcare.mscore.model.delegation.DelegationInstitution;
import it.pagopa.selfcare.mscore.web.model.delegation.DelegationInstitutionResponse;
import it.pagopa.selfcare.mscore.web.model.delegation.DelegationRequest;
import it.pagopa.selfcare.mscore.web.model.delegation.DelegationRequestFromTaxcode;
import it.pagopa.selfcare.mscore.web.model.delegation.DelegationResponse;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { InstitutionResourceMapper.class }, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DelegationMapper {

    Delegation toDelegation(DelegationRequest delegation);

    Delegation toDelegation(DelegationRequestFromTaxcode delegation);

    @Mapping(source = "from", target = "institutionId")
    @Mapping(source = "to", target = "brokerId")
    @Mapping(source = "institutionFromName", target = "institutionName")
    @Mapping(source = "institutionToName", target = "brokerName")
    @Mapping(source = "institutionFromRootName", target = "institutionRootName")
    DelegationResponse toDelegationResponse(Delegation delegation);

    @Mapping(source = "from", target = "institutionId")
    @Mapping(source = "to", target = "brokerId")
    @Mapping(source = "toTaxCode", target = "brokerTaxCode")
    @Mapping(source = "fromTaxCode", target = "taxCode")
    @Mapping(source = "institutionFromName", target = "institutionName")
    @Mapping(source = "institutionToName", target = "brokerName")
    @Mapping(source = "institutionFromRootName", target = "institutionRootName")
    DelegationResponse toDelegationResponseGet(Delegation delegation);

    @Mapping(source = "institution", target = "institution", qualifiedByName = "toInstitutionResponseWithType")
    DelegationInstitutionResponse toDelegationInstitutionResponse(DelegationInstitution delegationInstitution, @Context String productId);

}