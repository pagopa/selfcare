package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.model.delegation.*;

import java.util.List;
import java.util.Optional;

public interface DelegationService {

    Delegation createDelegation(Delegation delegation);

    boolean checkIfExistsWithStatus(Delegation delegation, DelegationState status);

    List<Delegation> getDelegations(String from, String to, String productId, String search, String taxCode, Optional<Order> order, Optional<Integer> page, Optional<Integer> size);

    Delegation createDelegationFromInstitutionsTaxCode(Delegation delegation);

    void deleteDelegationByDelegationId(String delegationId);

    DelegationWithPagination getDelegationsV2(GetDelegationParameters delegationParameters);

    List<DelegationInstitution> getDelegators(String institutionId, String productId, DelegationType type, Long cursor, int size);

    List<DelegationInstitution> getDelegates(String institutionId, String productId, DelegationType type, Long cursor, int size);

}
