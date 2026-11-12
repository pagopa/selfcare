package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.CallerIdentity;
import it.pagopa.selfcare.onboarding.service.HttpAuthorization;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@QuarkusTest
@TestProfile(NotificationFunctionsStrictTenantTest.StrictTenantProfile.class)
class NotificationFunctionsStrictTenantTest {

  public static class StrictTenantProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("selfcare.tenant.strict-data-isolation", "true");
    }
  }

  @Inject NotificationFunctions function;

  @InjectMock NotificationEventService notificationEventService;

  @InjectMock HttpJwtValidator httpJwtValidator;

  @Test
  void sendNotificationRejectsPayloadWithoutTenantWithStrictIsolation() {
    when(httpJwtValidator.authenticate(any(), any()))
        .thenReturn(
            HttpAuthorization.authorized(
                new CallerIdentity("requester-id", "SPID", null, null, null, null, null)));
    @SuppressWarnings("unchecked")
    HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
    doReturn(Optional.of("{\"onboardingId\":\"onboardingId\"}")).when(req).getBody();
    doReturn(Map.of("X-Tenant-Id", "AR")).when(req).getHeaders();
    doAnswer(
            (Answer<HttpResponseMessage.Builder>)
                invocation ->
                    new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
                        .status((HttpStatus) invocation.getArguments()[0]))
        .when(req)
        .createResponseBuilder(any(HttpStatus.class));
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    HttpResponseMessage response = function.sendNotification(req, context);

    verify(notificationEventService, never()).send(any(), any(), any());
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
  }
}
