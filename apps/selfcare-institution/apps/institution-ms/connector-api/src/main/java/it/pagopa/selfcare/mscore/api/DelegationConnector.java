package it.pagopa.selfcare.mscore.api;

import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.model.delegation.*;
import it.pagopa.selfcare.mscore.model.institution.Institution;

import java.time.OffsetDateTime;
import java.util.List;

public interface DelegationConnector {

    Delegation save(Delegation delegation);

    boolean checkIfExistsWithStatus(Delegation delegation, DelegationState status);

    List<Delegation> find(String from, String to, String productId, String search, String taxCode, Order order, Integer page, Integer size);

    DelegationWithPagination findFromDate(OffsetDateTime fromDate, Long page, Long size);

    DelegationWithPagination findAndCount(GetDelegationParameters delegationParameters);

    Delegation findByIdAndModifyStatus(String delegationId, DelegationState status);

    boolean checkIfDelegationsAreActive(String institutionId);

    Delegation findAndActivate(String from, String to, String productId, Boolean isTest);

    void updateDelegation(Institution update);

    List<DelegationInstitution> findDelegators(String institutionId, String productId, DelegationType type, Long cursor, int size);

    List<DelegationInstitution> findDelegates(String institutionId, String productId, DelegationType type, Long cursor, int size);

}
