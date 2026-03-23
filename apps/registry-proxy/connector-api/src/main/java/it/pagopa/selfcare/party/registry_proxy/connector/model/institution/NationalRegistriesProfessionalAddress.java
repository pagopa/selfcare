package it.pagopa.selfcare.party.registry_proxy.connector.model.institution;

import lombok.Data;

@Data
public class NationalRegistriesProfessionalAddress {
    private String description;
    private String municipality;
    private String province;
    private String address;
    private String zipCode;
}
