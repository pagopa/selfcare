package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserDetailResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.controller.response.UsersNotificationResponse;
import it.pagopa.selfcare.user.controller.response.product.SearchUserDto;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.service.UserRegistryService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

import java.util.List;

import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;

@Authenticated
@Path("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserRegistryService userRegistryService;

    @Operation(summary = "The API retrieves Users' emails using institution id and product id")
    @GET
    @Path(value = "/emails")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<String>> getUsersEmailByInstitutionAndProduct(@NotNull @QueryParam(value = "institutionId") String institutionId,
                                                                  @NotNull @QueryParam(value = "productId") String productId) {
        return userService.getUsersEmails(institutionId, productId);
    }

    /**
     * The getUserInfo function retrieves a user's information given the userId and optional ProductId.
     *
     * @param userId        String
     * @param institutionId String
     * @param productId     String
     * @return A uni&amp;lt;userresponse&amp;gt;
     */
    @Operation(summary = "Retrieves user given userId and optional ProductId")
    @GET
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserResponse> getUserInfo(@PathParam(value = "id") String userId,
                                         @QueryParam(value = "institutionId") String institutionId,
                                         @QueryParam(value = "productId") String productId) {
        return userService.retrievePerson(userId, productId, institutionId)
                .map(user -> userMapper.toUserResponse(user, institutionId));
    }

    @Operation(summary = "Retrieves products info and role which the user is enabled")
    @GET
    @Path("/{userId}/products")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserProductsResponse> getUserProductsInfo(@PathParam(value = "userId") String userId,
                                                         @QueryParam(value = "institutionId") String institutionId,
                                                         @QueryParam(value = "states") String[] states) {
        return userService.retrieveBindings(institutionId, userId, states)
                .map(userMapper::toUserProductsResponse);
    }

    /**
     * getUserById function returns all the information of a user retrieved from pdv given the userId
     * @param userId String
     * @return A uni UserDetailResponse
     */
    @Operation(summary = "Retrieves user's information from pdv: name, familyName, email, fiscalCode and workContacts")
    @GET
    @Path("/{id}/details")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserDetailResponse> getUserDetailsById(@PathParam(value = "id") String userId) {
        return userService.getUserById(userId).onItem().transform(userMapper::toUserResponse);
    }

    @Operation(summary = "Search user by fiscalCode")
    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UserDetailResponse> searchUserByFiscalCode(@RequestBody SearchUserDto dto){
        return userService.searchUserByFiscalCode(dto.getFiscalCode()).onItem().transform(userMapper::toUserResponse);
    }

    /**
     * The deleteProducts function is used to delete logically the association institution and product.
     *
     * @param userId        String
     * @param institutionId String
     * @param productId     String
     * @return A uni&lt;void&gt;
     */
    @Operation(summary = "Delete logically the association institution and product")
    @DELETE
    @Path(value = "/{userId}/institutions/{institutionId}/products/{productId}")
    public Uni<Void> deleteProducts(@PathParam(value = "userId") String userId,
                                    @PathParam(value = "institutionId") String institutionId,
                                    @PathParam(value = "productId") String productId) {
        return userService.deleteUserInstitutionProduct(userId, institutionId, productId);
    }

    /**
     * The updateUserStatus function updates the status of a user's product.
     *
     * @param userId        String
     * @param institutionId String
     * @param productId     String
     * @param role          PartyRole
     * @param productRole   String
     * @param status        OnboardedProductState
     * @return A uni&lt;response&gt;
     */
    @Operation(summary = "Update user status with optional filter for institution, product, role and productRole")
    @PUT
    @Path(value = "/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateUserStatus(@PathParam(value = "id") String userId,
                                          @QueryParam(value = "institutionId") String institutionId,
                                          @QueryParam(value = "productId") String productId,
                                          @QueryParam(value = "role") PartyRole role,
                                          @QueryParam(value = "productRole") String productRole,
                                          @QueryParam(value = "status") OnboardedProductState status) {
        log.debug("updateProductStatus - userId: {}", userId);
        return userService.updateUserStatusWithOptionalFilter(userId, institutionId, productId, role, productRole, status)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    /**
     * Retreive all the users given a list of userIds
     *
     * @param userIds List<String></String>
     */
    @Operation(
            summary = "Retrieve all users given their userIds"
    )
    @GET
    @Path("/ids")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<UserInstitutionResponse>> findAllByIds(@QueryParam(value = "userIds") List<String> userIds) {
        return userService.findAllByIds(formatQueryParameterList(userIds));
    }

    @Operation(summary = "Retrieve all SC-User for DataLake filtered by optional productId")
    @GET
    @Path(value = "/notification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UsersNotificationResponse> getUsers(@QueryParam(value = "page") @DefaultValue("0") Integer page,
                                                   @QueryParam(value = "size") @DefaultValue("100") Integer size,
                                                   @QueryParam(value = "productId") String productId) {
        return userService.findPaginatedUserNotificationToSend(size, page, productId)
                .map(userNotificationToSends -> {
                    UsersNotificationResponse usersNotificationResponse = new UsersNotificationResponse();
                    usersNotificationResponse.setUsers(userNotificationToSends.stream()
                            .map(userMapper::toUserNotification)
                            .toList());
                    return usersNotificationResponse;
                });
    }

    @Operation(summary = "The API retrieves paged users with optional filters in input as query params")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<UserInstitutionResponse> retrievePaginatedAndFilteredUser(@QueryParam(value = "institutionId") String institutionId,
                                                                           @QueryParam(value = "userId") String userId,
                                                                           @QueryParam(value = "roles") List<PartyRole> roles,
                                                                           @QueryParam(value = "states") List<String> states,
                                                                           @QueryParam(value = "products") List<String> products,
                                                                           @QueryParam(value = "productRoles") List<String> productRoles,
                                                                           @QueryParam(value = "page") @DefaultValue("0") Integer page,
                                                                           @QueryParam(value = "size") @DefaultValue("100") Integer size) {
        return userService.findPaginatedUserInstitutions(institutionId, userId, roles, states, products, productRoles, page, size);
    }

    /**
     * The updateUserRegistryAndSendNotification function is a service that sends notification when user data get's updated.
     *
     * @param userId        String
     * @param institutionId String
     * @return Uni&lt;response&gt;
     */
    @Operation(summary = "Service to update user in user-registry and send notification when user data gets updated")
    @ResponseStatus(HttpStatus.SC_NO_CONTENT)
    @PUT
    @Path("/{id}/user-registry")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> updateUserRegistryAndSendNotification(@PathParam(value = "id") String userId,
                                                               @QueryParam(value = "institutionId") String institutionId,
                                                               MutableUserFieldsDto userDto) {
        return userRegistryService.updateUserRegistryAndSendNotificationToQueue(userDto, userId, institutionId)
                .map(ignore -> Response
                        .status(HttpStatus.SC_NO_CONTENT)
                        .build());
    }

    /**
     * The updateUserProductStatus function is a service to update user product status.
     *
     * @param userId        String
     * @param institutionId String
     * @param productId     String
     * @param status        OnboardedProductState
     * @return Uni&lt;void&gt;
     */
    @Operation(summary = "Service to update user product status")
    @ResponseStatus(HttpStatus.SC_NO_CONTENT)
    @PUT
    @Path("/{id}/institution/{institutionId}/product/{productId}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> updateUserProductStatus(@PathParam("id") String userId,
                                             @PathParam("institutionId") String institutionId,
                                             @PathParam("productId") String productId,
                                             @NotNull @QueryParam("status") OnboardedProductState status,
                                             @Context SecurityContext ctx) {
        return readUserIdFromToken(ctx)
                .onItem().transformToUni(loggedUser -> userService.updateUserProductStatus(userId, institutionId, productId, status, loggedUser));
    }

    @Operation(summary = "Create or Update user")
    @ResponseStatus(HttpStatus.SC_NO_CONTENT)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createOrUpdate(@Valid CreateUserDto userDto) {
        return userService.createOrUpdateUser(userDto)
                .map(ignore -> Response.status(HttpStatus.SC_NO_CONTENT).build());
    }

    private Uni<LoggedUser> readUserIdFromToken(SecurityContext ctx) {
        return currentIdentityAssociation.getDeferredIdentity()
                .onItem().transformToUni(identity -> {
                    if (ctx.getUserPrincipal() == null || !ctx.getUserPrincipal().getName().equals(identity.getPrincipal().getName())) {
                        return Uni.createFrom().failure(new InternalServerErrorException("Principal and JsonWebToken names do not match"));
                    }

                    if (identity.getPrincipal() instanceof DefaultJWTCallerPrincipal jwtCallerPrincipal) {
                        String uid = jwtCallerPrincipal.getClaim("uid");
                        String familyName = jwtCallerPrincipal.getClaim("family_name");
                        String name = jwtCallerPrincipal.getClaim("name");
                        return Uni.createFrom().item(
                                LoggedUser.builder()
                                        .uid(uid)
                                        .familyName(familyName)
                                        .name(name)
                                        .build()
                        );
                    }
                    return Uni.createFrom().nullItem();
                });
    }
}

