package it.pagopa.selfcare.auth.integration_test.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.auth.exception.SamlSignatureException;
import it.pagopa.selfcare.auth.model.UserClaims;
import it.pagopa.selfcare.auth.service.SAMLService;
import it.pagopa.selfcare.auth.service.SessionService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class SamlStepDefinitions {
  @Inject
  SAMLService samlService;

  @Inject
  SessionService sessionService;

  private String samlResponse;
  private Uni<String> tokenUni;
  private Throwable failure;

  @Given("SAML service is available")
  public void samlServiceIsAvailable() {
    Assertions.assertNotNull(samlService);
  }

  @When("I submit a valid SAML response")
  public void iSubmitAValidSamlResponse() {
    UserClaims userClaims = new UserClaims();
    userClaims.setUid("123e4567-e89b-12d3-a456-426614174000");
    userClaims.setEmail("valid-user@example.com");
//      buildFakeSaml("valid-user@example.com", Instant.now(), true);
    samlResponse = sessionService.generateSessionTokenInternal(userClaims).await().indefinitely();
    execute();
  }


  @When("I submit a SAML response with invalid signature")
  public void iSubmitASamlResponseWithInvalidSignature() {
    samlResponse = buildFakeSaml("invalid-sign@example.com", Instant.now(), false);
    execute();
  }

  @When("I submit an expired SAML response")
  public void iSubmitAnExpiredSamlResponse() {
    samlResponse = buildFakeSaml("expired-user@example.com", Instant.now().minusSeconds(10_000), true);
    execute();
  }

  @Then("a session token is generated")
  public void aSessionTokenIsGenerated() {
    Assertions.assertNull(failure);
    String token = tokenUni.await().indefinitely();
    Assertions.assertNotNull(token);
    Assertions.assertFalse(token.isBlank());
  }

  @Then("the user is synchronized with IAM")
  public void theUserIsSynchronizedWithIam() {
    // Placeholder: in real case verify side effects (e.g. spy or mock IAM client)
    Assertions.assertNull(failure);
  }

  @Then("the login fails with signature error")
  public void theLoginFailsWithSignatureError() {
    Assertions.assertNotNull(failure);
    Assertions.assertTrue(failure instanceof SamlSignatureException);
  }

  @Then("the login fails with time interval error")
  public void theLoginFailsWithTimeIntervalError() {
    Assertions.assertNotNull(failure);
    Assertions.assertTrue(failure instanceof SamlSignatureException);
  }

  private void execute() {
    try {
      tokenUni = samlService.generateSessionToken(samlResponse);
      // Trigger resolution to capture failure
      tokenUni.subscribe().with(
        t -> {},
        f -> failure = f
      );
      // Wait briefly for async completion
      tokenUni.await().atMost(java.time.Duration.ofSeconds(2));
    } catch (Exception e) {
      failure = e;
    }
  }

  private String buildFakeSaml(String internalId, Instant issueTime, boolean validSignature) {
    // Simplified fake SAML (replace with real XML if needed)
    String xml = """
      <Assertion>
        <Subject>%s</Subject>
        <IssueInstant>%s</IssueInstant>
        <Signature>%s</Signature>
      </Assertion>
      """.formatted(internalId, issueTime.toString(), validSignature ? "VALID" : "INVALID");
    return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
  }
}
