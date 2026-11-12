package it.pagopa.selfcare.auth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.Problem;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import it.pagopa.selfcare.auth.service.MachineTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.ws.rs.ServiceUnavailableException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MachineTokenListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenListener.class);
  @Inject MachineTokenConfig config;
  @Inject MachineTokenService service;
  @Inject Vertx vertx;
  @Inject ObjectMapper mapper;
  @Inject Validator validator;

  @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
  int mainPort;

  private HttpServer server;

  void start(@Observes StartupEvent event) throws Exception {
    if (!config.enabled()) {
      return;
    }
    if (config.port() < 1 || config.port() > 65535 || config.port() == mainPort) {
      throw new IllegalStateException("M2M requires a distinct internal port");
    }
    String cert =
        config
            .certificatePem()
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalStateException("M2M TLS certificate is required"));
    String key =
        config
            .privateKeyPem()
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalStateException("M2M TLS private key is required"));
    Router router = Router.router(vertx);
    router
        .route()
        .handler(
            context -> {
              context
                  .response()
                  .putHeader("Cache-Control", "no-store")
                  .putHeader("Pragma", "no-cache");
              context.next();
            });
    router
        .post("/token/service")
        .consumes("application/json")
        .handler(BodyHandler.create().setBodyLimit(8192))
        .blockingHandler(this::issue, false);
    router
        .post("/token/service")
        .handler(context -> problem(context, 415, "Content-Type must be application/json"));
    router.route().handler(context -> problem(context, 404, "Resource not found"));
    router
        .route()
        .failureHandler(
            context -> {
              int status = context.statusCode() > 0 ? context.statusCode() : 500;
              problem(
                  context, status, status == 500 ? "Internal server error" : "Request rejected");
            });
    server =
        vertx
            .createHttpServer(
                new HttpServerOptions()
                    .setHost("0.0.0.0")
                    .setPort(config.port())
                    .setSsl(true)
                    .setEnabledSecureTransportProtocols(Set.of("TLSv1.2", "TLSv1.3"))
                    .setPemKeyCertOptions(
                        new PemKeyCertOptions()
                            .setCertValue(Buffer.buffer(cert))
                            .setKeyValue(Buffer.buffer(key)))
                    .setIdleTimeout(10))
            .requestHandler(router);
    server.listen().toCompletionStage().toCompletableFuture().get(15, TimeUnit.SECONDS);
    LOGGER.info("event=machine_listener_started port={}", config.port());
  }

  private void issue(RoutingContext context) {
    try {
      MachineTokenRequest request =
          mapper
              .readerFor(MachineTokenRequest.class)
              .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              .readValue(context.body().asString());
      if (request == null || !validator.validate(request).isEmpty()) {
        throw new InvalidRequestException("Invalid M2M request");
      }
      context
          .response()
          .putHeader("Content-Type", "application/json")
          .end(mapper.writeValueAsString(service.issue(request)));
    } catch (JsonProcessingException
        | IllegalArgumentException
        | InvalidRequestException exception) {
      problem(context, 400, "Invalid M2M request");
    } catch (ForbiddenException exception) {
      problem(context, 403, exception.getMessage());
    } catch (ResourceNotFoundException exception) {
      problem(context, 404, "Resource not found");
    } catch (ServiceUnavailableException exception) {
      problem(context, 503, "IAM authorization is unavailable");
    } catch (Exception exception) {
      LOGGER.error(
          "event=machine_request_failed exception={}", exception.getClass().getSimpleName());
      problem(context, 500, "Internal server error");
    }
  }

  private void problem(RoutingContext context, int status, String detail) {
    LOGGER.warn("event=machine_request_rejected status={}", status);
    context
        .response()
        .setStatusCode(status)
        .putHeader("Content-Type", "application/problem+json")
        .end(
            io.vertx.core.json.Json.encode(
                new Problem(detail, null, status, "Request rejected", null)));
  }

  void stop(@Observes ShutdownEvent event) throws Exception {
    if (server != null) {
      server.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
  }
}
