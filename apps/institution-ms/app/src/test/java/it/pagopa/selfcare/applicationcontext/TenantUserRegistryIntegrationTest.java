package it.pagopa.selfcare.applicationcontext;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import it.pagopa.selfcare.commons.tenant.TenantConfiguration;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.UnresolvedTenantException;
import it.pagopa.selfcare.mscore.api.UserRegistryConnector;
import it.pagopa.selfcare.mscore.connector.rest.UserRegistryConnectorImpl;
import it.pagopa.selfcare.mscore.connector.rest.client.UserRegistryRestClient;
import it.pagopa.selfcare.mscore.connector.rest.mapper.UserMapperClientImpl;
import it.pagopa.selfcare.mscore.web.util.DecryptIfUuidSerializer;
import it.pagopa.selfcare.mscore.web.util.EncryptIfTaxCodeDeserializer;
import it.pagopa.selfcare.mscore.web.util.EncryptedTaxCodeParam;
import it.pagopa.selfcare.mscore.web.util.EncryptedTaxCodeParamResolver;
import it.pagopa.selfcare.mscore.web.util.SpringContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TenantUserRegistryIntegrationTest.Config.class)
@TestPropertySource(locations = {
        "classpath:config/default-rest-client.properties",
        "classpath:config/user-registry-rest-client.properties"
})
class TenantUserRegistryIntegrationTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final List<String> API_KEYS = new CopyOnWriteArrayList<>();
    private static final HttpServer SERVER = startServer();

    @TestConfiguration
    @EnableFeignClients(clients = UserRegistryRestClient.class)
    @Import({TenantConfiguration.class, FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            UserRegistryConnectorImpl.class, UserMapperClientImpl.class})
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private TenantContext tenantContext;
    @Autowired
    private UserRegistryConnector connector;
    @Autowired
    private ObjectMapper mapper;

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/users", exchange -> {
                API_KEYS.add(exchange.getRequestHeaders().getFirst("x-api-key"));
                byte[] response = ("{\"id\":\"" + USER_ID + "\",\"fiscalCode\":\"TESTTAXCODE\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(response);
                } finally {
                    exchange.close();
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start User Registry test server", exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("rest-client.user-registry.base-url",
                () -> "http://localhost:" + SERVER.getAddress().getPort());
        registry.add("tenant.mongodb.required", () -> "false");
        registry.add("tenant.user-registry.mandatory-tenants", () -> "AR,PNPG");
        registry.add("USER_REGISTRY_API_KEY_AR", () -> "ar-key");
        registry.add("USER_REGISTRY_API_KEY_PNPG", () -> "pnpg-key");
        registry.add("tenant.registry.json", () -> """
                {"AR":{"userRegistry":{"apiKeyEnvVar":"USER_REGISTRY_API_KEY_AR"}},
                 "PNPG":{"userRegistry":{"apiKeyEnvVar":"USER_REGISTRY_API_KEY_PNPG"}}}
                """);
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @AfterEach
    void clearContext() {
        tenantContext.clear();
        SecurityContextHolder.clearContext();
        API_KEYS.clear();
    }

    @Test
    void convertersUseTheRequestTenantKeyForEveryLookup() throws Exception {
        String jwt = JWT.create().withAudience("selfcare").sign(Algorithm.HMAC256("test-only-key"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user", jwt));
        MethodParameter parameter = new MethodParameter(getClass()
                .getDeclaredMethod("parameter", String.class), 0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("taxCode", "TESTTAXCODE");

        try (MockedStatic<SpringContext> springContext = mockStatic(SpringContext.class)) {
            springContext.when(() -> SpringContext.getBean(UserRegistryConnector.class)).thenReturn(connector);
            for (String tenant : new String[] {"AR", "PNPG"}) {
                tenantContext.setTenantId(tenant);
                assertEquals(USER_ID, new EncryptedTaxCodeParamResolver(connector)
                        .resolveArgument(parameter, null, new ServletWebRequest(request), null));
                try (JsonParser parser = mapper.createParser("\"TESTTAXCODE\"")) {
                    parser.nextToken();
                    assertEquals(USER_ID, new EncryptIfTaxCodeDeserializer().deserialize(parser, null));
                }
                StringWriter output = new StringWriter();
                try (JsonGenerator generator = mapper.createGenerator(output)) {
                    new DecryptIfUuidSerializer().serialize(USER_ID, generator, null);
                }
                assertEquals("\"TESTTAXCODE\"", output.toString());
            }
        }
        assertEquals(List.of("ar-key", "ar-key", "ar-key", "pnpg-key", "pnpg-key", "pnpg-key"), API_KEYS);
    }

    @Test
    void missingTenantContextFailsBeforeCallingUserRegistry() {
        assertThrows(UnresolvedTenantException.class, () -> connector.getUserByFiscalCode("TESTTAXCODE"));
        assertTrue(API_KEYS.isEmpty());
    }

    private void parameter(@EncryptedTaxCodeParam("taxCode") String taxCode) {
    }
}
