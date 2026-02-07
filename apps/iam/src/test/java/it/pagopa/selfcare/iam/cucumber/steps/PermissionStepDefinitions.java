package it.pagopa.selfcare.iam.cucumber.steps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import it.pagopa.selfcare.iam.cucumber.CucumberSuiteTest;
import java.util.List;

public class PermissionStepDefinitions {

  private Response response;
  private String currentUserId;

  @Given("a user with UID {string} has the following permissions for product {string}:")
  public void aUserWithUIDHasTheFollowingPermissionsForProduct(
      String userId, String productId, DataTable dataTable) {
    this.currentUserId = userId;
    List<String> permissions = dataTable.asList(String.class);

    // In un test reale, prepareresti i dati nel database
    // Per ora assumiamo che il mock del repository gestisca questo
  }

  @Given("a user with UID {string} has no permissions for product {string}")
  public void aUserWithUIDHasNoPermissionsForProduct(String userId, String productId) {
    this.currentUserId = userId;
    // Setup per utente senza permessi
  }

  @Given("a user with UID {string} has permissions for product {string} and institution {string}")
  public void aUserWithUIDHasPermissionsForProductAndInstitution(
      String userId, String productId, String institutionId) {
    this.currentUserId = userId;
    // Setup per utente con permessi per istituzione specifica
  }

  @When("I check if user {string} has permission {string} for product {string}")
  public void iCheckIfUserHasPermissionForProduct(
      String userId, String permission, String productId) {
    response =
        given()
            .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
            .queryParam("productId", productId)
            .when()
            .get("/iam/users/" + userId + "/permissions/" + permission)
            .then()
            .extract()
            .response();
  }

  @When(
      "I check if user {string} has permission {string} for product {string} and institution {string}")
  public void iCheckIfUserHasPermissionForProductAndInstitution(
      String userId, String permission, String productId, String institutionId) {
    response =
        given()
            .queryParam("productId", productId)
            .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
            .queryParam("institutionId", institutionId)
            .when()
            .get("/iam/users/" + userId + "/permissions/" + permission)
            .then()
            .extract()
            .response();
  }

  @Then("the permission check should return {word}")
  public void thePermissionCheckShouldReturn(String expectedResult) {
    assertEquals(200, response.statusCode());
    boolean expected = Boolean.parseBoolean(expectedResult);
    boolean actual = response.getBody().as(Boolean.class);
    assertEquals(expected, actual);
  }
}
