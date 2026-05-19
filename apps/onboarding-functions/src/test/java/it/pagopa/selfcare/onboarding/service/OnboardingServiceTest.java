package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.common.*;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.dto.SendMailInput;
import it.pagopa.selfcare.onboarding.dto.ManagingInstitutionSendEmail;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentResponse;
import org.openapi.quarkus.user_json.model.SendMailDto;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class OnboardingServiceTest {

    final String productId = "productId";
    @InjectMock
    OnboardingRepositoryService onboardingRepositoryService;
    @InjectMock
    PdvUserRegistryService pdvUserRegistryService;
    @InjectMock
    UserService userService;
    @InjectMock
    RegistryProxyService registryProxyService;
    @InjectMock
    NotificationService notificationService;
    @InjectMock
    ProductService productService;
    @InjectMock
    UserMapper userMapper;
    @Inject
    OnboardingService onboardingService;
    @InjectMock
    DocumentService documentService;

    private static OnboardingWorkflow getOnboardingWorkflowInstitution(Onboarding onboarding) {
        return new OnboardingWorkflowInstitution(onboarding, "INSTITUTION");
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

        CertifiableFieldResourceOfstring workEmail = new CertifiableFieldResourceOfstring();
        workEmail.setCertification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
        workEmail.setValue("email.lavoro@test.it");

        WorkContactResource workContactResource = new WorkContactResource();
        workContactResource.setEmail(workEmail);

        Map<String, WorkContactResource> workContacts = new java.util.HashMap<>();
        workContacts.put("ID_MAIL#TEST-123", workContactResource);

        userResource.setWorkContacts(workContacts);

        return userResource;
    }

    @Test
    void getOnboarding() {
        Onboarding onboarding = createOnboarding();
        when(onboardingRepositoryService.findById(any())).thenReturn(Optional.of(onboarding));

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

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, user.getId()))
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
        manager.setUserMailUuid("ID_MAIL#TEST-123");
        onboarding.setUsers(List.of(manager));

        Product product = createDummyProduct();

        when(pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(userResource);

        when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        onboardingService.createContract(onboardingWorkflow);

        Mockito.verify(pdvUserRegistryService, Mockito.times(1))
                .getUserById(USERS_WORKS_FIELD_LIST, manager.getId());

        Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());

        ArgumentCaptor<ContractPdfRequest> captorRequest = ArgumentCaptor.forClass(ContractPdfRequest.class);
        Mockito.verify(documentService, Mockito.times(1))
                .createContractPdf(captorRequest.capture());
        assertEquals(
                captorRequest.getValue().getContractTemplatePath(),
                product
                        .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                        .getContractTemplatePath());
    }

    @Test
    void createContract() {

        UserResource userResource = createUserResource();
        UserResource delegateResource = createUserResource();

        Onboarding onboarding = createOnboarding();
        User manager = new User();
        manager.setId(userResource.getId().toString());
        manager.setRole(PartyRole.MANAGER);
        manager.setUserMailUuid("ID_MAIL#TEST-123");

        User delegate = new User();
        delegate.setId(delegateResource.getId().toString());
        delegate.setRole(PartyRole.DELEGATE);
        delegate.setUserMailUuid("ID_MAIL#TEST-123");
        onboarding.setUsers(List.of(manager, delegate));

        Product product = createDummyProduct();

        when(pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, manager.getId()))
                .thenReturn(userResource);

        when(pdvUserRegistryService.getUserById(USERS_WORKS_FIELD_LIST, delegate.getId()))
                .thenReturn(delegateResource);

        when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(product);

        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        onboardingService.createContract(onboardingWorkflow);

        Mockito.verify(pdvUserRegistryService, Mockito.times(1))
                .getUserById(USERS_WORKS_FIELD_LIST, manager.getId());

        Mockito.verify(pdvUserRegistryService, Mockito.times(1))
                .getUserById(USERS_WORKS_FIELD_LIST, delegate.getId());

        Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());

        ArgumentCaptor<ContractPdfRequest> captorRequest = ArgumentCaptor.forClass(ContractPdfRequest.class);
        Mockito.verify(documentService, Mockito.times(1))
                .createContractPdf(captorRequest.capture());
        assertEquals(
                captorRequest.getValue().getContractTemplatePath(),
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

        when(pdvUserRegistryService.getUserById(anyString(), anyString()))
                .thenReturn(userResource);

        // Act
        onboardingService.createAttachment(onboardingAttachment);

        // Assert
        Mockito.verify(productService, Mockito.times(1)).getProductIsValid(onboarding.getProductId());
        ArgumentCaptor<AttachmentPdfRequest> captorRequest = ArgumentCaptor.forClass(AttachmentPdfRequest.class);
        Mockito.verify(documentService, Mockito.times(1))
                .createAttachmentPdf(captorRequest.capture());
        assertEquals(attachmentTemplate.getTemplatePath(), captorRequest.getValue().getAttachmentTemplatePath());
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

    @Test
    void saveTokenWithContract_shouldSaveDocument() {
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
        Onboarding onboarding = createOnboarding();
        onboardingWorkflow.setOnboarding(onboarding);
        Product productExpected = createDummyProduct();
        when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(productExpected);
        onboardingService.saveTokenWithContract(onboardingWorkflow);
        ArgumentCaptor<DocumentBuilderRequest> requestCaptor =
                ArgumentCaptor.forClass(DocumentBuilderRequest.class);
        Mockito.verify(documentService, Mockito.times(1)).saveDocument(requestCaptor.capture());
        assertEquals(onboarding.getId(), requestCaptor.getValue().getOnboardingId());
        assertEquals(onboarding.getProductId(), requestCaptor.getValue().getProductId());
        assertEquals(
                org.openapi.quarkus.document_json.model.DocumentType.INSTITUTION,
                requestCaptor.getValue().getDocumentType());
        assertEquals(
                productExpected
                        .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                        .getContractTemplatePath(),
                requestCaptor.getValue().getTemplatePath());
        assertEquals(
                productExpected
                        .getInstitutionContractTemplate(Product.CONTRACT_TYPE_DEFAULT)
                        .getContractTemplateVersion(),
                requestCaptor.getValue().getTemplateVersion());
    }

    @Test
    void saveTokenWithAttachment_shouldSaveDocument() {
        Onboarding onboarding = createOnboarding();
        AttachmentTemplate attachmentTemplate = createDummyAttachmentTemplate();
        OnboardingAttachment onboardingAttachment = new OnboardingAttachment();
        onboardingAttachment.setOnboarding(onboarding);
        onboardingAttachment.setAttachment(attachmentTemplate);

        Product productExpected = createDummyProduct();
        when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(productExpected);
        onboardingService.saveTokenWithAttachment(onboardingAttachment);
        ArgumentCaptor<DocumentBuilderRequest> requestCaptor =
                ArgumentCaptor.forClass(DocumentBuilderRequest.class);
        Mockito.verify(documentService, Mockito.times(1)).saveDocument(requestCaptor.capture());
        assertEquals(onboarding.getId(), requestCaptor.getValue().getOnboardingId());
        assertEquals(onboarding.getProductId(), requestCaptor.getValue().getProductId());
        assertEquals(
                org.openapi.quarkus.document_json.model.DocumentType.ATTACHMENT,
                requestCaptor.getValue().getDocumentType());
        assertEquals(attachmentTemplate.getName(), requestCaptor.getValue().getAttachmentName());
        assertEquals(attachmentTemplate.getTemplatePath(), requestCaptor.getValue().getTemplatePath());
        assertEquals(attachmentTemplate.getTemplateVersion(), requestCaptor.getValue().getTemplateVersion());
    }

    @Test
    void saveTokenWithContract_shouldThrowWhenDocumentServiceFails() {
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
        onboardingWorkflow.setOnboarding(onboarding);
        Product productExpected = createDummyProduct();
        when(productService.getProductIsValid(onboarding.getProductId())).thenReturn(productExpected);
        doThrow(new GenericOnboardingException("ko"))
                .when(documentService)
                .saveDocument(any(DocumentBuilderRequest.class));

        assertThrows(
                GenericOnboardingException.class,
                () -> onboardingService.saveTokenWithContract(onboardingWorkflow));
    }

    @Test
    void sendMailRegistrationWithContract() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();
        DocumentResponse document = createDummyToken();

        when(documentService.getDocumentByOnboardingIdOrNull(onboarding.getId())).thenReturn(document);
        when(productService.getProduct(onboarding.getProductId())).thenReturn(product);

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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
        Mockito.verify(userService).sendMailRequest(any(),
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
        onboarding.setId("onboarding-id");
        Institution institution = new Institution();
        institution.setDescription("Test Institution");
        institution.setTaxCode("12345678901");
        onboarding.setInstitution(institution);
        onboarding.setProductId("prod-123");

        User user = new User();
        user.setId("user-1");
        user.setRole(PartyRole.MANAGER);
        user.setUserMailUuid("uuid-123");
        onboarding.setUsers(List.of(user));

        Mockito.when(registryProxyService.getInstitutionVisuraByTaxCode(any()))
                .thenReturn("test".getBytes(StandardCharsets.UTF_8));
        Mockito.when(documentService.saveVisuraForMerchant(any()))
                .thenReturn(Response.ok().build());
        // Act
        onboardingService.saveVisuraForMerchant(onboarding);

        // Assert
        Mockito.verify(registryProxyService).getInstitutionVisuraByTaxCode(Mockito.any());
        Mockito.verify(documentService).saveVisuraForMerchant(any());
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
                .when(registryProxyService).getInstitutionVisuraByTaxCode(Mockito.any());

        Assertions.assertThrows(RuntimeException.class, () -> onboardingService.saveVisuraForMerchant(onboarding));

        Mockito.verify(registryProxyService).getInstitutionVisuraByTaxCode(Mockito.any());
        Mockito.verifyNoInteractions(documentService);
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
                .when(userService).sendMailRequest(Mockito.any(), Mockito.any());

        Assertions.assertThrows(RuntimeException.class, () -> userService.sendMailRequest(Mockito.any(), Mockito.any()));

        Mockito.verify(userService).sendMailRequest(Mockito.any(), Mockito.any());
    }

    @Test
    void sendMailRegistrationWithContractAggregator() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();
        DocumentResponse document = createDummyToken();

        Integer expirationDate = 30;

        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenReturn(document);
        when(productService.getProduct(onboarding.getProductId())).thenReturn(product);
        when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(expirationDate);

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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
        DocumentResponse document = createDummyToken();

        Integer expirationDate = 30;

        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenReturn(document);
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
    void sendMailRegistrationWithContract_throwExceptionWhenDocumentIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        OnboardingWorkflow onboardingWorkflow = getOnboardingWorkflowInstitution(onboarding);
        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenThrow(new RuntimeException("Document not found"));
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

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
                .thenReturn(userResource);

        when(onboardingRepositoryService.findByFilters(any(), any(), any(), any(), any()))
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

        when(pdvUserRegistryService.getUserById(any(), any()))
                .thenReturn(userResource);

        when(onboardingRepositoryService.findByFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of(onboarding));

        UserInstitutionResponse userInstitutionResponse = new UserInstitutionResponse();
        userInstitutionResponse.setInstitutionId(onboarding.getInstitution().getId());
        userInstitutionResponse.setUserId("previousManagerId");

        when(userService.getActiveManagersByInstitutionAndProduct(any(), any(), any()))
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
        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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
    void sendMailRegistrationApprove_throwExceptionWhenDocumentIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenThrow(new RuntimeException("Document not found"));
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
        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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
    void sendMailOnboardingApprove_throwExceptionWhenDocumentIsNotPresent() {
        Onboarding onboarding = createOnboarding();
        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenThrow(new RuntimeException("Document not found"));
        assertThrows(
                GenericOnboardingException.class,
                () -> onboardingService.sendMailOnboardingApprove(onboarding));
    }

    @Test
    void countOnboardingShouldReturnCorrectResultsWhenProductsExist() {
        String from = "2021-01-01";
        String to = "2021-12-31";

        ExecutionContext context = getExecutionContext();

        Product product1 = new Product();
        product1.setId("product1");
        when(productService.getProducts(false, false)).thenReturn(List.of(product1));

        when(onboardingRepositoryService.countByQuery(any())).thenReturn(5L).thenReturn(3L);

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

        when(onboardingRepositoryService.findByQueryPaged(any(), anyInt(), anyInt()))
                .thenReturn(List.of(new Onboarding(), new Onboarding()));

        List<Onboarding> onboardings = onboardingService.getOnboardingsToResend(filters, 0, 100);
        assertEquals(2, onboardings.size());
    }

    @Test
    void getOnboardingsToResendShouldReturnEmptyList() {
        ResendNotificationsFilters filters = new ResendNotificationsFilters();
        filters.setFrom("2021-01-01");
        filters.setTo("2021-12-31");

        getExecutionContext();

        when(onboardingRepositoryService.findByQueryPaged(any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<Onboarding> onboardings = onboardingService.getOnboardingsToResend(filters, 0, 100);
        assertTrue(onboardings.isEmpty());
    }

    private ExecutionContext getExecutionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        return context;
    }

    private DocumentResponse createDummyToken() {
        DocumentResponse document = new DocumentResponse();
        document.setId(UUID.randomUUID().toString());
        return document;
    }

    @Test
    void sendMailRegistrationWithContractOK() {

        Onboarding onboarding = createOnboarding();
        Product product = createDummyProduct();
        UserResource userResource = createUserResource();
        DocumentResponse document = createDummyToken();

        when(documentService.getDocumentByOnboardingId(onboarding.getId())).thenReturn(document);
        when(productService.getProduct(onboarding.getProductId())).thenReturn(product);

        when(pdvUserRegistryService.getUserById(USERS_FIELD_LIST, onboarding.getUserRequester().getUserRequestUid()))
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
        verify(onboardingRepositoryService).update(onboarding);
    }

    @Test
    void updateOnboardingExpiringDate_shouldHandleNullExpirationDays() {
        Onboarding onboarding = createOnboarding();
        when(productService.getProductExpirationDate(onboarding.getProductId())).thenReturn(null);

        assertThrows(NullPointerException.class, () -> onboardingService.updateOnboardingExpiringDate(onboarding));
        verify(onboardingRepositoryService, never()).update(any(Onboarding.class));
    }

    @Test
    void updateOnboardingExpiringDate_shouldHandleInvalidProductId() {
        Onboarding onboarding = createOnboarding();
        onboarding.setProductId("invalid-product-id");
        when(productService.getProductExpirationDate(onboarding.getProductId()))
                .thenThrow(new GenericOnboardingException("Product not found"));

        assertThrows(GenericOnboardingException.class, () -> onboardingService.updateOnboardingExpiringDate(onboarding));
        verify(onboardingRepositoryService, never()).update(any(Onboarding.class));
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
        Mockito.verify(userService).sendMailRequest(any(),
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
                .when(userService).sendMailRequest(Mockito.any(), Mockito.any());

        Assertions.assertThrows(RuntimeException.class, () -> onboardingService.sendMailRegistrationForUserRequester(onboarding));

        Mockito.verify(userService).sendMailRequest(Mockito.any(), Mockito.any());
    }

    @Test
    void findByInstitutionAndProduct() {
        // given
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setId("institutionId");
        onboarding.setInstitution(institution);
        onboarding.setProductId(productId);
        onboarding.setUsers(List.of());
        // when
        when(onboardingRepositoryService.findByOnboardingUsers("institutionId", productId))
                .thenReturn(List.of(onboarding));
        // then
        List<String> onboardings = onboardingService.findByInstitutionAndProduct("institutionId", productId);
        assertNotNull(onboardings);
        assertTrue(onboardings.isEmpty());

        Mockito.verify(onboardingRepositoryService, times(1))
                .findByOnboardingUsers("institutionId", productId);

    }

    @Test
    void findByInstitutionAndProduct_NotEmptyList() {
        // given
        Onboarding onboarding = new Onboarding();
        Institution institution = new Institution();
        institution.setId("institutionId");
        onboarding.setInstitution(institution);
        onboarding.setProductId(productId);
        User user = new User();
        String userId = "userId";
        user.setId(userId);
        onboarding.setUsers(List.of(user));
        // when
        when(onboardingRepositoryService.findByOnboardingUsers("institutionId", productId))
                .thenReturn(List.of(onboarding));
        // then
        List<String> onboardings = onboardingService.findByInstitutionAndProduct("institutionId", productId);
        assertNotNull(onboardings);
        assertFalse(onboardings.isEmpty());
        assertEquals(1, onboardings.size());
        assertEquals(userId, onboardings.get(0));

        Mockito.verify(onboardingRepositoryService, times(1))
                .findByOnboardingUsers("institutionId", productId);

    }

    @Test
    void findByInstitutionAndProduct_EmptyList() {
        // when
        when(onboardingRepositoryService.findByOnboardingUsers("institutionId", productId))
                .thenReturn(List.of());
        // then
        List<String> onboardings = onboardingService.findByInstitutionAndProduct("institutionId", productId);
        assertNotNull(onboardings);
        assertTrue(onboardings.isEmpty());

        Mockito.verify(onboardingRepositoryService, times(1))
                .findByOnboardingUsers("institutionId", productId);

    }

    @Test
    void testSendMailManagingInstitution_Success() {
        ManagingInstitutionSendEmail request = new ManagingInstitutionSendEmail();
        request.setManagingInstitutionDescription("Test Institution");
        request.setProductId("prod-123");
        request.setUserMailUuid("uuid-123");
        request.setUserId("user-1");

        onboardingService.sendMailManagingInstitution(request);

        Mockito.verify(userService).sendMailRequest(eq("user-1"),
                Mockito.argThat(dto ->
                        "Test Institution".equals(dto.getInstitutionName())
                                && "prod-123".equals(dto.getProductId())
                                && "uuid-123".equals(dto.getUserMailUuid())
                )
        );
    }

    @Test
    void testSendMailManagingInstitution_Exception() {
        ManagingInstitutionSendEmail request = new ManagingInstitutionSendEmail();
        request.setManagingInstitutionDescription("Test Institution");
        request.setProductId("prod-123");
        request.setUserMailUuid("uuid-123");
        request.setUserId("user-1");

        Mockito.doThrow(new RuntimeException("Email failure"))
                .when(userService)
                .sendMailRequest(eq("uuid-123"), any(SendMailDto.class));

        Assertions.assertDoesNotThrow(() -> onboardingService.sendMailManagingInstitution(request));

        Mockito.verify(userService).sendMailRequest(eq("user-1"), any(SendMailDto.class));
    }
}
