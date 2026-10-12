package it.pagopa.selfcare.onboarding.integrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.auth0.jwt.JWT;
import com.sun.net.httpserver.HttpServer;
import it.pagopa.selfcare.cucumber.utils.CommonSteps;
import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import it.pagopa.selfcare.cucumber.utils.TestDataProvider;
import it.pagopa.selfcare.cucumber.utils.TestJwtGenerator;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CucumberTenantPropagationTest {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "HEAD", "DELETE"})
    void commonStepsSendTheFixtureTenantAlongsideTheToken(String method) throws Exception {
        TestDataProvider provider = new TestDataProvider();
        var account = provider.getTestData().getJwtData().get(0);
        String expectedTenant = (String) account.getJwtPayload().get("tenant_id");
        assertEquals("AR", expectedTenant);

        SharedStepData data = new SharedStepData();
        CommonSteps steps = new CommonSteps(data, provider, new TestJwtGenerator());
        AtomicReference<String> tenant = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/request", exchange -> {
            tenant.set(exchange.getRequestHeaders().getFirst("X-Tenant-Id"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().close();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        try {
            steps.login(account.getUsername(), account.getPassword());
            data.setRequestBody("{}");
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/request";
            switch (method) {
                case "GET" -> steps.sendGetRequest(url);
                case "POST" -> steps.sendPostRequest(url);
                case "PUT" -> steps.sendPutRequest(url);
                case "PATCH" -> steps.sendPatchRequest(url, "application/json");
                case "HEAD" -> steps.sendHeadRequest(url);
                case "DELETE" -> steps.sendDeleteRequest(url);
                default -> throw new IllegalArgumentException(method);
            }
            assertEquals(204, data.getResponse().statusCode());
            assertEquals(expectedTenant, tenant.get());
            assertNotNull(authorization.get());
            assertEquals("Bearer " + data.getToken(), authorization.get());
            assertEquals(expectedTenant, JWT.decode(data.getToken()).getClaim("tenant_id").asString());
        } finally {
            server.stop(0);
        }
    }
}
