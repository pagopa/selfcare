package it.pagopa.selfcare.contractscrm.model.dto.response;

import java.time.Instant;
import lombok.Data;

/** Contract delegation, returned by the API and published on SC-ContractsCRMDelegations. */
@Data
public class ContractDelegationResponse {

  private String id;
  private String productId;
  private InstitutionContractResponse contract;
  private InstitutionContractResponse membership;
  private InstitutionContractResponse recipient;
  private Instant createdAt;
  private Instant updatedAt;
}
