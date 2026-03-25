package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.OpenDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Category;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OpenDataLoader.class})
class OpenDataLoaderTest {

    @MockBean
    private OpenDataConnector openDataConnector;

    @MockBean
    private ANACService anacService;

    @MockBean
    private IvassDataConnector ivassDataConnector;

    @MockBean
    private IndexWriterService<Institution> institutionIndexWriterService;

    @MockBean
    private IndexWriterService<Category> categoryIndexWriterService;

    @MockBean
    private IndexWriterService<AOO> aooIndexWriterService;

    @MockBean
    private IndexWriterService<UO> uoIndexWriterService;

    @MockBean
    private IndexWriterService<Station> pdndIndexWriterService;

    @MockBean
    private IndexWriterService<InsuranceCompany> ivassIndexWriterService;

    @Autowired
    private OpenDataLoader openDataLoader;

    @Test
    void run() {
        // given
        final List institutions = List.of();
        when(openDataConnector.getInstitutions())
                .thenReturn(institutions);
        final List categories = List.of();
        when(openDataConnector.getCategories())
                .thenReturn(categories);
        final List aoos = List.of();
        when(openDataConnector.getAOOs())
                .thenReturn(aoos);
        final List uos = List.of();
        when(openDataConnector.getUOs())
                .thenReturn(uos);
        final List stations = List.of();
        final List insuranceCompanies = List.of();
        when(ivassDataConnector.getInsurances())
                .thenReturn(insuranceCompanies);
        // when
        openDataLoader.run();
        // then
        verify(openDataConnector, times(1))
                .getInstitutions();
        verify(institutionIndexWriterService, times(1))
                .adds(institutions);
        verify(openDataConnector, times(1))
                .getCategories();
        verify(categoryIndexWriterService, times(1))
                .adds(categories);
        verify(aooIndexWriterService, times(1))
                .adds(aoos);
        verify(uoIndexWriterService, times(1))
                .adds(uos);
        verify(pdndIndexWriterService, times(1))
                .adds(stations);
        verify(anacService, times(1))
                .loadStations();
        verify(ivassIndexWriterService, times(1))
                .adds(insuranceCompanies);
    }

}