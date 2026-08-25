package it.pagopa.selfcare.auth.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.controller.request.OtpResendRequest;
import it.pagopa.selfcare.auth.controller.request.OtpVerifyRequest;
import it.pagopa.selfcare.auth.controller.response.OtpForbiddenCode;
import it.pagopa.selfcare.auth.controller.response.OtpMailInfoResponse;
import it.pagopa.selfcare.auth.entity.OtpFlow;
import it.pagopa.selfcare.auth.exception.ConflictException;
import it.pagopa.selfcare.auth.exception.InternalException;
import it.pagopa.selfcare.auth.exception.OtpForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.model.OtpStatus;
import it.pagopa.selfcare.auth.service.OtpFlowService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(OtpController.class)
class OtpControllerTest {

  @InjectMock private OtpFlowService otpFlowService;

  @BeforeEach
  void setUpTenantHeader() {
    RestAssured.requestSpecification =
        new RequestSpecBuilder().addHeader("X-Tenant-Id", "AR").build();
  }

  @AfterEach
  void resetRequestSpecification() {
    RestAssured.requestSpecification = null;
  }

  @Test
  void verifyOtp_BadRequest() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void verifyOtp_NotFound() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uuid");
    when(otpFlowService.verifyOtp(anyString(), anyString()))
        .thenReturn(
            Uni.createFrom().failure(new ResourceNotFoundException("Cannot find Otp Flow")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  void verifyOtp_ForbiddenWrongCode() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uuid");
    OtpForbiddenException wrongCodeException =
        new OtpForbiddenException(
            "Wrong Otp Code", OtpForbiddenCode.CODE_001, 1, OtpStatus.PENDING);
    when(otpFlowService.verifyOtp(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(wrongCodeException));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_FORBIDDEN)
        .contentType("application/problem+json")
        .body("title", equalTo("Forbidden"))
        .body("detail", equalTo("OTP verification failed"))
        .body("otpForbiddenCode", equalTo(wrongCodeException.getCode().name()))
        .body("remainingAttempts", equalTo(wrongCodeException.getRemainingAttempts()));
  }

  @Test
  void verifyOtp_ForbiddenMaxAttempts() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uuid");
    OtpForbiddenException wrongCodeException =
        new OtpForbiddenException(
            "Max Attempts reached", OtpForbiddenCode.CODE_002, 0, OtpStatus.REJECTED);
    when(otpFlowService.verifyOtp(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(wrongCodeException));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_FORBIDDEN)
        .contentType("application/problem+json")
        .body("title", equalTo("Forbidden"))
        .body("detail", equalTo("OTP verification failed"))
        .body("otpForbiddenCode", equalTo(wrongCodeException.getCode().name()))
        .body("remainingAttempts", equalTo(wrongCodeException.getRemainingAttempts()));
  }

  @Test
  void verifyOtp_Conflict() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uuid");
    when(otpFlowService.verifyOtp(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new ConflictException("Conflict")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_CONFLICT);
  }

  @Test
  void verifyOtp_InternalServerError() {
    OtpVerifyRequest request = new OtpVerifyRequest();
    request.setOtp("123456");
    request.setOtpUuid("uuid");
    when(otpFlowService.verifyOtp(anyString(), anyString()))
        .thenReturn(Uni.createFrom().failure(new InternalException("Internal server error")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/verify")
        .then()
        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void resendOtp_BadRequest() {
    OtpResendRequest request = new OtpResendRequest();

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/resend")
        .then()
        .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void resendOtp_NotFound() {
    OtpResendRequest request = new OtpResendRequest();
    request.setOtpUuid("uuid");
    when(otpFlowService.resendOtp(anyString()))
        .thenReturn(
            Uni.createFrom().failure(new ResourceNotFoundException("Cannot find Otp Flow")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/resend")
        .then()
        .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  void resendOtp_Conflict() {
    OtpResendRequest request = new OtpResendRequest();
    request.setOtpUuid("uuid");
    when(otpFlowService.resendOtp(anyString()))
        .thenReturn(Uni.createFrom().failure(new ConflictException("Conflict")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/resend")
        .then()
        .statusCode(HttpStatus.SC_CONFLICT);
  }

  @Test
  void resendOtp_InternalServerError() {
    OtpResendRequest request = new OtpResendRequest();
    request.setOtpUuid("uuid");
    when(otpFlowService.resendOtp(anyString()))
        .thenReturn(Uni.createFrom().failure(new InternalException("Internal server error")));

    given()
        .body(request)
        .when()
        .contentType(ContentType.JSON)
        .post("/resend")
        .then()
        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void getOtpMailInfo_NotFound() {
    when(otpFlowService.getOtpMailInfo(anyString()))
      .thenReturn(
        Uni.createFrom()
          .failure(new ResourceNotFoundException("Cannot find mail info")));

    given()
      .when()
      .get("/mail-info/requestId")
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  void getOtpMailInfo_InternalServerError() {
    when(otpFlowService.getOtpMailInfo(anyString()))
      .thenReturn(
        Uni.createFrom()
          .failure(new InternalException("Internal server error")));

    given()
      .when()
      .get("/mail-info/requestId")
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void getOtpMailInfo_Ok() {
    OtpMailInfoResponse response =
      new OtpMailInfoResponse(
        "email-id",
        "DELIVERED",
        "test@test.com",
        1,
        java.util.List.of(
          new OtpMailInfoResponse.MailStatusHistory(
            "DELIVERED",
            java.time.OffsetDateTime.now())));

    when(otpFlowService.getOtpMailInfo(anyString()))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .when()
      .get("/mail-info/requestId")
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("mailRequestId", equalTo("email-id"))
      .body("status", equalTo("DELIVERED"))
      .body("recipient", equalTo("test@test.com"))
      .body("attempts", equalTo(1))
      .body("history.size()", equalTo(1))
      .body("history[0].status", equalTo("DELIVERED"));
  }

  @Test
  void getOtpInfo_BadRequest() {
    given()
      .when()
      .get("/info")
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void getOtpInfo_NotFound() {
    when(otpFlowService.getOtpInfo(anyString(), any()))
      .thenReturn(
        Uni.createFrom()
          .failure(new ResourceNotFoundException("Cannot find otp info")));

    given()
      .queryParam("userId", "userId")
      .when()
      .get("/info")
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  void getOtpInfo_InternalServerError() {
    when(otpFlowService.getOtpInfo(anyString(), any()))
      .thenReturn(
        Uni.createFrom()
          .failure(new InternalException("Internal server error")));

    given()
      .queryParam("userId", "userId")
      .when()
      .get("/info")
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  void getOtpInfo_Ok() {
    List<OtpFlow> response = List.of(
      new OtpFlow(
        "uuid1",
        "userId",
        "AR",
        "otp-1",
        OtpStatus.PENDING,
        1,
        OffsetDateTime.now().minusHours(1),
        OffsetDateTime.now().minusMinutes(30),
        OffsetDateTime.now().plusMinutes(5),
        "requestId1"),
      new OtpFlow(
        "uuid2",
        "userId",
        "AR",
        "otp-2",
        OtpStatus.COMPLETED,
        2,
        OffsetDateTime.now().minusHours(1),
        OffsetDateTime.now().minusMinutes(30),
        OffsetDateTime.now().plusMinutes(5),
        "requestId2"));

    when(otpFlowService.getOtpInfo(anyString(), any()))
      .thenReturn(Uni.createFrom().item(response));

    given()
      .queryParam("userId", "userId")
      .when()
      .get("/info")
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("size()", equalTo(2))

      .body("[0].uuid", equalTo("uuid1"))
      .body("[0].userId", equalTo("userId"))
      .body("[0].otp", equalTo("otp-1"))
      .body("[0].status", equalTo("PENDING"))
      .body("[0].attempts", equalTo(1))
      .body("[0].mailRequestId", equalTo("requestId1"))

      .body("[1].uuid", equalTo("uuid2"))
      .body("[1].userId", equalTo("userId"))
      .body("[1].otp", equalTo("otp-2"))
      .body("[1].status", equalTo("COMPLETED"))
      .body("[1].attempts", equalTo(2))
      .body("[1].mailRequestId", equalTo("requestId2"));
  }

  @Test
  void getOtpInfo_WithStatusFilter() {
    when(otpFlowService.getOtpInfo(anyString(), org.mockito.ArgumentMatchers.eq(OtpStatus.PENDING)))
      .thenReturn(Uni.createFrom().item(List.of()));

    given()
      .queryParam("userId", "userId")
      .queryParam("status", "PENDING")
      .when()
      .get("/info")
      .then()
      .statusCode(HttpStatus.SC_OK);
  }
}
