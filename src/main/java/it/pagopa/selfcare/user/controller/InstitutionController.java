package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Authenticated
@Path("/v1/institutions")
@AllArgsConstructor
public class InstitutionController {

    private final UserService userService;

    @Operation(summary = "The API retrieves user's info including details of roles on products")
    @GET
    @Path(value = "/{institutionId}/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<UserProductResponse> getInstitutionUsers(@PathParam(value = "institutionId") String institutionId) {
        return userService.getUserProductsByInstitution(institutionId);
    }
}
