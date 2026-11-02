package it.pagopa.selfcare.onboarding.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
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
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.FunctionInvocationLogger;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    static final String INTEGRATION_TEST_TENANT = "AR";
    // document-ms blob paths (document-ms.blob-storage.path-contracts / path-deleted)
    private static final String CONTRACTS_PATH = "parties/docs/";
    private static final String DELETED_CONTRACTS_PATH = "parties/deleted/";

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

        onboarding = loadOnboarding(onboardingId);
        setOnboardingId(onboarding.getId());
    }

    @Given("The onboarding {string} is present")
    public void ensureOnboardingIsPresent(String onboardingId) {
        loadOnboarding(onboardingId);
    }

    private Onboarding loadOnboarding(String onboardingId) {
        Onboarding current = integrationOperationUtils.findIntoMongoOnboarding(onboardingId);

        if (Objects.isNull(current)) {
            current = integrationOnboardingResources.getOnboardingJsonTemplate(onboardingId);

            if (!Objects.isNull(current)) {
                if (current.getTenantId() == null || current.getTenantId().isBlank()) {
                    current.setTenantId(INTEGRATION_TEST_TENANT);
                }
                integrationOperationUtils.persistIntoMongo(current);
            } else {
                throw new IllegalStateException("Errore nel caricamento del json di test per " + onboardingId);
            }
        }
        return current;
    }

    @Then("the contract document of the onboarding has rootOnboardingId {string}")
    public void verifyContractRootOnboardingId(String rootOnboardingId) {
        Document contract = findContractDocument(getOnboardingId());
        assertEquals(rootOnboardingId, contract.getString("rootOnboardingId"));
    }

    // The contract generated by BuildContract is marked as signed, so that a later deletion has a real blob to move
    @Given("the signed contract of the onboarding is uploaded")
    public void uploadSignedContract() {
        Document contract = findContractDocument(getOnboardingId());
        String contractPath = CONTRACTS_PATH + getOnboardingId() + "/" + contract.getString("contractFilename");
        contractDocumentsCollection().updateOne(contractDocumentFilter(getOnboardingId()), Updates.set("contractSigned", contractPath));
    }

    @Then("the onboarding {string} has status {string}")
    public void verifyOnboardingStatus(String onboardingId, String status) throws InterruptedException {
        waitForStatus(onboardingId, status);
    }

    @Then("the contract of the onboarding {string} has been moved to the deleted storage")
    public void verifyContractDeleted(String onboardingId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATUS_POLL_TIMEOUT_MS;
        String contractSigned = null;
        while (System.currentTimeMillis() < deadline) {
            contractSigned = findContractDocument(onboardingId).getString("contractSigned");
            if (contractSigned != null && contractSigned.startsWith(DELETED_CONTRACTS_PATH)) {
                return;
            }
            Thread.sleep(STATUS_POLL_INTERVAL_MS);
        }
        assertTrue(contractSigned != null && contractSigned.startsWith(DELETED_CONTRACTS_PATH),
                "Contract of onboarding " + onboardingId + " not moved to deleted storage, contractSigned=" + contractSigned);
    }

    @Then("the referenced onboarding {string} is aligned with the completed contract update")
    public void verifyReferencedOnboardingAligned(String referenceOnboardingId) {
        Onboarding contractUpdate = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());
        Onboarding adhesion = integrationOperationUtils.findIntoMongoOnboarding(referenceOnboardingId);
        assertNotNull(contractUpdate.getActivatedAt(), "Contract update activatedAt not set");
        assertEquals(Boolean.FALSE, adhesion.getContractUpdateRequired());
        assertEquals(contractUpdate.getActivatedAt(), adhesion.getUpdatedAt());
        assertNotNull(adhesion.getContractUpdateRequiredAt(), "contractUpdateRequiredAt must be preserved");
        assertEquals(OnboardingStatus.COMPLETED, adhesion.getStatus());
    }

    private Document findContractDocument(String onboardingId) {
        Document contract = contractDocumentsCollection().find(contractDocumentFilter(onboardingId)).first();
        assertNotNull(contract, "Contract document not found for onboarding " + onboardingId);
        return contract;
    }

    private MongoCollection<Document> contractDocumentsCollection() {
        return IntegrationFunctionProfile.getDocumentDatabase(mongoClient).getCollection("documents");
    }

    private static Bson contractDocumentFilter(String onboardingId) {
        return Filters.and(Filters.eq("onboardingId", onboardingId), Filters.eq("type", "INSTITUTION"));
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

    @Given("the request token has no tenant_id claim")
    public void useTokenWithoutTenantClaim() {
        authorizationHeader = "Bearer " + testBearerToken(null);
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
        if (tenant == null) {
            payload.remove(TenantContext.TENANT_CLAIM);
        } else {
            payload.put(TenantContext.TENANT_CLAIM, tenant);
        }
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
        onboarding = waitForStatus(getOnboardingId(), status);
    }

    private Onboarding waitForStatus(String onboardingId, String status) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATUS_POLL_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            Onboarding current = integrationOperationUtils.findIntoMongoOnboarding(onboardingId);

            if (Objects.nonNull(current)
                    && Objects.nonNull(current.getStatus())
                    && status.equals(current.getStatus().name())) {
                return current;
            }

            Thread.sleep(STATUS_POLL_INTERVAL_MS);
        }

        Onboarding current = integrationOperationUtils.findIntoMongoOnboarding(onboardingId);
        assertTrue(Objects.nonNull(current), "Onboarding not found after polling timeout");
        assertTrue(Objects.nonNull(current.getStatus()), "Onboarding status is null after polling timeout");
        assertEquals(status, current.getStatus().name());
        return current;
    }

    @AfterAll
    void afterAll() {
        //onboardingDatabase.drop();
        //institutionDatabase.drop();
        //userDatabase.drop();
        log.info("Test terminated!");
    }
}
