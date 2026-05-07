package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;

import java.util.List;

public interface BrokerService {

    List<BrokerInfo> findAllByInstitutionType(String institutionType);
    List<BrokerInfo> findInstitutionsByProductAndType(String productId, String institutionType);

}
