package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class PdaInstitutionRequest {

    @NotEmpty(message = "InjectionInstitutionType is required")
    private String injectionInstitutionType;

    @NotEmpty(message = "TaxCode is required")
    private String taxCode;

    private String description;

    private String istatCode;

    private BillingRequest billing;
}
