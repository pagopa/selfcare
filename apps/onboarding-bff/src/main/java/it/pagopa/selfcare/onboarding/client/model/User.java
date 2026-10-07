package it.pagopa.selfcare.onboarding.client.model;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;
import java.util.Map;

@Data
public class User {

    private String id;
    private CertifiedField<String> name;
    private CertifiedField<String> familyName;
    private String surname; // Used in some DTOs, but familyName is the standard
    private String taxCode;
    private PartyRole role;
    private CertifiedField<String> email;
    private String productRole;
    private Map<String, WorkContact> workContacts;

    public enum Fields {
        id,
        name,
        familyName,
        surname,
        email,
        fiscalCode,
        workContacts
    }
}
