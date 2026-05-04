package it.pagopa.selfcare.mscore.web.model.institution;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
public class CreatePgInstitutionRequest {

    @NotEmpty(message = "taxId is required")
    private String taxId;

    private String description;

    private String istatCode;

    @NotNull
    private boolean existsInRegistry;

}
