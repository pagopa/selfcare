package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.config.AzureStorageConfig;
import it.pagopa.selfcare.onboarding.config.MailTemplatePlaceholdersConfig;
import it.pagopa.selfcare.onboarding.config.PagoPaSignatureConfig;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.utils.DataEncryptionUtils;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class ContractServiceDefaultTest {

  @Inject
  AzureStorageConfig azureStorageConfig;

  @InjectMock
  AzureBlobClient azureBlobClient;

  @InjectMock
  @RestClient
  UserApi userRegistryApi;
  PadesSignService padesSignService;

  @Inject
  ContractService contractService;

  @Inject
  PagoPaSignatureConfig pagoPaSignatureConfig;

  @Inject
  MailTemplatePlaceholdersConfig mailTemplatePlaceholdersConfig;


  static final String PRODUCT_NAME_EXAMPLE = "product-name";
  static final String LOGO_PATH = "logo-path";
  static final String PDF_FORMAT_FILENAME = "%s_accordo_adesione.pdf";

  @BeforeEach
  void setup() {
    padesSignService = mock(PadesSignService.class);
    contractService =
      new ContractServiceDefault(
        azureStorageConfig,
        azureBlobClient,
        padesSignService,
        pagoPaSignatureConfig,
        mailTemplatePlaceholdersConfig,
        LOGO_PATH,
        true,
        userRegistryApi);
  }

  private Onboarding createOnboarding() {
    Onboarding onboarding = new Onboarding();
    onboarding.setId("example");
    onboarding.setProductId("productId");
    onboarding.setUsers(List.of());

    createInstitution(onboarding);

    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setUserMailUuid("setUserMailUuid");
    onboarding.setUsers(List.of(user));
    return onboarding;
  }

  private static void createInstitution(Onboarding onboarding) {
    Institution institution = new Institution();
    institution.setInstitutionType(InstitutionType.PSP);
    institution.setDescription("42");

    PaymentServiceProvider paymentServiceProvider = createPaymentServiceProvider();
    institution.setPaymentServiceProvider(paymentServiceProvider);

    GPUData gpuData = createGpuData();
    institution.setGpuData(gpuData);

    institution.setRea("rea");
    institution.setBusinessRegisterPlace("place");
    institution.setShareCapital("10000");
    onboarding.setInstitution(institution);
  }

  private static GPUData createGpuData() {
    GPUData gpuData = new GPUData();
    gpuData.setManager(true);
    gpuData.setManagerAuthorized(true);
    gpuData.setManagerEligible(true);
    gpuData.setManagerProsecution(true);
    gpuData.setInstitutionCourtMeasures(true);
    return gpuData;
  }

  private static PaymentServiceProvider createPaymentServiceProvider() {
    PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
    paymentServiceProvider.setBusinessRegisterNumber("businessRegisterNumber");
    paymentServiceProvider.setLegalRegisterName("legalRegisterName");
    paymentServiceProvider.setLegalRegisterNumber("legalRegisterNumber");
    return paymentServiceProvider;
  }

  AggregateInstitution createAggregateInstitutionIO(int number) {
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setTaxCode(String.format("taxCode%s", number));
    aggregateInstitution.setOriginId(String.format("originId%s", number));
    aggregateInstitution.setDescription(String.format("description%s", number));
    aggregateInstitution.setVatNumber(String.format("vatNumber%s", number));
    aggregateInstitution.setAddress(String.format("address%s", number));
    aggregateInstitution.setCity(String.format("city%s", number));
    aggregateInstitution.setCounty(String.format("county%s", number));
    aggregateInstitution.setDigitalAddress(String.format("pec%s", number));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionAOO_IO(int number) {
    AggregateInstitution aggregateInstitution = createAggregateInstitutionIO(number);
    aggregateInstitution.setSubunitType("AOO");
    aggregateInstitution.setSubunitCode(String.format("code%s", number));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionPagoPa(int number) {
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setTaxCode(String.format("taxCode%s", number));
    aggregateInstitution.setDescription(String.format("description%s", number));
    aggregateInstitution.setVatNumber(String.format("vatNumber%s", number));
    aggregateInstitution.setAddress(String.format("address%s", number));
    aggregateInstitution.setCity(String.format("city%s", number));
    aggregateInstitution.setCounty(String.format("county%s", number));
    aggregateInstitution.setDigitalAddress(String.format("pec%s", number));
    aggregateInstitution.setIban(String.format("iban%s", number));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionSend(int number) {
    AggregateInstitution aggregateInstitution = new AggregateInstitution();
    aggregateInstitution.setTaxCode(String.format("taxCode%s", number));
    aggregateInstitution.setOriginId(String.format("originId%s", number));
    aggregateInstitution.setDescription(String.format("description%s", number));
    aggregateInstitution.setVatNumber(String.format("vatNumber%s", number));
    aggregateInstitution.setRecipientCode(String.format("recipientCode%s", number));
    aggregateInstitution.setAddress(String.format("address%s", number));
    aggregateInstitution.setCity(String.format("city%s", number));
    aggregateInstitution.setCounty(String.format("county%s", number));
    aggregateInstitution.setDigitalAddress(String.format("pec%s", number));
    User user = new User();
    user.setId("userId");
    user.setUserMailUuid("mailUuid");
    aggregateInstitution.setUsers(List.of(user));
    return aggregateInstitution;
  }

  AggregateInstitution createAggregateInstitutionAOO_Send(int number) {
    AggregateInstitution aggregateInstitution = createAggregateInstitutionSend(number);
    aggregateInstitution.setSubunitType("AOO");
    aggregateInstitution.setSubunitCode(String.format("code%s", number));
    return aggregateInstitution;
  }

  OnboardingWorkflow createOnboardingWorkflowIO() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-io");
    List<AggregateInstitution> aggregateInstitutionList = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionIO(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    for (int i = 6; i <= 10; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionAOO_IO(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    onboarding.setAggregates(aggregateInstitutionList);

    return new OnboardingWorkflowAggregator(onboarding, "string");
  }

  OnboardingWorkflow createOnboardingWorkflowPagoPa() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-pagopa");
    List<AggregateInstitution> aggregateInstitutionList = new ArrayList<>();

    for (int i = 1; i <= 7; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionPagoPa(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    onboarding.setAggregates(aggregateInstitutionList);

    return new OnboardingWorkflowAggregator(onboarding, "string");
  }


  OnboardingWorkflow createOnboardingWorkflowSend() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-pn");
    List<AggregateInstitution> aggregateInstitutionList = new ArrayList<>();

    for (int i = 1; i <= 4; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionSend(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    for (int i = 5; i <= 7; i++) {
      AggregateInstitution aggregateInstitution = createAggregateInstitutionAOO_Send(i);
      aggregateInstitutionList.add(aggregateInstitution);
    }

    onboarding.setAggregates(aggregateInstitutionList);

    return new OnboardingWorkflowAggregator(onboarding, "string");
  }

  UserResource createDummyUserResource(String id, String userMailUuid) {
    UserResource validManager = new UserResource();
    validManager.setId(UUID.fromString(id));
    CertifiableFieldResourceOfstring emailCert = new CertifiableFieldResourceOfstring();
    emailCert.setValue("email");
    WorkContactResource workContact = new WorkContactResource();
    workContact.setEmail(emailCert);
    Map<String, WorkContactResource> map = new HashMap<>();
    map.put(userMailUuid, workContact);

    validManager.setWorkContacts(map);
    return validManager;
  }

  @Test
  void createContractPDF() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";
    final String productNameAccent = "Interoperabilità";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File contract =
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        productNameAccent,
        PDF_FORMAT_FILENAME);

    assertNotNull(contract);

    ArgumentCaptor<String> captorFilename = ArgumentCaptor.forClass(String.class);
    verify(azureBlobClient, times(1)).uploadFile(any(), captorFilename.capture(), any());
    assertEquals("Interoperabilita_accordo_adesione.pdf", captorFilename.getValue());
  }

  @Test
  void createAttachmentPDF() {
    final String contractFilepath = "attachment";
    final String contractHtml = "attachment";
    final String productNameAccent = "Interoperabilità";
    final String pdfFormatFile = "checklist";

    Onboarding onboarding = createOnboarding();

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    UserResource userResource = createUserResource();

    File attachmentPDF =
      contractService.createAttachmentPDF(
        contractFilepath, onboarding, productNameAccent, pdfFormatFile, userResource);

    assertNotNull(attachmentPDF);

    ArgumentCaptor<String> captorFilename = ArgumentCaptor.forClass(String.class);
    verify(azureBlobClient, times(1)).uploadFile(any(), captorFilename.capture(), any());
    assertEquals("Interoperabilita_checklist.pdf", captorFilename.getValue());
  }

  @Test
  void createContractPDFSA() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.SA);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME));
  }

  @Test
  void createContractPDFForECAndProdPagoPA() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
    onboarding.setProductId("prod-pagopa");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME));
  }

  @Test
  void createContractPDFForPRV_PF() {
    // given
    final String contractFilepath = "contract";
    final String contractHtml = "contract";
    final String institutionTaxCodeUuid = UUID.randomUUID().toString();
    final String realFiscalCode = "RSSMRA80A01H501U";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

    onboarding.getInstitution().setInstitutionType(InstitutionType.PRV_PF);
    onboarding.getInstitution().setTaxCode(institutionTaxCodeUuid);
    onboarding.getInstitution().setOriginId("originalOriginId");

    UserResource userResourceFromRegistry = createUserResource();
    userResourceFromRegistry.setFiscalCode(realFiscalCode);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);
    Mockito.when(userRegistryApi.findByIdUsingGET(any(), eq(institutionTaxCodeUuid)))
      .thenReturn(userResourceFromRegistry);

    // when
    File result = contractService.createContractPDF(
      contractFilepath,
      onboarding,
      manager,
      List.of(),
      PRODUCT_NAME_EXAMPLE,
      PDF_FORMAT_FILENAME);

    // then
    assertNotNull(result);

    Mockito.verify(userRegistryApi, times(1))
      .findByIdUsingGET(any(), eq(institutionTaxCodeUuid));

    assertEquals(realFiscalCode, onboarding.getInstitution().getTaxCode());
    assertEquals(realFiscalCode, onboarding.getInstitution().getOriginId());

    Mockito.verify(azureBlobClient, times(1)).getFileAsText(contractFilepath);
    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"prod-io", "prod-io-sign"})
  void createContractPDFForProdIo(String productId) {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PA);
    onboarding.setProductId(productId);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME));
  }

  @Test
  void createContractPDFAndSigned() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());

    pagoPaSignatureConfig = Mockito.spy(this.pagoPaSignatureConfig);
    when(pagoPaSignatureConfig.source()).thenReturn("local");
    contractService =
      new ContractServiceDefault(
        azureStorageConfig,
        azureBlobClient,
        padesSignService,
        pagoPaSignatureConfig,
        mailTemplatePlaceholdersConfig,
        "logo-path",
        true,
        userRegistryApi);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);

    Mockito.doNothing().when(padesSignService).padesSign(any(), any(), any());

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME));
  }

  @Test
  void loadContractPDF() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();

    File pdf =
      new File(
        Objects.requireNonNull(
            getClass().getClassLoader().getResource("application.properties"))
          .getFile());

    Mockito.when(azureBlobClient.getFileAsPdf(contractFilepath)).thenReturn(pdf);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    assertNotNull(
      contractService.loadContractPDF(
        contractFilepath, onboarding.getId(), PRODUCT_NAME_EXAMPLE));
  }

  @Test
  void retrieveContractNotSigned() {

    Onboarding onboarding = createOnboarding();
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowInstitution();
    onboardingWorkflow.setOnboarding(onboarding);

    File pdf = mock(File.class);
    Mockito.when(azureBlobClient.getFileAsPdf(any())).thenReturn(pdf);

    contractService.retrieveContractNotSigned(onboardingWorkflow, PRODUCT_NAME_EXAMPLE);

    ArgumentCaptor<String> filepathActual = ArgumentCaptor.forClass(String.class);
    Mockito.verify(azureBlobClient, times(1)).getFileAsPdf(filepathActual.capture());
    assertTrue(filepathActual.getValue().contains(onboarding.getId()));
    assertTrue(filepathActual.getValue().contains(PRODUCT_NAME_EXAMPLE));
  }

  @Test
  void getLogoFile() {
    Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn("example");

    contractService.getLogoFile();

    Mockito.verify(azureBlobClient, times(1)).getFileAsText(any());
  }

  @Test
  void uploadCsvAggregatesIO() {
    final String contractHtml = "contract";

    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowIO();

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesPagoPa() {
    final String contractHtml = "contract";

    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowPagoPa();

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesProdPn() {
    final String contractHtml = "contract";

    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowSend();

    UserResource userResource = new UserResource();
    CertifiableFieldResourceOfstring name = new CertifiableFieldResourceOfstring();
    name.setValue("name");
    CertifiableFieldResourceOfstring familyName = new CertifiableFieldResourceOfstring();
    familyName.setValue("familyName");
    String fiscalCode = "fiscalCode";
    CertifiableFieldResourceOfstring email = new CertifiableFieldResourceOfstring();
    email.setValue("email");
    WorkContactResource workContactResource = new WorkContactResource();
    workContactResource.setEmail(email);
    Map<String, WorkContactResource> workContacts = new HashMap<>();
    workContacts.put("mailUuid", workContactResource);
    userResource.setName(name);
    userResource.setFamilyName(familyName);
    userResource.setFiscalCode(fiscalCode);
    userResource.setWorkContacts(workContacts);

    when(userRegistryApi.findByIdUsingGET(anyString(), any()))
      .thenReturn(userResource);

    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    contractService.uploadAggregatesCsv(onboardingWorkflow);

    Mockito.verify(azureBlobClient, times(1)).uploadFile(any(), any(), any());
  }

  @Test
  void uploadCsvAggregatesProductNotValid() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-interop");
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowAggregator(onboarding, "string");

    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> contractService.uploadAggregatesCsv(onboardingWorkflow));
  }

  @Test
  void createContractPRV() {
    // given
    String contractFilepath = "contract";
    String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PRV);
    onboarding.setProductId("prod-pagopa");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    // when
    File result =
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME);

    // then
    assertNotNull(result);
    Mockito.verify(azureBlobClient, Mockito.times(1)).getFileAsText(contractFilepath);
    Mockito.verify(azureBlobClient, Mockito.times(1)).uploadFile(any(), any(), any());
    Mockito.verifyNoMoreInteractions(azureBlobClient);
  }


  @Test
  void createContractTestcaseDashboardPsp() {
    // given
    String contractFilepath = "contract";
    String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
      createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PSP);
    onboarding.setProductId("prod-dashboard-psp");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    // when
    File result =
      contractService.createContractPDF(
        contractFilepath,
        onboarding,
        manager,
        List.of(),
        PRODUCT_NAME_EXAMPLE,
        PDF_FORMAT_FILENAME);

    // then
    assertNotNull(result);
    Mockito.verify(azureBlobClient, Mockito.times(1)).getFileAsText(contractFilepath);
    Mockito.verify(azureBlobClient, Mockito.times(1)).uploadFile(any(), any(), any());
    Mockito.verifyNoMoreInteractions(azureBlobClient);
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
  void deleteContractTest() throws IOException {
    // given
    String originalContractPath = "parties/docs/123/contract.pdf";
    String expectedDeletedPath = "parties/deleted/123/contract.pdf";

    Token token = new Token();
    token.setContractSigned(originalContractPath);

    ClassLoader classLoader = getClass().getClassLoader();
    String resourcePath = Objects.requireNonNull(classLoader.getResource("documents/test.pdf")).getPath();
    File contract = new File(resourcePath);
    byte[] contractBytes = Files.readAllBytes(contract.toPath());

    // Mock specifici con parametri esatti
    when(azureBlobClient.retrieveFile(eq(originalContractPath))).thenReturn(contract);
    when(azureBlobClient.uploadFilePath(eq(expectedDeletedPath), eq(contractBytes))).thenReturn(expectedDeletedPath);
    doNothing().when(azureBlobClient).removeFile(eq(originalContractPath));

    // when
    String deletedContractSigned = contractService.deleteContract(token.getContractSigned(), true);

    // then
    assertNotNull(deletedContractSigned);
    assertEquals(expectedDeletedPath, deletedContractSigned);
    assertTrue(deletedContractSigned.startsWith("parties/deleted"));

    InOrder inOrder = inOrder(azureBlobClient);
    inOrder.verify(azureBlobClient).retrieveFile(eq(originalContractPath));
    inOrder.verify(azureBlobClient).uploadFilePath(eq(expectedDeletedPath), eq(contractBytes));
    inOrder.verify(azureBlobClient).removeFile(eq(originalContractPath));

    verifyNoMoreInteractions(azureBlobClient);
  }

  @Test
  void createContractIdPayMerchant() throws IOException {
    // given
    String contractFilepath = getDummyTemplate();
    String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager =
            createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PRV);
    onboarding.setProductId("prod-idpay-merchant");

    Payment payment = new Payment();

    payment.setHolder(DataEncryptionUtils.encrypt("test"));
    payment.setIban(DataEncryptionUtils.encrypt("IT12Z0300203280366182987462"));

    onboarding.setPayment(payment);

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractFilepath);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    // when
    File result =
            contractService.createContractPDF(
                    contractFilepath,
                    onboarding,
                    manager,
                    List.of(),
                    PRODUCT_NAME_EXAMPLE,
                    PDF_FORMAT_FILENAME);

    // then
    assertNotNull(result);
    Mockito.verify(azureBlobClient, Mockito.times(1)).getFileAsText(contractFilepath);
    Mockito.verify(azureBlobClient, Mockito.times(1)).uploadFile(any(), any(), any());
    Mockito.verifyNoMoreInteractions(azureBlobClient);
    Files.deleteIfExists(result.toPath());
  }

  @Test
  void createContractPDFForPspAndPagoPa() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.getInstitution().setInstitutionType(InstitutionType.PSP);
    onboarding.setProductId("prod-pagopa");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File result = contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME);

    assertNotNull(result);
    // Verify permissions if POSIX
    verifyFilePermissions(result);
  }

  @Test
  void createContractPDFForProdIO() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.setProductId("prod-io");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File result = contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME);

    assertNotNull(result);
    verifyFilePermissions(result);
  }

  @Test
  void createContractPDFForProdPN() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.setProductId("prod-pn");
    onboarding.setBilling(new Billing());

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File result = contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME);

    assertNotNull(result);
    verifyFilePermissions(result);
  }

  @Test
  void createContractPDFForProdInterop() {
    final String contractFilepath = "contract";
    final String contractHtml = "contract";

    Onboarding onboarding = createOnboarding();
    User userManager = onboarding.getUsers().get(0);
    UserResource manager = createDummyUserResource(userManager.getId(), userManager.getUserMailUuid());
    onboarding.setProductId("prod-interop");

    Mockito.when(azureBlobClient.getFileAsText(contractFilepath)).thenReturn(contractHtml);
    Mockito.when(azureBlobClient.uploadFile(any(), any(), any())).thenReturn(contractHtml);

    File result = contractService.createContractPDF(
            contractFilepath,
            onboarding,
            manager,
            List.of(),
            PRODUCT_NAME_EXAMPLE,
            PDF_FORMAT_FILENAME);

    assertNotNull(result);
    verifyFilePermissions(result);
  }

  @Test
  void createSafeTempFile() throws IOException {
    String prefix = "testPrefix";
    String suffix = ".txt";
    Path tempFile = ((ContractServiceDefault) contractService).createSafeTempFile(prefix, suffix);

    assertTrue(tempFile.getFileName().toString().startsWith(prefix));
    assertTrue(tempFile.getFileName().toString().endsWith(suffix));
    assertTrue(Files.exists(tempFile));

    try {
      if (Files.getFileStore(tempFile).supportsFileAttributeView("posix")) {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(tempFile);
        assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
        assertFalse(permissions.contains(PosixFilePermission.GROUP_READ));
        assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
      }
    } catch (IOException e) {
      // Fallback for non-POSIX systems, check readable/writable/executable
      File f = tempFile.toFile();
      assertTrue(f.canRead());
      assertTrue(f.canWrite());
      assertFalse(f.canExecute());
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  @Test
  void createSafeTempFileUnsupportedOperationException() throws IOException {
    String prefix = "testPrefix";
    String suffix = ".txt";

    ContractServiceDefault serviceSpy = spy((ContractServiceDefault) contractService);
    doThrow(new UnsupportedOperationException("forced"))
            .when(serviceSpy).createTempFileWithPosix(anyString(), anyString());

    Path tempFile = serviceSpy.createSafeTempFile(prefix, suffix);

    assertTrue(tempFile.getFileName().toString().startsWith(prefix));
    assertTrue(tempFile.getFileName().toString().endsWith(suffix));
    assertTrue(Files.exists(tempFile));

    File f = tempFile.toFile();
    assertTrue(f.canRead());
    assertTrue(f.canWrite());

    Files.deleteIfExists(tempFile);
  }

  private void verifyFilePermissions(File file) {
    Path path = file.toPath();
    try {
      if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
        assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
        // Ensure no other permissions are set (group/others)
        assertEquals(2, permissions.size(), "File should only have owner read/write permissions: " + permissions);
      }
    } catch (IOException e) {
      // Ignore if not supported
    }
  }

  String getDummyTemplate() throws IOException {

    FileInputStream fis = new FileInputStream("src/test/resources/fn/dummy-accordo_di_adesione.html");
    return IOUtils.toString(fis, StandardCharsets.UTF_8);

  }

}
