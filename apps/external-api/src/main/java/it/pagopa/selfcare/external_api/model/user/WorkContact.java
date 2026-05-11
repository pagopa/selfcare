package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

@Data
public class WorkContact {
    private CertifiedField<String> email;
    private CertifiedField<String> mobilePhone;
    private CertifiedField<String> telephone;
}
