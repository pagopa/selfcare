package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;

import java.util.Map;

/** User as stored by the user registry (certified fields). */
@Data
public class RegistryUser {

    private String id;
    private String fiscalCode;
    private CertifiedField<String> name;
    private CertifiedField<String> familyName;
    private CertifiedField<String> email;
    private Map<String, WorkContact> workContacts;

    /** Names of the fields requested to the user registry through the {@code fl} query parameter. */
    public enum Fields {
        fiscalCode,
        name,
        familyName,
        email,
        workContacts
    }
}
