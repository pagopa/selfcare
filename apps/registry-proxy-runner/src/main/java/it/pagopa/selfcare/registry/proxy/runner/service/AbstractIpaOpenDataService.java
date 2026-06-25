package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.inject.Inject;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractIpaOpenDataService<T> {

  @Inject AzureBlobStorageService storageService;

  public abstract List<T> fetch();

  /**
   * Parses the raw CSV string into a list of entities and saves a daily snapshot to Azure Blob
   * Storage under {@code opendata/<blobPrefix>}.
   */
  protected List<T> parseCsv(String csv, Class<T> type, String entityName, String blobPrefix) {
    storageService.saveDaily(csv.getBytes(StandardCharsets.UTF_8), "opendata/" + blobPrefix);
    try (Reader reader = new StringReader(csv)) {
      CsvToBean<T> csvToBean =
          new CsvToBeanBuilder<T>(reader).withType(type).withIgnoreLeadingWhiteSpace(true).build();
      List<T> result = csvToBean.parse();
      log.info("Fetched {} {} from IPA open data", result.size(), entityName);
      return result;
    } catch (Exception e) {
      log.error("Error fetching IPA {} from open data", entityName, e);
      return Collections.emptyList();
    }
  }
}
