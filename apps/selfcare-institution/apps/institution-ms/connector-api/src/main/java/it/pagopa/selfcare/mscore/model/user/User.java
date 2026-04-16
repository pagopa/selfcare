package it.pagopa.selfcare.mscore.model.user;

import it.pagopa.selfcare.mscore.model.CertifiedField;
import it.pagopa.selfcare.mscore.model.institution.WorkContact;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Data
@FieldNameConstants(asEnum = true)
public class User {

    @FieldNameConstants.Exclude
    private String id;
    private String fiscalCode;
    private CertifiedField<String> name;
    private CertifiedField<String> familyName;
    @FieldNameConstants.Exclude
    private CertifiedField<String> email;

    private Map<String, WorkContact> workContacts;

    public String getName() {
        return name != null ? name.getValue() : "";
    }

    public String getFamilyName() {
        return familyName != null ? familyName.getValue() : "";
    }

    public String getEmail() {
        return email != null ? email.getValue() : "";
    }
}
