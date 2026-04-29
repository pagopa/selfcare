package it.pagopa.selfcare.mscore.model.institution;

import lombok.Data;

import java.util.List;

@Data
public class GeographicTaxonomyPage {
    private Integer total;
    private List<GeographicTaxonomies> data;
}
