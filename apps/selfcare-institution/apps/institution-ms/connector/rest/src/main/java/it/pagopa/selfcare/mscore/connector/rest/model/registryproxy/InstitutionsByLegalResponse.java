package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import lombok.Data;

import java.util.List;

@Data
public class InstitutionsByLegalResponse {

    private List<Institutions> businesses;
    private String legalTaxId;
    private String requestDateTime;

}
