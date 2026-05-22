package it.pagopa.selfcare.registry.proxy.runner.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractIpaOpenDataService<T> {

  public abstract List<T> fetch();

  protected List<T> parseCsv(String csv, Class<T> type, String entityName) {
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
