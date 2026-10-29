package it.pagopa.selfcare.dashboard.integration_test.steps;

import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public class DocumentApiSteps {

    @Autowired
    private DashboardStepsUtil dashboardStepsUtil;

    @When("I send a GET request to {string} to get document status")
    public void iSendAGetRequestToGetDocumentStatus(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        dashboardStepsUtil.setHeader(requestSpecification);

        if (Objects.nonNull(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }
        if (Objects.nonNull(dashboardStepsUtil.filter.getProducts())) {
            requestSpecification.queryParam("productIds", dashboardStepsUtil.filter.getProducts());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.setResponse(response);
        } else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

}
