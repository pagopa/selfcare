package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.registry.proxy.runner.client.IvassRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IvassInsuranceCompany;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class IvassDataService {

  @Inject @RestClient IvassRestClient ivassRestClient;

  @ConfigProperty(name = "ivass.registry-types-admitted")
  List<String> registryTypesAdmitted;

  @ConfigProperty(name = "ivass.work-types-admitted")
  List<String> workTypesAdmitted;

  /**
   * Fetches IVASS insurance companies from the ZIP endpoint, extracts the CSV, parses it, and
   * filters by registry/work types.
   */
  public List<IvassInsuranceCompany> fetch() {
    log.info("Fetching IVASS insurance companies...");
    try {
      byte[] zip = ivassRestClient.retrieveDataSource();
      byte[] csv = extractFirstEntryFromZip(zip);
      csv = removeUtf8Bom(csv);
      List<IvassInsuranceCompany> companies = parseCsv(csv);
      List<IvassInsuranceCompany> filtered = filterCompanies(companies);
      log.info(
          "Fetched {} IVASS insurance companies ({} after filtering)",
          companies.size(),
          filtered.size());
      return filtered;
    } catch (Exception e) {
      log.error("Error fetching IVASS insurance companies", e);
      return Collections.emptyList();
    }
  }

  private List<IvassInsuranceCompany> parseCsv(byte[] csv) {
    try (Reader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv)))) {
      CsvToBean<IvassInsuranceCompany> csvToBean =
          new CsvToBeanBuilder<IvassInsuranceCompany>(reader)
              .withType(IvassInsuranceCompany.class)
              .withSeparator(';')
              .build();
      return csvToBean.parse();
    } catch (Exception e) {
      log.error("Error parsing IVASS CSV", e);
      return Collections.emptyList();
    }
  }

  private List<IvassInsuranceCompany> filterCompanies(List<IvassInsuranceCompany> companies) {
    return companies.stream()
        .filter(c -> c.getDigitalAddress() != null && !c.getDigitalAddress().isBlank())
        .filter(c -> c.getWorkType() != null && workTypesAdmitted.contains(c.getWorkType()))
        .filter(
            c -> {
              if (c.getRegisterType() == null) return false;
              String registerTypePrefix = c.getRegisterType().replaceAll("\\s", "").split("-")[0];
              return registryTypesAdmitted.stream().anyMatch(registerTypePrefix::equals);
            })
        .toList();
  }

  byte[] extractFirstEntryFromZip(byte[] zipBytes) {
    int thresholdSize = 100_000_000; // 100 MB
    double thresholdRatio = 10;

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry = zis.getNextEntry();
      if (entry == null) {
        log.error("No entries found in IVASS zip file");
        return new byte[0];
      }

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[1024];
        int length;
        int totalSize = 0;
        while ((length = zis.read(buffer)) != -1) {
          totalSize += length;
          if ((double) totalSize / entry.getCompressedSize() > thresholdRatio) {
            log.error("Compression ratio exceeds the maximum allowed limit");
            return new byte[0];
          }
          if (totalSize > thresholdSize) {
            log.error("Extracted file size exceeds the maximum allowed limit");
            return new byte[0];
          }
          baos.write(buffer, 0, length);
        }
        return baos.toByteArray();
      }
    } catch (IOException e) {
      log.error("Error extracting file from IVASS zip", e);
      return new byte[0];
    }
  }

  byte[] removeUtf8Bom(byte[] csv) {
    if (csv.length > 3 && csv[0] == (byte) 0xEF && csv[1] == (byte) 0xBB && csv[2] == (byte) 0xBF) {
      return Arrays.copyOfRange(csv, 3, csv.length);
    }
    return csv;
  }
}
