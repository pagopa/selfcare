package it.pagopa.selfcare.party.registry_proxy.connector.rest.writer;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IpaSearchServiceConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiSearchInstitutionIndexWriterServiceTest {

  private final IpaSearchServiceConnector connector = mock(IpaSearchServiceConnector.class);
  private final AiSearchInstitutionIndexWriterService service =
      new AiSearchInstitutionIndexWriterService(connector);

  private Institution upToDateInstitution() {
    Institution institution = mock(Institution.class);
    when(institution.getId()).thenReturn("id1");
    when(institution.getUpdateDate()).thenReturn("2026-01-01");
    when(connector.fetchById("id1")).thenReturn("2026-01-01");
    return institution;
  }

  @Test
  void adds_skipsUpToDateInstitutions() {
    service.adds(List.of(upToDateInstitution()));

    verify(connector, never()).index(anyList());
  }

  @Test
  void addsForced_indexesUpToDateInstitutions() {
    Institution institution = upToDateInstitution();

    service.addsForced(List.of(institution));

    verify(connector, times(1)).index(List.of(institution));
    verify(connector, never()).fetchById(any());
  }
}
