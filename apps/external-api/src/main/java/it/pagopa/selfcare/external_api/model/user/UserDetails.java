package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.user.generated.openapi.v1.dto.WorkContactResponse;
import lombok.Data;

import jakarta.validation.Valid;
import java.util.Map;

@Data
public class UserDetails {
    private String id;

    private String fiscalCode;

    private String name;

    private String familyName;

    private String email;

    @Valid
    private Map<String, WorkContactResponse> workContacts = null;
}
