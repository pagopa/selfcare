package it.pagopa.selfcare.webhook.cucumber.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import it.pagopa.selfcare.webhook.dto.NotificationRequest;
import it.pagopa.selfcare.webhook.dto.WebhookRequest;
import it.pagopa.selfcare.webhook.dto.WebhookResponse;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class WebhookSteps {

    @Inject
    WebhookRepository webhookRepository;

    private WebhookRequest webhookRequest;
    private Response response;
    private String lastWebhookProductId;

    @Given("the database is empty")
    public void theDatabaseIsEmpty() {
        webhookRepository.deleteAll().await().indefinitely();
    }

    @Given("I have a webhook request with url {string}")
    public void iHaveAWebhookRequest(String url) {
        webhookRequest = new WebhookRequest();
        webhookRequest.setUrl(url);
        webhookRequest.setHttpMethod("POST");
    }

    @Given("I have created a webhook with url {string} and productId {string}")
    public void iHaveCreatedAWebhook(String url, String productId) {
        WebhookRequest request = new WebhookRequest();
        request.setUrl(url);
        request.setHttpMethod("POST");

        WebhookResponse created = given()
                .contentType("application/json")
                .queryParam("requesterProductId", productId)
                .body(request)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(201)
                .extract().as(WebhookResponse.class);

        lastWebhookProductId = created.getProductId();
    }

    @Given("I have created a webhook for product {string}")
    public void iHaveCreatedAWebhookForProduct(String productId) {
        WebhookRequest request = new WebhookRequest();
        request.setUrl("http://example.com");
        request.setHttpMethod("POST");

        WebhookResponse created = given()
                .contentType("application/json")
                .queryParam("requesterProductId", productId)
                .body(request)
                .when()
                .post("/webhooks")
                .then()
                .statusCode(201)
                .extract().as(WebhookResponse.class);
        
        lastWebhookProductId = created.getProductId();
    }

    @When("I create the webhook")
    public void iCreateTheWebhook() {
        response = given()
                .contentType("application/json")
                .queryParam("requesterProductId", "prod-test")
                .body(webhookRequest)
                .when()
                .post("/webhooks");
        
        if (response.statusCode() == 201) {
            lastWebhookProductId = response.jsonPath().getString("id");
        }
    }

    @When("I list all webhooks")
    public void iListAllWebhooks() {
        response = given()
                .when()
                .get("/webhooks");
    }

    @When("I get the webhook by its ID")
    public void iGetTheWebhookByItsID() {
        response = given()
                .when()
                .queryParam("requesterProductId", lastWebhookProductId)
                .get("/webhooks/" + lastWebhookProductId);
    }

    @When("I update the webhook with url {string}")
    public void iUpdateTheWebhook(String url) {
        WebhookRequest updateRequest = new WebhookRequest();
        updateRequest.setUrl(url);
        updateRequest.setHttpMethod("POST");

        response = given()
                .contentType("application/json")
                .queryParam("requesterProductId", lastWebhookProductId)
                .body(updateRequest)
                .when()
                .put("/webhooks/" + lastWebhookProductId);
    }

    @When("I delete the webhook by its ID")
    public void iDeleteTheWebhook() {
        response = given()
                .when()
                .delete("/webhooks/" + lastWebhookProductId);
    }

    @When("I send a notification for product {string} with payload {string}")
    public void iSendANotification(String productId, String payload) {
        NotificationRequest request = new NotificationRequest();
        request.setProductId(productId);
        request.setPayload(payload);

        response = given()
                .contentType("application/json")
                .body(request)
                .when()
                .post("/webhooks/notify");
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int statusCode) {
        response.then().statusCode(statusCode);
    }

    @Then("the response should contain {string}")
    public void theResponseShouldContain(String field) {
        response.then().body(field, notNullValue());
    }

    @Then("the response should contain {string} with value {string}")
    public void theResponseShouldContainWithValue(String field, String value) {
        response.then().body(field, equalTo(value));
    }

    @Then("the response list should contain {int} items")
    public void theResponseListShouldContainItems(int size) {
        response.then().body("size()", is(size));
    }
}
