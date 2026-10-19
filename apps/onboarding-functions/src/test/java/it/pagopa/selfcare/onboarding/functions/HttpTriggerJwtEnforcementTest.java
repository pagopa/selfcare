package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@QuarkusTest
class HttpTriggerJwtEnforcementTest {

  @Inject ExternalFunctions externalFunctions;
  @Inject NotificationFunctions notificationFunctions;
  @Inject InstitutionFunctions institutionFunctions;
  @Inject OnboardingFunctions onboardingFunctions;

  @InjectMock HttpJwtValidator httpJwtValidator;

  private HttpRequestMessage<Optional<String>> request;
  private ExecutionContext context;
  private DurableClientContext durableContext;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    // given
    request = mock(HttpRequestMessage.class);
    when(request.createResponseBuilder(any(HttpStatus.class)))
        .thenAnswer(
            (Answer<HttpResponseMessage.Builder>)
                invocation ->
                    new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                        .status((HttpStatus) invocation.getArguments()[0]));
    context = mock(ExecutionContext.class);
    when(context.getLogger()).thenReturn(Logger.getGlobal());
    durableContext = mock(DurableClientContext.class);
    HttpResponseMessage unauthorized =
        new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
            .status(HttpStatus.UNAUTHORIZED)
            .build();
    when(httpJwtValidator.validate(any(), any())).thenReturn(Optional.of(unauthorized));
  }

  @Test
  void externalFunctionsRejectUnauthenticatedRequests() {
    // when / then
    assertUnauthorized(externalFunctions.testToken(request, context));
    assertUnauthorized(externalFunctions.checkOrganization(request, context));
    assertUnauthorized(
        externalFunctions.messageAcknowledgment(request, "prod-test", "message-id", "ACK", context));
    assertUnauthorized(externalFunctions.webhookTest(request, context));
  }

  @Test
  void notificationFunctionsRejectUnauthenticatedRequests() throws Exception {
    // when / then
    assertUnauthorized(notificationFunctions.sendNotification(request, context));
    assertUnauthorized(notificationFunctions.resendNotification(request, context));
    assertUnauthorized(notificationFunctions.countNotifications(request, context));
    assertUnauthorized(
        notificationFunctions.resendNotifications(request, durableContext, context));
  }

  @Test
  void institutionFunctionsRejectUnauthenticatedRequests() {
    // when / then
    assertUnauthorized(
        institutionFunctions.deleteInstitutionAndUserTrigger(request, durableContext, context));
  }

  @Test
  void onboardingFunctionsRejectUnauthenticatedRequests() {
    // when / then
    assertUnauthorized(onboardingFunctions.startOrchestration(request, durableContext, context));
    assertUnauthorized(
        onboardingFunctions.buildAttachmentsAndSaveTokens(request, durableContext, context));
    assertUnauthorized(onboardingFunctions.sendTestEmail(request, context));
  }

  private static void assertUnauthorized(HttpResponseMessage response) {
    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode());
  }
}
