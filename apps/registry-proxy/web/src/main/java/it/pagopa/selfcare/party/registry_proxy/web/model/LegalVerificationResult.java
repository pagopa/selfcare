package it.pagopa.selfcare.party.registry_proxy.web.model;

import lombok.Data;

@Data
public class LegalVerificationResult {

    private boolean verificationResult;
    private String resultCode;
    private String resultDetail;
}
