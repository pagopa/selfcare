package it.pagopa.selfcare.auth.m2m;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.exception.MachineAuthenticationException;
import it.pagopa.selfcare.auth.service.MachineIdentityVerifier;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
@TestProfile(MachineTokenFlowProfile.class)
class MachineTokenFlowIT {
  @InjectMock MachineIdentityVerifier entra;
  @Inject TenantSessionKeyProvider signingKeys;
  @TestHTTPResource URI auth;
  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach
  void configureEntraBoundary() {
    when(entra.verify(any())).thenThrow(new MachineAuthenticationException());
    MachineTokenFlowProfile.IDENTITIES.forEach((service, oid) ->
        doReturn(oid).when(entra).verify("Bearer test-identity-" + service));
    doReturn("44444444-4444-4444-8444-444444444444")
        .when(entra).verify("Bearer unknown-principal");
  }

  @ParameterizedTest
  @ValueSource(strings = {"onboarding-ms", "onboarding-cdc"})
  void realCallerAcquiresCachesRenewsAndFailsClosed(String service) throws Exception {
    try (var receiver = new FunctionReceiver()) {
      assertEquals(200, http.send(HttpRequest.newBuilder(receiver.uri().resolve("/_requests"))
          .GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
      Path workspace = Path.of(System.getProperty("m2m.workspace")).toRealPath();
      Path log = workspace.resolve("apps/auth/target/m2m-" + service + ".log");
      var builder = new ProcessBuilder(
          Path.of(System.getProperty("maven.home"), "bin", "mvn").toString(),
          "-B", "-f", workspace.resolve("apps/" + service + "/pom.xml").toString(),
          "-Pm2m-integration", "test", "-Dtest=MachineTokenCallerIT",
          "-Dmaven.repo.local=" + System.getProperty("m2m.repository"));
      builder.directory(workspace.toFile());
      builder.redirectErrorStream(true).redirectOutput(log.toFile());
      builder.environment().putAll(Map.of(
          "M2M_TEST_AUTH_URL", auth.toString().replaceAll("/$", ""),
          "M2M_TEST_FUNCTIONS_URL", receiver.uri().toString(),
          "M2M_TEST_IDENTITY", "test-identity-" + service,
          "M2M_TEST_SERVICE", service,
          "M2M_TEST_PUBLIC_KEY", ConfigProvider.getConfig().getValue("M2M_TEST_AR_PUBLIC_KEY", String.class)));
      Process process = builder.start();
      try {
        assertTrue(process.waitFor(5, TimeUnit.MINUTES), "Caller build timed out; see " + log);
        String output = Files.readString(log);
        assertEquals(0, process.exitValue(), () -> output.substring(Math.max(0, output.length() - 16000)));
        assertTrue(output.contains("Tests run: 2, Failures: 0, Errors: 0, Skipped: 0"),
            "Both caller contract tests must execute; see " + log);
      } finally {
        if (process.isAlive()) {
          var descendants = process.descendants().toList();
          descendants.forEach(ProcessHandle::destroy);
          process.destroy();
          process.waitFor(10, TimeUnit.SECONDS);
          descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
          if (process.isAlive()) {
            process.destroyForcibly();
          }
          if (!process.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Cannot stop caller process " + process.pid());
          }
        }
      }
      verify(entra, times(6)).verify("Bearer test-identity-" + service);
      verify(entra).verify("Bearer invalid-identity");
    }
  }

  @Test
  void issuerRejectsInvalidCredentialsAndUnauthorizedGrants() {
    for (String authorization : List.of("Bearer invalid-identity", "Bearer unknown-principal")) {
      given().header("Authorization", authorization).contentType("application/json")
          .body(request("AR", "onboarding-functions", "onboarding-functions.invoke"))
          .post("/token/service").then()
          .statusCode(authorization.endsWith("unknown-principal") ? 403 : 401);
    }
    for (Map<String, String> denied : List.of(
        request("AR", "user-ms", "onboarding-functions.invoke"),
        request("AR", "onboarding-functions", "admin"))) {
      given().header("Authorization", "Bearer test-identity-onboarding-ms")
          .contentType("application/json").body(denied)
          .post("/token/service").then().statusCode(403);
    }
  }

  @Test
  void receiverRejectsExpiredAlteredAndWronglyScopedTokensFromTheIssuer() throws Exception {
    try (var receiver = new FunctionReceiver()) {
      String token = given().header("Authorization", "Bearer test-identity-onboarding-ms")
          .contentType("application/json")
          .body(request("AR", "onboarding-functions", "onboarding-functions.invoke"))
          .post("/token/service").then().statusCode(200)
          .header("Cache-Control", org.hamcrest.Matchers.equalTo("no-store"))
          .extract().path("access_token");
      assertEquals(200, invoke(receiver, token, "AR"));
      assertEquals(401, invoke(receiver, token, "PNPG"));
      String[] parts = token.split("\\.");
      String damagedSignature = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
      assertEquals(401, invoke(receiver, parts[0] + "." + parts[1] + "." + damagedSignature, "AR"));

      // Re-sign modified claims with the fixture's real tenant key: rejection must not rely on a bad signature.
      var publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(
          Base64.getDecoder().decode(ConfigProvider.getConfig().getValue("M2M_TEST_AR_PUBLIC_KEY", String.class))));
      Claims claims = Jwts.parser().setSigningKey(publicKey)
          .parseClaimsJws(token).getBody();
      for (var invalid : Map.of("aud", "user-ms", "scope", "admin", "tenant_id", "PNPG").entrySet()) {
        Claims changed = Jwts.claims(claims);
        changed.put(invalid.getKey(), invalid.getValue());
        assertEquals(401, invoke(receiver, sign(changed), "AR"));
      }
      Claims expired = Jwts.claims(claims);
      expired.setIssuedAt(Date.from(Instant.now().minusSeconds(360)));
      expired.setExpiration(Date.from(Instant.now().minusSeconds(60)));
      assertEquals(401, invoke(receiver, sign(expired), "AR"));
    }
  }

  private String sign(Claims claims) {
    return Jwts.builder().setClaims(claims)
        .signWith(SignatureAlgorithm.RS256, signingKeys.getSigningKey("AR").privateKey()).compact();
  }

  private int invoke(FunctionReceiver receiver, String token, String tenant) throws Exception {
    return http.send(HttpRequest.newBuilder(receiver.uri().resolve("/api/notification"))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", "Bearer " + token)
        .header("X-Tenant-Id", tenant)
        .header("x-functions-key", "m2m-test-function-key")
        .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
        HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  private Map<String, String> request(String tenant, String audience, String scope) {
    return Map.of("tenantId", tenant, "audience", audience, "scope", scope);
  }
}
