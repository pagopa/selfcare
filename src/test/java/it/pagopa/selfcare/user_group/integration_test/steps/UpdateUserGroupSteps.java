package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class UpdateUserGroupSteps extends UserGroupSteps {

    @Before("@FirstUpdateScenario")
    public void beforeFeature() throws IOException {
        List<UserGroupEntity> groupsToInsert = objectMapper.readValue(new File("src/test/resources/dataPopulation/groupEntities.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, UserGroupEntity.class));
        userGroupRepository.insert(groupsToInsert);
    }

    @After("@LastUpdateScenario")
    public void afterFeature() {
        userGroupRepository.deleteAllById(userGroupsIds);
    }

    @Override
    @Then("[UPDATE] the response status should be {int}")
    public void verifyResponseStatus(int status) {
        super.verifyResponseStatus(status);
    }

    @Override
    @Then("[UPDATE] the response should contain an error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        super.verifyErrorMessage(expectedErrorMessage);
    }

    @Given("I have a valid group ID to update: {string}")
    public void iHaveAValidGroupIDToUpdate(String groupId) {
        userGroupId = groupId;
    }

    @Given("I have a non-existent group ID {string}")
    public void i_have_a_non_existent_group_ID(String nonExistentGroupId) {
        userGroupId = nonExistentGroupId;
    }

    @And("I have data to update:")
    public void iHaveDataToUpdate(List<UserGroupEntity> userGroupEntityList) {
        if (userGroupEntityList != null && userGroupEntityList.size() == 1)
            this.userGroupDetails = userGroupEntityList.get(0);
    }

    @When("I send a POST request to {string} with authentication {string}")
    public void iSendAPOSTRequestTo(String url, String isAuthenticated) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(Boolean.parseBoolean(isAuthenticated)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .pathParam("groupId", userGroupId)
                .when()
                .post(url)
                .then()
                .extract();

        this.status = response.statusCode();
        if(status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @When("I send a PUT request to {string} with authentication {string}")
    public void iSendAPUTRequestTo(String url, String isAuthenticated) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(Boolean.parseBoolean(isAuthenticated)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .pathParam("groupId", userGroupId)
                .when()
                .body(userGroupDetails)
                .put(url)
                .then()
                .extract();

        this.status = response.statusCode();
        if(status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} with authentication {string}")
    public void iSendADeleteRequestTo(String url, String isAuthenticated) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(Boolean.parseBoolean(isAuthenticated)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .pathParam("groupId", userGroupId)
                .when()
                .delete(url)
                .then()
                .extract();

        this.status = response.statusCode();
        if(status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @And("the retrieved group should be updated")
    public void theRetrievedGroupShouldBeUpdated() {
        ExtractableResponse<?> response = RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user-groups/" + userGroupId)
                .then()
                .extract();

        status = response.statusCode();
        if (status == 200) {
            updatedUserGroupEntity = response.body().as(new TypeRef<>() {
            });
            Assertions.assertNotEquals("io group", updatedUserGroupEntity.getName());
            Assertions.assertNotEquals("io group description", updatedUserGroupEntity.getDescription());
            Assertions.assertNotEquals(Set.of("75003d64-7b8c-4768-b20c-cf66467d44c7"), updatedUserGroupEntity.getMembers());
            Assertions.assertNotNull(updatedUserGroupEntity.getModifiedAt());
            Assertions.assertNotNull(updatedUserGroupEntity.getModifiedBy());
        }
    }

    @And("the retrieved group should be changed status to {string}")
    public void theRetrievedGroupShouldBeChangedStatusTo(String changedStatus) {
        ExtractableResponse<?> response = RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/v1/user-groups/" + userGroupId)
                .then()
                .extract();

        status = response.statusCode();
        if (status == 200) {
            updatedUserGroupEntity = response.body().as(new TypeRef<>() {
            });
            Assertions.assertEquals(changedStatus, updatedUserGroupEntity.getStatus().name());
            Assertions.assertNotNull(updatedUserGroupEntity.getModifiedAt());
            Assertions.assertNotNull(updatedUserGroupEntity.getModifiedBy());
        }
    }

}
