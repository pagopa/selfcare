package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.service.UserService;
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
public class UserControllerTest {

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

}
