package it.pagopa.selfcare.onboarding.steps;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.ValidatableResponse;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.controller.OnboardingController;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.GeoTaxonomies;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;
import org.openapi.quarkus.core_json.model.OnboardedProductResponse;
import org.openapi.quarkus.onboarding_functions_json.api.OrchestrationApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"it.pagopa.selfcare.onboarding"},
        plugin = {
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json"
        },
        tags = "@Onboarding")
@TestHTTPEndpoint(OnboardingController.class)
@TestProfile(IntegrationProfile.class)
@Slf4j
public class OnboardingStep extends CucumberQuarkusTest {

  private ValidatableResponse validatableResponse;
  private static ObjectMapper objectMapper;
  private static String tokenTest;
  private static final String JWT_BEARER_TOKEN_ENV = "custom.jwt-token-test";

  @InjectMock @RestClient OrchestrationApi orchestrationApi;
  @InjectMock @RestClient InstitutionApi institutionApi;

  @Inject ScenarioContext context;

  static MongoDatabase mongoDatabase;

  static ComposeContainer composeContainer;

  public static void main(String[] args) {
    runMain(OnboardingStep.class, args);
  }

  @BeforeAll
  static void setup() {
    tokenTest = ConfigProvider.getConfig().getValue(JWT_BEARER_TOKEN_ENV, String.class);
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Vertx vertx = Vertx.vertx();
    vertx
            .getOrCreateContext()
            .config()
            .put("quarkus.vertx.event-loop-blocked-check-interval", 5000);

    log.info("Starting test containers...");

    composeContainer = new ComposeContainer(new File("src/test/resources/docker-compose.yml"))
            .withPull(true)
            .waitingFor("mongo-db", Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(5));

    composeContainer.start();
    Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

    log.info("Test containers started successfully");

    initDb();
    log.debug("Init completed");
  }

  @BeforeEach
  void init() {
    when(orchestrationApi.apiStartOnboardingOrchestrationGet(any(), any()))
            .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));
    mockMSCoreResponses();
  }

  private static void initDb() {
    mongoDatabase = IntegrationProfile.getMongoClientConnection();

    Onboarding onboarding = createDummyOnboarding();
    Onboarding duplicatedOnboardingPA = createOnboardingForConflictScenario();
    Token token = createDummyToken();

    Uni.combine().all().unis(
                    onboarding.persist(),
                    duplicatedOnboardingPA.persist(),
                    token.persist()
            ).asTuple()
            .invoke(tuple -> {
              var persistedOnboarding = (Onboarding) tuple.getItem1();
              var deuplicatedOnboarding = (Onboarding) tuple.getItem2();
              var persistedToken = (Token) tuple.getItem3();

              assertNotNull(persistedOnboarding.getId());
              assertNotNull(deuplicatedOnboarding.getId());
              assertNotNull(persistedToken.getId());

              mongoDatabase.getCollection("onboardings")
                      .createIndex(Indexes.ascending("createdAt"));
            })
            .await().indefinitely();
  }

  @Given("I have a request object named {string}")
  public void iHaveRequestObjectNamed(String name) {
    context.storeRequestBody(name);
  }

  @When("I send a POST request to {string} with this request")
  public void iSendPostRequestWithNamedRequest(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingDefaultRequest request =
            objectMapper.readValue(requestBody, OnboardingDefaultRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request for PNPG to {string} with this request")
  public void iSendPostRequestWithNamedRequestForPnpg(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingPgRequest request = objectMapper.readValue(requestBody, OnboardingPgRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request for user to {string} with this request")
  public void iSendPostRequestWithNamedRequestForUser(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingUserRequest request = objectMapper.readValue(requestBody, OnboardingUserRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request for PSP to {string} with this request")
  public void iSendPostRequestWithNamedRequestForPsp(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingPspRequest request = objectMapper.readValue(requestBody, OnboardingPspRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request for import PSP to {string} with this request")
  public void iSendPostRequestWithNamedRequestForImportPsp(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingImportPspRequest request = objectMapper.readValue(requestBody, OnboardingImportPspRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request for import PRV to {string} with this request")
  public void iSendPostRequestWithNamedRequestForImportPrv(String url) throws JsonProcessingException {
    String requestBody = context.getCurrentRequestBody();
    OnboardingDefaultRequest request = objectMapper.readValue(requestBody, OnboardingDefaultRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @Given(
          "I have an onboarding record with onboardingId {string} the current recipient code is {string}")
  public void givenDataCheck(String onboardingId, String recipientCode) {
    assertNotNull(onboardingId);
    assertNotNull(recipientCode);
  }

  @When("I send a PUT request to {string} with {string} and {string}")
  public void doCallApi(String url, String onboardingId, String recipientCode) {
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .pathParam("onboardingId", onboardingId)
                    .queryParam("recipientCode", recipientCode)
                    .when()
                    .put(url)
                    .then();
  }

  @Then("the response status code should be {int}")
  public void verifyStatusCodeResponse(int statusCode) {
    assertEquals(statusCode, validatableResponse.extract().statusCode());
  }

  @Then("the response should contain the text {string}")
  public void verifyResponseText(String expectedText) {
    String responseBody = validatableResponse.extract().body().asString();
    System.out.println(responseBody);
    assertTrue(responseBody.contains(expectedText), "Response does not contain expected text");
  }

  @Given("I have an empty request object")
  public void givenEmptyRequst() {
    OnboardingDefaultRequest request = new OnboardingDefaultRequest();
    Arrays.stream(request.getClass().getDeclaredFields())
            .forEach(
                    field -> {
                      field.setAccessible(true);
                      try {
                        assertNull(
                                field.get(request),
                                "L'attributo " + field.getName() + " dovrebbe essere nullo");
                      } catch (IllegalAccessException e) {
                        fail("Impossibile accedere all'attributo " + field.getName());
                      }
                    });
  }

  @When("I send a POST request to {string} with empty body")
  @TestSecurity(
          user = "testUser",
          roles = {"admin", "user"})
  public void doCallApi(String url) {
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OnboardingDefaultRequest())
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a POST request to {string} with the request body")
  @TestSecurity(
          user = "testUser",
          roles = {"admin", "user"})
  public void doCallApi(String url, String onboardingRequest) throws JsonProcessingException {
    OnboardingDefaultRequest request =
            objectMapper.readValue(onboardingRequest, OnboardingDefaultRequest.class);
    assertNotNull(request);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @When("I send a duplicated POST request to {string} with the request body")
  @TestSecurity(
          user = "testUser",
          roles = {"admin", "user"})
  public void doCallApiForConflictScenario(String url, String requestBody)
          throws JsonProcessingException {
    OnboardingDefaultRequest request =
            objectMapper.readValue(requestBody, OnboardingDefaultRequest.class);
    validatableResponse =
            given()
                    .header("Authorization", "Bearer " + tokenTest)
                    .body(request)
                    .when()
                    .post(url)
                    .then();
  }

  @Then("the response body should not be empty")
  public void theResponseBodyShouldNotBeEmpty() {
    String responseBody = validatableResponse.extract().body().asString();
    assertThat(responseBody).isNotEmpty();
  }

  @Then("the response should have field {string} with value {string}")
  public void theResponseShouldHaveFieldWithValue(String fieldName, String expectedValue) {
    String actualValue = validatableResponse.extract().jsonPath().getString(fieldName);
    assertThat(actualValue).isEqualTo(expectedValue);
  }

  @Then("there is a document for onboardings with origin {string} originId {string} and workflowType {string}")
  public void theResponseShouldHaveFieldWithValue(String origin, String originId, String worfklowType) {
    var onboardings = Onboarding.find("workflowType = ?1 and institution.origin = ?2 and institution.originId = ?3",
                    worfklowType, origin, originId).list()
            .await().indefinitely();
    assertFalse(onboardings.isEmpty());
  }

  @AfterAll
  static void destroyDatabase() {
      if (mongoDatabase != null) {
          mongoDatabase.drop();
      }
      if (composeContainer != null) {
          composeContainer.stop();
      }
  }

  // utils
  private static Onboarding createDummyOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    onboarding.setProductId("prod-pagopa");
    onboarding.setCreatedAt(LocalDateTime.now());

    Institution institution = new Institution();
    institution.setTaxCode("taxCode");
    institution.setSubunitCode("subunitCode");
    institution.setInstitutionType(InstitutionType.PSP);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("RC000");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  private static Onboarding createOnboardingForConflictScenario() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId(UUID.randomUUID().toString());
    onboarding.setProductId("prod-io");
    onboarding.setStatus(OnboardingStatus.COMPLETED);
    onboarding.setCreatedAt(LocalDateTime.now());
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);

    Institution institution = new Institution();
    institution.setOrigin(Origin.IPA);
    institution.setOriginId("c_l186");
    institution.setDescription("Comune di Tocco da Casauria");
    institution.setTaxCode("00231830688");
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);

    Billing billing = new Billing();
    billing.setRecipientCode("UFD333");
    onboarding.setBilling(billing);

    User user = new User();
    user.setId("actual-user-id");
    user.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(user));

    return onboarding;
  }

  private static Token createDummyToken() {
    Token token = new Token();
    token.setId(UUID.fromString("89ad7142-24bb-48ad-8504-9c9231137e85").toString());
    token.setProductId("prod-pagopa");
    token.setCreatedAt(LocalDateTime.now());
    return token;
  }

  private void mockMSCoreResponses() {
    InstitutionsResponse institutionsResponse = new InstitutionsResponse();
    institutionsResponse.setInstitutions(List.of());
    when(institutionApi.getInstitutionsUsingGET("83001010616", null, null, null, null, null))
            .thenReturn(Uni.createFrom().item(institutionsResponse));
    when(institutionApi.getInstitutionsUsingGET("00095990644", null, null, null, null, null))
            .thenReturn(Uni.createFrom().item(mockInstitutionsResponse("00095990644", "c_a489")));
    when(institutionApi.getInstitutionsUsingGET("00231830688", null, null, null, null, null))
            .thenReturn(Uni.createFrom().item(mockInstitutionsResponse("00231830688", "c_l186")));
  }

  private static InstitutionsResponse mockInstitutionsResponse(String taxCode, String ipaCode) {
    InstitutionsResponse institutionsResponse = new InstitutionsResponse();
    InstitutionResponse institutionResponse = new InstitutionResponse();
    institutionResponse.setId(UUID.randomUUID().toString());
    institutionResponse.setCity("Napoli");
    institutionResponse.setCounty("NA");
    institutionResponse.setCountry("IT");
    institutionResponse.setCreatedAt(OffsetDateTime.now());
    institutionResponse.setInstitutionType("PA");
    institutionResponse.setDescription("Comune di Atripalda");
    institutionResponse.setOrigin(Origin.IPA.name());
    institutionResponse.setOriginId(ipaCode);
    institutionResponse.setAddress("p. Municipio 1");
    institutionResponse.setDigitalAddress("comune.atripalda@legalmail.it");
    institutionResponse.setExternalId(taxCode);
    institutionResponse.setTaxCode(taxCode);
    OnboardedProductResponse onboardedProductResponse = new OnboardedProductResponse();
    onboardedProductResponse.setProductId("prod-io");
    institutionResponse.setOnboarding(List.of(onboardedProductResponse));
    GeoTaxonomies geoTaxonomies = new GeoTaxonomies();
    geoTaxonomies.setCode("ITA");
    geoTaxonomies.setDesc("ITALIA");
    institutionResponse.setGeographicTaxonomies(List.of(geoTaxonomies));
    institutionsResponse.setInstitutions(List.of(institutionResponse));
    return institutionsResponse;
  }
}
