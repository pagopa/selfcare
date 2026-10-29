package it.pagopa.selfcare.contractscrm.mapper;

import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;
import it.pagopa.selfcare.contractscrm.model.dto.response.InstitutionContractResponse;
import it.pagopa.selfcare.contractscrm.model.entity.ContractDelegation;
import it.pagopa.selfcare.contractscrm.model.entity.InstitutionContract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface ContractDelegationMapper {

  ContractDelegationResponse toResponse(ContractDelegation contractDelegation);

  @Mapping(target = "id", source = "contractId")
  InstitutionContractResponse toResponse(InstitutionContract institutionContract);
}
