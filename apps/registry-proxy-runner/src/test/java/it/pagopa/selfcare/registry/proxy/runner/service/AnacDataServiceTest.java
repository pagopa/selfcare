package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.registry.proxy.runner.client.AnacRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.AnacStation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnacDataServiceTest {

  @Mock AnacRestClient anacRestClient;

  @InjectMocks AnacDataService anacDataService;

  @Test
  void fetchStations() {
    String csv =
        "codiceIPA,codiceFiscaleGestore,denominazioneGestore,PEC,ANAC_incaricato,ANAC_abilitato\n"
            + ",12345678901,Station 1,pec1@test.it,true,true\n"
            + "IPA1,12345678902,Station 2,pec2@test.it,false,false\n"
            + " ,12345678903,Station 3,pec3@test.it,true,false\n";

    when(anacRestClient.retrieveStations()).thenReturn(csv);

    List<AnacStation> stations = anacDataService.fetchStations();

    assertEquals(2, stations.size());
    assertEquals("12345678901", stations.get(0).getTaxCode());
    assertEquals("12345678903", stations.get(1).getTaxCode());
  }

  @Test
  void fetchStations_error() {
    when(anacRestClient.retrieveStations()).thenThrow(new RuntimeException("Error"));
    List<AnacStation> stations = anacDataService.fetchStations();
    assertTrue(stations.isEmpty());
  }
}
