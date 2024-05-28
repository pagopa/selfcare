package it.pagopa.selfcare.user.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.service.UserPermissionService;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/authorize")
@Slf4j
@Authenticated
@AllArgsConstructor
public class UserPermissionController {
    private final UserPermissionService userPermissionService;
    private final UserUtils userUtils;

    /**
     * Retrieves the permission for a user in an institution.
     *
     * @param institutionId The ID of the institution.
     * @param productId     The ID of the product (optional).
     * @param permission    The permission to check.
     * @param ctx           The security context.
     * @return A Uni<Boolean> indicating whether the user has the specified
     *         permission.
     */
    @Operation(summary = "Get permission for a user in an institution")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Boolean> getPermission(
            @QueryParam("institutionId") String institutionId,
            @QueryParam("productId") String productId,
            @NotNull(message = "Permission type is required") @QueryParam("permission") PermissionTypeEnum permission,
            @Context SecurityContext ctx) {

        return userUtils.readUserIdFromToken(ctx)
                .onItem().transformToUni(loggedUser -> userPermissionService.hasPermission(institutionId, productId, permission, loggedUser.getUid()))
                .onItem().invoke(result -> log.info("User has permission: {}", result));
    }
}