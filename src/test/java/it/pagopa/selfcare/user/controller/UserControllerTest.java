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
     * Method under test: {@link UserController#getUsersEmailByInstitution(String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getUsersEmailByInstitution() {

        Mockito.when(userService.getUsersEmailByInstitution(anyString()))
                .thenReturn(Uni.createFrom().item(List.of("test@test.it")));

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/emails/{institutionId}")
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitution(String)}
     */
    @Test
    void getUsersEmailByInstitutionNotAuthorized() {

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/emails/{institutionId}")
                .then()
                .statusCode(401);
    }

}
