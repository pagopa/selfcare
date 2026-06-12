package it.pagopa.selfcare.external_api.service;


import it.pagopa.selfcare.external_api.model.document.ResourceResponse;

public interface ContractService {

    ResourceResponse getContractV2(String institutionId, String productId);
}
