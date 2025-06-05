package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.DeletedUserCountResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.controller.response.UsersCountResponse;
import it.pagopa.selfcare.user.controller.response.product.SearchUserDto;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
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
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt() {

        var institutionId = "institutionId";
        var productId = "productId";
        var now = OffsetDateTime.now();
        Mockito.when(userService.updateUserProductCreatedAt(institutionId, List.of("userId"), productId, now))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/created-at?userIds=userId&createdAt=" + now)
                .then()
                .statusCode(204);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    void updateUserProductCreatedAt_NotAuthorized() {

        var institutionId = "institutionId";
        var productId = "productId";
        var now = OffsetDateTime.now();
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/created-at?createdAt=" + now)
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt_UserNotFound() {
        final OffsetDateTime now = OffsetDateTime.now();
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
                .put("/{institutionId}/products/{productId}/created-at?userIds=" + userId + "&createdAt=" + now)
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

    @Test
    @TestSecurity(user = "userJwt")
    void getUsersCount() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);

        Mockito.when(userService.getUsersCount(institutionId, productId, roles, status))
                .thenReturn(Uni.createFrom().item(new UsersCountResponse(institutionId, productId, roles, status, 2L)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .queryParam("roles", List.of("MANAGER", "DELEGATE"))
                .queryParam("status", List.of("ACTIVE", "PENDING"))
                .get("/{institutionId}/products/{productId}/users/count")
                .then()
                .statusCode(200);
    }

    @Test
    void getUsersCount_NotAuthorized() {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final List<String> roles = List.of("role1", "role2");
        final List<String> status = List.of("status1", "status2");

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .queryParam("roles", roles)
                .queryParam("status", status)
                .get("/{institutionId}/products/{productId}/users/count")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteUserInstitutionProductUsers() {
        final String institutionId = "institutionId";
        final String productId = "productId";

        Mockito.when(userService.deleteUserInstitutionProductUsers(institutionId, productId))
                .thenReturn(Uni.createFrom().item(new DeletedUserCountResponse(institutionId, productId, 1L)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .delete("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(200);
    }

    @Test
    void deleteUserInstitutionProductUsers_NotAuthorized() {
        final String institutionId = "institutionId";
        final String productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .delete("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkUser() {
        final String fiscalCode = "fiscalCode";
        final String institutionId = "institutionId";
        final String productId = "productId";

        Mockito.when(userService.checkUser(fiscalCode, institutionId, productId))
                .thenReturn(Uni.createFrom().item(Boolean.TRUE));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .body(new SearchUserDto(fiscalCode))
                .post("/{institutionId}/product/{productId}/check-user")
                .then()
                .statusCode(200);
    }

    @Test
    void checkUser_NotAuthorized() {
        final String fiscalCode = "fiscalCode";
        final String institutionId = "institutionId";
        final String productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .body(new SearchUserDto(fiscalCode))
                .post("/{institutionId}/product/{productId}/check-user")
                .then()
                .statusCode(401);
    }

}