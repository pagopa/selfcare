package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.controller.response.UsersNotificationResponse;
import it.pagopa.selfcare.user.controller.response.product.UserProductsResponse;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.service.UserEventService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

import java.util.List;

import static it.pagopa.selfcare.user.util.GeneralUtils.formatQueryParameterList;

@Authenticated
@Path("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserEventService userEventService;

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
     * The deleteProducts function is used to delete logically the association institution and product.
     *
     * @param userId String
     * @param institutionId String
     * @param productId String
     *
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
     * @param userIds   List<String></String>
     * @return
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

    @Operation(summary = "Retrieve all users according to optional params in input")
    @GET
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


    /**
     * The sendUpdateUserNotificationToQueue function is a service that sends notification when user data get's updated.
     *
     * @param userId String
     * @param institutionId String
     *
     * @return Uni&lt;response&gt;
     *
     */
    @Operation(summary = "Service to send notification when user data gets updated")
    @ResponseStatus(HttpStatus.SC_NO_CONTENT)
    @POST
    @Path("/{id}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Void> sendUpdateUserNotificationToQueue(@PathParam(value = "id") String userId,
                                                       @QueryParam(value = "institutionId") String institutionId,
                                                       MutableUserFieldsDto userDto) {
        return userEventService.sendUpdateUserNotificationToQueue(userDto, userId, institutionId);
    }
}

