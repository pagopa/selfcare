package it.pagopa.selfcare.external_api.model.pnpg;

import lombok.Data;

@Data
public class CreatePnPgInstitutionRequest {

    public CreatePnPgInstitutionRequest(CreatePnPgInstitution institution) {
        this.taxId = institution.getExternalId();
        this.description = institution.getDescription();
    }

    private String taxId;
    private String description;
}
