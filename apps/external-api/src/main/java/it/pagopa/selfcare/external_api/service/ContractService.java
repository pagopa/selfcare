package it.pagopa.selfcare.external_api.service;


import it.pagopa.selfcare.external_api.model.document.ResourceResponse;

public interface ContractService {

    ResourceResponse getContractV2(String institutionId, String productId, String documentId);

    default ResourceResponse getContractV2(String institutionId, String productId) {
        return getContractV2(institutionId, productId, null);
    }
}
