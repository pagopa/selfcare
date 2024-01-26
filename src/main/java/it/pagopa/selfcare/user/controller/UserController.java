package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserResponse;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.service.UserEventService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

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
     * @param userId String
     * @param institutionId String
     * @param productId String
     *
     * @return A uni&amp;lt;userresponse&amp;gt;
     *
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

}

