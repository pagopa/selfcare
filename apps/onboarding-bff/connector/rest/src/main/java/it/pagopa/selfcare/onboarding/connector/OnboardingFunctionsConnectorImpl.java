package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.OnboardingFunctionsConnector;
import it.pagopa.selfcare.onboarding.connector.rest.client.OnboardingFunctionsApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OnboardingFunctionsConnectorImpl implements OnboardingFunctionsConnector {
    private final OnboardingFunctionsApiClient onboardingFunctionsApiClient;

    public OnboardingFunctionsConnectorImpl(OnboardingFunctionsApiClient onboardingFunctionsApiClient) {
        this.onboardingFunctionsApiClient = onboardingFunctionsApiClient;
    }

    @Override
    public void checkOrganization(String fiscalCode, String vatNumber) {
        log.trace("checkOrganization start");
        if (fiscalCode.matches("\\w*") && vatNumber.matches("\\w*")) {
            log.debug("checkOrganization fiscalCode = {}, vatNumber = {}", fiscalCode, vatNumber );
        }
        onboardingFunctionsApiClient._checkOrganization(fiscalCode, vatNumber);
        log.trace("checkOrganization end");
    }
}
