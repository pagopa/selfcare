package it.pagopa.selfcare.auth.m2m;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.pagopa.selfcare.onboarding.config.JwtValidationConfig;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.ConfigProvider;

/** HTTP adapter around the branch's real validator; no Azure host or business side effects. */
final class FunctionReceiver implements AutoCloseable {
  private final ObjectMapper mapper = new ObjectMapper();
  private final List<Map<String, Object>> receipts = new CopyOnWriteArrayList<>();
  private final Map<String, PublicKey> publicKeys = new HashMap<>();
  private final AtomicReference<Throwable> failure = new AtomicReference<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final HttpServer server;
  private final HttpJwtValidator validator;
  private final ExecutionContext context = mock(ExecutionContext.class);

  FunctionReceiver() throws Exception {
    var config = ConfigProvider.getConfig();
    Map<String, JwtValidationConfig.TenantKey> tenantKeys = new HashMap<>();
    for (String tenant : List.of("AR", "PNPG")) {
      String pem = config.getValue("M2M_TEST_" + tenant + "_PUBLIC_KEY", String.class);
      tenantKeys.put(tenant, () -> Optional.of(pem));
      publicKeys.put(tenant, KeyFactory.getInstance("RSA").generatePublic(
          new X509EncodedKeySpec(Base64.getDecoder().decode(pem))));
    }
    JwtValidationConfig validation = mock(JwtValidationConfig.class);
    when(validation.machineTokensEnabled()).thenReturn(true);
    when(validation.tenantClaimRequired()).thenReturn(true);
    when(validation.allowedIssuers()).thenReturn(Set.of("SPID", "PAGOPA"));
    when(validation.tenants()).thenReturn(tenantKeys);
    validator = new HttpJwtValidator(validation, mapper);
    when(context.getLogger()).thenReturn(Logger.getLogger(FunctionReceiver.class.getName()));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(executor);
    server.createContext("/", this::handle);
    server.start();
  }

  URI uri() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  void assertHealthy() {
    if (failure.get() != null) {
      throw new AssertionError("Unexpected failure in the functions HTTP adapter", failure.get());
    }
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      try {
        if ("/_requests".equals(exchange.getRequestURI().getPath())) {
          if ("DELETE".equals(exchange.getRequestMethod())) {
            receipts.clear();
          }
          send(exchange, 200, mapper.writeValueAsString(receipts));
          return;
        }
        Map<String, String> headers = new HashMap<>();
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, values.get(0)));
        int status = "m2m-test-function-key".equals(exchange.getRequestHeaders().getFirst("x-functions-key"))
            ? validate(headers) : 401;
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("status", status);
        receipt.put("path", exchange.getRequestURI().getPath());
        if (status == 200) {
          String tenant = exchange.getRequestHeaders().getFirst("X-Tenant-Id");
          String token = exchange.getRequestHeaders().getFirst("Authorization").substring(7);
          Claims claims = Jwts.parser().setSigningKey(publicKeys.get(tenant)).parseClaimsJws(token).getBody();
          receipt.put("tenant", tenant);
          receipt.put("service", claims.getSubject());
          receipt.put("jti", claims.getId());
          receipt.put("lifetime", (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000);
        }
        receipts.add(receipt);
        exchange.getRequestBody().readAllBytes();
        send(exchange, status, "{}");
      } catch (Exception exception) {
        failure.compareAndSet(null, exception);
        Logger.getLogger(FunctionReceiver.class.getName()).log(Level.SEVERE, "HTTP adapter failed", exception);
        send(exchange, 500, "{}");
      }
    }
  }

  private int validate(Map<String, String> headers) {
    HttpRequestMessage<?> request = mock(HttpRequestMessage.class);
    when(request.getHeaders()).thenReturn(headers);
    when(request.createResponseBuilder(any(HttpStatus.class))).thenAnswer(invocation -> {
      HttpResponseMessage response = mock(HttpResponseMessage.class);
      when(response.getStatusCode()).thenReturn(invocation.<HttpStatus>getArgument(0).value());
      HttpResponseMessage.Builder builder = mock(HttpResponseMessage.Builder.class, RETURNS_SELF);
      when(builder.build()).thenReturn(response);
      return builder;
    });
    return validator.validate(request, context).map(HttpResponseMessage::getStatusCode).orElse(200);
  }

  private static void send(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @Override
  public void close() {
    server.stop(0);
    executor.shutdownNow();
    assertHealthy();
  }
}
