package it.pagopa.selfcare.onboarding.service;

import it.pagopa.selfcare.azurestorage.AzureBlobClient;
import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.document_json.api.DocumentContentControllerApi;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static it.pagopa.selfcare.onboarding.service.OnboardingService.USERS_WORKS_FIELD_LIST;
import static it.pagopa.selfcare.onboarding.utils.GenericError.*;

@Slf4j
@ApplicationScoped
public class ContractServiceDefault implements ContractService {

  private final UserApi userRegistryApi;
  private final AzureBlobClient azureBlobClient;
  private final String logoPath;
  private final boolean isLogoEnable;

  private static final String INSTITUTION_DESCRIPTION_HEADER = "Ragione Sociale";
  private static final String PEC_HEADER = "PEC";
  private static final String FISCAL_CODE_HEADER = "Codice Fiscale";
  private static final String PIVA_HEADER = "P.IVA";
  private static final String REGISTERED_OFFICE_ADDRESS = "Sede legale - Indirizzo";
  private static final String REGISTERED_OFFICE_CITY = "Sede legale - Citta'";
  private static final String REGISTERED_OFFICE_COUNTY = "Sede legale - Provincia (Sigla)";
  private static final String DATE_PATTERN_YYYY_M_MDD_H_HMMSS = "yyyyMMddHHmmss";

  public ContractServiceDefault(
            AzureBlobClient azureBlobClient,
            @ConfigProperty(name = "onboarding-functions.logo-path") String logoPath,
            @ConfigProperty(name = "onboarding-functions.logo-enable") Boolean isLogoEnable,
            @RestClient UserApi userRegistryApi) {
        this.azureBlobClient = azureBlobClient;
        this.logoPath = logoPath;
        this.isLogoEnable = isLogoEnable;
        this.userRegistryApi = userRegistryApi;
    }

  private static final String[] CSV_HEADERS_IO = {
    INSTITUTION_DESCRIPTION_HEADER,
    PEC_HEADER,
    FISCAL_CODE_HEADER,
    PIVA_HEADER,
    REGISTERED_OFFICE_ADDRESS,
    REGISTERED_OFFICE_CITY,
    REGISTERED_OFFICE_COUNTY,
    "Codice IPA",
    "AOO/UO",
    "Codice Univoco"
  };

  private static final String[] CSV_HEADERS_PAGOPA = {
    INSTITUTION_DESCRIPTION_HEADER,
    PEC_HEADER,
    FISCAL_CODE_HEADER,
    PIVA_HEADER,
    REGISTERED_OFFICE_ADDRESS,
    REGISTERED_OFFICE_CITY,
    REGISTERED_OFFICE_COUNTY,
    "IBAN"
  };

  private static final String[] CSV_HEADERS_SEND = {
    INSTITUTION_DESCRIPTION_HEADER,
    PEC_HEADER,
    FISCAL_CODE_HEADER,
    PIVA_HEADER,
    "Codice SDI",
    REGISTERED_OFFICE_ADDRESS,
    REGISTERED_OFFICE_CITY,
    REGISTERED_OFFICE_COUNTY,
    "Codice IPA",
    "AOO/UO",
    "Codice Univoco",
    "Nome Amministratore Ente Aggregato",
    "Cognome Amministratore Ente Aggregato",
    "Codice Fiscale Amministratore Ente Aggregato",
    "email Amministratore Ente Aggregato"
  };

  private static final String LEGAL_SENTENCE_IO =
    "*** Il presente file non puo' essere modificato se non unitamente al "
      + "documento \"Allegato 3\" in cui e' incoporato. Ogni modifica, alterazione e variazione dei dati e delle "
      + "informazioni del presente file non accompagnata dall'invio e dalla firma digitale dell'intero documento "
      + "\"Allegato 3\" e' da considerarsi priva di ogni efficacia ai sensi di legge e ai fini del presente Accordo. "
      + "In caso di discrepanza tra i dati contenuti nel presente file e i dati contenuti nell'Allegato 3, "
      + "sara' data prevalenza a questi ultimi.";

  private static final Function<AggregateInstitution, List<Object>> IO_MAPPER =
    institution ->
      Arrays.asList(
        institution.getDescription(),
        institution.getDigitalAddress(),
        institution.getTaxCode(),
        institution.getVatNumber(),
        institution.getAddress(),
        institution.getCity(),
        institution.getCounty(),
        Optional.ofNullable(institution.getSubunitType())
          .map(originId -> "")
          .orElse(institution.getOriginId()),
        institution.getSubunitType(),
        institution.getSubunitCode());

  private static final Function<AggregateInstitution, List<Object>> PAGOPA_MAPPER =
    institution ->
      Arrays.asList(
        institution.getDescription(),
        institution.getDigitalAddress(),
        institution.getTaxCode(),
        institution.getVatNumber(),
        institution.getAddress(),
        institution.getCity(),
        institution.getCounty(),
        institution.getIban());

  public static Function<AggregateInstitution, List<Object>> sendMapper(UserResource userInfo, User user) {
    return institution -> Arrays.asList(
      institution.getDescription(),
      institution.getDigitalAddress(),
      institution.getTaxCode(),
      institution.getVatNumber(),
      institution.getRecipientCode(),
      institution.getAddress(),
      institution.getCity(),
      institution.getCounty(),
      Optional.ofNullable(institution.getSubunitType())
        .map(originId -> "")
        .orElse(institution.getOriginId()),
      institution.getSubunitType(),
      institution.getSubunitCode(),
      userInfo.getName().getValue(),
      userInfo.getFamilyName().getValue(),
      userInfo.getFiscalCode(),
      userInfo.getWorkContacts().get(user.getUserMailUuid()).getEmail().getValue()
    );
  }

  @Override
  public Optional<File> getLogoFile() {
    if (isLogoEnable) {

      StringBuilder stringBuilder =
        new StringBuilder(
          LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN_YYYY_M_MDD_H_HMMSS)));
      stringBuilder.append("_").append(UUID.randomUUID()).append("_logo");
      try {
        Path path = createSafeTempFile(stringBuilder.toString(), ".png");
        Files.writeString(path, azureBlobClient.getFileAsText(logoPath));
        return Optional.of(path.toFile());
      } catch (IOException e) {
        throw new IllegalArgumentException(
          String.format(UNABLE_TO_DOWNLOAD_FILE.getMessage(), logoPath));
      }
    }

    return Optional.empty();
  }

  @Override
  public DocumentContentControllerApi.UploadAggregatesCsvMultipartForm requestUploadAggregatesCsv(OnboardingWorkflow onboardingWorkflow) {
    try {
      Onboarding onboarding = onboardingWorkflow.getOnboarding();
      Path filePath = createSafeTempFile("tempfile", ".csv");
      InputStream csv = generateAggregatesCsv(onboarding.getProductId(), onboarding.getAggregates(), filePath);
      DocumentContentControllerApi.UploadAggregatesCsvMultipartForm request =
          new DocumentContentControllerApi.UploadAggregatesCsvMultipartForm();
      request.onboardingId = onboardingWorkflow.getOnboarding().getId();
      request.productId = onboardingWorkflow.getOnboarding().getProductId();
      request.csv = csv;
      return request;
    } catch (IOException e) {
      throw new GenericOnboardingException(
        String.format(LOAD_AGGREGATES_CSV_ERROR.getMessage(), e.getMessage()));
    }
  }

  private InputStream generateAggregatesCsv(
    String productId, List<AggregateInstitution> institutions, Path filePath) throws FileNotFoundException {
    String[] headers;
    Function<AggregateInstitution, List<Object>> mapper;
    String legalSentence = null;

    // Determine headers and mapping logic based on productId
    switch (productId) {
      case "prod-io":
        headers = CSV_HEADERS_IO;
        mapper = IO_MAPPER;
        legalSentence = LEGAL_SENTENCE_IO;
        break;
      case "prod-pagopa":
        headers = CSV_HEADERS_PAGOPA;
        mapper = PAGOPA_MAPPER;
        break;
      case "prod-pn":
        headers = CSV_HEADERS_SEND;
        mapper = institution -> {
          List<Object> records = new ArrayList<>();
          for (User user : institution.getUsers()) {
            UserResource userInfo = userRegistryApi.findByIdUsingGET(USERS_WORKS_FIELD_LIST, user.getId());
            records.addAll(sendMapper(userInfo, user).apply(institution));
          }
          return records;
        };
        break;
      default:
        throw new IllegalArgumentException(
          String.format("Product %s is not available for aggregators", productId));
    }
    return createAggregatesCsv(institutions, filePath, headers, mapper, legalSentence);
  }

  private InputStream createAggregatesCsv(
    List<AggregateInstitution> institutions,
    Path filePath,
    String[] headers,
    Function<AggregateInstitution, List<Object>> mapper,
    String legalSentence) throws FileNotFoundException {

    File csvFile = filePath.toFile();

    // Using the builder pattern to create the CSV format with headers
    CSVFormat csvFormat =
      CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader(headers).setDelimiter(';').build();

    try (FileWriter writer = new FileWriter(csvFile);
         CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

      // Iterate over each AggregateInstitution object and write a row for each one
      for (AggregateInstitution institution : institutions) {
        csvPrinter.printRecord(mapper.apply(institution));
      }

      // Add the final legal sentence at the last row in case of prod-io
      csvPrinter.println();
      csvPrinter.printRecord(legalSentence);

    } catch (IOException e) {
      throw new GenericOnboardingException(
        String.format(CREATE_AGGREGATES_CSV_ERROR.getMessage(), e.getMessage()));
    }
    return new FileInputStream(csvFile);
  }

  Path createSafeTempFile(String prefix, String suffix) throws IOException {
    try {
      return createTempFileWithPosix(prefix, suffix);
    } catch (UnsupportedOperationException e) {
      // Fallback per sistemi non-POSIX (es. Windows in locale)
      File f = Files.createTempFile(prefix, suffix).toFile();

      boolean readable = f.setReadable(true, true); // true = leggibile, true = solo owner
      boolean writable = f.setWritable(true, true); // true = scrivibile, true = solo owner
      boolean executable = f.setExecutable(false);  // FIX: false = NON eseguibile (più sicuro)

      if (!readable || !writable || !executable) {
        log.warn("Could not set restricted permissions on temporary file: {}", f.getAbsolutePath());
      }
      return f.toPath();
    }
  }

  Path createTempFileWithPosix(String prefix, String suffix) throws IOException {
    FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(
            PosixFilePermissions.fromString("rw-------")
    );
    return Files.createTempFile(prefix, suffix, attr);
  }
}
