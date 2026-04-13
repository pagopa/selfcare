package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.model.InstitutionLegalAddressData;
import it.pagopa.selfcare.onboarding.client.model.MatchInfoResult;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RegisterRestClient(configKey = "party_registry_proxy")
@RegisterClientHeaders(it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory.class)
public interface PartyRegistryProxyRestClient {

    @POST
    @Path("/info-camere/institutions")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    InstitutionInfoIC getInstitutionsByUserLegalTaxId(InstitutionByLegalTaxIdRequest request);

    @GET
    @Path("/national-registries/verify-legal")
    @Produces(APPLICATION_JSON)
    MatchInfoResult matchInstitutionAndUser(@QueryParam("vatNumber") String institutionExternalId,
                                            @QueryParam("taxId") String taxCode);

    @GET
    @Path("/national-registries/legal-address")
    @Produces(APPLICATION_JSON)
    InstitutionLegalAddressData getInstitutionLegalAddress(@QueryParam("taxId") String externalInstitutionId);

    @GET
    @Path("/institutions/{institutionId}")
    @Consumes(APPLICATION_JSON)
    ProxyInstitutionResponse getInstitutionById(@PathParam("institutionId") String id);

    @GET
    @Path("/geotaxonomies/{geotax_id}")
    @Consumes(APPLICATION_JSON)
    GeographicTaxonomiesResponse getExtByCode(@PathParam("geotax_id") String code);

    @GET
    @Path("/aoo/{aooId}")
    @Consumes(APPLICATION_JSON)
    AooResponse getAooById(@PathParam("aooId") String aooId);

    @GET
    @Path("/uo/{uoId}")
    @Consumes(APPLICATION_JSON)
    UoResponse getUoById(@PathParam("uoId") String uoId);
}
