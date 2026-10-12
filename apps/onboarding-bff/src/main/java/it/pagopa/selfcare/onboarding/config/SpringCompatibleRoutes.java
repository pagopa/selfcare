package it.pagopa.selfcare.onboarding.config;

import io.smallrye.health.SmallRyeHealthReporter;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import it.pagopa.selfcare.onboarding.exception.handler.ProblemResponses;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Public paths of the Spring BFF that consumers rely on: the container probes of the infrastructure
 * target {@code /actuator/health} and the OpenAPI contract is published as JSON at {@code /v3/api-docs}.
 * Health retains the Spring representation while taking its status from SmallRye.
 */
@ApplicationScoped
public class SpringCompatibleRoutes {

    static final String ACTUATOR_HEALTH_PATH = "/actuator/health";
    static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String ACTUATOR_JSON = "application/vnd.spring-boot.actuator.v3+json";

    @Inject
    SmallRyeHealthReporter healthReporter;

    void register(@Observes Router router) {
        router.route(ACTUATOR_HEALTH_PATH).method(HttpMethod.GET).method(HttpMethod.HEAD)
                .handler(context -> healthReporter.getHealthAsync().subscribe().with(
                        health -> json(context, health.isDown() ? 503 : 200, ACTUATOR_JSON,
                                new JsonObject().put("status", health.getStatus().name())),
                        context::fail));
        router.route(ACTUATOR_HEALTH_PATH + "/*").method(HttpMethod.GET).method(HttpMethod.HEAD)
                .handler(context -> context.response().setStatusCode(404).end());
        router.route("/actuator").method(HttpMethod.GET).method(HttpMethod.HEAD).handler(context -> {
            String base = context.request().absoluteURI().split("\\?", 2)[0];
            JsonObject links = new JsonObject()
                    .put("self", link(base, false))
                    .put("health", link(base + "/health", false))
                    .put("health-path", link(base + "/health/{*path}", true));
            json(context, 200, ACTUATOR_JSON, new JsonObject().put("_links", links));
        });
        router.route("/error").handler(context -> json(context, 500, "application/json",
                new JsonObject(ProblemResponses.servletError(999, "None", null))));
        router.get("/swagger-ui.html").handler(context -> context.response()
                .setStatusCode(302).putHeader("Location", "/swagger-ui/index.html").end());
        router.get(API_DOCS_PATH).handler(context -> context.reroute("/q/openapi?format=json"));
    }

    private static JsonObject link(String href, boolean templated) {
        return new JsonObject().put("href", href).put("templated", templated);
    }

    private static void json(RoutingContext context, int status, String contentType, JsonObject body) {
        context.response().setStatusCode(status).putHeader("Content-Type", contentType).end(body.encode());
    }
}
