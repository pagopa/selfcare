package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionInfoIC {

    private String legalTaxId;
    private String requestDateTime;
    private List<BusinessInfoIC> businesses;

}
