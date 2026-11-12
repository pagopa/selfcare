package it.pagopa.selfcare.auth.service;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.model.UserClaims;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServiceUnavailableException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import org.eclipse.microprofile.config.Config;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(MachineTokenTenantTest.PnpgProfile.class)
class MachineTokenTenantTest {
  @InjectMock MachineIamAuthorizer iam;
  @Inject TenantRegistry tenants;
  @Inject SessionService sessions;
  @Inject Config config;
  @Inject ObjectMapper mapper;
  private HttpClient client;
  private URI endpoint;

  @BeforeEach
  void setUp() throws Exception {
    var certificate =
        CertificateFactory.getInstance("X.509")
            .generateCertificate(
                new ByteArrayInputStream(
                    config
                        .getValue("auth.m2m.certificate-pem", String.class)
                        .getBytes(StandardCharsets.US_ASCII)));
    KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
    trust.load(null, null);
    trust.setCertificateEntry("internal", certificate);
    TrustManagerFactory factory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(trust);
    SSLContext tls = SSLContext.getInstance("TLS");
    tls.init(null, factory.getTrustManagers(), null);
    client = HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(3)).build();
    endpoint =
        URI.create(
            "https://localhost:"
                + config.getValue("auth.m2m.port", Integer.class)
                + "/token/service");
  }

  @Test
  void issuesPnpgTokenOverPrivateTlsWithoutCallerCredentialsOrLogin() throws Exception {
    var response =
        post(
            """
        {"tenantId":"PNPG","audience":"user-ms","scope":"user.read"}
        """);
    assertEquals(200, response.statusCode());
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
    String token = mapper.readTree(response.body()).path("access_token").asText();
    var publicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(
                new X509EncodedKeySpec(
                    Base64.getDecoder()
                        .decode(config.getValue("TEST_PNPG_PUBLIC_KEY", String.class))));
    var claims =
        new JwtConsumerBuilder()
            .setVerificationKey(publicKey)
            .setExpectedIssuer("PAGOPA")
            .setExpectedAudience("api-pnpg.example.test")
            .setRequireExpirationTime()
            .build()
            .processToClaims(token);
    assertEquals(MachineTokenServiceTest.UID, claims.getStringClaimValue("uid"));
    assertEquals("PNPG", claims.getStringClaimValue("tenant_id"));
    assertEquals(List.of("SUPPORT"), claims.getStringListClaimValue("groups"));
    assertEquals(300, claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue());
    verify(iam).requirePermission(eq("PNPG"), any(), any(), any());
    assertThrows(ForbiddenException.class, () -> tenants.resolveEnabledTenant("PNPG"));
    UserClaims user = UserClaims.builder().uid("user").tenantId("PNPG").build();
    assertThrows(ForbiddenException.class, () -> sessions.generateSessionToken(user));
    assertThrows(ForbiddenException.class, () -> sessions.generateSessionTokenInternal(user));
  }

  @Test
  void tokenRouteIsAbsentFromMainPortAndLoginIsAbsentFromInternalPort() throws Exception {
    given()
        .contentType("application/json")
        .body(
            """
            {"tenantId":"PNPG","audience":"user-ms","scope":"user.read"}
            """)
        .post("/token/service")
        .then()
        .statusCode(404);
    var response =
        client.send(
            HttpRequest.newBuilder(endpoint.resolve("/oidc/exchange"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(404, response.statusCode());
    verifyNoInteractions(iam);
  }

  @Test
  void rejectsUntrustedTlsAndPlainHttp() {
    assertThrows(
        java.io.IOException.class,
        () ->
            HttpClient.newHttpClient()
                .send(
                    HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()));
    assertThrows(
        java.io.IOException.class,
        () ->
            client.send(
                HttpRequest.newBuilder(URI.create(endpoint.toString().replace("https:", "http:")))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()));
  }

  @Test
  void rejectsImpersonationMalformedRequestsAndUnconfiguredTenants() throws Exception {
    assertEquals(
        400,
        post(
            """
        {"tenantId":"PNPG","audience":"user-ms","scope":"user.read","uid":"someone-else"}
        """)
            .statusCode());
    assertEquals(400, post("{}").statusCode());
    assertEquals(400, post("{invalid").statusCode());
    assertEquals(
        415,
        client
            .send(
                HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString())
            .statusCode());
    assertEquals(413, post(" ".repeat(9000)).statusCode());
    assertEquals(
        403,
        post(
            """
        {"tenantId":"AR","audience":"user-ms","scope":"user.read"}
        """)
            .statusCode());
    verifyNoInteractions(iam);
  }

  @Test
  void refusesTokenWhenIamDeniesOrFails() throws Exception {
    String request =
        """
        {"tenantId":"PNPG","audience":"user-ms","scope":"user.read"}
        """;
    doThrow(new ForbiddenException("Denied"))
        .when(iam)
        .requirePermission(any(), any(), any(), any());
    var denied = post(request);
    assertEquals(403, denied.statusCode());
    assertFalse(denied.body().contains("access_token"));
    doThrow(new ServiceUnavailableException())
        .when(iam)
        .requirePermission(any(), any(), any(), any());
    assertEquals(503, post(request).statusCode());
  }

  private HttpResponse<String> post(String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(5))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  public static class PnpgProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      try {
        Map<String, String> values = tlsConfiguration();
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application-test.properties")) {
          properties.load(input);
        }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode registry =
            (ObjectNode) mapper.readTree(properties.getProperty("tenant.registry.json"));
        registry.set(
            "PNPG",
            mapper.readTree(
                """
            {"auth_enabled":false,
             "jwt":{"session":{"privateKeyEnvVar":"TEST_PNPG_PRIVATE_KEY",
               "keyIdEnvVar":"TEST_PNPG_KEY_ID","audience":"api-pnpg.example.test"}},
             "m2m":{"enabled":true,"uid":"22222222-2222-4222-8222-222222222222",
               "iamBaseUrl":"https://iam.example.test","groups":["SUPPORT"],
               "grants":[{"audience":"user-ms","scope":"user.read","productId":"prod-test"}]}}
            """));
        var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        values.put("tenant.registry.json", mapper.writeValueAsString(registry));
        values.put("tenant.supported-tenants", "AR");
        values.put("jwt.session.tenant-audience-enabled", "true");
        values.put("TEST_PNPG_PRIVATE_KEY", pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        values.put("TEST_PNPG_KEY_ID", "pnpg-kid");
        values.put(
            "TEST_PNPG_PUBLIC_KEY",
            Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()));
        values.put("auth.m2m.enabled", "true");
        return values;
      } catch (Exception exception) {
        throw new IllegalStateException("Cannot configure internal token test", exception);
      }
    }

    private Map<String, String> tlsConfiguration() throws Exception {
      Path directory = Files.createTempDirectory("auth-m2m-tls-");
      Path store = directory.resolve("test.p12");
      try {
        Process generator =
            new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                    "-genkeypair",
                    "-alias",
                    "tls",
                    "-keyalg",
                    "RSA",
                    "-keysize",
                    "2048",
                    "-storetype",
                    "PKCS12",
                    "-keystore",
                    store.toString(),
                    "-storepass",
                    "test-password",
                    "-dname",
                    "CN=localhost",
                    "-ext",
                    "SAN=dns:localhost,ip:127.0.0.1",
                    "-validity",
                    "2",
                    "-noprompt")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (!generator.waitFor(15, TimeUnit.SECONDS)) {
          generator.destroyForcibly();
          throw new IllegalStateException("Test certificate generation timed out");
        }
        if (generator.exitValue() != 0) {
          throw new IllegalStateException("Test certificate generation failed");
        }
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(store)) {
          keyStore.load(input, "test-password".toCharArray());
        }
        Map<String, String> values = new HashMap<>();
        values.put(
            "auth.m2m.certificate-pem",
            pem("CERTIFICATE", keyStore.getCertificate("tls").getEncoded()));
        values.put(
            "auth.m2m.private-key-pem",
            pem("PRIVATE KEY", keyStore.getKey("tls", "test-password".toCharArray()).getEncoded()));
        try (ServerSocket socket = new ServerSocket(0)) {
          values.put("auth.m2m.port", Integer.toString(socket.getLocalPort()));
        }
        return values;
      } finally {
        Files.deleteIfExists(store);
        Files.deleteIfExists(directory);
      }
    }

    private static String pem(String type, byte[] bytes) {
      return "-----BEGIN "
          + type
          + "-----\n"
          + Base64.getEncoder().encodeToString(bytes)
          + "\n-----END "
          + type
          + "-----";
    }
  }
}
