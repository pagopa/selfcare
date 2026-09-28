package it.pagopa.selfcare.onboarding.service.impl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.service.ProductService;
import it.pagopa.selfcare.onboarding.steps.IntegrationProfile;
import it.pagopa.selfcare.onboarding.support.ProductHttpContractProfile;
import it.pagopa.selfcare.onboarding.support.ProductHttpContractResource;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.ProductResponse;

@QuarkusTest
@TestProfile(ProductHttpContractProfile.class)
@QuarkusTestResource(value = ProductHttpContractResource.class, restrictToAnnotatedClass = true)
class ProductServiceHttpTest {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private final Map<String, String> tokens = Map.of(
            "AR", IntegrationProfile.tokenForTenant("AR", "PAGOPA"),
            "PNPG", IntegrationProfile.tokenForTenant("PNPG", "PAGOPA"));

    @Inject ProductService productService;
    private String serverUrl;

    @BeforeEach
    void resetRecordedRequests() throws Exception {
        serverUrl = ConfigProvider.getConfig().getValue("product-http-contract.url", String.class);
        assertInstanceOf(ProductServiceImpl.class, productService, "HTTP tests must not use IntegrationProductService");
        assertEquals(200, HTTP.send(HttpRequest.newBuilder(URI.create(serverUrl + "/_requests"))
                .DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test
    void generatedClientDeserializesTenantRolesAndContractFieldsAcrossAlternatingRequests() throws Exception {
        for (String tenant : List.of("AR", "PNPG", "AR", "PNPG")) {
            ProductResponse product = request(tenant).get("/product-http-contract/valid/prod-io")
                    .then().statusCode(200).extract().as(ProductResponse.class);
            String prefix = tenant.toLowerCase(Locale.ROOT);
            assertEquals("prod-io", product.getProductId());
            assertEquals(tenant, product.getTenantId());
            assertEquals("IO " + tenant + " contract fixture", product.getTitle());
            assertEquals(prefix + "-admin", product.getRoleMappings().get(0).getBackOfficeRoles().get(0).getCode());
            assertEquals(prefix + "-pt", product.getPartnerTechRoleMappings().get(0).getBackOfficeRoles().get(0).getCode());
            assertEquals("contracts/" + prefix + "/io.html", product.getContracts().get(0).getPath());
            assertEquals("AR".equals(tenant) ? "ar-1" : "pnpg-2", product.getContracts().get(0).getVersion());
            assertEquals("AR".equals(tenant) ? 15 : 45, product.getFeatures().getExpirationDays());
        }
        List<Map<String, String>> calls = recordedRequests();
        assertEquals(4, calls.size());
        for (int i = 0; i < calls.size(); i++) {
            assertWire(calls.get(i), i % 2 == 0 ? "AR" : "PNPG", "GET", "/prod-io/valid");
        }
    }

    @Test
    void concurrentRequestsDoNotShareTenantOrAuthorization() throws Exception {
        List<CompletableFuture<Void>> responses = IntStream.range(0, 8).mapToObj(i ->
                CompletableFuture.runAsync(() -> {
                    String tenant = i % 2 == 0 ? "AR" : "PNPG";
                    request(tenant).get("/product-http-contract/product/prod-io").then()
                            .statusCode(200).body("tenantId", org.hamcrest.Matchers.equalTo(tenant));
                })).toList();
        CompletableFuture.allOf(responses.toArray(CompletableFuture[]::new)).join();
        List<Map<String, String>> calls = recordedRequests();
        assertEquals(8, calls.size());
        assertEquals(4, calls.stream().filter(call -> "AR".equals(call.get("tenant"))).count());
        for (Map<String, String> call : calls) {
            assertWire(call, call.get("tenant"), "GET", "/prod-io");
        }
    }

    @Test
    void allCatalogOperationsUseTenantPathsAndCorrectQueriesAndMethods() throws Exception {
        for (String tenant : List.of("AR", "PNPG")) {
            String type = "AR".equals(tenant) ? "PA" : "PG";
            String origin = "AR".equals(tenant) ? "IPA" : "INFOCAMERE";
            request(tenant).queryParam("institutionType", type).queryParam("origin", origin)
                    .get("/product-http-contract/workflow/prod-io").then().statusCode(200)
                    .body("workflowType", org.hamcrest.Matchers.equalTo(
                            "AR".equals(tenant) ? "CONTRACT_REGISTRATION" : "FOR_APPROVE"));
            request(tenant).queryParam("institutionType", type).queryParam("origin", origin)
                    .get("/product-http-contract/documents/prod-io").then().statusCode(200)
                    .body("[0].id", org.hamcrest.Matchers.equalTo(tenant.toLowerCase(Locale.ROOT) + "-doc"))
                    .body("[0].required", org.hamcrest.Matchers.equalTo(true))
                    .body("[0].storageOrigin", org.hamcrest.Matchers.equalTo("USER"));
            assertEquals("true", request(tenant)
                    .queryParam("institutionType", type).queryParam("origin", origin)
                    .get("/product-http-contract/enabled/prod-io").then().statusCode(200).extract().asString());
            assertEquals("AR".equals(tenant) ? "15" : "45", request(tenant)
                    .get("/product-http-contract/expiration/prod-io").then().statusCode(200).extract().asString());
        }
        List<Map<String, String>> calls = recordedRequests();
        assertEquals(8, calls.size());
        for (int offset : List.of(0, 4)) {
            String tenant = offset == 0 ? "AR" : "PNPG";
            Map<String, String> query = Map.of("institutionType", offset == 0 ? "PA" : "PG",
                    "origin", offset == 0 ? "IPA" : "INFOCAMERE");
            assertWire(calls.get(offset), tenant, "GET", "/workflow-type");
            assertEquals("prod-io", query(calls.get(offset)).get("productId"));
            assertTrue(query(calls.get(offset)).entrySet().containsAll(query.entrySet()));
            assertWire(calls.get(offset + 1), tenant, "GET", "/prod-io/required-documents");
            assertEquals(query, query(calls.get(offset + 1)));
            assertWire(calls.get(offset + 2), tenant, "HEAD", "/prod-io/required-documents/enabled");
            assertEquals(query, query(calls.get(offset + 2)));
            assertWire(calls.get(offset + 3), tenant, "GET", "/prod-io/expiration-days");
        }
    }

    @Test
    void missingExpirationUsesExistingDefaultRatherThanBlobFallback() throws Exception {
        assertEquals("30", request("AR").get("/product-http-contract/expiration/empty-expiration")
                .then().statusCode(200).extract().asString());
        assertWire(recordedRequests().get(0), "AR", "GET", "/empty-expiration/expiration-days");
    }

    @Test
    void emptyRequiredDocumentsFlagIsFalse() throws Exception {
        assertEquals("false", request("PNPG").queryParam("institutionType", "PG")
                .queryParam("origin", "INFOCAMERE").get("/product-http-contract/enabled/prod-pn")
                .then().statusCode(200).extract().asString());
        assertWire(recordedRequests().get(0), "PNPG", "HEAD", "/prod-pn/required-documents/enabled");
    }

    @Test
    void notFoundUnavailableAndTimeoutRemainFailures() throws Exception {
        request("AR").get("/product-http-contract/valid/missing").then().statusCode(404)
                .body("exception", org.hamcrest.Matchers.endsWith("ResourceNotFoundException"));
        request("AR").get("/product-http-contract/product/unavailable").then().statusCode(503);
        Response timedOut = request("PNPG").get("/product-http-contract/product/slow");
        assertEquals(502, timedOut.statusCode(), timedOut.asString());
        assertTrue(timedOut.asString().toLowerCase(Locale.ROOT).contains("timeout"), timedOut.asString());
        assertEquals(3, recordedRequests().size());
    }

    @Test
    void canonicalTenantIsPropagatedAndConflictsDoNotReachProduct() throws Exception {
        assertEquals("PAGOPA", JWT.decode(tokens.get("AR")).getIssuer());
        request("ar").get("/product-http-contract/product/prod-io").then().statusCode(200);
        assertWire(recordedRequests().get(0), "AR", "GET", "/prod-io");
        request("AR").queryParam("explicitTenant", "PNPG")
                .get("/product-http-contract/valid/prod-io").then().statusCode(502)
                .body("exception", org.hamcrest.Matchers.endsWith("IllegalArgumentException"));
        given().basePath("").header("Authorization", "Bearer " + tokens.get("AR"))
                .get("/product-http-contract/product/prod-io").then().statusCode(400);
        given().basePath("").header("Authorization", "Bearer " + tokens.get("AR"))
                .header("X-Tenant-Id", "UNKNOWN")
                .get("/product-http-contract/product/prod-io").then().statusCode(400);
        assertEquals(1, recordedRequests().size());
    }

    @Test
    void spidCoherentClaimAndHeaderPropagateForBothTenants() throws Exception {
        for (String tenant : List.of("AR", "PNPG")) {
            String token = IntegrationProfile.tokenForTenant(tenant);
            assertEquals("SPID", JWT.decode(token).getIssuer());
            assertEquals(tenant, JWT.decode(token).getClaim("tenant_id").asString());
            request(tenant, token).get("/product-http-contract/product/prod-io").then()
                    .statusCode(200).body("tenantId", org.hamcrest.Matchers.equalTo(tenant));
            List<Map<String, String>> calls = recordedRequests();
            assertWire(calls.get(calls.size() - 1), tenant, "GET", "/prod-io", token);
        }
        assertEquals(2, recordedRequests().size());
    }

    @Test
    void spidMismatchedClaimAndNonCanonicalHeaderAreRejectedBeforeProduct() throws Exception {
        String arToken = IntegrationProfile.tokenForTenant("AR");
        request("PNPG", arToken).get("/product-http-contract/product/prod-io").then().statusCode(401);
        request("ar", arToken).get("/product-http-contract/product/prod-io").then().statusCode(401);
        request("AR", IntegrationProfile.tokenForTenant("UNKNOWN"))
                .get("/product-http-contract/product/prod-io").then().statusCode(401);
        assertTrue(recordedRequests().isEmpty());
    }

    @Test
    void pagopaTenantRemainsHeaderDrivenRatherThanApplyingSpidClaimPolicy() throws Exception {
        String arToken = tokens.get("AR");
        assertEquals("PAGOPA", JWT.decode(arToken).getIssuer());
        assertEquals("AR", JWT.decode(arToken).getClaim("tenant_id").asString());
        request("PNPG", arToken).get("/product-http-contract/product/prod-io").then()
                .statusCode(200).body("tenantId", org.hamcrest.Matchers.equalTo("PNPG"));
        assertWire(recordedRequests().get(0), "PNPG", "GET", "/prod-io", arToken);
    }

    private RequestSpecification request(String tenant) {
        return request(tenant, tokens.get(tenant.toUpperCase(Locale.ROOT)));
    }

    private RequestSpecification request(String tenant, String token) {
        return given().basePath("").header("X-Tenant-Id", tenant)
                .header("Authorization", "Bearer " + token);
    }

    private List<Map<String, String>> recordedRequests() throws Exception {
        String body = HTTP.send(HttpRequest.newBuilder(URI.create(serverUrl + "/_requests")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        return MAPPER.readValue(body, new TypeReference<>() {});
    }

    private void assertWire(Map<String, String> call, String tenant, String method, String suffix) {
        assertWire(call, tenant, method, suffix, tokens.get(tenant));
    }

    private void assertWire(Map<String, String> call, String tenant, String method, String suffix, String token) {
        assertEquals(method, call.get("method"));
        assertEquals("/product/" + tenant + suffix, call.get("path"));
        assertEquals(tenant, call.get("tenant"));
        assertEquals("Bearer " + token, call.get("authorization"));
    }

    private Map<String, String> query(Map<String, String> call) {
        return Arrays.stream(call.get("query").split("&")).filter(part -> !part.isEmpty())
                .map(part -> part.split("=", 2)).collect(Collectors.toMap(part -> part[0],
                        part -> URLDecoder.decode(part[1], StandardCharsets.UTF_8)));
    }
}
