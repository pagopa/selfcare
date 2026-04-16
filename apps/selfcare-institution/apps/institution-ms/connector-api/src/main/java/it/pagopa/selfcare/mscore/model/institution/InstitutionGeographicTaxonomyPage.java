package it.pagopa.selfcare.mscore.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionGeographicTaxonomyPage {
    private Integer total;
    private List<InstitutionGeographicTaxonomies> data;
}
