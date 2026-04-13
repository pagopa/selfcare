package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.client.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.institution_json.api.OnboardingApi;

import java.util.EnumSet;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "party_process")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory.class)
public interface PartyProcessRestClient extends OnboardingApi {

    @GET
    @Path("/external/institutions/{externalId}/relationships")
    @Produces(APPLICATION_JSON)
    RelationshipsResponse getUserInstitutions(@PathParam("userId") String userId,
                                              @QueryParam("role") PartyRole role,
                                              @QueryParam("states") EnumSet<RelationshipState> states,
                                              @QueryParam("products") List<String> products,
                                              @QueryParam("productRoles") List<String> productRoles,
                                              @QueryParam("institutionId") String institutionId);

    @GET
    @Path("/institutions/{institutionId}/onboarding")
    @Produces(APPLICATION_JSON)
    OnboardingContract getOnboardingContract(@PathParam("institutionId") String institutionId,
                                             @QueryParam("productId") String productId);

    @GET
    @Path("/institutions")
    @Produces(APPLICATION_JSON)
    List<Institution> getInstitutionsByTaxCodeAndSubunitCode(@QueryParam("taxCode") String taxCode,
                                                            @QueryParam("subunitCode") String subunitCode);

    @GET
    @Path("/institutions/{institutionId}")
    @Produces(APPLICATION_JSON)
    Institution getInstitutionById(@PathParam("institutionId") String id);

    @GET
    @Path("/external/institutions/{externalId}")
    @Produces(APPLICATION_JSON)
    Institution getInstitutionByExternalId(@PathParam("externalId") String externalId);

    @GET
    @Path("/institutions/{institutionId}/onboardings")
    @Produces(APPLICATION_JSON)
    List<OnboardingResource> getOnboardings(@PathParam("institutionId") String institutionId,
                                            @QueryParam("productId") String productId);

    @POST
    @Path("/institutions/insert-from-ipa")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    Institution createInstitutionFromIpa(InstitutionFromIpaPost institutionFromIpaPost);

    @POST
    @Path("/institutions")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    Institution createInstitution(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/anac")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    Institution createInstitutionFromANAC(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/ivass")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    Institution createInstitutionFromIVASS(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/infocamere")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    Institution createInstitutionFromInfocamere(InstitutionSeed institutionSeed);

    @POST
    @Path("/onboarding/organization")
    @Consumes(APPLICATION_JSON)
    void onboardingOrganization(OnboardingInstitutionRequest onboardingInstitutionRequest);

    @HEAD
    @Path("/institutions/{externalId}/products/{productId}")
    void verifyOnboarding(@PathParam("externalId") String externalId,
                          @PathParam("productId") String productId);

    @HEAD
    @Path("/onboarding")
    void verifyOnboarding(@QueryParam("productId") String productId,
                          @QueryParam("externalId") String externalId,
                          @QueryParam("taxCode") String taxCode,
                          @QueryParam("origin") String origin,
                          @QueryParam("originId") String originId,
                          @QueryParam("subunitCode") String subunitCode);

    @GET
    @Path("/institutions/{externalId}/billing")
    @Produces(APPLICATION_JSON)
    BillingDataResponse getInstitutionBillingData(@PathParam("externalId") String externalId,
                                                  @QueryParam("productId") String productId);
}
