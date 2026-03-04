package it.pagopa.selfcare.onboarding.service;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.eventhub.EventHubRestClient;
import it.pagopa.selfcare.onboarding.client.webhook.WebhookRestClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.dto.*;
import it.pagopa.selfcare.onboarding.dto.QueueEvent;
import it.pagopa.selfcare.onboarding.dto.UserToNotify;
import it.pagopa.selfcare.onboarding.dto.webhook.NotificationRequest;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.onboarding.utils.*;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.user_json.model.*;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class NotificationEventServiceDefaultTest {

  @Inject
  NotificationEventServiceDefault notificationServiceDefault;

  @InjectMock
  ProductService productService;

  @RestClient
  @InjectMock
  EventHubRestClient eventHubRestClient;

  @InjectMock
  NotificationBuilderFactory notificationBuilderFactory;

  @InjectMock
  NotificationUserBuilderFactory notificationUserBuilderFactory;

  @InjectMock
  TokenRepository tokenRepository;

  @RestClient
  @InjectMock
  org.openapi.quarkus.user_json.api.UserApi userApi;

  @RestClient
  @InjectMock
  InstitutionApi institutionApi;

  @InjectMock
  QueueEventExaminer queueEventExaminer;

    @RestClient
    @InjectMock
    WebhookRestClient webhookRestClient;


  @Test
  void sendMessage() {
    // given
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();

    when(productService.getProduct(any())).thenReturn(product);
    mockNotificationMapper(true);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    List<UserDataResponse> users = new ArrayList<>();
    when(userApi.retrieveUsers(any(), any(), any(), any(), any(), any(), any()))
      .thenReturn(users);
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    doNothing().when(webhookRestClient).sendNotification(any(NotificationRequest.class));

    // when
    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);

    // then
    verify(eventHubRestClient, times(3))
      .sendMessage(anyString(), anyString());
    verify(webhookRestClient, times(3))
      .sendNotification(any(NotificationRequest.class));
  }

  @Test
  void sendMessageWithoutQueueEvent() {
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();
    when(productService.getProduct(any())).thenReturn(product);
    mockNotificationMapper(true);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    when(queueEventExaminer.determineEventType(any())).thenReturn(QueueEvent.ADD);
    notificationServiceDefault.send(context, onboarding, null);
    verify(eventHubRestClient, times(3))
      .sendMessage(anyString(), anyString());
  }

  private void mockNotificationMapper(boolean shouldSendNotification) {
    BaseNotificationBuilder notificationMapper = mock(BaseNotificationBuilder.class);
    when(notificationBuilderFactory.create(any())).thenReturn(notificationMapper);
    when(notificationMapper.buildNotificationToSend(any(), any(), any(), any())).thenReturn(new NotificationToSend());
    when(notificationMapper.shouldSendNotification(any(), any())).thenReturn(shouldSendNotification);
  }

  @Test
  void sendMessageWithoutToken() {
    final Onboarding onboarding = createOnboarding();
    onboarding.setId("123");
    final Product product = createProduct();
    when(productService.getProduct(any())).thenReturn(product);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.empty());
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    mockNotificationMapper(true);
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
    verify(eventHubRestClient, times(3))
      .sendMessage(anyString(), anyString());
  }

  @Test
  void sendMessageDoesntSendNotificationIfFilterDoesntAllow() {
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();
    when(productService.getProduct(any())).thenReturn(product);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    mockNotificationMapper(false);
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
    verifyNoInteractions(eventHubRestClient);
  }

  @Test
  void sendMessageWithTestEnvProducts() {
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();
    product.setTestEnvProductIds(List.of("prod-interop-coll", "prod-interop-atst"));
    when(productService.getProduct(any())).thenReturn(product);
    mockNotificationMapper(true);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
    verify(eventHubRestClient, times(9))
      .sendMessage(anyString(), anyString());
  }

  @Test
  void sendMessageWithError() {
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();
    when(productService.getProduct(any())).thenReturn(product);
    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());
    mockNotificationMapper(true);
    doThrow(new NotificationException("Impossible to send notification for object" + onboarding))
      .when(eventHubRestClient).sendMessage(anyString(), anyString());
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    assertThrows(NotificationException.class, () -> notificationServiceDefault.send(context, onboarding, QueueEvent.ADD));
    verify(eventHubRestClient, times(1))
      .sendMessage(anyString(), anyString());
  }

  @Test
  void sendMessageNullConsumers() {
    final Onboarding onboarding = createOnboarding();
    Product test = new Product();
    test.setConsumers(List.of());
    when(productService.getProduct(any())).thenReturn(test);
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
    verifyNoInteractions(eventHubRestClient);
  }

  @Test
  void sendMessageWontProceedsWhenOnboardingIsNotReferredToInstitution() {
    final Onboarding onboarding = createOnboarding();
    onboarding.setWorkflowType(WorkflowType.USERS);

    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();

    notificationServiceDefault.send(context, onboarding, QueueEvent.ADD);
    verifyNoInteractions(productService);
    verifyNoInteractions(tokenRepository);
    verifyNoInteractions(institutionApi);
    verifyNoInteractions(eventHubRestClient);
  }

  @Test
  void onboardingEventMapTest() {
    final Onboarding onboarding = createOnboarding();
    onboarding.setId("ID");
    Map<String, String> properties = NotificationEventServiceDefault.onboardingEventMap(onboarding);
    assertNotNull(properties);
    assertEquals("ID", properties.get("id"));
  }

  @Test
  void onboardingEventFailureMapTest() {
    final Onboarding onboarding = createOnboarding();
    Map<String, String> properties = NotificationEventServiceDefault.onboardingEventFailureMap(onboarding, new Exception());
    assertNotNull(properties);
  }

  @Test
  void notificationEventMapTest() {
    NotificationToSend notificationToSend = getNotificationBaseToSend();

    InstitutionToNotify institution = getInstitutionToNotify();
    notificationToSend.setInstitution(institution);

    BillingToSend billing = new BillingToSend();
    billing.setRecipientCode("12345");
    billing.setPublicService(false);
    notificationToSend.setBilling(billing);

    Map<String, String> properties = NotificationEventServiceDefault.notificationEventMap(notificationToSend, "topic", "traceId");
    assertNotNull(properties);
    assertEquals("traceId", properties.get("notificationEventTraceId"));
    assertEquals("id", properties.get("id"));
    assertEquals("internal", properties.get("internalIstitutionID"));
    assertEquals("prod", properties.get("product"));
    assertEquals("state", properties.get("state"));
    assertEquals("fileName", properties.get("fileName"));
    assertEquals("filePath", properties.get("filePath"));
    assertEquals("application/pkcs7-mime", properties.get("contentType"));

    assertEquals("description", properties.get("description"));
    assertEquals("mail", properties.get("digitalAddress"));
    assertEquals("SA", properties.get("institutionType"));

    assertEquals("12345", properties.get("billing.recipientCode"));
    assertEquals("false", properties.get("billing.isPublicService"));
  }

  @Test
  void notificationEventMapRootParentTest() {
    NotificationToSend notificationToSend = getNotificationBaseToSend();
    notificationToSend.setContentType("/test/filé.pdf");

    InstitutionToNotify institution = getInstitutionToNotify();
    RootParent rootParent = new RootParent();
    rootParent.setDescription("RootDescription");
    rootParent.setId("RootId");
    rootParent.setOriginId("RootOriginId");
    institution.setRootParent(rootParent);
    notificationToSend.setInstitution(institution);

    BillingToSend billing = new BillingToSend();
    billing.setRecipientCode("12345");
    billing.setPublicService(true);
    billing.setVatNumber("123");
    billing.setTaxCodeInvoicing("456");
    notificationToSend.setBilling(billing);

    Map<String, String> properties = NotificationEventServiceDefault.notificationEventMap(notificationToSend, "topic", null);
    assertNotNull(properties);
    assertEquals("id", properties.get("id"));
    assertEquals("internal", properties.get("internalIstitutionID"));
    assertEquals("prod", properties.get("product"));
    assertEquals("state", properties.get("state"));
    assertEquals("fileName", properties.get("fileName"));
    assertEquals("filePath", properties.get("filePath"));
    assertEquals("application/pdf", properties.get("contentType"));

    assertEquals("description", properties.get("description"));
    assertEquals("mail", properties.get("digitalAddress"));
    assertEquals("SA", properties.get("institutionType"));
    assertEquals("RootId", properties.get("root.parentId"));
    assertEquals("RootDescription", properties.get("root.parentDescription"));
    assertEquals("RootOriginId", properties.get("root.parentOriginId"));

    assertEquals("12345", properties.get("billing.recipientCode"));
    assertEquals("true", properties.get("billing.isPublicService"));
    assertEquals("123", properties.get("billing.VatNumber"));
    assertEquals("456", properties.get("billing.TaxCodeInvoicing"));
  }

  @Test
  void notificationEventWithoutFile() {
    NotificationToSend notificationToSend = getNotificationBaseToSend();
    notificationToSend.setContentType(null);

    InstitutionToNotify institution = getInstitutionToNotify();
    RootParent rootParent = new RootParent();
    rootParent.setDescription("RootDescription");
    rootParent.setId("RootId");
    rootParent.setOriginId("RootOriginId");
    institution.setRootParent(rootParent);
    notificationToSend.setInstitution(institution);

    BillingToSend billing = new BillingToSend();
    billing.setRecipientCode("12345");
    billing.setPublicService(true);
    billing.setVatNumber("123");
    billing.setTaxCodeInvoicing("456");
    notificationToSend.setBilling(billing);

    Map<String, String> properties = NotificationEventServiceDefault.notificationEventMap(notificationToSend, "topic", null);
    assertNotNull(properties);
    assertEquals("id", properties.get("id"));
    assertEquals("internal", properties.get("internalIstitutionID"));
    assertEquals("prod", properties.get("product"));
    assertEquals("state", properties.get("state"));
    assertEquals("fileName", properties.get("fileName"));
    assertEquals("filePath", properties.get("filePath"));
    assertEquals("", properties.get("contentType"));

    assertEquals("description", properties.get("description"));
    assertEquals("mail", properties.get("digitalAddress"));
    assertEquals("SA", properties.get("institutionType"));
    assertEquals("RootId", properties.get("root.parentId"));
    assertEquals("RootDescription", properties.get("root.parentDescription"));
    assertEquals("RootOriginId", properties.get("root.parentOriginId"));

    assertEquals("12345", properties.get("billing.recipientCode"));
    assertEquals("true", properties.get("billing.isPublicService"));
    assertEquals("123", properties.get("billing.VatNumber"));
    assertEquals("456", properties.get("billing.TaxCodeInvoicing"));
  }

  @Test
  void sendNotificationsJsonError() {
    final Onboarding onboarding = createOnboarding();
    final Product product = createProduct();
    when(productService.getProduct(any())).thenReturn(product);

    when(tokenRepository.findByOnboardingId(any())).thenReturn(Optional.of(new Token()));
    when(institutionApi.retrieveInstitutionByIdUsingGET(any(), any())).thenReturn(new InstitutionResponse());

    BaseNotificationBuilder notificationMapper = mock(BaseNotificationBuilder.class);
    when(notificationBuilderFactory.create(any())).thenReturn(notificationMapper);

    NotificationToSend mockNotificationToSend = mock(NotificationToSend.class);
    when(mockNotificationToSend.toString()).thenReturn(mockNotificationToSend.getClass().getName());

    when(notificationMapper.buildNotificationToSend(any(), any(), any(), any())).thenReturn(mockNotificationToSend);
    when(notificationMapper.shouldSendNotification(any(), any())).thenReturn(true);

    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());
    TelemetryClient telemetryClient = mock(TelemetryClient.class);
    doNothing().when(telemetryClient).trackEvent(anyString(), any(), any());

    assertThrows(NotificationException.class, () -> notificationServiceDefault.send(context, onboarding, QueueEvent.ADD));
  }

  private Onboarding createOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setWorkflowType(WorkflowType.CONTRACT_REGISTRATION);
    onboarding.setId(onboarding.getId());
    String productId = "prod-io";
    onboarding.setProductId(productId);
    onboarding.setPricingPlan("pricingPlan");
    onboarding.setUsers(List.of());
    onboarding.setInstitution(new Institution());
    onboarding.setUserRequester(UserRequester.builder().userRequestUid("example-uid").build());

    Billing billing = new Billing();
    billing.setPublicServices(true);
    billing.setRecipientCode("example");
    billing.setVatNumber("example");
    billing.setTaxCodeInvoicing("taxCodeInvoicing");
    onboarding.setBilling(billing);
    return onboarding;
  }

  private Product createProduct() {
    var product = new Product();
    product.setConsumers(List.of("STANDARD", "SAP", "FD"));
    return product;
  }


  @Test
  void notificationEventUserMapTest() {
    NotificationUserToSend notificationUserToSend = getNotificationUserBaseToSend();
    UserToNotify user = new UserToNotify();
    user.setUserId("userId");
    user.setRole("OPERATOR");
    notificationUserToSend.setUser(user);

    Map<String, String> properties = NotificationEventServiceDefault.notificationUserEventMap(notificationUserToSend, "topic", "traceId");
    assertNotNull(properties);
    assertEquals("traceId", properties.get("notificationEventTraceId"));
    assertEquals("id", properties.get("id"));
    assertEquals("internal", properties.get("institutionId"));
    assertEquals("prod", properties.get("product"));

    assertEquals("userId", properties.get("userId"));
    assertEquals("OPERATOR", properties.get("role"));
  }

  @Test
  void getNotificationUserToSendTest() {
    Onboarding onboarding = createOnboarding();
    InstitutionResponse institutionResponse = new InstitutionResponse();
    Token token = new Token();
    NotificationsResources notificationsResources = new NotificationsResources(onboarding,
      institutionResponse, token, QueueEvent.ADD);

    OnboardedProductResponse onboardedProductResponse = new OnboardedProductResponse();
    onboardedProductResponse.productId("prod-fd-garantito");
    onboardedProductResponse.setEnv(Env.ROOT);
    onboardedProductResponse.setStatus(OnboardedProductState.ACTIVE);


    UserResponse userResponse = new UserResponse();
    userResponse.setId("userId");
    userResponse.setTaxCode("taxcode");
    userResponse.setName("Name");
    userResponse.setSurname("Surname");
    userResponse.setEmail("prv@email");
    HashMap<String, String> workContacts = new HashMap<String, String>();
    workContacts.put("email", "work@email");
    userResponse.setWorkContacts(workContacts);

    UserDataResponse userDataResponse = new UserDataResponse();
    userDataResponse.setId("userId");
    userDataResponse.institutionId("institutionId");
    userDataResponse.setInstitutionDescription("Institution Name");
    userDataResponse.setUserMailUuid("userMailId");
    userDataResponse.role("MANAGER");
    userDataResponse.setStatus("ADD");
    userDataResponse.setProducts(List.of(onboardedProductResponse));
    userDataResponse.setUserResponse(userResponse);

    NotificationUserToSend notificationUserToSendMock = new NotificationUserToSend();
    notificationUserToSendMock.setId("eventId");
    notificationUserToSendMock.setInstitutionId("institutionId");
    notificationUserToSendMock.setProduct("prod-fd-garantito");
    notificationUserToSendMock.setOnboardingTokenId("onboardingId");

    FdNotificationBuilder fdNotificationBuilder = mock(FdNotificationBuilder.class);
    when(notificationUserBuilderFactory.create(any())).thenReturn(fdNotificationBuilder);
    when(fdNotificationBuilder.buildUserNotificationToSend(any(), any(), any(), any(), any(), any(), any(),
      any(), any())).thenReturn(notificationUserToSendMock);
    when(fdNotificationBuilder.shouldSendUserNotification(any(), any())).thenReturn(true);
    doNothing().when(eventHubRestClient).sendMessage(anyString(), anyString());


    NotificationUserToSend notificationUserToSend = NotificationEventServiceDefault.getNotificationUserToSend(notificationsResources, userDataResponse,
      onboardedProductResponse, fdNotificationBuilder);

    assertNotNull(notificationUserToSend);
  }


  private static InstitutionToNotify getInstitutionToNotify() {
    InstitutionToNotify institution = new InstitutionToNotify();
    institution.setDescription("description");
    institution.setInstitutionType(InstitutionType.SA);
    institution.setDigitalAddress("mail");
    return institution;
  }

  private static NotificationToSend getNotificationBaseToSend() {
    NotificationToSend notificationToSend = new NotificationToSend();
    notificationToSend.setId("id");
    notificationToSend.setInternalIstitutionID("internal");
    notificationToSend.setProduct("prod");
    notificationToSend.setState("state");
    notificationToSend.setFileName("fileName");
    notificationToSend.setFilePath("filePath");
    notificationToSend.setContentType("/test/filé.p7m");
    return notificationToSend;
  }

  private static NotificationUserToSend getNotificationUserBaseToSend() {
    NotificationUserToSend notificationUserToSend = new NotificationUserToSend();
    notificationUserToSend.setId("id");
    notificationUserToSend.setInstitutionId("internal");
    notificationUserToSend.setProduct("prod");
    return notificationUserToSend;
  }

}
