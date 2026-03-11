package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.user_group.model.AddMembersToUserGroupDto;
import it.pagopa.selfcare.user_group.model.DeleteMembersFromUserGroupDto;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UserGroupMemberSteps extends UserGroupSteps {

    @DataTableType
    public AddMembersToUserGroupDto convertAddMembersRequest(Map<String, String> entry) {
        AddMembersToUserGroupDto request = new AddMembersToUserGroupDto();
        request.setInstitutionId(entry.get("institutionId"));
        request.setParentInstitutionId(entry.get("parentInstitutionId"));
        request.setProductId(entry.get("productId"));
        request.setName(entry.get("name"));
        request.setDescription(entry.get("description"));

        Set<UUID> members = Optional.ofNullable(entry.get("members"))
                .map(s -> Arrays.stream(s.split(","))
                        .map(UUID::fromString)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        request.setMembers(members);

        return request;
    }

    @Before("@FirstGroupMembersScenario")
    public void beforeFeature() throws IOException {
        initializeCollection();
    }

    @After("@LastGroupMembersScenario")
    public void afterFeature() {
        userGroupRepository.deleteAllById(userGroupsIds);
    }

    @Override
    @Then("[MEMBERS] the response status should be {int}")
    public void verifyResponseStatus(int status) {
        super.verifyResponseStatus(status);
    }

    @Override
    @Then("[MEMBERS] the response should contain an error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        super.verifyErrorMessage(expectedErrorMessage);
    }


    @Given("I have group ID {string} and member ID {string}")
    public void iHaveGroupIdMemberId(String groupId, String memberId) {
        userGroupId = groupId;
        userGroupMemberId = UUID.fromString(memberId);
    }

    @Given("I have a missing group ID and a valid member ID {string}")
    public void i_have_a_missing_group_ID_and_a_valid_member_ID(String memberId) {
        userGroupMemberId = UUID.fromString(memberId);
    }

    @Given("I have a member id {string}, institution id {string} and product id {string}")
    public void iHaveAValidMemberIDInstitutionIDAndProductID(String memberId, String institutionId, String productId) {
        userGroupMemberId = UUID.fromString(memberId);
        userGroupEntityFilter = new UserGroupEntity();
        userGroupEntityFilter.setInstitutionId(institutionId);
        userGroupEntityFilter.setProductId(productId);
    }

    @Given("I have a member id {string} and institution id {string}")
    public void iHaveAValidMemberIDAndInstitutionIDAndAMissingProductID(String memberId, String institutionId) {
        userGroupMemberId = UUID.fromString(memberId);
        userGroupEntityFilter = new UserGroupEntity();
        userGroupEntityFilter.setInstitutionId(institutionId);
    }

    @Given("I have a member id {string} and product id {string}")
    public void iHaveAValidMemberIDAndAMissingInstitutionIDAndProductID(String memberId, String productId) {
        userGroupMemberId = UUID.fromString(memberId);
        userGroupEntityFilter = new UserGroupEntity();
        userGroupEntityFilter.setProductId(productId);
    }

    @Given("the following add members to user group request details:")
    public void givenAddMembersToUserGroupRequestDetails(List<AddMembersToUserGroupDto> addMembersToUserGroupDtos) {
        if (addMembersToUserGroupDtos != null && addMembersToUserGroupDtos.size() == 1)
            this.addMembersRequest = addMembersToUserGroupDtos.get(0);
    }

    @Given("the following members to delete from user group request details:")
    public void givenMembersToDeleteFromUserGroupRequestDetails(List<DeleteMembersFromUserGroupDto> deleteMembersFromUserGroupDtos) {
        if (deleteMembersFromUserGroupDtos != null && deleteMembersFromUserGroupDtos.size() == 1)
            this.deleteMembersRequest = deleteMembersFromUserGroupDtos.get(0);
    }

    @When("I send a PUT request to {string}")
    public void iSendAPutRequestTo(String url) {
        ResponseOptions<Response> response = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .pathParam("id", userGroupId)
                .pathParam("memberId", userGroupMemberId)
                .when()
                .put(url);

        status = response.statusCode();
        if (status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @When("I send a PUT request to {string} to add members to a group")
    public void iSendAPUTRequestToAddMembers(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(token)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .body(addMembersRequest)
                .when()
                .put(url)
                .then()
                .extract();

        status = response.statusCode();
        if(status == 201) {
            userGroupEntityResponse = response.body().as(UserGroupEntity.class);
            userGroupId = userGroupEntityResponse.getId();
        } else {
            errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} to delete members from a group")
    public void iSendADELETERequestToDeleteMembers(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(token)){
            requestSpecification.header("Authorization", "Bearer " + token);
        }

        ExtractableResponse<?> response = requestSpecification
                .body(deleteMembersRequest)
                .when()
                .delete(url)
                .then()
                .extract();

        status = response.statusCode();
        if(status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @When("[MEMBERS] I send a DELETE request to {string}")
    public void iSendADELETERequestTo(String url) {
        ResponseOptions<Response> response = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .pathParam("id", userGroupId)
                .pathParam("memberId", userGroupMemberId)
                .when()
                .delete(url);

        status = response.statusCode();
        if (status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} with query parameters")
    public void iSendADELETERequestToWithQueryParameters(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .pathParam("memberId", userGroupMemberId);

        if (userGroupEntityFilter.getInstitutionId() != null) {
            requestSpecification.queryParam("institutionId", userGroupEntityFilter.getInstitutionId());
        }
        if (userGroupEntityFilter.getProductId() != null) {
            requestSpecification.queryParam("productId", userGroupEntityFilter.getProductId());
        }

        ResponseOptions<Response> response = requestSpecification
                .queryParam("institutionId", userGroupEntityFilter.getInstitutionId())
                .queryParam("productId", userGroupEntityFilter.getProductId())
                .when()
                .delete(url);

        status = response.statusCode();
        if (status != 204) {
            errorMessage = response.body().asString();
        }
    }

    @Given("[MEMBERS] user login with username {string} and password {string}")
    public void createUserLoginWithUsernameAndPassword(String user, String pass) {
        super.login(user, pass);
    }
}
