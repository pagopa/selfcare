package it.pagopa.selfcare.onboarding.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fake onboarding-functions recording the headers of every HTTP trigger call. */
public class OnboardingFunctionsHttpContractResource implements QuarkusTestResourceLifecycleManager {
    private static final String CLIENT_PREFIX = "quarkus.rest-client.\"org.openapi.quarkus.onboarding_functions_json.api.";

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
                    CLIENT_PREFIX + "OrchestrationApi\".url", url,
                    "onboarding-functions-http-contract.url", url);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start onboarding-functions HTTP contract server", e);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/_requests".equals(path)) {
                if ("DELETE".equals(exchange.getRequestMethod())) {
                    requests.clear();
                    send(exchange, "[]");
                } else {
                    send(exchange, mapper.writeValueAsString(requests));
                }
                return;
            }
            requests.add(Map.of(
                    "path", path,
                    "query", Objects.requireNonNullElse(exchange.getRequestURI().getRawQuery(), ""),
                    "tenant", Objects.requireNonNullElse(exchange.getRequestHeaders().getFirst("X-Tenant-Id"), ""),
                    "functionKey", Objects.requireNonNullElse(exchange.getRequestHeaders().getFirst("x-functions-key"), ""),
                    "authorization", Objects.requireNonNullElse(exchange.getRequestHeaders().getFirst("Authorization"), "")));
            send(exchange, "{}");
        }
    }

    private void send(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
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
