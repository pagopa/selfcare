package it.pagopa.selfcare.onboarding.parity;

import static it.pagopa.selfcare.onboarding.parity.DownstreamStub.MS_PRODUCT;

import it.pagopa.selfcare.onboarding.parity.DownstreamStub.Reply;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything a client can observe besides the happy path of an operation, as served by the
 * unchanged Spring reference: protection response headers, the public paths and their exact
 * answers, routing of paths that do not exist (Spring 6 never matches a trailing slash), CORS
 * handling, content negotiation and what the BFF does (not) forward downstream.
 */
final class HttpContractScenarios {

  private static final String G = "http-contract";
  private static final String PRODUCTS = "/v1/products";

  private HttpContractScenarios() {}

  static List<Scenario> all() {
    List<Scenario> s = new ArrayList<>();
    protectionHeaders(s);
    routing(s);
    publicPaths(s);
    cors(s);
    negotiation(s);
    forwarding(s);
    return s;
  }

  private static void productsOk(DownstreamStub stub) {
    stub.on(MS_PRODUCT, "GET", "/product", Reply.json(200, "[]"));
  }

  /** Cache-Control / Pragma / Expires / nosniff / frame / xss headers Spring Security adds to every protected response. */
  private static void hardened(Check c) {
    c.header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
        .header("Pragma", "no-cache")
        .header("Expires", "0")
        .header("X-Content-Type-Options", "nosniff")
        .header("X-Frame-Options", "DENY")
        .header("X-XSS-Protection", "0");
  }

  private static void protectionHeaders(List<Scenario> s) {
    s.add(
        Scenario.api(G, "protection-headers-on-success", PRODUCTS)
            .stub(HttpContractScenarios::productsOk)
            .expect(c -> hardened(c.status(200))));
    s.add(
        Scenario.api(G, "protection-headers-on-validation-error", "POST", "/v1/users/search-user")
            .json("{}")
            .expect(c -> hardened(c.status(400))));
    s.add(
        Scenario.api(G, "protection-headers-on-method-not-allowed", "DELETE", PRODUCTS)
            .expect(c -> hardened(c.status(405))));
    s.add(
        Scenario.api(G, "protection-headers-on-downstream-error", PRODUCTS)
            .stub(d -> d.on(MS_PRODUCT, "GET", "/product", Reply.status(503)))
            .expect(c -> hardened(c.status(502))));
    s.add(
        Scenario.api(G, "protection-headers-on-unknown-path", "/v1/unknown")
            .expect(c -> hardened(c.status(400))));
    s.add(
        Scenario.of(G, "protection-headers-on-unauthorized", "GET", PRODUCTS)
            .header("X-Tenant-Id", "PNPG")
            .expect(c -> hardened(c.status(401))));
    s.add(
        Scenario.api(G, "protection-headers-on-tenant-conflict", PRODUCTS)
            .setHeader("X-Tenant-Id", "AR")
            .expect(c -> hardened(c.status(400))));
    s.add(
        Scenario.api(G, "vary-header-on-success", PRODUCTS)
            .stub(HttpContractScenarios::productsOk)
            .expect(
                c ->
                    c.status(200).headerContains("Vary", "Origin")));
  }

  private static void routing(List<Scenario> s) {
    for (String path : new String[] {"/v1/unknown", "/nope", "/v2/tokens", "/v1/products.json"}) {
      s.add(
          Scenario.api(G, "unknown-path-is-400 GET " + path, path)
              .expect(
                  c ->
                      c.status(400)
                          .contentType("application/problem+json")
                          .json("/title", "Bad Request")
                          .json("/instance", path)
                          .totalCalls(0)));
    }
    s.add(
        Scenario.api(G, "unknown-path-is-400 POST /v1/unknown", "POST", "/v1/unknown")
            .json("{}")
            .expect(c -> c.status(400).contentType("application/problem+json").totalCalls(0)));
    s.add(
        Scenario.api(G, "unknown-path-detail", "/v1/unknown")
            .expect(c -> c.problem(400, "No static resource v1/unknown.")));
    s.add(
        Scenario.api(G, "root-path-is-400", "/")
            .expect(c -> c.problem(400, "No static resource .").totalCalls(0)));
    // Spring 6 does not match a trailing slash: the path is a missing static resource, never the operation
    s.add(
        Scenario.api(G, "trailing-slash-does-not-reach-the-operation", PRODUCTS + "/")
            .stub(HttpContractScenarios::productsOk)
            .expect(c -> c.problem(400, "No static resource v1/products.").totalCalls(0)));
    s.add(
        Scenario.api(G, "trailing-slash-does-not-reach-the-operation product", "/v1/product/")
            .expect(c -> c.problem(400, "No static resource v1/product.").totalCalls(0)));
    s.add(
        Scenario.api(G, "double-slash-is-rejected", "/v1//products")
            .stub(HttpContractScenarios::productsOk)
            .expect(c -> c.status(400).totalCalls(0)));
    s.add(
        Scenario.of(G, "unknown-path-unauthenticated-is-401", "GET", "/v1/unknown")
            .header("X-Tenant-Id", "PNPG")
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.of(G, "trailing-slash-unauthenticated-is-401", "GET", PRODUCTS + "/")
            .header("X-Tenant-Id", "PNPG")
            .expect(c -> c.problem(401, null).totalCalls(0)));
    s.add(
        Scenario.of(G, "root-path-unauthenticated-is-401", "GET", "/")
            .expect(c -> c.problem(401, null).totalCalls(0)));
  }

  private static void publicPaths(List<Scenario> s) {
    s.add(
        Scenario.of(G, "health-is-public", "GET", "/actuator/health")
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/vnd.spring-boot.actuator.v3+json")
                        .json("/status", "UP")
                        .headerAbsent("Cache-Control")
                        .totalCalls(0)));
    s.add(
        Scenario.of(G, "health-head-is-public", "HEAD", "/actuator/health")
            .expect(c -> c.status(200).noBody().totalCalls(0)));
    s.add(
        Scenario.api(G, "health-with-token-is-the-same", "/actuator/health")
            .expect(c -> c.status(200).json("/status", "UP").totalCalls(0)));
    s.add(
        Scenario.of(G, "health-liveness-is-not-exposed", "GET", "/actuator/health/liveness")
            .expect(c -> c.status(404).noBody()));
    s.add(
        Scenario.of(G, "health-readiness-is-not-exposed", "GET", "/actuator/health/readiness")
            .expect(c -> c.status(404).noBody()));
    s.add(
        Scenario.of(G, "actuator-index-is-public", "GET", "/actuator")
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/vnd.spring-boot.actuator.v3+json")
                        .jsonPresent("/_links/health")));
    for (String path : new String[] {"/actuator/info", "/actuator/prometheus", "/favicon.ico", "/dapr/config"}) {
      s.add(
          Scenario.of(G, "public-but-unmapped " + path, "GET", path)
              .expect(
                  c ->
                      c.problem(400, "No static resource " + path.substring(1) + ".")
                          .json("/instance", path)
                          .totalCalls(0)));
    }
    s.add(Scenario.of(G, "error-endpoint-is-public", "GET", "/error").expect(c -> c.status(500).totalCalls(0)));
    s.add(
        Scenario.of(G, "api-docs-are-public", "GET", "/v3/api-docs")
            .expect(
                c ->
                    c.status(200)
                        .contentType("application/json")
                        .jsonPresent("/paths")
                        .jsonPresent("/components/schemas")
                        .totalCalls(0)));
    s.add(
        Scenario.of(G, "swagger-ui-is-public", "GET", "/swagger-ui/index.html")
            .expect(c -> c.status(200).contentType("text/html").totalCalls(0)));
    s.add(
        Scenario.of(G, "swagger-ui-html-redirects", "GET", "/swagger-ui.html")
            .expect(c -> c.status(302).header("Location", "/swagger-ui/index.html")));
  }

  private static void cors(List<Scenario> s) {
    s.add(
        Scenario.api(G, "cors-preflight-is-rejected", "OPTIONS", PRODUCTS)
            .header("Origin", "https://example.test")
            .header("Access-Control-Request-Method", "GET")
            .expect(
                c ->
                    c.status(403)
                        .bodyContains("Invalid CORS request")
                        .headerAbsent("Access-Control-Allow-Origin")
                        .totalCalls(0)));
    s.add(
        Scenario.api(G, "cors-simple-request-gets-no-cors-headers", PRODUCTS)
            .header("Origin", "https://example.test")
            .stub(HttpContractScenarios::productsOk)
            .expect(
                c ->
                    c.status(200)
                        .headerAbsent("Access-Control-Allow-Origin")
                        .headerAbsent("Access-Control-Allow-Credentials")));
    s.add(
        Scenario.api(G, "options-lists-the-allowed-methods", "OPTIONS", PRODUCTS)
            .expect(c -> c.status(200).headerItems("Allow", "GET", "HEAD", "OPTIONS").noBody().totalCalls(0)));
    s.add(
        Scenario.of(G, "options-unauthenticated-is-401", "OPTIONS", PRODUCTS)
            .header("X-Tenant-Id", "PNPG")
            .expect(c -> c.problem(401, null).totalCalls(0)));
  }

  private static void negotiation(List<Scenario> s) {
    for (String accept : new String[] {"application/xml", "text/plain"}) {
      s.add(
          Scenario.api(G, "accept-" + accept + "-is-not-acceptable", PRODUCTS)
              .header("Accept", accept)
              .stub(HttpContractScenarios::productsOk)
              .expect(
                  c ->
                      c.problem(406, "No acceptable representation")
                          .json("/title", "Not Acceptable")
                          .totalCalls(0)));
    }
    s.add(
        Scenario.api(G, "accept-wildcard-is-served", PRODUCTS)
            .header("Accept", "*/*")
            .stub(HttpContractScenarios::productsOk)
            .expect(c -> c.status(200).contentType("application/json")));
  }

  private static void forwarding(List<Scenario> s) {
    s.add(
        Scenario.api(G, "tracing-headers-are-not-forwarded-downstream", PRODUCTS)
            .header("X-Correlation-Id", "corr-1")
            .header("X-Request-Id", "req-1")
            .header("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")
            .stub(HttpContractScenarios::productsOk)
            .expect(
                c ->
                    c.status(200)
                        .propagatesIdentity()
                        .call("ms-product", "GET", "/product")
                        .headerAbsent("x-correlation-id")
                        .headerAbsent("x-request-id")
                        .headerAbsent("traceparent")));
  }
}
