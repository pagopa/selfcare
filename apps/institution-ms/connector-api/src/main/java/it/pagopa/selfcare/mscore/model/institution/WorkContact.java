package it.pagopa.selfcare.mscore.model.institution;

import it.pagopa.selfcare.mscore.model.CertifiedField;
import lombok.Data;

@Data
public class WorkContact {
    private CertifiedField<String> email;

    public String getEmail() {
        return email != null ? email.getValue() : "";
    }
}
