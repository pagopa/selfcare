package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.service.UserServiceImpl;
import it.pagopa.selfcare.user.service.UserServiceTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
@QuarkusTest
@TestHTTPEndpoint(UserController.class)
@QuarkusTestResource(MongoTestResource.class)
class UserControllerTest {

    @InjectMock
    UserServiceImpl userService;

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserProductsInfoOk() {

        UserInstitution userInstitution = new UserInstitution();
        userInstitution.setUserId("test-user");

        Mockito.when(userService.retrieveBindings(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(List.of(userInstitution)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("userId", "test-user-id")
                .get("/{userId}/products")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetUserProductsInfoNotAuthorized() {

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("userId", "test-user-id")
                .get("/{userId}/products")
                .then()
                .statusCode(401);
    }
}
