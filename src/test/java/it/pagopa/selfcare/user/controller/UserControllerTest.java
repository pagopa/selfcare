package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.service.UserService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfLocalDate;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@TestHTTPEndpoint(UserController.class)
class UserControllerTest {

    @InjectMock
    private UserService userService;

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getUsersEmailByInstitution() {

        Mockito.when(userService.getUsersEmails(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(List.of("test@test.it")));

        var institutionId = "institutionId";
        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?institutionId=" + institutionId +"&productId=" + productId)
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getUsersEmailWithNullInstitutionId() {

        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?productId=" + productId)
                .then()
                .statusCode(400);
    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}
     */
    @Test
    void getUsersEmailByInstitutionNotAuthorized() {

        var institutionId = "institutionId";
        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?institutionId=" + institutionId + "&productId=" + productId)
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInfoOk() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setFiscalCode("test");
        userResource.setBirthDate(CertifiableFieldResourceOfLocalDate.builder().value(LocalDate.now()).build());
        userResource.setEmail(CertifiableFieldResourceOfstring.builder().value("test@test.com").build());
        userResource.setName(CertifiableFieldResourceOfstring.builder().value("testName").build());
        userResource.setFamilyName(CertifiableFieldResourceOfstring.builder().value("testFamilyName").build());

        Mockito.when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetUserInfoNotAuthorized() {
        Mockito.when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("test", "test")));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInfoFails() {
        Mockito.when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("test", "test")));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateUserStatus() {

        Mockito.when(userService.updateUserStatusWithOptionalFilter("userId", null, "prod-pagopa", null, null, OnboardedProductState.ACTIVE))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", "userId")
                .queryParam("productId", "prod-pagopa")
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put("/{id}/status")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateUserStatusError() {

        Mockito.when(userService.updateUserStatusWithOptionalFilter("userId", null, "prod-pagopa", null, null, OnboardedProductState.ACTIVE))
                .thenThrow(new ResourceNotFoundException("user non trovato"));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", "userId")
                .queryParam("productId", "prod-pagopa")
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put("/{id}/status")
                .then()
                .statusCode(404);
    }

    /**
     * Method under test:
     * {@link UserController#deleteProducts(String, String, String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void deleteDeleteProductsErrorTest() {
        String PATH_USER_ID = "userId";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_DELETE_PRODUCT = "{userId}/institutions/{institutionId}/products/{productId}";

        var user = "user1";
        var institution = "institution1";
        var product = "product1";
        Mockito.when(userService.deleteUserInstitutionProduct("user1","institution1", "product1"))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .delete(PATH_DELETE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Method under test:
     * {@link UserController#deleteProducts(String, String, String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void deleteDeleteProductsOKTest() {

        String PATH_USER_ID = "userId";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_DELETE_PRODUCT = "{userId}/institutions/{institutionId}/products/{productId}";

        var user = "user123";
        var institution = "institution123";
        var product = "prod-pagopa";

        Mockito.when(userService.deleteUserInstitutionProduct("user123", "institution123", "prod-pagopa"))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .delete(PATH_DELETE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

}
