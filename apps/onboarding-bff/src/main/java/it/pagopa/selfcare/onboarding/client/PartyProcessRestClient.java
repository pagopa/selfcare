package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.client.model.RelationshipInfo;
import it.pagopa.selfcare.onboarding.client.model.RelationshipState;
import it.pagopa.selfcare.onboarding.client.model.RelationshipsResponse;
import it.pagopa.selfcare.onboarding.client.model.BillingDataResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionFromIpaPost;
import it.pagopa.selfcare.onboarding.client.model.InstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionSeed;
import it.pagopa.selfcare.onboarding.client.model.InstitutionsResponse;
import it.pagopa.selfcare.onboarding.client.model.OnboardingInstitutionRequest;
import it.pagopa.selfcare.onboarding.client.model.OnboardingsResponse;
import java.util.EnumSet;
import java.util.Set;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.institution_json.api.OnboardingApi;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "party_process")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.client.auth.AuthenticationPropagationHeadersFactory.class)
public interface PartyProcessRestClient extends OnboardingApi {

    @GET
    @Path("/external/institutions/{externalId}/relationships")
    @Produces(APPLICATION_JSON)
    RelationshipsResponse getUserInstitutionRelationships(@PathParam("externalId") String institutionId,
                                                          @QueryParam("roles") EnumSet<PartyRole> roles,
                                                          @QueryParam("states") EnumSet<RelationshipState> states,
                                                          @QueryParam("products") Set<String> productIds,
                                                          @QueryParam("productRoles") Set<String> productRoles,
                                                          @QueryParam("personId") String personId);

    @POST
    @Path("/onboarding/institution")
    void onboardingOrganization(OnboardingInstitutionRequest request);

    @GET
    @Path("/institutions/{institutionId}/onboardings/")
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

    @POST
    @Path("/institutions/from-ipa/")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromIpa(InstitutionFromIpaPost institutionFromIpaPost);

    @POST
    @Path("/institutions/from-anac/")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromANAC(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/from-ivass/")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromIVASS(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/{externalId}")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionUsingExternalId(@PathParam("externalId") String externalId);

    @POST
    @Path("/institutions/from-infocamere/")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitutionFromInfocamere(InstitutionSeed institutionSeed);

    @POST
    @Path("/institutions/")
    @Produces(APPLICATION_JSON)
    InstitutionResponse createInstitution(InstitutionSeed institutionSeed);

    @GET
    @Path("/external/institutions/{externalId}/products/{productId}/manager")
    @Produces(APPLICATION_JSON)
    RelationshipInfo getInstitutionManager(@PathParam("externalId") String externalId,
                                           @PathParam("productId") String productId);

    @GET
    @Path("/external/institutions/{externalId}/products/{productId}/billing")
    @Produces(APPLICATION_JSON)
    BillingDataResponse getInstitutionBillingData(@PathParam("externalId") String externalId,
                                                  @PathParam("productId") String productId);

    @HEAD
    @Path("/onboarding/institution/{externalId}/products/{productId}")
    void verifyOnboarding(@PathParam("externalId") String externalInstitutionId,
                          @PathParam("productId") String productId);

    @HEAD
    @Path("/onboarding/")
    void verifyOnboarding(@QueryParam("taxCode") String taxCode,
                          @QueryParam("subunitCode") String subunitCode,
                          @QueryParam("productId") String productId);

    @GET
    @Path("/institutions/{institutionId}")
    @Produces(APPLICATION_JSON)
    InstitutionResponse getInstitutionById(@PathParam("institutionId") String institutionId);

    default void _verifyOnboardingInfoByFiltersUsingHEAD(
        String productId,
        String externalId,
        String taxCode,
        String origin,
        String originId,
        String subunitCode
    ) {
        verifyOnboardingInfoByFiltersUsingHEAD(productId, externalId, taxCode, origin, originId, subunitCode)
            .await().indefinitely();
    }
}
