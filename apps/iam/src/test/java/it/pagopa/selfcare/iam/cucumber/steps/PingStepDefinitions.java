package it.pagopa.selfcare.iam.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import it.pagopa.selfcare.iam.cucumber.CucumberSuiteTest;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class PingStepDefinitions {

  // @Inject
  // SharedStepData sharedStepData;

  //  private static ObjectMapper objectMapper;
  //  static MongoDatabase mongoDatabase;

  private Response response;
  private List<Response> responses = new ArrayList<>();
  private long startTime;

  @BeforeAll
  static void setup() {}

  //   @BeforeAll
  //   static void setup() {
  //     objectMapper = new ObjectMapper();
  //     objectMapper.registerModule(new JavaTimeModule());
  //     Vertx vertx = Vertx.vertx();
  //     vertx
  //       .getOrCreateContext()
  //       .config()
  //       .put("quarkus.vertx.event-loop-blocked-check-interval", 5000);

  //     log.info("Starting test containers...");

  // //    composeContainer = new ComposeContainer(new File("docker-compose.yml"))
  // //            .withLocalCompose(true);
  // //    // .waitingFor("azure-cli", Wait.forLogMessage(".*BLOBSTORAGE INITIALIZED.*\\n", 1));
  // //    composeContainer.start();
  // //    Runtime.getRuntime().addShutdownHook(new Thread(composeContainer::stop));

  //     log.info("Test containers started successfully");

  //     initDb();
  //     log.debug("Init completed");
  //   }

  @When("I ping the IAM service")
  public void iPingTheIAMService() {
    log.info("TOKEN: " + CucumberSuiteTest.tokenTest);
    response =
        given()
            .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
            .when()
            .get("/iam/ping")
            .then()
            .extract()
            .response();
  }

  @When("I send {int} consecutive ping requests")
  public void iSendConsecutivePingRequests(int count) {
    startTime = System.currentTimeMillis();
    responses.clear();

    for (int i = 0; i < count; i++) {
      responses.add(
          given()
              .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
              .when()
              .get("/iam/ping")
              .then()
              .extract()
              .response());
    }
  }

  @Then("I should receive a {int} OK response")
  public void iShouldReceiveAOKResponse(int expectedStatusCode) {
    assertEquals(expectedStatusCode, response.statusCode());
  }

  @Then("the response body should be {string}")
  public void theResponseBodyShouldBe(String expectedBody) {
    assertEquals(expectedBody, response.getBody().asString());
  }

  @Then("all requests should return {int} OK")
  public void allRequestsShouldReturnOK(int expectedStatusCode) {
    for (Response r : responses) {
      assertEquals(expectedStatusCode, r.statusCode());
    }
  }

  @Then("all responses should be received within {int} second")
  public void allResponsesShouldBeReceivedWithinSecond(int maxSeconds) {
    long duration = System.currentTimeMillis() - startTime;
    assertTrue(
        duration < maxSeconds * 1000,
        "Responses took " + duration + "ms, expected < " + (maxSeconds * 1000) + "ms");
  }
}
