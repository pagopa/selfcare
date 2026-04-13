package it.pagopa.selfcare.onboarding.client;

import it.pagopa.selfcare.onboarding.client.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;

import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

@RegisterRestClient(configKey = "user_registry_json")
@ClientHeaderParam(name = "x-api-key", value = "${USERVICE_USER_REGISTRY_API_KEY:api-key}")
public interface UserRegistryRestClient extends UserApi {

    @POST
    @Path("/users/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    User searchByExternalId(EmbeddedExternalId externalId, @QueryParam("fl") String fields);

    default User search(EmbeddedExternalId externalId, EnumSet<User.Fields> fields) {
        return searchByExternalId(externalId, toFieldList(fields));
    }

    @PATCH
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void patchUser(@PathParam("id") UUID id, MutableUserFieldsDto request);

    @GET
    @Path("/users/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    User getUserByInternalIdRaw(@PathParam("id") UUID id, @QueryParam("fl") String fieldList);

    default User getUserByInternalId(UUID id, EnumSet<User.Fields> fieldList) {
        return getUserByInternalIdRaw(id, toFieldList(fieldList));
    }

    @PATCH
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    UserId saveUser(SaveUserDto request);

    @DELETE
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    void deleteById(@PathParam("id") UUID id);

    default UserResource _searchUsingPOST(String fl, UserSearchDto userSearchDto) {
        return searchUsingPOST(fl, userSearchDto).await().indefinitely();
    }

    private static String toFieldList(EnumSet<User.Fields> fields) {
        return fields.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
