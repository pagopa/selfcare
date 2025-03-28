package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.controller.response.UsersCountResponse;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.service.UserService;
import it.pagopa.selfcare.user.util.GeneralUtils;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.openapi.quarkus.user_registry_json.model.Problem;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

@Authenticated
@Path("/institutions")
@Tag(name = "Institution")
@AllArgsConstructor
public class InstitutionController {

    private final UserService userService;

    @Operation(
            summary = "Retrieve user's information including product role details",
            operationId = "getInstitutionUsersUsingGET",
            description = "Fetches detailed information about users associated with a specific institution, including their roles on various products. This endpoint is useful for administrators to obtain comprehensive user-role mappings within an institution."
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserProductResponse.class, type = SchemaType.ARRAY), mediaType = "application/json")),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "401", description = "Not Authorized", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @GET
    @Path(value = "/{institutionId}/users")
    @Tag(name = "support")
    @Tag(name = "support-pnpg")
    @Tag(name = "Institution")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<UserProductResponse> getInstitutionUsers(@PathParam(value = "institutionId") String institutionId) {
        return userService.getUserProductsByInstitution(institutionId);
    }

    @Operation(
            summary = "Retrieve users with optional filters",
            description = "Fetches a list of users associated with a specific institution, applying optional filters such as userId, roles, states, products, and productRoles. This allows for flexible querying based on various user attributes and statuses."
    )
    @GET
    @Path(value = "/{institutionId}/user-institutions")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<UserInstitutionResponse> retrieveUserInstitutions(@PathParam(value = "institutionId") String institutionId,
                                                        @QueryParam(value = "userId") String userId,
                                                        @QueryParam(value = "roles") List<String> roles,
                                                        @QueryParam(value = "states") List<String> states,
                                                        @QueryParam(value = "products") List<String> products,
                                                        @QueryParam(value = "productRoles") List<String> productRoles) {
        return userService.findAllUserInstitutions(institutionId, userId, roles, states, products, productRoles);
    }

    @Operation(
            summary = "Get the number of users for a certain product of an institution with a certain role and status",
            description = "Count the number of users associated with a specific product of an institution, with an optional filter by roles and status. If no filter is specified count users with any role in status ACTIVE"
    )
    @GET
    @Path(value = "/{institutionId}/products/{productId}/users/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UsersCountResponse> getUsersCount(@PathParam(value = "institutionId") String institutionId,
                                                 @PathParam(value = "productId") String productId,
                                                 @QueryParam(value = "roles") List<String> roles,
                                                 @QueryParam(value = "status") List<String> status) {
        final List<PartyRole> roleList = GeneralUtils.parseEnumList(roles, PartyRole.class);
        final List<OnboardedProductState> statusList = GeneralUtils.parseEnumList(status, OnboardedProductState.class);
        return userService.getUsersCount(institutionId, productId, roleList, statusList);
    }

    @Operation(
            summary = "Update user's onboarded product creation date",
            description = "Updates the `createdAt` timestamp for a user's onboarded product based on the provided institutionId, productId, and list of userIds. This is useful for tracking when a user was onboarded to a specific product within an institution."
    )
    @PUT
    @Path(value = "/{institutionId}/products/{productId}/created-at")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateUserProductCreatedAt(@PathParam(value = "institutionId") String institutionId,
                                                    @PathParam(value = "productId") String productId,
                                                    @NotNull @QueryParam(value = "userIds") List<String> userIds,
                                                    @NotNull @QueryParam(value = "createdAt") OffsetDateTime createdAt) {
        return userService.updateUserProductCreatedAt(institutionId, userIds, productId, createdAt)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Update institution's description across all userInstitution records",
            description = "Modifies the description field in all occurrences of `userInstitution` entities associated with a given institutionId. This ensures that the institution's descriptive information is consistently updated across all related user records."
    )
    @PUT
    @Path(value = "/{institutionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateInstitutionDescription(@PathParam(value = "institutionId") String institutionId,
                                                      @Valid UpdateDescriptionDto descriptionDto) {
        return userService.updateInstitutionDescription(institutionId, descriptionDto)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    @Operation(
            summary = "Logically delete all the users associated with a product of an institution",
            description = "Set the status of all the users associated with a product of an institution to DELETED"
    )
    @APIResponses(value = {
            @APIResponse(responseCode = "204", description = "No Content"),
            @APIResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "401", description = "Not Authorized", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json")),
            @APIResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema(implementation = Problem.class), mediaType = "application/problem+json"))
    })
    @DELETE
    @Path(value = "/{institutionId}/products/{productId}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> deleteUserInstitutionProductUsers(@PathParam(value = "institutionId") String institutionId,
                                                           @PathParam(value = "productId") String productId) {
        return userService.deleteUserInstitutionProductUsers(institutionId.replaceAll("[^a-zA-Z0-9-_]", ""),
                        productId.replaceAll("[^a-zA-Z0-9-_]", ""))
                .map(ignore -> Response.status(HttpStatus.SC_NO_CONTENT).build());
    }

}
