package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.FunctionInvocationLogger;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EqualsAndHashCode(callSuper = true)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"it.pagopa.selfcare.onboarding"},
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        },
        tags = "@Onboarding")
@TestProfile(IntegrationFunctionProfile.class)
@Slf4j
@Data
@TestInstance(Lifecycle.PER_CLASS)
public class OnboardingFunctionStep extends CucumberQuarkusTest {

    private static final long STATUS_POLL_TIMEOUT_MS = 40_000L;
    private static final long STATUS_POLL_INTERVAL_MS = 1_000L;
    private static final String INTEGRATION_TEST_CALLER_ID = "m2m:onboarding-functions-it";
    private static final String INTEGRATION_TEST_TENANT = "AR";

    private ValidatableResponse validatableResponse;
    private static ObjectMapper objectMapper;
    private String onboardingId;
    private String authorizationHeader;
    private String tenantHeader;
    private String statusBeforeRequest;

    MongoClient mongoClient;
    MongoDatabase onboardingDatabase;
    MongoDatabase institutionDatabase;
    MongoDatabase userDatabase;

    private RequestSpecification request;
    private Response response;
    private Onboarding onboarding;

    @Inject
    IntegrationOnboardingResources integrationOnboardingResources;

    @Inject
    IntegrationOperationUtils integrationOperationUtils;

    public static void main(String[] args) {
        runMain(OnboardingFunctionStep.class, args);
    }

    @BeforeAll
    void setup() {
        initDb();
        log.debug("Init completed");
    }

    @BeforeEach
    void resetRestAssured() {
        RestAssured.reset();
        authorizationHeader = "Bearer " + testBearerToken(INTEGRATION_TEST_TENANT);
        tenantHeader = INTEGRATION_TEST_TENANT;
        statusBeforeRequest = null;
    }

    private void initDb() {
        mongoClient = IntegrationFunctionProfile.getMongoClientConnection();
        onboardingDatabase = IntegrationFunctionProfile.getOnboardingDatabase(mongoClient);
        institutionDatabase = IntegrationFunctionProfile.getInstitutionDatabase(mongoClient);
        userDatabase = IntegrationFunctionProfile.getUserDatabase(mongoClient);

        Map<String, Institution> institutionTemplateMap =
                integrationOnboardingResources.getInstitutionTemplateMap();

        institutionTemplateMap.forEach(
                (key, institution) -> {
                    log.info("Persist {}", key);
                    integrationOperationUtils.persistInstitution(institutionDatabase, institution);
                });
    }

    @Given("Preparing the invocation of {string} HTTP call with onboardingId {string}")
    public void setupCall(String functionName, String onboardingId) throws IllegalStateException {
        authorizationHeader = "Bearer " + testBearerToken(INTEGRATION_TEST_TENANT);
        tenantHeader = INTEGRATION_TEST_TENANT;
        statusBeforeRequest = null;
        RestAssured.baseURI = "http://localhost:8090";
        RestAssured.basePath = String.format("/api/%s", functionName);

        onboarding = integrationOperationUtils.findIntoMongoOnboarding(onboardingId);

        if (Objects.isNull(onboarding)) {
            onboarding = integrationOnboardingResources.getOnboardingJsonTemplate(onboardingId);

            if (!Objects.isNull(onboarding)) {
                if (onboarding.getTenantId() == null || onboarding.getTenantId().isBlank()) {
                    onboarding.setTenantId(INTEGRATION_TEST_TENANT);
                }
                integrationOperationUtils.persistIntoMongo(onboarding);
            } else {
                throw new IllegalStateException("Errore nel caricamento del json di test per " + onboardingId);
            }
        }
        setOnboardingId(onboarding.getId());
    }

    @Given("the request has no Authorization header")
    public void removeAuthorizationHeader() {
        authorizationHeader = null;
        rememberOnboardingStatus();
    }

    @Given("the request has a malformed bearer token")
    public void useMalformedBearerToken() {
        authorizationHeader = "Bearer malformed-test-token";
        rememberOnboardingStatus();
    }

    @Given("the request token belongs to tenant {string}")
    public void useTokenForTenant(String tenant) {
        authorizationHeader = "Bearer " + testBearerToken(tenant);
        rememberOnboardingStatus();
    }

    @Given("the request has no tenant header")
    public void removeTenantHeader() {
        tenantHeader = null;
        rememberOnboardingStatus();
    }

    @Given("the request tenant header is {string}")
    public void useTenantHeader(String tenant) {
        tenantHeader = tenant;
        rememberOnboardingStatus();
    }

    private static String testBearerToken(String tenant) {
        Map<String, String> payload = new HashMap<>(IntegrationFunctionProfile.buildJwtPayload());
        payload.put(TenantContext.TENANT_CLAIM, tenant);
        payload.put("uid", "fake-onboarding-functions-it-user");
        payload.put("fiscal_number", "FAKE-FISCAL-NUMBER");
        payload.put("name", "Test");
        payload.put("family_name", "User");
        return Objects.requireNonNull(
                it.pagopa.selfcare.onboarding.utils.JwtUtils.generateToken(
                        it.pagopa.selfcare.onboarding.utils.JwtData.builder()
                                .username("fake-user")
                                .password("fake-password")
                                .jwtHeader(IntegrationFunctionProfile.buildJwtHeader())
                                .jwtPayload(payload)
                                .build()),
                "Unable to generate the integration test JWT");
    }

    private void rememberOnboardingStatus() {
        Onboarding current = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());
        statusBeforeRequest = current == null || current.getStatus() == null
                ? null
                : current.getStatus().name();
    }

    @When("I send a GET request with given onboardingId")
    public void sendPostRequest() {
        RequestSpecification requestSpecification =
                given()
                        .log()
                        .all()
                        .header(FunctionInvocationLogger.USER_ID_HEADER, INTEGRATION_TEST_CALLER_ID);
        if (tenantHeader != null) {
            requestSpecification.header(TenantContext.TENANT_HEADER, tenantHeader);
        }
        if (authorizationHeader != null) {
            requestSpecification.header("Authorization", authorizationHeader);
        }

        response = requestSpecification
                .queryParam("onboardingId", getOnboardingId())
                .when()
                .get()
                .then()
                .log()
                .all()
                .extract()
                .response();
    }

    @Then("the response should have status code {int}")
    public void verifyStatusCode(int expectedStatusCode) {
        assertEquals(expectedStatusCode, response.getStatusCode(), "Status code non corrispondente");
    }

    @Then("the response header {string} should be {string}")
    public void verifyResponseHeader(String header, String expectedValue) {
        assertEquals(expectedValue, response.getHeader(header));
    }

    @Then("the onboarding status should not change")
    public void verifyOnboardingStatusDidNotChange() throws InterruptedException {
        Thread.sleep(1_000L);
        Onboarding current = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());
        String currentStatus = current == null || current.getStatus() == null
                ? null
                : current.getStatus().name();
        assertEquals(statusBeforeRequest, currentStatus, "Rejected request produced an onboarding side effect");
    }

    @Then("the answer should contain {string}")
    public void verifyResponseBody(String expectedResponse) {
        List<String> expectedValue = List.of(expectedResponse.split(","));

        response
                .then()
                .assertThat()
                .body("$", allOf(expectedValue.stream().map(key -> hasKey(key)).toArray(Matcher[]::new)));
    }

    @Then("there is a document for onboarding with status {string}")
    public void theResponseShouldHaveFieldWithValue(String status) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATUS_POLL_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            onboarding = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());

            if (Objects.nonNull(onboarding)
                    && Objects.nonNull(onboarding.getStatus())
                    && status.equals(onboarding.getStatus().name())) {
                return;
            }

            Thread.sleep(STATUS_POLL_INTERVAL_MS);
        }

        onboarding = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());
        assertTrue(Objects.nonNull(onboarding), "Onboarding not found after polling timeout");
        assertTrue(Objects.nonNull(onboarding.getStatus()), "Onboarding status is null after polling timeout");
        assertEquals(status, onboarding.getStatus().name());
    }

    @AfterAll
    void afterAll() {
        //onboardingDatabase.drop();
        //institutionDatabase.drop();
        //userDatabase.drop();
        log.info("Test terminated!");
    }
}
