package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.registry.proxy.runner.client.DataSourceRestClient;
import jakarta.inject.Inject;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Slf4j
public abstract class AbstractIpaOpenDataService<T> {

  @Inject @RestClient protected DataSourceRestClient<T> ipaOpenDataRestClient;

  public abstract List<T> fetch();

  protected List<T> parseCsv(String csv, Class<T> type, String entityName) {
    try (Reader reader =
        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(csv.getBytes())))) {
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
