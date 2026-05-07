package it.pagopa.selfcare.dashboard.integration_test.steps;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import it.pagopa.selfcare.dashboard.model.product.BrokerResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleMappingsResource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProductApiSteps{

    @Autowired
    private DashboardStepsUtil dashboardStepsUtil;

    @When("I send a GET request to {string} to retrieve productRoles")
    public void iSendAGETRequestToToRetrieveProductRoles(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }
        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionType())){
            requestSpecification.queryParam("institutionType", dashboardStepsUtil.filter.getInstitutionType());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("productId", dashboardStepsUtil.filter.getProductId())

                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setProductRoleMappingsResource(response.body().as(new TypeRef<>() {}));
        }else {
            dashboardStepsUtil. errorMessage = response.body().asString();
        }

    }

    @When("I send a GET request to {string} to retrieve product brokers")
    public void iSendAGETRequestToToRetrieveProductBrokes(String url) {
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
                .pathParam("productId", dashboardStepsUtil.filter.getProductId())
                .pathParam("institutionType", dashboardStepsUtil.filter.getInstitutionType())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setBrokerResource(response.body().as(new TypeRef<>() {}));
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve products tree")
    public void iSendAGETRequestToToRetrieveProductsTree(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }


        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
    }

    @When("I send a GET request to {string} to retrieve back-office URL")
    public void iSendAGETRequestToToRetrieveBackOfficeURL(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)) {
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }
        if(Objects.nonNull(dashboardStepsUtil.filter.getLang())) {
            requestSpecification.queryParam("lang", dashboardStepsUtil.filter.getLang());
        }
        if(Objects.nonNull(dashboardStepsUtil.filter.getEnvironment())) {
            requestSpecification.queryParam("environment", dashboardStepsUtil.filter.getEnvironment());
        }
        if(Objects.nonNull(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("productId", dashboardStepsUtil.filter.getProductId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.responses.setBackOfficeUrl(response.body().as(new TypeRef<>() {}));
        } else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve my permissions")
    public void iSendAGETRequestToToRetrieveMyPermissions(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200) {
            dashboardStepsUtil.setResponse(response);
        }else {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @And("the response should not contain any product brokers")
    public void theResponseShouldNotContainAnyProductBrokers() {
        List<BrokerResource> brokers = dashboardStepsUtil.responses.getBrokerResource();
        Assertions.assertTrue(brokers.isEmpty());
    }

    @And("the response should contain a list of pagopa product brokers")
    public void theResponseShouldContainAListOfPagoPaProductBrokers() {
        List<BrokerResource> brokers = dashboardStepsUtil.responses.getBrokerResource();
        Assertions.assertFalse(brokers.isEmpty());
        Assertions.assertEquals("223344556677889911", brokers.get(0).getCode());
    }

    @And("the response should contain a list of product brokers")
    public void theResponseShouldContainAListOfProductBrokers() {
        List<BrokerResource> brokers = dashboardStepsUtil.responses.getBrokerResource();
        Assertions.assertFalse(brokers.isEmpty());
        Assertions.assertEquals("15555555555", brokers.get(0).getCode());
    }

    @And("the response should contain a list of pagopa psp product brokers")
    public void theResponseShouldContainAListOfPspProductBrokers() {
        List<BrokerResource> brokers = dashboardStepsUtil.responses.getBrokerResource();
        Assertions.assertFalse(brokers.isEmpty());
        Assertions.assertEquals("223344556677889900", brokers.get(0).getCode());
    }

    @And("the response should contain a list of product roles")
    public void theResponseShouldContainAListOfProductRoles() {
       List<ProductRoleMappingsResource> roles = dashboardStepsUtil.responses.getProductRoleMappingsResource();
        Assertions.assertFalse(roles.isEmpty());
        Assertions.assertEquals(5, roles.size());
        Assertions.assertTrue(roles.stream().anyMatch(role -> role.getPartyRole().equals("MANAGER") && role.getSelcRole().equals(SelfCareAuthority.ADMIN)));
        Assertions.assertTrue(roles.stream().anyMatch(role -> role.getPartyRole().equals("DELEGATE") && role.getSelcRole().equals(SelfCareAuthority.ADMIN)));
        Assertions.assertTrue(roles.stream().anyMatch(role -> role.getPartyRole().equals("SUB_DELEGATE") && role.getSelcRole().equals(SelfCareAuthority.ADMIN)));
        Assertions.assertTrue(roles.stream().anyMatch(role -> role.getPartyRole().equals("ADMIN_EA") && role.getSelcRole().equals(SelfCareAuthority.ADMIN)));
        Assertions.assertTrue(roles.stream().anyMatch(role -> role.getPartyRole().equals("OPERATOR") && role.getSelcRole().equals(SelfCareAuthority.LIMITED)));
    }

    @And("the response should contain a back-office URL with selfcare token")
    public void theResponseShouldContainABackOfficeURLWithSelfcareToken() {
        String uri = dashboardStepsUtil.responses.getBackOfficeUrl().toString();
        String token = uri.split("#selfCareToken=")[1].split("&")[0];
        Map<String, Claim> payload = JWT.decode(token).getClaims();
        Assertions.assertEquals("Doe",payload.get("family_name").asString());
        Assertions.assertEquals("John",payload.get("name").asString());
        Assertions.assertEquals("PRVTNT80A41H401T",payload.get("fiscal_number").asString());
        Assertions.assertEquals("97a511a7-2acc-47b9-afed-2f3c65753b4a",payload.get("uid").asString());
    }

    @And("the response should not contain any product roles")
    public void theResponseShouldNotContainAnyProductRoles() {

    }

    @And("the response should contains {string} as lang query param")
    public void theResponseShouldContainsAsLangQueryParam(String lang) {
        String uri = dashboardStepsUtil.responses.getBackOfficeUrl().toString();
        Assertions.assertTrue(uri.contains("lang=" + lang));
    }

    @And("the response should contains {string} as productRole code")
    public void theResponseShouldContainsAsProductRole(String productRoleCode) {
        ProductRoleMappingsResource productRoleMappingsResource = dashboardStepsUtil.responses.getProductRoleMappingsResource()
                .stream().filter(resource -> resource.getPartyRole().equals("OPERATOR"))
                .findFirst()
                .orElse(null);

        assert productRoleMappingsResource != null;
        Assertions.assertTrue(productRoleCode.equalsIgnoreCase(productRoleMappingsResource.getProductRoles().get(0).getCode()));
    }
}
