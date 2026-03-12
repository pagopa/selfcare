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
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
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

    private ValidatableResponse validatableResponse;
    private static ObjectMapper objectMapper;
    private static String tokenTest;
    private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";
    private String onboardingId;

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
        RestAssured.baseURI = "http://localhost:8090";
        RestAssured.basePath = String.format("/api/%s", functionName);

        onboarding = integrationOperationUtils.findIntoMongoOnboarding(onboardingId);

        if (Objects.isNull(onboarding)) {
            onboarding = integrationOnboardingResources.getOnboardingJsonTemplate(onboardingId);

            if (!Objects.isNull(onboarding)) {
                integrationOperationUtils.persistIntoMongo(onboarding);
            } else {
                throw new IllegalStateException("Errore nel caricamento del json di test per " + onboardingId);
            }
        }
        setOnboardingId(onboarding.getId());
    }

    @When("I send a GET request with given onboardingId")
    public void sendPostRequest() {

        response =
                given()
                        .log()
                        .all()
                        .queryParam("onboardingId", getOnboardingId())
                        // .queryParam("timeout", 55000)
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
        Thread.sleep(40000);
        onboarding = integrationOperationUtils.findIntoMongoOnboarding(getOnboardingId());
        assertTrue(Objects.nonNull(onboarding));
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
