package it.pagopa.selfcare.user_group.integration_test.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class FakeSteps  extends UserGroupSteps {



    @Override
    @Then("[FAKE] the response status should be {int}")
    public void verifyResponseStatus(int expectedStatusCode) {
        super.verifyResponseStatus(expectedStatusCode);
    }

    @When("I send a request to {string}")
    public void iSendARequestTo(String url) {
        status = 200;
    }
}
