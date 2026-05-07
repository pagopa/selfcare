package it.pagopa.selfcare.dashboard.integration_test.steps;

import io.cucumber.java.DataTableType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.dashboard.model.support.SupportRequestDto;
import it.pagopa.selfcare.dashboard.model.support.SupportResponse;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;


public class SupportApiSteps{

    @Autowired
    DashboardStepsUtil dashboardStepsUtil;

    @DataTableType
    public SupportRequestDto convertRequest(Map<String, String> entry) {
        return dashboardStepsUtil.toSupportRequestDto(entry);
    }

    @And("the following support request details:")
    public void theFollowingSupportRequestDetails(List<SupportRequestDto> supportRequests) {
        if (supportRequests != null && supportRequests.size() == 1)
            dashboardStepsUtil.requests.setSupportRequestDto(supportRequests.get(0));
    }

    @When("I send a POST request to {string} with the given details to send a support request")
    public void whenISendAPOSTRequestToWithTheGivenDetailsWithAuthentication(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(dashboardStepsUtil.requests.getSupportRequestDto())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setSupportResponse(response.body().as(SupportResponse.class));
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @And("the response should contain an actionURL")
    public void theResponseShouldContainAnActionURL() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getActionUrl());

    }

    @And("the response should contain a JWT token")
    public void theResponseShouldContainAJWTToken(){
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getJwt());
    }

    @And("the response should contain a redirect URL with institutionId and productId")
    public void theResponseShouldContainARedirectURLWithInstitutionIdAndProductId() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains(dashboardStepsUtil.requests.getSupportRequestDto().getInstitutionId()));
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains(dashboardStepsUtil.requests.getSupportRequestDto().getProductId()));
    }

    @And("the response should contain a redirect URL without productId but with institutionId")
    public void theResponseShouldContainARedirectURLWithoutProductId() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains(dashboardStepsUtil.requests.getSupportRequestDto().getInstitutionId()));
        Assertions.assertFalse(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("product"));
    }

    @And("the response should contain a redirect URL without institutionId but with productId")
    public void theResponseShouldContainARedirectURLWithoutInstitutionId() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains(dashboardStepsUtil.requests.getSupportRequestDto().getProductId()));
        Assertions.assertFalse(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("institutionId"));
    }

    @And("the response should contain a redirect URL without productId and institutionId")
    public void theResponseShouldContainARedirectURLWithoutProductIdAndInstitutionId() {
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertFalse(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("institutionId"));
        Assertions.assertFalse(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("product"));
    }

    @And("the response should contain a redirect URL with productId and data")
    public void theResponseShouldContainARedirectURLWithProductIdAndData() {
        final String expectedProductId = dashboardStepsUtil.requests.getSupportRequestDto().getProductId();
        final String expectedData = dashboardStepsUtil.requests.getSupportRequestDto().getData();
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("?product=" + expectedProductId));
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("&data=" + expectedData));
    }

    @And("the response should contain a redirect URL with data")
    public void theResponseShouldContainARedirectURLWithData() {
        final String expectedData = dashboardStepsUtil.requests.getSupportRequestDto().getData();
        Assertions.assertNotNull(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl());
        Assertions.assertTrue(dashboardStepsUtil.responses.getSupportResponse().getRedirectUrl().contains("?data=" + expectedData));
    }
}
