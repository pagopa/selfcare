package it.pagopa.selfcare.dashboard.model.user;

import lombok.Data;

import java.util.Map;

@Data
public class MutableUserFieldsDto {

    private CertifiedField<String> name;
    private CertifiedField<String> familyName;
    private CertifiedField<String> email;
    private Map<String, WorkContact> workContacts;

}
