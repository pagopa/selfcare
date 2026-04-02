package it.pagopa.selfcare.onboarding.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.*;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class ContractServiceDefaultTest {

  @InjectMock @RestClient UserApi userRegistryApi;

  @InjectMock AzureBlobClient azureBlobClient;

  @Inject ContractService contractService;

  static final String LOGO_PATH = "logo-path";

  @BeforeEach
  void setup() {
    contractService = new ContractServiceDefault(azureBlobClient, LOGO_PATH, true, userRegistryApi);
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

  @Test
  void getLogoFile() {
    Mockito.when(azureBlobClient.getFileAsText(any())).thenReturn("example");

    contractService.getLogoFile();

    Mockito.verify(azureBlobClient, times(1)).getFileAsText(any());
  }

  @Test
  void uploadCsvAggregatesIO() throws Exception {
    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowIO();

    DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request =
      contractService.requestUploadAggregatesCsv(onboardingWorkflow);

    List<String> nonBlankLines = readCsvLines(request.csv).stream().filter(line -> !line.isBlank()).toList();
    assertEquals(
      "Ragione Sociale;PEC;Codice Fiscale;P.IVA;Sede legale - Indirizzo;Sede legale - Citta';Sede legale - Provincia (Sigla);Codice IPA;AOO/UO;Codice Univoco",
      nonBlankLines.get(0));
    assertEquals(onboardingWorkflow.getOnboarding().getAggregates().size() + 2, nonBlankLines.size());

    String[] firstInstitution = nonBlankLines.get(1).split(";", -1);
    assertEquals("description1", firstInstitution[0]);
    assertEquals("pec1", firstInstitution[1]);
    assertEquals("originId1", firstInstitution[7]);
    assertEquals("", firstInstitution[8]);
    assertEquals("", firstInstitution[9]);

    String subunitRow =
      nonBlankLines.stream().filter(line -> line.contains("description6")).findFirst().orElseThrow();
    String[] subunitInstitution = subunitRow.split(";", -1);
    assertEquals("", subunitInstitution[7]);
    assertEquals("AOO", subunitInstitution[8]);
    assertEquals("code6", subunitInstitution[9]);

    String legalRow = nonBlankLines.get(nonBlankLines.size() - 1);
    assertTrue(legalRow.startsWith("\"*** Il presente file"));
  }

  @Test
  void uploadCsvAggregatesPagoPa() throws Exception {
    OnboardingWorkflow onboardingWorkflow = createOnboardingWorkflowPagoPa();

    DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request =
      contractService.requestUploadAggregatesCsv(onboardingWorkflow);

    List<String> nonBlankLines = readCsvLines(request.csv).stream().filter(line -> !line.isBlank()).toList();
    assertEquals(
      "Ragione Sociale;PEC;Codice Fiscale;P.IVA;Sede legale - Indirizzo;Sede legale - Citta';Sede legale - Provincia (Sigla);IBAN",
      nonBlankLines.get(0));
    assertEquals(onboardingWorkflow.getOnboarding().getAggregates().size() + 1, nonBlankLines.size());

    String[] firstInstitution = nonBlankLines.get(1).split(";", -1);
    assertEquals("description1", firstInstitution[0]);
    assertEquals("iban1", firstInstitution[7]);
  }

  @Test
  void uploadCsvAggregatesProdPn() throws Exception {
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

    when(userRegistryApi.findByIdUsingGET(anyString(), any())).thenReturn(userResource);

    DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request =
      contractService.requestUploadAggregatesCsv(onboardingWorkflow);

    List<String> nonBlankLines = readCsvLines(request.csv).stream().filter(line -> !line.isBlank()).toList();
    assertEquals(
      "Ragione Sociale;PEC;Codice Fiscale;P.IVA;Codice SDI;Sede legale - Indirizzo;Sede legale - Citta';Sede legale - Provincia (Sigla);Codice IPA;AOO/UO;Codice Univoco;Nome Amministratore Ente Aggregato;Cognome Amministratore Ente Aggregato;Codice Fiscale Amministratore Ente Aggregato;email Amministratore Ente Aggregato",
      nonBlankLines.get(0));
    assertEquals(onboardingWorkflow.getOnboarding().getAggregates().size() + 1, nonBlankLines.size());

    String[] firstInstitution = nonBlankLines.get(1).split(";", -1);
    assertEquals("recipientCode1", firstInstitution[4]);
    assertEquals("originId1", firstInstitution[8]);
    assertEquals("name", firstInstitution[11]);
    assertEquals("familyName", firstInstitution[12]);
    assertEquals("fiscalCode", firstInstitution[13]);
    assertEquals("email", firstInstitution[14]);

    String subunitRow =
      nonBlankLines.stream().filter(line -> line.contains("description5")).findFirst().orElseThrow();
    String[] subunitInstitution = subunitRow.split(";", -1);
    assertEquals("", subunitInstitution[8]);
    assertEquals("AOO", subunitInstitution[9]);
    assertEquals("code5", subunitInstitution[10]);

    verify(userRegistryApi, times(onboardingWorkflow.getOnboarding().getAggregates().size()))
      .findByIdUsingGET(anyString(), any());
  }

  @Test
  void uploadCsvAggregatesProductNotValid() {
    Onboarding onboarding = createOnboarding();
    onboarding.setProductId("prod-interop");
    OnboardingWorkflow onboardingWorkflow = new OnboardingWorkflowAggregator(onboarding, "string");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> contractService.requestUploadAggregatesCsv(onboardingWorkflow));
  }

  @Test
  void createSafeTempFileUnsupportedOperationException() throws IOException {
    String prefix = "testPrefix";
    String suffix = ".txt";

    ContractServiceDefault serviceSpy = spy((ContractServiceDefault) contractService);
    doThrow(new UnsupportedOperationException("forced"))
        .when(serviceSpy)
        .createTempFileWithPosix(anyString(), anyString());

    Path tempFile = serviceSpy.createSafeTempFile(prefix, suffix);

    assertTrue(tempFile.getFileName().toString().startsWith(prefix));
    assertTrue(tempFile.getFileName().toString().endsWith(suffix));
    assertTrue(Files.exists(tempFile));

    File f = tempFile.toFile();
    assertTrue(f.canRead());
    assertTrue(f.canWrite());

    Files.deleteIfExists(tempFile);
  }

  private List<String> readCsvLines(InputStream csv) throws IOException {
    return new String(csv.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
  }
}
