package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

}