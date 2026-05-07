package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class InstitutionOnboardedRequest {

    @NotEmpty(message = "productId is required")
    private String productId;
}
