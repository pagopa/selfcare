package it.pagopa.selfcare.party.registry_proxy.connector.model;

import java.util.List;
import lombok.Data;

@Data
public class IpaInstitutionSearchResult implements SearchResult<IpaInstitution> {

    private List<IpaInstitution> institutions;
    private long totalElements = 0;

    @Override
    public List<IpaInstitution> getItems() {
        return institutions;
    }

}
