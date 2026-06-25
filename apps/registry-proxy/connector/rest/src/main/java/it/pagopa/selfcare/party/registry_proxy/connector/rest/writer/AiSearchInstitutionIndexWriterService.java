package it.pagopa.selfcare.party.registry_proxy.connector.rest.writer;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IpaSearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("aiSearchInstitutionIndexWriterService")
public class AiSearchInstitutionIndexWriterService implements IndexWriterService<Institution> {

  private final IpaSearchServiceConnector ipaSearchServiceConnector;

  @Autowired
  public AiSearchInstitutionIndexWriterService(
      IpaSearchServiceConnector ipaSearchServiceConnector) {
    log.trace("Initializing {}", AiSearchInstitutionIndexWriterService.class.getSimpleName());
    this.ipaSearchServiceConnector = ipaSearchServiceConnector;
  }

  @Override
  public void adds(List<? extends Institution> items) {
    log.trace("adds start");
    log.debug("adds items = {}", items.size());
    List<Institution> institutions = (List<Institution>) items;

    if (institutions.isEmpty()) {
      log.warn(
          "adds doInstitutionsIndex: no institutions retrieved from IPA open data, skipping AI Search index update");
      return;
    }

    List<Institution> toIndex = institutions.stream().filter(this::needsUpdate).toList();

    if (toIndex.isEmpty()) {
      log.info("adds doInstitutionsIndex: AI Search IPA institution index is already up to date");
      return;
    }

    log.info("adds doInstitutionsIndex: indexing {} institutions (new or updated)", toIndex.size());
    int batchSize = 1000;
    for (int i = 0; i < toIndex.size(); i += batchSize) {
      List<Institution> batch =
          toIndex.subList(i, Math.min(i + batchSize, toIndex.size())).stream()
              .filter(is -> is.getId() != null && !is.getId().isBlank())
              .toList();

      ipaSearchServiceConnector.index(batch);
    }
    log.trace("adds end");
  }

  private boolean needsUpdate(Institution institution) {
    String currentUpdateDate = ipaSearchServiceConnector.fetchById(institution.getId());
    if (currentUpdateDate == null) {
      return true;
    }
    String csvUpdateDate = institution.getUpdateDate();
    if (csvUpdateDate == null || csvUpdateDate.isBlank()) {
      return true;
    }
    return !currentUpdateDate.equals(csvUpdateDate);
  }

  @Override
  public void deleteAll() {
    log.trace("deleteAll - not supported for AI Search writer");
  }

  @Override
  public void cleanIndex(String entityType) {
    log.trace("cleanIndex - not supported for AI Search writer");
  }

  @Override
  public void updateDocumentValues(Institution item, Map<String, String> fieldsToUpdate) {
    log.trace("updateDocumentValues - not supported for AI Search writer");
  }
}
