package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.PermissionTypeEnum;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.service.UserPermissionService;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static it.pagopa.selfcare.user.constant.PermissionTypeEnum.ADMIN;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestHTTPEndpoint(UserPermissionController.class)
class UserPermissionControllerTest {
    @InjectMock
    UserPermissionService userPermissionService;

    @InjectMock
    UserUtils userUtils;

    private final String institutionIdField = "institutionId";
    private final String productIdField = "productId";
    private final String institutionId = UUID.randomUUID().toString();
    private final  String productId = "prod-io";
    private final String userId = UUID.randomUUID().toString();

    @Test
    void testGetPermissionUnhauthorize() {
        // Mock input parameters

        // Perform the API call without providing the permission query parameter
        given()
                .pathParam(institutionIdField, institutionId)
                .queryParam(productIdField, productId)
                .when()
                .get("/{institutionId}")
                .then()
                .statusCode(401);

        // Verify that the methods were not called
        verifyNoInteractions(userPermissionService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetPermission() {
        // Mock input parameters
        PermissionTypeEnum permission = ADMIN;

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getUserPrincipal()).thenReturn(() -> "userJwt");

        // Mock userUtils.readUserIdFromToken() method
        when(userUtils.readUserIdFromToken(any())).thenReturn(Uni.createFrom().item(LoggedUser.builder().uid(userId).build()));

        // Mock userPermissionService.hasPermission() method
        when(userPermissionService.hasPermission(institutionId, productId, permission,userId)).thenReturn(Uni.createFrom().item(Boolean.TRUE));

        // Perform the API call
        given()
                .pathParam(institutionIdField, institutionId)
                .queryParam(productIdField, productId)
                .queryParam("permission", permission)
                .when()
                .get("/{institutionId}")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON);

        // Verify that the methods were called with the expected parameters
        verify(userPermissionService).hasPermission(institutionId, productId, permission, userId);
    }
}