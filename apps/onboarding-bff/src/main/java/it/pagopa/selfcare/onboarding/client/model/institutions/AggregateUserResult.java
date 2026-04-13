package it.pagopa.selfcare.onboarding.client.model.institutions;

import lombok.Data;

@Data
public class AggregateUserResult {
    private String name;
    private String surname;
    private String taxCode;
    private String email;
    private String role;
}
