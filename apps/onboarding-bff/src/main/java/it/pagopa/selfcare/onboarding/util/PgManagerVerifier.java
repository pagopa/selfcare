package it.pagopa.selfcare.onboarding.util;

import it.pagopa.selfcare.onboarding.util.LogUtils;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyClient;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.client.model.ManagerVerification;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.BusinessInfoIC;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

import java.util.Objects;

@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class PgManagerVerifier {
    private final PartyRegistryProxyClient partyRegistryProxyConnector;

    public ManagerVerification doVerify(String userTaxCode, String institutionTaxCode) {
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "Checking if user with taxCode {} is manager of institution with taxCode {} on INFOCAMERE", Encode.forJava(userTaxCode), Encode.forJava(institutionTaxCode));
        InstitutionInfoIC institutionInfoIC = partyRegistryProxyConnector.getInstitutionsByUserFiscalCode(userTaxCode);
        if (Objects.nonNull(institutionInfoIC) && Objects.nonNull(institutionInfoIC.getBusinesses())){
            for (BusinessInfoIC business : institutionInfoIC.getBusinesses()) {
                if (institutionTaxCode.equals(business.getBusinessTaxId())) {
                    log.debug("User found as manager in INFOCAMERE for business with name = {}", business.getBusinessName());
                    return new ManagerVerification(Origin.INFOCAMERE.getValue(), business.getBusinessName(), true);
                }
            }
        }

        try {
            log.debug(LogUtils.CONFIDENTIAL_MARKER, "Checking if user with taxCode {} is manager of institution with taxCode {} on ADE", Encode.forJava(userTaxCode), Encode.forJava(institutionTaxCode));
            MatchInfoResult matchInfoResult = partyRegistryProxyConnector.matchInstitutionAndUser(institutionTaxCode, userTaxCode);
            if (Objects.nonNull(matchInfoResult) && matchInfoResult.isVerificationResult()) {
                log.debug("User found as manager in ADE, response = {}", matchInfoResult);
                return new ManagerVerification(Origin.ADE.getValue(), null, true);
            }
        } catch (InvalidRequestException e) {
            log.debug("User not found as manager in ADE");
            return new ManagerVerification(false);
        }

        log.debug("User not found as manager in INFOCAMERE or ADE");
        return new ManagerVerification(false);
    }

}
