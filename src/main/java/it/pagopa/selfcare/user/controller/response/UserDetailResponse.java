package it.pagopa.selfcare.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailResponse {
    private String id;
    private String fiscalCode;
    private CertifiableFieldResponse<String> name;
    private CertifiableFieldResponse<String> familyName;
    private CertifiableFieldResponse<String> email;
    private Map<String, WorkContactResponse> workContacts;
}
