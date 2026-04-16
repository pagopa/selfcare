package it.pagopa.selfcare.party.registry_proxy.web.model;

import lombok.Data;

@Data
public class GetInstitutionsByLegalFilterDto {
    private String legalTaxId;
}
