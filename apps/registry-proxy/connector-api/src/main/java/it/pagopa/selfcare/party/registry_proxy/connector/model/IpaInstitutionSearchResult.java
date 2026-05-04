package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

import java.util.List;

@Data
public class IpaInstitutionSearchResult {

    private List<IpaInstitution> institutions;
    private long totalElements = 0;

}
