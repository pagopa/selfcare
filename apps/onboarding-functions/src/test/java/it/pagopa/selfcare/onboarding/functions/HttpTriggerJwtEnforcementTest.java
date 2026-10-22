package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.FunctionInvocationLogger;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import jakarta.inject.Inject;
import java.util.List;
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
  @InjectMock FunctionInvocationLogger functionInvocationLogger;

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
    // given
    // the validator rejection is configured in setUp

    // when
    List<HttpResponseMessage> responses =
        List.of(
            externalFunctions.testToken(request, context),
            externalFunctions.checkOrganization(request, context),
            externalFunctions.messageAcknowledgment(
                request, "fake-product", "fake-message-id", "ACK", context),
            externalFunctions.webhookTest(request, context));

    // then
    responses.forEach(HttpTriggerJwtEnforcementTest::assertUnauthorized);
    verify(httpJwtValidator, times(4)).validate(request, context);
    verifyNoInteractions(functionInvocationLogger);
  }

  @Test
  void notificationFunctionsRejectUnauthenticatedRequests() throws Exception {
    // given
    // the validator rejection is configured in setUp

    // when
    List<HttpResponseMessage> responses =
        List.of(
            notificationFunctions.sendNotification(request, context),
            notificationFunctions.resendNotification(request, context),
            notificationFunctions.countNotifications(request, context),
            notificationFunctions.resendNotifications(request, durableContext, context));

    // then
    responses.forEach(HttpTriggerJwtEnforcementTest::assertUnauthorized);
    verify(httpJwtValidator, times(4)).validate(request, context);
    verifyNoInteractions(functionInvocationLogger, durableContext);
  }

  @Test
  void institutionFunctionsRejectUnauthenticatedRequests() {
    // given
    // the validator rejection is configured in setUp

    // when
    HttpResponseMessage response =
        institutionFunctions.deleteInstitutionAndUserTrigger(request, durableContext, context);

    // then
    assertUnauthorized(response);
    verify(httpJwtValidator).validate(request, context);
    verifyNoInteractions(functionInvocationLogger, durableContext);
  }

  @Test
  void onboardingFunctionsRejectUnauthenticatedRequests() {
    // given
    // the validator rejection is configured in setUp

    // when
    List<HttpResponseMessage> responses =
        List.of(
            onboardingFunctions.startOrchestration(request, durableContext, context),
            onboardingFunctions.buildAttachmentsAndSaveTokens(request, durableContext, context),
            onboardingFunctions.sendTestEmail(request, context));

    // then
    responses.forEach(HttpTriggerJwtEnforcementTest::assertUnauthorized);
    verify(httpJwtValidator, times(3)).validate(request, context);
    verifyNoInteractions(functionInvocationLogger, durableContext);
  }

  private static void assertUnauthorized(HttpResponseMessage response) {
    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode());
  }
}
