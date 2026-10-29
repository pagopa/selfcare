package it.pagopa.selfcare.contractscrm.model.dto.response;

import it.pagopa.selfcare.contractscrm.model.entity.ContractStatus;
import java.time.Instant;
import lombok.Data;

@Data
public class InstitutionContractResponse {

  private String institutionId;
  private String id;
  private Instant date;
  private ContractStatus status;
}
