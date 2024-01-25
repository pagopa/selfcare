package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
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
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Objects;

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
                                                       @QueryParam(value = "institutionId") String institutionId) {
        return userEventService.sendUpdateUserNotificationToQueue(userId, institutionId);
    }
}

