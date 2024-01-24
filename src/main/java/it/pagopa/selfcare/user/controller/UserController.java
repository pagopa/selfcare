package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.service.UserEventService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Authenticated
@Path("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserEventService userEventService;

    @Operation(summary = "Retrieves products info and role which the user is enabled")
    @GET
    @Path("/{userId}/products")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getUserProductsInfo(@PathParam(value = "userId") String userId,
                                             @QueryParam(value = "institutionId") String institutionId,
                                             @QueryParam(value = "states") String[] states) {
        return userService.retrieveBindings(institutionId, userId, states)
                .map(userMapper::toUserProductsResponse)
                .map(response -> {
                    if (response.getBindings() == null || response.getBindings().isEmpty()) {
                        return Response.status(404).build();
                    }

                    return Response.status(200).entity(response).build();
                });
    }
}

