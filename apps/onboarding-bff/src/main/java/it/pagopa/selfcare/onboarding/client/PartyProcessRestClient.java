package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.transport.ReplayOnConnectionDrop;
import it.pagopa.selfcare.onboarding.client.model.BillingDataResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionFromIpaPost;
import it.pagopa.selfcare.onboarding.client.model.InstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionSeed;
import it.pagopa.selfcare.onboarding.client.model.InstitutionsResponse;
import it.pagopa.selfcare.onboarding.client.model.OnboardingInstitutionRequest;
import it.pagopa.selfcare.onboarding.client.model.OnboardingsResponse;
import it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Party process (ms-core) client. The paths are the ones the previous implementation used; the trailing
 * slashes of the institution creation operations are part of the downstream contract.
 */
@RegisterRestClient(configKey = "party_process")
@ReplayOnConnectionDrop
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory.class)
public interface PartyProcessRestClient {

    @POST
    @Path("/onboarding/institution")
    @Consumes(APPLICATION_JSON)
    void onboardingOrganization(OnboardingInstitutionRequest request);

    @HEAD
    @Path("/onboarding/institution/{externalId}/products/{productId}")
    void verifyOnboarding(@PathParam("externalId") String externalInstitutionId,
                          @PathParam("productId") String productId);

    @HEAD
    @Path("/onboarding/verify")
    void verifyOnboardingInfoByFilters(@QueryParam("productId") String productId,
                                       @QueryParam("externalId") String externalId,
                                       @QueryParam("taxCode") String taxCode,
                                       @QueryParam("origin") String origin,
                                       @QueryParam("originId") String originId,
                                       @QueryParam("subunitCode") String subunitCode);

    @GET
    @Path("/institutions/{institutionId}/onboardings")
    @Produces(APPLICATION_JSON)
    OnboardingsResponse getOnboardings(@PathParam("institutionId") String institutionId,
                                       @QueryParam("productId") String productId);

    @GET
    @Path("/external/institutions/{externalId}")
    @Produces(APPLICATION_JSON)
    InstitutionResponse getInstitutionByExternalId(@PathParam("externalId") String externalId);

    @GET
    @Path("/institutions")
    @Produces(APPLICATION_JSON)
    InstitutionsResponse getInstitutions(@QueryParam("taxCode") String taxCode,
                                         @QueryParam("subunitCode") String subunitCode);

    @GET
    @Path("/institutions/{institutionId}")
    @Produces(APPLICATION_JSON)
    InstitutionResponse getInstitutionById(@PathParam("institutionId") String institutionId,
                                           @QueryParam("productId") String productId);

    @POST
    @Path("/institutions/from-ipa/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromIpa(InstitutionFromIpaPost institutionFromIpaPost);

    @POST
    @Path("/institutions/from-anac/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromANAC(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/from-ivass/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromIVASS(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/from-infocamere/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromInfocamere(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitution(InstitutionSeed institutionSeed);

    @GET
    @Path("/external/institutions/{externalId}/products/{productId}/billing")
    @Produces(APPLICATION_JSON)
    BillingDataResponse getInstitutionBillingData(@PathParam("externalId") String externalId,
                                                  @PathParam("productId") String productId);
}
