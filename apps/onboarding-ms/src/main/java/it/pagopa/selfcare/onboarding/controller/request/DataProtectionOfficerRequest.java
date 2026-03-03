package it.pagopa.selfcare.onboarding.controller.request;

import lombok.Data;

@Data
public class DataProtectionOfficerRequest {
    private String address;
    private String email;
    private String pec;
}
