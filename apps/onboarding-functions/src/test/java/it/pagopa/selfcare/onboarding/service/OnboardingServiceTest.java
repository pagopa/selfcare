package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.repository.OnboardingRepository;
import it.pagopa.selfcare.onboarding.repository.TokenRepository;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;
import org.openapi.quarkus.user_json.api.InstitutionApi;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class OnboardingServiceTest {

  @InjectMock OnboardingRepository onboardingRepository;
  @InjectMock TokenRepository tokenRepository;
  @RestClient @InjectMock UserApi userRegistryApi;
  @RestClient @InjectMock InstitutionApi userInstitutionApi;
  @RestClient @InjectMock
  org.openapi.quarkus.user_json.api.UserApi userApi;
  @RestClient @InjectMock
  PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi;
  @InjectMock NotificationService notificationService;
  @InjectMock ContractService contractService;
  @InjectMock ProductService productService;
  @InjectMock
  UserMapper userMapper;
  @InjectMock
  AzureBlobClient azureBlobClient;
  @Inject OnboardingService onboardingService;

  final String productId = "productId";

  private Onboarding createOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("id");
    onboarding.setProductId(productId);
    onboarding.setUsers(List.of());
    Institution institution = new Institution();
    institution.setDescription("description");
    institution.setInstitutionType(InstitutionType.PA);
    onboarding.setInstitution(institution);
    onboarding.setUserRequester(UserRequester.builder().userRequestUid("example-uid").build());
    onboarding.setWorkflowType(WorkflowType.FOR_APPROVE);
    onboarding.setStatus(OnboardingStatus.REQUEST);
    return onboarding;
  }

  private UserResource createUserResource() {
    UserResource userResource = new UserResource();
    userResource.setId(UUID.randomUUID());

    CertifiableFieldResourceOfstring resourceOfName = new CertifiableFieldResourceOfstring();
    resourceOfName.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
    resourceOfName.setValue("name");
    userResource.setName(resourceOfName);

    CertifiableFieldResourceOfstring resourceOfSurname = new CertifiableFieldResourceOfstring();
    resourceOfSurname.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
    resourceOfSurname.setValue("surname");
    userResource.setFamilyName(resourceOfSurname);
    return userResource;
  }

  @Test
  void getOnboarding() {
    Onboarding onboarding = createOnboarding();
    when(onboardingRepository.findByIdOptional(any())).thenReturn(Optional.of(onboarding));

    Optional<Onboarding> actual = onboardingService.getOnboarding(onboarding.getId());
    assertTrue(actual.isPresent());
    assertEquals(onboarding.getId(), actual.get().getId());
  }

  @Test
  void createContract_shouldThrowIfManagerNotfound() {
    Onboarding onboarding = createOnboarding();
    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);

    UserResource userResource = createUserResource();
    User user = new User();
    user.setId(userResource.getId().toString());
    user.setRole(PartyRole.MANAGER);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId()))
            .thenReturn(userResource);

    Product product = new Product();
    product.setTitle("title");

    when(productService.getProductIsValid(any())).thenReturn(product);

    assertThrows(
            GenericOnboardingException.class,
            () -> onboardingService.createContract(onboardingWorkflow));
  }

  @Test
  void createContract_InstitutionContractMappings() {

    UserResource userResource = createUserResource();

    Onboarding onboarding = createOnboarding();
    User manager = new User();
    manager.setId(userResource.getId().toString());
    manager.setRole(PartyRole.MANAGER);
    onboarding.setUsers(List.of(manager));

    Product product = createDummyProduct();

    when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
            .thenReturn(userResource);

    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
    onboardingService.createContract(onboardingWorkflow);

    Mockito.verify(userRegistryApi, Mockito.times(1))
            .findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId());

    Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());

    ArgumentCaptor<String> captorTemplatePath = ArgumentCaptor.forClass(String.class);
    Mockito.verify(contractService, Mockito.times(1))
            .createContractPDF(captorTemplatePath.capture(), any(), any(), any(), any(), any());
    assertEquals(
            captorTemplatePath.getValue(),
            product
                    .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                    .getContractTemplatePath());
  }

  private static OnboardingWorkflow getOnboardingWorkflowInstitution(Onboarding onboarding) {
    return new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
  }

  @Test
  void createContract() {

    UserResource userResource = createUserResource();
    UserResource delegateResource = createUserResource();

    Onboarding onboarding = createOnboarding();
    User manager = new User();
    manager.setId(userResource.getId().toString());
    manager.setRole(PartyRole.MANAGER);
    User delegate = new User();
    delegate.setId(delegateResource.getId().toString());
    delegate.setRole(PartyRole.DELEGATE);
    onboarding.setUsers(List.of(manager, delegate));

    Product product = createDummyProduct();

    when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId()))
            .thenReturn(userResource);

    when(userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, delegate.getId()))
            .thenReturn(delegateResource);

    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
    onboardingService.createContract(onboardingWorkflow);

    Mockito.verify(userRegistryApi, Mockito.times(1))
            .findByIdUsingGET(USERS_WORKS_FIELD_LIST, manager.getId());

    Mockito.verify(userRegistryApi, Mockito.times(1))
            .findByIdUsingGET(USERS_WORKS_FIELD_LIST, delegate.getId());

    Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());

    ArgumentCaptor<String> captorTemplatePath = ArgumentCaptor.forClass(String.class);
    Mockito.verify(contractService, Mockito.times(1))
            .createContractPDF(captorTemplatePath.capture(), any(), any(), any(), any(), any());
    assertEquals(
            captorTemplatePath.getValue(),
            product
                    .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                    .getContractTemplatePath());
  }

  @Test
  void createAttachments() {

    // Arrange
    Onboarding onboarding = createOnboarding();
    User user = new User();
    user.setRole(PartyRole.MANAGER);
    user.setId("id");
    onboarding.setUsers(List.of(user));

    AttachmentTemplate attachmentTemplate = createDummyAttachmentTemplate();
    Product product = createDummyProduct();
    OnboardingAttachment onboardingAttachment = new OnboardingAttachment();
    onboardingAttachment.setAttachment(attachmentTemplate);
    onboardingAttachment.setOnboarding(onboarding);

    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

    UserResource userResource = new UserResource();
    userResource.setId(UUID.randomUUID());
    Map<String, WorkContactResource> map = new HashMap<>();
    userResource.setWorkContacts(map);

    when(userRegistryApi.findByIdUsingGET(anyString(), anyString()))
            .thenReturn(userResource);

    // Act
    onboardingService.createAttachment(onboardingAttachment);

    // Assert
    Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());
    Mockito.verify(contractService, Mockito.times(1))
            .createAttachmentPDF(any(), any(), any(), any(), any());
  }

  private Product createDummyProduct() {
    Product product = new Product();
    product.setTitle("Title");
    product.setId(productId);
    product.setInstitutionContractMappings(createDummyContractTemplateInstitution());
    product.setUserContractMappings(createDummyContractTemplateInstitution());
    product.setExpirationDate(30);

    return product;
  }

  private static Map<String, ContractTemplate> createDummyContractTemplateInstitution() {
    Map<String, ContractTemplate> institutionTemplate = new HashMap<>();
    List<AttachmentTemplate> attachments = new ArrayList<>();
    AttachmentTemplate attachmentTemplate = createDummyAttachmentTemplate();
    attachments.add(attachmentTemplate);
    ContractTemplate conctractTemplate = new ContractTemplate();
    conctractTemplate.setAttachments(attachments);
    conctractTemplate.setContractTemplatePath("example");
    conctractTemplate.setContractTemplateVersion("version");
    institutionTemplate.put(Product.CONTRACT_TYPE_DEFAULT, conctractTemplate);
    return institutionTemplate;
  }

  private static AttachmentTemplate createDummyAttachmentTemplate() {
    AttachmentTemplate attachmentTemplate = new AttachmentTemplate();
    attachmentTemplate.setTemplatePath("path");
    attachmentTemplate.setName("name");
    attachmentTemplate.setWorkflowState(OnboardingStatus.REQUEST);
    attachmentTemplate.setWorkflowType(List.of(WorkflowType.FOR_APPROVE));
    return attachmentTemplate;
  }

  @Test
  void saveToken_shouldSkipIfTokenExists() {
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
    Onboarding onboarding = createOnboarding();
    Token token = createDummyToken();
    onboardingWorkflow.setOnboarding(onboarding);

    when(tokenRepository.findByIdOptional(onboarding.getId())).thenReturn(Optional.of(token));

    onboardingService.saveTokenWithContract(onboardingWorkflow);

    Mockito.verify(tokenRepository, Mockito.times(1)).findByIdOptional(onboarding.getId());
    Mockito.verifyNoMoreInteractions(tokenRepository);
  }

  @Test
  void saveToken() {
    Onboarding onboarding = createOnboarding();
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
    onboardingWorkflow.setOnboarding(onboarding);
    File contract =
            new File(
                    Objects.requireNonNull(
                                    getClass().getClassLoader().getResource("application.properties"))
                            .getFile());
    DSSDocument document = new FileDocument(contract);
    String digestExpected = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();

    Product productExpected = createDummyProduct();
    when(contractService.retrieveContractNotSigned(onboardingWorkflow, productExpected.getTitle()))
            .thenReturn(contract);
    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(productExpected);

    Mockito.doNothing().when(tokenRepository).persist(any(Token.class));

    onboardingService.saveTokenWithContract(onboardingWorkflow);

    ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
    Mockito.verify(tokenRepository, Mockito.times(1)).persist(tokenArgumentCaptor.capture());
    assertEquals(onboarding.getProductId(), tokenArgumentCaptor.getValue().getProductId());
    assertEquals(digestExpected, tokenArgumentCaptor.getValue().getChecksum());
    assertEquals(
            productExpected
                    .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                    .getContractTemplatePath(),
            tokenArgumentCaptor.getValue().getContractTemplate());
    assertEquals(
            productExpected
                    .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                    .getContractTemplateVersion(),
            tokenArgumentCaptor.getValue().getContractVersion());
  }

  @Test
  void saveTokenAttachment() {
    Onboarding onboarding = createOnboarding();
    AttachmentTemplate attachmentTemplate = createDummyAttachmentTemplate();
    OnboardingAttachment onboardingAttachment = new OnboardingAttachment();
    onboardingAttachment.setOnboarding(onboarding);
    onboardingAttachment.setAttachment(attachmentTemplate);
    File contract =
            new File(
                    Objects.requireNonNull(
                                    getClass().getClassLoader().getResource("application.properties"))
                            .getFile());
    DSSDocument document = new FileDocument(contract);
    String digestExpected = document.getDigest(DigestAlgorithm.SHA256).getBase64Value();

    Product productExpected = createDummyProduct();
    when(contractService.retrieveAttachment(onboardingAttachment, productExpected.getTitle()))
            .thenReturn(contract);
    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(productExpected);

    Mockito.doNothing().when(tokenRepository).persist(any(Token.class));

    onboardingService.saveTokenWithAttachment(onboardingAttachment);

    ArgumentCaptor<Token> tokenArgumentCaptor = ArgumentCaptor.forClass(Token.class);
    Mockito.verify(tokenRepository, Mockito.times(1)).persist(tokenArgumentCaptor.capture());
    assertEquals(onboarding.getProductId(), tokenArgumentCaptor.getValue().getProductId());
    assertEquals(attachmentTemplate.getName(), tokenArgumentCaptor.getValue().getName());
    assertEquals(digestExpected, tokenArgumentCaptor.getValue().getChecksum());
  }

  @Test
  void loadContract() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();

    when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

    onboardingService.loadContract(onboarding);

    Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());
    Mockito.verify(contractService, Mockito.times(1))
            .loadContractPDF(
                    product
                            .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                            .getContractTemplatePath(),
                    onboarding.getId(),
                    product.getTitle());
  }

  @Test
  void sendMailRegistrationWithContract() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    UserResource userResource = createUserResource();
    Token token = createDummyToken();

    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.of(token));
    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);

    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
    SendMailInput sendMailInput = new SendMailInput();
    sendMailInput.setUserRequestName(userResource.getName().getValue());
    sendMailInput.setUserRequestSurname(userResource.getFamilyName().getValue());
    sendMailInput.setProduct(product);
    sendMailInput.setInstitutionName("description");

    doNothing()
            .when(notificationService)
            .sendMailRegistrationForContract(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    sendMailInput,
                    "default",
                    "default", "10");

    onboardingService.sendMailRegistrationForContract(onboardingWorkflow);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistrationForContract(any(), any(), any(), anyString(), anyString(), anyString());
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void testSendMailRegistrationForUser_Success() {
    // Arrange
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setDescription("Test Institution");
    onboarding.setInstitution(institution);
    onboarding.setProductId("prod-123");

    User user = new User();
    user.setId("user-1");
    user.setRole(PartyRole.MANAGER);
    user.setUserMailUuid("uuid-123");
    onboarding.setUsers(List.of(user));

    SendMailDto expectedDto = new SendMailDto();
    expectedDto.setInstitutionName("Test Institution");
    expectedDto.setProductId("prod-123");
    expectedDto.setUserMailUuid("uuid-123");

    Mockito.when(userMapper.toUserPartyRole(PartyRole.MANAGER)).thenReturn(org.openapi.quarkus.user_json.model.PartyRole.MANAGER);

    // Act
    onboardingService.sendMailRegistrationForUser(onboarding);

    // Assert
    Mockito.verify(userApi).sendMailRequest(any(),
            Mockito.argThat(dto ->
                    dto.getInstitutionName().equals(expectedDto.getInstitutionName()) &&
                            dto.getProductId().equals(expectedDto.getProductId()) &&
                            dto.getUserMailUuid().equals(expectedDto.getUserMailUuid())
            )
    );
  }

  @Test
  void testSaveVisuraForMerchant_Success() {
    // Arrange
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setDescription("Test Institution");
    onboarding.setInstitution(institution);
    onboarding.setProductId("prod-123");

    User user = new User();
    user.setId("user-1");
    user.setRole(PartyRole.MANAGER);
    user.setUserMailUuid("uuid-123");
    onboarding.setUsers(List.of(user));

    Mockito.when(pdndVisuraInfoCamereControllerApi.institutionVisuraDocumentByTaxCodeUsingGET(any())).thenReturn("test".getBytes(StandardCharsets.UTF_8));
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn("test");
    // Act
    onboardingService.saveVisuraForMerchant(onboarding);

    // Assert
    Mockito.verify(pdndVisuraInfoCamereControllerApi).institutionVisuraDocumentByTaxCodeUsingGET(Mockito.any());

  }

  @Test
  void testSaveVisuraForMerchant_Exception() {
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setDescription("Test Institution");
    onboarding.setInstitution(institution);
    onboarding.setProductId("prod-123");
    User user = new User();
    user.setId("user-1");
    user.setRole(PartyRole.MANAGER);
    user.setUserMailUuid("uuid-123");
    onboarding.setUsers(List.of(user));

    Mockito.doThrow(new RuntimeException("Error during download"))
            .when(pdndVisuraInfoCamereControllerApi).institutionVisuraDocumentByTaxCodeUsingGET(Mockito.any());

    Assertions.assertDoesNotThrow(() -> onboardingService.saveVisuraForMerchant(onboarding));

    Mockito.verify(pdndVisuraInfoCamereControllerApi).institutionVisuraDocumentByTaxCodeUsingGET(Mockito.any());
  }

  @Test
  void testSendMailRegistrationForUser_Exception() {
    Onboarding onboarding = new Onboarding();
    Institution institution = new Institution();
    institution.setDescription("Test Institution");
    onboarding.setInstitution(institution);
    onboarding.setProductId("prod-123");
    User user = new User();
    user.setId("user-1");
    user.setRole(PartyRole.MANAGER);
    user.setUserMailUuid("uuid-123");
    onboarding.setUsers(List.of(user));
    Mockito.when(userMapper.toUserPartyRole(PartyRole.MANAGER)).thenReturn(org.openapi.quarkus.user_json.model.PartyRole.MANAGER);
    Mockito.doThrow(new RuntimeException("Email failure"))
            .when(userApi).sendMailRequest(Mockito.any(), Mockito.any());

    Assertions.assertDoesNotThrow(() -> onboardingService.sendMailRegistrationForUser(onboarding));

    Mockito.verify(userApi).sendMailRequest(Mockito.any(), Mockito.any());
  }

  @Test
  void sendMailRegistrationWithContractAggregator() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    UserResource userResource = createUserResource();
    Token token = createDummyToken();

    Integer expirationDate = 30;

    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.of(token));
    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);
    doNothing()
            .when(notificationService)
            .sendMailRegistrationForContractAggregator(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(),
                    String.valueOf(expirationDate));

    onboardingService.sendMailRegistrationForContractAggregator(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistrationForContractAggregator(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(),
                    product.getExpirationDate().toString());
  }

  @Test
  void sendMailRegistrationWithContractWhenApprove() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    Token token = createDummyToken();

    Integer expirationDate = 30;

    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.of(token));
    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);

    doNothing()
            .when(notificationService)
            .sendMailRegistrationForContract(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    onboarding.getInstitution().getDescription(),
                    "",
                    product.getTitle(),
                    "description",
                    "default",
                    "default", String.valueOf(expirationDate));

    onboardingService.sendMailRegistrationForContractWhenApprove(onboardingWorkflow);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistrationForContract(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    onboarding.getInstitution().getDescription(),
                    "",
                    product.getTitle(),
                    "description",
                    "contracts/template/mail/onboarding-request/1.0.1.json",
                    "https://dev.selfcare.pagopa.it/onboarding/confirm?jwt=", "30");
  }

  @Test
  void sendMailRegistrationWithContract_throwExceptionWhenTokenIsNotPresent() {
    Onboarding onboarding = createOnboarding();
    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.empty());
    assertThrows(
            GenericOnboardingException.class,
            () -> onboardingService.sendMailRegistrationForContract(onboardingWorkflow));
  }

  @Test
  void sendMailRegistration() {

    UserResource userResource = createUserResource();
    Product product = createDummyProduct();
    Onboarding onboarding = createOnboarding();

    Integer expirationDate = 30;

    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);
    doNothing()
            .when(notificationService)
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), product.getExpirationDate().toString());

    onboardingService.sendMailRegistration(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), String.valueOf(expirationDate));
  }

  @Test
  void sendMailRegistration_with_deletedManager() {

    UserResource userResource = createUserResource();
    Product product = createDummyProduct();
    Onboarding onboarding = createOnboarding();
    onboarding.getInstitution().setOrigin(Origin.IPA);
    onboarding.setPreviousManagerId("previousManagerId");

    Integer expirationDate = 30;

    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);

    when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

    doNothing()
            .when(notificationService)
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), String.valueOf(expirationDate));

    onboardingService.sendMailRegistration(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), product.getExpirationDate().toString());
  }

  @Test
  void sendMailRegistration_with_check_userMS() {

    UserResource userResource = createUserResource();
    Product product = createDummyProduct();
    Onboarding onboarding = createOnboarding();
    onboarding.getInstitution().setOrigin(Origin.IPA);
    onboarding.setPreviousManagerId("previousManagerId");

    Integer expirationDate = 30;

    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

    when(userRegistryApi.findByIdUsingGET(any(), any()))
            .thenReturn(userResource);

    when(onboardingRepository.findByFilters(any(), any(), any(), any(), any()))
            .thenReturn(List.of(onboarding));

    UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
    userInstitutionResponse.setInstitutionId(onboarding.getInstitution().getId());
    userInstitutionResponse.setUserId("previousManagerId");

    when(userInstitutionApi.retrieveUserInstitutions(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(userInstitutionResponse));

    doNothing()
            .when(notificationService)
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), String.valueOf(expirationDate));

    onboardingService.sendMailRegistration(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistration(
                    onboarding.getInstitution().getDescription(),
                    onboarding.getInstitution().getDigitalAddress(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(), product.getExpirationDate().toString());
  }

  @Test
  void sendMailRegistrationApprove() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    UserResource userResource = createUserResource();

    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);

    doNothing()
            .when(notificationService)
            .sendMailRegistrationApprove(any(), any(), any(), any(), any());

    onboardingService.sendMailRegistrationApprove(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailRegistrationApprove(
                    onboarding.getInstitution().getDescription(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(),
                    onboarding.getId());
  }

  @Test
  void sendMailRegistrationApprove_throwExceptionWhenTokenIsNotPresent() {
    Onboarding onboarding = createOnboarding();
    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.empty());
    assertThrows(
            GenericOnboardingException.class,
            () -> onboardingService.sendMailRegistrationApprove(onboarding));
  }

  @Test
  void sendMailOnboardingApprove() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    UserResource userResource = createUserResource();

    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);
    doNothing()
            .when(notificationService)
            .sendMailOnboardingApprove(any(), any(), any(), any(), any());

    onboardingService.sendMailOnboardingApprove(onboarding);

    Mockito.verify(notificationService, times(1))
            .sendMailOnboardingApprove(
                    onboarding.getInstitution().getDescription(),
                    userResource.getName().getValue(),
                    userResource.getFamilyName().getValue(),
                    product.getTitle(),
                    onboarding.getId());
  }

  @Test
  void sendMailOnboardingApprove_throwExceptionWhenTokenIsNotPresent() {
    Onboarding onboarding = createOnboarding();
    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.empty());
    assertThrows(
            GenericOnboardingException.class,
            () -> onboardingService.sendMailOnboardingApprove(onboarding));
  }

  @Test
  void countOnboardingShouldReturnCorrectResultsWhenProductsExist() {
    String from = "2021-01-01";
    String to = "2021-12-31";

    ExecutionContext context = getExecutionContext();

    PanacheQuery<Onboarding> onboardingQuery = mock(PanacheQuery.class);

    Product product1 = new Product();
    product1.setId("product1");
    when(productService.getProducts(false, false)).thenReturn(List.of(product1));

    when(onboardingRepository.find(any())).thenReturn(onboardingQuery);
    when(onboardingQuery.count()).thenReturn(5L).thenReturn(3L);

    List<NotificationCountResult> results =
            onboardingService.countNotifications(product1.getId(), from, to, context);

    assertEquals(1, results.size());
    assertEquals(8, results.get(0).getNotificationCount());
  }

  @Test
  void countOnboardingShouldReturnEmptyListWhenNoProductsExist() {
    ExecutionContext context = getExecutionContext();

    when(productService.getProducts(true, false)).thenReturn(List.of());
    List<NotificationCountResult> results =
            onboardingService.countNotifications(null, null, null, context);
    assertTrue(results.isEmpty());
  }

  @Test
  void getOnboardingsToResendShouldReturnResults() {
    ResendNotificationsFilters filters = new ResendNotificationsFilters();
    filters.setFrom("2021-01-01");
    filters.setTo("2021-12-31");

    getExecutionContext();

    PanacheQuery<Onboarding> onboardingQuery = mock(PanacheQuery.class);
    when(onboardingRepository.find(any())).thenReturn(onboardingQuery);
    when(onboardingQuery.page(anyInt(), anyInt())).thenReturn(onboardingQuery);
    when(onboardingQuery.list()).thenReturn(List.of(new Onboarding(), new Onboarding()));

    List<Onboarding> onboardings = onboardingService.getOnboardingsToResend(filters, 0, 100);
    assertEquals(2, onboardings.size());
  }

  @Test
  void getOnboardingsToResendShouldReturnEmptyList() {
    ResendNotificationsFilters filters = new ResendNotificationsFilters();
    filters.setFrom("2021-01-01");
    filters.setTo("2021-12-31");

    getExecutionContext();

    PanacheQuery<Onboarding> onboardingQuery = mock(PanacheQuery.class);
    when(onboardingRepository.find(any())).thenReturn(onboardingQuery);
    when(onboardingQuery.page(anyInt(), anyInt())).thenReturn(onboardingQuery);
    when(onboardingQuery.list()).thenReturn(List.of());

    List<Onboarding> onboardings = onboardingService.getOnboardingsToResend(filters, 0, 100);
    assertTrue(onboardings.isEmpty());
  }

  private ExecutionContext getExecutionContext() {
    ExecutionContext context = mock(ExecutionContext.class);
    doReturn(Logger.getGlobal()).when(context).getLogger();
    return context;
  }

  private Token createDummyToken() {
    Token token = new Token();
    token.setId(UUID.randomUUID().toString());
    return token;
  }

  @Test
  void testUpdateTokenContractFiles() {
    // Given
    String contractSigned = "parties/docs/token1/doc.7m";
    String contractFilename = "contract.pdf";
    Token token = new Token();
    token.setId("token1");
    token.setContractSigned(contractSigned);
    token.setContractFilename(contractFilename);

    PanacheUpdate panacheUpdate = mock(PanacheUpdate.class);
    when(tokenRepository.update(anyString(), any(Map.class)))
            .thenReturn(panacheUpdate);
    when(panacheUpdate.where(anyString(), any(Map.class)))
            .thenReturn(1L);
    long result = onboardingService.updateTokenContractFiles(token);

    assertEquals(1L, result);

    // Then
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);

    Mockito.verify(tokenRepository).update(queryCaptor.capture(), paramsCaptor.capture());

    String capturedQuery = queryCaptor.getValue();
    Map<String, Object> capturedParams = paramsCaptor.getValue();
    assertThat(capturedQuery, equalTo("contractSigned = :contractSigned and contractFilename = :contractFilename and updatedAt = :updatedAt"));
    assertThat(capturedParams, Matchers.hasKey("contractSigned"));
    assertThat(capturedParams, Matchers.hasValue(contractSigned));
    assertThat(capturedParams, Matchers.hasKey("contractFilename"));
    assertThat(capturedParams, Matchers.hasValue(contractFilename));
    assertThat(capturedParams, Matchers.hasKey("updatedAt"));

    // Verifica chiamata al where
    ArgumentCaptor<String> whereQueryCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, Object>> whereParamsCaptor = ArgumentCaptor.forClass(Map.class);

    verify(panacheUpdate).where(whereQueryCaptor.capture(), whereParamsCaptor.capture());

    String capturedWhereQuery = whereQueryCaptor.getValue();
    Map<String, Object> capturedWhereParams = whereParamsCaptor.getValue();

    assertThat(capturedWhereQuery, equalTo("_id = :tokenId"));
    assertThat(capturedWhereParams, Matchers.hasValue(token.getId()));
    assertThat(capturedWhereParams, Matchers.hasKey("tokenId"));
  }

  @Test
  void sendMailRegistrationWithContractOK() {

    Onboarding onboarding = createOnboarding();
    Product product = createDummyProduct();
    UserResource userResource = createUserResource();
    Token token = createDummyToken();

    when(tokenRepository.findByOnboardingId(onboarding.getId())).thenReturn(Optional.of(token));
    when(productService.getProduct(onboarding.getProductId())).thenReturn(product);

    when(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
            .thenReturn(userResource);

    OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
    SendMailInput sendMailInput = new SendMailInput();
    sendMailInput.setUserRequestName(userResource.getName().getValue());
    sendMailInput.setUserRequestSurname(userResource.getFamilyName().getValue());
    sendMailInput.setProduct(product);
    sendMailInput.setInstitutionName("description");

    doNothing()
            .when(notificationService)
            .sendMailRegistrationForContract(
                    onboarding.getId(),
                    onboarding.getInstitution().getDigitalAddress(),
                    sendMailInput,
                    "default",
                    "default", "10");

    onboardingService.sendMailRegistrationForContract(onboardingWorkflow);

    Mockito.verify(notificationService, times(1))
             .sendMailRegistrationForContract(any(), any(), any(), anyString(), anyString(), anyString());
    verifyNoMoreInteractions(notificationService);
  }

  @Test
  void updateOnboardingExpiringDate_shouldUpdateCorrectExpiringDateAndPersist() {
    Onboarding onboarding = createOnboarding();
    Integer expirationDays = 30;
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDays);

    onboardingService.updateOnboardingExpiringDate(onboarding);

    assertNotNull(onboarding.getExpiringDate());
    assertEquals(
        OffsetDateTime.now().plusDays(expirationDays).toLocalDate(),
        onboarding.getExpiringDate().toLocalDate());
    verify(onboardingRepository).update(onboarding);
  }

  @Test
  void updateOnboardingExpiringDate_shouldHandleNullExpirationDays() {
    Onboarding onboarding = createOnboarding();
    when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(null);

    assertThrows(NullPointerException.class, () -> onboardingService.updateOnboardingExpiringDate(onboarding));
    verify(onboardingRepository, never()).update(any(Onboarding.class));
  }

  @Test
  void updateOnboardingExpiringDate_shouldHandleInvalidProductId() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("invalid-product-id");
    when(productService.getProductExpirationDate(onboarding.getProductId()))
        .thenThrow(new GenericOnboardingException("Product not found"));

    assertThrows(GenericOnboardingException.class, () -> onboardingService.updateOnboardingExpiringDate(onboarding));
    verify(onboardingRepository, never()).update(any(Onboarding.class));
  }

    @Test
    void testSendMailRegistrationForUserRequester_Success() {
        // Arrange
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setDescription("Test Institution");
        onboarding.setInstitution(institution);
        onboarding.setProductId("prod-123");

        User user = new User();
        user.setId("user-1");
        user.setRole(PartyRole.MANAGER);
        user.setUserMailUuid("uuid-123");
        onboarding.setUsers(List.of(user));

        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .userMailUuid("uuid-123")
                .build();
        onboarding.setUserRequester(userRequester);

        SendMailDto expectedDto = new SendMailDto();
        expectedDto.setInstitutionName("Test Institution");
        expectedDto.setProductId("prod-123");
        expectedDto.setUserMailUuid("uuid-123");

        Mockito.when(userMapper.toUserPartyRole(PartyRole.MANAGER)).thenReturn(org.openapi.quarkus.user_json.model.PartyRole.MANAGER);

        // Act
        onboardingService.sendMailRegistrationForUserRequester(onboarding);

        // Assert
        Mockito.verify(userApi).sendMailRequest(any(),
                Mockito.argThat(dto ->
                        dto.getInstitutionName().equals(expectedDto.getInstitutionName()) &&
                                dto.getProductId().equals(expectedDto.getProductId()) &&
                                dto.getUserMailUuid().equals(expectedDto.getUserMailUuid())
                )
        );
    }

    @Test
    void testSendMailRegistrationForUserRequester_Exception() {
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setDescription("Test Institution");
        onboarding.setInstitution(institution);
        onboarding.setProductId("prod-123");
        User user = new User();
        user.setId("user-1");
        user.setRole(PartyRole.MANAGER);
        user.setUserMailUuid("uuid-123");
        onboarding.setUsers(List.of(user));
        UserRequester userRequester = UserRequester.builder()
                .userRequestUid(UUID.randomUUID().toString())
                .userMailUuid("uuid-123")
                .build();
        onboarding.setUserRequester(userRequester);
        Mockito.when(userMapper.toUserPartyRole(PartyRole.MANAGER)).thenReturn(org.openapi.quarkus.user_json.model.PartyRole.MANAGER);
        Mockito.doThrow(new RuntimeException("Email failure"))
                .when(userApi).sendMailRequest(Mockito.any(), Mockito.any());

        Assertions.assertDoesNotThrow(() -> onboardingService.sendMailRegistrationForUserRequester(onboarding));

        Mockito.verify(userApi).sendMailRequest(Mockito.any(), Mockito.any());
    }
}
