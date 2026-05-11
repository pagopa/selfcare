package it.pagopa.selfcare.dashboard.model;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionPut {
    private List<String> geographicTaxonomyCodes;
}
