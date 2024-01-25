package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.service.UserService;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

@Authenticated
@Path("/institutions")
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

    @Operation(summary = "The API retrieves users with optional filters in input as query params")
    @GET
    @Path(value = "/{institutionId}/user-institutions")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<UserInstitutionResponse> retrieveUsers(@PathParam(value = "institutionId") String institutionId,
                                                        @QueryParam(value = "userId") String userId,
                                                        @QueryParam(value = "roles") List<PartyRole> roles,
                                                        @QueryParam(value = "roles") List<OnboardedProductState> states,
                                                        @QueryParam(value = "products") List<String> products,
                                                        @QueryParam(value = "productRoles") List<String> productRoles) {
        return userService.findAllUserInstitutions(institutionId, userId, roles, states, products, productRoles);
    }
}
