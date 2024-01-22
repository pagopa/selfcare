package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.service.UserEventService;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Path(value = "/emails/{institutionId}/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<String>> getUsersEmailByInstitutionAndProduct(@PathParam(value = "institutionId") String institutionId,
                                                                  @PathParam(value = "productId") String productId) {
        return userService.getUsersEmails(institutionId, productId);
    }
}

