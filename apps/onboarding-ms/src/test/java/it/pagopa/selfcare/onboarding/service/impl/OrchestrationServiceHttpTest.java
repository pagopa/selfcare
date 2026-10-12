package it.pagopa.selfcare.onboarding.service.impl;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.steps.IntegrationProfile;
import it.pagopa.selfcare.onboarding.support.OnboardingFunctionsHttpContractResource;
import it.pagopa.selfcare.onboarding.support.ProductHttpContractProfile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ProductHttpContractProfile.class)
@QuarkusTestResource(value = OnboardingFunctionsHttpContractResource.class, restrictToAnnotatedClass = true)
class OrchestrationServiceHttpTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String BASE_PATH = "/onboarding-functions-http-contract";

    private String serverUrl;

    @BeforeEach
    void resetRecordedRequests() throws Exception {
        serverUrl = ConfigProvider.getConfig().getValue("onboarding-functions-http-contract.url", String.class);
        assertEquals(200, HTTP.send(HttpRequest.newBuilder(URI.create(serverUrl + "/_requests"))
                .DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    @Test
    void startOrchestrationSendsValidatedTenantAndFunctionKey() throws Exception {
        for (String tenant : List.of("AR", "PNPG", "AR")) {
            request(tenant).get(BASE_PATH + "/start/onb-" + tenant).then().statusCode(200);
        }

        List<Map<String, String>> calls = recordedRequests();
        assertEquals(3, calls.size());
        assertCall(calls.get(0), "AR", "/api/StartOnboardingOrchestration", "onboardingId=onb-AR");
        assertCall(calls.get(1), "PNPG", "/api/StartOnboardingOrchestration", "onboardingId=onb-PNPG");
        assertCall(calls.get(2), "AR", "/api/StartOnboardingOrchestration", "onboardingId=onb-AR");
    }

    @Test
    void deleteInstitutionAndUserSendsValidatedTenantAndFunctionKey() throws Exception {
        request("PNPG").get(BASE_PATH + "/delete/onb-1").then().statusCode(200);

        List<Map<String, String>> calls = recordedRequests();
        assertEquals(1, calls.size());
        assertCall(calls.get(0), "PNPG", "/api/TriggerDeleteInstitutionAndUser", "onboardingId=onb-1");
    }

    @Test
    void tenantHeaderIsCanonicalized() throws Exception {
        request("ar", IntegrationProfile.tokenForTenant("AR", "PAGOPA"))
                .get(BASE_PATH + "/start/onb-1").then().statusCode(200);

        assertEquals("AR", recordedRequests().get(0).get("tenant"));
    }

    private RequestSpecification request(String tenant) {
        return request(tenant, IntegrationProfile.tokenForTenant(tenant, "PAGOPA"));
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

    private void assertCall(Map<String, String> call, String tenant, String path, String query) {
        assertEquals(path, call.get("path"));
        assertEquals(query, call.get("query"));
        assertEquals(tenant, call.get("tenant"));
        assertTrue(!call.get("functionKey").isBlank(), "function key must still be sent");
        assertEquals("", call.get("authorization"), "caller Authorization must not reach onboarding-functions");
    }
}
