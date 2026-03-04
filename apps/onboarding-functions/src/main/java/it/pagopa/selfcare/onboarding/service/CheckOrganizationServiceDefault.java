package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.client.external.ExternalRestClient;
import it.pagopa.selfcare.onboarding.client.external.ExternalTokenRestClient;
import it.pagopa.selfcare.onboarding.config.ExternalConfig;
import it.pagopa.selfcare.onboarding.dto.OauthToken;
import it.pagopa.selfcare.onboarding.dto.OrganizationLightBeanResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Form;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.Objects;

@ApplicationScoped
public class CheckOrganizationServiceDefault implements CheckOrganizationService {
    @RestClient
    @Inject
    ExternalRestClient externalRestClient;

    @RestClient
    @Inject
    ExternalTokenRestClient externalTokenRestClient;

    private final ExternalConfig externalConfig;

    public CheckOrganizationServiceDefault(ExternalConfig externalConfig) {
        this.externalConfig = externalConfig;
    }

    @Override
    public String testToken(ExecutionContext context) {
        context.getLogger().info("testToken start");

        Form form = buildTokenEntity();
        context.getLogger().info(String.format("testToken calling external service with form %s", form.asMap()));

        OauthToken oauthToken = externalTokenRestClient.getToken(buildTokenEntity());
        context.getLogger().info(() -> String.format("testToken response %s", Objects.nonNull(oauthToken) ? oauthToken.toString() : null));
        context.getLogger().info("testToken end");
        return oauthToken.getAccessToken();
    }

    @Override
    public boolean checkOrganization(ExecutionContext context, String fiscalCode, String vatNumber) {
        context.getLogger().info("testCheckOrganization start");
        boolean alreadyRegistered;
        if (externalConfig.byPassCheckOrganization()) {
            context.getLogger().info("byPassCheckOrganization is true, skipping check");
            alreadyRegistered = false;
        } else {
            context.getLogger().info("byPassCheckOrganization is false, performing check");

            OauthToken oauthToken = getToken(context);
            context.getLogger().info("performing checkOrganization");
            String bearerToken = "Bearer " + oauthToken.getAccessToken();
            OrganizationLightBeanResponse response = externalRestClient.checkOrganization(fiscalCode, vatNumber, bearerToken);
            context.getLogger().info(() -> String.format("checkOrganization result = %s", Objects.nonNull(response) ? response.toString() : null));
            context.getLogger().info("checkOrganization end");
            alreadyRegistered = response.isAlreadyRegistered();
        }

        return alreadyRegistered;
    }

    private OauthToken getToken(ExecutionContext context) {
        Form form = buildTokenEntity();
        context.getLogger().info(String.format("calling getToken with form %s", form.asMap()));
        OauthToken oauthToken = externalTokenRestClient.getToken(buildTokenEntity());
        context.getLogger().info(() -> String.format("getToken response %s", Objects.nonNull(oauthToken) ? oauthToken.toString() : null));
        return oauthToken;
    }

    private Form buildTokenEntity() {
        Form form = new Form();
        form.param("grant_type", externalConfig.grantType());
        form.param("client_id", externalConfig.clientId());
        form.param("client_secret", externalConfig.clientSecret());
        return form;
    }
}
