package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CreateUserGroupSteps extends UserGroupSteps {

    @Before("@DuplicateGroupName")
    public void beforeScenarioOfCreateDuplicatedGroup() throws IOException {
        initializeCollection();
    }

    @After("@DuplicateGroupName")
    public void afterScenarioOfCreateDuplicatedGroup() {
        userGroupRepository.deleteAllById(userGroupsIds);
    }

    @After("@CreateNewGroup")
    public void afterScenarioOfCreateGroup() {
        userGroupRepository.deleteById(userGroupId);
    }

    @Override
    @Then("[CREATE] the response status should be {int}")
    public void verifyResponseStatus(int expectedStatusCode) {
        super.verifyResponseStatus(expectedStatusCode);
    }

    @Override
    @Then("[CREATE] the response should contain an error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        super.verifyErrorMessage(expectedErrorMessage);
    }

    @DataTableType
    public UserGroupEntity convertRequest(Map<String, String> entry) {
        UserGroupEntity userGroupEntity = new UserGroupEntity();
        userGroupEntity.setInstitutionId(entry.get("institutionId"));
        userGroupEntity.setParentInstitutionId(entry.get("parentInstitutionId"));
        userGroupEntity.setProductId(entry.get("productId"));
        userGroupEntity.setName(entry.get("name"));
        userGroupEntity.setDescription(entry.get("description"));
        userGroupEntity.setStatus(Optional.ofNullable(entry.get("status")).map(s -> UserGroupStatus.valueOf(entry.get("status"))).orElse(null));
        userGroupEntity.setMembers(Optional.ofNullable(entry.get("members")).map(s -> Set.of(entry.get("members").split(","))).orElse(null));
        return userGroupEntity;
    }

    @When("I send a POST request to {string} with the given details")
    public void iSendAPOSTRequestToWithTheGivenDetails(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(token)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .body(userGroupDetails)
                .when()
                .post(url)
                .then()
                .extract();

        status = response.statusCode();
        if(status == 201) {
            userGroupEntityResponse = response.body().as(UserGroupEntity.class);
            userGroupId = userGroupEntityResponse.getId();
        }else {
            errorMessage = response.body().asString();
        }
    }

    @Given("the following user group details:")
    public void givenUserGroupDetails(List<UserGroupEntity> userGroupEntityList) {
        if (userGroupEntityList != null && userGroupEntityList.size() == 1)
            this.userGroupDetails = userGroupEntityList.get(0);
    }

    @Then("the response should contain a valid user group resource with name {string}")
    public void verifyUserGroupName(String expectedName) {
        Assertions.assertEquals(expectedName, userGroupEntityResponse.getName());
    }

    @Then("the response should contain the productId {string}")
    public void verifyProductId(String expectedProductId) {
        Assertions.assertEquals(expectedProductId, userGroupEntityResponse.getProductId());
    }

    @Then("the response should contain the institutionId {string}")
    public void verifyInstitutionId(String expectedInstitutionId) {
        Assertions.assertEquals(expectedInstitutionId, userGroupEntityResponse.getInstitutionId());
    }

    @Then("the response should contain the parent institutionId {string}")
    public void verifyParentInstitutionId(String expectedParentInstitutionId) {
        Assertions.assertEquals(expectedParentInstitutionId, userGroupEntityResponse.getParentInstitutionId());
    }

    @And("the response should contain the status {string}")
    public void theResponseShouldContainTheStatus(String expectedStatus) {
        Assertions.assertEquals(expectedStatus, userGroupEntityResponse.getStatus().name());
    }

    @And("the response should contain {int} members")
    public void theResponseShouldContainMembers(int expectedMembersCount) {
        Assertions.assertEquals(expectedMembersCount, userGroupEntityResponse.getMembers().size());
    }

    @And("the response should contain the createdBy {string}")
    public void theResponseShouldContainTheCreatedBy(String expectedCreatedBy) {
        Assertions.assertEquals(expectedCreatedBy, userGroupEntityResponse.getCreatedBy());
    }

    @And("the response should contain the createdAt notNull")
    public void theResponseShouldContainTheCreatedAtNotNull() {
        verifyNotNull(userGroupEntityResponse.getCreatedAt());
    }

    @And("the response should contain the modified data null")
    public void theResponseShouldContainTheModifiedDataNull() {
        verifyNull(userGroupEntityResponse.getModifiedAt(), userGroupEntityResponse.getModifiedBy());
    }

    @And("the response should contain the description {string}")
    public void theResponseShouldContainTheDescription(String expectedDescription) {
        Assertions.assertEquals(expectedDescription, userGroupEntityResponse.getDescription());
    }

    @Given("[CREATE] user login with username {string} and password {string}")
    public void createUserLoginWithUsernameAndPassword(String user, String pass) {
        super.login(user, pass);
    }
}

