package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.*;
import jakarta.inject.Inject;

import java.util.*;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * Unit test for Institution Function class.
 */
@QuarkusTest
public class InstitutionFunctionsTest {

  @Inject
  InstitutionFunctions function;

  @InjectMock
  InstitutionService institutionService;

  @InjectMock
  OnboardingService onboardingService;

  @InjectMock
  UserService userService;

  final String institutionUserFilters = "{\"userId\":\"userId\",\"productId\":\"productId\",\"institutionId\":\"institutionId\"}";

  static ExecutionContext executionContext;

  static {
    executionContext = mock(ExecutionContext.class);
    when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
  }

  @Test
  void deleteInstitutionAndUser_validRequest_returnsAccepted() {
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

    final Map<String, String> queryParams = new HashMap<>();
    final String onboardingId = "onboardingId";
    queryParams.put("onboardingId", onboardingId);
    doReturn(queryParams).when(req).getQueryParameters();

    doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
    }).when(req).createResponseBuilder(any(HttpStatus.class));

    final ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    final DurableClientContext durableContext = mock(DurableClientContext.class);
    final DurableTaskClient client = mock(DurableTaskClient.class);
    final String instanceId = "instanceId123";

    doReturn(client).when(durableContext).getClient();
    doReturn(instanceId)
      .when(client)
      .scheduleNewOrchestrationInstance("DeleteInstitutionAndUser", onboardingId);
    when(durableContext.createCheckStatusResponse(any(), any()))
      .thenReturn(
        new HttpResponseMessageMock.HttpResponseMessageBuilderMock()
          .status(HttpStatus.ACCEPTED)
          .build());

    // Invoke
    HttpResponseMessage responseMessage =
      function.deleteInstitutionAndUserTrigger(req, durableContext, executionContext);

    // Verify
    assertEquals(HttpStatus.ACCEPTED.value(), responseMessage.getStatusCode());
  }

  @Test
  void deleteInstitutionAndUser_emptyRequest_returnsBadRequest() {
    // Mock HttpRequestMessage with empty body
    final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
    final Map<String, String> queryParams = new HashMap<>();
    doReturn(queryParams).when(req).getQueryParameters();

    doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
      HttpStatus status = (HttpStatus) invocation.getArguments()[0];
      return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
    }).when(req).createResponseBuilder(any(HttpStatus.class));

    final ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    final DurableClientContext durableContext = mock(DurableClientContext.class);

    // Invoke
    HttpResponseMessage responseMessage =
      function.deleteInstitutionAndUserTrigger(req, durableContext, context);

    // Verify
    assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    assertEquals("onboardingId can't be null or empty", responseMessage.getBody());
  }

  @Test
  void deleteInstitutionAndUser_invokeActivity() throws JsonProcessingException {
    // given
    final String onboardingId = "onboardingId";
    final String productId = "productId";
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboardingId);

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(task.await()).thenReturn("false");
    when(orchestrationContext.allOf(anyList())).thenReturn(task);
    Onboarding onboarding = new Onboarding();
    onboarding.setId(onboardingId);
    Institution institution = new Institution();
    institution.setId("institutionId");
    onboarding.setInstitution(institution);
    onboarding.setProductId(productId);
    User user = new User();
    user.setId("userId");
    onboarding.setUsers(List.of(user));
    when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.of(onboarding));

    // when
    function.deleteInstitutionAndUserOnboarding(orchestrationContext, executionContext);

    // then
    Mockito.verify(orchestrationContext, times(3)).callActivity(any(), any(), any(), any());
  }

  @Test
  void deleteInstitutionAndUser_error() {
    // given
    final String onboardingId = "onboardingId";
    TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
    when(orchestrationContext.getInput(String.class)).thenReturn(onboardingId);

    Task task = mock(Task.class);
    when(orchestrationContext.callActivity(any(), any(), any(), any())).thenReturn(task);
    when(task.await()).thenReturn("false");
    when(orchestrationContext.allOf(anyList())).thenReturn(task);
    when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.empty());

    assertThrows(
      ResourceNotFoundException.class,
      () -> function.deleteInstitutionAndUserOnboarding(orchestrationContext, executionContext));

  }

  @Test
  void deleteInstitution() throws JsonProcessingException {

    doNothing().when(institutionService).deleteByIdAndProductId(any(), any());

    function.deleteInstitutionOnboarding(institutionUserFilters, executionContext);

    verify(institutionService, times(1)).deleteByIdAndProductId(any(), any());
  }

  @Test
  void deleteUser() throws JsonProcessingException {

    doNothing().when(userService).deleteByIdAndInstitutionIdAndProductId(any(), any());

    function.deleteUserOnboarding(institutionUserFilters, executionContext);

    verify(userService, times(1)).deleteByIdAndInstitutionIdAndProductId(any(), any());
  }

}

