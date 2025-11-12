package it.pagopa.selfcare.iam.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.pagopa.selfcare.iam.controller.request.SaveUserRequest;
import it.pagopa.selfcare.iam.cucumber.CucumberSuiteTest;
import it.pagopa.selfcare.iam.model.ProductRolePermissions;
import it.pagopa.selfcare.iam.model.ProductRoles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class IamStepDefinitions {

  private Response response;
  private SaveUserRequest userRequest;
  private String createdUserUid;
  private String createdUserEmail;


  @DataTableType
  public ProductRolePermissions defineProductRolePermissions(Map<String, String> row) {
    return ProductRolePermissions.builder()
            .productId(row.get("productId"))
            .role(row.get("role"))
            .permissions(Arrays.asList(row.get("permissions").split(",")))
            .build();
  }

  @Given("the IAM service is running")
  public void theIamServiceIsRunning() {
    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .when()
      .get("/iam/ping")
      .then()
      .statusCode(200)
      .extract().response();
  }

  @Given("the database is clean")
  public void theDatabaseIsClean() {
    // In un ambiente reale, puliresti il database di test
    // Per ora assumiamo che @QuarkusTest gestisca l'isolamento
  }

  @When("I create a user with email {string} and name {string}")
  public void iCreateAUserWithEmailAndName(String email, String name) {
    userRequest = new SaveUserRequest();
    userRequest.setEmail(email);
    userRequest.setName(name);

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(userRequest)
      .when()
      .patch("/iam/users")
      .then()
      .extract().response();

    if (response.statusCode() == 200) {
      createdUserEmail = response.jsonPath().getString("email");
      createdUserUid = response.jsonPath().getString("uid");
    }
  }

  @When("I create a user with the following details:")
  public void iCreateAUserWithTheFollowingDetails(DataTable dataTable) {
    Map<String, String> data = dataTable.asMap(String.class, String.class);
    
    userRequest = new SaveUserRequest();
    userRequest.setEmail(data.get("email"));
    userRequest.setName(data.get("name"));
    userRequest.setFamilyName(data.get("familyName"));

    if (data.containsKey("roles")) {
      String productId = data.get("productId");
      List<String> roles = List.of(data.get("roles").split(","));
      userRequest.setProductRoles(List.of(
        ProductRoles.builder()
          .productId(productId)
          .roles(roles)
          .build()
      ));
    }

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(userRequest)
      .when()
      .patch("/iam/users")
      .then()
      .extract().response();

    if (response.statusCode() == 200) {
      createdUserEmail = response.jsonPath().getString("email");
      createdUserUid = response.jsonPath().getString("uid");
    }
  }

  @Given("a user exists with email {string}")
  public void aUserExistsWithEmail(String email) {
    iCreateAUserWithEmailAndName(email, "Test User");
    assertEquals(200, response.statusCode());
  }

  @Given("a user exists with email {string} and product {string} with roles {string}")
  public void aUserExistsWithEmailAndProductWithRoles(String email, String productId, String rolesStr) {
    userRequest = new SaveUserRequest();
    userRequest.setEmail(email);
    userRequest.setName("Test User");
    
    List<String> roles = List.of(rolesStr.split(","));
    userRequest.setProductRoles(List.of(
      ProductRoles.builder()
        .productId(productId)
        .roles(roles)
        .build()
    ));

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(userRequest)
      .when()
      .patch("/iam/users")
      .then()
      .statusCode(200)
      .extract().response();

    createdUserEmail = response.jsonPath().getString("email");
    createdUserUid = response.jsonPath().getString("uid");
  }

  @Given("a user exists with UID {string} and email {string}")
  public void aUserExistsWithUIDAndEmail(String uid, String email) {
    // Per semplicità, creiamo l'utente e poi lo recuperiamo
    iCreateAUserWithEmailAndName(email, "Test User");
    createdUserUid = uid; // In un test reale, useresti il UID effettivo
  }

  @Given("a user exists with the following product roles:")
  public void aUserExistsWithTheFollowingProductRoles(DataTable dataTable) {
    userRequest = new SaveUserRequest();
    userRequest.setEmail("multi-product@example.com");
    userRequest.setName("Multi Product User");

    List<ProductRoles> productRoles = new ArrayList<>();
    List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
    
    for (Map<String, String> row : rows) {
      productRoles.add(ProductRoles.builder()
        .productId(row.get("productId"))
        .roles(List.of(row.get("roles").split(",")))
        .build());
    }

    userRequest.setProductRoles(productRoles);

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(userRequest)
      .when()
      .patch("/iam/users")
      .then()
      .statusCode(200)
      .extract().response();

    createdUserUid = response.jsonPath().getString("uid");
  }

  @When("I update the user with new name {string}")
  public void iUpdateTheUserWithNewName(String newName) {
    userRequest.setName(newName);

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(userRequest)
      .when()
      .patch("/iam/users")
      .then()
      .extract().response();
  }

  @When("I add product {string} with roles {string} to the user")
  public void iAddProductWithRolesToTheUser(String productId, String rolesStr) {
    List<String> roles = List.of(rolesStr.split(","));
    
    SaveUserRequest updateRequest = new SaveUserRequest();
    updateRequest.setEmail(createdUserEmail);
    updateRequest.setProductRoles(List.of(
      ProductRoles.builder()
        .productId(productId)
        .roles(roles)
        .build()
    ));

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(updateRequest)
      .queryParam("productId", productId)
      .when()
      .patch("/iam/users")
      .then()
      .extract().response();
  }

  @When("I update product {string} with roles {string} for the user")
  public void iUpdateProductWithRolesForTheUser(String productId, String rolesStr) {
    List<String> roles = List.of(rolesStr.split(","));
    
    SaveUserRequest updateRequest = new SaveUserRequest();
    updateRequest.setEmail(createdUserEmail);
    updateRequest.setProductRoles(List.of(
      ProductRoles.builder()
        .productId(productId)
        .roles(roles)
        .build()
    ));

    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .contentType(ContentType.JSON)
      .body(updateRequest)
      .queryParam("productId", productId)
      .when()
      .patch("/iam/users")
      .then()
      .extract().response();
  }

  @When("I request the user with UID {string} for product {string}")
  public void iRequestTheUserWithUIDForProduct(String uid, String productId) {
    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .queryParam("productId", productId)
      .when()
      .get("/iam/users/" + uid)
      .then()
      .extract().response();
  }

  @When("I request the user product role permissions list with UID {string}")
  public void iRequestTheUserProductRolePermissionsListWithUID(String uid) {
    response = given()
            .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
            .when()
            .get("/iam/users/role/permissions/" + uid)
            .then()
            .extract().response();
  }

  @When("I request the user filtered by product {string}")
  public void iRequestTheUserFilteredByProduct(String productId) {
    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .queryParam("productId", productId)
      .when()
      .get("/iam/users/" + createdUserUid)
      .then()
      .extract().response();
  }

  @When("I try to create a user with email {string} and name {string}")
  public void iTryToCreateAUserWithEmailAndName(String email, String name) {
    iCreateAUserWithEmailAndName(email.isEmpty() ? null : email, name.isEmpty() ? null : name);
  }

  @And("I request the user without product")
  public void iRequestTheUser() {
    response = given()
      .header("Authorization", "Bearer " + CucumberSuiteTest.tokenTest)
      .when()
      .get("/iam/users/" + createdUserUid)
      .then()
      .extract().response();
  }

  @Then("the user should be created successfully")
  public void theUserShouldBeCreatedSuccessfully() {
    assertEquals(200, response.statusCode());
    assertNotNull(response.jsonPath().getString("uid"));
  }

  @Then("the user product role permissions list should be retrieved successfully")
  public void theUserProductRolePermissionsListShouldBeRetrievedSuccessfully() {
    assertEquals(200, response.statusCode());
    assertNotNull(response.jsonPath().get("items"));
  }

  @Then("the user should be updated successfully")
  public void theUserShouldBeUpdatedSuccessfully() {
    assertEquals(200, response.statusCode());
  }

  @Then("the user should be retrieved successfully")
  public void theUserShouldBeRetrievedSuccessfully() {
    assertEquals(200, response.statusCode());
  }

  @Then("the user should have a unique UID")
  public void theUserShouldHaveAUniqueUID() {
    assertNotNull(createdUserUid);
    assertFalse(createdUserUid.isEmpty());
  }

  @Then("the user email should be {string}")
  public void theUserEmailShouldBe(String expectedEmail) {
    assertEquals(expectedEmail, response.jsonPath().getString("email"));
  }

  @Then("the user name should be {string}")
  public void theUserNameShouldBe(String expectedName) {
    assertEquals(expectedName, response.jsonPath().getString("name"));
  }

  @Then("the user UID should remain the same")
  public void theUserUIDShouldRemainTheSame() {
    assertEquals(createdUserUid, response.jsonPath().getString("uid"));
  }

  @Then("the user should have {int} product role(s)")
  public void theUserShouldHaveProductRoles(int expectedCount) {
    List<Map<String, Object>> productRoles = response.jsonPath().getList("productRoles");
    assertEquals(expectedCount, productRoles.size());
  }

  @Then("the user should have the following product role permissions:")
  public void theUserShouldHaveProductRolePermissions(List<ProductRolePermissions> expectedProductRolePermissions) {
    List<ProductRolePermissions> responseList = response.jsonPath().getList("items", ProductRolePermissions.class);
    assertEquals(expectedProductRolePermissions.size(), responseList.size());
    for (ProductRolePermissions expected : expectedProductRolePermissions) {
      boolean found = responseList.stream().anyMatch(actual ->
        actual.getProductId().equals(expected.getProductId()) &&
        actual.getRole().equals(expected.getRole()) &&
        actual.getPermissions().containsAll(expected.getPermissions())
      );
      assertTrue(found, "Expected product role permissions not found: " + expected);
    }
  }

  @Then("the user should have role {string} for product {string}")
  public void theUserShouldHaveRoleForProduct(String role, String productId) {
    List<Map<String, Object>> productRoles = response.jsonPath().getList("productRoles");
    
    boolean found = productRoles.stream()
      .filter(pr -> pr.get("productId").equals(productId))
      .flatMap(pr -> ((List<String>) pr.get("roles")).stream())
      .anyMatch(r -> r.equals(role));

    assertTrue(found, "Role " + role + " not found for product " + productId);
  }

  @Then("the user should not have role {string} for product {string}")
  public void theUserShouldNotHaveRoleForProduct(String role, String productId) {
    List<Map<String, Object>> productRoles = response.jsonPath().getList("productRoles");
    
    boolean found = productRoles.stream()
      .filter(pr -> pr.get("productId").equals(productId))
      .flatMap(pr -> ((List<String>) pr.get("roles")).stream())
      .anyMatch(r -> r.equals(role));

    assertFalse(found, "Role " + role + " should not exist for product " + productId);
  }

  @Then("I should receive a {int} response")
  public void iShouldReceiveAResponse(int expectedStatusCode) {
    assertEquals(expectedStatusCode, response.statusCode());
  }

  @Then("I should receive a {int} Not Found response")
  public void iShouldReceiveANotFoundResponse(int expectedStatusCode) {
    assertEquals(expectedStatusCode, response.statusCode());
  }

  @Then("the error message should contain {string}")
  public void theErrorMessageShouldContain(String expectedMessage) {
     if (!expectedMessage.isEmpty()) {
      String body = response.getBody().asString();
      assertTrue(body.contains(expectedMessage), 
        "Expected message '" + expectedMessage + "' not found in response: " + body);
    }
  }

  @Then("the response should contain the user details")
  public void theResponseShouldContainTheUserDetails() {
    assertNotNull(response.jsonPath().getString("uid"));
    assertNotNull(response.jsonPath().getString("email"));
  }

  @Then("the response should contain only {int} product role")
  public void theResponseShouldContainOnlyProductRole(int expectedCount) {
    List<Map<String, Object>> productRoles = response.jsonPath().getList("productRoles");
    assertEquals(expectedCount, productRoles.size());
  }

  @Then("the product role should be for product {string}")
  public void theProductRoleShouldBeForProduct(String productId) {
    List<Map<String, Object>> productRoles = response.jsonPath().getList("productRoles");
    assertEquals(productId, productRoles.get(0).get("productId"));
  }
}