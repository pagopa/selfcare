package it.pagopa.selfcare.webhookconsumer.controller;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class NotificationControllerTest {

  private static final String PAYLOAD =
      "{\"tenantId\":\"AR\",\"productId\":\"prod-io\",\"topic\":\"SC-Contracts\",\"payload\":{\"id\":\"1\"}}";
  private static final String AUTHORIZATION = "Bearer test-token";

  private final Logger logger = Logger.getLogger(NotificationController.class.getName());
  private final List<String> messages = new CopyOnWriteArrayList<>();
  private final Handler handler =
      new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
          messages.add(
              logRecord instanceof ExtLogRecord extLogRecord
                  ? extLogRecord.getFormattedMessage()
                  : logRecord.getMessage());
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
      };

  @BeforeEach
  void addHandler() {
    logger.addHandler(handler);
  }

  @AfterEach
  void removeHandler() {
    logger.removeHandler(handler);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void shouldLogPayloadAndAuthorizationOnly(String method) {
    given()
        .contentType("application/json")
        .header("Authorization", AUTHORIZATION)
        .header("X-Tenant-Id", "AR")
        .body(PAYLOAD)
        .when()
        .request(method, "/notifications")
        .then()
        .statusCode(204);

    assertEquals(1, messages.size());
    String message = messages.get(0);
    assertTrue(message.contains("method: " + method));
    assertTrue(message.contains("Authorization: " + AUTHORIZATION));
    assertTrue(message.contains("payload: " + PAYLOAD));
    assertFalse(message.contains("X-Tenant-Id"));
  }

  @Test
  void shouldEscapeLineBreaksInLoggedValues() {
    given()
        .contentType("text/plain")
        .body("line1\r\nline2")
        .when()
        .post("/notifications")
        .then()
        .statusCode(204);

    assertEquals(1, messages.size());
    assertTrue(messages.get(0).contains("Authorization: null, payload: line1\\r\\nline2"));
  }
}
