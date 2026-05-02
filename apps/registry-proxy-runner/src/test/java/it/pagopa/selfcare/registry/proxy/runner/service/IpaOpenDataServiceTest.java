package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.registry.proxy.runner.client.IpaOpenDataRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpaOpenDataServiceTest {

  @Mock IpaOpenDataRestClient ipaOpenDataRestClient;

  @InjectMocks IpaOpenDataService ipaOpenDataService;

  @Test
  void fetchInstitutions() {
    String csv = "Codice_fiscale_ente,Denominazione_ente\n123,Inst1\n";
    when(ipaOpenDataRestClient.retrieveInstitutions()).thenReturn(csv);
    List<IpaInstitution> list = ipaOpenDataService.fetchInstitutions();
    assertEquals(1, list.size());
    assertEquals("123", list.get(0).getTaxCode());
  }

  @Test
  void fetchCategories() {
    String csv = "Codice_categoria,Nome_categoria\nCAT1,Cat1\n";
    when(ipaOpenDataRestClient.retrieveCategories()).thenReturn(csv);
    List<IpaCategory> list = ipaOpenDataService.fetchCategories();
    assertEquals(1, list.size());
    assertEquals("CAT1", list.get(0).getCode());
  }

  @Test
  void fetchAOOs() {
    String csv = "Codice_uni_aoo,Denominazione_aoo\nAOO1,Aoo1\n";
    when(ipaOpenDataRestClient.retrieveAOOs()).thenReturn(csv);
    List<IpaAoo> list = ipaOpenDataService.fetchAOOs();
    assertEquals(1, list.size());
    assertEquals("AOO1", list.get(0).getCodiceUniAoo());
  }

  @Test
  void fetchUOs() {
    String uoCsv = "Codice_uni_uo,Descrizione_uo\nUO1,Uo1\nUO2,Uo2\n";
    String sfeCsv = "Codice_uni_uo,Codice_fiscale_sfe\nUO1,SFE1\n";

    when(ipaOpenDataRestClient.retrieveUOs()).thenReturn(uoCsv);
    when(ipaOpenDataRestClient.retrieveUOsWithSfe()).thenReturn(sfeCsv);

    List<IpaUo> list = ipaOpenDataService.fetchUOs();

    assertEquals(2, list.size());
    assertEquals(
        "SFE1",
        list.stream().filter(u -> "UO1".equals(u.getId())).findFirst().get().getCodiceFiscaleSfe());
    assertEquals(
        null,
        list.stream().filter(u -> "UO2".equals(u.getId())).findFirst().get().getCodiceFiscaleSfe());
  }
}
