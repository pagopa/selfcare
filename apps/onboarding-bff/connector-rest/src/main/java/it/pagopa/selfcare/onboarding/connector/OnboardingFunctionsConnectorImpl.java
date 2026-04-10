package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.OnboardingFunctionsConnector;
import it.pagopa.selfcare.onboarding.connector.rest.client.OnboardingFunctionsApiClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Slf4j
public class OnboardingFunctionsConnectorImpl implements OnboardingFunctionsConnector {
    private final OnboardingFunctionsApiClient onboardingFunctionsApiClient;

    public OnboardingFunctionsConnectorImpl(@RestClient OnboardingFunctionsApiClient onboardingFunctionsApiClient) {
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
