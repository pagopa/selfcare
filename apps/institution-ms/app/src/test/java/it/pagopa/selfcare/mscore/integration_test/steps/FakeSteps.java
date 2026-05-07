package it.pagopa.selfcare.mscore.integration_test.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

public class FakeSteps {

    private int status;

    @When("I send a request to {string}")
    public void iSendARequestTo(String url) {
        status = 200;
    }

    @Then("[FAKE] the response status should be {int}")
    public void verifyResponseStatus(int expectedStatusCode) {
        Assertions.assertEquals(expectedStatusCode, status);
    }

}
