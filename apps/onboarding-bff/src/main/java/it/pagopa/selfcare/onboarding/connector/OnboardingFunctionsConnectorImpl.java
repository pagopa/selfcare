package it.pagopa.selfcare.onboarding.connector;

import it.pagopa.selfcare.onboarding.connector.api.OnboardingFunctionsConnector;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;
import org.openapi.quarkus.onboarding_functions_json.api.OrganizationApi;

@ApplicationScoped
@Slf4j
public class OnboardingFunctionsConnectorImpl implements OnboardingFunctionsConnector {
    private final OrganizationApi onboardingFunctionsApiClient;

    public OnboardingFunctionsConnectorImpl(@RestClient OrganizationApi onboardingFunctionsApiClient) {
        this.onboardingFunctionsApiClient = onboardingFunctionsApiClient;
    }

    @Override
    public void checkOrganization(String fiscalCode, String vatNumber) {
        log.trace("checkOrganization start");
        if (fiscalCode.matches("\\w*") && vatNumber.matches("\\w*")) {
            log.debug("checkOrganization fiscalCode = {}, vatNumber = {}", fiscalCode, vatNumber );
        }
        onboardingFunctionsApiClient.checkOrganization(fiscalCode, vatNumber).await().indefinitely();
        log.trace("checkOrganization end");
    }
}
