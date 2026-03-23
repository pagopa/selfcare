package it.pagopa.selfcare.party.connector.azure_storage;

import it.pagopa.selfcare.party.connector.azure_storage.client.AzureBlobClient;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IvassDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IvassDataConnectorImplTest {

    @Mock
    private AzureBlobClient azureBlobClientMock;

    @Test
    void getInsuranceCompanies() {
        final String filename = "test.csv";
        IvassDataConnector ivassDataConnector = new IvassDataConnectorImpl(filename, List.of(), List.of(), azureBlobClientMock);
        ResourceResponse response = new ResourceResponse();
        String bytes = "CODICE_IVASS;CODICE_FISCALE;DENOMINAZIONE_IMPRESA;PEC;TIPO_LAVORO;TIPO_ALBO\n" +
                "aaaaaaa;codice_test;denominazione_test;test@pec.it;lavoro;albo\n";
        response.setData(bytes.getBytes(StandardCharsets.UTF_8));
        when(azureBlobClientMock.getFile(anyString())).thenReturn(response);
        final List<InsuranceCompany> companies = ivassDataConnector.getInsurances();
        assertNotNull(companies);
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }

    @Test
    void getInsurancesFilteredByRegistryType() {
        final String filename = "test.csv";
        IvassDataConnector ivassDataConnector = new IvassDataConnectorImpl(filename, List.of("ElencoI", "ElencoII"), List.of("DANNI"), azureBlobClientMock);
        ResourceResponse response = new ResourceResponse();
        String bytes = "CODICE_IVASS;CODICE_FISCALE;DENOMINAZIONE_IMPRESA;PEC;TIPO_LAVORO;TIPO_ALBO\n" +
                "aaaaaaa;codice_test;denominazione_test;test@pec.it;DANNI;Elenco II - imprese\n";
        response.setData(bytes.getBytes(StandardCharsets.UTF_8));
        when(azureBlobClientMock.getFile(anyString())).thenReturn(response);
        final List<InsuranceCompany> companies = ivassDataConnector.getInsurances();
        assertNotNull(companies);
        assertFalse(companies.isEmpty());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }

    @Test
    void getInsurancesFilteredByWorkType() {
        final String filename = "test.csv";
        IvassDataConnector ivassDataConnector = new IvassDataConnectorImpl(filename, List.of("ElencoI"), List.of("DANNO"), azureBlobClientMock);
        ResourceResponse response = new ResourceResponse();
        String bytes = "CODICE_IVASS;CODICE_FISCALE;DENOMINAZIONE_IMPRESA;PEC;TIPO_LAVORO;TIPO_ALBO\n" +
                "aaaaaaa;codice_test;denominazione_test;test@pec.it;DANNI;Elenco I - imprese\n";
        response.setData(bytes.getBytes(StandardCharsets.UTF_8));
        when(azureBlobClientMock.getFile(anyString())).thenReturn(response);
        final List<InsuranceCompany> companies = ivassDataConnector.getInsurances();
        assertNotNull(companies);
        assertTrue(companies.isEmpty());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }

    @Test
    void getInsurancesFileNotFound() {
        final String filename = "test.csv";
        IvassDataConnector ivassDataConnector = new IvassDataConnectorImpl(filename, List.of(), List.of(), azureBlobClientMock);
        when(azureBlobClientMock.getFile(anyString())).thenThrow(ResourceNotFoundException.class);
        final List<InsuranceCompany> insurances = ivassDataConnector.getInsurances();
        assertTrue(insurances.isEmpty());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }
}
