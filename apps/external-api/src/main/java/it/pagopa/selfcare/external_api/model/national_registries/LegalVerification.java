package it.pagopa.selfcare.external_api.model.national_registries;

import lombok.Data;

@Data
public class LegalVerification {
    private String resultCode;
    private String resultDetail;
    private Boolean verificationResult;
}
