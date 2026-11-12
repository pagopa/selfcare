package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineDefinition;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineGrant;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import jakarta.ws.rs.ServiceUnavailableException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class MachineIamAuthorizerTest {
  private HttpServer server;
  private MachineIamAuthorizer authorizer;
  private MachineDefinition machine;
  private final MachineGrant grant = new MachineGrant("user-ms", "user.read", "prod-test");
  private final AtomicInteger status = new AtomicInteger(200);
  private final AtomicReference<String> body = new AtomicReference<>("{\"hasPermission\":true}");
  private final AtomicReference<String> request = new AtomicReference<>();
  private final AtomicReference<String> tenant = new AtomicReference<>();
  private final AtomicReference<String> authorization = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/iam/",
        exchange -> {
          request.set(exchange.getRequestURI().toString());
          tenant.set(exchange.getRequestHeaders().getFirst("X-Tenant-Id"));
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] payload = body.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status.get(), payload.length);
          exchange.getResponseBody().write(payload);
          exchange.close();
        });
    server.start();
    authorizer = new MachineIamAuthorizer();
    authorizer.mapper = new ObjectMapper();
    machine =
        new MachineDefinition(
            true,
            MachineTokenServiceTest.UID_ENV,
            "http://127.0.0.1:" + server.getAddress().getPort(),
            List.of("SUPPORT"),
            300,
            List.of(grant));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  @Test
  void checksExactTenantUidProductAndPermissionWithBootstrapIdentity() {
    authorizer.requirePermission(
        "PNPG", MachineTokenServiceTest.UID, machine, grant, "test-bootstrap");
    assertEquals(
        "/iam/users/"
            + MachineTokenServiceTest.UID
            + "/permissions/user.read?productId=prod-test&tenantId=PNPG",
        request.get());
    assertEquals("PNPG", tenant.get());
    assertEquals("Bearer test-bootstrap", authorization.get());
  }

  @Test
  void rejectsMissingUserAndDeniedPermission() {
    body.set("{\"hasPermission\":false}");
    assertThrows(
        ForbiddenException.class,
        () ->
            authorizer.requirePermission(
                "PNPG", MachineTokenServiceTest.UID, machine, grant, "test"));
    status.set(404);
    assertThrows(
        ForbiddenException.class,
        () ->
            authorizer.requirePermission(
                "PNPG", MachineTokenServiceTest.UID, machine, grant, "test"));
  }

  @Test
  void failsClosedForInvalidResponseOutageAndRedirect() {
    for (String invalid : List.of("{}", "{\"hasPermission\":\"true\"}", "{invalid", "null")) {
      body.set(invalid);
      assertThrows(
          ServiceUnavailableException.class,
          () ->
              authorizer.requirePermission(
                  "PNPG", MachineTokenServiceTest.UID, machine, grant, "test"));
    }
    for (int failure : List.of(302, 401, 500, 503)) {
      status.set(failure);
      assertThrows(
          ServiceUnavailableException.class,
          () ->
              authorizer.requirePermission(
                  "PNPG", MachineTokenServiceTest.UID, machine, grant, "test"));
    }
  }
}
