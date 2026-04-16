package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionGeographicTaxonomyPage {
    private Integer total;
    private List<InstitutionGeographicTaxonomies> data;
}
