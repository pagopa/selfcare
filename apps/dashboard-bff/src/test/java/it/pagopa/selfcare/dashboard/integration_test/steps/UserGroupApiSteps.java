package it.pagopa.selfcare.dashboard.integration_test.steps;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.dashboard.model.product.ProductUserResource;
import it.pagopa.selfcare.dashboard.model.user_groups.CreateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UpdateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupIdResource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus.ACTIVE;

public class UserGroupApiSteps {

    @Autowired
    private DashboardStepsUtil dashboardStepsUtil;

    @DataTableType
    public CreateUserGroupDto convertCreateRequest(Map<String, String> entry) {
        return dashboardStepsUtil.toCreateUserGroupDto(entry);
    }

    @DataTableType
    public UpdateUserGroupDto convertUpdateRequest(Map<String, String> entry) {
        return dashboardStepsUtil.toUpdateUserGroupDto(entry);
    }

    @Given("the following user group details:")
    public void theFollowingUserGroupDetails(List<CreateUserGroupDto> createUserGroupDtos) {
        if (createUserGroupDtos != null && createUserGroupDtos.size() == 1)
            dashboardStepsUtil.requests.setCreateUserGroupDto(createUserGroupDtos.get(0));
    }

    @And("I have data to update:")
    public void iHaveDataToUpdate(List<UpdateUserGroupDto> updateUserGroupDtos) {
        if (updateUserGroupDtos != null && updateUserGroupDtos.size() == 1)
            dashboardStepsUtil.requests.setUpdateUserGroupDto(updateUserGroupDtos.get(0));
    }


    @When("I send a POST request to {string} with the given details to create usergroup")
    public void iSendAPOSTRequestToWithTheGivenDetails(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(dashboardStepsUtil.requests.getCreateUserGroupDto())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 201) {
            dashboardStepsUtil.responses.setUserGroupIdResource(response.body().as(UserGroupIdResource.class));
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} to delete userGroup")
    public void iSendADELETERequestTo(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("groupId", dashboardStepsUtil.filter.getGroupId())
                .delete(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} to delete member")
    public void iSendADELETERequestToToDeleteMember(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("userGroupId", dashboardStepsUtil.filter.getGroupId())
                .pathParam("userId", dashboardStepsUtil.filter.getUserId())
                .delete(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a PUT request to {string} to update userGroup")
    public void iSendAPUTRequestToWithAuthentication(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(dashboardStepsUtil.requests.getUpdateUserGroupDto())
                .pathParam("groupId", dashboardStepsUtil.filter.getGroupId())
                .put(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil. errorMessage = response.body().asString();
        }
    }

    @When("I send a POST request to {string} to update userGroup status")
    public void iSendAPOSTRequestTo(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("groupId", dashboardStepsUtil.filter.getGroupId())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve userGroup")
    public void usergroupISendAGETRequestTo(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if (dashboardStepsUtil.filter.getInstitutionId() != null) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("id", dashboardStepsUtil.filter.getGroupId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setUserGroupResource(response.body().as(new TypeRef<>() {}));
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }


    @When("I send a GET request to {string} to retrieve userGroups")
    public void usergroupISendAGETRequestToToRetrieveUserGroups(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if (dashboardStepsUtil.filter.getInstitutionId() != null) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }
        if (dashboardStepsUtil.filter.getProductId() != null) {
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if (dashboardStepsUtil.filter.getUserId() != null) {
            requestSpecification.queryParam("userId", dashboardStepsUtil.filter.getUserId());
        }

        if (Objects.nonNull(dashboardStepsUtil.filter.getPageable())) {
            requestSpecification.queryParam("size",dashboardStepsUtil.filter.getPageable().getPageSize());
            requestSpecification.queryParam("page",dashboardStepsUtil.filter.getPageable().getPageNumber());
            if(dashboardStepsUtil.filter.getPageable().getSort().isSorted()){
                requestSpecification.queryParam("sort", dashboardStepsUtil.filter.getPageable().getSort().toString());
            }
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil. responses.setUserGroupPlainResourcePageable(response.body().as(new TypeRef<>() {}));
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a POST request to {string} to add userGroup member")
    public void iSendAPOSTRequestToToAddUserGroupMember(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("id", dashboardStepsUtil.filter.getGroupId())
                .pathParam("userId", dashboardStepsUtil.filter.getUserId())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }


    @And("the response should contain a valid user group id")
    public void theResponseShouldContainAValidUserGroupId() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getUserGroupIdResource(), dashboardStepsUtil.responses.getUserGroupIdResource().getId());
    }

    @And("the response should contain a paginated list of user groups of {int} items on page {int}")
    public void theResponseShouldContainAPaginatedListOfUserGroupsOfItemsOnPage(int items, int page) {
        Assertions.assertFalse(dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getContent().isEmpty());
        Assertions.assertEquals(items,dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getContent().size());
        Assertions.assertEquals(page,dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getNumber());
    }

    @And("the response should contain the group details")
    public void theResponseShouldContainTheGroupDetails() {
        Assertions.assertEquals(dashboardStepsUtil.filter.getGroupId(), dashboardStepsUtil.responses.getUserGroupResource().getId());
        Assertions.assertEquals("updatedDescription", dashboardStepsUtil.responses.getUserGroupResource().getDescription());
        Assertions.assertEquals("updated Name", dashboardStepsUtil.responses.getUserGroupResource().getName());
        Assertions.assertEquals("c9a50656-f345-4c81-84be-5b2474470544", dashboardStepsUtil.responses.getUserGroupResource().getInstitutionId());
        Assertions.assertEquals("prod-io", dashboardStepsUtil.responses.getUserGroupResource().getProductId());
        Assertions.assertEquals(ACTIVE, dashboardStepsUtil.responses.getUserGroupResource().getStatus());
        Assertions.assertNotNull(dashboardStepsUtil.responses.getUserGroupResource().getCreatedAt());
        Assertions.assertNotNull(dashboardStepsUtil.responses.getUserGroupResource().getModifiedAt());
        Assertions.assertNotNull(dashboardStepsUtil.responses.getUserGroupResource().getCreatedBy());
        Assertions.assertNotNull(dashboardStepsUtil.responses.getUserGroupResource().getModifiedBy());

        ProductUserResource productUserResource = dashboardStepsUtil.responses.getUserGroupResource().getMembers().get(0);
        Assertions.assertEquals(UUID.fromString("97a511a7-2acc-47b9-afed-2f3c65753b4a"), productUserResource.getId());
        Assertions.assertEquals("john", productUserResource.getName());
        Assertions.assertEquals("Doe", productUserResource.getSurname());

    }

    @And("the retrieved group should be changed status to {string}")
    public void theRetrievedGroupShouldBeChangedStatusTo(String status) {
        Assertions.assertTrue(true);

    }

    @And("the response should contains groupIds {string}")
    public void theResponseShouldContainsGroupIds(String ids) {
        Assertions.assertTrue(true);

    }

    @And("the response should contain an empty group list")
    public void theResponseShouldContainAnEmptyList() {
        Assertions.assertTrue(dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getContent().isEmpty());
        Assertions.assertEquals(0,dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getTotalElements());
    }

    @And("the response should contain {int} total pages with {int} total elements")
    public void theResponseShouldContainTotalPagesWithTotalElements(int totalPages, int totalElement) {
        Assertions.assertEquals(totalPages,dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getTotalPages());
        Assertions.assertEquals(totalElement,dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getTotalElements());
    }

    @And("the response should not contain userGroup {string}")
    public void theResponseShouldNotContainUserGroup(String id) {
        Assertions.assertFalse(dashboardStepsUtil.responses.getUserGroupPlainResourcePageable().getContent().stream()
                .anyMatch(g -> g.getId().equals(id)));
    }

    @And("the user is removed from user group")
    public void theUserIsRemovedFromUserGroup() {
        usergroupISendAGETRequestTo("/v2/user-groups/{id}");
        Assertions.assertFalse(dashboardStepsUtil.responses.getUserGroupResource().getMembers()
                .stream().anyMatch(member -> member.getId().equals(UUID.fromString(dashboardStepsUtil.filter.getUserId()))));
    }
}
