package it.pagopa.selfcare.registry.proxy.runner.service;
import it.pagopa.selfcare.registry.proxy.runner.client.*;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaAoo;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaCategory;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaInstitution;
import it.pagopa.selfcare.registry.proxy.runner.model.IpaUo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class IpaOpenDataServiceTest {
    @Mock
    IpaInstitutionOpenDataRestClient institutionRestClient;
    @Mock
    IpaCategoriesOpenDataRestClient categoryRestClient;
    @Mock
    IpaAOOOpenDataRestClient aooRestClient;
    @Mock
    IpaUOOpenDataRestClient uoRestClient;
    @Mock
    IpaUOSfeOpenDataRestClient uoSfeRestClient;
    @InjectMocks
    IpaInstitutionOpenDataService ipaInstitutionOpenDataService;
    @InjectMocks
    IpaCategoryOpenDataService ipaCategoryOpenDataService;
    @InjectMocks
    IpaAooOpenDataService ipaAooOpenDataService;
    @InjectMocks
    IpaUoOpenDataService ipaUoOpenDataService;
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ipaInstitutionOpenDataService, "restClient", institutionRestClient);
        ReflectionTestUtils.setField(ipaCategoryOpenDataService, "restClient", categoryRestClient);
        ReflectionTestUtils.setField(ipaAooOpenDataService, "restClient", aooRestClient);
        ReflectionTestUtils.setField(ipaUoOpenDataService, "restClient", uoRestClient);
        ReflectionTestUtils.setField(ipaUoOpenDataService, "sfeRestClient", uoSfeRestClient);
    }
    @Test
    void fetchInstitutions() {
        String csv = "Codice_fiscale_ente,Denominazione_ente\n123,Inst1\n";
        when(institutionRestClient.retrieveDataSource()).thenReturn(csv);
        List<IpaInstitution> list = ipaInstitutionOpenDataService.fetch();
        assertEquals(1, list.size());
        assertEquals("123", list.get(0).getTaxCode());
    }
    @Test
    void fetchCategories() {
        String csv = "Codice_categoria,Nome_categoria\nCAT1,Cat1\n";
        when(categoryRestClient.retrieveDataSource()).thenReturn(csv);
        List<IpaCategory> list = ipaCategoryOpenDataService.fetch();
        assertEquals(1, list.size());
        assertEquals("CAT1", list.get(0).getCode());
    }
    @Test
    void fetchAOOs() {
        String csv = "Codice_uni_aoo,Denominazione_aoo\nAOO1,Aoo1\n";
        when(aooRestClient.retrieveDataSource()).thenReturn(csv);
        List<IpaAoo> list = ipaAooOpenDataService.fetch();
        assertEquals(1, list.size());
        assertEquals("AOO1", list.get(0).getCodiceUniAoo());
    }
    @Test
    void fetchUOs() {
        String uoCsv = "Codice_uni_uo,Descrizione_uo\nUO1,Uo1\nUO2,Uo2\n";
        String sfeCsv = "Codice_uni_uo,Codice_fiscale_sfe\nUO1,SFE1\n";
        when(uoRestClient.retrieveDataSource()).thenReturn(uoCsv);
        when(uoSfeRestClient.retrieveDataSource()).thenReturn(sfeCsv);
        List<IpaUo> list = ipaUoOpenDataService.fetch();
        assertEquals(2, list.size());
        assertEquals("SFE1", list.stream().filter(u -> "UO1".equals(u.getId())).findFirst().get().getCodiceFiscaleSfe());
        assertEquals(null, list.stream().filter(u -> "UO2".equals(u.getId())).findFirst().get().getCodiceFiscaleSfe());
    }
}
