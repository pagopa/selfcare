package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.delegation.*;

import java.util.List;

public interface DelegationService {

    DelegationId createDelegation(DelegationRequest delegation);

    List<Delegation> getDelegations(GetDelegationParameters delegationParameters);

    DelegationWithPagination getDelegationsV2(GetDelegationParameters delegationParameters);

}
