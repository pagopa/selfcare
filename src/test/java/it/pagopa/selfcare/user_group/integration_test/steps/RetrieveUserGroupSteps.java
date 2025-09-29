package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.user_group.model.AddMembersToUserGroupDto;
import it.pagopa.selfcare.user_group.model.UserGroupEntity;
import it.pagopa.selfcare.user_group.model.UserGroupStatus;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class RetrieveUserGroupSteps extends UserGroupSteps {

    @Before("@FirstRetrieveGroupScenario")
    public void beforeFeature() throws IOException {
        initializeCollection();
    }

    @After("@LastRetrieveGroupScenario")
    public void afterFeature() {
        userGroupRepository.deleteAllById(userGroupsIds);
    }

    @DataTableType
    public AddMembersToUserGroupDto convertAddMembersRequest(Map<String, String> entry) {
        AddMembersToUserGroupDto request = new AddMembersToUserGroupDto();
        request.setInstitutionId(entry.get("institutionId"));
        request.setParentInstitutionId(entry.get("parentInstitutionId"));
        request.setProductId(entry.get("productId"));

        Set<UUID> members = Optional.ofNullable(entry.get("members"))
                .map(s -> Arrays.stream(s.split(","))
                        .map(UUID::fromString)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        request.setMembers(members);

        return request;
    }


    @Override
    @Then("[RETRIEVE] the response status should be {int}")
    public void verifyResponseStatus(int status) {
        super.verifyResponseStatus(status);
    }

    @Override
    @Then("[RETRIEVE] the response should contain an error message {string}")
    public void verifyErrorMessage(String expectedErrorMessage) {
        super.verifyErrorMessage(expectedErrorMessage);
    }


    @Given("I have a valid group ID to retrieve: {string}")
    public void iHaveAValidGroupId(String validGroupId) {
        userGroupId = validGroupId;
    }

    @Given("I have a non-existent group ID to retrieve {string}")
    public void iHaveANonExistentGroupId(String nonExistentGroupId) {
        userGroupId = nonExistentGroupId;
    }

    @Given("I have a filter with status {string} but no productId, institutionId or userId")
    public void iHaveAFilterWithStatusButNoOr(String status) {
        userGroupEntityFilter = new UserGroupEntity();
        userGroupEntityFilter.setStatus(UserGroupStatus.valueOf(status));
    }

    @Given("I have a filter with sorting by {string} but no filter")
    public void iHaveAFilterWithSortingByButNoFilter(String sortBy) {
        Sort sort = Sort.by(Sort.Order.asc(sortBy));
        pageable = PageRequest.of(0, 10, sort);
    }

    @And("I set the page number to {int} and page size to {int}")
    public void iSetThePageNumberToAndPageSizeTo(int page, int size) {
        pageable = Pageable.ofSize(size).withPage(page);
    }

    @Given("I have no filters")
    public void iHaveNoFilters() {
        userGroupEntityFilter = new UserGroupEntity();
    }

    @When("I send a GET request to {string}")
    public void iSendAGETRequestTo(String url) {
        ResponseOptions<Response> response = RestAssured.given()
                .pathParam("id", userGroupId)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(url);
        status = response.statusCode();
        if (status == 200) {
            userGroupEntityResponse = response.getBody().as(UserGroupEntity.class);
        } else {
            errorMessage = response.getBody().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve userGroups")
    public void iSendAGETRequestToRetrieveUserGroups(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token);

        if (Objects.nonNull(userGroupEntityFilter)) {
            if (Objects.nonNull(userGroupEntityFilter.getProductId())) {
                requestSpecification.queryParam("productId", userGroupEntityFilter.getProductId());
            }
            if (Objects.nonNull(userGroupEntityFilter.getInstitutionId())) {
                requestSpecification.queryParam("institutionId", userGroupEntityFilter.getInstitutionId());
            }
            if (Objects.nonNull(userGroupEntityFilter.getStatus())) {
                requestSpecification.queryParam("status", userGroupEntityFilter.getStatus());
            }
        }

        if (Objects.nonNull(pageable)) {
            requestSpecification.queryParam("size",pageable.getPageSize());
            requestSpecification.queryParam("page",pageable.getPageNumber());
            if(pageable.getSort().isSorted()){
                requestSpecification.queryParam("sort", pageable.getSort().toString());
            }
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        status = response.statusCode();
        if (status == 200) {
            userGroupEntityResponsePage = response.body().as(new TypeRef<>() {
            });
            if (Objects.nonNull(userGroupEntityResponsePage) && !userGroupEntityResponsePage.getContent().isEmpty()) {
                userGroupEntityResponse = userGroupEntityResponsePage.getContent().get(0);
                userGroupId = userGroupEntityResponse.getId();
            }
        } else {
            errorMessage = response.body().asString();
        }
    }

    @Then("the response should contain the group details")
    public void the_response_should_contain_the_group_details() {
        Assertions.assertEquals(userGroupId, userGroupEntityResponse.getId());
        Assertions.assertEquals("io group", userGroupEntityResponse.getName());
        Assertions.assertEquals("io group description", userGroupEntityResponse.getDescription());
        Assertions.assertEquals("9c8ae123-d990-4400-b043-67a60aff31bc", userGroupEntityResponse.getInstitutionId());
        Assertions.assertEquals("prod-test", userGroupEntityResponse.getProductId());
        Assertions.assertEquals("ACTIVE", userGroupEntityResponse.getStatus().name());
        Assertions.assertEquals(1, userGroupEntityResponse.getMembers().size());
        Assertions.assertEquals("75003d64-7b8c-4768-b20c-cf66467d44c7", userGroupEntityResponse.getMembers().iterator().next());
        Assertions.assertNotNull(userGroupEntityResponse.getCreatedAt());
        Assertions.assertEquals("4ba2832d-9c4c-40f3-9126-e1c72905ef14", userGroupEntityResponse.getCreatedBy());
        Assertions.assertNull(userGroupEntityResponse.getModifiedBy());
    }

    @Then("the response should contain the group details with parent institution")
    public void the_response_should_contain_the_group_details_with_parent_institution() {
        Assertions.assertEquals(userGroupId, userGroupEntityResponse.getId());
        Assertions.assertEquals("io group with parent", userGroupEntityResponse.getName());
        Assertions.assertEquals("io group with parent description", userGroupEntityResponse.getDescription());
        Assertions.assertEquals("9c7ae123-d990-4400-b043-67a60aff31bc", userGroupEntityResponse.getInstitutionId());
        Assertions.assertEquals("5d1ae124-d870-4400-b043-67a60aff32cb", userGroupEntityResponse.getParentInstitutionId());
        Assertions.assertEquals("prod-test", userGroupEntityResponse.getProductId());
        Assertions.assertEquals("ACTIVE", userGroupEntityResponse.getStatus().name());
        Assertions.assertEquals(1, userGroupEntityResponse.getMembers().size());
        Assertions.assertEquals("75003d64-7b8c-4768-b20c-cf66467d44c7", userGroupEntityResponse.getMembers().iterator().next());
        Assertions.assertNotNull(userGroupEntityResponse.getCreatedAt());
        Assertions.assertEquals("4ba2832d-9c4c-40f3-9126-e1c72905ef14", userGroupEntityResponse.getCreatedBy());
        Assertions.assertNull(userGroupEntityResponse.getModifiedBy());
    }

    @And("the response should contain a paginated list of user groups of {int} items on page {int}")
    public void theResponseShouldContainAPaginatedListOfUserGroups(int count, int page) {
        Assertions.assertEquals(count, userGroupEntityResponsePage.getContent().size());
        Assertions.assertEquals(4, userGroupEntityResponsePage.getTotalElements());
        Assertions.assertEquals(2, userGroupEntityResponsePage.getTotalPages());
        Assertions.assertEquals(2, userGroupEntityResponsePage.getSize());
        Assertions.assertEquals(page, userGroupEntityResponsePage.getNumber());
    }

    @Then("the response page should contain a group with parent institution id")
    public void the_response_page_should_contain_a_group_with_parent_institution_id() {
        List<UserGroupEntity> groups = userGroupEntityResponsePage.getContent();

        Assertions.assertFalse(groups.isEmpty(), "Expected response page to contain at least one group");

        boolean hasParentInstitutionId = groups.stream()
                .anyMatch(group -> group.getParentInstitutionId() != null);

        Assertions.assertTrue(hasParentInstitutionId,
                "Expected at least one group to have a parentInstitutionId");
    }


    @Given("I have valid filters institutionId {string} productId {string} and status {string}")
    public void iHaveValidFiltersAndAnd(String institutionId, String productId, String status) {
        userGroupEntityFilter = new UserGroupEntity();
        userGroupEntityFilter.setInstitutionId(institutionId);
        userGroupEntityFilter.setProductId(productId);
        userGroupEntityFilter.setStatus(UserGroupStatus.valueOf(status));
    }

    @And("the response should contains groupIds {string}")
    public void theResponseShouldContainGroupIds(String ids) {
        List<String> idsList = Arrays.asList(ids.split(","));
        Assertions.assertEquals(idsList, userGroupEntityResponsePage.getContent().stream().map(UserGroupEntity::getId).toList());
    }

    @And("the response should contain an empty list")
    public void theResponseShouldContainAnEmptyList() {
        Assertions.assertEquals(0, userGroupEntityResponsePage.getContent().size());
    }

    @And("the response should contain {int} item")
    public void theResponseShouldContainOneItem(int expectedItemsCount) {
        Assertions.assertEquals(expectedItemsCount, userGroupEntityResponsePage.getContent().size());
    }

    @Then("the response should contain all members")
    public void the_response_should_contain_all_members() {
        Assertions.assertEquals(3, userGroupEntityResponse.getMembers().size());
        Assertions.assertEquals("[525db33f-967f-4a82-8984-c606225e714a, 75003d64-7b8c-4768-b20c-cf66467d44c7, a1b7c86b-d195-41d8-8291-7c3467abfd30]", userGroupEntityResponse.getMembers().toString());
    }

    @Then("I should receive a response of retrieve user group operation with status code {int}")
    public void iShouldReceiveAResponseWithStatusCode(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, status);
    }

    @Given("[RETRIEVE] user login with username {string} and password {string}")
    public void createUserLoginWithUsernameAndPassword(String user, String pass) {
        super.login(user, pass);
    }
}

