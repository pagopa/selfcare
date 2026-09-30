package it.pagopa.selfcare.onboarding.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductHttpContractResource implements QuarkusTestResourceLifecycleManager {
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, String>> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private ExecutorService executor;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            executor = Executors.newCachedThreadPool();
            server.setExecutor(executor);
            server.createContext("/", this::handle);
            server.start();
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            return Map.of(
                    "quarkus.rest-client.\"org.openapi.quarkus.product_json.api.ProductApi\".url", url,
                    "quarkus.rest-client.\"org.openapi.quarkus.product_json.api.ProductApi\".read-timeout", "1000",
                    "product-http-contract.url", url);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start Product HTTP contract server", e);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/_requests".equals(path)) {
                if ("DELETE".equals(exchange.getRequestMethod())) {
                    requests.clear();
                    send(exchange, 200, "[]");
                } else {
                    send(exchange, 200, mapper.writeValueAsString(requests));
                }
                return;
            }
            String tenant = exchange.getRequestHeaders().getFirst("X-Tenant-Id");
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requests.add(Map.of("path", path, "method", exchange.getRequestMethod(),
                    "tenant", Objects.isNull(tenant) ? "" : tenant,
                    "authorization", Objects.isNull(authorization) ? "" : authorization,
                    "query", Objects.isNull(exchange.getRequestURI().getRawQuery()) ? "" : exchange.getRequestURI().getRawQuery()));
            if (Objects.isNull(tenant) || !List.of("AR", "PNPG").contains(tenant)
                    || !path.startsWith("/product/" + tenant + "/")
                    || Objects.isNull(authorization) || !authorization.startsWith("Bearer ")) {
                send(exchange, 400, "{\"detail\":\"Path, canonical tenant and Authorization are required\"}");
                return;
            }
            if (path.contains("/missing")) {
                send(exchange, 404, "{\"detail\":\"Product missing\"}");
                return;
            }
            if (path.contains("/unavailable")) {
                send(exchange, 503, "{\"detail\":\"Catalog unavailable\"}");
                return;
            }
            if (path.contains("/slow")) {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                send(exchange, 200, "{}");
                return;
            }
            JsonNode product = fixture(tenant);
            String root = "/product/" + tenant;
            if (path.equals(root + "/workflow-type") && "GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, "{\"workflowType\":\""
                        + product.path("workflowRules").get(0).path("workflowType").asText() + "\"}");
            } else if ((path.equals(root + "/prod-io/required-documents/enabled")
                    || path.equals(root + "/prod-pn/required-documents/enabled"))
                    && "HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("X-Required-Documents-Enabled", String.valueOf(path.contains("/prod-io/")));
                exchange.sendResponseHeaders(200, -1);
            } else if (path.equals(root + "/prod-io/required-documents") && "GET".equals(exchange.getRequestMethod())) {
                ArrayNode documents = product.path("requiredDocuments").deepCopy();
                documents.forEach(document -> ((ObjectNode) document).remove("filter"));
                send(exchange, 200, mapper.writeValueAsString(documents));
            } else if (path.equals(root + "/prod-io/expiration-days") && "GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, "{\"expirationDays\":" + product.path("features").path("expirationDays") + "}");
            } else if (path.equals(root + "/empty-expiration/expiration-days") && "GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, "{}");
            } else if ((path.equals(root + "/prod-io") || path.equals(root + "/prod-io/valid"))
                    && "GET".equals(exchange.getRequestMethod())) {
                send(exchange, 200, mapper.writeValueAsString(product));
            } else {
                send(exchange, 404, "{\"detail\":\"Unexpected Product API route\"}");
            }
        }
    }

    private JsonNode fixture(String tenant) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "integration-data/product-api/prod-io-" + tenant.toLowerCase(java.util.Locale.ROOT) + ".json")) {
            if (Objects.isNull(input)) {
                throw new IllegalStateException("Missing HTTP product fixture: " + tenant);
            }
            return mapper.readTree(input);
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void stop() {
        if (Objects.nonNull(server)) {
            server.stop(0);
        }
        if (Objects.nonNull(executor)) {
            executor.shutdownNow();
        }
    }
}
