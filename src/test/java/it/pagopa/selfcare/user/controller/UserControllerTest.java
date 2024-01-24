package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
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

}
