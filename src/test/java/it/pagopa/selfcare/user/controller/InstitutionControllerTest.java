package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@TestHTTPEndpoint(InstitutionController.class)
class InstitutionControllerTest {

    @InjectMock
    private UserService userService;

    /**
     * Method under test: {@link InstitutionController#getInstitutionUsers(String)}
     */
    @Test
    void getInstitutionUsersNotAuthorized() {

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/users")
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#getInstitutionUsers(String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionUsers() {

        Mockito.when(userService.getUserProductsByInstitution(anyString()))
                .thenReturn(Multi.createFrom().item(new UserProductResponse()));

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/users")
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link InstitutionController#retrieveUsers(String, String, List, List, List, List))}
     */
    @Test
    void testGetUserInstitutionsNotAuthorized() {
        var institutionId = "institutionId";
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/user-institutions")
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#retrieveUsers(String, String, List, List, List, List))}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInstitutions() {
        var institutionId = "institutionId";
        Mockito.when(userService.findAllUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Multi.createFrom().items(new UserInstitutionResponse()));
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/user-institutions?products=1,2")
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, LocalDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt() {

        var institutionId = "institutionId";
        var productId = "productId";
        var now = LocalDateTime.now();
        Mockito.when(userService.updateUserProductCreatedAt(institutionId, List.of("userId"), productId, now))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/createdAt?userIds=userId&createdAt=" + now)
                .then()
                .statusCode(204);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, LocalDateTime)}
     */
    @Test
    void updateUserProductCreatedAt_NotAuthorized() {

        var institutionId = "institutionId";
        var productId = "productId";
        var now = LocalDateTime.now();
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/createdAt?createdAt=" + now)
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, LocalDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt_UserNotFound() {
        final LocalDateTime now = LocalDateTime.now();
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        Mockito.when(userService.updateUserProductCreatedAt(institutionId, List.of(userId), productId, now))
                .thenThrow(new ResourceNotFoundException("user non trovato"));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/createdAt?userIds=" + userId + "&createdAt=" + now)
                .then()
                .statusCode(404);
    }

    /**
     * Method under test: {@link InstitutionController#updateInstitutionDescription(String, UpdateDescriptionDto)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateInstitutionDescription() {

        var institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        updateDescriptionDto.setInstitutionDescription("description");

        Mockito.when(userService.updateInstitutionDescription(institutionId, updateDescriptionDto))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updateDescriptionDto)
                .pathParam("institutionId", institutionId)
                .put("/{institutionId}")
                .then()
                .statusCode(204);
    }

    @Test
    void updateInstitutionDescription_NotAuthorized() {

        var institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        updateDescriptionDto.setInstitutionDescription("description");

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updateDescriptionDto)
                .pathParam("institutionId", institutionId)
                .put("/{institutionId}")
                .then()
                .statusCode(401);
    }

}