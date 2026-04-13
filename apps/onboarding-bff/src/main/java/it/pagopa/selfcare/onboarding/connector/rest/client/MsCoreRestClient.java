package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.institutions.Institution;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.CreateInstitutionData;
import it.pagopa.selfcare.onboarding.connector.rest.model.OnboardingInstitutionRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "ms_core")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.client.auth.AuthenticationPropagationHeadersFactory.class)
public interface MsCoreRestClient {

    @POST
    @Path("/onboarding/institution")
    @Consumes(APPLICATION_JSON)
    void onboardingOrganization(OnboardingInstitutionRequest request);

    @GET
    @Path("/external/institutions/{externalId}")
    @Produces(APPLICATION_JSON)
    Institution getInstitutionByExternalId(@PathParam("externalId") String externalId);

    @POST
    @Path("/institutions/pg")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Institution createInstitutionUsingInstitutionData(CreateInstitutionData request);

    @HEAD
    @Path("/onboarding/institution/{externalId}/products/{productId}")
    void verifyOnboarding(@PathParam("externalId") String externalInstitutionId,
                          @PathParam("productId") String productId);
}
