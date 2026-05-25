package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.registry.proxy.runner.client.AnacRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.AnacStation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.*;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
@ApplicationScoped
public class AnacDataService {

  @Inject @RestClient AnacRestClient anacRestClient;
  @Inject AzureBlobStorageService storageService;

  /**
   * Fetches ANAC stations from open data and filters out records that have an originId (same logic
   * as the existing ANACServiceImpl).
   */
  public List<AnacStation> fetch() {
    log.info("Fetching ANAC stations...");
    try {
      String csv = anacRestClient.retrieveDataSource();
      storageService.saveDaily(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), "opendata/anac");
      try (Reader reader =
          new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
        CsvToBean<AnacStation> csvToBean =
            new CsvToBeanBuilder<AnacStation>(reader)
                .withType(AnacStation.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build();
        List<AnacStation> stations =
            csvToBean.parse().stream()
                .filter(station -> station.getOriginId() == null || station.getOriginId().isBlank())
                .toList();
        log.info("Fetched {} ANAC stations (after filtering)", stations.size());
        return stations;
      }
    } catch (Exception e) {
      log.error("Error fetching ANAC stations", e);
      return Collections.emptyList();
    }
  }
}
