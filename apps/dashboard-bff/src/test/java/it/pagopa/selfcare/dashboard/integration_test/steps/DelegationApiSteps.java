package it.pagopa.selfcare.dashboard.integration_test.steps;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.dashboard.model.delegation.DelegationType.AOO;

public class DelegationApiSteps{

    @Autowired
    private DashboardStepsUtil dashboardStepsUtil;

    @DataTableType
    public DelegationRequestDto convertRequest(Map<String, String> entry) {
        return dashboardStepsUtil.toDelegationRequestDto(entry);
    }


    @And("the following delegation request details:")
    public void theFollowingSupportRequestDetails(List<DelegationRequestDto> delegationRequestDtos) {
        if (delegationRequestDtos != null && delegationRequestDtos.size() == 1)
            dashboardStepsUtil.requests.setDelegationRequestDto(delegationRequestDtos.get(0));
    }

    @When("I send a POST request to {string} to create a delegation")
    public void iSendAPOSTRequestToToCreateADelegation(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)) {
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(dashboardStepsUtil.requests.getDelegationRequestDto())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 201) {
            dashboardStepsUtil.responses.setDelegationIdResource(response.body().as(new TypeRef<>() {}));
        } else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve paginated delegations")
    public void whenISendAGetRequestToRetrievePaginatedDelegations(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)) {
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }
        if(Objects.nonNull(dashboardStepsUtil.filter.getSize())) {
            requestSpecification.queryParam("size", dashboardStepsUtil.filter.getSize());
        }
        if(Objects.nonNull(dashboardStepsUtil.filter.getPage())) {
            requestSpecification.queryParam("page", dashboardStepsUtil.filter.getPage());
        }
        if (Objects.nonNull(dashboardStepsUtil.filter.getProductId())) {
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if (Objects.nonNull(dashboardStepsUtil.filter.getSearch())) {
            requestSpecification.queryParam("search", dashboardStepsUtil.filter.getSearch());
        }
        if (Objects.nonNull(dashboardStepsUtil.filter.getOrder())) {
            requestSpecification.queryParam("order", dashboardStepsUtil.filter.getOrder());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setDelegationWithPagination(response.body().as(new TypeRef<>() {}));
        } else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve delegations")
    public void whenISendAGetRequestToRetrieveDelegations(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)) {
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }
        if (Objects.nonNull(dashboardStepsUtil.filter.getProductId())) {
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setDelegationResource(response.body().as(new TypeRef<>() {}));
        } else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }


    @And("the response should contain a list of delegations for all product")
    public void theResponseShouldContainAListOfDelegationsForALlProduct() {
        Assertions.assertFalse(dashboardStepsUtil.responses.getDelegationResource().isEmpty());
        Assertions.assertEquals(2, dashboardStepsUtil.responses.getDelegationResource().size());
        Assertions.assertEquals("prod-io", dashboardStepsUtil.responses.getDelegationResource().get(0).getProductId());
        Assertions.assertEquals("prod-pagopa", dashboardStepsUtil.responses.getDelegationResource().get(1).getProductId());
        Assertions.assertEquals("c9a50656-f345-4c81-84be-5b2474470544", dashboardStepsUtil.responses.getDelegationResource().get(0).getInstitutionId());
        Assertions.assertEquals("c9a50656-f345-4c81-84be-5b2474470544", dashboardStepsUtil.responses.getDelegationResource().get(1).getInstitutionId());
        Assertions.assertEquals("Comune di Castelbuono", dashboardStepsUtil.responses.getDelegationResource().get(0).getInstitutionName());
        Assertions.assertEquals("Comune di Castelbuono", dashboardStepsUtil.responses.getDelegationResource().get(1).getInstitutionName());
        Assertions.assertEquals("AOO test", dashboardStepsUtil.responses.getDelegationResource().get(0).getBrokerName());
        Assertions.assertEquals("AOO test", dashboardStepsUtil.responses.getDelegationResource().get(1).getBrokerName());
        Assertions.assertEquals("067327d3-bdd6-408d-8655-87e8f1960046", dashboardStepsUtil.responses.getDelegationResource().get(0).getBrokerId());
        Assertions.assertEquals("067327d3-bdd6-408d-8655-87e8f1960046", dashboardStepsUtil.responses.getDelegationResource().get(1).getBrokerId());
        Assertions.assertEquals(AOO, dashboardStepsUtil.responses.getDelegationResource().get(0).getType());
        Assertions.assertEquals(AOO, dashboardStepsUtil.responses.getDelegationResource().get(1).getType());

    }

    @And("the response should contain a list of delegations only for {string}")
    public void theResponseShouldContainAListOfDelegations(String productId) {
        Assertions.assertFalse(dashboardStepsUtil.responses.getDelegationResource().isEmpty());
        Assertions.assertEquals(1, dashboardStepsUtil.responses.getDelegationResource().size());
        Assertions.assertEquals(productId, dashboardStepsUtil.responses.getDelegationResource().get(0).getProductId());
    }

    @And("the response should contain an empty list")
    public void theResponseShouldContainAnEmptyList() {
        Assertions.assertTrue(dashboardStepsUtil.responses.getDelegationResource().isEmpty());
    }

    @And("the response should contain a list of delegations filtered by {string}")
    public void theResponseShouldContainAListOfDelegationsFilteredBy(String search) {

    }

    @And("the response should be ordered by {string}")
    public void theResponseShouldBeOrderedBy(String order) {

    }

    @And("the response contains delegation only for {string}")
    public void theResponseContainsDelegationOnlyFor(String productId) {
        Assertions.assertTrue(dashboardStepsUtil.responses.getDelegationResource().stream()
                .allMatch(delegation -> delegation.getProductId().equals(productId)));
    }

    @And("the response should contain a list of paginated delegations")
    public void theResponseShouldContainAListOfPaginatedDelegations() {
        Assertions.assertFalse(dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().isEmpty());
        Assertions.assertEquals(2, dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().size());
        Assertions.assertEquals(2, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getTotalElements());
        Assertions.assertEquals(1, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getTotalPages());
        Assertions.assertEquals(0, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getPageNo());
        Assertions.assertEquals("prod-io", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getProductId());
        Assertions.assertEquals("prod-pagopa", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getProductId());
        Assertions.assertEquals("c9a50656-f345-4c81-84be-5b2474470544", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getInstitutionId());
        Assertions.assertEquals("c9a50656-f345-4c81-84be-5b2474470544", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getInstitutionId());
        Assertions.assertEquals("Comune di Castelbuono", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getInstitutionName());
        Assertions.assertEquals("Comune di Castelbuono", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getInstitutionName());
        Assertions.assertEquals("AOO test", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getBrokerName());
        Assertions.assertEquals("AOO test", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getBrokerName());
        Assertions.assertEquals("067327d3-bdd6-408d-8655-87e8f1960046", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getBrokerId());
        Assertions.assertEquals("067327d3-bdd6-408d-8655-87e8f1960046", dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getBrokerId());
        Assertions.assertEquals(AOO, dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(0).getType());
        Assertions.assertEquals(AOO, dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().get(1).getType());
    }

    @And("the response should contain a list of paginated delegations filtered by {string}")
    public void theResponseShouldContainAListOfPaginatedDelegationsFilteredBy(String arg0) {

    }

    @And("the paginated response contains delegation only for {string}")
    public void thePaginatedResponseContainsDelegationOnlyFor(String productId) {
        Assertions.assertTrue(dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().stream()
                .allMatch(delegation -> delegation.getProductId().equals(productId)));
    }


    @And("the response should contain a delegation id")
    public void theResponseShouldContainADelegationId() {
        Assertions.assertTrue(StringUtils.isNotBlank(dashboardStepsUtil.responses.getDelegationIdResource().getId()));
    }

    @And("the response should contain an empty list for paginatedApi")
    public void theResponseShouldContainAnEmptyListForPaginatedApi() {
        Assertions.assertEquals(0, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getTotalElements());
        Assertions.assertTrue(dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().isEmpty());
    }

    @And("the response should contain a list of paginated delegations with other pages")
    public void theResponseShouldContainAListOfPaginatedDelegationsWithOtherPages() {
        Assertions.assertEquals(2, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getTotalElements());
        Assertions.assertEquals(1, dashboardStepsUtil.responses.getDelegationWithPagination().getDelegations().size());
        Assertions.assertEquals(2, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getTotalPages());
        Assertions.assertEquals(0, dashboardStepsUtil.responses.getDelegationWithPagination().getPageInfo().getPageNo());
    }
}
