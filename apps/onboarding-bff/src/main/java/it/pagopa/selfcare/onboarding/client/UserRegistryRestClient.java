package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.transport.ReplayOnConnectionDrop;
import it.pagopa.selfcare.onboarding.client.model.EmbeddedExternalId;
import it.pagopa.selfcare.onboarding.client.model.MutableUserFieldsDto;
import it.pagopa.selfcare.onboarding.client.model.RegistryUser;
import it.pagopa.selfcare.onboarding.client.model.SaveUserDto;
import it.pagopa.selfcare.onboarding.client.model.UserId;
import it.pagopa.selfcare.onboarding.security.AuthenticationPropagationHeadersFactory;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * User registry (PDV) client. Like every client of the Spring BFF it sends the registry api key together with the
 * caller credentials (bearer token and tenant header).
 */
@RegisterRestClient(configKey = "user_registry_json")
@ReplayOnConnectionDrop
@RegisterClientHeaders(AuthenticationPropagationHeadersFactory.class)
@ClientHeaderParam(name = "x-api-key", value = "${rest-client.user-registry.api-key}")
@Path("/users")
public interface UserRegistryRestClient {

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    RegistryUser search(EmbeddedExternalId externalId, @QueryParam("fl") List<String> fields);

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void patchUser(@PathParam("id") UUID id, MutableUserFieldsDto request);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    RegistryUser getUserByInternalId(@PathParam("id") UUID id, @QueryParam("fl") List<String> fields);

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    UserId saveUser(SaveUserDto request);

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void deleteById(@PathParam("id") UUID id);

    /** One {@code fl} query parameter per field, in enum order, as the previous implementation sent them. */
    static List<String> toFieldList(EnumSet<RegistryUser.Fields> fields) {
        return fields.stream().map(Enum::name).toList();
    }
}
