package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(EventsController.class)
class EventsControllerTest {
    @InjectMock
    private UserService userService;

    @Test
    @TestSecurity(user = "userJwt")
    void sendOldUsers(){
        final String institutionId = "institutionId";
        final String userId = "userId";
        final OffsetDateTime fromDate = OffsetDateTime.now();

        when(userService.sendEventsByDateAndUserIdAndInstitutionId(any(), anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());
        given()
                .when()
                .contentType(ContentType.JSON)
                .post("/sc-users?institutionId="+institutionId+"&userId="+userId+"&fromDate="+fromDate)
                .then()
                .statusCode(204);
    }

}