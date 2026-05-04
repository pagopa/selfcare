package it.pagopa.selfcare.dashboard.integration_test.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.specification.RequestSpecification;
import it.pagopa.selfcare.dashboard.model.SearchUserDto;
import it.pagopa.selfcare.dashboard.model.UpdateUserDto;
import it.pagopa.selfcare.dashboard.model.product.ProductInfoResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleInfoResource;
import it.pagopa.selfcare.dashboard.model.user.CheckAttachmentResponse;
import it.pagopa.selfcare.dashboard.model.user.CheckUserResponse;
import it.pagopa.selfcare.dashboard.model.user.UserResource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserApiSteps{

    @Autowired
    private DashboardStepsUtil dashboardStepsUtil;

    @Autowired
    private InstitutionApiSteps institutionApiSteps;

    @When("I send a POST request to {string} to update user status")
    public void iSendAPOSTRequestTo(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getProductId())){
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getProductRoles())){
            requestSpecification.queryParam("productRole", dashboardStepsUtil.filter.getProductRoles().get(0));
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("userId", dashboardStepsUtil.filter.getUserId())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a DELETE request to {string} to delete user product")
    public void iSendADELETERequestToToDeleteUserProduct(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getProductId())){
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getProductRoles())){
            requestSpecification.queryParam("productRole", dashboardStepsUtil.filter.getProductRoles().get(0));
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("userId", dashboardStepsUtil.filter.getUserId())
                .delete(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve user data")
    public void iSendAGETRequestToToRetrieveUserData(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }
        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getFields())){
            requestSpecification.queryParam("fields", dashboardStepsUtil.filter.getFields());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("id", dashboardStepsUtil.filter.getUserId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.responses.setUserResource(response.as(UserResource.class));
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve user product data")
    public void iSendAGETRequestToToRetrieveUserProductData(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getProductId())){
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getRoles())){
            requestSpecification.queryParam("roles", dashboardStepsUtil.filter.getRoles());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getProductRoles())){
            requestSpecification.queryParam("productRoles", dashboardStepsUtil.filter.getProductRoles());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.responses.setProductUserResource(response.as(new TypeRef<>() {}));
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }

    }

    @When("I send a GET request to {string} to retrieve all user product data")
    public void iSendAGETRequestToToRetrieveAllUserProductData(String url) {
        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getProductId())){
            requestSpecification.queryParam("productId", dashboardStepsUtil.filter.getProductId());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getRoles())){
            requestSpecification.queryParam("roles", dashboardStepsUtil.filter.getRoles());
        }
        if(!CollectionUtils.isEmpty(dashboardStepsUtil.filter.getStates())){
            requestSpecification.queryParam("states", dashboardStepsUtil.filter.getStates());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.setResponse(response);
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }

    }

    @When("I send a POST request to {string} to retrieve user data from taxCode")
    public void iSendAPOSTRequestToToRetrieveUserDataFromTaxCode(String url) {

        SearchUserDto searchUserDto = new SearchUserDto();

        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getTaxCode())){
            searchUserDto.setFiscalCode(dashboardStepsUtil.filter.getTaxCode());
        }
        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(searchUserDto)
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.responses.setUserResource(response.as(UserResource.class));
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a PUT request to {string} to update user data")
    public void iSendAPUTRequestTo(String url) {
        UpdateUserDto updateUserDto = new UpdateUserDto();

        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getEmail())){
            updateUserDto.setEmail(dashboardStepsUtil.filter.getEmail());
        }
        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getMobilePhone())){
            updateUserDto.setMobilePhone(dashboardStepsUtil.filter.getMobilePhone());
        }

        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getInstitutionId())) {
            requestSpecification.queryParam("institutionId", dashboardStepsUtil.filter.getInstitutionId());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("id", dashboardStepsUtil.filter.getUserId())
                .body(updateUserDto)
                .put(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status != 204) {
            dashboardStepsUtil.errorMessage = response.body().asString();
        }

    }

    @When("I send a POST request to {string} to check user from taxCode")
    public void iSendAPOSTRequestToCheckUserFromTaxCode(String url) {

        SearchUserDto searchUserDto = new SearchUserDto();

        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if(StringUtils.isNotBlank(dashboardStepsUtil.filter.getTaxCode())){
            searchUserDto.setFiscalCode(dashboardStepsUtil.filter.getTaxCode());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .body(searchUserDto)
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .pathParam("productId", dashboardStepsUtil.filter.getProductId())
                .post(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.responses.setCheckUserResponse(response.as(CheckUserResponse.class));
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to check attachment status from attachment name")
    public void iSendAGETRequestToCheckAttachmentStatus(String url) {


        RequestSpecification requestSpecification = RestAssured.given()
                .contentType("application/json");

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        if (StringUtils.isNotBlank(dashboardStepsUtil.filter.getName())) {
            requestSpecification.queryParam("name", dashboardStepsUtil.filter.getName());
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .pathParam("institutionId", dashboardStepsUtil.filter.getInstitutionId())
                .pathParam("productId", dashboardStepsUtil.filter.getProductId())
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if(dashboardStepsUtil.status == 200){
            dashboardStepsUtil.responses.setCheckAttachmentResponse(response.as(CheckAttachmentResponse.class));
        }else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @When("I send a GET request to {string} to retrieve user otp info")
    public void iSendAGETRequestToToRetrieveUserOtpInfo(String url) {
        RequestSpecification requestSpecification = RestAssured.given();

        if(StringUtils.isNotBlank(dashboardStepsUtil.token)){
            requestSpecification.header("Authorization", "Bearer " + dashboardStepsUtil.token);
        }

        ExtractableResponse<?> response = requestSpecification
                .when()
                .get(url)
                .then()
                .extract();

        dashboardStepsUtil.status = response.statusCode();
        if (dashboardStepsUtil.status == 200){
            dashboardStepsUtil.response = response;
        } else{
            dashboardStepsUtil.errorMessage = response.body().asString();
        }
    }

    @And("the user product should be {string} only on filtered product roles")
    public void theUserShouldBeSuspendedOnlyOnFilteredProductRoles(String status) {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getProducts().stream()
                .filter(p ->
                        p.getId().equals(dashboardStepsUtil.filter.getProductId()))
                .toList().get(0)
                .getRoleInfos().stream()
                .filter(r ->
                        r.getRole().equals(dashboardStepsUtil.filter.getProductRoles().get(0)))
                .forEach( roleInfo -> assertEquals(status, roleInfo.getStatus()));
    }

    @And("the user product should be {string}")
    public void theUserShouldBeSuspended(String status) {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getProducts().stream()
                .filter(p ->
                        p.getId().equals(dashboardStepsUtil.filter.getProductId()))
                .toList().get(0)
                .getRoleInfos()
                .forEach( roleInfo -> assertEquals(status, roleInfo.getStatus()));
    }

    @And("the response should contain the user data")
    public void theResponseShouldContainTheUserData() {
        assertEquals(UUID.fromString("97a511a7-2acc-47b9-afed-2f3c65753b4a"),dashboardStepsUtil.responses.getUserResource().getId());
        assertEquals("john",dashboardStepsUtil.responses.getUserResource().getName().getValue());
        assertEquals("Doe",dashboardStepsUtil.responses.getUserResource().getFamilyName().getValue());
        assertNotEquals("personal@test.it",dashboardStepsUtil.responses.getUserResource().getEmail().getValue());
        assertEquals("3365252525",dashboardStepsUtil.responses.getUserResource().getMobilePhone().getValue());
    }

    @And("the response should contain only name and familyName of user")
    public void theResponseShouldContainOnlyNameAndFamilyNameOfUser() {
        assertEquals(UUID.fromString("97a511a7-2acc-47b9-afed-2f3c65753b4a"),dashboardStepsUtil.responses.getUserResource().getId());
        assertEquals("john",dashboardStepsUtil.responses.getUserResource().getName().getValue());
        assertNull(dashboardStepsUtil.responses.getUserResource().getFiscalCode());
        assertEquals("Doe", dashboardStepsUtil.responses.getUserResource().getFamilyName().getValue());
        assertNull(dashboardStepsUtil.responses.getUserResource().getEmail());
        assertNull(dashboardStepsUtil.responses.getUserResource().getMobilePhone());
    }

    @And("the response should contain the user data with  name, familyName, email, workContacts fields")
    public void theResponseShouldContainTheUserDataWithNameFamilyNameEmailWorkContactsFields() {
        assertEquals(UUID.fromString("97a511a7-2acc-47b9-afed-2f3c65753b4a"),dashboardStepsUtil.responses.getUserResource().getId());
        assertEquals("john",dashboardStepsUtil.responses.getUserResource().getName().getValue());
        assertEquals("Doe",dashboardStepsUtil.responses.getUserResource().getFamilyName().getValue());
        assertNull(dashboardStepsUtil.responses.getUserResource().getFiscalCode());
        assertNotEquals("personal@test.it",dashboardStepsUtil.responses.getUserResource().getEmail().getValue());
        assertEquals("3365252525",dashboardStepsUtil.responses.getUserResource().getMobilePhone().getValue());
    }

    @And("the response should contain {int} items")
    public void theResponseShouldContainItems(int items) {
        assertEquals(items, dashboardStepsUtil.responses.getProductUserResource().size());
    }

    @And("the user email should be updated")
    public void theUserEmailShouldBeUpdated() {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        assertEquals(dashboardStepsUtil.filter.getEmail(), dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getEmail());
    }

    @And("the user mobilePhone should be updated")
    public void theUserMobilePhoneShouldBeUpdated() {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        assertEquals(dashboardStepsUtil.filter.getMobilePhone(), dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getMobilePhone());
    }

    @And("the user product should not contain the mentioned product")
    public void theUserProductShouldNotContainTheMentionedProduct() {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        List<ProductInfoResource> response = dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getProducts().stream()
                .filter(p ->
                        p.getId().equals(dashboardStepsUtil.filter.getProductId()))
                .toList();

        assertEquals(Collections.EMPTY_LIST, response);
    }

    @And("the user product should not contain the mentioned roles info")
    public void theUserProductShouldNotContainTheMentionedRolesInfo() {
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        institutionApiSteps.iSendAGETRequestToToRetrieveInstitutionUser("/v2/institutions/{institutionId}/users/{userId}");
        List<ProductRoleInfoResource> response = dashboardStepsUtil.getResponses().getInstitutionUserDetailsResource().getProducts().stream()
                .filter(p ->
                        p.getId().equals(dashboardStepsUtil.filter.getProductId()))
                .toList().get(0)
                .getRoleInfos().stream()
                .filter(r ->
                        r.getRole().equals(dashboardStepsUtil.filter.getProductRoles().get(0))).toList();
        assertEquals(Collections.EMPTY_LIST, response);
    }
}
