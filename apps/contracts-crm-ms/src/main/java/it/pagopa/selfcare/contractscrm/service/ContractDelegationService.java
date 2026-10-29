package it.pagopa.selfcare.contractscrm.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.contractscrm.model.dto.request.DelegationNotification;
import it.pagopa.selfcare.contractscrm.model.dto.response.ContractDelegationResponse;

public interface ContractDelegationService {

  /**
   * Updates the contract delegation related to a PSP delegation and publishes it when both
   * membership and recipient are known.
   *
   * @return the updated contract delegation, or an empty item when the delegation type is not
   *     handled by this service
   */
  Uni<ContractDelegationResponse> processDelegation(DelegationNotification delegation);
}
