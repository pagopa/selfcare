package it.pagopa.selfcare.document.integrationTest;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthStep {

    private String readinessPath;
    private Response response;

    @Given("the readiness endpoint is available at {string}")
    public void the_readiness_endpoint_is_available_at(String path) {
        this.readinessPath = path;
    }

    @When("I call the readiness endpoint")
    public void i_call_the_readiness_endpoint() {
        this.response = given().when().get(readinessPath);
    }

    @Then("the readiness HTTP status is {int}")
    public void the_readiness_http_status_is(Integer status) {
        assertThat(response.statusCode()).isEqualTo(status);
    }

    @Then("the readiness overall status is {string}")
    public void the_readiness_overall_status_is(String expected) {
        assertThat(response.jsonPath().getString("status")).isEqualTo(expected);
    }

    @Then("the readiness response contains a check named {string} with status {string}")
    public void the_readiness_response_contains_a_check(String name, String expectedStatus) {
        Map<String, Object> check = findCheck(name);
        assertThat(check)
                .as("readiness check named '%s'", name)
                .isNotNull()
                .containsEntry("status", expectedStatus);
    }

    @Then("the readiness check {string} data contains key {string}")
    public void the_readiness_check_data_contains_key(String checkName, String dataKey) {
        Map<String, Object> check = findCheck(checkName);
        assertThat(check).as("check '%s'", checkName).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) check.get("data");
        assertThat(data)
                .as("data of check '%s'", checkName)
                .isNotNull()
                .containsKey(dataKey);
    }

    private Map<String, Object> findCheck(String name) {
        List<Map<String, Object>> checks = response.jsonPath().getList("checks");
        return checks.stream()
                .filter(c -> name.equals(c.get("name")))
                .findFirst()
                .orElse(null);
    }
}

