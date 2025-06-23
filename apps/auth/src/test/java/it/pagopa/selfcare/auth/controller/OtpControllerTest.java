package it.pagopa.selfcare.auth.controller;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import it.pagopa.selfcare.auth.controller.request.OtpVerifyRequest;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestHTTPEndpoint(OtpController.class)
class OtpControllerTest {

  @Test
  void verify_notImplemented() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uid");

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
  }
}
