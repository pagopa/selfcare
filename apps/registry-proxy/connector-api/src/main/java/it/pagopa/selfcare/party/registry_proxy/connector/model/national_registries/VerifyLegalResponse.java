package it.pagopa.selfcare.party.registry_proxy.connector.model.national_registries;

import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultCodeEnum;
import it.pagopa.selfcare.party.registry_proxy.connector.constant.AdEResultDetailEnum;
import lombok.Data;

@Data
public class VerifyLegalResponse {

    private boolean verificationResult = false;
    private AdEResultCodeEnum verifyLegalResultCode;
    private AdEResultDetailEnum verifyLegalResultDetail;
    private String verifyLegalResultDetailMessage;
}
